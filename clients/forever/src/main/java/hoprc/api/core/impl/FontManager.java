package hoprc.api.core.impl;

import hoprc.api.gui.fonts.FontRenderer;
import dev.kissoo.mod.modules.impl.client.Fonts;
import dev.kissoo.mod.modules.impl.client.Fonts.FontMode;

import java.awt.Font;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FontManager {

   private static final String FONT_DIR = "assets/alienclient/font/";
   private static final String DEFAULT_FONT      = FONT_DIR + "default.ttf";
   private static final String MISANS_FONT        = FONT_DIR + "misans-regular.ttf";
   private static final String BOREL_FONT         = FONT_DIR + "borel-regular.ttf";
   private static final String ICON_FONT          = FONT_DIR + "icon.ttf";
   private static final String ICON_NEW_FONT      = FONT_DIR + "iconn.ttf";
   private static final String HOPRC_FONT         = FONT_DIR + "hoprc.ttf";
   private static final String HYW_FONT           = BOREL_FONT;
   private static final String CUSTOM_OTF_FONT    = FONT_DIR + "font.otf";
   private static final String NOTIFICATION_MAIN_FONT     = FONT_DIR + "lexenddeca-regular.ttf";
   private static final String NOTIFICATION_FALLBACK_FONT = FONT_DIR + "jura-light.ttf";
   private static final String OXANIUM_FONT       = FONT_DIR + "oxanium-light.ttf";
   private static final String ORBITRON_FONT      = FONT_DIR + "orbitron.ttf";

   public static final float NOTIF_TITLE_SIZE = 14.0f;
   public static final float NOTIF_MSG_SIZE   = 11.0f;

   private static final ConcurrentHashMap<String, FontRenderer> cache = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<String, Font> awtFontCache  = new ConcurrentHashMap<>();
   private static FontRenderer iconFont;
   private static FontRenderer iconNewFont;
   private static String currentConfigHash   = "";
   private static volatile boolean initializing = false;

   public static final String ICON_DISABLED = new String(Character.toChars(0xF0A19));
   public static final String ICON_ENABLED  = new String(Character.toChars(0xF0521));

   @Deprecated public static FontRenderer ui     = null;
   @Deprecated public static FontRenderer small  = null;
   @Deprecated public static FontRenderer icon   = null;
   @Deprecated public static FontRenderer iconn  = null;
   @Deprecated public static FontRenderer misans = null;
   @Deprecated public static FontRenderer artist = null;

   public static void init() {
      if (initializing) return;
      initializing = true;
      try {
         iconFont = createFromAssets(ICON_FONT, 8.0f, 0, false);
         if (iconFont == null) iconFont = createSafeIconFallback();
         icon = iconFont;

         iconNewFont = createFromAssets(ICON_NEW_FONT, 8.0f, 0, false);
         if (iconNewFont == null) iconNewFont = createSafeIconFallback();
         iconn = iconNewFont;

         refresh();
      } catch (Exception e) {
         e.printStackTrace();
         iconFont    = createSafeIconFallback();
         icon        = iconFont;
         iconNewFont = createSafeIconFallback();
         iconn       = iconNewFont;
      } finally {
         initializing = false;
      }
   }

   @NotNull
   public static FontRenderer getNotificationTitleFont() {
      return getNotificationFont(NOTIF_TITLE_SIZE);
   }
   @NotNull
   public static FontRenderer getNotificationMessageFont() {
      return getNotificationFont(NOTIF_MSG_SIZE);
   }
   @NotNull
   public static FontRenderer getNotificationFont(float size) {
      String cacheKey = "notif_lexend_" + size;
      return cache.computeIfAbsent(cacheKey, key -> {
         FontRenderer fr = createFromAssets(NOTIFICATION_MAIN_FONT, size, Font.PLAIN, false);
         if (fr != null) return fr;
         FontRenderer jura = createFromAssets(NOTIFICATION_FALLBACK_FONT, size, Font.PLAIN, false);
         if (jura != null) return jura;
         FontRenderer def = createFromAssets(DEFAULT_FONT, size, Font.PLAIN, false);
         if (def != null) return def;
         return createFallback(size, Font.PLAIN);
      });
   }

   @NotNull
   public static FontRenderer getIconFont(float size) {
      String cacheKey = "fixed_icon_" + size;
      return cache.computeIfAbsent(cacheKey, key -> {
         FontRenderer fr = createFromAssets(ICON_FONT, size, Font.PLAIN, false);
         if (fr != null) return fr;
         return createSafeIconFallback();
      });
   }

   @NotNull
   public static FontRenderer getIconNewFont(float size) {
      String cacheKey = "fixed_iconn_" + size;
      return cache.computeIfAbsent(cacheKey, key -> {
         FontRenderer fr = createFromAssets(ICON_NEW_FONT, size, Font.PLAIN, false);
         if (fr != null) return fr;
         return createSafeIconFallback();
      });
   }

   @Nullable
   public static FontRenderer getMiSansFont(float size) {
      String cacheKey = "fixed_misans_" + size;
      return cache.computeIfAbsent(cacheKey, key -> {
         FontRenderer fr = createFromAssets(MISANS_FONT, size, Font.PLAIN, false);
         if (fr != null) return fr;
         FontRenderer def = createFromAssets(DEFAULT_FONT, size, Font.PLAIN, false);
         if (def != null) return def;
         return createFallback(size, Font.PLAIN);
      });
   }

   @Nullable
   public static FontRenderer getHywFont(float size) {
      String cacheKey = "fixed_hyw_" + size;
      return cache.computeIfAbsent(cacheKey, key -> {
         FontRenderer fr = createFromAssets(HYW_FONT, size, Font.PLAIN, false);
         if (fr != null) return fr;
         FontRenderer def = createFromAssets(DEFAULT_FONT, size, Font.PLAIN, false);
         if (def != null) return def;
         return createFallback(size, Font.PLAIN);
      });
   }

   @Nullable
   public static FontRenderer getCustomOtfFont(float size) {
      String cacheKey = "fixed_custom_otf_" + size;
      return cache.computeIfAbsent(cacheKey, key -> {
         FontRenderer fr = createFromAssets(CUSTOM_OTF_FONT, size, Font.PLAIN, false);
         if (fr != null) return fr;
         FontRenderer def = createFromAssets(DEFAULT_FONT, size, Font.PLAIN, false);
         if (def != null) return def;
         return createFallback(size, Font.PLAIN);
      });
   }
   @Nullable
   public static FontRenderer getOxaniumFont(float size) {
      String cacheKey = "fixed_oxanium_" + size;
      return cache.computeIfAbsent(cacheKey, key -> {
         FontRenderer fr = createFromAssets(OXANIUM_FONT, size, Font.PLAIN, false);
         if (fr != null) return fr;
         FontRenderer def = createFromAssets(DEFAULT_FONT, size, Font.PLAIN, false);
         if (def != null) return def;
         return createFallback(size, Font.PLAIN);
      });
   }
   @Nullable
   public static FontRenderer getOrbitronFont(float size) {
      String cacheKey = "fixed_orbitron_" + size;
      return cache.computeIfAbsent(cacheKey, key -> {
         FontRenderer fr = createFromAssets(ORBITRON_FONT, size, Font.PLAIN, false);
         if (fr != null) return fr;
         FontRenderer def = createFromAssets(DEFAULT_FONT, size, Font.PLAIN, false);
         if (def != null) return def;
         return createFallback(size, Font.PLAIN);
      });
   }

   @NotNull
   public static FontRenderer get() {
      return get(getSize());
   }

   @NotNull
   public static FontRenderer get(float size) {
      FontMode mode = getMode();
      if (mode == FontMode.MINECRAFT) {
         return getOrCreate(FontMode.DEFAULT, size);
      }
      return getOrCreate(mode, size);
   }

   @NotNull
   public static FontRenderer getSmall() {
      return get(6.0f);
   }

   @NotNull
   public static FontRenderer getIcon() {
      if (iconFont == null) init();
      return iconFont != null ? iconFont : createSafeIconFallback();
   }

   @Nullable
   public static FontRenderer getArtistFont(float size) {
      String cacheKey = "artist_borel_" + size;
      return cache.computeIfAbsent(cacheKey, key -> {
         FontRenderer fr = createFromAssets(BOREL_FONT, size, Font.PLAIN, true);
         return fr != null ? fr : createFallback(size, Font.PLAIN);
      });
   }

   public static boolean isCustom() {
      return getMode().isCustom();
   }

   @NotNull
   public static FontMode getMode() {
      if (Fonts.INSTANCE == null) return FontMode.DEFAULT;
      return (FontMode) Fonts.INSTANCE.mode.getValue();
   }

   public static float getSize() {
      if (Fonts.INSTANCE == null) return 8.0f;
      return Fonts.INSTANCE.size.getValueFloat();
   }

   public static float getWidth(String text) {
      if (!isCustom()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc == null || mc.textRenderer == null) return text == null ? 0 : text.length() * 6f;
         return mc.textRenderer.getWidth(text);
      }
      return get().getWidth(text);
   }

   public static float getFontHeight() {
      if (!isCustom()) return 9.0f;
      return get().getFontHeight();
   }

   public static float getFontHeight(String text) {
      if (!isCustom()) return 9.0f;
      return get().getFontHeight(text);
   }

   public static void refresh() {
      String newHash = buildConfigHash();
      if (newHash.equals(currentConfigHash)) return;
      clearCache();
      currentConfigHash = newHash;
      try {
         FontRenderer main      = get();
         FontRenderer smallFont = getSmall();
         ui     = main;
         small  = smallFont;
         icon   = getIcon();
         iconn  = getIconNewFont(8.0f);
         misans = main;
         artist = null; // Lazy-loaded by getArtistFont() when the Artist watermark option is used.
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static void clearCache() {
      for (FontRenderer renderer : cache.values()) {
         try { renderer.close(); } catch (Exception ignored) {}
      }
      cache.clear();
      currentConfigHash = "";
   }

   @NotNull
   private static FontRenderer getOrCreate(FontMode mode, float size) {
      int style       = getStyle();
      String cacheKey = mode.name() + "_" + size + "_" + style;
      return cache.computeIfAbsent(cacheKey, key -> {
         try {
            return createFontRenderer(mode, size, style);
         } catch (Exception e) {
            e.printStackTrace();
            return createFallback(size, style);
         }
      });
   }

   @NotNull
   private static FontRenderer createFontRenderer(FontMode mode, float size, int style) {
      switch (mode) {
         case DEFAULT: {
            FontRenderer fr = createFromAssets(DEFAULT_FONT, size, style, true);
            if (fr != null) return fr;
            return createFallback(size, style);
         }
         case MISANS: {
            FontRenderer fr = createFromAssets(MISANS_FONT, size, style, true);
            if (fr != null) return fr;
            FontRenderer def = createFromAssets(DEFAULT_FONT, size, style, true);
            if (def != null) return def;
            return createFallback(size, style);
         }
         case BOREL: {
            FontRenderer fr = createFromAssets(BOREL_FONT, size, style, true);
            if (fr != null) return fr;
            FontRenderer def = createFromAssets(DEFAULT_FONT, size, style, true);
            if (def != null) return def;
            return createFallback(size, style);
         }
         case HOPRC: {
            FontRenderer fr = createFromAssets(HOPRC_FONT, size, style, true);
            if (fr != null) return fr;
            FontRenderer def = createFromAssets(DEFAULT_FONT, size, style, true);
            if (def != null) return def;
            return createFallback(size, style);
         }
         case HYW: {
            FontRenderer fr = createFromAssets(HYW_FONT, size, style, true);
            if (fr != null) return fr;
            FontRenderer def = createFromAssets(DEFAULT_FONT, size, style, true);
            if (def != null) return def;
            return createFallback(size, style);
         }
         case CUSTOM_OTF: {
            FontRenderer fr = createFromAssets(CUSTOM_OTF_FONT, size, style, true);
            if (fr != null) return fr;
            FontRenderer def = createFromAssets(DEFAULT_FONT, size, style, true);
            if (def != null) return def;
            return createFallback(size, style);
         }
         case LEXENDDECA: {
            FontRenderer fr = createFromAssets(NOTIFICATION_MAIN_FONT, size, style, true);
            if (fr != null) return fr;
            FontRenderer def = createFromAssets(DEFAULT_FONT, size, style, true);
            if (def != null) return def;
            return createFallback(size, style);
         }
         case JURA: {
            FontRenderer fr = createFromAssets(NOTIFICATION_FALLBACK_FONT, size, style, true);
            if (fr != null) return fr;
            FontRenderer def = createFromAssets(DEFAULT_FONT, size, style, true);
            if (def != null) return def;
            return createFallback(size, style);
         }
         case OXANIUM: {
            FontRenderer fr = createFromAssets(OXANIUM_FONT, size, style, true);
            if (fr != null) return fr;
            FontRenderer def = createFromAssets(DEFAULT_FONT, size, style, true);
            if (def != null) return def;
            return createFallback(size, style);
         }
         case ORBITRON: {
            FontRenderer fr = createFromAssets(ORBITRON_FONT, size, style, true);
            if (fr != null) return fr;
            FontRenderer def = createFromAssets(DEFAULT_FONT, size, style, true);
            if (def != null) return def;
            return createFallback(size, style);
         }
         case SYSTEM: {
            FontRenderer fr = createFromSystem(size, style);
            if (fr != null) return fr;
            FontRenderer def = createFromAssets(DEFAULT_FONT, size, style, true);
            if (def != null) return def;
            return createFallback(size, style);
         }
         case MINECRAFT:
         default: {
            FontRenderer def = createFromAssets(DEFAULT_FONT, size, style, true);
            if (def != null) return def;
            return createFallback(size, style);
         }
      }
   }

   @Nullable
   private static FontRenderer createFromAssets(String path, float size, int style, boolean withOffset) {
      try {
         Font awtFont = awtFontCache.computeIfAbsent(path, p -> {
            try {
               InputStream stream = FontManager.class.getClassLoader().getResourceAsStream(p);
               if (stream == null) return null;
               Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
               stream.close();
               return font;
            } catch (Exception e) {
               e.printStackTrace();
               return null;
            }
         });
         if (awtFont == null) return null;

         Font derivedFont = awtFont.deriveFont(style, size);
         Font fallbackCJK = getCJKFallbackFont(style, size);

         if (withOffset) {
            if (fallbackCJK != null) {
               return new FontRenderer(derivedFont, fallbackCJK, size) {
                  @Override
                  public void drawString(MatrixStack stack, String s, float x, float y,
                                         float r, float g, float b, float a, boolean shadow) {
                     super.drawString(stack, s, x + getTranslateX(), y + getTranslateY(), r, g, b, a, shadow);
                  }
               };
            } else {
               return new FontRenderer(derivedFont, size) {
                  @Override
                  public void drawString(MatrixStack stack, String s, float x, float y,
                                         float r, float g, float b, float a, boolean shadow) {
                     super.drawString(stack, s, x + getTranslateX(), y + getTranslateY(), r, g, b, a, shadow);
                  }
               };
            }
         } else {
            if (fallbackCJK != null) {
               return new FontRenderer(derivedFont, fallbackCJK, size);
            } else {
               return new FontRenderer(derivedFont, size);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   @Nullable
   private static Font getCJKFallbackFont(int style, float size) {
      FontMode mode = getMode();
      if (mode == FontMode.MISANS || mode == FontMode.HYW || mode == FontMode.CUSTOM_OTF) return null;

      Font misansFont = awtFontCache.computeIfAbsent(MISANS_FONT, p -> {
         try {
            InputStream stream = FontManager.class.getClassLoader().getResourceAsStream(p);
            if (stream == null) return null;
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
            stream.close();
            return font;
         } catch (Exception e) { return null; }
      });
      if (misansFont != null) return misansFont.deriveFont(style, size);

      String[] systemCJKFonts = {
              "Microsoft YaHei", "SimHei", "PingFang SC",
              "Noto Sans CJK SC", "WenQuanYi Micro Hei"
      };
      for (String fontName : systemCJKFonts) {
         Font f = new Font(fontName, style, (int) size);
         if (f.canDisplay('中')) return f;
      }
      return null;
   }

   @Nullable
   private static FontRenderer createFromSystem(float size, int style) {
      String[] fontDirs = {
              System.getenv("WINDIR") != null ? System.getenv("WINDIR") + "\\Fonts" : null,
              "/usr/share/fonts",
              "/System/Library/Fonts",
              System.getProperty("user.home") + "/.local/share/fonts"
      };
      String[] preferredFonts = {"msyh", "arial", "segoeui", "roboto", "noto"};

      for (String dir : fontDirs) {
         if (dir == null) continue;
         File fontDir = new File(dir);
         if (!fontDir.exists() || !fontDir.isDirectory()) continue;
         File[] files = fontDir.listFiles();
         if (files == null) continue;
         for (String preferred : preferredFonts) {
            for (File file : files) {
               String name = file.getName().toLowerCase();
               if (name.contains(preferred) && (name.endsWith(".ttf") || name.endsWith(".otf"))) {
                  try {
                     Font font = Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(style, size);
                     return new FontRenderer(font, size) {
                        @Override
                        public void drawString(MatrixStack stack, String s, float x, float y,
                                               float r, float g, float b, float a, boolean shadow) {
                           super.drawString(stack, s, x + getTranslateX(), y + getTranslateY(), r, g, b, a, shadow);
                        }
                     };
                  } catch (Exception ignored) {}
               }
            }
         }
      }
      return null;
   }

   @NotNull
   private static FontRenderer createFallback(float size, int style) {
      Font fallback = new Font(Font.SANS_SERIF, style, (int) size);
      return new FontRenderer(fallback, size) {
         @Override
         public void drawString(MatrixStack stack, String s, float x, float y,
                                float r, float g, float b, float a, boolean shadow) {
            super.drawString(stack, s, x + getTranslateX(), y + getTranslateY(), r, g, b, a, shadow);
         }
      };
   }

   @NotNull
   private static FontRenderer createSafeIconFallback() {
      Font fallback = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
      return new FontRenderer(fallback, 8.0f) {
         @Override
         public void drawString(MatrixStack stack, String s, float x, float y,
                                float r, float g, float b, float a, boolean shadow) {
            super.drawString(stack, s, x + getTranslateX(), y + getTranslateY(), r, g, b, a, shadow);
         }
      };
   }

   private static int getStyle() {
      if (Fonts.INSTANCE == null) return Font.PLAIN;
      return ((Fonts.Style) Fonts.INSTANCE.style.getValue()).get();
   }

   private static float getTranslateX() {
      if (Fonts.INSTANCE == null) return 0;
      return Fonts.INSTANCE.translate.getValueInt();
   }

   private static float getTranslateY() {
      if (Fonts.INSTANCE == null) return 0;
      return Fonts.INSTANCE.shift.getValueInt();
   }

   private static String buildConfigHash() {
      if (Fonts.INSTANCE == null) return "default_8_0";
      return Fonts.INSTANCE.mode.getValue().name()
              + "_" + Fonts.INSTANCE.size.getValue()
              + "_" + Fonts.INSTANCE.style.getValue();
   }
}
