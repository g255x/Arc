package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.Render3DEvent;
import dev.G255.Lumi.api.events.impl.RotationEvent;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.entity.PlayerEntityPredict;
import dev.G255.Lumi.api.utils.math.AnimateUtil;
import dev.G255.Lumi.api.utils.math.ExplosionUtil;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.render.ColorUtil;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.exploit.Blink;
import dev.G255.Lumi.mod.modules.impl.movement.ElytraFly;
import dev.G255.Lumi.mod.modules.impl.movement.Velocity;
import dev.G255.Lumi.mod.modules.impl.player.AirPlace;
import dev.G255.Lumi.mod.modules.settings.enums.SwingSide;
import dev.G255.Lumi.mod.modules.settings.enums.Timing;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.ColorSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.HitResult;

public class AutoAnchor extends Module {
   public static AutoAnchor INSTANCE;
   static Vec3d placeVec3d;
   static Vec3d curVec3d;
   public final EnumSetting page;
   public final SliderSetting range;
   public final SliderSetting targetRange;
   public final SliderSetting minDamage;
   public final SliderSetting breakMin;
   public final SliderSetting headDamage;
   private final SliderSetting selfPredict;
   private final SliderSetting predictTicks;
   private final SliderSetting simulation;
   private final SliderSetting maxMotionY;
   private final BooleanSetting step;
   private final BooleanSetting doubleStep;
   private final BooleanSetting jump;
   private final BooleanSetting inBlockPause;
   final ArrayList chargeList;
   private final BooleanSetting assist;
   private final BooleanSetting obsidian;
   private final BooleanSetting checkMine;
   private final SliderSetting assistRange;
   private final SliderSetting assistDamage;
   private final SliderSetting delay;
   private final BooleanSetting preferCrystal;
   private final BooleanSetting thread;
   private final BooleanSetting light;
   private final BooleanSetting inventorySwap;
   private final BooleanSetting breakCrystal;
   private final BooleanSetting spam;
   private final BooleanSetting mineSpam;
   private final BooleanSetting spamPlace;
   private final BooleanSetting inSpam;
   private final BooleanSetting usingPause;
   private final EnumSetting swingMode;
   private final EnumSetting timing;
   private final SliderSetting placeDelay;
   private final SliderSetting fillDelay;
   private final SliderSetting breakDelay;
   private final SliderSetting spamDelay;
   private final SliderSetting updateDelay;
   private final BooleanSetting rotate;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   private final BooleanSetting noSuicide;
   private final BooleanSetting smart;
   private final BooleanSetting terrainIgnore;
   private final SliderSetting minPrefer;
   private final SliderSetting maxSelfDamage;
   private final EnumSetting mode;
   private final ColorSetting color;
   private final ColorSetting outlineColor;
   private final BooleanSetting render;
   private final BooleanSetting shrink;
   private final ColorSetting box;
   private final ColorSetting fill;
   private final SliderSetting sliderSpeed;
   private final SliderSetting startFadeTime;
   private final SliderSetting fadeSpeed;
   private final Timer delayTimer;
   private final Timer calcTimer;
   private final Timer noPosTimer;
   private final Timer assistTimer;
   public Vec3d directionVec;
   public PlayerEntity displayTarget;
   public BlockPos currentPos;
   public BlockPos tempPos;
   public double lastDamage;
   double fade;
   BlockPos assistPos;

   public AutoAnchor() {
      super("AutoAnchor", Module.Category.Combat);
      this.page = this.add(new EnumSetting("Page", AutoAnchor.Page.General));
      this.range = this.add((new SliderSetting("Range", (double)5.0F, (double)0.0F, (double)6.0F, 0.1, () -> this.page.getValue() == AutoAnchor.Page.General)).setSuffix("m"));
      this.targetRange = this.add((new SliderSetting("TargetRange", (double)8.0F, 0.1, (double)12.0F, 0.1, () -> this.page.getValue() == AutoAnchor.Page.General)).setSuffix("m"));
      this.minDamage = this.add((new SliderSetting("Min", (double)4.0F, (double)0.0F, (double)36.0F, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact)).setSuffix("dmg"));
      this.breakMin = this.add((new SliderSetting("ExplosionMin", (double)4.0F, (double)0.0F, (double)36.0F, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact)).setSuffix("dmg"));
      this.headDamage = this.add((new SliderSetting("ForceHead", (double)7.0F, (double)0.0F, (double)36.0F, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact)).setSuffix("dmg"));
      this.selfPredict = this.add((new SliderSetting("SelfPredict", 4, 0, 10, () -> this.page.getValue() == AutoAnchor.Page.Predict)).setSuffix("ticks"));
      this.predictTicks = this.add((new SliderSetting("Predict", 4, 0, 10, () -> this.page.getValue() == AutoAnchor.Page.Predict)).setSuffix("ticks"));
      this.simulation = this.add(new SliderSetting("Simulation", (double)5.0F, (double)0.0F, (double)20.0F, (double)1.0F, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.maxMotionY = this.add(new SliderSetting("MaxMotionY", 0.34, (double)0.0F, (double)2.0F, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.step = this.add(new BooleanSetting("Step", false, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.doubleStep = this.add(new BooleanSetting("DoubleStep", false, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.jump = this.add(new BooleanSetting("Jump", false, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.inBlockPause = this.add(new BooleanSetting("InBlockPause", true, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.chargeList = new ArrayList();
      this.assist = this.add(new BooleanSetting("Assist", true, () -> this.page.getValue() == AutoAnchor.Page.Assist));
      this.obsidian = this.add(new BooleanSetting("Obsidian", true, () -> this.page.getValue() == AutoAnchor.Page.Assist));
      this.checkMine = this.add(new BooleanSetting("DetectMining", false, () -> this.page.getValue() == AutoAnchor.Page.Assist));
      this.assistRange = this.add((new SliderSetting("AssistRange", (double)5.0F, (double)0.0F, (double)6.0F, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Assist)).setSuffix("m"));
      this.assistDamage = this.add((new SliderSetting("AssistDamage", (double)6.0F, (double)0.0F, (double)36.0F, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Assist)).setSuffix("h"));
      this.delay = this.add((new SliderSetting("AssistDelay", 0.1, (double)0.0F, (double)1.0F, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Assist)).setSuffix("s"));
      this.preferCrystal = this.add(new BooleanSetting("PreferCrystal", false, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.thread = this.add(new BooleanSetting("Thread", false, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.light = this.add(new BooleanSetting("LessCPU", true, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.inventorySwap = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.breakCrystal = this.add((new BooleanSetting("BreakCrystal", true, () -> this.page.getValue() == AutoAnchor.Page.General)).setParent());
      this.spam = this.add((new BooleanSetting("Spam", true, () -> this.page.getValue() == AutoAnchor.Page.General)).setParent());
      this.mineSpam = this.add(new BooleanSetting("OnlyMining", true, () -> this.page.getValue() == AutoAnchor.Page.General && this.spam.isOpen()));
      this.spamPlace = this.add((new BooleanSetting("Fast", true, () -> this.page.getValue() == AutoAnchor.Page.General)).setParent());
      this.inSpam = this.add(new BooleanSetting("WhenSpamming", true, () -> this.page.getValue() == AutoAnchor.Page.General && this.spamPlace.isOpen()));
      this.usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.swingMode = this.add(new EnumSetting("Swing", SwingSide.All, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.timing = this.add(new EnumSetting("Timing", Timing.All, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.placeDelay = this.add((new SliderSetting("PlaceDelay", (double)100.0F, (double)0.0F, (double)500.0F, (double)1.0F, () -> this.page.getValue() == AutoAnchor.Page.General)).setSuffix("ms"));
      this.fillDelay = this.add((new SliderSetting("FillDelay", (double)100.0F, (double)0.0F, (double)500.0F, (double)1.0F, () -> this.page.getValue() == AutoAnchor.Page.General)).setSuffix("ms"));
      this.breakDelay = this.add((new SliderSetting("BreakDelay", (double)100.0F, (double)0.0F, (double)500.0F, (double)1.0F, () -> this.page.getValue() == AutoAnchor.Page.General)).setSuffix("ms"));
      this.spamDelay = this.add((new SliderSetting("SpamDelay", (double)200.0F, (double)0.0F, (double)1000.0F, (double)1.0F, () -> this.page.getValue() == AutoAnchor.Page.General)).setSuffix("ms"));
      this.updateDelay = this.add((new SliderSetting("UpdateDelay", (double)200.0F, (double)0.0F, (double)1000.0F, (double)1.0F, () -> this.page.getValue() == AutoAnchor.Page.General)).setSuffix("ms"));
      this.rotate = this.add((new BooleanSetting("Rotate", true, () -> this.page.getValue() == AutoAnchor.Page.Rotate)).setParent());
      this.yawStep = this.add((new BooleanSetting("YawStep", true, () -> this.rotate.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate)).setParent());
      this.whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate));
      this.steps = this.add(new SliderSetting("Steps", 0.05, (double)0.0F, (double)1.0F, 0.01, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate));
      this.checkFov = this.add(new BooleanSetting("OnlyLooking", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate));
      this.fov = this.add(new SliderSetting("Fov", (double)20.0F, (double)0.0F, (double)360.0F, 0.1, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.checkFov.getValue() && this.page.getValue() == AutoAnchor.Page.Rotate));
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate));
      this.noSuicide = this.add(new BooleanSetting("NoSuicide", true, () -> this.page.getValue() == AutoAnchor.Page.Interact));
      this.smart = this.add(new BooleanSetting("Smart", true, () -> this.page.getValue() == AutoAnchor.Page.Interact));
      this.terrainIgnore = this.add(new BooleanSetting("TerrainIgnore", true, () -> this.page.getValue() == AutoAnchor.Page.Interact));
      this.minPrefer = this.add((new SliderSetting("Prefer", (double)7.0F, (double)0.0F, (double)36.0F, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact)).setSuffix("dmg"));
      this.maxSelfDamage = this.add((new SliderSetting("MaxSelf", (double)8.0F, (double)0.0F, (double)36.0F, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact)).setSuffix("dmg"));
      this.mode = this.add(new EnumSetting("TargetESP", Aura.TargetESP.Jello, () -> this.page.getValue() == AutoAnchor.Page.Render));
      this.color = this.add(new ColorSetting("TargetColor", new Color(255, 255, 255, 250), () -> this.page.getValue() == AutoAnchor.Page.Render));
      this.outlineColor = this.add(new ColorSetting("TargetOutlineColor", new Color(255, 255, 255, 250), () -> this.page.getValue() == AutoAnchor.Page.Render));
      this.render = this.add(new BooleanSetting("Render", true, () -> this.page.getValue() == AutoAnchor.Page.Render));
      this.shrink = this.add(new BooleanSetting("Shrink", true, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue()));
      this.box = this.add((new ColorSetting("Box", new Color(255, 255, 255, 255), () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())).injectBoolean(true));
      this.fill = this.add((new ColorSetting("Fill", new Color(255, 255, 255, 100), () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())).injectBoolean(true));
      this.sliderSpeed = this.add(new SliderSetting("SliderSpeed", 0.2, (double)0.0F, (double)1.0F, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue()));
      this.startFadeTime = this.add((new SliderSetting("StartFade", 0.3, (double)0.0F, (double)2.0F, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())).setSuffix("s"));
      this.fadeSpeed = this.add(new SliderSetting("FadeSpeed", 0.2, 0.01, (double)1.0F, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue()));
      this.delayTimer = new Timer();
      this.calcTimer = new Timer();
      this.noPosTimer = new Timer();
      this.assistTimer = new Timer();
      this.directionVec = null;
      this.fade = (double)0.0F;
      this.setChinese("");
      INSTANCE = this;
      Lumi.EVENT_BUS.subscribe((Object)(new AnchorRender()));
   }

   public static boolean canSee(Vec3d from, Vec3d to) {
      BlockHitResult result = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
      return result == null || result.getType() == HitResult.Type.MISS;
   }

   public String getInfo() {
      return this.displayTarget != null && this.currentPos != null ? this.displayTarget.getName().getString() : null;
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (this.displayTarget != null && this.currentPos != null) {
         Aura.doRender(matrixStack, mc.getRenderTickCounter().getTickDelta(true), this.displayTarget, this.color.getValue(), this.outlineColor.getValue(), (Aura.TargetESP)this.mode.getValue());
      }

   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.currentPos != null && this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }

   }

   public void onDisable() {
      this.tempPos = null;
      this.currentPos = null;
   }

   public void onThread() {
      if (!this.isOff() && !nullCheck()) {
         if (this.thread.getValue()) {
            if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
               this.currentPos = null;
               return;
            }

            if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null && this.preferCrystal.getValue()) {
               this.currentPos = null;
               return;
            }

            int anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
            int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
            int unBlock = this.inventorySwap.getValue() ? anchor : InventoryUtil.findUnBlock();
            if (anchor == -1) {
               this.currentPos = null;
               return;
            }

            if (glowstone == -1) {
               this.currentPos = null;
               return;
            }

            if (unBlock == -1) {
               this.currentPos = null;
               return;
            }

            if (mc.player.isSneaking()) {
               this.currentPos = null;
               return;
            }

            if (this.usingPause.getValue() && mc.player.isUsingItem()) {
               this.currentPos = null;
               return;
            }

            this.calc();
         }

      }
   }

   private boolean shouldYawStep() {
      if (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())) {
         return this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
      } else {
         return false;
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            int anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
            int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
            int unBlock = this.inventorySwap.getValue() ? anchor : InventoryUtil.findUnBlock();
            int old = mc.player.getInventory().selectedSlot;
            if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
               this.currentPos = null;
            } else if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null) {
               this.currentPos = null;
            } else if (anchor == -1) {
               this.currentPos = null;
            } else if (glowstone == -1) {
               this.currentPos = null;
            } else if (unBlock == -1) {
               this.currentPos = null;
            } else if (mc.player.isSneaking()) {
               this.currentPos = null;
            } else if (this.usingPause.getValue() && mc.player.isUsingItem()) {
               this.currentPos = null;
            } else if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
               if (this.assist.getValue()) {
                  this.onAssist();
               }

               if (!this.thread.getValue()) {
                  this.calc();
               }

               BlockPos pos;
               if ((pos = this.currentPos) != null) {
                  if (this.breakCrystal.getValue()) {
                     CombatUtil.attackCrystal(new BlockPos(pos), this.rotate.getValue(), false);
                  }

                  boolean shouldSpam = this.spam.getValue() && (!this.mineSpam.getValue() || Lumi.BREAK.isMining(pos));
                  if (shouldSpam) {
                     if (!this.delayTimer.passed((long)this.spamDelay.getValueFloat())) {
                        return;
                     }

                     this.delayTimer.reset();
                     if (BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                        this.placeBlock(pos, this.rotate.getValue(), anchor);
                     }

                     if (!this.chargeList.contains(pos)) {
                        this.delayTimer.reset();
                        this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), glowstone);
                        this.chargeList.add(pos);
                     }

                     this.chargeList.remove(pos);
                     this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), unBlock);
                     if (this.spamPlace.getValue() && this.inSpam.getValue()) {
                        if (this.shouldYawStep() && this.checkFov.getValue()) {
                           Direction side = BlockUtil.getClickSide(pos);
                           Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
                           if (Lumi.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
                              CombatUtil.modifyPos = pos;
                              CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                              this.placeBlock(pos, this.rotate.getValue(), anchor);
                              CombatUtil.modifyPos = null;
                           }
                        } else {
                           CombatUtil.modifyPos = pos;
                           CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                           this.placeBlock(pos, this.rotate.getValue(), anchor);
                           CombatUtil.modifyPos = null;
                        }
                     }
                  } else if (BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                     if (!this.delayTimer.passed((long)this.placeDelay.getValueFloat())) {
                        return;
                     }

                     this.delayTimer.reset();
                     this.placeBlock(pos, this.rotate.getValue(), anchor);
                  } else if (BlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR) {
                     if (!this.chargeList.contains(pos)) {
                        if (!this.delayTimer.passed((long)this.fillDelay.getValueFloat())) {
                           return;
                        }

                        this.delayTimer.reset();
                        this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), glowstone);
                        this.chargeList.add(pos);
                     } else {
                        if (!this.delayTimer.passed((long)this.breakDelay.getValueFloat())) {
                           return;
                        }

                        this.delayTimer.reset();
                        this.chargeList.remove(pos);
                        this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), unBlock);
                        if (this.spamPlace.getValue()) {
                           if (this.shouldYawStep() && this.checkFov.getValue()) {
                              Direction side = BlockUtil.getClickSide(pos);
                              Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
                              if (Lumi.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
                                 CombatUtil.modifyPos = pos;
                                 CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                                 this.placeBlock(pos, this.rotate.getValue(), anchor);
                                 CombatUtil.modifyPos = null;
                              }
                           } else {
                              CombatUtil.modifyPos = pos;
                              CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                              this.placeBlock(pos, this.rotate.getValue(), anchor);
                              CombatUtil.modifyPos = null;
                           }
                        }
                     }
                  }

                  if (!this.inventorySwap.getValue()) {
                     this.doSwap(old);
                  }
               }

            }
         }
      }
   }

   private void calc() {
      if (!nullCheck()) {
         if (this.calcTimer.passed((long)this.updateDelay.getValueFloat())) {
            this.calcTimer.reset();
            PlayerEntityPredict selfPredict = new PlayerEntityPredict(mc.player, this.maxMotionY.getValue(), this.selfPredict.getValueInt(), this.simulation.getValueInt(), this.step.getValue(), this.doubleStep.getValue(), this.jump.getValue(), this.inBlockPause.getValue());
            this.tempPos = null;
            double placeDamage = this.minDamage.getValue();
            double breakDamage = this.breakMin.getValue();
            boolean anchorFound = false;
            List<PlayerEntity> enemies = CombatUtil.getEnemies(this.targetRange.getValue());
            ArrayList<PlayerEntityPredict> list = new ArrayList();

            for(PlayerEntity player : enemies) {
               list.add(new PlayerEntityPredict(player, this.maxMotionY.getValue(), this.predictTicks.getValueInt(), this.simulation.getValueInt(), this.step.getValue(), this.doubleStep.getValue(), this.jump.getValue(), this.inBlockPause.getValue()));
            }

            for(PlayerEntityPredict pap : list) {
               BlockPos pos = EntityUtil.getEntityPos(pap.player, true).up(2);
               double selfDamage;
               if ((BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue()) || BlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR && BlockUtil.getClickSideStrict(pos) != null) && !((selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue()) && (!this.noSuicide.getValue() || !(selfDamage > (double)(mc.player.getHealth() + mc.player.getAbsorptionAmount())))) {
                  double damage = this.getAnchorDamage(pos, pap.player, pap.predict);
                  if (damage > (double)this.headDamage.getValueFloat() && (!this.smart.getValue() || !(selfDamage > damage))) {
                     this.lastDamage = damage;
                     this.displayTarget = pap.player;
                     this.tempPos = pos;
                     break;
                  }
               }
            }

            if (this.tempPos == null) {
               for(BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat() + 1.0F, mc.player.getEyePos())) {
                  for(PlayerEntityPredict pap : list) {
                     if (this.light.getValue()) {
                        CombatUtil.modifyPos = pos;
                        CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                        boolean skip = !canSee(pos.toCenterPos(), pap.predict.getPos());
                        CombatUtil.modifyPos = null;
                        if (skip) {
                           continue;
                        }
                     }

                     if (BlockUtil.getBlock(pos) != Blocks.RESPAWN_ANCHOR) {
                        if (!anchorFound && BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                           CombatUtil.modifyPos = pos;
                           CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                           boolean skip = BlockUtil.getClickSideStrict(pos) == null;
                           CombatUtil.modifyPos = null;
                           double damage;
                           double selfDamage2;
                           if (!skip && (damage = this.getAnchorDamage(pos, pap.player, pap.predict)) >= placeDamage && (AutoCrystal.INSTANCE.crystalPos == null || AutoCrystal.INSTANCE.isOff() || (double)AutoCrystal.INSTANCE.lastDamage < damage) && !((selfDamage2 = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue()) && (!this.noSuicide.getValue() || !(selfDamage2 > (double)(mc.player.getHealth() + mc.player.getAbsorptionAmount()))) && (!this.smart.getValue() || !(selfDamage2 > damage))) {
                              this.lastDamage = damage;
                              this.displayTarget = pap.player;
                              placeDamage = damage;
                              this.tempPos = pos;
                           }
                        }
                     } else {
                        double damage2 = this.getAnchorDamage(pos, pap.player, pap.predict);
                        if (BlockUtil.getClickSideStrict(pos) != null && damage2 >= breakDamage) {
                           if (damage2 >= this.minPrefer.getValue()) {
                              anchorFound = true;
                           }

                           double selfDamage;
                           if ((anchorFound || !(damage2 < placeDamage)) && (AutoCrystal.INSTANCE.crystalPos == null || AutoCrystal.INSTANCE.isOff() || (double)AutoCrystal.INSTANCE.lastDamage < damage2) && !((selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue()) && (!this.noSuicide.getValue() || !(selfDamage > (double)(mc.player.getHealth() + mc.player.getAbsorptionAmount()))) && (!this.smart.getValue() || !(selfDamage > damage2))) {
                              this.lastDamage = damage2;
                              this.displayTarget = pap.player;
                              breakDamage = damage2;
                              this.tempPos = pos;
                           }
                        }
                     }
                  }
               }
            }
         }

         this.currentPos = this.tempPos;
      }
   }

   public double getAnchorDamage(BlockPos anchorPos, PlayerEntity target, PlayerEntity predict) {
      if (this.terrainIgnore.getValue()) {
         CombatUtil.terrainIgnore = true;
      }

      double damage = (double)ExplosionUtil.anchorDamage(anchorPos, target, predict);
      CombatUtil.terrainIgnore = false;
      return damage;
   }

   public void placeBlock(BlockPos pos, boolean rotate, int slot) {
      if (BlockUtil.allowAirPlace()) {
         this.airPlace(pos, rotate, slot);
      } else {
         Direction side = BlockUtil.getPlaceSide(pos);
         if (side != null) {
            this.clickBlock(pos.offset(side), side.getOpposite(), rotate, slot);
         }
      }
   }

   public void clickBlock(BlockPos pos, Direction side, boolean rotate, int slot) {
      if (pos != null) {
         Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
         if (!rotate || this.faceVector(directionVec)) {
            this.doSwap(slot);
            EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)this.swingMode.getValue());
            BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
            Module.sendSequencedPacket((id) -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
            if (this.inventorySwap.getValue()) {
               this.doSwap(slot);
            }

            if (rotate && !this.shouldYawStep()) {
               Lumi.ROTATION.snapBack();
            }

         }
      }
   }

   public void airPlace(BlockPos pos, boolean rotate, int slot) {
      if (pos != null) {
         Direction side = BlockUtil.getClickSide(pos);
         Vec3d directionVec = new Vec3d((double)pos.getX() + (double)0.5F + (double)side.getVector().getX() * (double)0.5F, (double)pos.getY() + (double)0.5F + (double)side.getVector().getY() * (double)0.5F, (double)pos.getZ() + (double)0.5F + (double)side.getVector().getZ() * (double)0.5F);
         if (!rotate || this.faceVector(directionVec)) {
            this.doSwap(slot);
            boolean bypass = AirPlace.INSTANCE.grimBypass.getValue();
            if (bypass) {
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
            }

            EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)this.swingMode.getValue());
            BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
            Module.sendSequencedPacket((id) -> new PlayerInteractBlockC2SPacket(bypass ? Hand.OFF_HAND : Hand.MAIN_HAND, result, id));
            if (bypass) {
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
            }

            if (this.inventorySwap.getValue()) {
               this.doSwap(slot);
            }

            if (rotate && !this.shouldYawStep()) {
               Lumi.ROTATION.snapBack();
            }

         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
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

   public void onAssist() {
      this.assistPos = null;
      int anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
      int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
      int old = mc.player.getInventory().selectedSlot;
      if (anchor != -1) {
         if (this.obsidian.getValue()) {
            anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
            if (anchor == -1) {
               return;
            }
         }

         if (glowstone != -1) {
            if (!mc.player.isSneaking()) {
               if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                  if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                     if (this.assistTimer.passed((long)(this.delay.getValueFloat() * 1000.0F))) {
                        this.assistTimer.reset();
                        ArrayList<PlayerEntityPredict> list = new ArrayList();

                        for(PlayerEntity player : CombatUtil.getEnemies(this.assistRange.getValue())) {
                           list.add(new PlayerEntityPredict(player, this.maxMotionY.getValue(), this.predictTicks.getValueInt(), this.simulation.getValueInt(), this.step.getValue(), this.doubleStep.getValue(), this.jump.getValue(), this.inBlockPause.getValue()));
                        }

                        double bestDamage = this.assistDamage.getValue();

                        for(PlayerEntityPredict pap : list) {
                           BlockPos pos = EntityUtil.getEntityPos(pap.player, true).up(2);
                           if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                              return;
                           }

                           double damage;
                           if (BlockUtil.clientCanPlace(pos, false) && (damage = this.getAnchorDamage(pos, pap.player, pap.predict)) >= bestDamage) {
                              bestDamage = damage;
                              this.assistPos = pos;
                           }

                           for(Direction i : Direction.values()) {
                              double damage2;
                              if (i != Direction.UP && i != Direction.DOWN && BlockUtil.clientCanPlace(pos.offset(i), false) && (damage2 = this.getAnchorDamage(pos.offset(i), pap.player, pap.predict)) >= bestDamage) {
                                 bestDamage = damage2;
                                 this.assistPos = pos.offset(i);
                              }
                           }
                        }

                        BlockPos placePos;
                        if (this.assistPos != null && BlockUtil.getPlaceSide(this.assistPos, this.range.getValue()) == null && (placePos = this.getHelper(this.assistPos)) != null) {
                           this.doSwap(anchor);
                           BlockUtil.placeBlock(placePos, this.rotate.getValue());
                           if (this.inventorySwap.getValue()) {
                              this.doSwap(anchor);
                           } else {
                              this.doSwap(old);
                           }
                        }

                     }
                  }
               }
            }
         }
      }
   }

   public BlockPos getHelper(BlockPos pos) {
      for(Direction i : Direction.values()) {
         if ((!this.checkMine.getValue() || !Lumi.BREAK.isMining(pos.offset(i))) && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite()) && BlockUtil.canPlace(pos.offset(i))) {
            return pos.offset(i);
         }
      }

      return null;
   }

   public static enum Page {
      General,
      Interact,
      Predict,
      Rotate,
      Assist,
      Render;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Interact, Predict, Rotate, Assist, Render};
      }
   }

   public class AnchorRender {
      @EventListener
      public void onRender3D(Render3DEvent event) {
         BlockPos currentPos = AutoAnchor.INSTANCE.currentPos;
         if (currentPos != null) {
            AutoAnchor.this.noPosTimer.reset();
            AutoAnchor.placeVec3d = currentPos.toCenterPos();
         }

         if (AutoAnchor.placeVec3d != null) {
            AutoAnchor.this.fade = AutoAnchor.this.fadeSpeed.getValue() >= (double)1.0F ? (AutoAnchor.this.noPosTimer.passed((long)(AutoAnchor.this.startFadeTime.getValue() * (double)1000.0F)) ? (double)0.0F : (double)0.5F) : AnimateUtil.animate(AutoAnchor.this.fade, AutoAnchor.this.noPosTimer.passed((long)(AutoAnchor.this.startFadeTime.getValue() * (double)1000.0F)) ? (double)0.0F : (double)0.5F, AutoAnchor.this.fadeSpeed.getValue() / (double)10.0F);
            if (AutoAnchor.this.fade == (double)0.0F) {
               AutoAnchor.curVec3d = null;
            } else {
               AutoAnchor.curVec3d = AutoAnchor.curVec3d != null && !(AutoAnchor.this.sliderSpeed.getValue() >= (double)1.0F) ? new Vec3d(AnimateUtil.animate(AutoAnchor.curVec3d.x, AutoAnchor.placeVec3d.x, AutoAnchor.this.sliderSpeed.getValue() / (double)10.0F), AnimateUtil.animate(AutoAnchor.curVec3d.y, AutoAnchor.placeVec3d.y, AutoAnchor.this.sliderSpeed.getValue() / (double)10.0F), AnimateUtil.animate(AutoAnchor.curVec3d.z, AutoAnchor.placeVec3d.z, AutoAnchor.this.sliderSpeed.getValue() / (double)10.0F)) : AutoAnchor.placeVec3d;
               if (AutoAnchor.this.render.getValue()) {
                  Box cbox = new Box(AutoAnchor.curVec3d, AutoAnchor.curVec3d);
                  cbox = AutoAnchor.this.shrink.getValue() ? cbox.expand(AutoAnchor.this.fade) : cbox.expand((double)0.5F);
                  if (AutoAnchor.this.fill.booleanValue) {
                     event.drawFill(cbox, ColorUtil.injectAlpha(AutoAnchor.this.fill.getValue(), (int)((double)AutoAnchor.this.fill.getValue().getAlpha() * AutoAnchor.this.fade * (double)2.0F)));
                  }

                  if (AutoAnchor.this.box.booleanValue) {
                     event.drawBox(cbox, ColorUtil.injectAlpha(AutoAnchor.this.box.getValue(), (int)((double)AutoAnchor.this.box.getValue().getAlpha() * AutoAnchor.this.fade * (double)2.0F)));
                  }
               }

            }
         }
      }
   }
}
