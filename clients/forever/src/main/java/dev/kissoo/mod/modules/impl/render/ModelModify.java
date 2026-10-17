package dev.kissoo.mod.modules.impl.render;

import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;

public class ModelModify extends Module {
   public static ModelModify INSTANCE;

   public final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Sneak));

   public ModelModify() {
      super("ModelModify", "Modify player model pose", Module.Category.Render);
      this.setChinese("模型修改");
      INSTANCE = this;
   }

   public boolean isSneak() {
      return this.isOn() && this.mode.is(Mode.Sneak);
   }

   public boolean isSit() {
      return this.isOn() && this.mode.is(Mode.Sit);
   }

   public boolean isSwim() {
      return this.isOn() && this.mode.is(Mode.Swim);
   }

   public boolean isProne() {
      return this.isOn() && this.mode.is(Mode.Prone);
   }

   public enum Mode {
      Sneak,
      Sit,
      Swim,
      Prone
   }
}
