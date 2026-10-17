package dev.kissoo.mod.modules.impl.client;

import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;

public class Fonts extends Module {
   public static Fonts INSTANCE;

   public final EnumSetting<FontMode> mode = this.add(new EnumSetting<>("Mode", FontMode.DEFAULT));

   public final SliderSetting size = this.add(new SliderSetting("Size", 8.0, 1.0, 15.0, 1.0));
   public final SliderSetting shift = this.add(new SliderSetting("Shift", 0.0, -10.0, 10.0, 1.0));
   public final SliderSetting translate = this.add(new SliderSetting("Translate", 0.0, -10.0, 10.0, 1.0));

   public final EnumSetting<Style> style = this.add(new EnumSetting<>("Style", Style.PLAIN, () -> this.mode.is(FontMode.DEFAULT) || this.mode.is(FontMode.SYSTEM)));

   public Fonts() {
      super("Fonts", Module.Category.Client);
      this.setChinese("字体");
      INSTANCE = this;
   }

   @Override
   public void enable() {
      refresh();
   }

   public void refresh() {
      FontManager.refresh();
   }

   public void onSettingChanged() {
      if (this.isOn()) {
         FontManager.refresh();
      }
   }

   public enum FontMode {
      DEFAULT(true),
      MISANS(true),
      BOREL(true),
      HOPRC(true),
      HYW(true),
      SYSTEM(true),
      MINECRAFT(false),
      CUSTOM_OTF(true),
      LEXENDDECA(true),
      JURA(true),
      OXANIUM(true),
      ORBITRON(true);

      private final boolean custom;

      FontMode(boolean custom) {
         this.custom = custom;
      }

      public boolean isCustom() {
         return this.custom;
      }
   }

   public enum Style {
      PLAIN(0),
      BOLD(1),
      ITALIC(2);

      private final int awtStyle;

      Style(int awtStyle) {
         this.awtStyle = awtStyle;
      }

      public int get() {
         return awtStyle;
      }
   }
}