package dev.g255.arc.asm.mixins;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.impl.EntityVelocityUpdateEvent;
import dev.g255.arc.api.events.impl.GameLeftEvent;
import dev.g255.arc.api.events.impl.InventoryS2CPacketEvent;
import dev.g255.arc.api.events.impl.S2CCloseScreenEvent;
import dev.g255.arc.api.events.impl.SendMessageEvent;
import dev.g255.arc.api.events.impl.ServerChangePositionEvent;
import dev.g255.arc.mod.modules.impl.exploit.AntiPacket;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.EnterReconfigurationS2CPacket;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinClientPlayNetworkHandler extends ClientCommonNetworkHandler {
   @Shadow
   private ClientWorld world;
   @Unique
   private boolean arc$worldNotNull;
   @Unique
   private boolean ignore;

   protected MixinClientPlayNetworkHandler(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
      super(client, connection, connectionState);
   }

   @Inject(
      method = {"onEnterReconfiguration"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
   shift = Shift.AFTER
)}
   )
   private void onEnterReconfiguration(EnterReconfigurationS2CPacket packet, CallbackInfo info) {
      Arc.EVENT_BUS.post(GameLeftEvent.INSTANCE);
   }

   @Inject(
      method = {"onGameJoin"},
      at = {@At("HEAD")}
   )
   private void onGameJoinHead(GameJoinS2CPacket packet, CallbackInfo info) {
      this.arc$worldNotNull = this.world != null;
   }

   @Inject(
      method = {"onGameJoin"},
      at = {@At("TAIL")}
   )
   private void onGameJoinTail(GameJoinS2CPacket packet, CallbackInfo info) {
      if (this.arc$worldNotNull) {
         Arc.EVENT_BUS.post(GameLeftEvent.INSTANCE);
      }

   }

   @Shadow
   public abstract void sendChatMessage(String var1);

   @Inject(
      method = {"onInventory"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
   shift = Shift.AFTER
)},
      cancellable = true
   )
   public void onInventoryS2CPacket(InventoryS2CPacket packet, CallbackInfo ci) {
      InventoryS2CPacketEvent event = InventoryS2CPacketEvent.get(packet);
      Arc.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"sendChatMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendChatMessage(String message, CallbackInfo ci) {
      if (!this.ignore) {
         if (message.startsWith(Arc.getPrefix())) {
            Arc.COMMAND.command(message.split(" "));
            ci.cancel();
         } else {
            SendMessageEvent event = SendMessageEvent.get(message);
            Arc.EVENT_BUS.post(event);
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
      method = {"onCloseScreen"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
   shift = Shift.AFTER
)},
      cancellable = true
   )
   public void onCloseScreen(CloseScreenS2CPacket packet, CallbackInfo ci) {
      S2CCloseScreenEvent event = S2CCloseScreenEvent.get();
      Arc.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Redirect(
      method = {"onEntityVelocityUpdate"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/Entity;setVelocityClient(DDD)V"
),
      require = 0
   )
   private void velocityHook(Entity instance, double x, double y, double z) {
      EntityVelocityUpdateEvent event = EntityVelocityUpdateEvent.get(instance, x, y, z, false);
      Arc.EVENT_BUS.post(event);
      if (!event.isCancelled()) {
         instance.setVelocityClient(event.getX(), event.getY(), event.getZ());
      }

   }

   @Redirect(
      method = {"onExplosion"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;"
),
      require = 0
   )
   private Vec3d velocityHook2(Vec3d instance, double x, double y, double z) {
      EntityVelocityUpdateEvent event = EntityVelocityUpdateEvent.get(this.client.player, x, y, z, true);
      Arc.EVENT_BUS.post(event);
      return !event.isCancelled() ? instance.add(event.getX(), event.getY(), event.getZ()) : instance;
   }

   @Inject(
      method = {"onPlayerPositionLook"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
   shift = Shift.AFTER
)},
      cancellable = true
   )
   public void onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
      boolean noRotate = AntiPacket.INSTANCE.isOn() && AntiPacket.INSTANCE.s2CRotate.getValue() && Arc.SERVER.playerNull.passedS((double)0.25F);
      if (noRotate) {
         ci.cancel();
         NetworkThreadUtils.forceMainThread(packet, (ClientPlayPacketListener)ClientPlayNetworkHandler.class.cast(this), this.client);
         ClientPlayerEntity playerEntity = this.client.player;
         Vec3d vec3d = playerEntity.getVelocity();
         boolean bl2 = packet.getFlags().contains(PositionFlag.X);
         boolean bl22 = packet.getFlags().contains(PositionFlag.Y);
         boolean bl3 = packet.getFlags().contains(PositionFlag.Z);
         double e;
         double d;
         if (bl2) {
            d = vec3d.getX();
            e = playerEntity.getX() + packet.getX();
            playerEntity.lastRenderX += packet.getX();
            playerEntity.prevX += packet.getX();
         } else {
            d = (double)0.0F;
            playerEntity.lastRenderX = e = packet.getX();
            playerEntity.prevX = e;
         }

         double g;
         double f;
         if (bl22) {
            f = vec3d.getY();
            g = playerEntity.getY() + packet.getY();
            playerEntity.lastRenderY += packet.getY();
            playerEntity.prevY += packet.getY();
         } else {
            f = (double)0.0F;
            playerEntity.lastRenderY = g = packet.getY();
            playerEntity.prevY = g;
         }

         double i;
         double h;
         if (bl3) {
            h = vec3d.getZ();
            i = playerEntity.getZ() + packet.getZ();
            playerEntity.lastRenderZ += packet.getZ();
            playerEntity.prevZ += packet.getZ();
         } else {
            h = (double)0.0F;
            playerEntity.lastRenderZ = i = packet.getZ();
            playerEntity.prevZ = i;
         }

         playerEntity.setPosition(e, g, i);
         playerEntity.setVelocity(d, f, h);
         if (AntiPacket.INSTANCE.applyYaw.getValue()) {
            float yaw = packet.getYaw();
            float pitch = packet.getPitch();
            if (packet.getFlags().contains(PositionFlag.X_ROT)) {
               pitch += Arc.ROTATION.getLastPitch();
            }

            if (packet.getFlags().contains(PositionFlag.Y_ROT)) {
               yaw += Arc.ROTATION.getLastYaw();
            }

            this.connection.send(new TeleportConfirmC2SPacket(packet.getTeleportId()));
            this.connection.send(new PlayerMoveC2SPacket.Full(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), yaw, pitch, false));
         } else {
            this.connection.send(new TeleportConfirmC2SPacket(packet.getTeleportId()));
            this.connection.send(new PlayerMoveC2SPacket.Full(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), Arc.ROTATION.getLastYaw(), Arc.ROTATION.getLastPitch(), false));
         }

         Arc.EVENT_BUS.post(ServerChangePositionEvent.INSTANCE);
      }

   }
}
