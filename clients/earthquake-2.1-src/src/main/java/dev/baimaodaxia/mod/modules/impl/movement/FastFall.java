package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.MoveEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.TimerEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.MovementUtil;
import dev.baimaodaxia.api.utils.world.BlockPosX;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.Objects;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class FastFall extends Module {
   private final EnumSetting<Mode> mode;
   private final BooleanSetting noLag;
   private final BooleanSetting useTimerSetting;
   private final SliderSetting timer;
   private final BooleanSetting anchor;
   private final SliderSetting height;
   private final Timer lagTimer;
   boolean onGround;
   private boolean useTimer;

   public FastFall() {
      super("FastFall", "Miyagi son simulator", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", FastFall.Mode.Fast));
      this.noLag = this.add(new BooleanSetting("NoLag", true, () -> this.mode.getValue() == FastFall.Mode.Fast));
      this.useTimerSetting = this.add(new BooleanSetting("UseTimer", false));
      BooleanSetting var10009 = this.useTimerSetting;
      Objects.requireNonNull(var10009);
      this.timer = this.add(new SliderSetting("Timer", (double)2.5F, (double)1.0F, (double)8.0F, 0.1, var10009::getValue));
      this.anchor = this.add(new BooleanSetting("Anchor", true));
      this.height = this.add(new SliderSetting("Height", (double)10.0F, (double)1.0F, (double)20.0F, (double)0.5F));
      this.lagTimer = new Timer();
      this.onGround = false;
      this.setChinese("快速坠落");
   }

   public void onDisable() {
      this.useTimer = false;
   }

   public String getInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   @EventListener(
      priority = -100
   )
   public void onMove(MoveEvent event) {
      if (!nullCheck()) {
         if (mc.player.isOnGround() && this.anchor.getValue() && this.traceDown() != 0 && (double)this.traceDown() <= this.height.getValue() && this.trace()) {
            event.setX(event.getX() * 0.05);
            event.setZ(event.getZ() * 0.05);
         }

      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if ((!(this.height.getValue() > (double)0.0F) || !((double)this.traceDown() > this.height.getValue())) && !mc.player.isInsideWall() && !mc.player.isSubmergedInWater() && !mc.player.isInLava() && !mc.player.isHoldingOntoLadder() && this.lagTimer.passed(1000L) && !mc.player.isFallFlying() && !Fly.INSTANCE.isOn() && !nullCheck()) {
         if (!Alien.PLAYER.isInWeb(mc.player)) {
            if (mc.player.isOnGround() && this.mode.getValue() == FastFall.Mode.Fast) {
               MovementUtil.setMotionY(MovementUtil.getMotionY() - (double)(this.noLag.getValue() ? 0.62F : 1.0F));
            }

            if (this.useTimerSetting.getValue()) {
               if (!mc.player.isOnGround()) {
                  if (this.onGround) {
                     this.useTimer = true;
                  }

                  if (MovementUtil.getMotionY() >= (double)0.0F) {
                     this.useTimer = false;
                  }

                  this.onGround = false;
               } else {
                  this.useTimer = false;
                  MovementUtil.setMotionY(-0.08);
                  this.onGround = true;
               }
            } else {
               this.useTimer = false;
            }

         }
      }
   }

   @EventListener
   public void onTimer(TimerEvent event) {
      if (!nullCheck()) {
         if (!mc.player.isOnGround() && this.useTimer) {
            event.set(this.timer.getValueFloat());
         }

      }
   }

   @EventListener
   public void onPacket(PacketEvent.Receive event) {
      if (!nullCheck() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.lagTimer.reset();
      }

   }

   private int traceDown() {
      int retval = 0;

      for(int tracey = (int)Math.round(mc.player.getY()) - 1; tracey >= 0; --tracey) {
         BlockHitResult trace = mc.world.raycast(new RaycastContext(mc.player.getPos(), new Vec3d(mc.player.getX(), (double)tracey, mc.player.getZ()), ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         if (trace != null && trace.getType() == Type.BLOCK) {
            return retval;
         }

         ++retval;
      }

      return retval;
   }

   private boolean trace() {
      Box bbox = mc.player.getBoundingBox();
      Vec3d basepos = bbox.getCenter();
      double minX = bbox.minX;
      double minZ = bbox.minZ;
      double maxX = bbox.maxX;
      double maxZ = bbox.maxZ;
      HashMap<Vec3d, Vec3d> positions = new HashMap();
      positions.put(basepos, new Vec3d(basepos.x, basepos.y - (double)1.0F, basepos.z));
      positions.put(new Vec3d(minX, basepos.y, minZ), new Vec3d(minX, basepos.y - (double)1.0F, minZ));
      positions.put(new Vec3d(maxX, basepos.y, minZ), new Vec3d(maxX, basepos.y - (double)1.0F, minZ));
      positions.put(new Vec3d(minX, basepos.y, maxZ), new Vec3d(minX, basepos.y - (double)1.0F, maxZ));
      positions.put(new Vec3d(maxX, basepos.y, maxZ), new Vec3d(maxX, basepos.y - (double)1.0F, maxZ));

      for(Vec3d key : positions.keySet()) {
         RaycastContext context = new RaycastContext(key, (Vec3d)positions.get(key), ShapeType.COLLIDER, FluidHandling.NONE, mc.player);
         BlockHitResult result = mc.world.raycast(context);
         if (result != null && result.getType() == Type.BLOCK) {
            return false;
         }
      }

      BlockState state = mc.world.getBlockState(new BlockPosX(mc.player.getX(), mc.player.getY() - (double)1.0F, mc.player.getZ()));
      return state.isAir();
   }

   private static enum Mode {
      Fast,
      None;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Fast, None};
      }
   }
}
