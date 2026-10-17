package dev.baimaodaxia.mod.modules.impl.player;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.KeyboardInputEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.TickEvent;
import dev.baimaodaxia.asm.accessors.IPlayerMoveC2SPacket;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.exploit.BowBomb;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class NoFall extends Module {
   private final EnumSetting<NoFallMode> mode;
   private final SliderSetting getDistance;
   private boolean grimCheck;
   private boolean grimS08Check;
   private boolean grimJump;
   private boolean grimReceiveS08;
   private boolean grimJumpSuccess;
   private int grimTicks;

   public NoFall() {
      super("NoFall", "Prevents fall damage.", Module.Category.Player);
      this.mode = this.add(new EnumSetting("Mode", NoFall.NoFallMode.Packet));
      this.getDistance = this.add(new SliderSetting("Distance", (double)3.0F, (double)0.0F, (double)8.0F, 0.1));
      this.setChinese("没有摔落伤害");
   }

   public void onLogin() {
      this.resetGrim();
   }

   public void onEnable() {
      this.resetGrim();
   }

   public void onDisable() {
      this.resetGrim();
   }

   public String getInfo() {
      return ((NoFallMode)this.mode.getValue()).name();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.isPre()) {
         if (this.mode.is(NoFall.NoFallMode.Grim)) {
            if (!nullCheck()) {
               if (!mc.isInSingleplayer()) {
                  if (this.grimReceiveS08) {
                     if (mc.player.getVelocity().y > 0.2 && mc.options.jumpKey.isPressed() && !mc.player.isOnGround()) {
                        this.grimJumpSuccess = true;
                     }

                     if (this.grimJumpSuccess && ++this.grimTicks >= 20) {
                        this.resetGrim();
                     }
                  }

                  if (mc.player.fallDistance >= this.getDistance.getValueFloat()) {
                     this.grimCheck = true;
                  }

                  if (mc.player.isOnGround() && mc.player.verticalCollision && this.grimCheck) {
                     this.grimJump = true;
                     this.grimS08Check = true;
                     this.sendGrim();
                  }

               }
            }
         }
      }
   }

   @EventListener(
      priority = -200
   )
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (this.mode.is(NoFall.NoFallMode.Grim)) {
         if (!nullCheck()) {
            if (this.grimJump) {
               mc.options.jumpKey.setPressed(true);
            } else {
               mc.options.jumpKey.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.jumpKey.getBoundKeyTranslationKey()).getCode()));
            }

         }
      }
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (!this.hasElytra()) {
            if (this.mode.is(NoFall.NoFallMode.Packet)) {
               Packet<?> packet = event.getPacket();
               if (packet instanceof PlayerMoveC2SPacket) {
                  PlayerMoveC2SPacket packet2 = (PlayerMoveC2SPacket)packet;
                  if (mc.player.fallDistance >= (float)this.getDistance.getValue() && !BowBomb.send) {
                     ((IPlayerMoveC2SPacket)packet2).setOnGround(true);
                  }
               }

            }
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.mode.is(NoFall.NoFallMode.Grim)) {
         if (!nullCheck()) {
            if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
               if (this.grimS08Check) {
                  this.grimReceiveS08 = true;
               }

            }
         }
      }
   }

   private boolean hasElytra() {
      for(ItemStack is : mc.player.getArmorItems()) {
         if (is.getItem() == Items.ELYTRA) {
            return true;
         }
      }

      return false;
   }

   private void resetGrim() {
      this.grimCheck = false;
      this.grimS08Check = false;
      this.grimJump = false;
      this.grimReceiveS08 = false;
      this.grimJumpSuccess = false;
      this.grimTicks = 0;
   }

   private void sendGrim() {
      try {
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
      } catch (Throwable var2) {
      }

   }

   private boolean isJumpPhysicallyDown() {
      return InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.jumpKey.getBoundKeyTranslationKey()).getCode());
   }

   public static enum NoFallMode {
      Packet,
      Grim;

      // $FF: synthetic method
      private static NoFallMode[] $values() {
         return new NoFallMode[]{Packet, Grim};
      }
   }
}
