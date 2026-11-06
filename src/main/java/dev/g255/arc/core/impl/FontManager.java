package dev.g255.arc.core.impl;

import dev.g255.arc.mod.gui.fonts.FontRenderer;
import dev.g255.arc.mod.modules.impl.client.Fonts;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import net.minecraft.client.util.math.MatrixStack;

public class FontManager {
   public static FontRenderer ui;
   public static FontRenderer small;
   public static FontRenderer icon;

   public static boolean isCustomFontEnabled() {
      return Fonts.INSTANCE != null && Fonts.INSTANCE.isOn();
   }

   public static boolean isShadowEnabled() {
      return Fonts.INSTANCE == null || Fonts.INSTANCE.shadow.getValue();
   }

   public static void init() {
      try {
         // 默认界面字体必须包含中文字形，否则物品/方块名会渲染成豆腐块
         ui = assets(8.0F, "MiSans-Regular", 0);
         small = assets(6.0F, "MiSans-Regular", 0);
         icon = assetsWithoutOffset(8.0F, "icon", 0);
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   /**
    * 按名字取字体流：assets/arc/fonts/&lt;name&gt;.ttf → MiSans-Regular.ttf → minecraft 自带字体。
    * 最后一级只用于兜底，它只有 216 个 ASCII 码位，不能让中文界面走到这里。
    */
   private static InputStream openFontFile(String name) {
      ClassLoader classLoader = FontManager.class.getClassLoader();
      InputStream stream = classLoader.getResourceAsStream("assets/arc/fonts/" + name + ".ttf");
      if (stream == null) {
         stream = classLoader.getResourceAsStream("assets/arc/fonts/MiSans-Regular.ttf");
      }

      if (stream == null) {
         stream = classLoader.getResourceAsStream("assets/minecraft/font/font.ttf");
      }

      return stream;
   }

   /**
    * 所有字体统一放在 assets/arc/fonts/ 下；请求的字体文件不存在时统一回落到 MiSans-Regular
    * （含完整中文字形），只有它也不存在时才用 minecraft 自带字体兜底。
    */
   public static FontRenderer assets(float size, String font, int style, String alternate) throws IOException, FontFormatException {
      InputStream stream = openFontFile(font);
      return new FontRenderer(Font.createFont(0, (InputStream)Objects.requireNonNull(stream)).deriveFont(style, size), getFont(alternate, style, (int)size), size) {
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
            float dx = 0.0F;
            float dy = 0.0F;
            if (FontManager.isCustomFontEnabled()) {
               dx = (float)Fonts.INSTANCE.translate.getValueInt();
               dy = (float)Fonts.INSTANCE.shift.getValueInt();
            }

            super.drawString(stack, s, x + dx, y + dy, r, g, elementCodec, keyCodec, shadow);
         }
      };
   }

   public static FontRenderer assetsWithoutOffset(float size, String name, int style) throws IOException, FontFormatException {
      InputStream stream = openFontFile(name);
      return new FontRenderer(Font.createFont(0, (InputStream)Objects.requireNonNull(stream)).deriveFont(style, size), size);
   }

   public static FontRenderer assets(float size, String name, int style) throws IOException, FontFormatException {
      InputStream stream = openFontFile(name);
      return new FontRenderer(Font.createFont(0, (InputStream)Objects.requireNonNull(stream)).deriveFont(style, size), size) {
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
            float dx = 0.0F;
            float dy = 0.0F;
            if (FontManager.isCustomFontEnabled()) {
               dx = (float)Fonts.INSTANCE.translate.getValueInt();
               dy = (float)Fonts.INSTANCE.shift.getValueInt();
            }

            super.drawString(stack, s, x + dx, y + dy, r, g, elementCodec, keyCodec, shadow);
         }
      };
   }

   public static FontRenderer create(int size, String font, int style, String alternate) {
      return new FontRenderer(getFont(font, style, size), getFont(alternate, style, size), (float)size) {
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
            float dx = 0.0F;
            float dy = 0.0F;
            if (FontManager.isCustomFontEnabled()) {
               dx = (float)Fonts.INSTANCE.translate.getValueInt();
               dy = (float)Fonts.INSTANCE.shift.getValueInt();
            }

            super.drawString(stack, s, x + dx, y + dy, r, g, elementCodec, keyCodec, shadow);
         }
      };
   }

   public static FontRenderer create(int size, String font, int style) {
      return new FontRenderer(getFont(font, style, size), (float)size) {
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
            float dx = 0.0F;
            float dy = 0.0F;
            if (FontManager.isCustomFontEnabled()) {
               dx = (float)Fonts.INSTANCE.translate.getValueInt();
               dy = (float)Fonts.INSTANCE.shift.getValueInt();
            }

            super.drawString(stack, s, x + dx, y + dy, r, g, elementCodec, keyCodec, shadow);
         }
      };
   }

   private static Font getFont(String font, int style, int size) {
      File fontDir = new File("C:\\Windows\\Fonts");

      try {
         for(File file : fontDir.listFiles()) {
            if (file.getName().replace(".ttf", "").replace(".ttc", "").replace(".otf", "").equalsIgnoreCase(font)) {
               try {
                  return Font.createFont(0, file).deriveFont(style, (float)size);
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
         }

         for(File file : fontDir.listFiles()) {
            if (file.getName().startsWith(font)) {
               try {
                  return Font.createFont(0, file).deriveFont(style, (float)size);
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
         }
      } catch (Exception var11) {
      }

      return new Font((String)null, style, size);
   }
}
