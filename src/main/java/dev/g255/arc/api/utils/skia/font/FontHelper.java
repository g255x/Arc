package dev.g255.arc.api.utils.skia.font;

import dev.g255.arc.api.utils.skia.utils.SkiaUtils;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Typeface;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class FontHelper {
   private static final Map<String, Typeface> CACHE = new HashMap();
   private static final Map FONTS = Collections.synchronizedMap(new LinkedHashMap(64, 0.75F, true));

   public static Font load(String string, float f) {
      String string2 = string + "\u0000" + Float.floatToIntBits(f);
      Font font = (Font)FONTS.get(string2);
      if (font != null) {
         return font;
      } else {
         font = new Font(getTypeface(string), f);
         if (FONTS.size() >= 256) {
            FONTS.clear();
         }

         FONTS.put(string2, font);
         return font;
      }
   }

   public static void preloadFonts(String... fonts) {
      for(String font : fonts) {
         getTypeface(font);
      }

   }

   public static Font loadSystem(String string, float f) {
      String string2 = "S\u0000" + string + "\u0000" + Float.floatToIntBits(f);
      Font font = (Font)FONTS.get(string2);
      if (font != null) {
         return font;
      } else {
         font = new Font(getSystemTypeface(string), f);
         if (FONTS.size() >= 256) {
            FONTS.clear();
         }

         FONTS.put(string2, font);
         return font;
      }
   }

   private static Typeface getTypeface(String font) {
      return (Typeface)CACHE.computeIfAbsent(font, FontHelper::loadTypeface);
   }

   private static Typeface loadTypeface(String font) {
      Optional direct = SkiaUtils.convertToData("/assets/arc/fonts/" + font);
      if (direct.isPresent()) {
         return Typeface.makeFromData((Data)direct.get());
      } else {
         throw new IllegalArgumentException("Font not found: " + font);
      }
   }

   private static Typeface getSystemTypeface(String path) {
      return (Typeface)CACHE.computeIfAbsent("system:" + path, (key) -> {
         File file = new File(path);
         if (!file.exists()) {
            throw new IllegalArgumentException("System font not found: " + path);
         } else {
            return Typeface.makeFromFile(path);
         }
      });
   }
}
