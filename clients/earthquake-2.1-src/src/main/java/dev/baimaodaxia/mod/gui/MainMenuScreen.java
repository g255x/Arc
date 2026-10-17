package dev.baimaodaxia.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.core.impl.FontManager;
import dev.baimaodaxia.mod.gui.fonts.FontRenderer;
import dev.baimaodaxia.mod.gui.items.buttons.TransparentButton;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import net.minecraft.text.Text;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;
import net.minecraft.client.render.VertexFormat;

public class MainMenuScreen extends Screen {
   private static final int BUTTON_WIDTH = 200;
   private static final int BUTTON_HEIGHT = 20;
   private static final int BUTTON_MARGIN = 4;
   private static final Identifier TITLE_IMAGE = Identifier.of("alien", "textures/hud/earthquake.png");
   private static final int DISPLAY_WIDTH = 480;
   private static final int DISPLAY_HEIGHT = 135;
   private static FontRenderer menuFont = null;

   public MainMenuScreen() {
      super(Text.of("MainMenu"));
      loadMenuFont();
   }

   private static void loadMenuFont() {
      if (menuFont == null) {
         try {
            menuFont = createMenuFont(18.0F, "default", 0);
         } catch (Exception var1) {
            menuFont = FontManager.ui;
         }

      }
   }

   private static FontRenderer createMenuFont(float size, String font, int style) throws IOException, FontFormatException {
      ClassLoader classLoader = MainMenuScreen.class.getClassLoader();
      InputStream primary = classLoader.getResourceAsStream("assets/earthquake/font/" + font + ".ttf");
      InputStream fallback = classLoader.getResourceAsStream("assets/minecraft/font/font.ttf");
      InputStream stream = primary != null ? primary : fallback;
      return new FontRenderer(Font.createFont(0, (InputStream)Objects.requireNonNull(stream)).deriveFont(style, size), size);
   }

   protected void init() {
      super.init();
      int buttonCount = 4;
      int totalHeight = buttonCount * 20 + (buttonCount - 1) * 4;
      int startY = (this.height - totalHeight) / 2 + 20 + 40;
      int buttonX = (this.width - 200) / 2;
      this.addDrawableChild(new TransparentButton(buttonX, startY, 200, 20, Text.literal("Singleplayer"), (button) -> this.client.setScreen(new SelectWorldScreen(this)), menuFont));
      int buttonY = startY + 24;
      this.addDrawableChild(new TransparentButton(buttonX, buttonY, 200, 20, Text.literal("Multiplayer"), (button) -> this.client.setScreen(new MultiplayerScreen(this)), menuFont));
      buttonY += 24;
      this.addDrawableChild(new TransparentButton(buttonX, buttonY, 200, 20, Text.literal("Options"), (button) -> this.client.setScreen(new OptionsScreen(this, this.client.options)), menuFont));
      buttonY += 24;
      this.addDrawableChild(new TransparentButton(buttonX, buttonY, 200, 20, Text.literal("Quit"), (button) -> this.client.scheduleStop(), menuFont));
   }

   public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
      super.render(drawContext, mouseX, mouseY, partialTicks);
      int titleY = this.height / 6;
      int imageX = (this.width - 480) / 2;
      this.drawTexturedQuad(drawContext.getMatrices(), TITLE_IMAGE, imageX, titleY, 480, 135);
   }

   private void drawTexturedQuad(MatrixStack matrices, Identifier texture, int x, int y, int width, int height) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, texture);
      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      buffer.vertex(matrix, (float)x, (float)(y + height), 0.0F).texture(0.0F, 1.0F);
      buffer.vertex(matrix, (float)(x + width), (float)(y + height), 0.0F).texture(1.0F, 1.0F);
      buffer.vertex(matrix, (float)(x + width), (float)y, 0.0F).texture(1.0F, 0.0F);
      buffer.vertex(matrix, (float)x, (float)y, 0.0F).texture(0.0F, 0.0F);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return super.mouseClicked(mouseX, mouseY, button);
   }
}
