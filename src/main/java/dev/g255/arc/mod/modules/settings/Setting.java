package dev.g255.arc.mod.modules.settings;

import java.util.function.BooleanSupplier;

public abstract class Setting {
   private final BooleanSupplier visibility;
   private final String name;
   private boolean hidden;

   public Setting(String name) {
      this.name = name;
      this.visibility = null;
   }

   public Setting(String name, BooleanSupplier visibilityIn) {
      this.name = name;
      this.visibility = visibilityIn;
   }

   public boolean isVisible() {
      if (this.hidden) {
         return false;
      } else {
         return this.visibility == null || this.visibility.getAsBoolean();
      }
   }

   public void setHidden() {
      this.hidden = true;
   }

   public String getName() {
      return this.name;
   }

   public String getDisplayName() {
      return ChineseTranslator.setting(this.name);
   }
}
