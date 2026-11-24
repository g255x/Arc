package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;

public class MacePlus extends Module {
   public static MacePlus INSTANCE;
   private final EnumSetting<Mode> mode;
   private final SliderSetting upwardVelocity;
   private final SliderSetting forwardVelocity;
   private final SliderSetting delay;
   private final BooleanSetting silentSwitch;
   private final BooleanSetting onlyGround;
   private final Timer timer;

   public MacePlus() {
      super("MacePlus", "Mace wind charge boost", Module.Category.Combat);
      this.setChinese("重锤增强");
      INSTANCE = this;
      this.timer = new Timer();
      this.mode = this.add(new EnumSetting("Mode", MacePlus.Mode.WindJump));
      this.upwardVelocity = this.add(new SliderSetting("UpwardVelocity", (double)2.5F, (double)0.5F, (double)5.0F, (double)0.1F));
      this.forwardVelocity = this.add(new SliderSetting("ForwardVelocity", (double)2.5F, (double)0.5F, (double)5.0F, (double)0.1F));
      this.delay = this.add(new SliderSetting("Delay", (double)1.0F, (double)0.1F, (double)5.0F, (double)0.1F));
      this.silentSwitch = this.add(new BooleanSetting("SilentSwitch", false));
      this.onlyGround = this.add(new BooleanSetting("OnlyGround", true));
   }

   @EventListener
   public void onTick(ClientTickEvent clientTickEvent) {
      if (!nullCheck()) {
         if (this.timer.passedS(this.delay.getValue())) {
            if (!this.onlyGround.getValue() || mc.player.isOnGround()) {
               int n = InventoryUtil.findItem(Items.WIND_CHARGE);
               if (n != -1) {
                  int n2 = mc.player.getInventory().selectedSlot;
                  if (this.silentSwitch.getValue()) {
                     sendSlotPacket(n);
                  } else {
                     InventoryUtil.switchToSlot(n);
                  }

                  float f = mc.player.getYaw();
                  float f2 = mc.player.getPitch();
                  sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, f, f2));
                  Mode mode = this.mode.getValue();
                  if (mode == MacePlus.Mode.WindJump) {
                     mc.player.addVelocity((double)0.0F, this.upwardVelocity.getValue(), (double)0.0F);
                  } else if (mode == MacePlus.Mode.Forward) {
                     double d = Math.toRadians((double)mc.player.getYaw());
                     double d2 = this.forwardVelocity.getValue();
                     double d3 = -Math.sin(d) * d2;
                     double d4 = Math.cos(d) * d2;
                     mc.player.addVelocity(d3, (double)0.0F, d4);
                  } else {
                     mc.player.addVelocity((double)0.0F, -this.upwardVelocity.getValue(), (double)0.0F);
                  }

                  if (this.silentSwitch.getValue()) {
                     sendSlotPacket(n2);
                  } else {
                     InventoryUtil.switchToSlot(n2);
                  }

                  this.timer.reset();
               }
            }
         }
      }
   }

   private static void sendSlotPacket(int n) {
      UpdateSelectedSlotC2SPacket updateSelectedSlotC2SPacket = new UpdateSelectedSlotC2SPacket(n);
      mc.getNetworkHandler().sendPacket(updateSelectedSlotC2SPacket);
   }

   public String getInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   public static enum Mode {
      WindJump,
      Forward,
      Smash;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{WindJump, Forward, Smash};
      }
   }
}
