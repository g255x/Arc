package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.JumpEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class HighJump extends Module {
   private final SliderSetting multiplier = this.add(new SliderSetting("Multiplier", (double)1.0F, (double)0.0F, (double)100.0F));
   private final BooleanSetting damageJump = this.add(new BooleanSetting("DamageJump", true));
   private final SliderSetting damageJumpMultiplier;
   private boolean skipNextJump = false;

   public HighJump() {
      super("HighJump", "Makes you jump higher than normal", Module.Category.Movement);
      BooleanSetting var10008 = this.damageJump;
      Objects.requireNonNull(var10008);
      this.damageJumpMultiplier = this.add(new SliderSetting("DamageJumpMultiplier", (double)1.0F, (double)0.0F, (double)100.0F, var10008::getValue));
      this.setChinese("高跳");
   }

   @EventListener
   public void onJump(JumpEvent event) {
      if (nullCheck() && !event.isPre()) {
         if (this.skipNextJump) {
            this.skipNextJump = false;
         } else {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, velocity.y * this.multiplier.getValue(), velocity.z);
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.damageJump.getValue() && !nullCheck()) {
         Packet var3 = event.getPacket();
         if (var3 instanceof EntityStatusS2CPacket) {
            EntityStatusS2CPacket packet = (EntityStatusS2CPacket)var3;
            if (packet.getStatus() == 2 && packet.getEntity(mc.world) == mc.player && mc.player.isOnGround()) {
               this.skipNextJump = true;
               mc.player.jump();
               Vec3d velocity = mc.player.getVelocity();
               mc.player.setVelocity(velocity.x, velocity.y * this.damageJumpMultiplier.getValue(), velocity.z);
            }
         }

      }
   }
}
