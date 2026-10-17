package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.RotationEvent;
import dev.baimaodaxia.api.events.impl.SendMovementPacketsEvent;
import dev.baimaodaxia.api.events.impl.TickEvent;
import dev.baimaodaxia.api.utils.math.Animation;
import dev.baimaodaxia.api.utils.math.Easing;
import dev.baimaodaxia.api.utils.math.MathUtil;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.JelloUtil;
import dev.baimaodaxia.api.utils.render.Render3DUtil;
import dev.baimaodaxia.asm.accessors.IEntity;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.movement.ElytraFly;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;

public class Follower extends Module {
   public static Follower INSTANCE;
   public Entity target;
   private final EnumSetting<Mode> mode;
   private final SliderSetting range;
   private final BooleanSetting onlyElytraFly;
   private final BooleanSetting autoElytraMode;
   private final EnumSetting<ElytraFly.Mode> elytraMode;
   private ElytraFly.Mode previousMode;
   private final EnumSetting<TargetESP> renderMode;
   private final SliderSetting animationTime;
   private final EnumSetting<Easing> ease;
   private final ColorSetting color;
   private final ColorSetting outlineColor;
   private final ColorSetting hitColor;
   private final ColorSetting hitOutlineColor;
   private final Animation animation;
   private final BooleanSetting render;
   private final SliderSetting rotationSpeed;
   private final SliderSetting priority;

   public Follower() {
      super("Follower", Module.Category.Combat);
      this.mode = this.add(new EnumSetting("Mode", Follower.Mode.LowestDistance));
      this.range = this.add(new SliderSetting("Range", (double)50.0F, (double)5.0F, (double)100.0F));
      this.onlyElytraFly = this.add(new BooleanSetting("OnlyElytraFly", true));
      this.autoElytraMode = this.add(new BooleanSetting("AutoElytraMode", true));
      ElytraFly.Mode var10005 = ElytraFly.Mode.Freeze;
      BooleanSetting var10006 = this.autoElytraMode;
      Objects.requireNonNull(var10006);
      this.elytraMode = this.add(new EnumSetting("ElytraMode", var10005, var10006::getValue));
      this.previousMode = null;
      this.renderMode = this.add(new EnumSetting("RenderMode", Follower.TargetESP.Fill));
      this.animationTime = this.add(new SliderSetting("AnimationTime", (double)200.0F, (double)0.0F, (double)2000.0F, (double)1.0F));
      this.ease = this.add(new EnumSetting("Ease", Easing.CubicInOut));
      this.color = this.add(new ColorSetting("Color", new Color(160, 0, 225, 50)));
      this.outlineColor = this.add(new ColorSetting("OutlineColor", new Color(255, 255, 255, 100)));
      this.hitColor = this.add(new ColorSetting("HitColor", new Color(255, 255, 255, 150)));
      this.hitOutlineColor = this.add(new ColorSetting("HitOutlineColor", new Color(255, 255, 255, 150)));
      this.animation = new Animation();
      this.render = this.add(new BooleanSetting("Render", true));
      this.rotationSpeed = this.add(new SliderSetting("RotationSpeed", (double)180.0F, (double)10.0F, (double)360.0F));
      this.priority = this.add(new SliderSetting("Priority", (double)5.0F, (double)0.0F, (double)10.0F));
      this.setChinese("自动追踪");
      INSTANCE = this;
   }

   public String getInfo() {
      return this.target == null ? null : this.target.getName().getString();
   }

   public void onEnable() {
      if (this.autoElytraMode.getValue() && ElytraFly.INSTANCE.isOn()) {
         this.previousMode = ElytraFly.INSTANCE.mode.getValue();
         ElytraFly.INSTANCE.mode.setValue(this.elytraMode.getValue());
      }

   }

   public void onDisable() {
      if (this.autoElytraMode.getValue() && this.previousMode != null && ElytraFly.INSTANCE.isOn()) {
         ElytraFly.INSTANCE.mode.setValue(this.previousMode);
         this.previousMode = null;
      }

      this.target = null;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (!nullCheck()) {
         if (this.onlyElytraFly.getValue() && !ElytraFly.INSTANCE.isOn()) {
            this.target = null;
            return;
         }

         this.target = this.findTarget();
      }

   }

   @EventListener
   public void onSendMovementPackets(SendMovementPacketsEvent event) {
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (!nullCheck() && this.target != null && (!this.onlyElytraFly.getValue() || ElytraFly.INSTANCE.isOn())) {
         Vec3d hitVec = this.getHitVec(this.target);
         event.setTarget(hitVec, this.rotationSpeed.getValueFloat(), this.priority.getValueFloat());
      }

   }

   public void onRender3D(MatrixStack matrixStack) {
      if (this.target != null && this.render.getValue()) {
         this.doRender(matrixStack, mc.getRenderTickCounter().getTickDelta(true), this.target);
      }

   }

   public void doRender(MatrixStack stack, float partialTicks, Entity entity) {
      switch (((TargetESP)this.renderMode.getValue()).ordinal()) {
         case 0 -> Render3DUtil.draw3DBox(stack, ((IEntity)entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks))).expand((double)0.0F, 0.1, (double)0.0F), ColorUtil.fadeColor(this.color.getValue(), this.hitColor.getValue(), this.animation.get((double)0.0F, (long)this.animationTime.getValueInt(), this.ease.getValue())), ColorUtil.fadeColor(this.outlineColor.getValue(), this.hitOutlineColor.getValue(), this.animation.get((double)0.0F, (long)this.animationTime.getValueInt(), this.ease.getValue())), false, true);
         case 1 -> Render3DUtil.draw3DBox(stack, ((IEntity)entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks))).expand((double)0.0F, 0.1, (double)0.0F), ColorUtil.fadeColor(this.color.getValue(), this.hitColor.getValue(), this.animation.get((double)0.0F, (long)this.animationTime.getValueInt(), this.ease.getValue())), ColorUtil.fadeColor(this.outlineColor.getValue(), this.hitOutlineColor.getValue(), this.animation.get((double)0.0F, (long)this.animationTime.getValueInt(), this.ease.getValue())), true, true);
         case 2 -> JelloUtil.drawJello(stack, entity, this.color.getValue());
         case 3 -> Render3DUtil.drawTargetEsp(stack, this.target, this.color.getValue());
      }

   }

   private Entity findTarget() {
      Entity bestTarget = null;

      for(Entity entity : Alien.THREAD.getEntities()) {
         if (this.isValidTarget(entity)) {
            if (bestTarget == null) {
               bestTarget = entity;
            } else {
               double distance = mc.player.getEyePos().distanceTo(entity.getPos());
               float health = EntityUtil.getHealth(entity);
               switch (((Mode)this.mode.getValue()).ordinal()) {
                  case 0:
                     double bestDistance = mc.player.getEyePos().distanceTo(bestTarget.getPos());
                     if (distance < bestDistance) {
                        bestTarget = entity;
                     }
                     break;
                  case 1:
                     float bestHealth = EntityUtil.getHealth(bestTarget);
                     if (health < bestHealth) {
                        bestTarget = entity;
                     } else if (health == bestHealth) {
                        double currentTargetDistance = mc.player.getEyePos().distanceTo(bestTarget.getPos());
                        if (distance < currentTargetDistance) {
                           bestTarget = entity;
                        }
                     }
               }
            }
         }
      }

      return bestTarget;
   }

   private boolean isValidTarget(Entity entity) {
      if (entity instanceof PlayerEntity player) {
         if (entity == mc.player) {
            return false;
         } else if (Alien.FRIEND.isFriend(player)) {
            return false;
         } else if (!entity.isAlive()) {
            return false;
         } else {
            double distance = mc.player.getEyePos().distanceTo(entity.getPos());
            return !(distance > this.range.getValue());
         }
      } else {
         return false;
      }
   }

   private Vec3d getHitVec(Entity entity) {
      Vec3d eyePos = mc.player.getEyePos();
      Box box = entity.getBoundingBox();
      double targetCenterX = (box.minX + box.maxX) / (double)2.0F;
      double targetCenterY = (box.minY + box.maxY) / (double)2.0F;
      double targetCenterZ = (box.minZ + box.maxZ) / (double)2.0F;
      double playerEyeHeight = (double)mc.player.getEyeHeight(mc.player.getPose());
      double heightOffset = playerEyeHeight * (double)0.5F;
      if (entity instanceof PlayerEntity) {
         heightOffset = (double)entity.getHeight() * (double)0.5F;
      }

      double targetY = box.minY + heightOffset;
      double closestX = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
      double closestZ = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));
      return new Vec3d(closestX, targetY, closestZ);
   }

   public static enum Mode {
      LowestDistance,
      LowestHealth;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{LowestDistance, LowestHealth};
      }
   }

   public static enum TargetESP {
      Fill,
      Box,
      Jello,
      ThunderHack;

      // $FF: synthetic method
      private static TargetESP[] $values() {
         return new TargetESP[]{Fill, Box, Jello, ThunderHack};
      }
   }
}
