package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.ServerConnectBeginEvent;
import dev.kissoo.proxy.ProxyManager;
import dev.kissoo.proxy.ZenithChoiceScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class MixinConnectScreen {

   // ── Intercept connections to the proxied server and show the choice screen ─
   // Always shows ZenithChoiceScreen for the proxied target so the user can
   // choose between the proxy queue and a direct connection.
   // Exception: if the user already picked "直接连接" in the choice screen,
   // consumeDirectOnce() returns true and we let this second call through.
   @Inject(
      method = "connect(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;ZLnet/minecraft/client/network/CookieStorage;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void zenithGate(
      Screen screen, MinecraftClient client, ServerAddress address, ServerInfo info, boolean quickPlay, CookieStorage cookieStorage, CallbackInfo ci
   ) {
      if (ProxyManager.isProxiedTarget(address.getAddress(), address.getPort())) {
         if (ProxyManager.consumeDirectOnce()) {
            // User chose direct connect in the choice screen — let this call through.
            return;
         }
         // Show choice screen; proxy is NOT started here.
         client.setScreen(new ZenithChoiceScreen(screen, address, info));
         ci.cancel();
      }
   }

   // ── Redirect proxied-target address to localhost when the proxy is READY ──
   // The @ModifyArg runs only when zenithGate did NOT cancel.
   // That means either: non-proxied server, or user chose direct connect.
   // For direct connect the proxy is not READY so redirectIfProxied returns
   // the original address unchanged.
   @ModifyArg(
      method = "connect(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;ZLnet/minecraft/client/network/CookieStorage;)V",
      at = @At(value = "INVOKE",
               target = "Lnet/minecraft/client/gui/screen/multiplayer/ConnectScreen;connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V"),
      index = 1
   )
   private static ServerAddress redirectToProxy(ServerAddress original) {
      String host = original.getAddress();
      int    port = original.getPort();
      String redirected = ProxyManager.redirectIfProxied(host, port);
      if (redirected.equals(host + ":" + port)) return original;
      return ServerAddress.parse(redirected);
   }

   @Inject(
      method = "connect(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;ZLnet/minecraft/client/network/CookieStorage;)V",
      at = @At("HEAD")
   )
   private static void tryConnectEvent(
      Screen screen, MinecraftClient client, ServerAddress address, ServerInfo info, boolean quickPlay, CookieStorage cookieStorage, CallbackInfo ci
   ) {
      Forever.EVENT_BUS.post(ServerConnectBeginEvent.get(address, info));
   }

   @Inject(
      method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V",
      at = @At("HEAD")
   )
   private void tryConnectEvent(MinecraftClient client, ServerAddress address, ServerInfo info, CookieStorage cookieStorage, CallbackInfo ci) {
      Forever.EVENT_BUS.post(ServerConnectBeginEvent.get(address, info));
   }
}

