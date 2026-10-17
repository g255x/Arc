package dev.Lumi.mod.modules.impl.hud;

import dev.Lumi.mod.modules.Module;
import dev.Lumi.mod.modules.settings.impl.BooleanSetting;

public class HudSetting extends Module {
   public static HudSetting INSTANCE;
   public final BooleanSetting font = this.add(new BooleanSetting("Font", true));
   public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));

   public HudSetting() {
      super("HUDSetting", Module.Category.Client);
      this.setChinese("HUD设置");
      INSTANCE = this;
   }

   public static boolean useFont() {
      return INSTANCE == null || INSTANCE.font.getValue();
   }

   public static boolean useShadow() {
      return INSTANCE == null || INSTANCE.shadow.getValue();
   }

   public void enable() {
      this.state = true;
   }

   public void disable() {
      this.state = true;
   }

   public boolean isOn() {
      return true;
   }
}
