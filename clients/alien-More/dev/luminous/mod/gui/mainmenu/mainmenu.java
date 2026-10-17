package dev.luminous.mod.gui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.Snow;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_429;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_500;
import net.minecraft.class_526;

public class mainmenu extends class_437 {
   private SnowRenderer snowRenderer = new SnowRenderer();
   private SmokeRenderer smokeRenderer = new SmokeRenderer();
   private float time = 0.0F;
   private Random random = new Random();
   private static final String CONFIG_FILE = "particle_mode.cfg";
   private ParticleMode currentParticleMode = this.loadParticleMode();
   private static final float BUTTON_WIDTH = 130.0F;
   private static final float BUTTON_HEIGHT = 18.0F;
   private static final float BUTTON_SPACING = 8.0F;
   private static final float BUTTON_RADIUS = 6.0F;
   private static final Color BUTTON_NORMAL_COLOR = new Color(60, 60, 65, 200);
   private static final Color BUTTON_HOVER_COLOR = new Color(80, 80, 85, 220);
   private static final Color BUTTON_TEXT_COLOR = new Color(230, 230, 235, 255);
   private static final Color BUTTON_BORDER_COLOR = new Color(100, 100, 105, 80);
   private static final Color BUTTON_SHADOW_COLOR = new Color(30, 30, 35, 50);
   private final List buttons = new ArrayList();

   public mainmenu() {
      super(class_2561.method_43471("narrator.screen.title"));
      System.out.println("[MainMenu] Loaded particle mode: " + this.currentParticleMode.getDisplayName());
      this.initializeButtons();
   }

   private ParticleMode loadParticleMode() {
      File configFile = new File("particle_mode.cfg");
      if (!configFile.exists()) {
         this.saveParticleMode(mainmenu.ParticleMode.NONE);
         return mainmenu.ParticleMode.NONE;
      } else {
         try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));

            label40: {
               ParticleMode var5;
               try {
                  String line = reader.readLine();
                  if (line == null || line.trim().isEmpty()) {
                     break label40;
                  }

                  try {
                     int id = Integer.parseInt(line.trim());
                     var5 = mainmenu.ParticleMode.fromId(id);
                  } catch (NumberFormatException var7) {
                     System.err.println("[MainMenu] Invalid particle mode ID in config: " + line);
                     break label40;
                  }
               } catch (Throwable var8) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var8.addSuppressed(var6);
                  }

                  throw var8;
               }

               reader.close();
               return var5;
            }

            reader.close();
         } catch (IOException e) {
            System.err.println("[MainMenu] Failed to load particle mode config: " + e.getMessage());
         }

         return mainmenu.ParticleMode.NONE;
      }
   }

   private void saveParticleMode(ParticleMode mode) {
      try {
         BufferedWriter writer = new BufferedWriter(new FileWriter("particle_mode.cfg"));

         try {
            writer.write(String.valueOf(mode.getId()));
            PrintStream var10000 = System.out;
            String var10001 = mode.getDisplayName();
            var10000.println("[MainMenu] Saved particle mode: " + var10001 + " (ID: " + mode.getId() + ")");
         } catch (Throwable var6) {
            try {
               writer.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         writer.close();
      } catch (IOException e) {
         System.err.println("[MainMenu] Failed to save particle mode config: " + e.getMessage());
      }

   }

   private void initializeButtons() {
      this.buttons.add(new MenuButton("Singleplayer", () -> Wrapper.mc.method_1507(new class_526(this))));
      this.buttons.add(new MenuButton("Multiplayer", () -> {
         if (!Wrapper.mc.field_1690.field_21840) {
            Wrapper.mc.field_1690.field_21840 = true;
            Wrapper.mc.field_1690.method_1640();
         }

         Wrapper.mc.method_1507(new class_500(this));
      }));
      this.buttons.add(new MenuButton("Options", () -> Wrapper.mc.method_1507(new class_429(this, Wrapper.mc.field_1690))));
      this.buttons.add(new MenuButton("Quit Game", () -> Wrapper.mc.method_1490()));
   }

   protected void method_25426() {
      super.method_25426();
      this.snowRenderer.init(this.field_22789, this.field_22790);
      this.smokeRenderer.init(this.field_22789, this.field_22790);
   }

   private void toggleParticleMode() {
      this.currentParticleMode = this.currentParticleMode.next();
      this.saveParticleMode(this.currentParticleMode);
      System.out.println("[MainMenu] Particle mode changed to: " + this.currentParticleMode.getDisplayName());
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      this.time += delta * 0.05F;
      this.method_25420(context, mouseX, mouseY, delta);
      switch (this.currentParticleMode.ordinal()) {
         case 0:
         default:
            break;
         case 1:
            this.snowRenderer.render(context);
            break;
         case 2:
            this.smokeRenderer.update(delta);
            this.smokeRenderer.render(context);
      }

      this.drawTitle(context);
      this.drawButtons(context, mouseX, mouseY);
   }

   private void drawButtons(class_332 context, int mouseX, int mouseY) {
      float centerX = (float)this.field_22789 / 2.0F;
      float centerY = (float)this.field_22790 / 2.0F;
      float totalHeight = (float)this.buttons.size() * 18.0F + (float)(this.buttons.size() - 1) * 8.0F;
      float startY = centerY - totalHeight / 2.0F;

      for(int i = 0; i < this.buttons.size(); ++i) {
         float buttonY = startY + (float)i * 26.0F;
         float buttonX = centerX - 65.0F;
         ((MenuButton)this.buttons.get(i)).render(context, buttonX, buttonY, mouseX, mouseY);
      }

      float particleButtonX = centerX + 40.0F + 65.0F;
      float particleButtonY = startY + 52.0F;
      this.drawParticleButton(context, particleButtonX, particleButtonY, mouseX, mouseY);
   }

   private void drawParticleButton(class_332 context, float x, float y, int mouseX, int mouseY) {
      boolean hovered = this.isMouseOver((double)x, (double)y, (double)130.0F, (double)18.0F, (double)mouseX, (double)mouseY);
      Color buttonColor = BUTTON_NORMAL_COLOR;
      switch (this.currentParticleMode.ordinal()) {
         case 0 -> buttonColor = new Color(60, 60, 65, 200);
         case 1 -> buttonColor = new Color(90, 100, 110, 200);
         case 2 -> buttonColor = new Color(80, 85, 95, 200);
      }

      if (hovered) {
         buttonColor = BUTTON_HOVER_COLOR;
      }

      Render2DUtil.drawRound(context.method_51448(), x + 1.0F, y + 1.0F, 130.0F, 18.0F, 6.0F, BUTTON_SHADOW_COLOR);
      Render2DUtil.drawRound(context.method_51448(), x, y, 130.0F, 18.0F, 6.0F, buttonColor);
      Render2DUtil.drawRoundedStroke(context.method_51448(), x, y, 130.0F, 18.0F, 6.0F, BUTTON_BORDER_COLOR, 8);
      String buttonText = "Particle: " + this.currentParticleMode.getDisplayName();
      float textX = x + 65.0F;
      float var10000 = y + 9.0F;
      Objects.requireNonNull(Wrapper.mc.field_1772);
      float textY = var10000 - (float)(9 / 2);
      context.method_25300(Wrapper.mc.field_1772, buttonText, (int)textX, (int)textY, BUTTON_TEXT_COLOR.getRGB());
      String description = this.currentParticleMode.getDescription();
      float descX = x + 65.0F;
      float descY = y + 18.0F + 4.0F;
      int descColor = (new Color(180, 180, 185, 180)).getRGB();
      context.method_25300(Wrapper.mc.field_1772, description, (int)descX, (int)descY, descColor);
   }

   private void drawTitle(class_332 context) {
      String title = "Nina";
      int titleWidth = Wrapper.mc.field_1772.method_1727(title);
      float floatX = (float)Math.sin((double)this.time * (double)1.5F) * 8.0F;
      float floatY = (float)Math.cos((double)this.time * 1.2) * 6.0F;
      float scale = 2.0F;
      context.method_51448().method_22903();
      float centerX = (float)this.field_22789 / 2.0F + floatX;
      float centerY = (float)this.field_22790 / 4.0F + floatY;
      context.method_51448().method_46416(centerX, centerY, 0.0F);
      context.method_51448().method_22905(scale, scale, 1.0F);
      class_4587 var10000 = context.method_51448();
      float var10001 = (float)(-titleWidth) / 2.0F;
      Objects.requireNonNull(Wrapper.mc.field_1772);
      var10000.method_46416(var10001, (float)(-9) / 2.0F, 0.0F);
      int color = Color.HSBtoRGB(this.time % 1.0F, 0.7F, 1.0F);
      context.method_51433(Wrapper.mc.field_1772, title, 0, 0, color, false);
      context.method_51448().method_22909();
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.method_25402(mouseX, mouseY, button);
      } else {
         float centerX = (float)this.field_22789 / 2.0F;
         float centerY = (float)this.field_22790 / 2.0F;
         float totalHeight = (float)this.buttons.size() * 18.0F + (float)(this.buttons.size() - 1) * 8.0F;
         float startY = centerY - totalHeight / 2.0F;

         for(int i = 0; i < this.buttons.size(); ++i) {
            float buttonY = startY + (float)i * 26.0F;
            float buttonX = centerX - 65.0F;
            if (this.isMouseOver((double)buttonX, (double)buttonY, (double)130.0F, (double)18.0F, mouseX, mouseY)) {
               ((MenuButton)this.buttons.get(i)).onClick();
               return true;
            }
         }

         float particleButtonX = centerX + 40.0F + 65.0F;
         float particleButtonY = startY + 52.0F;
         if (this.isMouseOver((double)particleButtonX, (double)particleButtonY, (double)130.0F, (double)18.0F, mouseX, mouseY)) {
            this.toggleParticleMode();
            return true;
         } else {
            return super.method_25402(mouseX, mouseY, button);
         }
      }
   }

   public void method_25420(class_332 context, int mouseX, int mouseY, float delta) {
      context.method_25294(0, 0, this.field_22789, this.field_22790, -16115158);
      int startColor = -16115158;
      int endColor = -15455686;

      for(int i = 0; i < this.field_22790; ++i) {
         float ratio = (float)i / (float)this.field_22790;
         int color = this.interpolateColor(startColor, endColor, ratio);
         context.method_25294(0, i, this.field_22789, i + 1, color);
      }

   }

   private int interpolateColor(int color1, int color2, float ratio) {
      int r1 = color1 >> 16 & 255;
      int g1 = color1 >> 8 & 255;
      int b1 = color1 & 255;
      int r2 = color2 >> 16 & 255;
      int g2 = color2 >> 8 & 255;
      int b2 = color2 & 255;
      int r = (int)((float)r1 + (float)(r2 - r1) * ratio);
      int g = (int)((float)g1 + (float)(g2 - g1) * ratio);
      int b = (int)((float)b1 + (float)(b2 - b1) * ratio);
      return -16777216 | r << 16 | g << 8 | b;
   }

   public void method_25393() {
      super.method_25393();
   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         Wrapper.mc.method_1490();
         return true;
      } else {
         return super.method_25404(keyCode, scanCode, modifiers);
      }
   }

   private boolean isMouseOver(double x, double y, double width, double height, double mouseX, double mouseY) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private static enum ParticleMode {
      NONE("No particle effects", 0),
      SNOW_ONLY("Snow effect", 1),
      SMOKE_ONLY("Dynamic smoke particles", 2);

      private final String description;
      private final int id;

      private ParticleMode(String description, int id) {
         this.description = description;
         this.id = id;
      }

      public String getDisplayName() {
         String var10000;
         switch (this.ordinal()) {
            case 0 -> var10000 = "None";
            case 1 -> var10000 = "Snow";
            case 2 -> var10000 = "Smoke";
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      public String getDescription() {
         return this.description;
      }

      public int getId() {
         return this.id;
      }

      public static ParticleMode fromId(int id) {
         ParticleMode var10000;
         switch (id) {
            case 0 -> var10000 = NONE;
            case 1 -> var10000 = SNOW_ONLY;
            case 2 -> var10000 = SMOKE_ONLY;
            default -> var10000 = NONE;
         }

         return var10000;
      }

      public ParticleMode next() {
         ParticleMode var10000;
         switch (this.ordinal()) {
            case 0 -> var10000 = SNOW_ONLY;
            case 1 -> var10000 = SMOKE_ONLY;
            case 2 -> var10000 = NONE;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      // $FF: synthetic method
      private static ParticleMode[] $values() {
         return new ParticleMode[]{NONE, SNOW_ONLY, SMOKE_ONLY};
      }
   }

   private class MenuButton {
      private final String text;
      private final Runnable action;

      public MenuButton(String text, Runnable action) {
         this.text = text;
         this.action = action;
      }

      public void render(class_332 context, float x, float y, int mouseX, int mouseY) {
         boolean hovered = mainmenu.this.isMouseOver((double)x, (double)y, (double)130.0F, (double)18.0F, (double)mouseX, (double)mouseY);
         Color buttonColor = hovered ? mainmenu.BUTTON_HOVER_COLOR : mainmenu.BUTTON_NORMAL_COLOR;
         Render2DUtil.drawRound(context.method_51448(), x + 1.0F, y + 1.0F, 130.0F, 18.0F, 6.0F, mainmenu.BUTTON_SHADOW_COLOR);
         Render2DUtil.drawRound(context.method_51448(), x, y, 130.0F, 18.0F, 6.0F, buttonColor);
         Render2DUtil.drawRoundedStroke(context.method_51448(), x, y, 130.0F, 18.0F, 6.0F, mainmenu.BUTTON_BORDER_COLOR, 8);
         float textX = x + 65.0F;
         float var10000 = y + 9.0F;
         Objects.requireNonNull(Wrapper.mc.field_1772);
         float textY = var10000 - (float)(9 / 2);
         context.method_25300(Wrapper.mc.field_1772, this.text, (int)textX, (int)textY, mainmenu.BUTTON_TEXT_COLOR.getRGB());
      }

      public void onClick() {
         this.action.run();
      }
   }

   private class SnowRenderer {
      private List snowflakes = new ArrayList();
      private int width;
      private int height;

      void init(int width, int height) {
         this.width = width;
         this.height = height;
         this.snowflakes.clear();
         int snowCount = 100;

         for(int i = 0; i < snowCount; ++i) {
            Random rand = new Random();
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            int fallingSpeed = rand.nextInt(10) + 1;
            int size = rand.nextInt(4) + 1;
            this.snowflakes.add(new Snow(x, y, fallingSpeed, size));
         }

      }

      void render(class_332 context) {
         class_4587 matrices = context.method_51448();
         matrices.method_22903();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();

         for(Snow snowflake : this.snowflakes) {
            snowflake.drawSnow(context);
            if (snowflake.getY() > this.height + 10 || snowflake.getY() < -10) {
               Random rand = new Random();
               snowflake.setX(rand.nextInt(this.width));
               snowflake.setY(-10);
            }
         }

         RenderSystem.disableBlend();
         matrices.method_22909();
      }
   }

   private class SmokeRenderer {
      private List particles = new ArrayList();
      private int width;
      private int height;

      void init(int width, int height) {
         this.width = width;
         this.height = height;
         this.particles.clear();
         int particleCount = 80;

         for(int i = 0; i < particleCount; ++i) {
            this.particles.add(new SmokeParticle(width, height));
         }

      }

      void update(float delta) {
         for(SmokeParticle particle : this.particles) {
            particle.update(delta);
         }

      }

      void render(class_332 context) {
         class_4587 matrices = context.method_51448();
         matrices.method_22903();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();

         for(SmokeParticle particle : this.particles) {
            particle.render(context);
         }

         RenderSystem.disableBlend();
         matrices.method_22909();
      }

      class SmokeParticle {
         float x;
         float y;
         float size;
         float speedX;
         float speedY;
         Color color;
         float life;
         float maxLife;
         float alpha;

         SmokeParticle(int width, int height) {
            this.reset(width, height);
         }

         void reset(int width, int height) {
            this.x = mainmenu.this.random.nextFloat() * (float)width;
            this.y = (float)(height + 10);
            this.size = 2.0F + mainmenu.this.random.nextFloat() * 8.0F;
            this.speedX = (mainmenu.this.random.nextFloat() - 0.5F) * 0.8F;
            this.speedY = -(1.5F + mainmenu.this.random.nextFloat() * 3.0F);
            this.life = 0.0F;
            this.maxLife = 50.0F + mainmenu.this.random.nextFloat() * 100.0F;
            this.alpha = 0.7F;
            int colorType = mainmenu.this.random.nextInt(3);
            switch (colorType) {
               case 0:
                  int r = 180 + mainmenu.this.random.nextInt(40);
                  int g = 220 + mainmenu.this.random.nextInt(35);
                  int b = 255;
                  this.color = new Color(r, g, b, (int)(this.alpha * 255.0F));
                  break;
               case 1:
                  int r = 220 + mainmenu.this.random.nextInt(35);
                  int g = 220 + mainmenu.this.random.nextInt(35);
                  int b = 220 + mainmenu.this.random.nextInt(35);
                  this.color = new Color(r, g, b, (int)(this.alpha * 255.0F));
                  break;
               case 2:
                  int r = 200 + mainmenu.this.random.nextInt(55);
                  int g = 180 + mainmenu.this.random.nextInt(75);
                  int b = 220 + mainmenu.this.random.nextInt(35);
                  this.color = new Color(r, g, b, (int)(this.alpha * 255.0F));
            }

         }

         void update(float delta) {
            this.x += this.speedX * delta * 30.0F;
            this.y += this.speedY * delta * 30.0F;
            this.life += delta * 10.0F;
            if (this.life > this.maxLife * 0.7F) {
               float fade = 1.0F - (this.life - this.maxLife * 0.7F) / (this.maxLife * 0.3F);
               this.alpha = 0.7F * fade;
            }

            if (this.x < -this.size || this.x > (float)SmokeRenderer.this.width + this.size || this.y < -this.size || this.life > this.maxLife) {
               this.reset(SmokeRenderer.this.width, SmokeRenderer.this.height);
            }

         }

         void render(class_332 context) {
            Color renderColor = new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), (int)(this.alpha * 255.0F));
            int intSize = (int)this.size;
            int halfSize = intSize / 2;
            context.method_25294((int)(this.x - (float)halfSize), (int)(this.y - (float)halfSize), (int)(this.x + (float)halfSize), (int)(this.y + (float)halfSize), renderColor.getRGB());
         }
      }
   }
}
