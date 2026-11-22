package dev.g255.arc.asm.mixins;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.impl.PreRender2DEvent;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.render.Crosshair;
import dev.g255.arc.mod.modules.impl.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameHud.class})
public abstract class MixinInGameHud {
   @Final
   @Shadow
   private MinecraftClient client;

   @Inject(
      method = {"renderPortalOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderPortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.portal.getValue()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"renderStatusEffectOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderStatusEffectOverlay(CallbackInfo info) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.potionsIcon.getValue()) {
         info.cancel();
      }

   }

   @Shadow
   public abstract void render(DrawContext var1, RenderTickCounter var2);

   @Inject(
      at = {@At("HEAD")},
      method = {"render"}
   )
   public void renderStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Arc.EVENT_BUS.post(PreRender2DEvent.get(context));
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"render"}
   )
   public void renderHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Arc.MODULE.onRender2D(context);
   }

   @Inject(
      method = {"clear"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/hud/ChatHud;clear(Z)V"
)},
      cancellable = true
   )
   private void onClear(CallbackInfo info) {
      if (ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.keepHistory.getValue()) {
         info.cancel();
      }

   }

   @ModifyArg(
      method = {"renderHotbar"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V",
   ordinal = 1
),
      index = 1
   )
   private int selectedSlotX(int x) {
      if (ClientSetting.INSTANCE.hotbar()) {
         double hotbarX = ClientSetting.animation.get((double)x, (long)ClientSetting.INSTANCE.hotbarTime.getValueInt(), ClientSetting.INSTANCE.animEase.getValue());
         return (int)hotbarX;
      } else {
         return x;
      }
   }

   @Inject(
      method = {"renderCrosshair"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderCrosshairBegin(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (Crosshair.INSTANCE.isOn()) {
         Crosshair.INSTANCE.draw(context);
         ci.cancel();
      }

   }
}
