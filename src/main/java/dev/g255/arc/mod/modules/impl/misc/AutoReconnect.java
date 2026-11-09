package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.ServerConnectBeginEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import dev.g255.arc.mod.modules.settings.impl.StringSetting;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import java.util.HashMap;
import net.minecraft.util.Hand;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class AutoReconnect extends Module {
   public static AutoReconnect INSTANCE;
   public final BooleanSetting rejoin = this.add(new BooleanSetting("Rejoin", true));
   public final SliderSetting delay = this.add((new SliderSetting("Delay", (double)5.0F, (double)0.0F, (double)20.0F, 0.1)).setSuffix("s"));
   public final BooleanSetting autoLogin = this.add(new BooleanSetting("AutoAuth", true));
   public final SliderSetting afterLoginTime = this.add((new SliderSetting("AfterLoginTime", (double)3.0F, (double)0.0F, (double)10.0F, 0.1)).setSuffix("s"));
   public final BooleanSetting autoQueue = this.add(new BooleanSetting("AutoQueue", true));
   public final SliderSetting joinQueueDelay = this.add((new SliderSetting("JoinQueueDelay", (double)3.0F, (double)0.0F, (double)10.0F, 0.1)).setSuffix("s"));
   final StringSetting password = this.add(new StringSetting("password", "123456"));
   public final BooleanSetting autoAnswer = this.add(new BooleanSetting("AutoAnswer", true));
   public static boolean inQueueServer;
   private boolean queueingByMessage;
   private final Timer queueTimer = new Timer();
   private final Timer queueMenuTimer = new Timer();
   private final Timer timer = new Timer();
   public Pair<ServerAddress, ServerInfo> lastServerConnection;
   private boolean login = false;
   final String[] abc = new String[]{"A", "B", "C"};
   public static final HashMap<String, String> asks = new HashMap<String, String>() {
      {
         this.put("红石火把", "15");
         this.put("猪被闪电", "僵尸猪人");
         this.put("小箱子能", "27");
         this.put("开服年份", "2020");
         this.put("定位末地遗迹", "0");
         this.put("爬行者被闪电", "高压爬行者");
         this.put("大箱子能", "54");
         this.put("羊驼会主动", "不会");
         this.put("无限水", "3");
         this.put("挖掘速度最快", "金镐");
         this.put("凋灵死后", "下界之星");
         this.put("苦力怕的官方", "爬行者");
         this.put("南瓜的生长", "不需要");
         this.put("定位末地", "0");
      }
   };

   public AutoReconnect() {
      super("AutoReconnect", Module.Category.Misc);
      this.setChinese("自动重连");
      INSTANCE = this;
      Arc.EVENT_BUS.subscribe(new StaticListener());
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      // 单人/局域网存档没有 AuthMe 这类登录插件，也不是排队服：往里面发 /login 只会换来一句
      // 「未知或不完整的命令」；自动排队更是会在有指南针时每 3 秒把手持物品切成指南针。
      // 这些功能只在真正的多人服务器上才有意义。
      boolean multiplayer = !mc.isInSingleplayer();

      if (this.login && this.timer.passedS(this.afterLoginTime.getValue())) {
         this.login = false;

         String password = this.password.getValue();
         if (multiplayer && mc.getNetworkHandler() != null && password != null && !password.isBlank()) {
            mc.getNetworkHandler().sendChatCommand("login " + password);
         }
      }

      if (multiplayer && this.autoQueue.getValue() && !this.queueingByMessage && InventoryUtil.findItem(Items.COMPASS) != -1 && this.queueTimer.passedS(this.joinQueueDelay.getValue())) {
         InventoryUtil.switchToSlot(InventoryUtil.findItem(Items.COMPASS));
         sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Arc.ROTATION.getLastYaw(), Arc.ROTATION.getLastPitch()));
         this.queueTimer.reset();
      }

      if (nullCheck()) {
         inQueueServer = false;
         this.queueingByMessage = false;
      } else {
         if (multiplayer && this.autoQueue.getValue() && !this.queueingByMessage && mc.currentScreen instanceof GenericContainerScreen && this.queueMenuTimer.passedS(this.joinQueueDelay.getValue())) {
            GenericContainerScreenHandler container = (GenericContainerScreenHandler)((GenericContainerScreen)mc.currentScreen).getScreenHandler();
            if (container != null && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
               int size = container.getInventory().size();
               int targetSlot = size == 9 ? 4 : (size == 5 ? 2 : -1);
               if (targetSlot != -1) {
                  ItemStack stack = container.getInventory().getStack(targetSlot);
                  if (stack != null && stack.getItem() == Items.COMPASS) {
                     mc.interactionManager.clickSlot(container.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                     this.queueMenuTimer.reset();
                  }
               }
            }
         }

         inQueueServer = InventoryUtil.findItem(Items.COMPASS) != -1;
      }
   }

   public void onLogin() {
      if (this.autoLogin.getValue()) {
         this.login = true;
         this.timer.reset();
      }

   }

   public boolean rejoin() {
      return this.isOn() && this.rejoin.getValue() && !AutoLog.loggedOut;
   }

   public void onLogout() {
      inQueueServer = false;
      this.queueingByMessage = false;
   }

   public void onDisable() {
      inQueueServer = false;
      this.queueingByMessage = false;
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive e) {
      if (!nullCheck()) {
         Packet<?> packet = e.getPacket();
         String content = null;
         if (packet instanceof GameMessageS2CPacket) {
            GameMessageS2CPacket packet2 = (GameMessageS2CPacket)packet;
            content = packet2.content().getString();
            if (content.toLowerCase().contains("position in queue")) {
               this.queueingByMessage = true;
            }
         }

         if (this.autoAnswer.getValue()) {
            if (inQueueServer) {
               if (content != null) {
                  for(String key : asks.keySet()) {
                     if (content.contains(key)) {
                        for(String s : this.abc) {
                           if (content.contains(s + "." + (String)asks.get(key))) {
                              mc.getNetworkHandler().sendChatMessage(s.toLowerCase());
                              return;
                           }
                        }
                     }
                  }
               }

            }
         }
      }
   }

   private class StaticListener {
      @EventListener
      private void onGameJoined(ServerConnectBeginEvent event) {
         AutoReconnect.this.lastServerConnection = new ObjectObjectImmutablePair(event.address, event.info);
      }
   }
}
