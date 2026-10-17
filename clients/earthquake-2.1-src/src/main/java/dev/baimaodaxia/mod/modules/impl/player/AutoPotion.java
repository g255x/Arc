package dev.baimaodaxia.mod.modules.impl.player;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.ClientTickEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.api.utils.world.BlockPosX;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BindSetting;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.item.Item;

public class AutoPotion extends Module {
   public static AutoPotion INSTANCE;
   private final SliderSetting delay = this.add((new SliderSetting("Delay", (double)0.5F, (double)0.0F, (double)10.0F, 0.1)).setSuffix("s"));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final SliderSetting pitch = this.add(new SliderSetting("Pitch", (double)90.0F, (double)70.0F, (double)90.0F, (double)1.0F));
   private final BooleanSetting raytrace = this.add(new BooleanSetting("Raytrace", true));
   private final SliderSetting predictTicks = this.add(new SliderSetting("PredictTicks", (double)2.0F, (double)0.0F, (double)10.0F, (double)1.0F, () -> this.raytrace.getValue()));
   private final SliderSetting effectRange = this.add(new SliderSetting("EffectRange", (double)3.0F, (double)0.0F, (double)6.0F, 0.1, () -> this.raytrace.getValue()));
   private final BooleanSetting healthCheck = this.add(new BooleanSetting("HealthCheck", true));
   private final SliderSetting health = this.add((new SliderSetting("Health", (double)16.0F, (double)1.0F, (double)36.0F, (double)0.5F, () -> this.healthCheck.getValue())).setSuffix("HP"));
   private final BooleanSetting speed = this.add(new BooleanSetting("Speed", false));
   private final BooleanSetting resistance = this.add(new BooleanSetting("Resistance", false));
   private final BooleanSetting strength = this.add(new BooleanSetting("Strength", false));
   private final BooleanSetting slowFalling = this.add(new BooleanSetting("SlowFalling", false));
   private final BooleanSetting earlyThrow = this.add(new BooleanSetting("EarlyThrow", true));
   private final SliderSetting earlyThrowTime = this.add((new SliderSetting("EarlyThrowTime", 0.4, (double)0.0F, (double)5.0F, 0.1)).setSuffix("s"));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BindSetting speedKey = this.add(new BindSetting("SpeedKey", -1));
   private final BindSetting strengthKey = this.add(new BindSetting("StrengthKey", -1));
   private final BindSetting resistanceKey = this.add(new BindSetting("ResistanceKey", -1));
   private final Timer delayTimer = new Timer();
   private boolean throwing = false;
   private boolean turtlePress;
   private boolean speedPress;
   private boolean strengthPress;

   public AutoPotion() {
      super("AutoPotion", Module.Category.Player);
      this.setChinese("自动药水");
      INSTANCE = this;
      earthquake.EVENT_BUS.subscribe(new AutoPotTick());
   }

   public void onDisable() {
      this.throwing = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if ((!this.inventory.getValue() || EntityUtil.inInventory()) && this.delayTimer.passedMs(this.delay.getValue() * (double)1000.0F)) {
         if (this.onlyGround.getValue() && !mc.player.isOnGround() && mc.world.isAir(new BlockPosX(mc.player.getPos().add((double)0.0F, (double)-1.0F, (double)0.0F)))) {
            return;
         }

         if (this.resistance.getValue()) {
            boolean healthLow = !this.healthCheck.getValue() || (double)(mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= this.health.getValue();
            StatusEffectInstance resistInstance = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
            boolean hasResist = resistInstance != null;
            boolean shouldThrow = false;
            if (this.earlyThrow.getValue() && hasResist) {
               int ticksLeft = resistInstance.getDuration();
               if ((double)ticksLeft <= this.earlyThrowTime.getValue() * (double)20.0F) {
                  shouldThrow = true;
               }
            }

            if (!hasResist || healthLow && resistInstance.getAmplifier() < 1) {
               shouldThrow = true;
            }

            if (shouldThrow) {
               this.throwing = this.checkThrow((StatusEffect)StatusEffects.RESISTANCE.value());
               if (this.isThrow()) {
                  this.throwPotion((StatusEffect)StatusEffects.RESISTANCE.value());
                  return;
               }
            }
         }

         if (this.speed.getValue()) {
            StatusEffectInstance speedInstance = mc.player.getStatusEffect(StatusEffects.SPEED);
            boolean hasSpeed = speedInstance != null;
            boolean shouldThrow = false;
            if (this.earlyThrow.getValue() && hasSpeed && (double)speedInstance.getDuration() <= this.earlyThrowTime.getValue() * (double)20.0F) {
               shouldThrow = true;
            }

            if (!hasSpeed) {
               shouldThrow = true;
            }

            if (shouldThrow) {
               this.throwing = this.checkThrow((StatusEffect)StatusEffects.SPEED.value());
               if (this.isThrow()) {
                  this.throwPotion((StatusEffect)StatusEffects.SPEED.value());
                  return;
               }
            }
         }

         if (this.strength.getValue()) {
            StatusEffectInstance strengthInstance = mc.player.getStatusEffect(StatusEffects.STRENGTH);
            boolean hasStrength = strengthInstance != null;
            boolean shouldThrow = false;
            if (this.earlyThrow.getValue() && hasStrength && (double)strengthInstance.getDuration() <= this.earlyThrowTime.getValue() * (double)20.0F) {
               shouldThrow = true;
            }

            if (!hasStrength) {
               shouldThrow = true;
            }

            if (shouldThrow) {
               this.throwing = this.checkThrow((StatusEffect)StatusEffects.STRENGTH.value());
               if (this.isThrow()) {
                  this.throwPotion((StatusEffect)StatusEffects.STRENGTH.value());
                  return;
               }
            }
         }

         if (this.slowFalling.getValue()) {
            StatusEffectInstance slowFallingInstance = mc.player.getStatusEffect(StatusEffects.SLOW_FALLING);
            boolean hasSlowFalling = slowFallingInstance != null;
            boolean shouldThrow = false;
            if (this.earlyThrow.getValue() && hasSlowFalling && (double)slowFallingInstance.getDuration() <= this.earlyThrowTime.getValue() * (double)20.0F) {
               shouldThrow = true;
            }

            if (!hasSlowFalling) {
               shouldThrow = true;
            }

            if (shouldThrow) {
               this.throwing = this.checkThrow((StatusEffect)StatusEffects.SLOW_FALLING.value());
               if (this.isThrow()) {
                  this.throwPotion((StatusEffect)StatusEffects.SLOW_FALLING.value());
               }
            }
         }
      }

   }

   public void throwPotion(StatusEffect targetEffect) {
      int oldSlot = mc.player.getInventory().selectedSlot;
      int newSlot = findPotion(targetEffect);
      boolean foundInInv = false;
      if (newSlot == -1 && this.inventory.getValue()) {
         newSlot = findPotionInventorySlot(targetEffect);
         foundInInv = true;
      }

      if (newSlot != -1) {
         if (this.rotate.getValue()) {
            earthquake.ROTATION.snapAt(earthquake.ROTATION.rotationYaw, this.pitch.getValueFloat());
         }

         if (foundInInv) {
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, earthquake.ROTATION.getLastYaw(), earthquake.ROTATION.getLastPitch()));
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
         } else {
            InventoryUtil.switchToSlot(newSlot);
            sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, earthquake.ROTATION.getLastYaw(), earthquake.ROTATION.getLastPitch()));
            InventoryUtil.switchToSlot(oldSlot);
         }

         if (this.rotate.getValue()) {
            earthquake.ROTATION.snapBack();
         }

         this.delayTimer.reset();
      }

   }

   public boolean isThrow() {
      return this.throwing;
   }

   public boolean checkThrow(StatusEffect targetEffect) {
      if (!EntityUtil.inInventory()) {
         return false;
      } else if (this.usingPause.getValue() && mc.player.isUsingItem()) {
         return false;
      } else {
         if (this.raytrace.getValue()) {
            Vec3d hitPos = this.calcTrajectory(Items.SPLASH_POTION, earthquake.ROTATION.rotationYaw, this.pitch.getValueFloat());
            if (hitPos == null) {
               return false;
            }

            Vec3d playerFuturePos = this.getPredictedPos(mc.player, this.predictTicks.getValueInt());
            if (playerFuturePos.squaredDistanceTo(hitPos) > this.effectRange.getValue() * this.effectRange.getValue()) {
               return false;
            }
         }

         return findPotion(targetEffect) != -1 || this.inventory.getValue() && findPotionInventorySlot(targetEffect) != -1;
      }
   }

   private Vec3d getPredictedPos(Entity entity, int ticks) {
      if (ticks == 0) {
         return entity.getPos();
      } else {
         Vec3d motion = new Vec3d(entity.getVelocity().x, entity.getVelocity().y, entity.getVelocity().z);
         return entity.getPos().add(motion.multiply((double)ticks));
      }
   }

   private Vec3d calcTrajectory(Item item, float yaw, float pitch) {
      float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
      double x = MathHelper.lerp((double)tickDelta, mc.player.prevX, mc.player.getX());
      double y = MathHelper.lerp((double)tickDelta, mc.player.prevY, mc.player.getY());
      double z = MathHelper.lerp((double)tickDelta, mc.player.prevZ, mc.player.getZ());
      y = y + (double)mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
      x -= (double)(MathHelper.cos(yaw / 180.0F * (float)Math.PI) * 0.16F);
      z -= (double)(MathHelper.sin(yaw / 180.0F * (float)Math.PI) * 0.16F);
      float maxDist = 0.4F;
      double motionX = (double)(-MathHelper.sin(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI) * 0.4F);
      double motionY = (double)(-MathHelper.sin((pitch - 20.0F) / 180.0F * 3.141593F) * 0.4F);
      double motionZ = (double)(MathHelper.cos(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI) * 0.4F);
      float distance = MathHelper.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
      motionX /= (double)distance;
      motionY /= (double)distance;
      motionZ /= (double)distance;
      float pow = 0.5F;
      motionX *= (double)0.5F;
      motionY *= (double)0.5F;
      motionZ *= (double)0.5F;
      if (!mc.player.isOnGround()) {
         motionY += mc.player.getVelocity().getY();
      }

      for(int i = 0; i < 300; ++i) {
         Vec3d lastPos = new Vec3d(x, y, z);
         x += motionX;
         y += motionY;
         z += motionZ;
         if (mc.world.getBlockState(new BlockPos((int)x, (int)y, (int)z)).getBlock() == Blocks.WATER) {
            motionX *= 0.8;
            motionY *= 0.8;
            motionZ *= 0.8;
         } else {
            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;
         }

         motionY -= (double)0.03F;
         Vec3d pos = new Vec3d(x, y, z);
         BlockHitResult bhr = mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         if (bhr != null && bhr.getType() == Type.BLOCK) {
            return bhr.getPos();
         }
      }

      return null;
   }

   public static int findPotionInventorySlot(StatusEffect targetEffect) {
      for(int i = 35; i >= 0; --i) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for(StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  return i < 9 ? i + 36 : i;
               }
            }
         }
      }

      return -1;
   }

   public static int findPotion(StatusEffect targetEffect) {
      for(int i = 0; i < 9; ++i) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for(StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  return i;
               }
            }
         }
      }

      return -1;
   }

   public class AutoPotTick {
      @EventListener
      public void onTick(ClientTickEvent event) {
         if (!Module.nullCheck() && !event.isPost() && (!AutoPotion.this.inventory.getValue() || EntityUtil.inInventory())) {
            if (Wrapper.mc.currentScreen == null) {
               if (AutoPotion.this.resistanceKey.isPressed()) {
                  if (!AutoPotion.this.turtlePress && AutoPotion.this.checkThrow((StatusEffect)StatusEffects.RESISTANCE.value())) {
                     AutoPotion.this.throwPotion((StatusEffect)StatusEffects.RESISTANCE.value());
                     AutoPotion.this.turtlePress = true;
                     return;
                  }
               } else {
                  AutoPotion.this.turtlePress = false;
               }

               if (AutoPotion.this.strengthKey.isPressed()) {
                  if (!AutoPotion.this.strengthPress && AutoPotion.this.checkThrow((StatusEffect)StatusEffects.STRENGTH.value())) {
                     AutoPotion.this.throwPotion((StatusEffect)StatusEffects.STRENGTH.value());
                     AutoPotion.this.strengthPress = true;
                     return;
                  }
               } else {
                  AutoPotion.this.strengthPress = false;
               }

               if (AutoPotion.this.speedKey.isPressed()) {
                  if (!AutoPotion.this.speedPress && AutoPotion.this.checkThrow((StatusEffect)StatusEffects.SPEED.value())) {
                     AutoPotion.this.throwPotion((StatusEffect)StatusEffects.SPEED.value());
                     AutoPotion.this.speedPress = true;
                  }
               } else {
                  AutoPotion.this.speedPress = false;
               }
            } else {
               AutoPotion.this.speedPress = false;
               AutoPotion.this.turtlePress = false;
               AutoPotion.this.strengthPress = false;
            }
         }

      }
   }
}
