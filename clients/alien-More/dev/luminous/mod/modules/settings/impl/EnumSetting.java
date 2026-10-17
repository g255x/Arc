package dev.luminous.mod.modules.settings.impl;

import dev.luminous.mod.modules.settings.EnumConverter;
import dev.luminous.mod.modules.settings.Setting;
import java.util.function.BooleanSupplier;

public class EnumSetting extends Setting {
   private final Enum defaultValue;
   private Enum value;
   private Runnable task = null;
   private boolean injectTask = false;

   public EnumSetting(String name, Enum defaultValue) {
      super(name);
      this.value = defaultValue;
      this.defaultValue = defaultValue;
   }

   public EnumSetting(String name, Enum defaultValue, BooleanSupplier visibilityIn) {
      super(name, visibilityIn);
      this.value = defaultValue;
      this.defaultValue = defaultValue;
   }

   public void loadSetting(String enumString) {
      EnumConverter converter = new EnumConverter();
      if (enumString == null) {
         this.value = this.defaultValue;
      } else {
         Enum<?> value = converter.get(this.defaultValue, enumString);
         this.value = value != null ? value : this.defaultValue;
      }
   }

   public void increaseEnum() {
      this.value = EnumConverter.increaseEnum(this.value);
      if (this.injectTask) {
         this.task.run();
      }

   }

   public void resetValue() {
      this.value = this.defaultValue;
   }

   public Enum getValue() {
      return this.value;
   }

   public void setValue(Enum value) {
      this.value = value;
   }

   public void setEnumValue(String value) {
      for(Enum e : (Enum[])this.value.getDeclaringClass().getEnumConstants()) {
         if (e.name().equalsIgnoreCase(value)) {
            this.value = e;
            if (this.injectTask) {
               this.task.run();
            }
         }
      }

   }

   public EnumSetting injectTask(Runnable task) {
      this.task = task;
      this.injectTask = true;
      return this;
   }

   public boolean is(Enum mode) {
      return this.getValue() == mode;
   }

   public Enum getDefaultValue() {
      return this.defaultValue;
   }
}
