package hoprc.asm.mixins;

import dev.kissoo.mod.gui.background.MainMenuModePreference;
import dev.kissoo.mod.gui.background.PhobosBackground;
import hoprc.asm.accessors.IClickableWidget;
import hoprc.asm.accessors.ITitleScreenFade;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public abstract class MixinPressableWidget {

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void lyrae$renderPhobosTitleButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null
                || client.world != null
                || client.currentScreen == null
                || !MainMenuModePreference.isPhobosSelected()) {
            return;
        }

        ci.cancel();
        ClickableWidget self = (ClickableWidget) (Object) this;
        IClickableWidget accessor = (IClickableWidget) (Object) this;
        float alpha = accessor.getAlpha() * lyrae$getScreenFadeAlpha(client.currentScreen);
        PhobosBackground.getInstance().renderButton(
                context,
                self.getX(),
                self.getY(),
                self.getWidth(),
                self.getHeight(),
                alpha
        );

        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Text message = self.getMessage();
        if (message != null && !message.getString().isEmpty() && !lyrae$isIconButton(self)) {
            int alphaByte = MathHelper.ceil(alpha * 255.0F);
            int textColor = accessor.isActive() ? 0xFFFFFF : 0xA0A0A0;
            int x = self.getX() + (self.getWidth() - client.textRenderer.getWidth(message)) / 2;
            int y = self.getY() + (self.getHeight() - 8) / 2;
            int shadowColor = (Math.min(alphaByte, 190) << 24);
            context.drawText(client.textRenderer, message, x + 1, y + 1, shadowColor, false);
            context.drawText(client.textRenderer, message, x - 1, y + 1, shadowColor, false);
            context.drawTextWithShadow(client.textRenderer, message, x, y, textColor | alphaByte << 24);
        }
    }

    private boolean lyrae$isIconButton(ClickableWidget widget) {
        return widget.getWidth() <= 24 && widget.getHeight() <= 24;
    }

    private float lyrae$getScreenFadeAlpha(net.minecraft.client.gui.screen.Screen screen) {
        if (!(screen instanceof TitleScreen titleScreen)) {
            return 1.0F;
        }

        return lyrae$getTitleFadeAlpha(titleScreen);
    }

    private float lyrae$getTitleFadeAlpha(TitleScreen screen) {
        ITitleScreenFade fade = (ITitleScreenFade) screen;
        if (!fade.forever$getDoBackgroundFade()) {
            return 1.0F;
        }

        float progress = (float) (Util.getMeasuringTimeMs() - fade.forever$getBackgroundFadeStart()) / 2000.0F;
        if (progress > 1.0F) {
            return 1.0F;
        }
        progress = MathHelper.clamp(progress, 0.0F, 1.0F);
        return MathHelper.clampedMap(progress, 0.5F, 1.0F, 0.0F, 1.0F);
    }
}
