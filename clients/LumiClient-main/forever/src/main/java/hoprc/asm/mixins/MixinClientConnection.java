package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.TimeoutException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.handler.PacketEncoderException;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {
   @Inject(at = @At("HEAD"), method = "handlePacket", cancellable = true)
   private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
      if (!forever$shouldPostGameplayPacketEvents()) {
         return;
      }

      PacketEvent.Receive event = new PacketEvent.Receive(packet);
      Forever.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
   private void onSendPacketPre(Packet<?> packet, CallbackInfo info) {
      if (!forever$shouldPostGameplayPacketEvents()) {
         return;
      }

      PacketEvent.Send event = new PacketEvent.Send(packet);
      Forever.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("RETURN"))
   private void onSendPacketPost(Packet<?> packet, CallbackInfo info) {
      if (!forever$shouldPostGameplayPacketEvents()) {
         return;
      }

      PacketEvent.Sent event = new PacketEvent.Sent(packet);
      Forever.EVENT_BUS.post(event);
   }

   private static boolean forever$shouldPostGameplayPacketEvents() {
      if (!Forever.loaded || Forever.MODULE == null) {
         return false;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      return client != null && client.player != null && client.world != null;
   }

   @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
   private void exceptionCaught(ChannelHandlerContext context, Throwable throwable, CallbackInfo ci) {
      if (!(throwable instanceof TimeoutException)
              && !(throwable instanceof PacketEncoderException)
              && ClientSetting.INSTANCE != null
              && ClientSetting.INSTANCE.caughtException.getValue()) {
         if (ClientSetting.INSTANCE.log.getValue()) {
            CommandManager.sendMessage("§4Caught exception §7" + throwable.getMessage());
         }

         ci.cancel();
      }
   }
}
