package dev.g255.arc.asm.mixins;

import com.mojang.authlib.GameProfile;
import dev.g255.arc.Arc;
import dev.g255.arc.api.events.Event;
import dev.g255.arc.api.events.impl.MoveEvent;
import dev.g255.arc.api.events.impl.MovedEvent;
import dev.g255.arc.api.events.impl.SendMovementPacketsEvent;
import dev.g255.arc.api.events.impl.TickEvent;
import dev.g255.arc.api.events.impl.TickMovementEvent;
import dev.g255.arc.asm.accessors.IClientPlayerEntity;
import dev.g255.arc.core.impl.CommandManager;
import dev.g255.arc.core.impl.RotationManager;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.exploit.PacketControl;
import dev.g255.arc.mod.modules.impl.movement.NoSlow;
import dev.g255.arc.mod.modules.impl.movement.Velocity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerEntity.class})
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
   @Shadow
   public Input input;
   @Final
   @Shadow
   protected MinecraftClient client;
   @Unique
   private float preYaw;
   @Unique
   private float prePitch;
   @Unique
   private boolean rotation = false;
   @Shadow
   private double lastX;
   @Shadow
   private double lastBaseY;
   @Shadow
   private double lastZ;
   @Shadow
   private int ticksSinceLastPositionPacketSent;
   @Shadow
   private float lastYaw;
   @Shadow
   private float lastPitch;

   public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @Inject(
      method = {"pushOutOfBlocks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPushOutOfBlocksHook(double x, double d, CallbackInfo info) {
      if (Velocity.INSTANCE.isOn() && Velocity.INSTANCE.blockPush.getValue()) {
         info.cancel();
      }

   }

   @Redirect(
      method = {"tickMovement"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
),
      require = 0
   )
   private boolean tickMovementHook(ClientPlayerEntity player) {
      return NoSlow.INSTANCE.noSlow() ? false : player.isUsingItem();
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"tickNausea"},
      cancellable = true
   )
   private void updateNausea(CallbackInfo ci) {
      if (ClientSetting.INSTANCE.portalGui()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"move"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
)},
      cancellable = true
   )
   public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      MoveEvent event = MoveEvent.get(movement.x, movement.y, movement.z);
      Arc.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      } else if (event.modify) {
         ci.cancel();
         super.move(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
         Arc.EVENT_BUS.post(MovedEvent.INSTANCE);
      }

   }

   @Inject(
      method = {"move"},
      at = {@At("TAIL")}
   )
   public void onMoveReturnHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      Arc.EVENT_BUS.post(MovedEvent.INSTANCE);
   }

   @Shadow
   public abstract float getPitch(float var1);

   @Shadow
   public abstract float getYaw(float var1);

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void tickHead(CallbackInfo ci) {
      try {
         Arc.EVENT_BUS.post(TickEvent.get(Event.Stage.Pre));
      } catch (Exception e) {
         e.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (ClientPlayerEntity.tick() [HEAD]) Message: [" + e.getMessage() + "]");
         }
      }

   }

   @Inject(
      method = {"tick"},
      at = {@At("RETURN")}
   )
   private void tickReturn(CallbackInfo ci) {
      try {
         Arc.EVENT_BUS.post(TickEvent.get(Event.Stage.Post));
      } catch (Exception e) {
         e.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (ClientPlayerEntity.tick() [RETURN]) Message: [" + e.getMessage() + "]");
         }
      }

   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("HEAD")}
   )
   private void onSendMovementPacketsHead(CallbackInfo info) {
      this.rotation();
      if (PacketControl.INSTANCE.isOn() && PacketControl.INSTANCE.positionSync.getValue() && this.ticksSinceLastPositionPacketSent >= PacketControl.INSTANCE.positionDelay.getValueInt() - 1) {
         ((IClientPlayerEntity)this).setTicksSinceLastPositionPacketSent(50);
      }

      if (RotationManager.snapBack) {
         ((IClientPlayerEntity)this).setTicksSinceLastPositionPacketSent(50);
         ((IClientPlayerEntity)this).setLastYaw(999.0F);
         RotationManager.snapBack = false;
      } else {
         if (AntiCheat.INSTANCE.fullPackets.getValue()) {
            double d = this.getX() - this.lastX;
            double e = this.getY() - this.lastBaseY;
            double f = this.getZ() - this.lastZ;
            double g = (double)(this.getYaw() - this.lastYaw);
            double h = (double)(this.getPitch() - this.lastPitch);
            boolean bl3 = g != (double)0.0F || h != (double)0.0F;
            if (AntiCheat.INSTANCE.force.getValue() || !(MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4)) && this.ticksSinceLastPositionPacketSent >= 19 || bl3) {
               ((IClientPlayerEntity)this).setTicksSinceLastPositionPacketSent(50);
               ((IClientPlayerEntity)this).setLastYaw(999.0F);
            }
         }

      }
   }

   @Inject(
      method = {"tick"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
   ordinal = 0
)}
   )
   private void onTickHasVehicleBeforeSendPackets(CallbackInfo info) {
      this.rotation();
   }

   @Unique
   private void rotation() {
      this.rotation = true;
      this.preYaw = this.getYaw();
      this.prePitch = this.getPitch();
      SendMovementPacketsEvent event = SendMovementPacketsEvent.get(this.getYaw(), this.getPitch());
      Arc.EVENT_BUS.post(event);
      Arc.ROTATION.rotationYaw = event.getYaw();
      Arc.ROTATION.rotationPitch = event.getPitch();
      this.setYaw(event.getYaw());
      this.setPitch(event.getPitch());
   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("TAIL")}
   )
   private void onSendMovementPacketsTail(CallbackInfo info) {
      if (this.rotation) {
         this.setYaw(this.preYaw);
         this.setPitch(this.prePitch);
         this.rotation = false;
      }

   }

   @Inject(
      method = {"tick"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
   ordinal = 1,
   shift = Shift.AFTER
)}
   )
   private void onTickHasVehicleAfterSendPackets(CallbackInfo info) {
      if (this.rotation) {
         this.setYaw(this.preYaw);
         this.setPitch(this.prePitch);
         this.rotation = false;
      }

   }

   @Inject(
      method = {"tickMovement"},
      at = {@At("HEAD")}
   )
   private void tickMovement(CallbackInfo ci) {
      Arc.EVENT_BUS.post(TickMovementEvent.INSTANCE);
   }
}
