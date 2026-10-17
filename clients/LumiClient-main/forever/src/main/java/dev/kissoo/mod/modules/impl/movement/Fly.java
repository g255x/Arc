package dev.kissoo.mod.modules.impl.movement;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.KeyboardInputEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;

import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.Forever;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class Fly extends Module {
   public static Fly INSTANCE;

   // ================== 模式枚举 ==================
   public enum Mode {
      Normal,
      WindJump
   }

   private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Normal));

   // ================== Normal (普通飞行) 设置 ==================
   private final SliderSetting speedConfig = this.add(new SliderSetting("Speed", 2.5, 0.1, 10.0, () -> this.mode.is(Mode.Normal)));
   private final SliderSetting vspeedConfig = this.add(new SliderSetting("VerticalSpeed", 1.0, 0.1, 5.0, () -> this.mode.is(Mode.Normal)));
   private final BooleanSetting antiKickConfig = this.add(new BooleanSetting("AntiKick", true, () -> this.mode.is(Mode.Normal)).setParent());
   private final BooleanSetting up = this.add(new BooleanSetting("Up", true, () -> this.mode.is(Mode.Normal) && this.antiKickConfig.isOpen()));
   private final BooleanSetting allowSneak = this.add(new BooleanSetting("AllowSneak", false, () -> this.mode.is(Mode.Normal)));

   // ================== WindJump (风弹维持速度跳跃) 设置 ==================
   // 丢出风弹后的等待时间 (等爆炸)
   private final SliderSetting throwDelay = this.add(new SliderSetting("ThrowDelay", 300.0, 0.0, 2000.0, 10.0, () -> this.mode.is(Mode.WindJump)).setSuffix(" ms"));
   // 向上的初始起跳速度 (只给一次，随后重力接管)
   private final SliderSetting upJumpSpeed = this.add(new SliderSetting("UpSpeed", 1.2, 0.1, 5.0, 0.1, () -> this.mode.is(Mode.WindJump)));
   // 维持向前的速度大小
   private final SliderSetting forwardSpeed = this.add(new SliderSetting("ForwardSpeed", 2.0, 0.0, 10.0, 0.1, () -> this.mode.is(Mode.WindJump)));
   // 【新增】向前速度维持的时间
   private final SliderSetting maintainTime = this.add(new SliderSetting("MaintainTime", 1000.0, 100.0, 5000.0, 10.0, () -> this.mode.is(Mode.WindJump)).setSuffix(" ms"));

   // Normal 状态变量
   private final Timer antiKickTimer = new Timer();
   private final Timer antiKick2Timer = new Timer();

   // WindJump 状态变量
   private long boostStartTime = 0L; // 记录丢风弹的时间戳 (仿照Speed模块)
   private boolean hasThrown = false;
   private boolean hasJumped = false; // 是否已经给了向上的力

   public Fly() {
      super("Fly", Module.Category.Movement);
      this.setChinese("飞行");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @Override
   public void onLogin() {
      this.antiKickTimer.reset();
      this.antiKick2Timer.reset();
   }

   @Override
   public void onEnable() {
      if (nullCheck()) return;

      if (this.mode.is(Mode.Normal)) {
         // 普通飞行初始化
         this.antiKickTimer.reset();
         this.antiKick2Timer.reset();
      } else if (this.mode.is(Mode.WindJump)) {
         // 风弹起跳初始化
         this.hasThrown = false;
         this.hasJumped = false;
         this.boostStartTime = 0L;

         // 尝试丢风弹，如果没有风弹则直接关闭
         if (!throwWindChargeDown()) {
            this.disable();
            return;
         }

         this.hasThrown = true;
         this.boostStartTime = System.currentTimeMillis(); // 记录起始时间戳
      }
   }

   @Override
   public void onDisable() {
      this.hasThrown = false;
      this.boostStartTime = 0L;
      // 关闭时清理向前速度，防止一直滑行
      if (this.mode.is(Mode.WindJump) && mc.player != null) {
         MovementUtil.setMotionX(0);
         MovementUtil.setMotionZ(0);
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.mode.is(Mode.Normal)) {
         handleNormalUpdate();
      } else if (this.mode.is(Mode.WindJump)) {
         handleWindJumpUpdate();
      }
   }

   /**
    * 原版的飞行逻辑
    */
   private void handleNormalUpdate() {
      if (this.antiKickTimer.passed(3900L) && this.antiKickConfig.getValue() && !mc.player.isOnGround()) {
         MovementUtil.setMotionY(-0.04);
         this.antiKickTimer.reset();
      } else if (this.antiKick2Timer.passed(4000L) && this.antiKickConfig.getValue() && !mc.player.isOnGround() && this.up.getValue()) {
         MovementUtil.setMotionY(0.04);
         this.antiKick2Timer.reset();
      } else {
         MovementUtil.setMotionY(0.0);
         if (mc.options.jumpKey.isPressed()) {
            MovementUtil.setMotionY(this.vspeedConfig.getValue());
         } else if (mc.options.sneakKey.isPressed()) {
            MovementUtil.setMotionY(-this.vspeedConfig.getValue());
         }
      }

      double[] move = MovementUtil.directionSpeed(this.speedConfig.getValueFloat());
      MovementUtil.setMotionX(move[0]);
      MovementUtil.setMotionZ(move[1]);
   }

   /**
    * 风弹跳跃（仿照 Speed 模块的 Maintain 逻辑）
    */
   private void handleWindJumpUpdate() {
      if (mc.player == null || !this.hasThrown || this.boostStartTime == 0L) return;

      long now = System.currentTimeMillis();
      long elapsed = now - this.boostStartTime;
      long waitTime = (long) this.throwDelay.getValue(); // 等待爆炸的时间
      long totalTime = waitTime + (long) this.maintainTime.getValue(); // 等待时间 + 维持时间

      // 如果处于“维持飞行”的阶段内
      if (elapsed >= waitTime && elapsed < totalTime) {

         // 1. 只有在这个阶段的【第一帧】，才给予向上的速度，让后续自然受重力影响
         if (!this.hasJumped) {
            if (mc.player.isOnGround()) {
               mc.player.jump();
            }
            MovementUtil.setMotionY(this.upJumpSpeed.getValue());
            this.hasJumped = true;
         }

         // 2. 在这几秒的维持期间内，强制修改向前速度（类似 Speed 模块改速度）
         double yaw = Math.toRadians(mc.player.getYaw());
         double speed = this.forwardSpeed.getValue();

         // 直接赋值强制往前飞 (自动)
         MovementUtil.setMotionX(-Math.sin(yaw) * speed);
         MovementUtil.setMotionZ(Math.cos(yaw) * speed);

      }
      // 维持时间结束，自动关闭模块
      else if (elapsed >= totalTime) {
         this.disable();
      }
   }

   @EventListener(priority = -100)
   public void keyboard(KeyboardInputEvent event) {
      if (this.mode.is(Mode.Normal) && !this.allowSneak.getValue()) {
         mc.player.input.sneaking = false;
      }
   }

   /**
    * 向脚下丢弃风弹 (内部处理了物品栏交互逻辑)
    */
   private boolean throwWindChargeDown() {
      int windChargeSlot = -1;
      boolean fromInventory = false;

      if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
         windChargeSlot = mc.player.getInventory().selectedSlot;
      } else if ((windChargeSlot = InventoryUtil.findItem(Items.WIND_CHARGE)) != -1) {
         // 在快捷栏找到
      } else if ((windChargeSlot = InventoryUtil.findItemInventorySlotFromZero(Items.WIND_CHARGE)) != -1) {
         fromInventory = true; // 在背包深处找到
      }

      if (windChargeSlot == -1) return false;

      float yaw = mc.player.getYaw();
      float pitch = 90.0F; // 垂直向下

      Forever.ROTATION.snapAt(yaw, pitch);

      if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
      } else if (fromInventory) {
         InventoryUtil.inventorySwap(windChargeSlot, mc.player.getInventory().selectedSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
         InventoryUtil.inventorySwap(windChargeSlot, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
      } else {
         int oldSlot = mc.player.getInventory().selectedSlot;
         InventoryUtil.switchToSlot(windChargeSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
         InventoryUtil.switchToSlot(oldSlot);
      }

      Forever.ROTATION.snapBack();
      return true;
   }
}