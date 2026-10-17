package dev.Lumi.asm.mixins;

import dev.Lumi.api.utils.render.Render2DUtil;
import java.awt.Color;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4264;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_4264.class})
public abstract class MixinPressableWidget {
   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   protected void renderWidgetHook(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      class_4264 self = (class_4264)this;
      int x = self.method_46426();
      int y = self.method_46427();
      int w = self.method_25368();
      int h = self.method_25364();
      boolean hovered = self.method_49606();
      int acrylicBg = hovered ? 905969663 : 419430399;
      int acrylicBorder = hovered ? 1627389951 : 905969663;
      Render2DUtil.drawRoundedRectWithOutline(context.method_51448(), (float)x, (float)y, (float)w, (float)h, 4.0F, new Color(acrylicBg, true), new Color(acrylicBorder, true));
      class_2561 message = self.method_25369();
      if (message != null) {
         String text = message.getString();
         if (!text.isEmpty()) {
            int textWidth = class_310.method_1551().field_1772.method_1727(text);
            int textColor = hovered ? -1 : -1325400065;
            context.method_51433(class_310.method_1551().field_1772, text, x + (w - textWidth) / 2, y + (h - 8) / 2, textColor, false);
         }
      }

   }
}
