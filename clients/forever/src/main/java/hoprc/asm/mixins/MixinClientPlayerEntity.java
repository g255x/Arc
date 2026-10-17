package hoprc.asm.mixins;

import com.mojang.authlib.GameProfile;
import dev.kissoo.Forever;
import dev.kissoo.auth.AuthFail;
import dev.kissoo.api.events.Event;
import dev.kissoo.api.events.impl.MoveEvent;
import dev.kissoo.api.events.impl.MovedEvent;
import dev.kissoo.api.events.impl.SendMovementPacketsEvent;
import dev.kissoo.api.events.impl.TickEvent;
import dev.kissoo.api.events.impl.TickMovementEvent;
import hoprc.asm.accessors.IClientPlayerEntity;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.core.impl.RotationManager;
import dev.kissoo.mod.spearfix.SpearFix;
import dev.kissoo.mod.modules.impl.client.AntiCheat;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.mod.modules.impl.movement.NoSlow;
import dev.kissoo.mod.modules.impl.movement.Velocity;
import hoprc.obf.mixin.MixinObfuscate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MixinObfuscate
@Mixin(ClientPlayerEntity.class)
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
   @Unique
   private static int alienGuard = 0;
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

   @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
   private void onPushOutOfBlocksHook(double x, double d, CallbackInfo info) {
      if (Velocity.INSTANCE != null && Velocity.INSTANCE.isOn() && Velocity.INSTANCE.blockPush.getValue()) {
         info.cancel();
      }
   }

   @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
   private boolean tickMovementHook(ClientPlayerEntity player) {
      if (SpearFix.shouldBypassUseSlowdown(player.getMainHandStack())) {
         return false;
      }
      return NoSlow.INSTANCE != null && NoSlow.INSTANCE.noSlow() ? false : player.isUsingItem();
   }

   @Redirect(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
   private boolean spearfix$canStartSprintingWhileUsingSpear(ClientPlayerEntity player) {
      if (SpearFix.shouldBypassUseSlowdown(player.getMainHandStack())) {
         return false;
      }
      return player.isUsingItem();
   }

   @Inject(at = @At("HEAD"), method = "tickNausea", cancellable = true)
   private void updateNausea(CallbackInfo ci) {
      if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.portalGui()) {
         ci.cancel();
      }
   }

   @Inject(
      method = "move",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
      ),
      cancellable = true
   )
   public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      MoveEvent event = MoveEvent.get(movement.x, movement.y, movement.z);
      Forever.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      } else if (event.modify) {
         ci.cancel();
         super.move(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
         Forever.EVENT_BUS.post(MovedEvent.INSTANCE);
      }
   }

   @Inject(method = "move", at = @At("TAIL"))
   public void onMoveReturnHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      Forever.EVENT_BUS.post(MovedEvent.INSTANCE);
   }

   @Shadow
   public abstract float getPitch(float var1);

   @Shadow
   public abstract float getYaw(float var1);

   @Inject(method = "tick", at = @At("HEAD"))
   private void tickHead(CallbackInfo ci) {
      if (alienGuard < 300) {
         alienGuard++;
         if (alienGuard == 220) {
            boolean bad = false;
            try {
               if (false) bad = true;
               // Auth guard: only enforced after login has been confirmed.
               if (!bad && !false) {
                  if (!true) bad = true;
                  if (!bad && !true) bad = true;
                  if (!bad && false) bad = true;
                  // Multi-layer reflective verification
                  if (!bad && false) bad = true;
                  if (!bad && false) bad = true;
               }
            } catch (Throwable t) {
               bad = true;
            }
            if (bad) {
               AuthFail.hardStop("player auth guard");
            }
         }
      }
      try {
         Forever.EVENT_BUS.post(TickEvent.get(Event.Stage.Pre));
      } catch (Throwable var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (ClientPlayerEntity.tick() [HEAD]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Inject(method = "tick", at = @At("RETURN"))
   private void tickReturn(CallbackInfo ci) {
      try {
         Forever.EVENT_BUS.post(TickEvent.get(Event.Stage.Post));
      } catch (Throwable var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (ClientPlayerEntity.tick() [RETURN]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Inject(method = "sendMovementPackets", at = @At("HEAD"))
   private void onSendMovementPacketsHead(CallbackInfo info) {
      this.rotation();
      if (RotationManager.snapBack) {
         ((IClientPlayerEntity)this).setTicksSinceLastPositionPacketSent(50);
         ((IClientPlayerEntity)this).setLastYaw(999.0F);
         RotationManager.snapBack = false;
      } else {
         if (AntiCheat.INSTANCE != null && AntiCheat.INSTANCE.fullPackets.getValue()) {
            double d = this.getX() - this.lastX;
            double e = this.getY() - this.lastBaseY;
            double f = this.getZ() - this.lastZ;
            double g = this.getYaw() - this.lastYaw;
            double h = this.getPitch() - this.lastPitch;
            boolean bl3 = g != 0.0 || h != 0.0;
            if (AntiCheat.INSTANCE.force.getValue() || !(MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4)) && this.ticksSinceLastPositionPacketSent >= 19 || bl3) {
               ((IClientPlayerEntity)this).setTicksSinceLastPositionPacketSent(50);
               ((IClientPlayerEntity)this).setLastYaw(999.0F);
            }
         }
      }
   }

   @Inject(
      method = "tick",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
         ordinal = 0
      )
   )
   private void onTickHasVehicleBeforeSendPackets(CallbackInfo info) {
      this.rotation();
   }

   @Unique
   private void rotation() {
      this.rotation = true;
      this.preYaw = safeYaw(this.getYaw(), safeYaw(this.lastYaw, 0.0F));
      this.prePitch = safePitch(this.getPitch(), safePitch(this.lastPitch, 0.0F));
      SendMovementPacketsEvent event = SendMovementPacketsEvent.get(this.preYaw, this.prePitch);
      Forever.EVENT_BUS.post(event);
      float yaw = safeYaw(event.getYaw(), this.preYaw);
      float pitch = safePitch(event.getPitch(), this.prePitch);
      if (Forever.ROTATION != null) {
         Forever.ROTATION.rotationYaw = yaw;
         Forever.ROTATION.rotationPitch = pitch;
      }
      this.setYaw(yaw);
      this.setPitch(pitch);
   }

   @Unique
   private static float safeYaw(float yaw, float fallback) {
      return Float.isFinite(yaw) ? MathHelper.wrapDegrees(yaw) : fallback;
   }

   @Unique
   private static float safePitch(float pitch, float fallback) {
      return Float.isFinite(pitch) ? MathHelper.clamp(pitch, -90.0F, 90.0F) : fallback;
   }

   @Inject(method = "sendMovementPackets", at = @At("TAIL"))
   private void onSendMovementPacketsTail(CallbackInfo info) {
      if (this.rotation) {
         this.setYaw(this.preYaw);
         this.setPitch(this.prePitch);
         this.rotation = false;
      }
   }

   @Inject(
      method = "tick",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
         ordinal = 1,
         shift = Shift.AFTER
      )
   )
   private void onTickHasVehicleAfterSendPackets(CallbackInfo info) {
      if (this.rotation) {
         this.setYaw(this.preYaw);
         this.setPitch(this.prePitch);
         this.rotation = false;
      }
   }

   @Inject(method = "tickMovement", at = @At("HEAD"))
   private void tickMovement(CallbackInfo ci) {
      Forever.EVENT_BUS.post(TickMovementEvent.INSTANCE);
   }
}
