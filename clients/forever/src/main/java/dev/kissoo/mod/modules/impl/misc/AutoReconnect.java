package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.ServerConnectBeginEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.HashMap;

public class AutoReconnect extends Module {
   public static AutoReconnect INSTANCE;

   public final BooleanSetting rejoin          = this.add(new BooleanSetting("Rejoin", true));
   public final SliderSetting  delay           = this.add(new SliderSetting("Delay", 5.0, 0.0, 20.0, 0.1).setSuffix("s"));
   public final BooleanSetting autoLogin       = this.add(new BooleanSetting("AutoAuth", true));
   public final SliderSetting  afterLoginTime  = this.add(new SliderSetting("AfterLoginTime", 3.0, 0.0, 10.0, 0.1).setSuffix("s"));
   public final BooleanSetting autoQueue       = this.add(new BooleanSetting("AutoQueue", true));
   public final SliderSetting  joinQueueDelay  = this.add(new SliderSetting("JoinQueueDelay", 3.0, 0.0, 10.0, 0.1).setSuffix("s"));
   final StringSetting         password        = this.add(new StringSetting("password", "123456"));
   public final BooleanSetting autoAnswer      = this.add(new BooleanSetting("AutoAnswer", true));

   public static boolean inQueueServer = false;

   private enum QueueState {
      IDLE, SWITCH, RIGHT_CLICK, WAIT_GUI, CLICK_COMPASS
   }
   private QueueState queueState = QueueState.IDLE;

   private final Timer queueTimer  = new Timer();
   private final Timer timer       = new Timer();
   private final Timer stateTimer  = new Timer();

   public Pair<ServerAddress, ServerInfo> lastServerConnection;

   private boolean login = false;

   final String[] abc = new String[]{"A", "B", "C"};

   public static final HashMap<String, String> asks = new HashMap<String, String>() {{
      put("红石火把",    "15");
      put("猪被闪电",    "僵尸猪人");
      put("小箱子能",    "27");
      put("开服年份",    "2020");
      put("定位末地遗迹", "0");
      put("爬行者被闪电", "高压爬行者");
      put("大箱子能",    "54");
      put("羊驼会主动",  "不会");
      put("无限水",      "3");
      put("挖掘速度最快", "金镐");
      put("凋灵死后",    "下界之星");
      put("苦力怕的官方", "爬行者");
      put("南瓜的生长",  "不需要");
      put("定位末地",    "0");
   }};

   public AutoReconnect() {
      super("AutoReconnect", Module.Category.Misc);
      this.setChinese("自动重连");
      INSTANCE = this;
      Forever.EVENT_BUS.subscribe(new StaticListener());
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.login && this.timer.passedS(this.afterLoginTime.getValue())) {
         mc.getNetworkHandler().sendChatCommand("login " + this.password.getValue());
         this.login = false;
      }

      if (nullCheck()) {
         inQueueServer = false;
         queueState = QueueState.IDLE;
         return;
      }

      inQueueServer = InventoryUtil.findItem(Items.COMPASS) != -1;

      if (this.autoQueue.getValue() && inQueueServer) {
         handleQueueState();
      }
   }

   private void handleQueueState() {
      switch (queueState) {

         case IDLE -> {
            if (queueTimer.passedS(joinQueueDelay.getValue())) {
               queueState = QueueState.SWITCH;
               stateTimer.reset();
            }
         }

         case SWITCH -> {
            int slot = InventoryUtil.findItem(Items.COMPASS);
            if (slot == -1) {
               queueState = QueueState.IDLE;
               queueTimer.reset();
               return;
            }
            InventoryUtil.switchToSlot(slot);
            queueState = QueueState.RIGHT_CLICK;
            stateTimer.reset();
         }

         case RIGHT_CLICK -> {
            if (stateTimer.passedMs(250)) {
               sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(
                       Hand.MAIN_HAND, id,
                       Forever.ROTATION.getLastYaw(),
                       Forever.ROTATION.getLastPitch()
               ));
               queueState = QueueState.WAIT_GUI;
               stateTimer.reset();
            }
         }

         case WAIT_GUI -> {
            if (mc.currentScreen instanceof GenericContainerScreen screen) {
               queueState = QueueState.CLICK_COMPASS;
               stateTimer.reset();
            } else if (stateTimer.passedS(3.0)) {
               queueState = QueueState.IDLE;
               queueTimer.reset();
            }
         }

         case CLICK_COMPASS -> {
            if (!stateTimer.passedMs(250)) return;

            if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
               queueState = QueueState.IDLE;
               queueTimer.reset();
               return;
            }

            int targetSlot = -1;
            for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
               if (screen.getScreenHandler().slots.get(i).getStack().getItem() == Items.COMPASS) {
                  targetSlot = i;
                  break;
               }
            }

            if (targetSlot == -1) {
               if (stateTimer.passedS(2.0)) {
                  queueState = QueueState.IDLE;
                  queueTimer.reset();
               }
               return;
            }

            Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectArrayMap<>();

            int syncId = screen.getScreenHandler().syncId;
            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                    syncId,
                    screen.getScreenHandler().getRevision(),
                    targetSlot,
                    0,
                    SlotActionType.PICKUP,
                    screen.getScreenHandler().slots.get(targetSlot).getStack().copy(),
                    changedSlots
            ));

            mc.player.closeHandledScreen();
            queueState = QueueState.IDLE;
            queueTimer.reset();
         }
      }
   }

   @Override
   public void onLogin() {
      AutoLog.loggedOut = false;

      if (this.autoLogin.getValue()) {
         this.login = true;
         this.timer.reset();
      }
   }

   public boolean rejoin() {
      return this.isOn() && this.rejoin.getValue() && !AutoLog.loggedOut;
   }

   @Override
   public void onLogout() {
      inQueueServer = false;
      queueState = QueueState.IDLE;
   }

   @Override
   public void onDisable() {
      inQueueServer = false;
      queueState = QueueState.IDLE;
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive e) {
      if (nullCheck()) return;
      if (!this.autoAnswer.getValue()) return;
      if (!inQueueServer) return;

      if (e.getPacket() instanceof GameMessageS2CPacket packet) {
         String msg = packet.content().getString();
         for (String key : asks.keySet()) {
            if (msg.contains(key)) {
               for (String s : abc) {
                  if (msg.contains(s + "." + asks.get(key))) {
                     mc.getNetworkHandler().sendChatMessage(s.toLowerCase());
                     return;
                  }
               }
            }
         }
      }
   }

   private class StaticListener {
      @EventListener
      private void onGameJoined(ServerConnectBeginEvent event) {
         AutoReconnect.this.lastServerConnection =
                 new ObjectObjectImmutablePair<>(event.address, event.info);
      }
   }
}