package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.EntityVelocityUpdateEvent;
import dev.kissoo.api.events.impl.GameLeftEvent;
import dev.kissoo.api.events.impl.InventoryS2CPacketEvent;
import dev.kissoo.api.events.impl.S2CCloseScreenEvent;
import dev.kissoo.api.events.impl.SendMessageEvent;
import dev.kissoo.api.events.impl.ServerChangePositionEvent;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.core.impl.RotationManager;
import dev.kissoo.mod.commands.impl.SpearFixCommand;
import dev.kissoo.mod.modules.impl.exploit.AntiPacket;
import hoprc.obf.mixin.MixinObfuscate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.EnterReconfigurationS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MixinObfuscate
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler extends ClientCommonNetworkHandler {
   @Shadow
   private ClientWorld world;
   @Unique
   private boolean alien$worldNotNull;
   @Unique
   private boolean ignore;

   protected MixinClientPlayNetworkHandler(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
      super(client, connection, connectionState);
   }

   @Inject(
      method = "onEnterReconfiguration",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
         shift = Shift.AFTER
      )
   )
   private void onEnterReconfiguration(EnterReconfigurationS2CPacket packet, CallbackInfo info) {
      Forever.EVENT_BUS.post(GameLeftEvent.INSTANCE);
   }

   @Inject(method = "onGameJoin", at = @At("HEAD"))
   private void onGameJoinHead(GameJoinS2CPacket packet, CallbackInfo info) {
      this.alien$worldNotNull = this.world != null;

      // Auth guard removed — public build
   }

   @Inject(method = "onGameJoin", at = @At("TAIL"))
   private void onGameJoinTail(GameJoinS2CPacket packet, CallbackInfo info) {
      if (this.alien$worldNotNull) {
         Forever.EVENT_BUS.post(GameLeftEvent.INSTANCE);
      }
   }

   @Shadow
   public abstract void sendChatMessage(String var1);

   @Inject(
      method = "onInventory",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
         shift = Shift.AFTER
      ),
      cancellable = true
   )
   public void onInventoryS2CPacket(InventoryS2CPacket packet, CallbackInfo ci) {
      InventoryS2CPacketEvent event = InventoryS2CPacketEvent.get(packet);
      Forever.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
   private void onSendChatMessage(String message, CallbackInfo ci) {
      if (!this.ignore) {
         if (message.equals(".spearfix") || message.startsWith(".spearfix ")) {
            String[] parts = message.trim().split("\\s+");
            String[] parameters = new String[Math.max(0, parts.length - 1)];
            if (parameters.length > 0) {
               System.arraycopy(parts, 1, parameters, 0, parameters.length);
            }
            if (!SpearFixCommand.handle(parameters, CommandManager::sendMessage)) {
               CommandManager.sendMessage("§4Parameter error §r.spearfix §7noslow <true|false>");
            }
            ci.cancel();
         } else if (message.startsWith(Forever.getPrefix()) && Forever.COMMAND != null) {
            Forever.COMMAND.command(message.split(" "));
            ci.cancel();
         } else {
            SendMessageEvent event = SendMessageEvent.get(message);
            Forever.EVENT_BUS.post(event);
            if (event.isCancelled()) {
               ci.cancel();
            } else if (!event.message.equals(event.defaultMessage)) {
               this.ignore = true;
               this.sendChatMessage(event.message);
               this.ignore = false;
               ci.cancel();
            }
         }
      }
   }

   @Inject(
      method = "onCloseScreen",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
         shift = Shift.AFTER
      ),
      cancellable = true
   )
   public void onCloseScreen(CloseScreenS2CPacket packet, CallbackInfo ci) {
      S2CCloseScreenEvent event = S2CCloseScreenEvent.get();
      Forever.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Redirect(method = "onEntityVelocityUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setVelocityClient(DDD)V"), require = 0)
   private void velocityHook(Entity instance, double x, double y, double z) {
      EntityVelocityUpdateEvent event = EntityVelocityUpdateEvent.get(instance, x, y, z, false);
      Forever.EVENT_BUS.post(event);
      if (!event.isCancelled()) {
         instance.setVelocityClient(event.getX(), event.getY(), event.getZ());
      }
   }

   @Redirect(
      method = "onExplosion",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;"),
      require = 0
   )
   private Vec3d velocityHook2(Vec3d instance, double x, double y, double z) {
      EntityVelocityUpdateEvent event = EntityVelocityUpdateEvent.get(this.client.player, x, y, z, true);
      Forever.EVENT_BUS.post(event);
      return !event.isCancelled() ? instance.add(event.getX(), event.getY(), event.getZ()) : instance;
   }

   @Inject(
      method = "onPlayerPositionLook",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
         shift = Shift.AFTER
      ),
      cancellable = true
   )
   public void onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
      boolean noRotate = AntiPacket.INSTANCE != null
              && Forever.SERVER != null
              && Forever.ROTATION != null
              && this.client.player != null
              && AntiPacket.INSTANCE.isOn()
              && AntiPacket.INSTANCE.s2CRotate.getValue()
              && Forever.SERVER.playerNull.passedS(0.25);
      if (noRotate) {
         ci.cancel();
         NetworkThreadUtils.forceMainThread(packet, (ClientPlayPacketListener)ClientPlayNetworkHandler.class.cast(this), this.client);
         PlayerEntity playerEntity = this.client.player;
         Vec3d vec3d = playerEntity.getVelocity();
         boolean bl = packet.getFlags().contains(PositionFlag.X);
         boolean bl2 = packet.getFlags().contains(PositionFlag.Y);
         boolean bl3 = packet.getFlags().contains(PositionFlag.Z);
         double d;
         double e;
         if (bl) {
            d = vec3d.getX();
            e = playerEntity.getX() + packet.getX();
            playerEntity.lastRenderX = playerEntity.lastRenderX + packet.getX();
            playerEntity.prevX = playerEntity.prevX + packet.getX();
         } else {
            d = 0.0;
            e = packet.getX();
            playerEntity.lastRenderX = e;
            playerEntity.prevX = e;
         }

         double f;
         double g;
         if (bl2) {
            f = vec3d.getY();
            g = playerEntity.getY() + packet.getY();
            playerEntity.lastRenderY = playerEntity.lastRenderY + packet.getY();
            playerEntity.prevY = playerEntity.prevY + packet.getY();
         } else {
            f = 0.0;
            g = packet.getY();
            playerEntity.lastRenderY = g;
            playerEntity.prevY = g;
         }

         double h;
         double i;
         if (bl3) {
            h = vec3d.getZ();
            i = playerEntity.getZ() + packet.getZ();
            playerEntity.lastRenderZ = playerEntity.lastRenderZ + packet.getZ();
            playerEntity.prevZ = playerEntity.prevZ + packet.getZ();
         } else {
            h = 0.0;
            i = packet.getZ();
            playerEntity.lastRenderZ = i;
            playerEntity.prevZ = i;
         }

         playerEntity.setPosition(e, g, i);
         playerEntity.setVelocity(d, f, h);
         if (AntiPacket.INSTANCE.applyYaw.getValue()) {
            float yaw = packet.getYaw();
            float pitch = packet.getPitch();
            if (packet.getFlags().contains(PositionFlag.X_ROT)) {
               pitch += Forever.ROTATION.getLastPitch();
            }

            if (packet.getFlags().contains(PositionFlag.Y_ROT)) {
               yaw += Forever.ROTATION.getLastYaw();
            }

            this.connection.send(new TeleportConfirmC2SPacket(packet.getTeleportId()));
            this.connection.send(new Full(
               playerEntity.getX(),
               playerEntity.getY(),
               playerEntity.getZ(),
               RotationManager.sanitizeYaw(yaw, playerEntity.getYaw()),
               RotationManager.sanitizePitch(pitch, playerEntity.getPitch()),
               false
            ));
         } else {
            float yaw = RotationManager.sanitizeYaw(Forever.ROTATION.getLastYaw(), playerEntity.getYaw());
            float pitch = RotationManager.sanitizePitch(Forever.ROTATION.getLastPitch(), playerEntity.getPitch());
            this.connection.send(new TeleportConfirmC2SPacket(packet.getTeleportId()));
            this.connection
               .send(
                  new Full(
                     playerEntity.getX(),
                     playerEntity.getY(),
                     playerEntity.getZ(),
                     yaw,
                     pitch,
                     false
                  )
               );
         }

         Forever.EVENT_BUS.post(ServerChangePositionEvent.INSTANCE);
      }
   }
}
