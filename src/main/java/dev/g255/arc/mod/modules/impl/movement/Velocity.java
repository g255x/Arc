package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.EntityVelocityUpdateEvent;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.TickEvent;
import dev.g255.arc.api.events.impl.UpdateRotateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.MovementUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.exploit.Blink;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class Velocity extends Module {
   public static Velocity INSTANCE;
   private final EnumSetting<Mode> mode;
   private final SliderSetting lagPause;
   public final BooleanSetting ignorePearlLag;
   private final SliderSetting phaseTime;
   public final BooleanSetting noRotation;
   public final BooleanSetting flagInWall;
   public final BooleanSetting whenPushOutOfBlocks;
   public final BooleanSetting staticSetting;
   public final BooleanSetting cancelAll;
   private final SliderSetting horizontal;
   private final SliderSetting vertical;
   public final BooleanSetting whileLiquid;
   public final BooleanSetting whileElytra;
   public final BooleanSetting noClimb;
   public final BooleanSetting waterPush;
   public final BooleanSetting entityPush;
   public final BooleanSetting blockPush;
   public final BooleanSetting fishBob;
   public final Timer pearlTimer;
   private final Timer lagBackTimer;
   private boolean flag;
   static boolean pushOutOfBlocks = false;

   public Velocity() {
      super("Velocity", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", Velocity.Mode.Plain));
      this.lagPause = this.add(new SliderSetting("LagPause", 50, 0, 500, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)));
      this.ignorePearlLag = this.add((new BooleanSetting("IgnorePearlLag", true, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall))).setParent());
      this.phaseTime = this.add(new SliderSetting("PhaseTime", 250, 0, 1000, () -> (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && this.ignorePearlLag.isOpen()));
      this.noRotation = this.add(new BooleanSetting("NoRotation", false, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)));
      this.flagInWall = this.add((new BooleanSetting("FlagInWall", false, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall))).setParent());
      this.whenPushOutOfBlocks = this.add(new BooleanSetting("WhilePushOut", false, () -> (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && this.flagInWall.isOpen()));
      this.staticSetting = this.add(new BooleanSetting("Static", false, () -> this.mode.is(Velocity.Mode.Grim)));
      this.cancelAll = this.add(new BooleanSetting("CancelAll", false, () -> !this.mode.is(Velocity.Mode.None)));
      this.horizontal = this.add(new SliderSetting("Horizontal", (double)0.0F, (double)0.0F, (double)100.0F, (double)1.0F, () -> !this.mode.is(Velocity.Mode.None) && !this.cancelAll.getValue()));
      this.vertical = this.add(new SliderSetting("Vertical", (double)0.0F, (double)0.0F, (double)100.0F, (double)1.0F, () -> !this.mode.is(Velocity.Mode.None) && !this.cancelAll.getValue()));
      this.whileLiquid = this.add(new BooleanSetting("WhileLiquid", false));
      this.whileElytra = this.add(new BooleanSetting("FallFlying", false));
      this.noClimb = this.add(new BooleanSetting("NoClimb", false));
      this.waterPush = this.add(new BooleanSetting("NoWaterPush", false));
      this.entityPush = this.add(new BooleanSetting("NoEntityPush", true));
      this.blockPush = this.add(new BooleanSetting("NoBlockPush", true));
      this.fishBob = this.add(new BooleanSetting("NoFishBob", true));
      this.pearlTimer = new Timer();
      this.lagBackTimer = new Timer();
      this.setChinese("反击退");
      INSTANCE = this;
   }

   public String getInfo() {
      if (this.mode.is(Velocity.Mode.None)) {
         return null;
      } else {
         String var10000 = ((Mode)this.mode.getValue()).name();
         return var10000 + ", " + (this.cancelAll.getValue() ? "Cancel" : this.horizontal.getValueInt() + "%, " + this.vertical.getValueInt() + "%");
      }
   }

   @EventListener
   public void onRotate(UpdateRotateEvent event) {
      if (this.noRotation()) {
         event.setRotation(Arc.ROTATION.rotationYaw, 89.0F);
      }

   }

   public boolean noRotation() {
      return this.isOn() && (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && EntityUtil.isInsideBlock() && this.noRotation.getValue();
   }

   @EventListener
   public void onVelocity(EntityVelocityUpdateEvent event) {
      if (!nullCheck()) {
         if (event.getEntity() == mc.player) {
            if (!this.mode.is(Velocity.Mode.None)) {
               if (!mc.player.isInFluid() || this.whileLiquid.getValue()) {
                  if (!mc.player.isFallFlying() || this.whileElytra.getValue()) {
                     switch (((Mode)this.mode.getValue()).ordinal()) {
                        case 1:
                           if (!this.lagBackTimer.passedMs(this.lagPause.getValue())) {
                              return;
                           }

                           if (!EntityUtil.isInsideBlock() && this.getPos() == null && (!this.staticSetting.getValue() || !MovementUtil.isStatic())) {
                              return;
                           }

                           if (event.getX() != (double)0.0F || event.getZ() != (double)0.0F) {
                              this.flag = true;
                           }
                           break;
                        case 2:
                           if (!this.lagBackTimer.passedMs(this.lagPause.getValue())) {
                              return;
                           }

                           if (!EntityUtil.isInsideBlock()) {
                              return;
                           }

                           if (event.getX() != (double)0.0F || event.getZ() != (double)0.0F) {
                              this.flag = true;
                           }
                     }

                     if (this.cancelAll.getValue()) {
                        event.cancel();
                     } else {
                        double h = (double)this.horizontal.getValueInt() / (double)100.0F;
                        double v = (double)this.vertical.getValueInt() / (double)100.0F;
                        event.setX(event.getX() * h);
                        event.setZ(event.getZ() * h);
                        event.setY(event.getY() * v);
                     }

                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onReceivePacket(PacketEvent.Receive event) {
      if (event.getPacket() instanceof PlayerPositionLookS2CPacket && (!this.ignorePearlLag.getValue() || this.pearlTimer.passed((long)this.phaseTime.getValueInt()))) {
         this.lagBackTimer.reset();
      }

      if (!nullCheck()) {
         if (!mc.player.isInFluid() || this.whileLiquid.getValue()) {
            if (this.fishBob.getValue() && event.getPacket() instanceof EntityStatusS2CPacket) {
               EntityStatusS2CPacket packet = (EntityStatusS2CPacket)event.getPacket();
               if (packet.getStatus() == 31) {
                  Entity entity = packet.getEntity(mc.world);
                  if (entity instanceof FishingBobberEntity) {
                     FishingBobberEntity fishHook = (FishingBobberEntity)entity;
                     if (fishHook.getHookedEntity() == mc.player) {
                        event.setCancelled(true);
                     }
                  }
               }
            }

         }
      }
   }

   @EventListener
   public void onUpdate(TickEvent event) {
      if (!nullCheck()) {
         if (!event.isPost() && (!mc.player.isInFluid() || this.whileLiquid.getValue())) {
            if (this.flagInWall.getValue()) {
               pushOutOfBlocks = false;
               pushOutOfBlocks(mc.player.getX() - (double)mc.player.getWidth() * 0.35, mc.player.getZ() + (double)mc.player.getWidth() * 0.35);
               pushOutOfBlocks(mc.player.getX() - (double)mc.player.getWidth() * 0.35, mc.player.getZ() - (double)mc.player.getWidth() * 0.35);
               pushOutOfBlocks(mc.player.getX() + (double)mc.player.getWidth() * 0.35, mc.player.getZ() - (double)mc.player.getWidth() * 0.35);
               pushOutOfBlocks(mc.player.getX() + (double)mc.player.getWidth() * 0.35, mc.player.getZ() + (double)mc.player.getWidth() * 0.35);
            }

            if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
               if (this.flag) {
                  if (this.lagBackTimer.passedMs(this.lagPause.getValue()) && (this.flagInWall.getValue() && (!pushOutOfBlocks || this.whenPushOutOfBlocks.getValue()) || !EntityUtil.isInsideBlock())) {
                     mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), Arc.ROTATION.rotationYaw, Arc.ROTATION.rotationPitch, mc.player.isOnGround()));
                     BlockPos pos = this.getPos();
                     if (pos != null) {
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, mc.player.getHorizontalFacing().getOpposite()));
                     }
                  }

                  this.flag = false;
               }

            }
         }
      }
   }

   public BlockPos getPos() {
      return mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() == Blocks.OBSIDIAN ? mc.player.getBlockPos().down() : null;
   }

   private static void pushOutOfBlocks(double x, double z) {
      BlockPos blockPos = BlockPos.ofFloored(x, mc.player.getY(), z);
      if (wouldCollideAt(blockPos)) {
         double d = x - (double)blockPos.getX();
         double e = z - (double)blockPos.getZ();
         Direction direction = null;
         double f = Double.MAX_VALUE;

         for(Direction direction2 : new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}) {
            double g = direction2.getAxis().choose(d, (double)0.0F, e);
            double h = direction2.getDirection() == AxisDirection.POSITIVE ? (double)1.0F - g : g;
            if (h < f && !wouldCollideAt(blockPos.offset(direction2))) {
               f = h;
               direction = direction2;
            }
         }

         if (direction != null) {
            pushOutOfBlocks = true;
         }
      }

   }

   private static boolean wouldCollideAt(BlockPos pos) {
      Box box = mc.player.getBoundingBox();
      Box box2 = (new Box((double)pos.getX(), box.minY, (double)pos.getZ(), (double)pos.getX() + (double)1.0F, box.maxY, (double)pos.getZ() + (double)1.0F)).contract(1.0E-7);
      return mc.player.getWorld().canCollide(mc.player, box2);
   }

   public static enum Mode {
      Plain,
      Grim,
      Wall,
      None;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Plain, Grim, Wall, None};
      }
   }
}
