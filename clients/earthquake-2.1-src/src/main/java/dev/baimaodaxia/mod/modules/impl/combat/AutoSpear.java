package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.TickEvent;
import dev.baimaodaxia.api.utils.combat.CombatUtil;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.api.utils.render.Render3DUtil;
import dev.baimaodaxia.mod.features.SpearPort;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BindSetting;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoSpear extends Module {
   public static AutoSpear INSTANCE;
   private static final double[] SEARCH_ORDER = createSearchOrder();
   private static final double PATH_SAMPLE_STEP = (double)1.0F;
   public final BindSetting actionKey = this.add(new BindSetting("ActionKey", -2));
   public final SliderSetting spearDistance = this.add((new SliderSetting("Distance", (double)50.0F, (double)1.0F, (double)100.0F, (double)1.0F)).setSuffix("m"));
   public final SliderSetting spearMaxTp = this.add((new SliderSetting("MaxTp", (double)100.0F, (double)1.0F, (double)200.0F, (double)1.0F)).setSuffix("m"));
   public final SliderSetting attackRange = this.add((new SliderSetting("AttackRange", (double)3.0F, (double)1.0F, (double)8.0F, 0.1)).setSuffix("m"));
   public final SliderSetting minRange = this.add((new SliderSetting("MinRange", (double)3.0F, (double)0.0F, (double)8.0F, 0.1)).setSuffix("m"));
   public final SliderSetting delay = this.add((new SliderSetting("Delay", 2, 0, 10)).setSuffix("t"));
   public final SliderSetting predictTicks = this.add((new SliderSetting("PredictTicks", 1, 0, 5)).setSuffix("t"));
   public final BooleanSetting autoSwitch = this.add(new BooleanSetting("AutoSwitch", true));
   public final BooleanSetting autoCharge = this.add(new BooleanSetting("AutoCharge", true));
   public final BooleanSetting autoAttack = this.add(new BooleanSetting("AutoAttack", true));
   public final BooleanSetting autoRestart = this.add(new BooleanSetting("AutoRestart", true));
   public final BooleanSetting playersOnly = this.add(new BooleanSetting("PlayersOnly", true));
   public final BooleanSetting renderTarget = this.add(new BooleanSetting("RenderTarget", false));
   public final BooleanSetting swing = this.add(new BooleanSetting("Swing", true));
   public final BooleanSetting elytraFireworkCompat = this.add(new BooleanSetting("兼容鞘翅飞行", true));
   private int prevSlot = -1;
   private int fireworkPauseTicks;
   private int waitBackTicks;
   private boolean using;
   private boolean sendingSpearPacket;
   private boolean lastActionPressed;
   private Entity renderEntity;

   public AutoSpear() {
      super("AutoSpear", Module.Category.Combat);
      this.setChinese("自动长矛");
      INSTANCE = this;
   }

   public void onEnable() {
      this.prevSlot = -1;
      this.fireworkPauseTicks = 0;
      this.waitBackTicks = 0;
      this.using = false;
      this.sendingSpearPacket = false;
      this.lastActionPressed = false;
      this.renderEntity = null;
   }

   public void onDisable() {
      this.pauseUsing();
      this.waitBackTicks = 0;
      this.sendingSpearPacket = false;
      this.lastActionPressed = false;
      this.renderEntity = null;
      this.releaseCombatTask();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.isPre() && !nullCheck()) {
         if (mc.currentScreen == null && !this.shouldPauseForFirework()) {
            this.handleAutoRestart();
            Entity target = this.findTarget();
            this.renderEntity = target;
            if (target != null && this.prepareSpear()) {
               if (this.autoCharge.getValue()) {
                  this.startUsingSpear();
               }

               boolean actionPressed = this.actionKey.isPressed();
               boolean shouldAttack = this.autoAttack.getValue() || actionPressed && !this.lastActionPressed;
               this.lastActionPressed = actionPressed;
               if (shouldAttack && this.waitBackTicks == 0 && this.canSpearKineticAttack()) {
                  this.spearAttack(target);
               }
            } else {
               this.pauseUsing();
            }
         } else {
            this.renderEntity = null;
            this.pauseUsing();
         }
      }

   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!this.sendingSpearPacket && this.waitBackTicks > 0 && !nullCheck() && event.getPacket() instanceof PlayerMoveC2SPacket) {
         --this.waitBackTicks;
         if (this.waitBackTicks > this.delay.getValueInt()) {
            this.waitBackTicks = this.delay.getValueInt();
         }

         if (this.waitBackTicks > 0) {
            mc.player.setOnGround(false);
            event.cancel();
         } else {
            this.releaseCombatTask();
         }
      }

   }

   public void onRender3D(MatrixStack matrixStack) {
      if (this.renderTarget.getValue() && this.renderEntity != null && this.renderEntity.isAlive() && this.canSpearKineticAttack()) {
         Box box = this.renderEntity.getBoundingBox();
         Render3DUtil.draw3DBox(matrixStack, box, new Color(30, 220, 90, 70), new Color(30, 220, 90, 170), true, true);
      }

   }

   public static void pauseForFirework() {
      if (INSTANCE != null && INSTANCE.elytraFireworkCompat.getValue()) {
         INSTANCE.fireworkPauseTicks = 6;
      }

   }

   private boolean prepareSpear() {
      int spearSlot = SpearPort.findSpearHotbarSlot();
      if (spearSlot == -1) {
         return false;
      } else {
         int currentSlot = mc.player.getInventory().selectedSlot;
         if (!this.autoSwitch.getValue() && currentSlot != spearSlot && !SpearPort.isSpear(mc.player.getMainHandStack())) {
            return false;
         } else {
            if (this.autoSwitch.getValue() && currentSlot != spearSlot && !this.using) {
               this.prevSlot = currentSlot;
               InventoryUtil.switchToSlot(spearSlot);
            }

            return SpearPort.isSpear(mc.player.getMainHandStack());
         }
      }
   }

   private void startUsingSpear() {
      if (this.isUsingSpear()) {
         this.using = true;
         this.holdUsingItemPause();
      } else if (SpearPort.isSpear(mc.player.getMainHandStack())) {
         mc.options.useKey.setPressed(true);
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         this.using = true;
         this.holdUsingItemPause();
      }

   }

   private void pauseUsing() {
      mc.options.useKey.setPressed(false);
      if (mc.player != null && mc.interactionManager != null) {
         if (this.using && this.isUsingSpear()) {
            mc.interactionManager.stopUsingItem(mc.player);
         }

         if (this.prevSlot >= 0) {
            InventoryUtil.switchToSlot(this.prevSlot);
            this.prevSlot = -1;
         }

         this.using = false;
         this.releaseUsingItemPause();
      } else {
         this.prevSlot = -1;
         this.using = false;
         this.releaseUsingItemPause();
      }

   }

   private boolean shouldPauseForFirework() {
      if (!this.elytraFireworkCompat.getValue()) {
         return false;
      } else if (this.fireworkPauseTicks > 0) {
         --this.fireworkPauseTicks;
         return true;
      } else {
         return mc.player.isFallFlying() && mc.options.useKey.isPressed() && (mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET) || mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET));
      }
   }

   private void handleAutoRestart() {
      if (this.autoRestart.getValue() && this.isUsingSpear()) {
         ItemStack stack = mc.player.getActiveItem();
         int maxUseTicks = SpearPort.getMaxKineticUseTicks(stack);
         if (maxUseTicks > 0 && mc.player.getItemUseTime() > maxUseTicks) {
            Hand hand = mc.player.getActiveHand();
            mc.interactionManager.stopUsingItem(mc.player);
            mc.interactionManager.interactItem(mc.player, hand);
            this.using = true;
         }
      }

   }

   private boolean canSpearKineticAttack() {
      if (!this.isUsingSpear()) {
         return false;
      } else {
         ItemStack stack = mc.player.getActiveItem();
         int useTime = mc.player.getItemUseTime();
         int delayTicks = SpearPort.getKineticDelayTicks(stack);
         int maxUseTicks = SpearPort.getMaxKineticUseTicks(stack);
         return useTime >= delayTicks && maxUseTicks > 0 && useTime < maxUseTicks;
      }
   }

   private boolean isUsingSpear() {
      return mc.player != null && mc.player.isUsingItem() && SpearPort.isSpear(mc.player.getActiveItem());
   }

   private Entity findTarget() {
      double maxDist = this.spearDistance.getValue() + this.attackRange.getValue();
      double maxDistSq = maxDist * maxDist;
      Entity closest = null;
      double closestDist = Double.MAX_VALUE;

      for(Entity entity : mc.world.getEntities()) {
         if (entity instanceof LivingEntity && (!this.playersOnly.getValue() || entity instanceof PlayerEntity) && CombatUtil.isValid(entity)) {
            double distSq = mc.player.squaredDistanceTo(entity);
            if (distSq <= maxDistSq && distSq < closestDist && this.isSpearable(entity)) {
               closest = entity;
               closestDist = distSq;
            }
         }
      }

      return closest;
   }

   private boolean isSpearable(Entity entity) {
      if (mc.player.getEyePos().squaredDistanceTo(entity.getEyePos()) <= square(this.minRange.getValue())) {
         return false;
      } else {
         BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), entity.getEyePos(), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
         return result.getType() == HitResult.Type.MISS;
      }
   }

   private boolean spearAttack(Entity target) {
      if (!(this.spearDistance.getValue() < (double)0.0F) && !(this.spearMaxTp.getValue() < this.spearDistance.getValue())) {
         if (!this.tryUseCombatTask(350L)) {
            return false;
         } else {
            Vec3d playerPos = mc.player.getPos();
            Vec3d targetEyePos = this.getPredictedEyePos(target);
            Vec3d direction = targetEyePos.subtract(mc.player.getEyePos());
            double distance = direction.length();
            if (!(distance <= this.minRange.getValue()) && !(distance < 1.0E-4)) {
               direction = direction.normalize();
               Vec3d tpDirection = direction.multiply((double)-1.0F);
               Vec3d expandDirection = getExpandDirection(direction);
               List<Vec3d> path = this.findValidTpPath(playerPos, tpDirection, expandDirection, distance);
               if (path != null && !path.isEmpty()) {
                  Vec2f rotation = rotationToPitchYaw(direction);
                  this.sendSpearMoveSequence(path, playerPos, rotation);
                  this.waitBackTicks = this.delay.getValueInt() + 1;
                  if (this.swing.getValue()) {
                     mc.player.swingHand(Hand.MAIN_HAND);
                  }

                  return true;
               } else {
                  this.releaseCombatTask();
                  return false;
               }
            } else {
               this.releaseCombatTask();
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private Vec3d getPredictedEyePos(Entity target) {
      return target.getEyePos().add(target.getVelocity().multiply(this.predictTicks.getValue()));
   }

   private List<Vec3d> findValidTpPath(Vec3d playerPos, Vec3d tpDirection, Vec3d expandDirection, double targetDistance) {
      double maxTpSq = square(this.spearMaxTp.getValue());

      for(double dist = this.spearDistance.getValue(); dist > targetDistance; dist -= (double)2.0F) {
         for(double expand : SEARCH_ORDER) {
            Vec3d pos = playerPos.add(tpDirection.multiply(dist)).add(expandDirection.multiply(expand));
            if (!(pos.squaredDistanceTo(playerPos) > maxTpSq) && this.isSafePlayerLocation(pos) && this.isPathClear(pos, playerPos)) {
               List<Vec3d> sequence = this.generateTpSequence(playerPos, pos);
               if (!sequence.isEmpty()) {
                  return sequence;
               }
            }
         }
      }

      return null;
   }

   private List<Vec3d> generateTpSequence(Vec3d current, Vec3d target) {
      if (!this.isSafePlayerLocation(target)) {
         return List.of();
      } else if (this.isPathClear(current, target)) {
         return List.of(target);
      } else {
         for(double y = (double)1.0F; y <= (double)6.0F; ++y) {
            Vec3d up = current.add((double)0.0F, y, (double)0.0F);
            Vec3d across = new Vec3d(target.x, up.y, target.z);
            if (this.isSafePlayerLocation(up) && this.isSafePlayerLocation(across) && this.isPathClear(current, up) && this.isPathClear(up, across) && this.isPathClear(across, target)) {
               List<Vec3d> path = new ArrayList();
               addIfMoved(path, current, up);
               addIfMoved(path, up, across);
               addIfMoved(path, across, target);
               return path;
            }
         }

         return List.of();
      }
   }

   private void sendSpearMoveSequence(List<Vec3d> path, Vec3d backPos, Vec2f rotation) {
      this.sendingSpearPacket = true;

      try {
         for(Vec3d pos : path) {
            this.sendFullMove(pos, rotation, false);
         }

         this.sendFullMove(backPos, rotation, false);
      } finally {
         this.sendingSpearPacket = false;
      }

   }

   private void sendFullMove(Vec3d pos, Vec2f rotation, boolean onGround) {
      mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(pos.x, pos.y, pos.z, rotation.y, rotation.x, onGround));
   }

   private boolean isSafePlayerLocation(Vec3d pos) {
      if (!(pos.y < (double)mc.world.getBottomY()) && !(pos.y > (double)(mc.world.getBottomY() + mc.world.getHeight()))) {
         Box box = mc.player.getBoundingBox().offset(pos.subtract(mc.player.getPos()));
         return mc.world.isSpaceEmpty(mc.player, box);
      } else {
         return false;
      }
   }

   private boolean isPathClear(Vec3d from, Vec3d to) {
      double distance = from.distanceTo(to);
      int steps = Math.max(1, (int)Math.ceil(distance / (double)1.0F));

      for(int i = 1; i <= steps; ++i) {
         Vec3d checkPos = from.lerp(to, (double)i / (double)steps);
         if (!this.isSafePlayerLocation(checkPos)) {
            return false;
         }
      }

      return true;
   }

   private static void addIfMoved(List<Vec3d> path, Vec3d from, Vec3d to) {
      if (from.squaredDistanceTo(to) > 1.0E-6) {
         path.add(to);
      }

   }

   private static Vec3d getExpandDirection(Vec3d direction) {
      Vec3d expand;
      if (Math.abs(direction.y) > 1.0E-5) {
         expand = new Vec3d(direction.x, -(direction.x * direction.x + direction.z * direction.z) / direction.y, direction.z);
      } else {
         expand = new Vec3d((double)0.0F, (double)1.0F, (double)0.0F);
      }

      return expand.lengthSquared() < 1.0E-6 ? new Vec3d((double)0.0F, (double)1.0F, (double)0.0F) : expand.normalize();
   }

   private static Vec2f rotationToPitchYaw(Vec3d direction) {
      double horizontalDist = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      float yaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(direction.z, direction.x)) - (double)90.0F);
      float pitch = (float)(-Math.toDegrees(Math.atan2(direction.y, horizontalDist)));
      return new Vec2f(pitch, yaw);
   }

   private static double square(double value) {
      return value * value;
   }

   private static double[] createSearchOrder() {
      double[] order = new double[19];
      order[0] = (double)0.0F;
      int index = 1;

      for(int i = 1; i < 10; ++i) {
         order[index++] = (double)i;
         order[index++] = (double)(-i);
      }

      return order;
   }
}
