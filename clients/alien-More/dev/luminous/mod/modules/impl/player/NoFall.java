package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.asm.accessors.IPlayerMoveC2SPacket;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.BowBomb;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2596;
import net.minecraft.class_2708;
import net.minecraft.class_2828;
import net.minecraft.class_2848;
import net.minecraft.class_3675;
import net.minecraft.class_2848.class_2849;

public class NoFall extends Module {
   private final EnumSetting mode;
   private final SliderSetting getDistance;
   private boolean grimCheck;
   private boolean grimS08Check;
   private boolean grimJump;
   private boolean grimReceiveS08;
   private boolean grimJumpSuccess;
   private int grimTicks;

   public NoFall() {
      super("NoFall", "Prevents fall damage.", Module.Category.Player);
      this.mode = this.add(new EnumSetting("Mode", NoFall.NoFallMode.Packet));
      this.getDistance = this.add(new SliderSetting("Distance", (double)3.0F, (double)0.0F, (double)8.0F, 0.1));
      this.setChinese("没有摔落伤害");
   }

   public void onLogin() {
      this.resetGrim();
   }

   public void onEnable() {
      this.resetGrim();
   }

   public void onDisable() {
      this.resetGrim();
   }

   public String getInfo() {
      return ((NoFallMode)this.mode.getValue()).name();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.isPre()) {
         if (this.mode.is(NoFall.NoFallMode.Grim)) {
            if (!nullCheck()) {
               if (!mc.method_1542()) {
                  if (this.grimReceiveS08) {
                     if (mc.field_1724.method_18798().field_1351 > 0.2 && mc.field_1690.field_1903.method_1434() && !mc.field_1724.method_24828()) {
                        this.grimJumpSuccess = true;
                     }

                     if (this.grimJumpSuccess && ++this.grimTicks >= 20) {
                        this.resetGrim();
                     }
                  }

                  if (mc.field_1724.field_6017 >= this.getDistance.getValueFloat()) {
                     this.grimCheck = true;
                  }

                  if (mc.field_1724.method_24828() && mc.field_1724.field_5992 && this.grimCheck) {
                     this.grimJump = true;
                     this.grimS08Check = true;
                     this.sendGrim();
                  }

               }
            }
         }
      }
   }

   @EventListener(
      priority = -200
   )
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (this.mode.is(NoFall.NoFallMode.Grim)) {
         if (!nullCheck()) {
            if (this.grimJump) {
               mc.field_1690.field_1903.method_23481(true);
            } else {
               mc.field_1690.field_1903.method_23481(class_3675.method_15987(mc.method_22683().method_4490(), class_3675.method_15981(mc.field_1690.field_1903.method_1428()).method_1444()));
            }

         }
      }
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (!this.hasElytra()) {
            if (this.mode.is(NoFall.NoFallMode.Packet)) {
               class_2596<?> packet = event.getPacket();
               if (packet instanceof class_2828) {
                  class_2828 packet2 = (class_2828)packet;
                  if (mc.field_1724.field_6017 >= (float)this.getDistance.getValue() && !BowBomb.send) {
                     ((IPlayerMoveC2SPacket)packet2).setOnGround(true);
                  }
               }

            }
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.mode.is(NoFall.NoFallMode.Grim)) {
         if (!nullCheck()) {
            if (event.getPacket() instanceof class_2708) {
               if (this.grimS08Check) {
                  this.grimReceiveS08 = true;
               }

            }
         }
      }
   }

   private boolean hasElytra() {
      for(class_1799 is : mc.field_1724.method_5661()) {
         if (is.method_7909() == class_1802.field_8833) {
            return true;
         }
      }

      return false;
   }

   private void resetGrim() {
      this.grimCheck = false;
      this.grimS08Check = false;
      this.grimJump = false;
      this.grimReceiveS08 = false;
      this.grimJumpSuccess = false;
      this.grimTicks = 0;
   }

   private void sendGrim() {
      try {
         mc.method_1562().method_52787(new class_2848(mc.field_1724, class_2849.field_12982));
      } catch (Throwable var2) {
      }

   }

   private boolean isJumpPhysicallyDown() {
      return class_3675.method_15987(mc.method_22683().method_4490(), class_3675.method_15981(mc.field_1690.field_1903.method_1428()).method_1444());
   }

   public static enum NoFallMode {
      Packet,
      Grim;

      // $FF: synthetic method
      private static NoFallMode[] $values() {
         return new NoFallMode[]{Packet, Grim};
      }
   }
}
