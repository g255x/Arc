package dev.kissoo.mod.modules.impl.movement;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.MoveEvent;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.combat.AutoAnchor;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.Random;

public class BlockStrafe extends Module {
   public static BlockStrafe INSTANCE;

   private final SliderSetting speed = this.add(new SliderSetting("Speed", 10.0, 0.0, 20.0, 1.0).setSuffix("%"));
   private final SliderSetting aSpeed = this.add(new SliderSetting("AnchorSpeed", 3.0, 0.0, 20.0, 1.0).setSuffix("%"));

   private final BooleanSetting antiSlowFalling = this.add(new BooleanSetting("AntiSlowFalling", false));
   private final EnumSetting<AntiSlowFallingMode> antiSlowFallingMode = this.add(
           new EnumSetting<>("ASF Mode", AntiSlowFallingMode.NCPAlign, () -> antiSlowFalling.getValue()));

   private final SliderSetting asfPushUp = this.add(new SliderSetting("ASF PushUp", 0.05, 0.01, 0.2, 0.001,
           () -> antiSlowFalling.getValue() && antiSlowFallingMode.getValue() == AntiSlowFallingMode.SendOffsetPacket));
   private final SliderSetting asfPushDown = this.add(new SliderSetting("ASF PushDown", 0.05, 0.01, 0.2, 0.001,
           () -> antiSlowFalling.getValue() && antiSlowFallingMode.getValue() == AntiSlowFallingMode.SendOffsetPacket));
   private final SliderSetting asfPushDelay = this.add(new SliderSetting("ASF PushDelay", 10, 1, 50, 1,
           () -> antiSlowFalling.getValue() && antiSlowFallingMode.getValue() == AntiSlowFallingMode.SendOffsetPacket));
   private final BooleanSetting asfExtraPackets = this.add(new BooleanSetting("ASF ExtraPackets", false,
           () -> antiSlowFalling.getValue() && antiSlowFallingMode.getValue() == AntiSlowFallingMode.SendOffsetPacket));

   private final SliderSetting asfAlignY = this.add(new SliderSetting("ASF AlignY", 0.015625, 0.001, 0.1, 0.000001,
           () -> antiSlowFalling.getValue() && antiSlowFallingMode.getValue() == AntiSlowFallingMode.NCPAlign));
   private final BooleanSetting asfAutoJump = this.add(new BooleanSetting("ASF AutoJump", true,
           () -> antiSlowFalling.getValue() && antiSlowFallingMode.getValue() == AntiSlowFallingMode.NCPAlign));
   private final SliderSetting asfJumpHeight = this.add(new SliderSetting("ASF JumpHeight", 0.2, 0.01, 0.42, 0.01,
           () -> antiSlowFalling.getValue()
                   && antiSlowFallingMode.getValue() == AntiSlowFallingMode.NCPAlign
                   && asfAutoJump.getValue()));

   private final BooleanSetting asfAutoSneak = this.add(new BooleanSetting("ASF AutoSneak", false,
           () -> antiSlowFalling.getValue()));

   private boolean asfWasActive = false;
   private boolean asfIsSneakingForced = false;
   private int asfPushSequenceCounter = 0;
   private boolean asfInPushSequence = false;
   private Random asfRandom = new Random();

   public enum AntiSlowFallingMode {
      SendOffsetPacket("SendOffsetPacket"),
      NCPAlign("NCPAlign");

      private final String name;

      AntiSlowFallingMode(String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   public BlockStrafe() {
      super("BlockStrafe", Module.Category.Movement);
      this.setChinese("方块灵活移动");
      INSTANCE = this;
   }


   @Override
   public void onEnable() {
      asfWasActive = false;
      asfIsSneakingForced = false;
      asfInPushSequence = false;
      asfPushSequenceCounter = 0;
      asfRandom = new Random();
   }

   @Override
   public void onDisable() {
      if (asfWasActive) {
         asfSendNormalPacket();
      }
      if (asfIsSneakingForced) {
         asfForceSneak(false);
      }
   }


   @EventListener
   public void onMove(MoveEvent event) {
      if (EntityUtil.isInsideBlock()) {
         double speed = AutoAnchor.INSTANCE.currentPos == null ? this.speed.getValue() : this.aSpeed.getValue();
         double moveSpeed = 0.002873 * speed;
         double n = mc.player.input.movementForward;
         double n2 = mc.player.input.movementSideways;
         double n3 = mc.player.getYaw();
         if (n == 0.0 && n2 == 0.0) {
            event.setX(0.0);
            event.setZ(0.0);
         } else {
            if (n != 0.0 && n2 != 0.0) {
               n *= Math.sin(Math.PI / 4);
               n2 *= Math.cos(Math.PI / 4);
            }
            event.setX(n * moveSpeed * -Math.sin(Math.toRadians(n3)) + n2 * moveSpeed * Math.cos(Math.toRadians(n3)));
            event.setZ(n * moveSpeed * Math.cos(Math.toRadians(n3)) - n2 * moveSpeed * -Math.sin(Math.toRadians(n3)));
         }
      }
   }


   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!event.isPre() || mc.player == null || !antiSlowFalling.getValue()) return;

      boolean hasSlowFalling = mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
      boolean isInsideBlock = EntityUtil.isInsideBlock();

      if (hasSlowFalling && isInsideBlock) {
         asfHandleActiveState();
      } else {
         asfHandleInactiveState();
      }

      if (asfIsSneakingForced && !mc.options.sneakKey.isPressed()) {
         mc.options.sneakKey.setPressed(true);
      }
   }

   private void asfHandleActiveState() {
      if (!asfWasActive) {
         asfWasActive = true;
         asfPushSequenceCounter = 0;
         asfInPushSequence = false;

         if (asfAutoSneak.getValue()) {
            asfForceSneak(true);
         }

         if (antiSlowFallingMode.getValue() == AntiSlowFallingMode.SendOffsetPacket) {
            asfSendNormalPacket();
            asfExecutePushSequence();
            asfInPushSequence = true;
         }
      }

      switch (antiSlowFallingMode.getValue()) {
         case SendOffsetPacket:
            if (asfInPushSequence && asfPushSequenceCounter++ >= asfPushDelay.getValueInt()) {
               asfExecutePushSequence();
               asfPushSequenceCounter = 0;
            }
            break;

         case NCPAlign:
            asfExecuteNCPAlign();
            break;
      }
   }

   private void asfHandleInactiveState() {
      if (asfWasActive) {
         asfSendNormalPacket();
         if (asfAutoSneak.getValue() && asfIsSneakingForced) {
            asfForceSneak(false);
         }
         asfWasActive = false;
         asfInPushSequence = false;
      }
      asfPushSequenceCounter = 0;
   }


   private void asfExecuteNCPAlign() {
      if (mc.player == null || mc.getNetworkHandler() == null) return;

      if (asfAutoJump.getValue() && asfIsPlayerMoving() && mc.player.isOnGround()) {
         double height = asfJumpHeight.getValue();
         mc.player.setVelocity(mc.player.getVelocity().x, height, mc.player.getVelocity().z);
         return;
      }

      double y = mc.player.getY();
      double offset = asfAlignY.getValue();
      double newY = Math.floor(y / offset) * offset;
      double diff = y - newY;

      if (diff > offset * 0.6 && diff < 0.05) {
         mc.player.setPosition(mc.player.getX(), newY, mc.player.getZ());
         mc.getNetworkHandler().sendPacket(
                 new PlayerMoveC2SPacket.PositionAndOnGround(
                         mc.player.getX(),
                         newY,
                         mc.player.getZ(),
                         mc.player.isOnGround()
                 )
         );
      }
   }

   private boolean asfIsPlayerMoving() {
      return mc.options.forwardKey.isPressed()
              || mc.options.backKey.isPressed()
              || mc.options.leftKey.isPressed()
              || mc.options.rightKey.isPressed();
   }


   private void asfExecutePushSequence() {
      if (mc.getNetworkHandler() == null || mc.player == null) return;

      double baseX = mc.player.getX();
      double baseY = mc.player.getY();
      double baseZ = mc.player.getZ();
      float yaw = mc.player.getYaw();
      float pitch = mc.player.getPitch();
      double upOffset = asfPushUp.getValue();
      double downOffset = asfPushDown.getValue();

      asfSendBasicTwoStep(baseX, baseY, baseZ, upOffset, downOffset);

      if (asfExtraPackets.getValue()) {
         asfSendExtraPackets(baseX, baseY, baseZ, upOffset, downOffset, yaw, pitch);
      }

      asfSendPacket(baseX, baseY - downOffset * 0.2, baseZ, true);
   }

   private void asfSendBasicTwoStep(double x, double y, double z, double up, double down) {
      asfSendPacket(x, y + up, z, false);

      if (asfRandom.nextBoolean() && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(
                 new PlayerMoveC2SPacket.LookAndOnGround(
                         mc.player.getYaw(),
                         mc.player.getPitch(),
                         false
                 )
         );
      }

      asfSendPacket(x, y - down, z, true);
   }

   private void asfSendExtraPackets(double baseX, double baseY, double baseZ,
                                    double up, double down, float yaw, float pitch) {
      int extraCount = asfRandom.nextInt(3);
      for (int i = 0; i < extraCount; i++) {
         double offsetX = (asfRandom.nextDouble() - 0.5) * up * 0.3;
         double offsetZ = (asfRandom.nextDouble() - 0.5) * up * 0.3;
         double offsetY = asfRandom.nextBoolean()
                 ? asfRandom.nextDouble() * up * 0.5
                 : -asfRandom.nextDouble() * down * 0.3;
         boolean onGround = asfRandom.nextDouble() < 0.3;

         asfSendPacket(baseX + offsetX, baseY + offsetY, baseZ + offsetZ, onGround);

         if (asfRandom.nextFloat() < 0.4f && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.LookAndOnGround(
                            yaw + (asfRandom.nextFloat() - 0.5f) * 10,
                            pitch + (asfRandom.nextFloat() - 0.5f) * 10,
                            onGround
                    )
            );
         }
      }
   }


   private void asfSendPacket(double x, double y, double z, boolean onGround) {
      if (mc.getNetworkHandler() == null) return;
      mc.getNetworkHandler().sendPacket(
              new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround)
      );
   }

   private void asfSendNormalPacket() {
      if (mc.player == null || mc.getNetworkHandler() == null) return;
      mc.getNetworkHandler().sendPacket(
              new PlayerMoveC2SPacket.PositionAndOnGround(
                      mc.player.getX(),
                      mc.player.getY(),
                      mc.player.getZ(),
                      mc.player.isOnGround()
              )
      );
   }

   private void asfForceSneak(boolean pressed) {
      if (mc.options == null) return;
      mc.options.sneakKey.setPressed(pressed);
      asfIsSneakingForced = pressed;
   }

   @Override
   public String getInfo() {
      if (antiSlowFalling.getValue() && asfWasActive) {
         return "ASF:" + antiSlowFallingMode.getValue().toString();
      }
      return null;
   }
}