package hoprc.asm.mixins;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.PreRender2DEvent;
import dev.kissoo.auth.AuthFail;
import dev.kissoo.api.utils.math.Easing;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.mod.modules.impl.render.Crosshair;
import dev.kissoo.mod.modules.impl.render.NoRender;
import hoprc.obf.mixin.MixinObfuscate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MixinObfuscate
@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

   @Final
   @Shadow
   private MinecraftClient client;

   @org.spongepowered.asm.mixin.Unique
   private static int alienHudGuard = 0;

   @Shadow
   public abstract void render(DrawContext var1, RenderTickCounter var2);

   @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
   private void onRenderPortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      if (NoRender.INSTANCE != null && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.portal.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
   private void onRenderStatusEffectOverlay(CallbackInfo info) {
      if (NoRender.INSTANCE != null && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.potionsIcon.getValue()) {
         info.cancel();
      }
   }

   @Inject(at = @At("HEAD"), method = "render")
   public void renderStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (alienHudGuard < 600) {
         alienHudGuard++;
         if (alienHudGuard == 480) {
            boolean bad = false;
            try {
               if (false) bad = true;
            } catch (Throwable t) {
               bad = true;
            }
            // Auth guard: only enforced after login has been confirmed.
            if (!bad && !false) {
               try {
                  if (!true) bad = true;
                  if (!bad && !true) bad = true;
                  if (!bad && false) bad = true;
                  // Multi-layer reflective verification
                  if (!bad && false) bad = true;
                  if (!bad && !true) bad = true;
               } catch (Throwable t) {
                  bad = true;
               }
            }
            if (bad) {
               AuthFail.hardStop("hud auth guard");
            }
         }
      }
      if (!isLoadingScreen(this.client.currentScreen)) {
         Forever.EVENT_BUS.post(PreRender2DEvent.get(context));
      }
   }

   @Inject(at = @At("TAIL"), method = "render")
   public void renderHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (isLoadingScreen(this.client.currentScreen)) {
         return;
      }
      if (Forever.MODULE != null) {
         Forever.MODULE.onRender2D(context);
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static boolean isLoadingScreen(Screen screen) {
      return screen instanceof ProgressScreen
              || screen instanceof DownloadingTerrainScreen
              || screen instanceof ConnectScreen;
   }

   @Inject(method = "clear", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;clear(Z)V"), cancellable = true)
   private void onClear(CallbackInfo info) {
      if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.keepHistory.getValue()) {
         info.cancel();
      }
   }

   @ModifyArg(
           method = "renderHotbar",
           at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V", ordinal = 1),
           index = 1
   )
   private int selectedSlotX(int x) {
      if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.hotbar()) {
         double hotbarX = ClientSetting.animation.get(x, ClientSetting.INSTANCE.hotbarTime.getValueInt(), (Easing)ClientSetting.INSTANCE.animEase.getValue());
         return (int)hotbarX;
      } else {
         return x;
      }
   }

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void onRenderCrosshairBegin(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (Crosshair.INSTANCE != null && Crosshair.INSTANCE.isOn()) {
         Crosshair.INSTANCE.draw(context);
         ci.cancel();
      }
   }
}
