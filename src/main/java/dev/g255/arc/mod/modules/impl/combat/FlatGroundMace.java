package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.passive.MerchantEntity;

public class FlatGroundMace extends Module {
   public static FlatGroundMace INSTANCE;
   private final EnumSetting<Mode> mode;
   private final SliderSetting range;
   private final SliderSetting fallHeight;
   private final SliderSetting resetDistance;
   private final BooleanSetting requireMace;
   private final BooleanSetting lockView;
   private final SliderSetting splitStep;
   private final SliderSetting blinkDelay;
   private final SliderSetting spoofHeight;
   private final BooleanSetting spoof;
   private final EnumSetting<VMMode> vmMode;
   private final SliderSetting voidY;
   private final SliderSetting vmFallHeight;
   private final SliderSetting tpCooldown;
   private final BooleanSetting players;
   private final BooleanSetting villagers;
   private final BooleanSetting animals;
   private final BooleanSetting slimes;
   private final BooleanSetting mobs;
   private final BooleanSetting keepVelocity;
   private final BooleanSetting antiAir;
   private final SliderSetting attackPackets;
   private final Timer packetTimer;
   private final Timer tpTimer;
   private final Timer stateTimer;
   private Entity target;
   private final ArrayList<PlayerMoveC2SPacket> bufferedPackets;
   private int blinkTicks;
   private int tpCd;
   private double spoofY;
   private boolean spoofRising;
   private boolean blinkActive;
   private boolean attacking;
   private State state;
   private double anchorX;
   private double anchorY;
   private double anchorZ;
   private int stepTick;
   private int attackCd;
   private boolean timerActive;

   public FlatGroundMace() {
      super("FlatGroundMace", Module.Category.Combat);
      this.mode = this.add(new EnumSetting("Mode", FlatGroundMace.Mode.Vanilla));
      this.range = this.add(new SliderSetting("Range", (double)4.0F, (double)1.0F, (double)8.0F, (double)0.5F));
      this.fallHeight = this.add(new SliderSetting("FallHeight", (double)4.0F, (double)0.5F, (double)15.0F, 0.1));
      this.resetDistance = this.add(new SliderSetting("ResetDistance", 0.1, 0.05, (double)0.5F, 0.05));
      this.requireMace = this.add(new BooleanSetting("RequireMace", false));
      this.lockView = this.add(new BooleanSetting("LockView", true));
      this.splitStep = this.add(new SliderSetting("SplitStep", 0.42, 0.1, (double)1.0F, 0.01, () -> this.mode.is(FlatGroundMace.Mode.VerticalSplit) || this.mode.is(FlatGroundMace.Mode.Hybrid)));
      this.blinkDelay = this.add(new SliderSetting("BlinkDelay", (double)3.0F, (double)1.0F, (double)20.0F, (double)1.0F, () -> this.mode.is(FlatGroundMace.Mode.BlinkBuffer)));
      this.spoofHeight = this.add(new SliderSetting("SpoofHeight", (double)12.0F, (double)2.0F, (double)70.0F, (double)1.0F, () -> this.mode.is(FlatGroundMace.Mode.MotionSpoof)));
      this.spoof = this.add(new BooleanSetting("Spoof", true, () -> this.mode.is(FlatGroundMace.Mode.TheVoid)));
      this.vmMode = this.add(new EnumSetting("VMMode", FlatGroundMace.VMMode.DoublePacket, () -> this.mode.is(FlatGroundMace.Mode.VoidMace)));
      this.voidY = this.add(new SliderSetting("VoidY", (double)-60.0F, (double)-100.0F, (double)-50.0F, (double)1.0F, () -> this.mode.is(FlatGroundMace.Mode.VoidMace)));
      this.vmFallHeight = this.add(new SliderSetting("VMFallHeight", (double)100.0F, (double)20.0F, (double)500.0F, (double)10.0F, () -> this.mode.is(FlatGroundMace.Mode.VoidMace)));
      this.tpCooldown = this.add(new SliderSetting("TPCooldown", (double)5.0F, (double)1.0F, (double)30.0F, (double)1.0F, () -> this.mode.is(FlatGroundMace.Mode.TP)));
      this.players = this.add(new BooleanSetting("Players", true));
      this.villagers = this.add(new BooleanSetting("Villagers", false));
      this.animals = this.add(new BooleanSetting("Animals", false));
      this.slimes = this.add(new BooleanSetting("Slimes", false));
      this.mobs = this.add(new BooleanSetting("Mobs", false));
      this.keepVelocity = this.add(new BooleanSetting("KeepVelocity", false, () -> this.mode.is(FlatGroundMace.Mode.VoidMace)));
      this.antiAir = this.add(new BooleanSetting("AntiAir", true));
      this.attackPackets = this.add(new SliderSetting("AttackPackets", (double)1.0F, (double)1.0F, (double)10.0F, (double)1.0F));
      this.packetTimer = new Timer();
      this.tpTimer = new Timer();
      this.stateTimer = new Timer();
      this.bufferedPackets = new ArrayList();
      this.state = FlatGroundMace.State.IDLE;
      this.setChinese("平地重锤");
      INSTANCE = this;
   }

   public void onEnable() {
      super.onEnable();
      this.resetState();
   }

   public void onDisable() {
      super.onDisable();
      this.flushBuffer();
      this.stopTimer();
      this.resetState();
   }

   private void resetState() {
      this.target = null;
      this.bufferedPackets.clear();
      this.blinkTicks = 0;
      this.tpCd = 0;
      this.spoofY = (double)0.0F;
      this.spoofRising = false;
      this.blinkActive = false;
      this.attacking = false;
      this.state = FlatGroundMace.State.IDLE;
      this.stepTick = 0;
      this.attackCd = 0;
      this.timerActive = false;
      this.packetTimer.reset();
      this.tpTimer.reset();
      this.stateTimer.reset();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         if (this.tpCd > 0) {
            --this.tpCd;
         }

         if (this.attackCd > 0) {
            --this.attackCd;
         }

         this.target = this.findTarget();
         if (this.target == null) {
            this.stopTimer();
            this.flushBuffer();
         } else if (!this.requireMace.getValue() || mc.player.getMainHandStack().getItem() == Items.MACE || FlatGroundMace.Inventory.findMace() != -1) {
            double fallY = this.getGroundHeight();
            double currentY = mc.player.getY();
            double d = currentY - fallY;
            double d2 = this.fallHeight.getValue();
            switch (((Mode)this.mode.getValue()).ordinal()) {
               case 1 -> this.doStateMachine();
               case 2 -> this.doVerticalSplit(d, d2);
               case 3 -> this.doHybrid();
               case 4 -> this.doBlinkBuffer(d, d2);
               case 5 -> this.doMotionSpoof(d, d2);
               case 6 -> this.doTheVoid(d, d2);
               case 7 -> this.doVoidMace(d, d2);
               case 8 -> this.doTp(d, d2);
               default -> this.doVoidMaceMode();
            }

            if (this.lockView.getValue() && this.target != null) {
               this.faceTarget(this.target);
            }

         }
      }
   }

   private void doTp(double d, double d2) {
      if (d <= d2 && this.tpCd <= 0) {
         double targetY = mc.player.getY() + this.fallHeight.getValue();
         mc.player.setPosition(mc.player.getX(), targetY, mc.player.getZ());
         mc.player.setVelocity(mc.player.getVelocity().x, (double)0.0F, mc.player.getVelocity().z);
         this.tpCd = (int)this.tpCooldown.getValue();
      }

   }

   private void doVoidMace(double d, double d2) {
      if (d <= d2 && this.tpCd <= 0) {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + this.fallHeight.getValue(), mc.player.getZ(), false));
         this.tpCd = (int)this.tpCooldown.getValue();
         mc.player.setPosition(mc.player.getX(), mc.player.getY() + this.fallHeight.getValue(), mc.player.getZ());
         mc.player.setVelocity(mc.player.getVelocity().x, (double)0.0F, mc.player.getVelocity().z);
      }

   }

   private void doMotionSpoof(double d, double d2) {
      if (!this.spoofRising && d <= d2) {
         this.spoofRising = true;
         this.spoofY = mc.player.getY();
      }

      if (this.spoofRising) {
         if (this.spoofY < mc.player.getY() + this.fallHeight.getValue()) {
            this.spoofY += 0.42;
            if (this.spoofY > mc.player.getY() + this.fallHeight.getValue()) {
               this.spoofY = mc.player.getY() + this.fallHeight.getValue();
            }

            mc.player.setPosition(mc.player.getX(), this.spoofY, mc.player.getZ());
            mc.player.setVelocity(mc.player.getVelocity().x, (double)0.0F, mc.player.getVelocity().z);
         } else {
            this.spoofRising = false;
         }
      }

   }

   private void doBlinkBuffer(double d, double d2) {
      if (!this.blinkActive && d <= d2) {
         this.blinkActive = true;
         this.blinkTicks = 0;
      }

      if (this.blinkActive) {
         ++this.blinkTicks;
         if (this.blinkTicks >= (int)this.blinkDelay.getValue()) {
            this.flushBuffer();
            this.blinkActive = false;
         }
      }

   }

   private void doHybrid() {
      if (this.attackCd <= 0) {
         Entity entity = this.target;
         if (entity != null) {
            this.bufferedPackets.clear();
            this.attacking = true;
            double startY = mc.player.getY();

            for(int i = 1; i <= 6; ++i) {
               double y = startY + (double)i * (this.fallHeight.getValue() / (double)6.0F);
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), y, mc.player.getZ(), false));
            }

            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), startY, mc.player.getZ(), false));
            this.attackTarget(entity);
            mc.player.setPosition(mc.player.getX(), startY, mc.player.getZ());
            mc.player.setVelocity(mc.player.getVelocity().x, (double)0.0F, mc.player.getVelocity().z);
            mc.player.fallDistance = 0.0F;
            this.attacking = false;
            this.attackCd = 3;
         }
      }
   }

   private void doTheVoid(double d, double d2) {
      if (d <= d2 && this.attackCd <= 0) {
         mc.player.setPosition(mc.player.getX(), mc.player.getY() + this.fallHeight.getValue(), mc.player.getZ());
         mc.player.setVelocity(mc.player.getVelocity().x, (double)0.0F, mc.player.getVelocity().z);
         this.attackTarget(this.target);
         this.attackCd = 3;
      }

   }

   private void doVerticalSplit(double d, double d2) {
      if (!this.spoofRising && d <= d2) {
         this.spoofRising = true;
         this.spoofY = mc.player.getY();
      }

      if (this.spoofRising) {
         if (this.spoofY < mc.player.getY() + this.fallHeight.getValue()) {
            this.spoofY += this.splitStep.getValue();
            if (this.spoofY > mc.player.getY() + this.fallHeight.getValue()) {
               this.spoofY = mc.player.getY() + this.fallHeight.getValue();
            }

            mc.player.setPosition(mc.player.getX(), this.spoofY, mc.player.getZ());
            mc.player.setVelocity(mc.player.getVelocity().x, (double)0.0F, mc.player.getVelocity().z);
         } else {
            this.spoofRising = false;
         }
      }

   }

   private void doStateMachine() {
      if (this.state == FlatGroundMace.State.IDLE) {
         if (this.target != null) {
            this.anchorX = mc.player.getX();
            this.anchorY = mc.player.getY();
            this.anchorZ = mc.player.getZ();
            this.stepTick = 0;
            this.attackCd = 0;
            this.state = FlatGroundMace.State.FALLING;
            this.stateTimer.reset();
         }
      } else if (this.state == FlatGroundMace.State.FALLING) {
         ++this.stepTick;
         if (this.stepTick <= 1) {
            this.sendAnchorPacket(this.anchorX, this.anchorY, this.anchorZ);
            this.sendAnchorPacket(this.anchorX, this.anchorY - this.resetDistance.getValue(), this.anchorZ);
            this.sendAnchorPacket(this.anchorX, this.anchorY - this.resetDistance.getValue() * (double)2.0F, this.anchorZ);
            this.sendAnchorPacket(this.anchorX, this.anchorY + (double)1.0F, this.anchorZ);
            this.attackCd = (int)this.attackPackets.getValue();
         } else if (this.attackCd > 0) {
            --this.attackCd;
         } else {
            if (this.target != null && this.target.isAlive() && CombatUtil.isValid(this.target)) {
               this.attackTarget(this.target);
            }

            this.state = FlatGroundMace.State.IDLE;
            this.target = null;
         }
      }
   }

   private void sendAnchorPacket(double x, double y, double z) {
      if (this.keepVelocity.getValue()) {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
      } else {
         mc.player.setPosition(x, y, z);
         mc.player.setVelocity(mc.player.getVelocity().x, (double)0.0F, mc.player.getVelocity().z);
      }

   }

   private void doVoidMaceMode() {
      if (this.packetTimer.passedMs((double)500.0F)) {
         this.packetTimer.reset();
         Entity entity = this.target;
         if (entity != null) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + this.spoofHeight.getValue(), z, false));
            mc.player.setPosition(x, y + this.spoofHeight.getValue(), z);
            double baseY = y + this.spoofHeight.getValue();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, baseY + 1.1, z, false));
            mc.player.setPosition(x, baseY + 1.1, z);
            ++baseY;
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, this.voidY.getValue(), z, false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, baseY + (double)1.0F, z, false));
            mc.player.setPosition(x, baseY + (double)1.0F, z);
            mc.player.fallDistance = (float)this.vmFallHeight.getValue();
            this.attackTarget(entity);
            if (this.keepVelocity.getValue()) {
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
               mc.player.setPosition(x, y, z);
            }

         }
      }
   }

   private void attackTarget(Entity entity) {
      if (entity != null && entity.isAlive()) {
         int count = (int)this.attackPackets.getValue();

         for(int i = 0; i < count; ++i) {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
         }

         mc.player.swingHand(Hand.MAIN_HAND);
         mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      }
   }

   private void faceTarget(Entity entity) {
      Vec3d eyes = mc.player.getEyePos();
      double dx = entity.getX() - eyes.x;
      double dy = entity.getY() + (double)entity.getHeight() * (double)0.5F - eyes.y;
      double dz = entity.getZ() - eyes.z;
      double dist = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(MathHelper.atan2(dz, dx) * (double)(180F / (float)Math.PI)) - 90.0F;
      float pitch = (float)(-(MathHelper.atan2(dy, dist) * (double)(180F / (float)Math.PI)));
      mc.player.setYaw(yaw);
      mc.player.setPitch(MathHelper.clamp(pitch, -90.0F, 90.0F));
   }

   private double getGroundHeight() {
      BlockPos pos = mc.player.getBlockPos();
      if (this.antiAir.getValue()) {
         for(int i = 0; i < 6; ++i) {
            BlockPos check = pos.down(i);
            if (mc.world.getBlockState(check).isSolidBlock(mc.world, check)) {
               return (double)(check.getY() + 1);
            }
         }
      }

      return (double)mc.world.getBottomY();
   }

   private Entity findTarget() {
      Entity best = null;
      double bestDist = this.range.getValue();

      for(Entity entity : Arc.THREAD.getEntities()) {
         if (entity != null && entity != mc.player && entity.isAlive() && CombatUtil.isValid(entity)) {
            if (entity instanceof PlayerEntity) {
               if (!this.players.getValue() || Arc.FRIEND.isFriend((PlayerEntity)entity)) {
                  continue;
               }
            } else if (entity instanceof MerchantEntity) {
               if (!this.villagers.getValue()) {
                  continue;
               }
            } else if (entity instanceof SlimeEntity) {
               if (!this.slimes.getValue()) {
                  continue;
               }
            } else if (entity instanceof AnimalEntity) {
               if (!this.animals.getValue()) {
                  continue;
               }
            } else if (!(entity instanceof MobEntity) || !this.mobs.getValue()) {
               continue;
            }

            double dist = mc.player.getEyePos().distanceTo(entity.getPos());
            if (dist <= bestDist) {
               bestDist = dist;
               best = entity;
            }
         }
      }

      return best;
   }

   private void flushBuffer() {
      if (!this.bufferedPackets.isEmpty()) {
         for(PlayerMoveC2SPacket packet : this.bufferedPackets) {
            mc.getNetworkHandler().sendPacket(packet);
         }

         this.bufferedPackets.clear();
      }
   }

   private void stopTimer() {
      if (this.timerActive) {
         if (Arc.TIMER != null) {
            Arc.TIMER.reset();
         }

         this.timerActive = false;
      }

   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (this.blinkActive) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket) {
               event.cancel();
               this.bufferedPackets.add((PlayerMoveC2SPacket)event.getPacket());
            }

         }
      }
   }

   public static enum Mode {
      Vanilla,
      DoublePacket,
      VerticalSplit,
      Hybrid,
      BlinkBuffer,
      MotionSpoof,
      TheVoid,
      VoidMace,
      TP;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Vanilla, DoublePacket, VerticalSplit, Hybrid, BlinkBuffer, MotionSpoof, TheVoid, VoidMace, TP};
      }
   }

   public static enum VMMode {
      DoublePacket,
      Legacy;

      // $FF: synthetic method
      private static VMMode[] $values() {
         return new VMMode[]{DoublePacket, Legacy};
      }
   }

   public static enum State {
      IDLE,
      FALLING;

      // $FF: synthetic method
      private static State[] $values() {
         return new State[]{IDLE, FALLING};
      }
   }

   private static class Inventory {
      static int findMace() {
         for(int i = 0; i < 9; ++i) {
            if (Wrapper.mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
               return i;
            }
         }

         return -1;
      }
   }
}
