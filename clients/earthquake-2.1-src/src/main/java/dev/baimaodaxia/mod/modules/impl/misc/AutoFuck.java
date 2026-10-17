package dev.baimaodaxia.mod.modules.impl.misc;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

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
      if (!nullCheck() && mc.getNetworkHandler() != null) {
         if (this.sneaking && !mc.options.sneakKey.isPressed()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
         }

         this.sneaking = false;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck() && mc.getNetworkHandler() != null) {
         if (this.timer.passedMs(this.delay.getValue())) {
            this.sneaking = !this.sneaking;
            ClientCommandC2SPacket.Mode mode = this.sneaking ? Mode.PRESS_SHIFT_KEY : Mode.RELEASE_SHIFT_KEY;
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, mode));
            this.timer.reset();
         }
      }
   }
}
