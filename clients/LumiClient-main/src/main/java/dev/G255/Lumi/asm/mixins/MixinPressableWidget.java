package dev.G255.Lumi.asm.mixins;

import dev.G255.Lumi.api.utils.render.Render2DUtil;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PressableWidget.class})
public abstract class MixinPressableWidget {
    @Inject(
        method = {"renderWidget"},
        at = {@At("HEAD")},
        cancellable = true
    )
    protected void renderWidgetHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();
        PressableWidget self = (PressableWidget) (Object) this;
        int x = self.getX();
        int y = self.getY();
        int w = self.getWidth();
        int h = self.getHeight();
        boolean hovered = self.isHovered();
        int acrylicBg = hovered ? 905969663 : 419430399;
        int acrylicBorder = hovered ? 1627389951 : 905969663;
        Render2DUtil.drawRoundedRectWithOutline(context.getMatrices(), (float) x, (float) y, (float) w, (float) h, 4.0F, new Color(acrylicBg, true), new Color(acrylicBorder, true));
        Text message = self.getMessage();
        if (message != null) {
            String text = message.getString();
            if (!text.isEmpty()) {
                int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
                int textColor = hovered ? -1 : -1325400065;
                context.drawText(MinecraftClient.getInstance().textRenderer, text, x + (w - textWidth) / 2, y + (h - 8) / 2, textColor, false);
            }
        }
    }
}