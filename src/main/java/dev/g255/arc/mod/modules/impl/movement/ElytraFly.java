package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.MoveEvent;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.TravelEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.events.impl.UpdateRotateEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.player.MovementUtil;
import dev.g255.arc.asm.accessors.IFireworkRocketEntity;
import dev.g255.arc.asm.accessors.ILivingEntity;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.settings.impl.BindSetting;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class ElytraFly extends Module {
   public static ElytraFly INSTANCE;
   public final EnumSetting<Mode> mode;
   public final BooleanSetting infiniteDura;
   public final BooleanSetting packet;
   private final SliderSetting packetDelay;
   private final BooleanSetting setFlag;
   private final BooleanSetting firework;
   public final BindSetting fireWork;
   public final BooleanSetting packetInteract;
   public final BooleanSetting inventory;
   public final BooleanSetting onlyOne;
   private final BooleanSetting usingPause;
   public final BooleanSetting autoJump;
   public final SliderSetting upPitch;
   public final SliderSetting upFactor;
   public final SliderSetting downFactor;
   public final SliderSetting speed;
   public final BooleanSetting speedLimit;
   public final SliderSetting maxSpeed;
   public final BooleanSetting noDrag;
   public final Timer fireworkTimer;
   private final BooleanSetting autoStop;
   private final BooleanSetting sprint;
   private final SliderSetting pitch;
   private final BooleanSetting instantFly;
   private final BooleanSetting checkSpeed;
   public final SliderSetting minSpeed;
   private final SliderSetting delay;
   private final SliderSetting timeout;
   private final SliderSetting sneakDownSpeed;
   private final SliderSetting boost;
   private final BooleanSetting freeze;
   private final BooleanSetting motionStop;
   private final SliderSetting infiniteMaxSpeed;
   private final SliderSetting infiniteMinSpeed;
   private final SliderSetting infiniteMaxHeight;
   public final BooleanSetting releaseSneak;
   private final Timer instantFlyTimer;
   boolean prev;
   float prePitch;
   private boolean hasElytra;
   float yaw;
   float rotationPitch;
   boolean flying;
   int packetDelayInt;
   private boolean down;
   private float lastInfinitePitch;
   private float infinitePitch;

   public ElytraFly() {
      super("ElytraFly", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", ElytraFly.Mode.Control));
      this.infiniteDura = this.add(new BooleanSetting("InfiniteDura", false));
      this.packet = this.add((new BooleanSetting("Packet", false)).setParent());
      BooleanSetting var10009 = this.packet;
      Objects.requireNonNull(var10009);
      this.packetDelay = this.add(new SliderSetting("PacketDelay", (double)0.0F, (double)0.0F, (double)20.0F, (double)1.0F, var10009::isOpen));
      this.setFlag = this.add(new BooleanSetting("SetFlag", false, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.firework = this.add((new BooleanSetting("Firework", false)).setParent());
      BooleanSetting var10006 = this.firework;
      Objects.requireNonNull(var10006);
      this.fireWork = this.add(new BindSetting("FireWorkBind", -1, var10006::isOpen));
      var10006 = this.firework;
      Objects.requireNonNull(var10006);
      this.packetInteract = this.add(new BooleanSetting("PacketInteract", true, var10006::isOpen));
      var10006 = this.firework;
      Objects.requireNonNull(var10006);
      this.inventory = this.add(new BooleanSetting("InventorySwap", true, var10006::isOpen));
      var10006 = this.firework;
      Objects.requireNonNull(var10006);
      this.onlyOne = this.add(new BooleanSetting("OnlyOne", true, var10006::isOpen));
      var10006 = this.firework;
      Objects.requireNonNull(var10006);
      this.usingPause = this.add(new BooleanSetting("UsingPause", true, var10006::isOpen));
      this.autoJump = this.add(new BooleanSetting("AutoJump", true, () -> this.mode.is(ElytraFly.Mode.Bounce)));
      this.upPitch = this.add(new SliderSetting("UpPitch", (double)0.0F, (double)0.0F, (double)90.0F, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.upFactor = this.add(new SliderSetting("UpFactor", (double)1.0F, (double)0.0F, (double)10.0F, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.downFactor = this.add(new SliderSetting("FallSpeed", (double)1.0F, (double)0.0F, (double)10.0F, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.speed = this.add(new SliderSetting("Speed", (double)1.0F, (double)0.1F, (double)10.0F, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.speedLimit = this.add(new BooleanSetting("SpeedLimit", true, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.maxSpeed = this.add(new SliderSetting("MaxSpeed", (double)2.5F, (double)0.1F, (double)10.0F, () -> this.speedLimit.getValue() && this.mode.getValue() == ElytraFly.Mode.Control));
      this.noDrag = this.add(new BooleanSetting("NoDrag", false, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.fireworkTimer = new Timer();
      this.autoStop = this.add(new BooleanSetting("AutoStop", true));
      this.sprint = this.add(new BooleanSetting("Sprint", true, () -> this.mode.is(ElytraFly.Mode.Bounce)));
      this.pitch = this.add(new SliderSetting("Pitch", (double)88.0F, (double)-90.0F, (double)90.0F, 0.1, () -> this.mode.is(ElytraFly.Mode.Bounce)));
      this.instantFly = this.add(new BooleanSetting("AutoStart", true, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.checkSpeed = this.add(new BooleanSetting("CheckSpeed", false, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.minSpeed = this.add(new SliderSetting("MinSpeed", (double)70.0F, 0.1, (double)200.0F, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.delay = this.add(new SliderSetting("Delay", (double)1000.0F, (double)0.0F, (double)20000.0F, (double)50.0F, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.timeout = this.add(new SliderSetting("Timeout", (double)0.0F, 0.1, (double)1.0F, 0.1, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.sneakDownSpeed = this.add(new SliderSetting("DownSpeed", (double)1.0F, 0.1, (double)10.0F, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.boost = this.add(new SliderSetting("Boost", (double)1.0F, 0.1, (double)4.0F, () -> this.mode.getValue() == ElytraFly.Mode.Boost));
      this.freeze = this.add(new BooleanSetting("Freeze", false, () -> this.mode.is(ElytraFly.Mode.Rotation)));
      this.motionStop = this.add(new BooleanSetting("MotionStop", false, () -> this.mode.is(ElytraFly.Mode.Rotation)));
      this.infiniteMaxSpeed = this.add(new SliderSetting("InfiniteMaxSpeed", (double)150.0F, (double)50.0F, (double)170.0F, () -> this.mode.getValue() == ElytraFly.Mode.Pitch));
      this.infiniteMinSpeed = this.add(new SliderSetting("InfiniteMinSpeed", (double)25.0F, (double)10.0F, (double)70.0F, () -> this.mode.getValue() == ElytraFly.Mode.Pitch));
      this.infiniteMaxHeight = this.add(new SliderSetting("InfiniteMaxHeight", 200, -50, 360, () -> this.mode.getValue() == ElytraFly.Mode.Pitch));
      this.releaseSneak = this.add(new BooleanSetting("ReleaseSneak", false));
      this.instantFlyTimer = new Timer();
      this.hasElytra = false;
      this.yaw = 0.0F;
      this.rotationPitch = 0.0F;
      this.flying = false;
      this.packetDelayInt = 0;
      this.setChinese("鞘翅飞行");
      INSTANCE = this;
      Arc.EVENT_BUS.subscribe(new FireWorkTweak());
   }

   public void off() {
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (this.onlyOne.getValue()) {
            for(Entity entity : Arc.THREAD.getEntities()) {
               FireworkRocketEntity fireworkRocketEntity;
               if (entity instanceof FireworkRocketEntity && ((IFireworkRocketEntity)(fireworkRocketEntity = (FireworkRocketEntity)entity)).getShooter() == mc.player) {
                  return;
               }
            }
         }

         INSTANCE.fireworkTimer.reset();
         if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            if (this.packetInteract.getValue()) {
               sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Arc.ROTATION.getLastYaw(), Arc.ROTATION.getLastPitch()));
            } else {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
         } else {
            int firework;
            if (this.inventory.getValue() && (firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
               InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
               if (this.packetInteract.getValue()) {
                  sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Arc.ROTATION.getLastYaw(), Arc.ROTATION.getLastPitch()));
               } else {
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               }

               InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
               EntityUtil.syncInventory();
            } else {
               firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
               if (firework != -1) {
                  int old = mc.player.getInventory().selectedSlot;
                  InventoryUtil.switchToSlot(firework);
                  if (this.packetInteract.getValue()) {
                     sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Arc.ROTATION.getLastYaw(), Arc.ROTATION.getLastPitch()));
                  } else {
                     mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                  }

                  InventoryUtil.switchToSlot(old);
               }
            }
         }

      }
   }

   public static boolean recastElytra(ClientPlayerEntity player) {
      if (checkConditions(player) && ignoreGround(player)) {
         player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
         if (INSTANCE.setFlag.getValue()) {
            mc.player.startFallFlying();
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean checkConditions(ClientPlayerEntity player) {
      ItemStack itemStack = player.getEquippedStack(EquipmentSlot.CHEST);
      return !player.getAbilities().flying && !player.hasVehicle() && !player.isClimbing() && itemStack.isOf(Items.ELYTRA) && ElytraItem.isUsable(itemStack);
   }

   private static boolean ignoreGround(ClientPlayerEntity player) {
      if (!player.isTouchingWater() && !player.hasStatusEffect(StatusEffects.LEVITATION)) {
         ItemStack itemStack = player.getEquippedStack(EquipmentSlot.CHEST);
         if (itemStack.isOf(Items.ELYTRA) && ElytraItem.isUsable(itemStack)) {
            player.startFallFlying();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public String getInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   public void onEnable() {
      if (!nullCheck()) {
         this.hasElytra = false;
         this.yaw = mc.player.getYaw();
         this.rotationPitch = mc.player.getPitch();
      }
   }

   public void onDisable() {
      if (!nullCheck()) {
         if (this.releaseSneak.getValue()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
         }

      }
   }

   private void boost() {
      if (this.hasElytra) {
         if (!this.isFallFlying()) {
            return;
         }

         float yaw = (float)Math.toRadians((double)mc.player.getYaw());
         if (mc.options.forwardKey.isPressed()) {
            mc.player.addVelocity((double)(-MathHelper.sin(yaw) * this.boost.getValueFloat() / 10.0F), (double)0.0F, (double)(MathHelper.cos(yaw) * this.boost.getValueFloat() / 10.0F));
         }
      }

   }

   @EventListener(
      priority = -9999
   )
   public void onRotation(UpdateRotateEvent event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Rotation)) {
            if (this.isFallFlying()) {
               if (MovementUtil.isMoving()) {
                  if (mc.options.jumpKey.isPressed()) {
                     this.rotationPitch = -45.0F;
                  } else if (mc.options.sneakKey.isPressed()) {
                     this.rotationPitch = 45.0F;
                  } else {
                     this.rotationPitch = -1.9F;
                     if (this.motionStop.getValue()) {
                        this.setY((double)0.0F);
                     }
                  }
               } else if (mc.options.jumpKey.isPressed()) {
                  this.rotationPitch = -89.0F;
               } else if (mc.options.sneakKey.isPressed()) {
                  this.rotationPitch = 89.0F;
               } else if (this.motionStop.getValue()) {
                  this.setY((double)0.0F);
               }

               if (!mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed() && !mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
                  if (this.motionStop.getValue()) {
                     this.setX((double)0.0F);
                     this.setZ((double)0.0F);
                  }
               } else {
                  this.yaw = Sprint.getSprintYaw(mc.player.getYaw());
               }

               event.setYaw(this.yaw);
               event.setPitch(this.rotationPitch);
            }
         } else if (this.mode.is(ElytraFly.Mode.Pitch)) {
            if (this.isFallFlying()) {
               event.setPitch(this.infinitePitch);
            }
         } else if (this.mode.is(ElytraFly.Mode.Bounce) && this.isFallFlying()) {
            event.setPitch(this.pitch.getValueFloat());
         }

      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.getInfinitePitch();
      this.flying = false;
      if (this.packet.getValue()) {
         this.hasElytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA) != -1;
      } else {
         this.hasElytra = false;

         for(ItemStack is : mc.player.getArmorItems()) {
            if (is.getItem() instanceof ElytraItem) {
               this.hasElytra = true;
               break;
            }
         }

         if (this.infiniteDura.getValue() && !mc.player.isOnGround() && this.hasElytra) {
            this.flying = true;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            if (this.setFlag.getValue()) {
               mc.player.startFallFlying();
            }
         }

         if (this.mode.is(ElytraFly.Mode.Bounce)) {
            ((ILivingEntity)mc.player).setLastJumpCooldown(0);
            return;
         }
      }

      double x = mc.player.getX() - mc.player.prevX;
      double y = mc.player.getY() - mc.player.prevY;
      double z = mc.player.getZ() - mc.player.prevZ;
      double dist = Math.sqrt(x * x + z * z + y * y) / (double)1000.0F;
      double div = 1.388888888888889E-5;
      float timer = Arc.TIMER.get();
      double speed = dist / div * (double)timer;
      if (this.mode.getValue() == ElytraFly.Mode.Boost) {
         this.boost();
      }

      if (this.packet.getValue()) {
         if (!mc.player.isOnGround()) {
            ++this.packetDelayInt;
            if (!((double)this.packetDelayInt <= this.packetDelay.getValue())) {
               int elytra = InventoryUtil.findItem(Items.ELYTRA);
               if (elytra != -1) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, elytra, SlotActionType.SWAP, mc.player);
                  mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                  mc.player.startFallFlying();
                  if ((!this.checkSpeed.getValue() || speed <= this.minSpeed.getValue()) && this.firework.getValue() && this.fireworkTimer.passed((long)this.delay.getValueInt()) && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.jumpKey.isPressed()) && (!mc.player.isUsingItem() || !this.usingPause.getValue()) && this.isFallFlying()) {
                     this.off();
                     this.fireworkTimer.reset();
                  }

                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, elytra, SlotActionType.SWAP, mc.player);
                  this.packetDelayInt = 0;
               } else {
                  elytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
                  if (elytra != -1) {
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                     mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                     mc.player.startFallFlying();
                     if ((!this.checkSpeed.getValue() || speed <= this.minSpeed.getValue()) && this.firework.getValue() && this.fireworkTimer.passed((long)this.delay.getValueInt()) && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.jumpKey.isPressed()) && (!mc.player.isUsingItem() || !this.usingPause.getValue()) && this.isFallFlying()) {
                        this.off();
                        this.fireworkTimer.reset();
                     }

                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                     this.packetDelayInt = 0;
                  }
               }

            }
         }
      } else {
         if ((!this.checkSpeed.getValue() || speed <= this.minSpeed.getValue()) && this.firework.getValue() && this.fireworkTimer.passed((long)this.delay.getValueInt()) && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.jumpKey.isPressed()) && (!mc.player.isUsingItem() || !this.usingPause.getValue()) && this.isFallFlying()) {
            this.off();
            this.fireworkTimer.reset();
         }

         if (!this.isFallFlying() && this.hasElytra) {
            this.fireworkTimer.setMs(99999999L);
            if (!mc.player.isOnGround() && this.instantFly.getValue() && mc.player.getVelocity().getY() < (double)0.0F && !this.infiniteDura.getValue()) {
               if (!this.instantFlyTimer.passed((long)((double)1000.0F * this.timeout.getValue()))) {
                  return;
               }

               this.instantFlyTimer.reset();
               mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               if (this.setFlag.getValue()) {
                  mc.player.startFallFlying();
               }
            }
         }

      }
   }

   protected final Vec3d getRotationVector(float pitch, float yaw) {
      float f = pitch * ((float)Math.PI / 180F);
      float g = -yaw * ((float)Math.PI / 180F);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
   }

   public Vec3d getRotationVec(float tickDelta) {
      return this.getRotationVector(-this.upPitch.getValueFloat(), mc.player.getYaw(tickDelta));
   }

   @EventListener
   private void onPlayerMove(MoveEvent event) {
      if (this.autoStop.getValue() && this.isFallFlying()) {
         int chunkX = (int)(mc.player.getX() / (double)16.0F);
         int chunkZ = (int)(mc.player.getZ() / (double)16.0F);
         if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
            event.cancel();
         }
      }

   }

   @EventListener
   private void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra) {
            if (this.autoJump.getValue()) {
               mc.options.jumpKey.setPressed(true);
            }

            if (event.isPost()) {
               if (!this.isFallFlying()) {
                  mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               }

               if (checkConditions(mc.player) && !this.sprint.getValue()) {
                  if (this.isFallFlying()) {
                     mc.player.setSprinting(mc.player.isOnGround());
                  } else {
                     mc.player.setSprinting(true);
                  }
               }
            } else if (checkConditions(mc.player) && this.sprint.getValue()) {
               mc.player.setSprinting(true);
            }
         }

      }
   }

   @EventListener
   private void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra && event.getPacket() instanceof ClientCommandC2SPacket && ((ClientCommandC2SPacket)event.getPacket()).getMode().equals(ClientCommandC2SPacket.Mode.START_FALL_FLYING) && !this.sprint.getValue()) {
            mc.player.setSprinting(true);
         }

      }
   }

   @EventListener
   private void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            mc.player.stopFallFlying();
         }

      }
   }

   @EventListener
   public void travel(TravelEvent event) {
      if (!nullCheck()) {
         if (!AntiCheat.INSTANCE.movementSync()) {
            if (this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra) {
               if (event.isPre()) {
                  this.prev = true;
                  this.prePitch = mc.player.getPitch();
                  mc.player.setPitch(this.pitch.getValueFloat());
               } else if (this.prev) {
                  this.prev = false;
                  mc.player.setPitch(this.prePitch);
               }
            } else if (this.mode.is(ElytraFly.Mode.Pitch) && this.isFallFlying()) {
               if (event.isPre()) {
                  this.prev = true;
                  this.prePitch = mc.player.getPitch();
                  mc.player.setPitch(this.lastInfinitePitch);
               } else if (this.prev) {
                  this.prev = false;
                  mc.player.setPitch(this.prePitch);
               }
            }
         }

      }
   }

   @EventListener
   public void onMove(TravelEvent event) {
      if (!nullCheck() && this.hasElytra && this.isFallFlying() && !event.isPost()) {
         if ((this.mode.is(ElytraFly.Mode.Freeze) || this.mode.is(ElytraFly.Mode.Rotation) && this.freeze.getValue()) && !MovementUtil.isMoving() && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) {
            event.cancel();
         } else {
            if (this.mode.getValue() == ElytraFly.Mode.Control) {
               if (this.firework.getValue()) {
                  if (mc.options.sneakKey.isPressed() && mc.player.input.jumping) {
                     this.setY((double)0.0F);
                  } else if (mc.options.sneakKey.isPressed()) {
                     this.setY(-this.sneakDownSpeed.getValue());
                  } else if (mc.player.input.jumping) {
                     this.setY(this.upFactor.getValue());
                  } else {
                     this.setY(-3.0E-11 * this.downFactor.getValue());
                  }

                  double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
                  this.setX(dir[0]);
                  this.setZ(dir[1]);
               } else {
                  Vec3d lookVec = this.getRotationVec(mc.getRenderTickCounter().getTickDelta(true));
                  double lookDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
                  double motionDist = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
                  if (mc.player.input.sneaking) {
                     this.setY(-this.sneakDownSpeed.getValue());
                  } else if (!mc.player.input.jumping) {
                     this.setY(-3.0E-11 * this.downFactor.getValue());
                  }

                  if (mc.player.input.jumping) {
                     if (motionDist > this.upFactor.getValue() / this.upFactor.getMax()) {
                        double rawUpSpeed = motionDist * 0.01325;
                        this.setY(this.getY() + rawUpSpeed * 3.2);
                        this.setX(this.getX() - lookVec.x * rawUpSpeed / lookDist);
                        this.setZ(this.getZ() - lookVec.z * rawUpSpeed / lookDist);
                     } else {
                        double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
                        this.setX(dir[0]);
                        this.setZ(dir[1]);
                     }
                  }

                  if (lookDist > (double)0.0F) {
                     this.setX(this.getX() + (lookVec.x / lookDist * motionDist - this.getX()) * 0.1);
                     this.setZ(this.getZ() + (lookVec.z / lookDist * motionDist - this.getZ()) * 0.1);
                  }

                  if (!mc.player.input.jumping) {
                     double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
                     this.setX(dir[0]);
                     this.setZ(dir[1]);
                  }

                  if (!this.noDrag.getValue()) {
                     this.setY(this.getY() * (double)0.99F);
                     this.setX(this.getX() * (double)0.98F);
                     this.setZ(this.getZ() * (double)0.99F);
                  }

                  double finalDist = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
                  if (this.speedLimit.getValue() && finalDist > this.maxSpeed.getValue()) {
                     this.setX(this.getX() * this.maxSpeed.getValue() / finalDist);
                     this.setZ(this.getZ() * this.maxSpeed.getValue() / finalDist);
                  }

                  event.cancel();
                  mc.player.move(MovementType.SELF, mc.player.getVelocity());
               }
            }

         }
      }
   }

   private double getX() {
      return MovementUtil.getMotionX();
   }

   private void setX(double f) {
      MovementUtil.setMotionX(f);
   }

   private double getY() {
      return MovementUtil.getMotionY();
   }

   private void setY(double f) {
      MovementUtil.setMotionY(f);
   }

   private double getZ() {
      return MovementUtil.getMotionZ();
   }

   private void setZ(double f) {
      MovementUtil.setMotionZ(f);
   }

   private void getInfinitePitch() {
      this.lastInfinitePitch = this.infinitePitch;
      double currentPlayerSpeed = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ);
      if (mc.player.getY() < this.infiniteMaxHeight.getValue()) {
         if (currentPlayerSpeed * (double)72.0F < this.infiniteMinSpeed.getValue() && !this.down) {
            this.down = true;
         }

         if (currentPlayerSpeed * (double)72.0F > this.infiniteMaxSpeed.getValue() && this.down) {
            this.down = false;
         }
      } else {
         this.down = true;
      }

      this.infinitePitch = this.down ? (this.infinitePitch += 3.0F) : (this.infinitePitch -= 3.0F);
      this.infinitePitch = MathUtil.clamp(this.infinitePitch, -40.0F, 40.0F);
   }

   public boolean isFallFlying() {
      return mc.player.isFallFlying() || this.packet.getValue() && this.hasElytra && !mc.player.isOnGround() || this.flying;
   }

   public static enum Mode {
      Control,
      Boost,
      Bounce,
      Freeze,
      None,
      Rotation,
      Pitch;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Control, Boost, Bounce, Freeze, None, Rotation, Pitch};
      }
   }

   public class FireWorkTweak {
      boolean press;

      @EventListener
      public void onTick(ClientTickEvent event) {
         if (!Module.nullCheck() && !event.isPost()) {
            if (!ElytraFly.this.inventory.getValue() || EntityUtil.inInventory()) {
               if (Wrapper.mc.currentScreen == null) {
                  if (ElytraFly.this.fireWork.isPressed()) {
                     if (!this.press && ElytraFly.this.fireworkTimer.passed((long)ElytraFly.this.delay.getValueInt()) && (!Wrapper.mc.player.isUsingItem() || !ElytraFly.this.usingPause.getValue()) && ElytraFly.this.isFallFlying()) {
                        ElytraFly.this.off();
                        ElytraFly.this.fireworkTimer.reset();
                     }

                     this.press = true;
                  } else {
                     this.press = false;
                  }
               } else {
                  this.press = false;
               }

            }
         }
      }
   }
}
