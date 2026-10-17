package dev.kissoo.mod.modules.settings.impl;

import dev.kissoo.mod.modules.settings.Setting;
import java.util.function.BooleanSupplier;

public class SliderSetting extends Setting {
   private final double defaultValue;
   private final double minValue;
   private final double maxValue;
   private final double increment;
   private double value;
   private Double runtimeMaxValue;
   private String suffix = "";
   public Runnable task = null;
   public boolean injectTask = false;

   public SliderSetting(String name, double value, double min, double max, double increment) {
      super(name);
      this.value = value;
      this.defaultValue = value;
      this.minValue = min;
      this.maxValue = max;
      this.increment = increment;
   }

   public SliderSetting(String name, double value, double min, double max) {
      this(name, value, min, max, 0.1);
   }

   public SliderSetting(String name, int value, int min, int max) {
      this(name, value, min, max, 1.0);
   }

   public SliderSetting(String name, double value, double min, double max, double increment, BooleanSupplier visibilityIn) {
      super(name, visibilityIn);
      this.value = value;
      this.defaultValue = value;
      this.minValue = min;
      this.maxValue = max;
      this.increment = increment;
   }

   public SliderSetting(String name, double value, double min, double max, BooleanSupplier visibilityIn) {
      this(name, value, min, max, 0.1, visibilityIn);
   }

   public SliderSetting(String name, int value, int min, int max, BooleanSupplier visibilityIn) {
      this(name, value, min, max, 1.0, visibilityIn);
   }

   public double getDefaultValue() {
      return this.defaultValue;
   }

   public double getValue() {
      return this.value;
   }

   public float getValueFloat() {
      return (float)this.value;
   }

   public int getValueInt() {
      return (int)this.value;
   }

   public void setValue(double value) {
      double rounded = Math.round(value / this.getIncrement()) * this.getIncrement();
      this.value = Math.max(this.getMin(), Math.min(this.getMax(), rounded));
      if (this.injectTask) {
         this.task.run();
      }
   }

   public void setValueUnclamped(double value) {
      this.value = Math.round(value / this.getIncrement()) * this.getIncrement();
      if (this.injectTask) {
         this.task.run();
      }
   }

   public double getMin() {
      return this.minValue;
   }

   public double getMax() {
      return this.runtimeMaxValue == null ? this.maxValue : this.runtimeMaxValue;
   }

   public SliderSetting setRuntimeMax(double maxValue) {
      double effectiveMax = Math.max(this.minValue, maxValue);
      this.runtimeMaxValue = effectiveMax;
      if (this.value > effectiveMax) {
         this.setValue(effectiveMax);
      }
      return this;
   }

   public double getIncrement() {
      return this.increment;
   }

   public double getRange() {
      return this.getMax() - this.getMin();
   }

   public String getSuffix() {
      return this.suffix;
   }

   public SliderSetting setSuffix(String suffix) {
      this.suffix = suffix;
      return this;
   }

   public SliderSetting injectTask(Runnable task) {
      this.task = task;
      this.injectTask = true;
      return this;
   }
}
