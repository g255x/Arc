package dev.baimaodaxia.mod.gui.items.buttons;

import dev.baimaodaxia.core.impl.FontManager;
import dev.baimaodaxia.mod.gui.fonts.FontRenderer;
import java.util.Objects;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
public class TransparentButton extends ClickableWidget {
   private final PressAction onPress;
   private FontRenderer customFont = null;

   public TransparentButton(int x, int y, int width, int height, Text message, PressAction onPress) {
      super(x, y, width, height, message);
      this.onPress = onPress;
   }

   public TransparentButton(int x, int y, int width, int height, Text message, PressAction onPress, FontRenderer customFont) {
      super(x, y, width, height, message);
      this.onPress = onPress;
      this.customFont = customFont;
   }

   public void setCustomFont(FontRenderer font) {
      this.customFont = font;
   }

   public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
      int backgroundColor = this.isHovered() ? Integer.MIN_VALUE : 1610612736;
      int borderColor = this.isHovered() ? -2130706433 : 1090519039;
      context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, backgroundColor);
      context.drawBorder(this.getX(), this.getY(), this.width, this.height, borderColor);
      int textColor = this.isHovered() ? 16777120 : -1;
      String text = this.getMessage().getString();
      FontRenderer fontToUse = this.customFont != null ? this.customFont : FontManager.ui;
      if (fontToUse != null) {
         int textWidth = (int)fontToUse.getWidth(text);
         int textHeight = (int)fontToUse.getFontHeight();
         int textX = this.getX() + (this.width - textWidth) / 2;
         int textY = this.getY() + (this.height - textHeight) / 2;
         fontToUse.drawString(context.getMatrices(), text, (double)textX, (double)textY, textColor);
      } else {
         TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
         int textWidth = textRenderer.getWidth(text);
         Objects.requireNonNull(textRenderer);
         int textHeight = 9;
         int textX = this.getX() + (this.width - textWidth) / 2;
         int textY = this.getY() + (this.height - textHeight) / 2;
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
      void onPress(TransparentButton var1);
   }
}
