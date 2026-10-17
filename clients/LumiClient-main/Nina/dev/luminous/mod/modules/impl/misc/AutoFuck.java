package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.class_2848;
import net.minecraft.class_2848.class_2849;

public class AutoFuck extends Module {
   public static AutoFuck INSTANCE;
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 500, 0, 2000));
   private final Timer timer = new Timer();
   private boolean sneaking = false;

   public AutoFuck() {
      super("AutoFuck", Module.Category.Misc);
      this.setChinese("打滑");
      INSTANCE = this;
   }

   public void onEnable() {
      this.timer.reset();
      this.sneaking = false;
   }

   public void onDisable() {
      if (!nullCheck() && mc.method_1562() != null) {
         if (this.sneaking && !mc.field_1690.field_1832.method_1434()) {
            mc.method_1562().method_52787(new class_2848(mc.field_1724, class_2849.field_12984));
         }

         this.sneaking = false;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck() && mc.method_1562() != null) {
         if (this.timer.passedMs(this.delay.getValue())) {
            this.sneaking = !this.sneaking;
            class_2848.class_2849 mode = this.sneaking ? class_2849.field_12979 : class_2849.field_12984;
            mc.method_1562().method_52787(new class_2848(mc.field_1724, mode));
            this.timer.reset();
         }
      }
   }
}
