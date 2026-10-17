package dev.kissoo.mod.modules.impl.player;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.RotationEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.Wrapper;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.world.BlockPosX;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class AutoPot extends Module {
   public static AutoPot INSTANCE;

   private final BooleanSetting debug = this.add(new BooleanSetting("Debug", false));
   private String lastDebugState = "";
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 5.0, 0.0, 10.0, 0.1).setSuffix("s"));
   private final BooleanSetting speed = this.add(new BooleanSetting("Speed", false));
   private final BooleanSetting resistance = this.add(new BooleanSetting("Resistance", false));
   private final BooleanSetting strength = this.add(new BooleanSetting("Strength", false));
   private final BooleanSetting slowFalling = this.add(new BooleanSetting("SlowFalling", false));
   private final SliderSetting frontTick = this.add(new SliderSetting("FrontTick", 20.0, 1.0, 100.0, 1.0).setSuffix("ticks"));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting checkPlayer = this.add(new BooleanSetting("CheckPlayer", false));
   private final SliderSetting playerRange = this.add(new SliderSetting("Range", 10.0, 1.0, 50.0, 1.0).setSuffix("m"));
   private final BooleanSetting emergencyPot = this.add(new BooleanSetting("EmergencyPot", true).setParent());
   private final SliderSetting emergencyHealth = this.add(
           new SliderSetting("EmergencyHP", 6.0, 1.0, 19.0, 0.5, () -> this.emergencyPot.isOpen()).setSuffix("HP")
   );
   private final Timer emergencyTimer = new Timer();
   private float lastHealth = 20.0F;
   public final BooleanSetting rotateDown = this.add(new BooleanSetting("RotateDown", true)).setParent();
   private final SliderSetting steps = this.add(new SliderSetting("Steps", 0.61, 0.0, 1.0, 0.01, () -> this.rotateDown.isOpen()));
   private final SliderSetting priority = this.add(new SliderSetting("Priority", 100, 0, 100, () -> this.rotateDown.isOpen()));
   private final BooleanSetting packetSneak = this.add(new BooleanSetting("PacketSneak", true).setParent());
   private final SliderSetting preSneakTicks = this.add(new SliderSetting("PreSneakTicks", 2.0, 1.0, 10.0, 1.0, packetSneak::isOpen).setSuffix("ticks"));
   private final SliderSetting postSneakTicks = this.add(new SliderSetting("PostSneakTicks", 5.0, 1.0, 20.0, 1.0, packetSneak::isOpen).setSuffix("ticks"));

   private final BindSetting speedKey = this.add(new BindSetting("SpeedKey", -1));
   private final BindSetting strengthKey = this.add(new BindSetting("StrengthKey", -1));
   private final BindSetting resistanceKey = this.add(new BindSetting("ResistanceKey", -1));

   private final Timer delayTimer = new Timer();
   private boolean throwing = false;
   private boolean turtlePress;
   private boolean speedPress;
   private boolean strengthPress;
   public Vec3d directionVec = null;

   private StatusEffect pendingPotion = null;
   private int preSneakTimer = 0;
   private int postSneakTimer = 0;
   private boolean isPacketSneaking = false;
   private final Timer resCD = new Timer();
   private final Timer speedCD = new Timer();
   private final Timer strCD = new Timer();
   private final Timer slowCD = new Timer();
   private static final long RES_DURATION_MS = 18000L;
   private static final long SPEED_DURATION_MS = 85000L;
   private static final long STR_DURATION_MS = 55000L;
   private static final long SLOW_DURATION_MS = 85000L;

   private boolean timerActive() { return Forever.TIMER.get() > 1.05F; }
   private float timerAdjusted(float v) { return v * Forever.TIMER.get(); }

   public AutoPot() {
      super("AutoPot", Module.Category.Player);
      this.setChinese("自动药水");
      INSTANCE = this;
      Forever.EVENT_BUS.subscribe(new AutoPot.AutoPotTick());
   }
   private void printDebug(String message) {
      if (this.debug.getValue() && mc.player != null) {
         mc.player.sendMessage(Text.literal("§8[§bAutoPot-Action§8] §f" + message), false);
      }
   }

   private void printState(String stateID, String message) {
      if (!this.debug.getValue() || mc.player == null) return;
      if (!stateID.equals(this.lastDebugState)) {
         mc.player.sendMessage(Text.literal("§8[§6AutoPot-Check§8] §7" + message), false);
         this.lastDebugState = stateID;
      }
   }

   public boolean isPausing() {
      if (this.isOff() || mc.player == null) return false;
      if (this.throwing || this.turtlePress || this.speedPress || this.strengthPress || this.pendingPotion != null) return true;
      if (!this.delayTimer.passedMs(this.delay.getValue() * 1000.0 - 100.0)) return false;
      if (this.inventory.getValue() && !EntityUtil.inInventory()) return false;
      if (this.onlyGround.getValue() && !(mc.player.isOnGround() || Forever.PLAYER.isInWeb(mc.player)) && mc.world.isAir(new BlockPosX(mc.player.getPos().add(0.0, -1.0, 0.0)))) return false;
      if (this.checkPlayer.getValue() && !this.isPlayerInRange()) return false;

      if (this.resistance.getValue() && (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2 || !this.timerActive() && (float)mc.player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() <= 2.0F)) {
         if (this.checkThrow((StatusEffect)StatusEffects.RESISTANCE.value())) return true;
      }
      if (this.speed.getValue() && (!mc.player.hasStatusEffect(StatusEffects.SPEED) || !this.timerActive() && (float)mc.player.getStatusEffect(StatusEffects.SPEED).getDuration() <= 2.0F)) {
         if (this.checkThrow((StatusEffect)StatusEffects.SPEED.value())) return true;
      }
      if (this.strength.getValue() && (!mc.player.hasStatusEffect(StatusEffects.STRENGTH) || !this.timerActive() && (float)mc.player.getStatusEffect(StatusEffects.STRENGTH).getDuration() <= 2.0F)) {
         if (this.checkThrow((StatusEffect)StatusEffects.STRENGTH.value())) return true;
      }
      if (this.slowFalling.getValue() && (!mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) || !this.timerActive() && (float)mc.player.getStatusEffect(StatusEffects.SLOW_FALLING).getDuration() <= 2.0F)) {
         if (this.checkThrow((StatusEffect)StatusEffects.SLOW_FALLING.value())) return true;
      }

      StatusEffectInstance resInst = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
      StatusEffectInstance fireResInst = mc.player.getStatusEffect(StatusEffects.FIRE_RESISTANCE);
      if (resInst != null && fireResInst != null) {
         int resDur = resInst.getDuration();
         int fireDur = fireResInst.getDuration();
         if (!this.timerActive() && resDur > 0 && fireDur > 0 && ((float)resDur <= this.frontTick.getValueFloat() || (float)fireDur <= this.frontTick.getValueFloat())) {
            if (this.checkThrow((StatusEffect)StatusEffects.RESISTANCE.value())) return true;
         }
      } else if (resInst == null && fireResInst == null) {
         if (this.checkThrow((StatusEffect)StatusEffects.RESISTANCE.value())) return true;
      }

      return false;
   }

   public boolean isActivelyThrowing() {
      return this.throwing || this.turtlePress || this.speedPress || this.strengthPress || this.pendingPotion != null;
   }

   public static int findPotionInventorySlot(StatusEffect targetEffect) {
      for (int i = 35; i >= 0; i--) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)itemStack.getOrDefault(
                    DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT
            );

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  return i < 9 ? i + 36 : i;
               }
            }
         }
      }
      return -1;
   }

   @EventListener
   public void RotateEvent(RotationEvent event) {
      if (this.rotateDown.getValue()) {
         if (directionVec != null) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
         }
      }
   }

   public static int findPotion(StatusEffect targetEffect) {
      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)itemStack.getOrDefault(
                    DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT
            );

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  return i;
               }
            }
         }
      }
      return -1;
   }

   @Override
   public void onDisable() {
      if (this.isPacketSneaking && mc.player != null) {
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
         printDebug("模块关闭，释放下蹲状态");
         this.isPacketSneaking = false;
      }
      this.pendingPotion = null;
      this.preSneakTimer = 0;
      this.postSneakTimer = 0;
      this.throwing = false;
      this.lastDebugState = "";
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.throwing = false;
      // Emergency pot: throw when health drops sharply
      if (this.emergencyPot.getValue() && this.emergencyPot.isOpen() && mc.player != null) {
         float hp = mc.player.getHealth();
         float delta = this.lastHealth - hp;
         if ((double)delta >= this.emergencyHealth.getValue() && this.emergencyTimer.passedMs(100.0)) {
            boolean hasArmor = false;
            for (var stack : mc.player.getArmorItems()) {
               if (stack != null && !stack.isEmpty()) { hasArmor = true; break; }
            }
            if (hasArmor) {
               printDebug("§c急救触发: 掉血 " + String.format("%.1f", delta) + " HP, 立即喷药!");
               this.emergencyTimer.reset();
               if (this.checkThrow(StatusEffects.RESISTANCE.value())) {
                  this.throwPotion(StatusEffects.RESISTANCE.value());
                  this.resCD.reset();
               }
            }
         }
         this.lastHealth = hp;
      }
      if (this.postSneakTimer > 0) {
         this.postSneakTimer--;
         this.throwing = true;
         if (this.postSneakTimer <= 0 && this.isPacketSneaking) {
            if (!mc.options.sneakKey.isPressed()) {
               mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
               printDebug("后摇结束，发送取消下蹲包");
            }
            this.isPacketSneaking = false;
         }
      }
      if (this.pendingPotion != null) {
         this.throwing = true;
         if (this.preSneakTimer > 0) {
            this.preSneakTimer--;
         }
         if (this.preSneakTimer <= 0) {
            printDebug("前摇结束，开始执行投掷");
            this.executeThrow(this.pendingPotion);
            this.pendingPotion = null;
            if (this.packetSneak.getValue()) {
               this.postSneakTimer = this.postSneakTicks.getValueInt();
            }
         }
         return;
      }
      boolean needRes = this.resistance.getValue() && (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2);
      boolean needSpeed = this.speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED);
      boolean needStr = this.strength.getValue() && !mc.player.hasStatusEffect(StatusEffects.STRENGTH);
      boolean needSlow = this.slowFalling.getValue() && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);

      boolean needCombo = false;
      StatusEffectInstance resEffect = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
      StatusEffectInstance slowEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
      if (resEffect != null && slowEffect != null) {
         if (resEffect.getDuration() <= this.frontTick.getValue() || slowEffect.getDuration() <= this.frontTick.getValue()) {
            needCombo = true;
         }
      } else if (resEffect == null && slowEffect == null) {
         needCombo = true;
      }
      if (!needRes && !needSpeed && !needStr && !needSlow && !needCombo) {
         printState("Standby", "待机状态: 状态良好，无需补药");
         return;
      }


      if (this.inventory.getValue() && !EntityUtil.inInventory()) {
         printState("Global_InvCheck", "全局拦截: 必须处于背包界面外 (inInventory=false)");
         return;
      }

      if (!this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) {
         printState("Global_Delay", String.format("全局拦截: 扔药CD冷却中 (设置: %.1fs)", this.delay.getValue()));
         return;
      }

      if (this.onlyGround.getValue() && !(mc.player.isOnGround() || Forever.PLAYER.isInWeb(mc.player)) && mc.world.isAir(new BlockPosX(mc.player.getPos().add(0.0, -1.0, 0.0)))) {
         printState("Global_Ground", "全局拦截: OnlyGround已开启，但你目前在空中");
         return;
      }


      if (needRes && tryThrowEffect((StatusEffect)StatusEffects.RESISTANCE.value(), "抗性")) return;
      if (needSpeed && tryThrowEffect((StatusEffect)StatusEffects.SPEED.value(), "速度")) return;
      if (needStr && tryThrowEffect((StatusEffect)StatusEffects.STRENGTH.value(), "力量")) return;
      if (needSlow && tryThrowEffect((StatusEffect)StatusEffects.SLOW_FALLING.value(), "缓降")) return;
      if (needCombo && tryThrowEffect((StatusEffect)StatusEffects.RESISTANCE.value(), "抗性(连招补药)")) return;
   }

   private boolean tryThrowEffect(StatusEffect targetEffect, String effectName) {
      if (!this.checkThrow(targetEffect)) {
         if (this.usingPause.getValue() && mc.player.isUsingItem()) {
            printState("Pause_Eating_" + effectName, "内部拦截 [" + effectName + "]: UsingPause开启，且玩家正在使用物品(如吃苹果)");
         } else {
            printState("Pause_NotFound_" + effectName, "内部拦截 [" + effectName + "]: 背包或快捷栏内【未找到】该药水");
         }
         return false;
      }

      if (this.checkPlayer.getValue() && !this.isPlayerInRange()) {
         printState("Pause_NoPlayer_" + effectName, "内部拦截 [" + effectName + "]: CheckPlayer开启，但周围范围(" + this.playerRange.getValue() + "m)内无敌人");
         return false;
      }

      printState("Action_Prepare_" + effectName, "条件全满足，准备投掷: [" + effectName + "]");
      this.prepareThrow(targetEffect);
      return true;
   }

   public void prepareThrow(StatusEffect targetEffect) {
      if (this.packetSneak.getValue()) {
         if (!this.isPacketSneaking && !mc.player.isSneaking()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            printDebug("发送下蹲包，开始进入前摇 (" + this.preSneakTicks.getValueInt() + " ticks)...");
            this.isPacketSneaking = true;
         }
         this.pendingPotion = targetEffect;
         this.preSneakTimer = this.preSneakTicks.getValueInt();
         this.throwing = true;
      } else {
         printDebug("未开启PacketSneak，无前摇直接投掷！");
         this.executeThrow(targetEffect);
      }
   }

   public void throwPotion(StatusEffect targetEffect) {
      prepareThrow(targetEffect);
   }

   private void executeThrow(StatusEffect targetEffect) {
      int oldSlot = mc.player.getInventory().selectedSlot;
      int newSlot;

      if (this.inventory.getValue() && (newSlot = findPotionInventorySlot(targetEffect)) != -1) {
         printDebug("找到背包槽位 [" + newSlot + "]，发送背包发包连招");
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), 90.0F, mc.player.isOnGround()));
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), 90.0F));
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
      } else if ((newSlot = findPotion(targetEffect)) != -1) {
         printDebug("找到快捷栏槽位 [" + newSlot + "]，发送快捷栏发包连招");
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), 90.0F, mc.player.isOnGround()));
         InventoryUtil.switchToSlot(newSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), 90.0F));
         InventoryUtil.switchToSlot(oldSlot);
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
      } else {
         printDebug("§c致命异常：执行投掷时药水槽位消失！");
      }

      this.delayTimer.reset();
      this.resetEffectCD(targetEffect);
   }

   private void resetEffectCD(StatusEffect effect) {
      if (effect == StatusEffects.RESISTANCE.value()) this.resCD.reset();
      else if (effect == StatusEffects.SPEED.value()) this.speedCD.reset();
      else if (effect == StatusEffects.STRENGTH.value()) this.strCD.reset();
      else if (effect == StatusEffects.SLOW_FALLING.value()) this.slowCD.reset();
   }

   public boolean isThrow() {
      return this.throwing || this.pendingPotion != null;
   }

   public boolean checkThrow(StatusEffect targetEffect) {
      if (!EntityUtil.inInventory()) {
         return false;
      } else {
         return this.usingPause.getValue() && mc.player.isUsingItem()
                 ? false
                 : findPotion(targetEffect) != -1 || this.inventory.getValue() && findPotionInventorySlot(targetEffect) != -1;
      }
   }

   private boolean isPlayerInRange() {
      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player != mc.player
                 && mc.player.distanceTo(player) <= this.playerRange.getValue()
                 && player.getDisplayName() != null
                 && !Forever.FRIEND.isFriend(player)) {
            return true;
         }
      }
      return false;
   }

   public class AutoPotTick {
      @EventListener
      public void onTick(ClientTickEvent event) {
         if (!Module.nullCheck() && !event.isPost() && (!AutoPot.this.inventory.getValue() || EntityUtil.inInventory())) {
            if (Wrapper.mc.currentScreen == null) {
               if (AutoPot.this.resistanceKey.isPressed()) {
                  if (!AutoPot.this.turtlePress) {
                     if (!AutoPot.this.checkThrow((StatusEffect)StatusEffects.RESISTANCE.value())) {
                        printDebug("按键拦截：未找到抗性药水，或正在吃东西(UsingPause)");
                     } else if (AutoPot.this.checkPlayer.getValue() && !AutoPot.this.isPlayerInRange()) {
                        printDebug("按键拦截：周围无敌人(CheckPlayer开启)");
                     } else {
                        printDebug("手动按键触发：抗性药水");
                        AutoPot.this.prepareThrow((StatusEffect)StatusEffects.RESISTANCE.value());
                     }
                     AutoPot.this.turtlePress = true;
                  }
               } else {
                  AutoPot.this.turtlePress = false;
               }

               if (AutoPot.this.strengthKey.isPressed()) {
                  if (!AutoPot.this.strengthPress) {
                     if (!AutoPot.this.checkThrow((StatusEffect)StatusEffects.STRENGTH.value())) {
                        printDebug("按键拦截：未找到力量药水，或正在吃东西(UsingPause)");
                     } else if (AutoPot.this.checkPlayer.getValue() && !AutoPot.this.isPlayerInRange()) {
                        printDebug("按键拦截：周围无敌人(CheckPlayer开启)");
                     } else {
                        printDebug("手动按键触发：力量药水");
                        AutoPot.this.prepareThrow((StatusEffect)StatusEffects.STRENGTH.value());
                     }
                     AutoPot.this.strengthPress = true;
                  }
               } else {
                  AutoPot.this.strengthPress = false;
               }

               if (AutoPot.this.speedKey.isPressed()) {
                  if (!AutoPot.this.speedPress) {
                     if (!AutoPot.this.checkThrow((StatusEffect)StatusEffects.SPEED.value())) {
                        printDebug("按键拦截：未找到速度药水，或正在吃东西(UsingPause)");
                     } else if (AutoPot.this.checkPlayer.getValue() && !AutoPot.this.isPlayerInRange()) {
                        printDebug("按键拦截：周围无敌人(CheckPlayer开启)");
                     } else {
                        printDebug("手动按键触发：速度药水");
                        AutoPot.this.prepareThrow((StatusEffect)StatusEffects.SPEED.value());
                     }
                     AutoPot.this.speedPress = true;
                  }
               } else {
                  AutoPot.this.speedPress = false;
               }
            } else {
               AutoPot.this.speedPress = false;
               AutoPot.this.turtlePress = false;
               AutoPot.this.strengthPress = false;
            }
         }
      }
   }
}