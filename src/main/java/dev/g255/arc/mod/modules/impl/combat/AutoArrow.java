package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.RotationEvent;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.core.impl.RotationManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public class AutoArrow extends Module {
   public final EnumSetting<Page> page;
   public final SliderSetting range;
   public final BooleanSetting ignoreFriends;
   public final EnumSetting<TargetMode> targetMode;
   public final SliderSetting fov;
   public final SliderSetting steps;
   public final SliderSetting priority;
   public final BooleanSetting lookAtHead;
   public final SliderSetting minCharge;
   public final BooleanSetting tpsSync;
   public final BooleanSetting switchToBow;
   public final EnumSetting<ReleaseMode> releaseMode;
   public final SliderSetting fixedTicks;
   public final SliderSetting minReleaseCharge;
   public final BooleanSetting predictTarget;
   public final SliderSetting predictionFactor;
   public final BooleanSetting crossbowSupport;
   public final BooleanSetting autoRelease;
   private Entity target;
   private int bowSlot;
   private int startSlot;
   private boolean switched;

   public AutoArrow() {
      super("AutoArrow", Module.Category.Combat);
      this.page = this.add(new EnumSetting("Page", AutoArrow.Page.AimBot));
      this.range = this.add(new SliderSetting("Range", (double)6.0F, 0.1, (double)7.0F, () -> this.page.getValue() == AutoArrow.Page.AimBot));
      this.ignoreFriends = this.add(new BooleanSetting("IgnoreFriends", false, () -> this.page.getValue() == AutoArrow.Page.AimBot));
      this.targetMode = this.add(new EnumSetting("TargetMode", AutoArrow.TargetMode.DISTANCE, () -> this.page.getValue() == AutoArrow.Page.AimBot));
      this.fov = this.add(new SliderSetting("FOV", (double)180.0F, (double)0.0F, (double)360.0F, 0.1, () -> this.page.getValue() == AutoArrow.Page.AimBot));
      this.steps = this.add(new SliderSetting("Steps", (double)1.0F, 0.01, (double)1.0F, 0.01, () -> this.page.getValue() == AutoArrow.Page.AimBot));
      this.priority = this.add(new SliderSetting("Priority", (double)10.0F, (double)0.0F, (double)100.0F, () -> this.page.getValue() == AutoArrow.Page.AimBot));
      this.lookAtHead = this.add(new BooleanSetting("LookAtHead", true, () -> this.page.getValue() == AutoArrow.Page.AimBot));
      this.minCharge = this.add(new SliderSetting("MinCharge", (double)1.0F, 0.1, (double)1.0F, 0.01, () -> this.page.getValue() == AutoArrow.Page.AutoBow));
      this.tpsSync = this.add(new BooleanSetting("TPSSync", true, () -> this.page.getValue() == AutoArrow.Page.AutoBow));
      this.switchToBow = this.add(new BooleanSetting("SwitchToBow", true, () -> this.page.getValue() == AutoArrow.Page.AutoBow));
      this.releaseMode = this.add(new EnumSetting("ReleaseMode", AutoArrow.ReleaseMode.Smart, () -> this.page.getValue() == AutoArrow.Page.Release));
      this.fixedTicks = this.add(new SliderSetting("FixedTicks", (double)20.0F, (double)1.0F, (double)30.0F, (double)1.0F, () -> this.page.getValue() == AutoArrow.Page.Release && this.releaseMode.getValue() == AutoArrow.ReleaseMode.Fixed));
      this.minReleaseCharge = this.add(new SliderSetting("MinReleaseCharge", 0.7, 0.1, (double)1.0F, 0.01, () -> this.page.getValue() == AutoArrow.Page.Release && this.releaseMode.getValue() == AutoArrow.ReleaseMode.Smart));
      this.predictTarget = this.add(new BooleanSetting("PredictTarget", true, () -> this.page.getValue() == AutoArrow.Page.Release && this.releaseMode.getValue() == AutoArrow.ReleaseMode.Smart));
      this.predictionFactor = this.add(new SliderSetting("PredictionFactor", (double)1.0F, (double)0.0F, (double)5.0F, 0.1, () -> this.page.getValue() == AutoArrow.Page.Release && this.predictTarget.getValue() && this.releaseMode.getValue() == AutoArrow.ReleaseMode.Smart));
      this.crossbowSupport = this.add(new BooleanSetting("CrossbowSupport", false));
      this.autoRelease = this.add(new BooleanSetting("AutoRelease", true, () -> this.releaseMode.getValue() != AutoArrow.ReleaseMode.None));
      this.target = null;
      this.bowSlot = -1;
      this.startSlot = -1;
      this.switched = false;
      this.setChinese("自动弓箭");
   }

   public void onEnable() {
      this.target = null;
      this.bowSlot = -1;
      this.startSlot = -1;
      this.switched = false;
   }

   public void onDisable() {
      this.target = null;
      this.bowSlot = -1;
      this.startSlot = -1;
      this.switched = false;
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         int bowSlot = this.findBow();
         boolean hasBow = bowSlot != -1;
         boolean isHoldingBow = this.isHoldingBow();
         if (hasBow) {
            this.bowSlot = bowSlot;
            if (this.switchToBow.getValue() && !isHoldingBow && (this.page.getValue() == AutoArrow.Page.AutoBow || this.page.getValue() == AutoArrow.Page.Both)) {
               if (this.startSlot == -1) {
                  this.startSlot = mc.player.getInventory().selectedSlot;
               }

               InventoryUtil.switchToSlot(this.bowSlot);
               this.switched = true;
               return;
            }

            if (this.switched && !isHoldingBow) {
               InventoryUtil.switchToSlot(this.startSlot);
               this.switched = false;
            }
         }

         if (this.page.getValue() == AutoArrow.Page.AimBot || this.page.getValue() == AutoArrow.Page.Both) {
            this.target = this.getTarget(this.range.getValue());
         }

         if ((this.page.getValue() == AutoArrow.Page.AutoBow || this.page.getValue() == AutoArrow.Page.Both) && hasBow && isHoldingBow && !mc.player.isUsingItem()) {
            mc.options.useKey.setPressed(true);
         }

         if (this.autoRelease.getValue() && this.page.getValue() != AutoArrow.Page.AimBot && isHoldingBow && mc.player.isUsingItem()) {
            this.doAutoRelease();
         }

      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.target != null) {
         if (this.page.getValue() != AutoArrow.Page.AutoBow && this.page.getValue() != AutoArrow.Page.Release) {
            Vec3d aimPoint = this.getAimPoint(this.target);
            if (aimPoint != null) {
               RotationManager var10000 = Arc.ROTATION;
               float[] rot = RotationManager.getRotation(aimPoint);
               float yawDelta = MathHelper.wrapDegrees(rot[0] - Arc.ROTATION.rotationYaw);
               float pitchDelta = rot[1] - Arc.ROTATION.rotationPitch;
               if (!((double)(yawDelta * yawDelta + pitchDelta * pitchDelta) > this.fov.getValue() * this.fov.getValue())) {
                  event.setRotation(rot[0], rot[1], (float)this.steps.getValue(), (float)this.priority.getValue());
               }
            }
         }
      }
   }

   private void doAutoRelease() {
      float charge = getBowPullProgress(mc.player.getItemUseTime());
      boolean canRelease = false;
      if (this.releaseMode.getValue() == AutoArrow.ReleaseMode.Smart) {
         canRelease = (double)charge >= this.minReleaseCharge.getValue();
      } else if (this.releaseMode.getValue() == AutoArrow.ReleaseMode.Fixed) {
         long ticks = this.tpsSync.getValue() ? (long)(this.fixedTicks.getValue() * (double)Arc.SERVER.getTPSFactor()) : (long)this.fixedTicks.getValue();
         canRelease = (long)mc.player.getItemUseTime() >= ticks;
      }

      if (canRelease) {
         mc.options.useKey.setPressed(false);
         mc.interactionManager.stopUsingItem(mc.player);
      }

   }

   private Entity getTarget(double range) {
      Entity target = null;
      double closestDist = Double.MAX_VALUE;
      float maxHealth = Float.MAX_VALUE;

      for(Entity entity : Arc.THREAD.getEntities()) {
         if (entity instanceof PlayerEntity player) {
            if (CombatUtil.isValid(player) && (!this.ignoreFriends.getValue() || !Arc.FRIEND.isFriend(player))) {
               double dist = mc.player.getEyePos().distanceTo(player.getEyePos());
               if (!(dist > range)) {
                  if (this.targetMode.getValue() == AutoArrow.TargetMode.HEALTH) {
                     float health = EntityUtil.getHealth(player);
                     if (target == null || health < maxHealth) {
                        target = player;
                        maxHealth = health;
                     }
                  } else if (dist < closestDist) {
                     closestDist = dist;
                     target = player;
                  }
               }
            }
         }
      }

      return target;
   }

   private Vec3d getAimPoint(Entity target) {
      Box bb = target.getBoundingBox();
      if (this.lookAtHead.getValue() && target instanceof LivingEntity) {
         float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
         return new Vec3d(MathUtil.interpolate(target.lastRenderX, target.getX(), (double)tickDelta), bb.maxY - 0.1, MathUtil.interpolate(target.lastRenderZ, target.getZ(), (double)tickDelta));
      } else {
         return MathUtil.getClosestPointToBox(mc.player.getEyePos(), bb);
      }
   }

   private int findBow() {
      for(int i = 0; i < 9; ++i) {
         if (this.crossbowSupport.getValue() && mc.player.getInventory().getStack(i).getItem() == Items.CROSSBOW) {
            return i;
         }

         if (mc.player.getInventory().getStack(i).getItem() == Items.BOW) {
            return i;
         }
      }

      return -1;
   }

   private boolean isHoldingBow() {
      if (mc.player.getMainHandStack().getItem() != Items.BOW && mc.player.getOffHandStack().getItem() != Items.BOW) {
         return this.crossbowSupport.getValue() && (mc.player.getMainHandStack().getItem() == Items.CROSSBOW || mc.player.getOffHandStack().getItem() == Items.CROSSBOW);
      } else {
         return true;
      }
   }

   public static float getBowPullProgress(int useTime) {
      return Math.min((float)useTime / 20.0F, 1.0F);
   }

   public static enum Page {
      AimBot,
      AutoBow,
      Release,
      Both;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{AimBot, AutoBow, Release, Both};
      }
   }

   public static enum TargetMode {
      DISTANCE,
      HEALTH;

      // $FF: synthetic method
      private static TargetMode[] $values() {
         return new TargetMode[]{DISTANCE, HEALTH};
      }
   }

   public static enum ReleaseMode {
      None,
      Smart,
      Fixed;

      // $FF: synthetic method
      private static ReleaseMode[] $values() {
         return new ReleaseMode[]{None, Smart, Fixed};
      }
   }
}
