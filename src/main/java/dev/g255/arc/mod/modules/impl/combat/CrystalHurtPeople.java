package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.EntitySpawnedEvent;
import dev.g255.arc.api.events.impl.Render3DEvent;
import dev.g255.arc.api.events.impl.RotationEvent;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.entity.PlayerEntityPredict;
import dev.g255.arc.api.utils.math.AnimateUtil;
import dev.g255.arc.api.utils.math.ExplosionUtil;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render3DUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.settings.enums.SwingSide;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity.RemovalReason;

public class CrystalHurtPeople extends Module {
   public static CrystalHurtPeople INSTANCE;
   private final SliderSetting cps = this.add((new SliderSetting("CPS", 16, 1, 32)).setSuffix("cps"));
   private final SliderSetting placeRange = this.add((new SliderSetting("PlaceRange", (double)5.0F, (double)0.0F, (double)6.0F, 0.01)).setSuffix("m"));
   private final SliderSetting breakRange = this.add((new SliderSetting("BreakRange", (double)4.0F, (double)0.0F, (double)6.0F, 0.01)).setSuffix("m"));
   private final SliderSetting minDamage = this.add((new SliderSetting("MinDamage", (double)4.0F, (double)0.0F, (double)36.0F, 0.1)).setSuffix("dmg"));
   private final SliderSetting maxSelfDamage = this.add((new SliderSetting("MaxSelfDmg", (double)10.0F, (double)0.0F, (double)36.0F, 0.1)).setSuffix("dmg"));
   private final EnumSetting<SwapMode> autoSwap;
   private final BooleanSetting fastLayer;
   private final BooleanSetting render;
   private final BooleanSetting eatingPause;
   private final EnumSetting<Page> page;
   private final SliderSetting reserve;
   private final SliderSetting targetRange;
   private final SliderSetting wallRange;
   private final BooleanSetting strongLoop;
   private final BooleanSetting instantBreak;
   private final SliderSetting attackFactor;
   private final SliderSetting placeRate;
   private final BooleanSetting onlyBestDamage;
   private final BooleanSetting silentSwap;
   private final BooleanSetting remove;
   private final SliderSetting updateDelay;
   private final BooleanSetting rotations;
   private final BooleanSetting predict;
   private final SliderSetting predictTicks;
   private final SliderSetting maxMotionY;
   private final BooleanSetting usingPause;
   private final BooleanSetting guiPause;
   private final BooleanSetting shrink;
   private final ColorSetting fill;
   private final ColorSetting box;
   private final SliderSetting lineWidth;
   private final SliderSetting sliderSpeed;
   private final SliderSetting startFade;
   private final SliderSetting fadeSpeed;
   private final BooleanSetting damageText;
   private final ColorSetting textColor;
   private static Constructor<?> ATTACK_CTOR;
   private static Object ATTACK_TYPE;
   private static Field ATTACK_ID_FIELD;
   private static boolean ATTACK_REFLECT_READY;
   private static Field INTERACTION_MANAGER_FIELD;
   private static Method SYNC_SELECTED_SLOT_METHOD;
   private final Timer updateTimer;
   private final Timer placeTimer;
   private final Timer breakTimer;
   private final Timer renderTimer;
   private final Timer rotationTimer;
   private int placeTickCount;
   private final DecimalFormat damageFormat;
   private Vec3d rotationTarget;
   private BlockPos bestPlacePos;
   private EndCrystalEntity bestBreakCrystal;
   private EndCrystalEntity targetCrystal;
   private PlayerEntity bestTarget;
   private float bestPlaceDamage;
   private float bestBreakDamage;
   private float targetDamage;
   private Vec3d renderTargetPos;
   private Vec3d renderPos;
   private double renderScale;

   public CrystalHurtPeople() {
      super("CrystalHurtPeople", Module.Category.Combat);
      this.autoSwap = this.add(new EnumSetting("AutoSwap", CrystalHurtPeople.SwapMode.None));
      this.fastLayer = this.add((new BooleanSetting("FastLayer", true)).setParent());
      this.render = this.add(new BooleanSetting("Render", true));
      this.eatingPause = this.add(new BooleanSetting("EatingPause", false));
      this.page = this.add(new EnumSetting("Page", CrystalHurtPeople.Page.Combat));
      this.reserve = this.add((new SliderSetting("Reserve", (double)2.0F, (double)0.0F, (double)10.0F, 0.1)).setSuffix("hp"));
      this.targetRange = this.add((new SliderSetting("TargetRange", (double)12.0F, (double)0.0F, (double)20.0F, 0.1)).setSuffix("m"));
      this.wallRange = this.add((new SliderSetting("WallRange", (double)6.0F, (double)0.0F, (double)6.0F, 0.01)).setSuffix("m"));
      this.strongLoop = this.add(new BooleanSetting("StrongLoop", true));
      this.instantBreak = this.add(new BooleanSetting("InstantBreak", true));
      this.attackFactor = this.add((new SliderSetting("AttackFactor", (double)1.0F, (double)1.0F, (double)8.0F, (double)1.0F)).setSuffix("x"));
      this.placeRate = this.add((new SliderSetting("PlaceRate", (double)1.0F, (double)1.0F, (double)10.0F, (double)1.0F)).setSuffix("t"));
      this.onlyBestDamage = this.add(new BooleanSetting("OnlyBestDmg", false));
      this.silentSwap = this.add(new BooleanSetting("SilentSwap", false));
      this.remove = this.add(new BooleanSetting("Remove", false));
      this.updateDelay = this.add((new SliderSetting("UpdateDelay", 30, 0, 200)).setSuffix("ms"));
      this.rotations = this.add(new BooleanSetting("Rotations", false));
      this.predict = this.add((new BooleanSetting("Predict", true)).setParent());
      this.predictTicks = this.add((new SliderSetting("PredictTicks", 4, 0, 20)).setSuffix("ticks"));
      this.maxMotionY = this.add((new SliderSetting("MaxMotionY", 0.34, (double)0.0F, (double)2.0F, 0.01)).setSuffix("b/t"));
      this.usingPause = this.add(new BooleanSetting("UsingPause", true));
      this.guiPause = this.add(new BooleanSetting("GuiPause", true));
      this.shrink = this.add(new BooleanSetting("Shrink", true));
      this.fill = this.add((new ColorSetting("Fill", new Color(255, 255, 255, 100))).injectBoolean(true));
      this.box = this.add((new ColorSetting("Box", new Color(255, 255, 255, 255))).injectBoolean(true));
      this.lineWidth = this.add(new SliderSetting("LineWidth", (double)1.5F, 0.01, (double)3.0F, 0.01));
      this.sliderSpeed = this.add(new SliderSetting("SliderSpeed", 0.2, 0.01, (double)1.0F, 0.01));
      this.startFade = this.add((new SliderSetting("StartFade", 0.3, (double)0.0F, (double)2.0F, 0.01)).setSuffix("s"));
      this.fadeSpeed = this.add(new SliderSetting("FadeSpeed", 0.2, 0.01, (double)1.0F, 0.01));
      this.damageText = this.add(new BooleanSetting("DamageText", true));
      this.textColor = this.add(new ColorSetting("TextColor", new Color(255, 255, 255, 255)));
      this.page.setHidden();
      this.reserve.setHidden();
      this.targetRange.setHidden();
      this.wallRange.setHidden();
      this.strongLoop.setHidden();
      this.instantBreak.setHidden();
      this.attackFactor.setHidden();
      this.silentSwap.setHidden();
      this.remove.setHidden();
      this.updateDelay.setHidden();
      this.rotations.setHidden();
      this.predict.setHidden();
      this.predictTicks.setHidden();
      this.maxMotionY.setHidden();
      this.usingPause.setHidden();
      this.guiPause.setHidden();
      this.shrink.setHidden();
      this.fill.setHidden();
      this.box.setHidden();
      this.lineWidth.setHidden();
      this.sliderSpeed.setHidden();
      this.startFade.setHidden();
      this.fadeSpeed.setHidden();
      this.damageText.setHidden();
      this.textColor.setHidden();
      this.updateTimer = new Timer();
      this.placeTimer = new Timer();
      this.breakTimer = new Timer();
      this.renderTimer = new Timer();
      this.rotationTimer = new Timer();
      this.placeTickCount = 0;
      this.damageFormat = new DecimalFormat("0.0");
      this.setChinese("水晶速爆");
      INSTANCE = this;
      Arc.EVENT_BUS.subscribe(new FastRender());
   }

   public String getInfo() {
      return this.bestTarget != null && this.targetDamage > 0.0F ? this.damageFormat.format((double)this.targetDamage) : null;
   }

   public void onEnable() {
      this.resetState();
   }

   public void onDisable() {
      this.resetState();
   }

   private void resetState() {
      this.bestPlacePos = null;
      this.bestBreakCrystal = null;
      this.targetCrystal = null;
      this.bestTarget = null;
      this.bestPlaceDamage = 0.0F;
      this.bestBreakDamage = 0.0F;
      this.targetDamage = 0.0F;
      this.rotationTarget = null;
      this.renderTargetPos = null;
      this.renderPos = null;
      this.renderScale = (double)0.0F;
   }

   private long crystalPeriod() {
      int v = this.cps.getValueInt();
      if (v < 1) {
         v = 1;
      }

      return (long)((double)1000.0F / (double)v);
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.rotationTimer.passedMs((double)300.0F)) {
         this.rotationTarget = null;
      }

      if (this.rotationTarget != null) {
         event.setTarget(this.rotationTarget, 1.0F, 100.0F);
      }

   }

   private void setRotationTarget(Vec3d vec) {
      if (this.rotations.getValue() && vec != null) {
         this.rotationTarget = vec;
         this.rotationTimer.reset();
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (!event.isPost()) {
            ++this.placeTickCount;
            if (this.shouldPause()) {
               this.placePosReset();
            } else {
               long period = this.crystalPeriod();
               if (this.updateTimer.passedMs((double)this.updateDelay.getValueInt())) {
                  this.updateTimer.reset();
                  this.findBestTargets();
                  this.targetCrystal = this.bestBreakCrystal;
                  this.targetDamage = Math.max(this.bestPlaceDamage, this.bestBreakDamage);
               }

               this.processTargets(period);
            }
         }
      }
   }

   private void placePosReset() {
      this.bestPlacePos = null;
      this.bestBreakCrystal = null;
      this.targetCrystal = null;
      this.bestTarget = null;
      this.rotationTarget = null;
   }

   private void processTargets(long period) {
      if (this.targetCrystal != null) {
         this.breakCrystal(this.targetCrystal, period);
      }

      BlockPos pos = this.bestPlacePos;
      if (pos != null) {
         if (this.hasCrystalAt(pos)) {
            this.breakNearestCrystal(pos);
         } else if (this.placeRate.getValueInt() <= 1 || this.placeTickCount % this.placeRate.getValueInt() == 0) {
            if (!this.onlyBestDamage.getValue() || !(this.bestBreakDamage > 0.0F) || !(this.bestPlaceDamage < this.bestBreakDamage)) {
               if (this.rotations.getValue()) {
                  this.setRotationTarget(this.getPlaceRotationVec(pos));
               }

               this.placeCrystal(pos, period, false);
            }
         }
      }
   }

   private void findBestTargets() {
      this.bestBreakCrystal = null;
      this.bestBreakDamage = 0.0F;
      this.bestPlacePos = null;
      this.bestPlaceDamage = 0.0F;
      this.bestTarget = null;
      ArrayList<PlayerEntity> enemies = new ArrayList();

      for(PlayerEntity player : CombatUtil.getEnemies((double)this.targetRange.getValueFloat())) {
         enemies.add(player);
      }

      if (!enemies.isEmpty()) {
         int predictTicks = this.predict.getValue() ? this.predictTicks.getValueInt() : 0;
         ArrayList<PlayerEntityPredict> predicted = new ArrayList();

         for(PlayerEntity enemy : enemies) {
            predicted.add(new PlayerEntityPredict(enemy, this.maxMotionY.getValue(), predictTicks, 5, false, false, false, true));
         }

         PlayerEntityPredict selfPredict = new PlayerEntityPredict(mc.player, this.maxMotionY.getValue(), 0, 5, false, false, false, true);
         float hpPool = mc.player.getHealth() + mc.player.getAbsorptionAmount();

         for(Entity entity : Arc.THREAD.getEntities()) {
            if (entity instanceof EndCrystalEntity) {
               EndCrystalEntity crystal = (EndCrystalEntity)entity;
               if (crystal.isAlive()) {
                  Vec3d attackVec = this.getVisiblePoint(crystal.getPos());
                  if (attackVec != null) {
                     double dist = mc.player.getEyePos().distanceTo(attackVec);
                     if (!(dist > this.breakRange.getValue()) && (mc.player.canSee(crystal) || !(dist > this.wallRange.getValue()))) {
                        float selfDamage = this.getCrystalDamageFromVec(crystal.getPos(), selfPredict);
                        if (!((double)selfDamage > this.maxSelfDamage.getValue()) && (!(this.reserve.getValue() > (double)0.0F) || !((double)selfDamage > (double)hpPool - this.reserve.getValue()))) {
                           for(PlayerEntityPredict pap : predicted) {
                              float damage = this.getCrystalDamageFromVec(crystal.getPos(), pap);
                              if (damage > this.bestBreakDamage && !((double)damage < this.minDamage.getValue())) {
                                 this.bestBreakDamage = damage;
                                 this.bestBreakCrystal = crystal;
                                 this.bestTarget = pap.player;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         float range = this.placeRange.getValueFloat() + 1.5F;

         for(BlockPos pos : BlockUtil.getSphere(range)) {
            Vec3d attackVec = this.getVisiblePoint(pos.toBottomCenterPos());
            if (attackVec != null && this.isWithinReach(pos.down(), this.placeRange.getValue()) && this.isMoving(pos)) {
               float selfDamage = this.getCrystalDamage(pos, selfPredict);
               if (!((double)selfDamage > this.maxSelfDamage.getValue()) && (!(this.reserve.getValue() > (double)0.0F) || !((double)selfDamage > (double)hpPool - this.reserve.getValue()))) {
                  for(PlayerEntityPredict pap : predicted) {
                     float damage = this.getCrystalDamage(pos, pap);
                     if (damage > this.bestPlaceDamage && !((double)damage < this.minDamage.getValue())) {
                        this.bestPlaceDamage = damage;
                        this.bestPlacePos = pos;
                        this.bestTarget = pap.player;
                     }
                  }
               }
            }
         }

      }
   }

   private void breakCrystal(EndCrystalEntity crystal, long period) {
      if (crystal != null && crystal.isAlive()) {
         if (this.breakTimer.passedMs((double)period)) {
            if (this.canBreakCrystal(crystal)) {
               if (this.rotations.getValue()) {
                  Vec3d attackVec = this.getVisiblePoint(crystal.getPos());
                  this.setRotationTarget(attackVec != null ? attackVec : crystal.getPos());
               }

               this.attackCrystal(crystal);
            }
         }
      }
   }

   private boolean breakNearestCrystal(BlockPos pos) {
      if (!this.breakTimer.passedMs((double)this.crystalPeriod())) {
         return false;
      } else {
         ArrayList<BlockPos> positions = new ArrayList();
         positions.add(pos);
         positions.add(pos.up());

         for(int x = -1; x <= 1; ++x) {
            for(int z = -1; z <= 1; ++z) {
               if (x != 0 || z != 0) {
                  BlockPos offset = pos.add(x, 0, z);
                  positions.add(offset);
                  positions.add(offset.up());
               }
            }
         }

         ArrayList<EndCrystalEntity> crystals = new ArrayList();

         for(BlockPos checkPos : positions) {
            for(Entity entity : BlockUtil.getEndCrystals(new Box(checkPos))) {
               EndCrystalEntity crystal = (EndCrystalEntity)entity;
               if (crystal.isAlive() && !crystals.contains(crystal)) {
                  crystals.add(crystal);
               }
            }
         }

         if (crystals.isEmpty()) {
            return false;
         } else {
            EndCrystalEntity closest = null;
            double closestDist = Double.MAX_VALUE;

            for(EndCrystalEntity crystal : crystals) {
               Vec3d attackVec = this.getVisiblePoint(crystal.getPos());
               if (attackVec != null) {
                  double dist = mc.player.getEyePos().distanceTo(attackVec);
                  if (!(dist > this.breakRange.getValue()) && (mc.player.canSee(crystal) || !(dist > this.wallRange.getValue())) && dist < closestDist) {
                     closestDist = dist;
                     closest = crystal;
                  }
               }
            }

            if (closest != null) {
               if (this.rotations.getValue()) {
                  Vec3d attackVec = this.getVisiblePoint(closest.getPos());
                  this.setRotationTarget(attackVec != null ? attackVec : closest.getPos());
               }

               this.attackCrystal(closest);
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private void attackCrystal(EndCrystalEntity crystal) {
      if (crystal != null && crystal.isAlive()) {
         PlayerInteractEntityC2SPacket attack = PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking());
         mc.getNetworkHandler().sendPacket(attack);
         this.sendAttackFactor(attack);
         mc.player.swingHand(Hand.MAIN_HAND);
         mc.player.resetLastAttackedTicks();
         if (this.remove.getValue()) {
            mc.world.removeEntity(crystal.getId(), RemovalReason.KILLED);
         }

         this.breakTimer.reset();
         if (this.strongLoop.getValue() && this.bestPlacePos != null && !this.hasCrystalAt(this.bestPlacePos)) {
            this.placeCrystal(this.bestPlacePos, 0L, true);
         }

      }
   }

   private void sendAttackFactor(PlayerInteractEntityC2SPacket base) {
      int factor = this.fastLayer.getValue() ? this.attackFactor.getValueInt() : 1;
      if (factor > 1) {
         try {
            ensureAttackReflection();
            if (!ATTACK_REFLECT_READY) {
               return;
            }

            int entityId = ATTACK_ID_FIELD.getInt(base);
            boolean sneaking = base.isPlayerSneaking();

            for(int i = 1; i < factor; ++i) {
               mc.getNetworkHandler().sendPacket((Packet)ATTACK_CTOR.newInstance(entityId + i, sneaking, ATTACK_TYPE));
            }
         } catch (Throwable var6) {
         }

      }
   }

   private static synchronized void ensureAttackReflection() {
      if (!ATTACK_REFLECT_READY) {
         try {
            Class<?> typeHandler = Class.forName("net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket$InteractTypeHandler");
            ATTACK_CTOR = PlayerInteractEntityC2SPacket.class.getDeclaredConstructor(Integer.TYPE, Boolean.TYPE, typeHandler);
            ATTACK_CTOR.setAccessible(true);
            Field attackField = PlayerInteractEntityC2SPacket.class.getDeclaredField("ATTACK");
            attackField.setAccessible(true);
            ATTACK_TYPE = attackField.get((Object)null);
            ATTACK_ID_FIELD = PlayerInteractEntityC2SPacket.class.getDeclaredField("entityId");
            ATTACK_ID_FIELD.setAccessible(true);
            ATTACK_REFLECT_READY = true;
         } catch (Throwable var2) {
            ATTACK_REFLECT_READY = false;
         }

      }
   }

   @EventListener
   private void onEntitySpawn(EntitySpawnedEvent event) {
      if (!nullCheck()) {
         if (!this.shouldPause()) {
            if (this.fastLayer.getValue() && this.instantBreak.getValue()) {
               Entity entity = event.getEntity();
               if (entity instanceof EndCrystalEntity) {
                  EndCrystalEntity crystal = (EndCrystalEntity)entity;
                  BlockPos pos = this.bestPlacePos;
                  if (pos != null) {
                     if (this.blockBoxContains(pos, crystal.getPos())) {
                        this.attackCrystal(crystal);
                     }

                  }
               }
            }
         }
      }
   }

   private void placeCrystal(BlockPos pos, long period, boolean force) {
      if (force || this.placeTimer.passedMs((double)period)) {
         if (!this.hasCrystalAt(pos)) {
            if (this.isMoving(pos)) {
               if (this.isWithinReach(pos.down(), this.placeRange.getValue())) {
                  boolean mainHandCrystal = mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL;
                  boolean offHandCrystal = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
                  int crystalSlot = -1;
                  if (!mainHandCrystal && !offHandCrystal) {
                     if (this.autoSwap.getValue() == CrystalHurtPeople.SwapMode.None) {
                        return;
                     }

                     crystalSlot = this.findCrystalSlot();
                     if (crystalSlot == -1) {
                        return;
                     }
                  }

                  int oldSlot = mc.player.getInventory().selectedSlot;
                  if (crystalSlot != -1) {
                     this.swapToSlot(crystalSlot);
                  }

                  BlockPos obsPos = pos.down();
                  Direction facing = BlockUtil.getClickSide(obsPos);
                  if (facing == null) {
                     facing = Direction.UP;
                  }

                  Hand hand = offHandCrystal ? Hand.OFF_HAND : Hand.MAIN_HAND;
                  if (crystalSlot != -1) {
                     hand = Hand.MAIN_HAND;
                  }

                  BlockUtil.clickBlock(obsPos, facing, false, hand, SwingSide.All);
                  if (crystalSlot != -1) {
                     SwapMode mode = this.autoSwap.getValue();
                     if (mode == CrystalHurtPeople.SwapMode.Silent) {
                        InventoryUtil.switchToSlot(oldSlot);
                     } else if (mode == CrystalHurtPeople.SwapMode.Inventory) {
                        InventoryUtil.inventorySwap(crystalSlot, mc.player.getInventory().selectedSlot);
                        EntityUtil.syncInventory();
                     }
                  }

                  this.placeTimer.reset();
               }
            }
         }
      }
   }

   private int findCrystalSlot() {
      SwapMode mode = this.autoSwap.getValue();
      if (mode != CrystalHurtPeople.SwapMode.Silent && mode != CrystalHurtPeople.SwapMode.Normal) {
         return mode == CrystalHurtPeople.SwapMode.Inventory ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : -1;
      } else {
         return InventoryUtil.findItem(Items.END_CRYSTAL);
      }
   }

   private void swapToSlot(int slot) {
      if (this.fastLayer.getValue() && this.silentSwap.getValue()) {
         this.silentSelectSlot(slot);
      } else {
         SwapMode mode = this.autoSwap.getValue();
         if (mode != CrystalHurtPeople.SwapMode.Silent && mode != CrystalHurtPeople.SwapMode.Normal) {
            if (mode == CrystalHurtPeople.SwapMode.Inventory) {
               InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
            }
         } else {
            InventoryUtil.switchToSlot(slot);
         }

      }
   }

   private void silentSelectSlot(int slot) {
      if (slot >= 0 && slot <= 8) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && client.getNetworkHandler() != null) {
            int previous = client.player.getInventory().selectedSlot;
            if (previous != slot) {
               try {
                  if (INTERACTION_MANAGER_FIELD == null) {
                     INTERACTION_MANAGER_FIELD = MinecraftClient.class.getDeclaredField("interactionManager");
                     INTERACTION_MANAGER_FIELD.setAccessible(true);
                  }

                  ClientPlayerInteractionManager interactionManager = (ClientPlayerInteractionManager)INTERACTION_MANAGER_FIELD.get(client);
                  client.player.getInventory().selectedSlot = slot;
                  if (SYNC_SELECTED_SLOT_METHOD == null) {
                     SYNC_SELECTED_SLOT_METHOD = ClientPlayerInteractionManager.class.getDeclaredMethod("syncSelectedSlot");
                     SYNC_SELECTED_SLOT_METHOD.setAccessible(true);
                  }

                  SYNC_SELECTED_SLOT_METHOD.invoke(interactionManager);
               } catch (Throwable var5) {
                  client.player.getInventory().selectedSlot = previous;
               }

            }
         }
      }
   }

   private boolean shouldPause() {
      if (this.usingPause.getValue() && mc.player.isUsingItem()) {
         return true;
      } else if (this.eatingPause.getValue() && this.isEating()) {
         return true;
      } else {
         if (this.usingPause.getValue() && mc.options.useKey.isPressed()) {
            Item mainHand = mc.player.getMainHandStack().getItem();
            Item offHand = mc.player.getOffHandStack().getItem();
            if (this.isUsablePauseItem(mainHand) || this.isUsablePauseItem(offHand)) {
               return true;
            }
         }

         return this.guiPause.getValue() && mc.currentScreen instanceof HandledScreen;
      }
   }

   private boolean isEating() {
      return mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getComponents().contains(DataComponentTypes.FOOD);
   }

   private boolean isUsablePauseItem(Item item) {
      return item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE || item == Items.CHORUS_FRUIT || item == Items.POTION || item == Items.BOW || item == Items.CROSSBOW || item == Items.TRIDENT || item == Items.EXPERIENCE_BOTTLE;
   }

   private float getCrystalDamage(BlockPos pos, PlayerEntityPredict pap) {
      Vec3d vec = new Vec3d((double)pos.getX() + (double)0.5F, (double)pos.getY(), (double)pos.getZ() + (double)0.5F);
      return ExplosionUtil.calculateDamage(vec, pap.player, pap.predict, 6.0F);
   }

   private float getCrystalDamageFromVec(Vec3d vec, PlayerEntityPredict pap) {
      return ExplosionUtil.calculateDamage(vec, pap.player, pap.predict, 6.0F);
   }

   private Vec3d getVisiblePoint(Vec3d vec) {
      return MathUtil.getPointToBoxFromBottom(mc.player.getEyePos(), vec, this.breakRange.getValue(), (double)2.0F, 0.1);
   }

   public boolean isMoving(BlockPos pos) {
      BlockPos down = pos.down();
      if (BlockUtil.getBlock(down) != Blocks.BEDROCK && BlockUtil.getBlock(down) != Blocks.OBSIDIAN) {
         return false;
      } else if (BlockUtil.getClickSideStrict(down) == null) {
         return false;
      } else if (!mc.world.isAir(pos) && BlockUtil.getBlock(pos) != Blocks.FIRE) {
         return false;
      } else if (ClientSetting.INSTANCE.lowVersion.getValue() && !mc.world.isAir(pos.up())) {
         return false;
      } else {
         return !this.isBlockClear(pos) ? false : this.isBlockClear(pos.up());
      }
   }

   private boolean isBlockClear(BlockPos pos) {
      Box box = new Box(pos);

      for(Entity entity : BlockUtil.getEntities(box)) {
         if (entity.isAlive() && !(entity instanceof ItemEntity) && !(entity instanceof EndCrystalEntity)) {
            return false;
         }
      }

      return true;
   }

   private boolean isWithinReach(BlockPos pos, double range) {
      Direction side = BlockUtil.getClickSideStrict(pos);
      if (side == null) {
         return false;
      } else {
         Vec3d sideOffset = new Vec3d((double)side.getVector().getX() * (double)0.5F, (double)side.getVector().getY() * (double)0.5F, (double)side.getVector().getZ() * (double)0.5F);
         return pos.toCenterPos().add(sideOffset).distanceTo(mc.player.getEyePos()) <= range;
      }
   }

   private boolean canBreakCrystal(EndCrystalEntity crystal) {
      Vec3d attackVec = this.getVisiblePoint(crystal.getPos());
      if (attackVec == null) {
         return false;
      } else {
         double dist = mc.player.getEyePos().distanceTo(attackVec);
         if (dist > this.breakRange.getValue()) {
            return false;
         } else if (!mc.player.canSee(crystal)) {
            return dist <= this.wallRange.getValue();
         } else {
            return true;
         }
      }
   }

   private boolean hasCrystalAt(BlockPos pos) {
      Box box = new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 2), (double)(pos.getZ() + 1));

      for(Entity entity : Arc.THREAD.getEntities()) {
         if (entity instanceof EndCrystalEntity crystal) {
            if (crystal.isAlive() && crystal.getBoundingBox().intersects(box)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean blockBoxContains(BlockPos pos, Vec3d vec) {
      Box box = new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 2), (double)(pos.getZ() + 1));
      return box.contains(vec);
   }

   private Vec3d getPlaceRotationVec(BlockPos pos) {
      BlockPos obsPos = pos.down();
      Direction side = BlockUtil.getClickSide(obsPos);
      return side == null ? obsPos.toCenterPos().add((double)0.0F, (double)0.5F, (double)0.0F) : obsPos.toCenterPos().add((double)side.getVector().getX() * (double)0.5F, (double)side.getVector().getY() * (double)0.5F, (double)side.getVector().getZ() * (double)0.5F);
   }

   public BlockPos getPlacePos() {
      return this.bestPlacePos;
   }

   public float getRenderDamage() {
      return this.targetDamage;
   }

   private class FastRender {
      @EventListener
      public void onRender3D(Render3DEvent event) {
         CrystalHurtPeople var10000 = CrystalHurtPeople.this;
         if (!CrystalHurtPeople.nullCheck()) {
            MatrixStack matrixStack = event.matrixStack;
            BlockPos blockPos = CrystalHurtPeople.this.bestPlacePos;
            if (blockPos != null) {
               CrystalHurtPeople.this.renderTargetPos = blockPos.down().toCenterPos();
               CrystalHurtPeople.this.renderTimer.reset();
            }

            if (CrystalHurtPeople.this.renderTargetPos != null) {
               boolean active = !CrystalHurtPeople.this.renderTimer.passedMs((double)((long)(CrystalHurtPeople.this.startFade.getValue() * (double)1000.0F)));
               if (CrystalHurtPeople.this.fadeSpeed.getValue() >= (double)1.0F) {
                  CrystalHurtPeople.this.renderScale = active ? (double)0.5F : (double)0.0F;
               } else {
                  CrystalHurtPeople.this.renderScale = AnimateUtil.animate(CrystalHurtPeople.this.renderScale, active ? (double)0.5F : (double)0.0F, CrystalHurtPeople.this.fadeSpeed.getValue() / (double)10.0F);
               }

               if (CrystalHurtPeople.this.renderScale <= 0.001) {
                  CrystalHurtPeople.this.renderPos = null;
               } else {
                  if (CrystalHurtPeople.this.renderPos != null && CrystalHurtPeople.this.sliderSpeed.getValue() < (double)1.0F) {
                     double speed = CrystalHurtPeople.this.sliderSpeed.getValue() / (double)10.0F;
                     CrystalHurtPeople.this.renderPos = new Vec3d(AnimateUtil.animate(CrystalHurtPeople.this.renderPos.x, CrystalHurtPeople.this.renderTargetPos.x, speed), AnimateUtil.animate(CrystalHurtPeople.this.renderPos.y, CrystalHurtPeople.this.renderTargetPos.y, speed), AnimateUtil.animate(CrystalHurtPeople.this.renderPos.z, CrystalHurtPeople.this.renderTargetPos.z, speed));
                  } else {
                     CrystalHurtPeople.this.renderPos = CrystalHurtPeople.this.renderTargetPos;
                  }

                  if (CrystalHurtPeople.this.render.getValue()) {
                     Box box = new Box(CrystalHurtPeople.this.renderPos, CrystalHurtPeople.this.renderPos);
                     box = CrystalHurtPeople.this.shrink.getValue() ? box.expand(CrystalHurtPeople.this.renderScale) : box.expand((double)0.5F);
                     double scale = CrystalHurtPeople.this.renderScale * (double)2.0F;
                     if (CrystalHurtPeople.this.fill.booleanValue) {
                        int alpha = (int)((double)CrystalHurtPeople.this.fill.getValue().getAlpha() * scale);
                        Color color = ColorUtil.injectAlpha(CrystalHurtPeople.this.fill.getValue(), alpha);
                        Render3DUtil.drawFill(matrixStack, box, color);
                     }

                     if (CrystalHurtPeople.this.box.booleanValue) {
                        int alpha = (int)((double)CrystalHurtPeople.this.box.getValue().getAlpha() * scale);
                        Color color = ColorUtil.injectAlpha(CrystalHurtPeople.this.box.getValue(), alpha);
                        Render3DUtil.drawBox(matrixStack, box, color, CrystalHurtPeople.this.lineWidth.getValueFloat());
                     }

                     if (CrystalHurtPeople.this.damageText.getValue() && CrystalHurtPeople.this.targetDamage > 0.0F && active) {
                        Render3DUtil.drawText3D(CrystalHurtPeople.this.damageFormat.format((double)CrystalHurtPeople.this.targetDamage), CrystalHurtPeople.this.renderPos, CrystalHurtPeople.this.textColor.getValue());
                     }

                  }
               }
            }
         }
      }
   }

   public static enum Page {
      Combat,
      Misc,
      Render;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{Combat, Misc, Render};
      }
   }

   public static enum SwapMode {
      None,
      Normal,
      Silent,
      Inventory;

      // $FF: synthetic method
      private static SwapMode[] $values() {
         return new SwapMode[]{None, Normal, Silent, Inventory};
      }
   }
}
