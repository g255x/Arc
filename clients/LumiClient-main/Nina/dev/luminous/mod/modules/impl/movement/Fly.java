package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.EntityVelocityUpdateEvent;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import net.minecraft.class_2596;
import net.minecraft.class_2708;
import net.minecraft.class_2824;
import net.minecraft.class_2848;
import net.minecraft.class_6374;
import net.minecraft.class_2848.class_2849;

public class Fly extends Module {
   public static Fly INSTANCE;
   public final EnumSetting<Mode> mode;
   private final SliderSetting speedConfig;
   private final SliderSetting vspeedConfig;
   private final BooleanSetting antiKickConfig;
   private final BooleanSetting up;
   private final BooleanSetting allowSneak;
   private final Timer antiKickTimer;
   private final Timer antiKick2Timer;
   private boolean grimTriggered;
   private boolean grimStartQueued;
   private int grimTicks;
   private boolean grimSkidActive;
   private boolean grimSkidQueuedStart;
   private int grimSkidTicks;
   private boolean grimSkidFlushing;
   private final Deque<class_2596<?>> grimSkidQueue;

   public Fly() {
      super("Fly", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", Fly.Mode.Vanilla));
      this.speedConfig = this.add(new SliderSetting("Speed", (double)2.5F, 0.1, (double)10.0F));
      this.vspeedConfig = this.add(new SliderSetting("VerticalSpeed", (double)1.0F, 0.1, (double)5.0F));
      this.antiKickConfig = this.add((new BooleanSetting("AntiKick", true)).setParent());
      BooleanSetting var10006 = this.antiKickConfig;
      Objects.requireNonNull(var10006);
      this.up = this.add(new BooleanSetting("Up", true, var10006::isOpen));
      this.allowSneak = this.add(new BooleanSetting("AllowSneak", false));
      this.antiKickTimer = new Timer();
      this.antiKick2Timer = new Timer();
      this.grimSkidQueue = new ArrayDeque();
      this.setChinese("飞行");
      INSTANCE = this;
   }

   public void onLogin() {
      this.antiKickTimer.reset();
      this.antiKick2Timer.reset();
      this.resetGrim();
      this.resetGrimSkid();
   }

   public void onEnable() {
      if (!nullCheck()) {
         this.antiKickTimer.reset();
         this.antiKick2Timer.reset();
         this.resetGrim();
         this.resetGrimSkid();
      }
   }

   public void onDisable() {
      this.flushGrimSkidQueue();
      this.resetGrim();
      this.resetGrimSkid();
   }

   public String getInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.mode.getValue() == Fly.Mode.Grim) {
         this.onUpdateGrim();
      } else if (this.mode.getValue() == Fly.Mode.GrimSkid) {
         this.onUpdateGrimSkid();
      } else {
         this.onUpdateVanilla();
      }

   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.isPre()) {
         if (!nullCheck()) {
            if (this.mode.getValue() == Fly.Mode.GrimSkid) {
               if (this.grimSkidQueuedStart) {
                  if (++this.grimSkidTicks >= 8) {
                     mc.method_1562().method_52787(new class_2848(mc.field_1724, class_2849.field_12982));
                     this.grimSkidTicks = 0;
                     this.grimSkidQueuedStart = false;
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onVelocity(EntityVelocityUpdateEvent event) {
      if (!nullCheck()) {
         if (this.mode.getValue() == Fly.Mode.GrimSkid) {
            if (event.getEntity() == mc.field_1724) {
               if (!this.grimSkidActive && this.grimSkidQueue.isEmpty()) {
                  event.cancel();
                  this.grimSkidTicks = 0;
                  this.grimSkidActive = true;
               }
            }
         } else if (this.mode.getValue() == Fly.Mode.Grim) {
            if (event.getEntity() == mc.field_1724) {
               if (!this.grimTriggered) {
                  event.cancel();
                  this.grimTriggered = true;
                  this.grimStartQueued = true;
                  this.grimTicks = 0;
               }

            }
         }
      }
   }

   @EventListener
   public void onReceive(PacketEvent.Receive event) {
      if (this.mode.getValue() == Fly.Mode.GrimSkid) {
         if (event.getPacket() instanceof class_2708 && this.grimSkidActive && !this.grimSkidQueue.isEmpty()) {
            this.flushGrimSkidQueue();
         }

      } else if (this.mode.getValue() == Fly.Mode.Grim) {
         if (event.getPacket() instanceof class_2708) {
            this.grimStartQueued = true;
            this.grimTicks = 0;
         }

      }
   }

   @EventListener
   public void onSend(PacketEvent.Send event) {
      if (!this.grimSkidFlushing) {
         if (!nullCheck()) {
            if (this.mode.getValue() == Fly.Mode.GrimSkid) {
               class_2596<?> packet = event.getPacket();
               if (packet instanceof class_6374 && this.grimSkidActive) {
                  boolean wasEmpty = this.grimSkidQueue.isEmpty();
                  event.cancel();
                  this.grimSkidQueue.add(packet);
                  if (wasEmpty) {
                     this.grimSkidQueuedStart = true;
                     this.grimSkidTicks = 0;
                  }

               } else {
                  if (packet instanceof class_2824 && this.grimSkidActive && !this.grimSkidQueue.isEmpty()) {
                     this.flushGrimSkidQueue();
                  }

               }
            }
         }
      }
   }

   @EventListener(
      priority = -100
   )
   public void keyboard(KeyboardInputEvent event) {
      if (!this.allowSneak.getValue()) {
         mc.field_1724.field_3913.field_3903 = false;
      }

   }

   private void onUpdateVanilla() {
      if (this.antiKickTimer.passed(3900L) && this.antiKickConfig.getValue() && !mc.field_1724.method_24828()) {
         MovementUtil.setMotionY(-0.04);
         this.antiKickTimer.reset();
      } else if (this.antiKick2Timer.passed(4000L) && this.antiKickConfig.getValue() && !mc.field_1724.method_24828() && this.up.getValue()) {
         MovementUtil.setMotionY(0.04);
         this.antiKick2Timer.reset();
      } else {
         MovementUtil.setMotionY((double)0.0F);
         if (mc.field_1690.field_1903.method_1434()) {
            MovementUtil.setMotionY(this.vspeedConfig.getValue());
         } else if (mc.field_1690.field_1832.method_1434()) {
            MovementUtil.setMotionY(-this.vspeedConfig.getValue());
         }
      }

      double[] move = MovementUtil.directionSpeed((double)this.speedConfig.getValueFloat());
      MovementUtil.setMotionX(move[0]);
      MovementUtil.setMotionZ(move[1]);
   }

   private void onUpdateGrim() {
      this.onUpdateVanilla();
      if (this.grimStartQueued) {
         if (mc.field_1724.method_24828()) {
            this.grimTicks = 0;
         } else if (++this.grimTicks >= 8) {
            mc.method_1562().method_52787(new class_2848(mc.field_1724, class_2849.field_12982));
            this.grimTicks = 0;
            this.grimStartQueued = false;
         }
      }
   }

   private void onUpdateGrimSkid() {
      this.onUpdateVanilla();
   }

   private void resetGrim() {
      this.grimTriggered = false;
      this.grimStartQueued = false;
      this.grimTicks = 0;
   }

   private void resetGrimSkid() {
      this.grimSkidActive = false;
      this.grimSkidQueuedStart = false;
      this.grimSkidTicks = 0;
      this.grimSkidFlushing = false;
      this.grimSkidQueue.clear();
   }

   private void flushGrimSkidQueue() {
      this.grimSkidQueuedStart = false;
      this.grimSkidTicks = 0;
      if (!this.grimSkidQueue.isEmpty()) {
         if (nullCheck()) {
            this.grimSkidQueue.clear();
         } else {
            this.grimSkidFlushing = true;

            while(!this.grimSkidQueue.isEmpty()) {
               mc.method_1562().method_52787((class_2596)this.grimSkidQueue.poll());
            }

            this.grimSkidFlushing = false;
         }
      }
   }

   public static enum Mode {
      Vanilla,
      Grim,
      GrimSkid;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Vanilla, Grim, GrimSkid};
      }
   }
}
