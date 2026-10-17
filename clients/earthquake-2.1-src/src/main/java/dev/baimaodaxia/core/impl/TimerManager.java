package dev.baimaodaxia.core.impl;

import dev.baimaodaxia.mod.modules.impl.player.TimerModule;

public class TimerManager {
   public float timer = 1.0F;
   public float lastTimer;
   private int skipTicks;

   public void set(float factor) {
      if (factor < 0.1F) {
         factor = 0.1F;
      }

      this.timer = factor;
   }

   public void reset() {
      this.lastTimer = this.timer = this.getDefault();
      this.skipTicks = 0;
   }

   public void tryReset() {
      if (this.lastTimer != this.getDefault()) {
         this.reset();
      }

   }

   public float get() {
      return this.timer;
   }

   public float getDefault() {
      return TimerModule.INSTANCE.isOn() ? (TimerModule.INSTANCE.boostKey.isPressed() ? TimerModule.INSTANCE.boost.getValueFloat() : TimerModule.INSTANCE.multiplier.getValueFloat()) : 1.0F;
   }

   public void addSkipTicks(int ticks) {
      if (ticks > 0) {
         this.skipTicks += ticks;
      }
   }

   public boolean consumeSkipTick() {
      if (this.skipTicks <= 0) {
         return false;
      } else {
         --this.skipTicks;
         return true;
      }
   }

   public void clearSkipTicks() {
      this.skipTicks = 0;
   }
}
