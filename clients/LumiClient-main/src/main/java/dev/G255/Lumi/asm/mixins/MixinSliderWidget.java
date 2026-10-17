package dev.G255.Lumi.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.G255.Lumi.api.utils.render.Render2DUtil;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SliderWidget.class)
public class MixinSliderWidget {

    @Shadow
    private double value;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void onRenderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

        SliderWidget self = (SliderWidget) (Object) this;
        MatrixStack matrices = context.getMatrices();
        boolean hovered = self.isMouseOver(mouseX, mouseY);

        float x = (float) self.getX();
        float y = (float) self.getY();
        float w = (float) self.getWidth();
        float h = (float) self.getHeight();
        float radius = 6f;
        float padding = 2f;

        Color baseColor = new Color(40, 40, 40);
        float bgAlpha = hovered ? 120f : 80f;
        float borderAlpha = hovered ? 180f : 120f;

        Render2DUtil.drawRoundedRectWithOutline(matrices, x, y, w, h, radius,
                new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int) bgAlpha),
                new Color(255, 255, 255, (int) borderAlpha));

        double fraction = Math.max(0.0, Math.min(1.0, this.value));
        float fillWidth = (w - padding * 2) * (float) fraction;
        if (fillWidth > 0.5f) {
            float fillAlpha = hovered ? 160f : 110f;
            Render2DUtil.drawRoundedRect(matrices, x + padding, y + padding, fillWidth, h - padding * 2, radius - 1f,
                    new Color(255, 255, 255, (int) fillAlpha));
        }

        int textColor = hovered ? 0xFFFFFF : 0xFFB0B0B0;
        String text = self.getMessage().getString();
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
        int textX = (int) (x + (w - textWidth) / 2);
        int textY = (int) (y + (h - 8) / 2);
        context.drawText(MinecraftClient.getInstance().textRenderer, text, textX, textY, textColor, false);
    }
}