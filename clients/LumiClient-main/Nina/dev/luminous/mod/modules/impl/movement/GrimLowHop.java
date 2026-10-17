package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class GrimLowHop extends Module {
   public static GrimLowHop INSTANCE;
   private final BooleanSetting logging = this.add(new BooleanSetting("Logging", false));
   private final SliderSetting startTicks = this.add(new SliderSetting("StartTick", 2, 0, 10));
   private final SliderSetting skipTicks = this.add(new SliderSetting("SkipTicks", 2, 1, 10));
   private final SliderSetting ticks = this.add(new SliderSetting("Ticks", 3, 1, 10));
   private int airTicks;
   private boolean canTimer;
   private boolean timer;

   public GrimLowHop() {
      super("GrimLowHop", Module.Category.Movement);
      this.setChinese("严峻低跳");
      INSTANCE = this;
   }

   public String getInfo() {
      int var10000 = this.skipTicks.getValueInt();
      return var10000 + "+" + this.ticks.getValueInt();
   }

   public void onEnable() {
      this.airTicks = 0;
      this.canTimer = false;
      this.timer = false;
      Alien.TIMER.clearSkipTicks();
   }

   public void onDisable() {
      this.airTicks = 0;
      this.canTimer = false;
      this.timer = false;
      Alien.TIMER.clearSkipTicks();
   }

   private void log(String message) {
      if (this.logging.getValue()) {
         CommandManager.sendMessage(message);
      }

   }

   @EventListener
   public void onPreTick(ClientTickEvent event) {
      if (!event.isPost() && !nullCheck()) {
         if (this.airTicks >= this.startTicks.getValueInt() && !this.canTimer) {
            Alien.TIMER.addSkipTicks(this.skipTicks.getValueInt());
            this.canTimer = true;
            this.timer = false;
         }

      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         if (mc.field_1724.method_24828()) {
            this.airTicks = 0;
            this.canTimer = false;
            this.timer = false;
         } else {
            ++this.airTicks;
            if (this.canTimer && !this.timer) {
               this.timer = true;
               int var10001 = this.airTicks;
               this.log("GrimLowHop trigger: airTicks=" + var10001 + " skipTicks=" + this.skipTicks.getValueInt() + " ticks=" + this.ticks.getValueInt());
               int count = this.ticks.getValueInt();

               for(int i = 0; i < count; ++i) {
                  mc.field_1724.method_5773();
               }

               this.log("GrimLowHop done");
            }

         }
      }
   }

   @EventListener(
      priority = -200
   )
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (!nullCheck()) {
         mc.field_1724.field_3913.field_3904 = true;
      }
   }
}
