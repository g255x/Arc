package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.MoveEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.TickEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.player.MovementUtil;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import io.netty.util.internal.ConcurrentSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class PacketFly extends Module {
   public static PacketFly INSTANCE;
   public final EnumSetting<Mode> mode;
   public final SliderSetting factor;
   public final EnumSetting<Phase> phase;
   public final EnumSetting<Type> type;
   public final BooleanSetting antiKick;
   public final BooleanSetting noRotation;
   public final BooleanSetting noMovePacket;
   public final BooleanSetting bbOffset;
   public final SliderSetting invalidY;
   public final SliderSetting invalids;
   public final SliderSetting sendTeleport;
   public final SliderSetting concealY;
   public final SliderSetting conceal;
   public final SliderSetting ySpeed;
   public final SliderSetting xzSpeed;
   public final BooleanSetting elytra;
   public final BooleanSetting xzJitter;
   public final BooleanSetting yJitter;
   public final BooleanSetting zeroSpeed;
   public final BooleanSetting zeroY;
   public final BooleanSetting zeroTeleport;
   public final SliderSetting zoomer;
   public final Map<Integer, TimeVec> posLooks;
   public final Set<Packet<?>> playerPackets;
   public final AtomicInteger teleportID;
   public Vec3d vecDelServer;
   public int packetCounter;
   public boolean zoomies;
   public float lastFactor;
   public int zoomTimer;

   public PacketFly() {
      super("PacketFly", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", PacketFly.Mode.Factor));
      this.factor = this.add(new SliderSetting("Factor", (double)1.0F, (double)0.0F, (double)10.0F));
      this.phase = this.add(new EnumSetting("Phase", PacketFly.Phase.Full));
      this.type = this.add(new EnumSetting("Type", PacketFly.Type.Up));
      this.antiKick = this.add(new BooleanSetting("AntiKick", true));
      this.noRotation = this.add(new BooleanSetting("NoRotation", false));
      this.noMovePacket = this.add(new BooleanSetting("NoMovePacket", false));
      this.bbOffset = this.add(new BooleanSetting("BB-Offset", false));
      this.invalidY = this.add(new SliderSetting("Invalid-Offset", 1337, 0, 1337));
      this.invalids = this.add(new SliderSetting("Invalids", 1, 0, 10));
      this.sendTeleport = this.add(new SliderSetting("Teleport", 1, 0, 10));
      this.concealY = this.add(new SliderSetting("C-Y", (double)0.0F, (double)-256.0F, (double)256.0F));
      this.conceal = this.add(new SliderSetting("C-Multiplier", (double)1.0F, (double)0.0F, (double)2.0F));
      this.ySpeed = this.add(new SliderSetting("Y-Multiplier", (double)1.0F, (double)0.0F, (double)2.0F));
      this.xzSpeed = this.add(new SliderSetting("X/Z-Multiplier", (double)1.0F, (double)0.0F, (double)2.0F));
      this.elytra = this.add(new BooleanSetting("Elytra", false));
      this.xzJitter = this.add(new BooleanSetting("Jitter-XZ", false));
      this.yJitter = this.add(new BooleanSetting("Jitter-Y", false));
      this.zeroSpeed = this.add(new BooleanSetting("Zero-Speed", false));
      this.zeroY = this.add(new BooleanSetting("Zero-Y", false));
      this.zeroTeleport = this.add(new BooleanSetting("Zero-Teleport", true));
      this.zoomer = this.add(new SliderSetting("Zoomies", 3, 0, 10));
      this.posLooks = new ConcurrentHashMap();
      this.playerPackets = new ConcurrentSet();
      this.teleportID = new AtomicInteger();
      this.zoomTimer = 0;
      this.setChinese("发包飞行");
      INSTANCE = this;
   }

   public void onLogin() {
      this.disable();
      this.clearValues();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.posLooks.entrySet().removeIf((entry) -> System.currentTimeMillis() - ((TimeVec)entry.getValue()).getTime() > TimeUnit.SECONDS.toMillis(30L));
   }

   @EventListener
   public void invoke(TickEvent event) {
      if (event.isPre() && this.mode.getValue() != PacketFly.Mode.Compatibility) {
         double ySpeed = (double)0.0F;
         MovementUtil.setMotionX((double)0.0F);
         MovementUtil.setMotionY((double)0.0F);
         MovementUtil.setMotionZ((double)0.0F);
         if (this.mode.getValue() != PacketFly.Mode.Setback && this.teleportID.get() == 0) {
            if (this.checkPackets(6)) {
               this.sendPackets((double)0.0F, (double)0.0F, (double)0.0F, true);
            }

            return;
         }

         boolean isPhasing = this.isPlayerCollisionBoundingBoxEmpty();
         if (!mc.player.input.jumping || !isPhasing && MovementUtil.isMoving()) {
            if (!mc.player.input.sneaking) {
               if (!isPhasing) {
                  if (this.checkPackets(4)) {
                     double var10000 = this.antiKick.getValue() ? -0.04 : (double)0.0F;
                  } else {
                     double var15 = (double)0.0F;
                  }
               } else {
                  ySpeed = (double)0.0F;
               }
            } else {
               ySpeed = this.yJitter.getValue() && this.zoomies ? -0.061 : -0.062;
            }
         } else {
            ySpeed = this.antiKick.getValue() && !isPhasing ? (this.checkPackets(this.mode.getValue() == PacketFly.Mode.Setback ? 10 : 20) ? -0.032 : 0.062) : (this.yJitter.getValue() && this.zoomies ? 0.061 : 0.062);
         }

         if (this.phase.getValue() == PacketFly.Phase.Full && isPhasing && MovementUtil.isMoving() && ySpeed != (double)0.0F) {
            ySpeed /= (double)2.5F;
         }

         double high = this.xzJitter.getValue() && this.zoomies ? (double)0.25F : 0.26;
         double low = this.xzJitter.getValue() && this.zoomies ? 0.03 : 0.031;
         double[] dirSpeed = MovementUtil.directionSpeed(this.phase.getValue() == PacketFly.Phase.Full && isPhasing ? low : high);
         if (this.mode.getValue() == PacketFly.Mode.Increment) {
            if ((double)this.lastFactor >= this.factor.getValue()) {
               this.lastFactor = 1.0F;
            } else {
               ++this.lastFactor;
               if ((double)this.lastFactor > this.factor.getValue()) {
                  this.lastFactor = this.factor.getValueFloat();
               }
            }
         } else {
            this.lastFactor = this.factor.getValueFloat();
         }

         int i = 1;

         while(true) {
            float f = (float)i;
            float f2 = this.mode.getValue() != PacketFly.Mode.Factor && this.mode.getValue() != PacketFly.Mode.Slow && this.mode.getValue() != PacketFly.Mode.Increment ? 1.0F : this.lastFactor;
            if (!(f <= f2)) {
               ++this.zoomTimer;
               if ((double)this.zoomTimer > this.zoomer.getValue()) {
                  this.zoomies = !this.zoomies;
                  this.zoomTimer = 0;
               }
               break;
            }

            double conceal = mc.player.getY() < this.concealY.getValue() && MovementUtil.isMoving() ? this.conceal.getValue() : (double)1.0F;
            MovementUtil.setMotionX(dirSpeed[0] * (double)i * conceal * this.xzSpeed.getValue());
            MovementUtil.setMotionY(ySpeed * (double)i * this.ySpeed.getValue());
            MovementUtil.setMotionZ(dirSpeed[1] * (double)i * conceal * this.xzSpeed.getValue());
            this.sendPackets(MovementUtil.getMotionX(), MovementUtil.getMotionY(), MovementUtil.getMotionZ(), this.mode.getValue() != PacketFly.Mode.Setback);
            ++i;
         }
      }

   }

   @EventListener
   public void invoke(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (this.mode.getValue() != PacketFly.Mode.Compatibility) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof PlayerPositionLookS2CPacket) {
               PlayerPositionLookS2CPacket packet2 = (PlayerPositionLookS2CPacket)packet;
               TimeVec vec;
               if (mc.player.isAlive() && this.mode.getValue() != PacketFly.Mode.Setback && this.mode.getValue() != PacketFly.Mode.Slow && !(mc.currentScreen instanceof DownloadingTerrainScreen) && (vec = (TimeVec)this.posLooks.remove(packet2.getTeleportId())) != null && vec.x == packet2.getX() && vec.y == packet2.getY() && vec.z == packet2.getZ()) {
                  event.setCancelled(true);
                  return;
               }

               this.teleportID.set(packet2.getTeleportId());
            }

         }
      }
   }

   @EventListener
   public void invoke(MoveEvent event) {
      if (this.phase.getValue() == PacketFly.Phase.Semi || this.isPlayerCollisionBoundingBoxEmpty()) {
         mc.player.noClip = true;
      }

      if (this.mode.getValue() != PacketFly.Mode.Compatibility && (this.mode.getValue() == PacketFly.Mode.Setback || this.teleportID.get() != 0)) {
         if (this.zeroSpeed.getValue()) {
            event.setX((double)0.0F);
            event.setY((double)0.0F);
            event.setZ((double)0.0F);
         } else {
            event.setX(MovementUtil.getMotionX());
            event.setY(MovementUtil.getMotionY());
            event.setZ(MovementUtil.getMotionZ());
         }

         if (this.zeroY.getValue()) {
            event.setY((double)0.0F);
         }
      }

   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      Packet<?> packet = event.getPacket();
      if (packet instanceof PlayerMoveC2SPacket packet2) {
         if (this.mode.getValue() != PacketFly.Mode.Compatibility && !this.playerPackets.remove(event.getPacket())) {
            if (packet2 instanceof PlayerMoveC2SPacket.LookAndOnGround && !this.noRotation.getValue()) {
               return;
            }

            if (!this.noMovePacket.getValue()) {
               return;
            }

            event.setCancelled(true);
         }
      }

   }

   public void onEnable() {
      this.clearValues();
      if (mc.player == null) {
         this.disable();
      }

   }

   public String getInfo() {
      return ((Mode)this.mode.getValue()).toString();
   }

   public void clearValues() {
      this.lastFactor = 1.0F;
      this.packetCounter = 0;
      this.teleportID.set(0);
      this.playerPackets.clear();
      this.posLooks.clear();
      this.vecDelServer = null;
   }

   public boolean isPlayerCollisionBoundingBoxEmpty() {
      double o = this.bbOffset.getValue() ? (double)-0.0625F : (double)0.0F;
      return BlockUtil.canCollide(mc.player, mc.player.getBoundingBox().expand(o, o, o));
   }

   public boolean checkPackets(int amount) {
      if (++this.packetCounter >= amount) {
         this.packetCounter = 0;
         return true;
      } else {
         return false;
      }
   }

   public void sendPackets(double x, double y, double z, boolean confirm) {
      Vec3d offset = new Vec3d(x, y, z);
      Vec3d vec;
      this.vecDelServer = vec = mc.player.getPos().add(offset);
      Vec3d oOB = ((Type)this.type.getValue()).createOutOfBounds(vec, this.invalidY.getValueInt());
      this.sendCPacket(new PlayerMoveC2SPacket.PositionAndOnGround(vec.x, vec.y, vec.z, mc.player.isOnGround()));
      if (!mc.isInSingleplayer()) {
         for(int i = 0; (double)i < this.invalids.getValue(); ++i) {
            this.sendCPacket(new PlayerMoveC2SPacket.PositionAndOnGround(oOB.x, oOB.y, oOB.z, mc.player.isOnGround()));
            oOB = ((Type)this.type.getValue()).createOutOfBounds(oOB, this.invalidY.getValueInt());
         }
      }

      if (confirm && (this.zeroTeleport.getValue() || this.teleportID.get() != 0)) {
         for(int i = 0; (double)i < this.sendTeleport.getValue(); ++i) {
            this.sendConfirmTeleport(vec);
         }
      }

      if (this.elytra.getValue()) {
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
      }

   }

   public void sendConfirmTeleport(Vec3d vec) {
      int id = this.teleportID.incrementAndGet();
      mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(id));
      this.posLooks.put(id, new TimeVec(vec));
   }

   public void sendCPacket(Packet<?> packet) {
      this.playerPackets.add(packet);
      mc.getNetworkHandler().sendPacket(packet);
   }

   public static enum Mode {
      Setback,
      Fast,
      Factor,
      Slow,
      Increment,
      Compatibility;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Setback, Fast, Factor, Slow, Increment, Compatibility};
      }
   }

   public static enum Phase {
      Off,
      Semi,
      Full;

      // $FF: synthetic method
      private static Phase[] $values() {
         return new Phase[]{Off, Semi, Full};
      }
   }

   public static enum Type {
      Down {
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add((double)0.0F, (double)(-invalid), (double)0.0F);
         }
      },
      Up {
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add((double)0.0F, (double)invalid, (double)0.0F);
         }
      },
      Preserve {
         final Random random = new Random();

         private int randomInt() {
            int result = this.random.nextInt(29000000);
            return this.random.nextBoolean() ? result : -result;
         }

         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add((double)this.randomInt(), (double)0.0F, (double)this.randomInt());
         }
      },
      Switch {
         final Random random = new Random();

         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            boolean down = this.random.nextBoolean();
            return down ? vec3d.add((double)0.0F, (double)(-invalid), (double)0.0F) : vec3d.add((double)0.0F, (double)invalid, (double)0.0F);
         }
      },
      X {
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add((double)invalid, (double)0.0F, (double)0.0F);
         }
      },
      Z {
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add((double)0.0F, (double)0.0F, (double)invalid);
         }
      },
      XZ {
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add((double)invalid, (double)0.0F, (double)invalid);
         }
      };

      public abstract Vec3d createOutOfBounds(Vec3d var1, int var2);

      // $FF: synthetic method
      private static Type[] $values() {
         return new Type[]{Down, Up, Preserve, Switch, X, Z, XZ};
      }
   }

   public static class TimeVec extends Vec3d {
      final long time;

      public TimeVec(Vec3d vec3d) {
         this(vec3d.x, vec3d.y, vec3d.z, System.currentTimeMillis());
      }

      public TimeVec(double xIn, double yIn, double zIn, long time) {
         super(xIn, yIn, zIn);
         this.time = time;
      }

      public long getTime() {
         return this.time;
      }
   }
}
