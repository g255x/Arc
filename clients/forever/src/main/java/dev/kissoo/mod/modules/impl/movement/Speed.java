package dev.kissoo.mod.modules.impl.movement;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.MoveEvent;
import dev.kissoo.api.events.impl.MovedEvent;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.TravelEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.path.BaritoneUtil;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.api.utils.world.BlockUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class Speed extends Module {
   public static Speed INSTANCE;

   private final EnumSetting<Mode> mode = this.add(
           new EnumSetting<>("Mode", Mode.Strafe)
   );

   private final BooleanSetting windChargeBoost = this.add(
           new BooleanSetting("WindChargeBoost", false,
                   () -> this.mode.is(Mode.Strafe))
   );
   private final SliderSetting timeUntilBoost = this.add(
           new SliderSetting("TimeUntilBoost", 50.0, 0.0, 1000.0, 10.0,
                   () -> this.mode.is(Mode.Strafe) && this.windChargeBoost.getValue())
                   .setSuffix("ms")
   );
   private final SliderSetting boostDelay = this.add(
           new SliderSetting("BoostDelay", 2000.0, 500.0, 10000.0, 10.0,
                   () -> this.mode.is(Mode.Strafe) && this.windChargeBoost.getValue())
                   .setSuffix("ms")
   );
   private final SliderSetting boostTime = this.add(
           new SliderSetting("BoostTime", 1000.0, 100.0, 5000.0, 10.0,
                   () -> this.mode.is(Mode.Strafe) && this.windChargeBoost.getValue())
                   .setSuffix("ms")
   );
   private final SliderSetting boostSpeed = this.add(
           new SliderSetting("BoostSpeed", 0.6, 0.1, 2.0, 0.01,
                   () -> this.mode.is(Mode.Strafe) && this.windChargeBoost.getValue())
   );

   public final SliderSetting collideSpeed = this.add(
           new SliderSetting("CollideSpeed", 0.08, 0.0, 0.08, 0.01,
                   () -> this.mode.is(Mode.Grim))
   );
   private final BooleanSetting strict = this.add(
           new BooleanSetting("Strict", true,
                   () -> this.mode.is(Mode.Grim))
   );
   private final BooleanSetting boat = this.add(
           new BooleanSetting("BoatLongJump", true,
                   () -> this.mode.is(Mode.Grim))
   );
   public final SliderSetting boatExpand = this.add(
           new SliderSetting("BoatExpand", 0.2, 0.0, 1.0, 0.01,
                   () -> this.mode.is(Mode.Grim))
   );
   public final SliderSetting boatSpeed = this.add(
           new SliderSetting("BoatSpeed", 0.2, -2.0, 2.0, 0.01,
                   () -> this.mode.is(Mode.Grim))
   );
   public final SliderSetting boatJump = this.add(
           new SliderSetting("BoatJump", 0.2, 0.0, 2.0, 0.01,
                   () -> this.mode.is(Mode.Grim))
   );

   private final BooleanSetting inWater = this.add(
           new BooleanSetting("InWater", false,
                   () -> !this.mode.is(Mode.Grim) && !this.mode.is(Mode.Elytra))
   );
   private final BooleanSetting inBlock = this.add(
           new BooleanSetting("InBlock", false,
                   () -> !this.mode.is(Mode.Grim) && !this.mode.is(Mode.Elytra))
   );
   private final BooleanSetting airStop = this.add(
           new BooleanSetting("AirStop", false,
                   () -> !this.mode.is(Mode.Grim) && !this.mode.is(Mode.Elytra))
   );
   private final SliderSetting lagTime = this.add(
           new SliderSetting("LagTime", 500.0, 0.0, 1000.0, 1.0,
                   () -> !this.mode.is(Mode.Grim) && !this.mode.is(Mode.Elytra))
   );

   private final BooleanSetting jump = this.add(
           new BooleanSetting("Jump", true,
                   () -> this.mode.is(Mode.Strafe))
   );
   private final SliderSetting strafeSpeed = this.add(
           new SliderSetting("NormalSpeed", 0.2873, 0.0, 1.0, 1.0E-4,
                   () -> this.mode.is(Mode.Strafe))
   );
   private final BooleanSetting explosions = this.add(
           new BooleanSetting("ExplosionsBoost", false,
                   () -> this.mode.is(Mode.Strafe))
   );
   private final BooleanSetting velocity = this.add(
           new BooleanSetting("VelocityBoost", true,
                   () -> this.mode.is(Mode.Strafe))
   );
   private final SliderSetting multiplier = this.add(
           new SliderSetting("H-Factor", 1.0, 0.0, 5.0, 0.01,
                   () -> this.mode.is(Mode.Strafe))
   );
   private final SliderSetting vertical = this.add(
           new SliderSetting("V-Factor", 1.0, 0.0, 5.0, 0.01,
                   () -> this.mode.is(Mode.Strafe))
   );
   private final SliderSetting coolDown = this.add(
           new SliderSetting("CoolDown", 1000.0, 0.0, 5000.0, 1.0,
                   () -> this.mode.is(Mode.Strafe))
   );
   private final BooleanSetting slow = this.add(
           new BooleanSetting("Slowness", false,
                   () -> this.mode.is(Mode.Strafe))
   );
   private final SliderSetting elytraSpeed = this.add(
           new SliderSetting("Speed", 1.0, 0.1, 10.0,
                   () -> this.mode.is(Mode.Elytra))
   );

   private final SliderSetting elytraFireworkDelay = this.add(
           new SliderSetting("FireworkDelay", 1500.0, 200.0, 10000.0, 50.0,
                   () -> this.mode.is(Mode.Elytra))
                   .setSuffix("ms")
   );

   private final SliderSetting elytraDownSpeed = this.add(
           new SliderSetting("DownSpeed", 1.0, 0.1, 10.0,
                   () -> this.mode.is(Mode.Elytra))
   );

   private final Timer elytraFireworkTimer = new Timer();
   private final Timer windChargeTimer = new Timer();

   private boolean elytraHasElytra = false;
   private boolean elytraSwapping = false;
   private final Timer expTimer = new Timer();
   private final Timer lagTimer = new Timer();
   private boolean stop;
   private double speed;
   private double distance;
   private int strictTicks;
   private int strafe = 4;
   private int stage;
   private double lastExp;
   private boolean boost;

   private long boostStartTime = 0L;
   private boolean firstLaunch = true;

   public Speed() {
      super("Speed", Module.Category.Movement);
      this.setChinese("加速");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.speed = MovementUtil.getSpeed(false);
         this.distance = MovementUtil.getDistance2D();
      }
      this.stage = 4;

      if (this.mode.is(Mode.Elytra)) {
         elytraHasElytra = false;
         elytraSwapping = false;
         elytraFireworkTimer.setMs(99999999L);
      }
      windChargeTimer.reset();
      this.firstLaunch = true;
      this.boostStartTime = 0L;
   }

   @Override
   public void onDisable() {
      if (this.mode.is(Mode.Elytra)) {
         elytraHasElytra = false;
         elytraSwapping = false;
      }
   }

   @EventListener(priority = 100)
   public void invoke(PacketEvent.Receive event) {
      if (!BaritoneUtil.isActive()) {
         if (this.mode.is(Mode.Strafe)) {
            if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
               if (mc.player != null
                       && packet.getEntityId() == mc.player.getId()
                       && this.velocity.getValue()) {
                  double spd = Math.sqrt(
                          packet.getVelocityX() * packet.getVelocityX()
                                  + packet.getVelocityZ() * packet.getVelocityZ()
                  );
                  this.lastExp = this.expTimer.passed(this.coolDown.getValueInt())
                          ? spd : spd - this.lastExp;
                  if (this.lastExp > 0.0) {
                     this.expTimer.reset();
                     this.speed += this.lastExp * this.multiplier.getValue();
                     this.distance += this.lastExp * this.multiplier.getValue();
                     if (MovementUtil.getMotionY() > 0.0
                             && this.vertical.getValue() != 0.0) {
                        MovementUtil.setMotionY(
                                MovementUtil.getMotionY() * this.vertical.getValue());
                     }
                  }
               }
            } else if (event.getPacket() instanceof ExplosionS2CPacket packetx
                    && this.explosions.getValue()
                    && mc.player.getPos().distanceTo(
                    new Vec3d(packetx.getX(), packetx.getY(), packetx.getZ())) < 15.0) {
               double spd = Math.sqrt(
                       packetx.getPlayerVelocityX() * packetx.getPlayerVelocityX()
                               + packetx.getPlayerVelocityZ() * packetx.getPlayerVelocityZ()
               );
               this.lastExp = this.expTimer.passed(this.coolDown.getValueInt())
                       ? spd : spd - this.lastExp;
               if (this.lastExp > 0.0) {
                  this.expTimer.reset();
                  this.speed += this.lastExp * this.multiplier.getValue();
                  this.distance += this.lastExp * this.multiplier.getValue();
                  if (MovementUtil.getMotionY() > 0.0) {
                     MovementUtil.setMotionY(
                             MovementUtil.getMotionY() * this.vertical.getValue());
                  }
               }
            }
         }

         if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.lagTimer.reset();
            this.resetStrafe();
         }
      }
   }

   @EventListener
   public void onMove(MovedEvent event) {
      if (!nullCheck()) {
         double dx = mc.player.getX() - mc.player.prevX;
         double dz = mc.player.getZ() - mc.player.prevZ;
         this.distance = Math.sqrt(dx * dx + dz * dz);
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.mode.is(Mode.Strafe) && this.windChargeBoost.getValue() && MovementUtil.isMoving()) {
         boolean canThrow = this.firstLaunch;
         if (!canThrow) {
            canThrow = this.windChargeTimer.passed(this.boostDelay.getValueInt());
         }

         if (canThrow) {
            this.handleWindChargeThrow();
            this.firstLaunch = false;
         }
      }

      if (this.mode.is(Mode.Grim)) {
         if (!MovementUtil.isMoving()) return;
         int collisions = 0;
         Box box = this.strict.getValue()
                 ? mc.player.getBoundingBox()
                 : mc.player.getBoundingBox().expand(1.0);

         for (Entity entity : Forever.THREAD.getEntities()) {
            Box entityBox = entity.getBoundingBox();
            if (this.boat.getValue()
                    && mc.player.isOnGround()
                    && entity instanceof BoatEntity
                    && box.intersects(entityBox.expand(this.boatExpand.getValue()))) {
               double yaw = Math.toRadians(Sprint.getSprintYaw(mc.player.getYaw()));
               double b = this.boatSpeed.getValue();
               mc.player.setVelocity(-Math.sin(yaw) * b, this.boatJump.getValue(),
                       Math.cos(yaw) * b);
               return;
            }
            if (box.intersects(entityBox) && this.canCauseSpeed(entity)) {
               collisions++;
            }
         }

         double yaw = Math.toRadians(Sprint.getSprintYaw(mc.player.getYaw()));
         double b = this.collideSpeed.getValue() * collisions;
         mc.player.addVelocity(-Math.sin(yaw) * b, 0.0, Math.cos(yaw) * b);
         return;
      }

      if (this.mode.is(Mode.Elytra)) {
         handleElytraUpdate();
      }
   }

   private void handleWindChargeThrow() {
      if (this.mc.world == null || this.mc.player == null) return;

      int windChargeSlot = -1;
      boolean fromInventory = false;
      if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
         windChargeSlot = mc.player.getInventory().selectedSlot;
      } else if ((windChargeSlot = InventoryUtil.findItem(Items.WIND_CHARGE)) != -1) {
      } else if ((windChargeSlot = InventoryUtil.findItemInventorySlotFromZero(Items.WIND_CHARGE)) != -1) {
         fromInventory = true;
      }

      if (windChargeSlot == -1) return;
      float yaw = mc.player.getYaw();
      float pitch = 90.0F;
      Forever.ROTATION.snapAt(yaw, pitch);

      if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
      } else if (fromInventory) {
         InventoryUtil.inventorySwap(windChargeSlot, mc.player.getInventory().selectedSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
         InventoryUtil.inventorySwap(windChargeSlot, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
      } else {
         int oldSlot = mc.player.getInventory().selectedSlot;
         InventoryUtil.switchToSlot(windChargeSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
         InventoryUtil.switchToSlot(oldSlot);
      }
      Forever.ROTATION.snapBack();
      this.windChargeTimer.reset();
      this.boostStartTime = System.currentTimeMillis();
   }

   private void handleElytraUpdate() {
      if (mc.player == null || mc.world == null) return;
      elytraHasElytra = false;
      for (ItemStack is : mc.player.getArmorItems()) {
         if (is.getItem() instanceof ElytraItem) {
            elytraHasElytra = true;
            break;
         }
      }
      if (!elytraHasElytra) {
         elytraHasElytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA) != -1;
      }
      if (!elytraHasElytra) return;
      if (mc.player.isOnGround()) return;
      if (!mc.player.isFallFlying() && mc.player.getVelocity().getY() < 0.0) {
         elytraEquipAndStartFly();
      }
      if (mc.player.isFallFlying() && MovementUtil.isMoving()) {
         handleElytraFirework();
      }
   }

   private void elytraEquipAndStartFly() {
      if (mc.player == null) return;
      ItemStack chestItem = mc.player.getInventory().getArmorStack(2); // 2 = 胸甲
      if (chestItem.getItem() instanceof ElytraItem) {
         sendElytraStartPacket();
         return;
      }

      int elytraHotbar = InventoryUtil.findItem(Items.ELYTRA);
      if (elytraHotbar != -1) {
         mc.interactionManager.clickSlot(
                 mc.player.currentScreenHandler.syncId,
                 6, elytraHotbar,
                 SlotActionType.SWAP,
                 mc.player
         );
         sendElytraStartPacket();
         mc.interactionManager.clickSlot(
                 mc.player.currentScreenHandler.syncId,
                 6, elytraHotbar,
                 SlotActionType.SWAP,
                 mc.player
         );
      } else {
         int elytraInventory = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
         if (elytraInventory == -1) return;
         mc.interactionManager.clickSlot(
                 mc.player.currentScreenHandler.syncId,
                 elytraInventory, 0,
                 SlotActionType.PICKUP,
                 mc.player
         );
         mc.interactionManager.clickSlot(
                 mc.player.currentScreenHandler.syncId,
                 6, 0,
                 SlotActionType.PICKUP,
                 mc.player
         );
         sendElytraStartPacket();
         mc.interactionManager.clickSlot(
                 mc.player.currentScreenHandler.syncId,
                 6, 0,
                 SlotActionType.PICKUP,
                 mc.player
         );
         mc.interactionManager.clickSlot(
                 mc.player.currentScreenHandler.syncId,
                 elytraInventory, 0,
                 SlotActionType.PICKUP,
                 mc.player
         );
      }
   }

   private void sendElytraStartPacket() {
      mc.getNetworkHandler().sendPacket(
              new ClientCommandC2SPacket(
                      mc.player,
                      ClientCommandC2SPacket.Mode.START_FALL_FLYING
              )
      );
      mc.player.startFallFlying();
   }

   @EventListener
   public void onElytraTravel(TravelEvent event) {
      if (nullCheck()) return;
      if (!this.mode.is(Mode.Elytra)) return;
      if (!elytraHasElytra) return;
      if (!mc.player.isFallFlying()) return;
      if (event.isPost()) return;
      if (!mc.player.isOnGround() && mc.player.getVelocity().y < -0.05) {
         setMotionY(0.0);
      }
      if (mc.options.sneakKey.isPressed() && mc.player.input.jumping) {
         setMotionY(0.0);
      } else if (mc.options.sneakKey.isPressed()) {
         setMotionY(-elytraDownSpeed.getValue());
      } else if (mc.player.input.jumping) {
         setMotionY(1.0);
      } else {
         setMotionY(-3.0E-11);
      }
      double[] dir = MovementUtil.directionSpeed(elytraSpeed.getValue());
      setMotionX(dir[0]);
      setMotionZ(dir[1]);
      setMotionY(getMotionY() * 0.99);
      setMotionX(getMotionX() * 0.98);
      setMotionZ(getMotionZ() * 0.99);
      event.cancel();
      mc.player.move(MovementType.SELF, mc.player.getVelocity());
   }

   private void handleElytraFirework() {
      // ... (保持原有 Elytra 逻辑不变) ...
      if (mc.player.isUsingItem()) return;
      if (!elytraFireworkTimer.passed((long) elytraFireworkDelay.getValue())) return;
      for (Entity entity : Forever.THREAD.getEntities()) {
         if (entity instanceof FireworkRocketEntity frw
                 && ((hoprc.asm.accessors.IFireworkRocketEntity) frw).getShooter()
                 == mc.player) {
            return;
         }
      }

      useElytraFirework();
      elytraFireworkTimer.reset();
   }

   private void useElytraFirework() {
      if (mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) {
         sendElytraFireworkPacket();
         return;
      }

      int firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET);
      if (firework != -1) {
         InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
         sendElytraFireworkPacket();
         InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
         return;
      }

      firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
      if (firework != -1) {
         int oldSlot = mc.player.getInventory().selectedSlot;
         InventoryUtil.switchToSlot(firework);
         sendElytraFireworkPacket();
         InventoryUtil.switchToSlot(oldSlot);
      }
   }

   private void sendElytraFireworkPacket() {
      sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(
              Hand.MAIN_HAND, id,
              Forever.ROTATION.getLastYaw(),
              Forever.ROTATION.getLastPitch()
      ));
   }

   public boolean isElytraNoAnimation() {
      return isOn() && this.mode.is(Mode.Elytra) && mc.player != null
              && mc.player.isFallFlying();
   }

   private boolean canCauseSpeed(Entity entity) {
      return entity != mc.player
              && entity instanceof LivingEntity
              && !(entity instanceof ArmorStandEntity);
   }

   @EventListener
   public void invoke(MoveEvent event) {
      if (this.mode.is(Mode.Elytra)) return;

      if (!MovementUtil.isMoving() && this.airStop.getValue()
              && !this.mode.is(Mode.Grim)) {
         MovementUtil.setMotionX(0.0);
         MovementUtil.setMotionZ(0.0);
      }

      if ((this.inWater.getValue()
              || !mc.player.isSubmergedInWater()
              && !mc.player.isTouchingWater()
              && !mc.player.isInLava())
              && !mc.player.isRiding()
              && !mc.player.isHoldingOntoLadder()
              && (this.inBlock.getValue() || !EntityUtil.isInsideBlock())
              && !mc.player.getAbilities().flying
              && !mc.player.isFallFlying()
              && MovementUtil.isMoving()) {

         if (!this.mode.is(Mode.Strafe)) {
            double speedEffect = 1.0, slowEffect = 1.0;
            if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
               double amp = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
               speedEffect = 1.0 + 0.2 * (amp + 1.0);
            }
            if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
               double amp = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
               slowEffect = 1.0 + 0.2 * (amp + 1.0);
            }
            double base = 0.2873F * speedEffect / slowEffect;
            float jumpEffect = 0.0F;
            if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
               jumpEffect += (mc.player.getStatusEffect(StatusEffects.JUMP_BOOST)
                       .getAmplifier() + 1) * 0.1F;
            }

            if (this.mode.getValue() == Mode.StrafeStrict) {
               if (!this.lagTimer.passed(this.lagTime.getValueInt())) return;

               if (this.strafe == 1) {
                  this.speed = 1.35F * base - 0.01F;
               } else if (this.strafe == 2) {
                  if (mc.player.input.jumping || !mc.player.isOnGround()) return;
                  float j = 0.39999995F + jumpEffect;
                  event.setY(j);
                  MovementUtil.setMotionY(j);
                  this.speed *= 2.149;
               } else if (this.strafe == 3) {
                  this.speed = this.distance - 0.66 * (this.distance - base);
               } else {
                  if ((!mc.world.isSpaceEmpty(mc.player,
                          mc.player.getBoundingBox().offset(
                                  0.0, mc.player.getVelocity().getY(), 0.0))
                          || mc.player.verticalCollision) && this.strafe > 0) {
                     this.strafe = 1;
                  }
                  this.speed = this.distance - this.distance / 159.0;
               }

               this.strictTicks++;
               this.speed = Math.max(this.speed, base);
               double baseMax = 0.465 * speedEffect / slowEffect;
               double baseMin = 0.44 * speedEffect / slowEffect;
               this.speed = Math.min(this.speed,
                       this.strictTicks > 25 ? baseMax : baseMin);
               if (this.strictTicks > 50) this.strictTicks = 0;

               Vec2f motion = this.handleStrafeMotion((float) this.speed);
               event.setX(motion.x);
               event.setZ(motion.y);
               this.strafe++;
            }

         } else if (this.stop) {
            this.stop = false;
         } else if (this.lagTimer.passed(this.lagTime.getValueInt())) {
            if (this.stage == 1) {
               this.speed = 1.35
                       * MovementUtil.getSpeed(this.slow.getValue(),
                       this.strafeSpeed.getValue())
                       - 0.01;
            } else if (this.stage != 2
                    || !mc.player.isOnGround()
                    || !mc.options.jumpKey.isPressed() && !this.jump.getValue()) {
               if (this.stage == 3) {
                  this.speed = this.distance
                          - 0.66 * (this.distance
                          - MovementUtil.getSpeed(this.slow.getValue(),
                          this.strafeSpeed.getValue()));
                  this.boost = !this.boost;
               } else {
                  if ((BlockUtil.canCollide(null,
                          mc.player.getBoundingBox()
                                  .offset(0.0, MovementUtil.getMotionY(), 0.0))
                          || mc.player.collidedSoftly) && this.stage > 0) {
                     this.stage = 1;
                  }
                  this.speed = this.distance - this.distance / 159.0;
               }
            } else {
               double yMotion = 0.3999 + MovementUtil.getJumpSpeed();
               MovementUtil.setMotionY(yMotion);
               event.setY(yMotion);
               this.speed = this.speed * (this.boost ? 1.6835 : 1.395);
            }

            this.speed = Math.min(this.speed, 10.0);
            this.speed = Math.max(this.speed,
                    MovementUtil.getSpeed(this.slow.getValue(),
                            this.strafeSpeed.getValue()));
            if (this.windChargeBoost.getValue() && this.boostStartTime != 0L) {
               long now = System.currentTimeMillis();
               long waitTime = (long) this.timeUntilBoost.getValue();
               long totalTime = waitTime + (long) this.boostTime.getValue();
               long elapsed = now - this.boostStartTime;

               if (elapsed >= waitTime && elapsed < totalTime) {
                  this.speed = this.boostSpeed.getValue();
               } else if (elapsed >= totalTime) {
                  this.boostStartTime = 0L;
               }
            }

            double n = mc.player.input.movementForward;
            double n2 = mc.player.input.movementSideways;
            double n3 = mc.player.getYaw();

            if (n == 0.0 && n2 == 0.0) {
               event.setX(0.0);
               event.setZ(0.0);
            } else if (n != 0.0 && n2 != 0.0) {
               n *= Math.sin(Math.PI / 4);
               n2 *= Math.cos(Math.PI / 4);
            }

            event.setX((n * this.speed * -Math.sin(Math.toRadians(n3))
                    + n2 * this.speed * Math.cos(Math.toRadians(n3))) * 0.99);
            event.setZ((n * this.speed * Math.cos(Math.toRadians(n3))
                    - n2 * this.speed * -Math.sin(Math.toRadians(n3))) * 0.99);
            this.stage++;
         }
      } else {
         this.resetStrafe();
         this.stop = true;
      }
   }

   public Vec2f handleStrafeMotion(float spd) {
      float forward = mc.player.input.movementForward;
      float strafeSide = mc.player.input.movementSideways;
      float yaw = mc.player.prevYaw
              + (mc.player.getYaw() - mc.player.prevYaw)
              * mc.getRenderTickCounter().getTickDelta(true);

      if (forward == 0.0F && strafeSide == 0.0F) return Vec2f.ZERO;

      if (forward != 0.0F) {
         if (strafeSide >= 1.0F) {
            yaw += forward > 0.0F ? -45.0F : 45.0F;
            strafeSide = 0.0F;
         } else if (strafeSide <= -1.0F) {
            yaw += forward > 0.0F ? 45.0F : -45.0F;
            strafeSide = 0.0F;
         }
         forward = forward > 0.0F ? 1.0F : -1.0F;
      }

      float rx = (float) Math.cos(Math.toRadians(yaw));
      float rz = (float) (-Math.sin(Math.toRadians(yaw)));
      return new Vec2f(
              forward * spd * rz + strafeSide * spd * rx,
              forward * spd * rx - strafeSide * spd * rz
      );
   }

   public void resetStrafe() {
      this.strafe = 4;
      this.strictTicks = 0;
      this.speed = 0.0;
      this.distance = 0.0;
   }

   private double getMotionX() { return MovementUtil.getMotionX(); }
   private void setMotionX(double v) { MovementUtil.setMotionX(v); }
   private double getMotionY() { return MovementUtil.getMotionY(); }
   private void setMotionY(double v) { MovementUtil.setMotionY(v); }
   private double getMotionZ() { return MovementUtil.getMotionZ(); }
   private void setMotionZ(double v) { MovementUtil.setMotionZ(v); }

   public enum Mode {
      Strafe,
      StrafeStrict,
      Grim,
      Elytra
   }
}