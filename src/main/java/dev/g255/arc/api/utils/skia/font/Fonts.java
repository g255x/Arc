package dev.g255.arc.api.utils.skia.font;

import io.github.humbleui.skija.Font;

public class Fonts {
   public static void loadAll() {
      FontHelper.preloadFonts("font.ttf", "icon.ttf");
   }

   public static Font getMiSans(float size) {
      return FontHelper.load("font.ttf", size);
   }

   /**
    * 原 productsans-medium.ttf 与 font.ttf（旧 MiSans-Regular.ttf）字节完全相同，已合并为单一字体文件，
    * 此处保留方法名以免改动调用方（Skia#getFont 用它区分粗体档位）。
    */
   public static Font getProductSansMedium(float size) {
      return getMiSans(size);
   }

   public static Font getCjkRegular(float size) {
      try {
         return FontHelper.loadSystem("C:\\Windows\\Fonts\\msyh.ttc", size);
      } catch (Exception var2) {
         return getMiSans(size);
      }
   }

   public static Font getCjkBold(float size) {
      try {
         return FontHelper.loadSystem("C:\\Windows\\Fonts\\msyhbd.ttc", size);
      } catch (Exception var2) {
         return getProductSansMedium(size);
      }
   }

   public static Font getNavenIcon(float size) {
      return FontHelper.load("icon.ttf", size);
   }
}
