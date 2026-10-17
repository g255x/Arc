package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.PacketEvent;
import dev.G255.Lumi.api.events.impl.RotationEvent;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.math.Animation;
import dev.G255.Lumi.api.utils.math.Easing;
import dev.G255.Lumi.api.utils.math.MathUtil;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.render.ColorUtil;
import dev.G255.Lumi.api.utils.render.JelloUtil;
import dev.G255.Lumi.api.utils.render.Render3DUtil;
import dev.G255.Lumi.asm.accessors.IEntity;
import dev.G255.Lumi.asm.accessors.ILivingEntity;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.exploit.Blink;
import dev.G255.Lumi.mod.modules.impl.movement.ElytraFly;
import dev.G255.Lumi.mod.modules.impl.movement.Velocity;
import dev.G255.Lumi.mod.modules.settings.enums.SwingSide;
import dev.G255.Lumi.mod.modules.settings.enums.Timing;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.ColorSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.attribute.EntityAttributes;

public class Aura extends Module {
   public static Aura INSTANCE;
   public static Entity target;
   public final EnumSetting page;
   public final SliderSetting range;
   private final SliderSetting targetRange;
   private final EnumSetting cooldownMode;
   private final BooleanSetting reset;
   private final EnumSetting swingMode;
   private final SliderSetting hurtTime;
   private final SliderSetting cooldown;
   private final SliderSetting wallRange;
   private final BooleanSetting whileEating;
   private final BooleanSetting weaponOnly;
   private final EnumSetting timing;
   private final BooleanSetting Players;
   private final BooleanSetting armorLow;
   private final BooleanSetting Mobs;
   private final BooleanSetting Animals;
   private final BooleanSetting Villagers;
   private final BooleanSetting Slimes;
   private final EnumSetting targetMode;
   private final EnumSetting mode;
   private final SliderSetting animationTime;
   private final EnumSetting ease;
   private final ColorSetting color;
   private final ColorSetting outlineColor;
   private final ColorSetting hitColor;
   private final ColorSetting hitOutlineColor;
   private final Animation animation;
   private final BooleanSetting rotate;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   private final Timer tick;
   public Vec3d directionVec;

   public Aura() {
      super("Aura", Module.Category.Combat);
      this.page = this.add(new EnumSetting("Page", Aura.Page.General));
      this.range = this.add(new SliderSetting("Range", (double)6.0F, (double)0.1F, (double)7.0F, () -> this.page.getValue() == Aura.Page.General));
      this.targetRange = this.add(new SliderSetting("TargetRange", (double)8.0F, (double)0.1F, (double)14.0F, () -> this.page.getValue() == Aura.Page.General));
      this.cooldownMode = this.add(new EnumSetting("CooldownMode", Aura.Cooldown.Delay, () -> this.page.getValue() == Aura.Page.General));
      this.reset = this.add(new BooleanSetting("Reset", true, () -> this.page.getValue() == Aura.Page.General && this.cooldownMode.is(Aura.Cooldown.Delay)));
      this.swingMode = this.add(new EnumSetting("Swing", SwingSide.All, () -> this.page.getValue() == Aura.Page.General));
      this.hurtTime = this.add(new SliderSetting("HurtTime", (double)10.0F, (double)0.0F, (double)10.0F, (double)1.0F, () -> this.page.getValue() == Aura.Page.General));
      this.cooldown = this.add(new SliderSetting("Cooldown", (double)1.1F, (double)0.0F, (double)1.2F, 0.01, () -> this.page.getValue() == Aura.Page.General));
      this.wallRange = this.add(new SliderSetting("WallRange", (double)6.0F, (double)0.1F, (double)7.0F, () -> this.page.getValue() == Aura.Page.General));
      this.whileEating = this.add(new BooleanSetting("WhileUsing", true, () -> this.page.getValue() == Aura.Page.General));
      this.weaponOnly = this.add(new BooleanSetting("WeaponOnly", true, () -> this.page.getValue() == Aura.Page.General));
      this.timing = this.add(new EnumSetting("Timing", Timing.All, () -> this.page.getValue() == Aura.Page.General));
      this.Players = this.add((new BooleanSetting("Players", true, () -> this.page.getValue() == Aura.Page.Target)).setParent());
      this.armorLow = this.add(new BooleanSetting("ArmorLow", true, () -> this.page.getValue() == Aura.Page.Target && this.Players.isOpen()));
      this.Mobs = this.add(new BooleanSetting("Mobs", true, () -> this.page.getValue() == Aura.Page.Target));
      this.Animals = this.add(new BooleanSetting("Animals", true, () -> this.page.getValue() == Aura.Page.Target));
      this.Villagers = this.add(new BooleanSetting("Villagers", true, () -> this.page.getValue() == Aura.Page.Target));
      this.Slimes = this.add(new BooleanSetting("Slimes", true, () -> this.page.getValue() == Aura.Page.Target));
      this.targetMode = this.add(new EnumSetting("Filter", Aura.TargetMode.DISTANCE, () -> this.page.getValue() == Aura.Page.Target));
      this.mode = this.add(new EnumSetting("TargetESP", Aura.TargetESP.Fill, () -> this.page.getValue() == Aura.Page.Render));
      this.animationTime = this.add(new SliderSetting("AnimationTime", (double)200.0F, (double)0.0F, (double)2000.0F, (double)1.0F, () -> this.page.getValue() == Aura.Page.Render));
      this.ease = this.add(new EnumSetting("Ease", Easing.CubicInOut, () -> this.page.getValue() == Aura.Page.Render));
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 50), () -> this.page.getValue() == Aura.Page.Render));
      this.outlineColor = this.add(new ColorSetting("OutlineColor", new Color(255, 255, 255, 50), () -> this.page.getValue() == Aura.Page.Render));
      this.hitColor = this.add(new ColorSetting("HitColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == Aura.Page.Render));
      this.hitOutlineColor = this.add(new ColorSetting("HitOutlineColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == Aura.Page.Render));
      this.animation = new Animation();
      this.rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == Aura.Page.Rotate));
      this.yawStep = this.add((new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.getValue() == Aura.Page.Rotate)).setParent());
      this.whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == Aura.Page.Rotate));
      this.steps = this.add(new SliderSetting("Steps", 0.05, (double)0.0F, (double)1.0F, 0.01, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen()));
      this.checkFov = this.add(new BooleanSetting("OnlyLooking", true, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen()));
      this.fov = this.add(new SliderSetting("Fov", (double)20.0F, (double)0.0F, (double)360.0F, 0.1, () -> this.checkFov.getValue() && this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen()));
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen()));
      this.tick = new Timer();
      this.directionVec = null;
      this.setChinese("");
      INSTANCE = this;
   }

   public static void doRender(MatrixStack stack, float partialTicks, Entity entity, Color color, Color outlineColor, TargetESP mode) {
      switch (mode.ordinal()) {
         case 0 -> Render3DUtil.draw3DBox(stack, ((IEntity)entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks))).expand((double)0.0F, 0.1, (double)0.0F), color, outlineColor, false, true);
         case 1 -> Render3DUtil.draw3DBox(stack, ((IEntity)entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks))).expand((double)0.0F, 0.1, (double)0.0F), color, outlineColor, true, true);
         case 2 -> JelloUtil.drawJello(stack, entity, color);
         case 3 -> Render3DUtil.drawTargetEsp(stack, target, color);
      }

   }

   public static float getAttackCooldownProgressPerTick() {
      return (float)((double)1.0F / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * (double)20.0F);
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (target != null) {
         this.doRender(matrixStack, mc.getRenderTickCounter().getTickDelta(true), target, (TargetESP)this.mode.getValue());
      }

   }

   public void doRender(MatrixStack stack, float partialTicks, Entity entity, TargetESP mode) {
      switch (mode.ordinal()) {
         case 0 -> Render3DUtil.draw3DBox(stack, ((IEntity)entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks))).expand((double)0.0F, 0.1, (double)0.0F), ColorUtil.fadeColor(this.color.getValue(), this.hitColor.getValue(), this.animation.get((double)0.0F, (long)this.animationTime.getValueInt(), (Easing)this.ease.getValue())), ColorUtil.fadeColor(this.outlineColor.getValue(), this.hitOutlineColor.getValue(), this.animation.get((double)0.0F, (long)this.animationTime.getValueInt(), (Easing)this.ease.getValue())), false, true);
         case 1 -> Render3DUtil.draw3DBox(stack, ((IEntity)entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks))).expand((double)0.0F, 0.1, (double)0.0F), ColorUtil.fadeColor(this.color.getValue(), this.hitColor.getValue(), this.animation.get((double)0.0F, (long)this.animationTime.getValueInt(), (Easing)this.ease.getValue())), ColorUtil.fadeColor(this.outlineColor.getValue(), this.hitOutlineColor.getValue(), this.animation.get((double)0.0F, (long)this.animationTime.getValueInt(), (Easing)this.ease.getValue())), true, true);
         case 2 -> JelloUtil.drawJello(stack, entity, this.color.getValue());
         case 3 -> Render3DUtil.drawTargetEsp(stack, target, this.color.getValue());
      }

   }

   public String getInfo() {
      return target == null ? null : target.getName().getString();
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            if (this.weaponOnly.getValue() && !EntityUtil.isHoldingWeapon(mc.player)) {
               target = null;
            } else {
               target = this.getTarget((double)this.range.getValueFloat());
               if (target == null) {
                  target = this.getTarget((double)this.targetRange.getValueFloat());
               } else {
                  this.doAura();
               }
            }
         }
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (target != null && this.rotate.getValue() && this.shouldYawStep()) {
         this.directionVec = this.getAttackVec(target);
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }

   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      Packet<?> packet;
      if (this.reset.getValue() && ((packet = event.getPacket()) instanceof HandSwingC2SPacket || packet instanceof PlayerInteractEntityC2SPacket && Criticals.getInteractType((PlayerInteractEntityC2SPacket)packet) == PlayerInteractEntityC2SPacket.InteractType.ATTACK)) {
         this.tick.reset();
      }

   }

   private boolean check() {
      if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
         return false;
      } else {
         int at = (int)(this.tick.getMs() / 50L);
         if (this.cooldownMode.getValue() == Aura.Cooldown.Vanilla) {
            at = ((ILivingEntity)mc.player).getLastAttackedTicks();
         }

         if (!((double)Math.max((float)((int)((float)at * Lumi.SERVER.getTPSFactor())) / getAttackCooldownProgressPerTick(), 0.0F) >= this.cooldown.getValue())) {
            return false;
         } else {
            Entity entity = target;
            if (entity instanceof LivingEntity) {
               LivingEntity entity2 = (LivingEntity)entity;
               if ((double)entity2.hurtTime > this.hurtTime.getValue()) {
                  return false;
               }
            }

            return this.whileEating.getValue() || !mc.player.isUsingItem();
         }
      }
   }

   private void doAura() {
      if (this.check()) {
         if (!this.rotate.getValue() || this.faceVector(this.getAttackVec(target))) {
            this.animation.to = (double)1.0F;
            this.animation.from = (double)1.0F;
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            mc.player.resetLastAttackedTicks();
            EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)this.swingMode.getValue());
            this.tick.reset();
            if (this.rotate.getValue() && !this.shouldYawStep()) {
               Lumi.ROTATION.snapBack();
            }

         }
      }
   }

   private Vec3d getAttackVec(Entity entity) {
      return MathUtil.getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
   }

   private boolean shouldYawStep() {
      if (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())) {
         return this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
      } else {
         return false;
      }
   }

   public boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         Lumi.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         if (Lumi.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
            return true;
         } else {
            return !this.checkFov.getValue();
         }
      }
   }

   public Entity getTarget(double range) {
      Entity target = null;
      double getDistance = range;
      double maxHealth = (double)36.0F;

      for(Entity entity : Lumi.THREAD.getEntities()) {
         if (this.isEnemy(entity)) {
            Vec3d hitVec = this.getAttackVec(entity);
            if (!(mc.player.getEyePos().distanceTo(hitVec) > range) && (mc.player.canSee(entity) || !(mc.player.getEyePos().distanceTo(hitVec) > this.wallRange.getValue())) && CombatUtil.isValid(entity)) {
               if (target == null) {
                  target = entity;
                  getDistance = mc.player.getEyePos().distanceTo(hitVec);
                  maxHealth = (double)EntityUtil.getHealth(entity);
               } else {
                  if (this.armorLow.getValue() && entity instanceof PlayerEntity && EntityUtil.isArmorLow((PlayerEntity)entity, 10)) {
                     target = entity;
                     break;
                  }

                  if (this.targetMode.getValue() == Aura.TargetMode.HEALTH && (double)EntityUtil.getHealth(entity) < maxHealth) {
                     target = entity;
                     maxHealth = (double)EntityUtil.getHealth(entity);
                  } else if (this.targetMode.getValue() == Aura.TargetMode.DISTANCE && mc.player.getEyePos().distanceTo(hitVec) < getDistance) {
                     target = entity;
                     getDistance = mc.player.getEyePos().distanceTo(hitVec);
                  }
               }
            }
         }
      }

      return target;
   }

   private boolean isEnemy(Entity entity) {
      if (entity instanceof SlimeEntity) {
         return this.Slimes.getValue();
      } else if (entity instanceof PlayerEntity) {
         return this.Players.getValue();
      } else if (!(entity instanceof VillagerEntity) && !(entity instanceof WanderingTraderEntity)) {
         if (entity instanceof AnimalEntity) {
            return this.Animals.getValue();
         } else {
            return entity instanceof MobEntity ? this.Mobs.getValue() : false;
         }
      } else {
         return this.Villagers.getValue();
      }
   }

   public static enum Page {
      General,
      Rotate,
      Target,
      Render;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Rotate, Target, Render};
      }
   }

   public static enum Cooldown {
      Vanilla,
      Delay;

      // $FF: synthetic method
      private static Cooldown[] $values() {
         return new Cooldown[]{Vanilla, Delay};
      }
   }

   private static enum TargetMode {
      DISTANCE,
      HEALTH;

      // $FF: synthetic method
      private static TargetMode[] $values() {
         return new TargetMode[]{DISTANCE, HEALTH};
      }
   }

   public static enum TargetESP {
      Fill,
      Box,
      Jello,
      ThunderHack,
      None;

      // $FF: synthetic method
      private static TargetESP[] $values() {
         return new TargetESP[]{Fill, Box, Jello, ThunderHack, None};
      }
   }

   public static enum Mode {
      Mace,
      Axe,
      Sword;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Mace, Axe, Sword};
      }
   }
}
