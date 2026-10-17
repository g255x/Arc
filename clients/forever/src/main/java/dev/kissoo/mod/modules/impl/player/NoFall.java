package dev.kissoo.mod.modules.impl.player;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.KeyboardInputEvent;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import hoprc.asm.accessors.IPlayerMoveC2SPacket;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.exploit.BowBomb;
import dev.kissoo.mod.modules.impl.exploit.MaceSpoof;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;

public class NoFall extends Module {
   private final EnumSetting<NoFallMode> mode = this.add(new EnumSetting<>("Mode", NoFallMode.Packet));
   private final SliderSetting distance = this.add(new SliderSetting("Distance", 3.0, 0.0, 8.0, 0.1));

   private boolean jump;
   private int fallCounter;

   public NoFall() {
      super("NoFall", "Prevents fall damage.", Module.Category.Player);
      this.setChinese("没有摔落伤害");
   }

   private boolean isPausedByConditions() {
      if (mc.player == null) return false;
      if (MaceSpoof.INSTANCE != null && MaceSpoof.INSTANCE.isOn() && mc.player.getMainHandStack().getItem() == Items.MACE) {
         return true;
      }
      return false;
   }

   @Override
   public void onDisable() {
      this.jump = false;
      this.fallCounter = 0;
      super.onDisable();
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (nullCheck()) return;
      if (isPausedByConditions()) return;

      if (this.mode.is(NoFallMode.Grim)) {
         if (this.checkFalling()) {
            mc.getNetworkHandler()
                    .sendPacket(
                            new Full(
                                    mc.player.getX(),
                                    mc.player.getY() + 1.0E-9,
                                    mc.player.getZ(),
                                    mc.player.getYaw(),
                                    mc.player.getPitch(),
                                    false
                            )
                    );
            mc.player.onLanding();
         }
      }

      if (this.mode.is(NoFallMode.Exploit)) {
         if (!mc.player.isOnGround()) {
            this.fallCounter = mc.player.fallDistance > (float) this.distance.getValue()
                    ? ++this.fallCounter : 0;
         }
      }
   }

   @EventListener
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (nullCheck()) return;
      if (isPausedByConditions()) return;

      if (this.mode.is(NoFallMode.Exploit)) {
         if (this.jump) {
            mc.player.input.jumping = true;
            this.jump = false;
         }
      }
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (nullCheck()) return;
      if (isPausedByConditions()) return;

      for (ItemStack is : mc.player.getArmorItems()) {
         if (is.getItem() == Items.ELYTRA) {
            return;
         }
      }

      if (this.mode.is(NoFallMode.Packet)) {
         if (event.getPacket() instanceof PlayerMoveC2SPacket packet
                 && mc.player.fallDistance >= (float) this.distance.getValue()
                 && !BowBomb.send) {
            ((IPlayerMoveC2SPacket) packet).setOnGround(true);
         }
      }

      if (this.mode.is(NoFallMode.Exploit)) {
         if (event.getPacket() instanceof PlayerMoveC2SPacket packet) {
            if (this.jump && this.fallCounter == 0) {
               event.setCancelled(true);
               return;
            }
            if (mc.player.isOnGround() && this.fallCounter > 0 && !this.jump) {
               ((IPlayerMoveC2SPacket) packet).setY(mc.player.getY() + 10.0);
               this.jump = true;
            }
         }
      }
   }

   private boolean checkFalling() {
      return mc.player.fallDistance > mc.player.getSafeFallDistance()
              && !mc.player.isOnGround()
              && !mc.player.isFallFlying();
   }

   public enum NoFallMode {
      Packet,
      Grim,
      Exploit
   }
}