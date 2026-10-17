package dev.baimaodaxia.mod.gui.items.buttons;

import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.core.impl.FontManager;
import java.awt.Color;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
public class CustomButton extends ClickableWidget {
   private final PressAction onPress;
   private boolean hovered;

   public CustomButton(int x, int y, int width, int height, Text message, PressAction onPress) {
      super(x, y, width, height, message);
      this.onPress = onPress;
   }

   public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrices = context.getMatrices();
      this.hovered = this.isHovered();
      int textColor = this.hovered ? 16777120 : 16777215;
      Color bgColor = this.hovered ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 150);
      Render2DUtil.drawRoundedRect(matrices, (float)this.getX(), (float)this.getY(), (float)this.width, (float)this.height, 3.0F, bgColor);
      String text = this.getMessage().getString();
      if (FontManager.ui != null) {
         int textWidth = (int)FontManager.ui.getWidth(text);
         int textX = this.getX() + (this.width - textWidth) / 2;
         int textY = this.getY() + (this.height - 8) / 2;
         FontManager.ui.drawString(matrices, text, (double)textX, (double)textY, textColor);
      } else {
         TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
         int textWidth = textRenderer.getWidth(text);
         int textX = this.getX() + (this.width - textWidth) / 2;
         int textY = this.getY() + (this.height - 8) / 2;
         context.drawText(textRenderer, this.getMessage(), textX, textY, textColor, false);
      }

   }

   public void onClick(double mouseX, double mouseY) {
      this.onPress.onPress(this);
   }

   protected void appendClickableNarrations(NarrationMessageBuilder builder) {
   }

   @FunctionalInterface
   public interface PressAction {
      void onPress(CustomButton var1);
   }
}
