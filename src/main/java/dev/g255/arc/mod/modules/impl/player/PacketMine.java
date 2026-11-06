package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClickBlockEvent;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.RotationEvent;
import dev.g255.arc.api.utils.combat.CombatUtil;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.math.FadeUtils;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.render.Render3DUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.asm.accessors.IPlayerMoveC2SPacket;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.combat.AutoAnchor;
import dev.g255.arc.mod.modules.impl.combat.CevBreaker;
import dev.g255.arc.mod.modules.impl.combat.Criticals;
import dev.g255.arc.mod.modules.impl.exploit.Blink;
import dev.g255.arc.mod.modules.impl.movement.ElytraFly;
import dev.g255.arc.mod.modules.impl.movement.Velocity;
import dev.g255.arc.mod.modules.settings.enums.SwingSide;
import dev.g255.arc.mod.modules.settings.enums.Timing;
import dev.g255.arc.mod.modules.settings.impl.BindSetting;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class PacketMine extends Module {
   public static PacketMine INSTANCE;
   public static BlockPos secondPos;
   public static double progress = (double)0.0F;
   private final FadeUtils animationTime = new FadeUtils(1000L);
   private final FadeUtils secondAnim = new FadeUtils(1000L);
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final EnumSetting<Page> page;
   private final SliderSetting stopDelay;
   private final SliderSetting startDelay;
   private final SliderSetting damage;
   private final SliderSetting maxBreak;
   public final BooleanSetting noGhostHand;
   public final BooleanSetting noCollide;
   private final EnumSetting<Timing> timing;
   private final BooleanSetting grimDisabler;
   private final BooleanSetting instant;
   private final BooleanSetting wait;
   private final BooleanSetting mineAir;
   private final BooleanSetting hotBar;
   private final BooleanSetting doubleBreak;
   public final BooleanSetting autoSwitch;
   private final SliderSetting start;
   private final SliderSetting timeOut;
   private final BooleanSetting setAir;
   private final BooleanSetting swing;
   private final BooleanSetting endSwing;
   public final SliderSetting range;
   private final EnumSetting<SwingSide> swingMode;
   private final BooleanSetting unbreakableCancel;
   private final BooleanSetting switchReset;
   private final BooleanSetting preferWeb;
   private final BooleanSetting preferHead;
   private final BooleanSetting farCancel;
   private final BooleanSetting onlyGround;
   private final BooleanSetting checkWeb;
   private final BooleanSetting checkGround;
   private final BooleanSetting smart;
   private final BooleanSetting usingPause;
   private final BooleanSetting allowOffhand;
   private final BooleanSetting bypassGround;
   private final SliderSetting bypassTime;
   private final BindSetting pause;
   private final BooleanSetting rotate;
   private final BooleanSetting endRotate;
   private final SliderSetting syncTime;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   public final BooleanSetting crystal;
   private final BooleanSetting onlyHeadBomber;
   private final BooleanSetting spamPlace;
   private final BooleanSetting afterBreak;
   private final BooleanSetting checkDamage;
   private final SliderSetting crystalDamage;
   public final BindSetting obsidian;
   private final BindSetting enderChest;
   private final BooleanSetting placeRotate;
   private final BooleanSetting inventory;
   private final SliderSetting placeDelay;
   private final BooleanSetting checkDouble;
   private final EnumSetting<Animation> animation;
   private final EnumSetting<Easing> ease;
   private final EnumSetting<Easing> fadeEase;
   private final SliderSetting expandLine;
   private final ColorSetting startColor;
   private final ColorSetting startOutlineColor;
   private final ColorSetting endColor;
   private final ColorSetting endOutlineColor;
   private final ColorSetting doubleColor;
   private final ColorSetting doubleOutlineColor;
   private final BooleanSetting text;
   private final BooleanSetting box;
   private final BooleanSetting outline;
   private final Timer mineTimer;
   private final Timer sync;
   private final Timer secondTimer;
   private final Timer delayTimer;
   private final Timer placeTimer;
   private final Timer startTime;
   public static boolean ghost = false;
   public static boolean complete = false;
   int lastSlot;
   Vec3d directionVec;
   Runnable switchBack;
   BlockPos breakPos;
   boolean startPacket;
   int breakNumber;
   double breakFinalTime;
   double secondFinalTime;
   boolean sendGroundPacket;
   boolean swapped;
   int mainSlot;

   public PacketMine() {
      super("PacketMine", Module.Category.Player);
      this.page = this.add(new EnumSetting("Page", PacketMine.Page.General));
      this.stopDelay = this.add(new SliderSetting("StopDelay", (double)50.0F, (double)0.0F, (double)500.0F, (double)1.0F, () -> this.page.is(PacketMine.Page.General)));
      this.startDelay = this.add(new SliderSetting("StartDelay", (double)200.0F, (double)0.0F, (double)500.0F, (double)1.0F, () -> this.page.is(PacketMine.Page.General)));
      this.damage = this.add(new SliderSetting("Damage", (double)0.7F, (double)0.0F, (double)2.0F, 0.01, () -> this.page.is(PacketMine.Page.General)));
      this.maxBreak = this.add(new SliderSetting("MaxBreak", (double)3.0F, (double)0.0F, (double)20.0F, (double)1.0F, () -> this.page.is(PacketMine.Page.General)));
      this.noGhostHand = this.add(new BooleanSetting("1.21", false, () -> this.page.is(PacketMine.Page.General)));
      this.noCollide = this.add(new BooleanSetting("NoCollide", true, () -> this.page.is(PacketMine.Page.General)));
      this.timing = this.add(new EnumSetting("Timing", Timing.All, () -> this.page.getValue() == PacketMine.Page.General));
      this.grimDisabler = this.add(new BooleanSetting("GrimDisabler", false, () -> this.page.is(PacketMine.Page.General)));
      this.instant = this.add(new BooleanSetting("Instant", false, () -> this.page.is(PacketMine.Page.General)));
      this.wait = this.add(new BooleanSetting("Wait", true, () -> !this.instant.getValue() && this.page.is(PacketMine.Page.General)));
      this.mineAir = this.add(new BooleanSetting("MineAir", true, () -> this.wait.getValue() && !this.instant.getValue() && this.page.is(PacketMine.Page.General)));
      this.hotBar = this.add(new BooleanSetting("HotbarSwap", false, () -> this.page.is(PacketMine.Page.General)));
      this.doubleBreak = this.add(new BooleanSetting("DoubleBreak", true, () -> this.page.is(PacketMine.Page.General))).setParent();
      this.autoSwitch = this.add(new BooleanSetting("AutoSwitch", true, () -> this.page.is(PacketMine.Page.General) && this.doubleBreak.isOpen()));
      this.start = this.add(new SliderSetting("Start", (double)0.9F, (double)0.0F, (double)2.0F, 0.01, () -> this.page.is(PacketMine.Page.General) && this.doubleBreak.isOpen()));
      this.timeOut = this.add(new SliderSetting("TimeOut", (double)1.2F, (double)0.0F, (double)2.0F, 0.01, () -> this.page.is(PacketMine.Page.General) && this.doubleBreak.isOpen()));
      this.setAir = this.add(new BooleanSetting("SetAir", false, () -> this.page.is(PacketMine.Page.General)));
      this.swing = this.add(new BooleanSetting("Swing", true, () -> this.page.is(PacketMine.Page.General)));
      this.endSwing = this.add(new BooleanSetting("EndSwing", false, () -> this.page.is(PacketMine.Page.General)));
      this.range = this.add(new SliderSetting("Range", (double)6.0F, (double)3.0F, (double)10.0F, 0.1, () -> this.page.is(PacketMine.Page.General)));
      this.swingMode = this.add(new EnumSetting("SwingSide", SwingSide.All, () -> this.page.is(PacketMine.Page.General)));
      this.unbreakableCancel = this.add(new BooleanSetting("UnbreakableCancel", true, () -> this.page.is(PacketMine.Page.Check)));
      this.switchReset = this.add(new BooleanSetting("SwitchReset", false, () -> this.page.is(PacketMine.Page.Check)));
      this.preferWeb = this.add(new BooleanSetting("PreferWeb", true, () -> this.page.is(PacketMine.Page.Check)));
      this.preferHead = this.add(new BooleanSetting("PreferHead", true, () -> this.page.is(PacketMine.Page.Check)));
      this.farCancel = this.add(new BooleanSetting("FarCancel", false, () -> this.page.is(PacketMine.Page.Check)));
      this.onlyGround = this.add(new BooleanSetting("OnlyGround", true, () -> this.page.is(PacketMine.Page.Check)));
      this.checkWeb = this.add(new BooleanSetting("CheckWeb", true, () -> this.page.is(PacketMine.Page.Check)));
      this.checkGround = this.add(new BooleanSetting("CheckGround", true, () -> this.page.is(PacketMine.Page.Check)));
      this.smart = this.add(new BooleanSetting("Smart", true, () -> this.page.is(PacketMine.Page.Check) && this.checkGround.getValue()));
      this.usingPause = this.add((new BooleanSetting("UsingPause", false, () -> this.page.is(PacketMine.Page.Check))).setParent());
      this.allowOffhand = this.add(new BooleanSetting("AllowOffhand", true, () -> this.page.is(PacketMine.Page.Check) && this.usingPause.isOpen()));
      this.bypassGround = this.add(new BooleanSetting("BypassGround", true, () -> this.page.is(PacketMine.Page.Check)));
      this.bypassTime = this.add(new SliderSetting("BypassTime", 400, 0, 2000, () -> this.bypassGround.getValue() && this.page.is(PacketMine.Page.Check)));
      this.pause = this.add(new BindSetting("Pause", -1, () -> this.page.is(PacketMine.Page.Check)));
      this.rotate = this.add(new BooleanSetting("StartRotate", true, () -> this.page.is(PacketMine.Page.Rotation)));
      this.endRotate = this.add(new BooleanSetting("EndRotate", false, () -> this.page.is(PacketMine.Page.Rotation)));
      this.syncTime = this.add(new SliderSetting("Sync", 300, 0, 1000, () -> this.page.is(PacketMine.Page.Rotation)));
      this.yawStep = this.add((new BooleanSetting("YawStep", false, () -> this.page.is(PacketMine.Page.Rotation))).setParent());
      this.whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen()));
      this.steps = this.add(new SliderSetting("Steps", 0.05, (double)0.0F, (double)1.0F, 0.01, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen()));
      this.checkFov = this.add(new BooleanSetting("OnlyLooking", true, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen()));
      this.fov = this.add(new SliderSetting("Fov", (double)20.0F, (double)0.0F, (double)360.0F, 0.1, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen()));
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen()));
      this.crystal = this.add((new BooleanSetting("Crystal", false, () -> this.page.is(PacketMine.Page.Place))).setParent());
      this.onlyHeadBomber = this.add(new BooleanSetting("OnlyCev", true, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen()));
      this.spamPlace = this.add(new BooleanSetting("SpamPlace", false, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen()));
      this.afterBreak = this.add(new BooleanSetting("AfterBreak", true, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen()));
      this.checkDamage = this.add(new BooleanSetting("DetectProgress", true, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen()));
      this.crystalDamage = this.add(new SliderSetting("Progress", (double)0.9F, (double)0.0F, (double)1.0F, 0.01, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen() && this.checkDamage.getValue()));
      this.obsidian = this.add(new BindSetting("Obsidian", -1, () -> this.page.is(PacketMine.Page.Place)));
      this.enderChest = this.add(new BindSetting("EnderChest", -1, () -> this.page.is(PacketMine.Page.Place)));
      this.placeRotate = this.add(new BooleanSetting("PlaceRotate", true, () -> this.page.is(PacketMine.Page.Place)));
      this.inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.is(PacketMine.Page.Place)));
      this.placeDelay = this.add(new SliderSetting("PlaceDelay", 100, 0, 1000, () -> this.page.is(PacketMine.Page.Place)));
      this.checkDouble = this.add(new BooleanSetting("CheckDouble", false, () -> this.page.is(PacketMine.Page.Render)));
      this.animation = this.add(new EnumSetting("Animation", PacketMine.Animation.Up, () -> this.page.is(PacketMine.Page.Render)));
      this.ease = this.add(new EnumSetting("Ease", Easing.CubicInOut, () -> this.page.is(PacketMine.Page.Render)));
      this.fadeEase = this.add(new EnumSetting("FadeEase", Easing.CubicInOut, () -> this.page.is(PacketMine.Page.Render)));
      this.expandLine = this.add(new SliderSetting("ExpandLine", (double)0.0F, (double)0.0F, (double)1.0F, () -> this.page.is(PacketMine.Page.Render)));
      this.startColor = this.add(new ColorSetting("StartFill", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render)));
      this.startOutlineColor = this.add(new ColorSetting("StartOutline", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render)));
      this.endColor = this.add(new ColorSetting("EndFill", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render)));
      this.endOutlineColor = this.add(new ColorSetting("EndOutline", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render)));
      this.doubleColor = this.add(new ColorSetting("DoubleFill", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(PacketMine.Page.Render)));
      this.doubleOutlineColor = this.add(new ColorSetting("DoubleOutline", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(PacketMine.Page.Render)));
      this.text = this.add(new BooleanSetting("Text", true, () -> this.page.is(PacketMine.Page.Render)));
      this.box = this.add(new BooleanSetting("Box", true, () -> this.page.is(PacketMine.Page.Render)));
      this.outline = this.add(new BooleanSetting("Outline", true, () -> this.page.is(PacketMine.Page.Render)));
      this.mineTimer = new Timer();
      this.sync = new Timer();
      this.secondTimer = new Timer();
      this.delayTimer = new Timer();
      this.placeTimer = new Timer();
      this.startTime = new Timer();
      this.lastSlot = -1;
      this.directionVec = null;
      this.startPacket = false;
      this.breakNumber = 0;
      this.sendGroundPacket = false;
      this.swapped = false;
      this.mainSlot = 0;
      this.setChinese("发包挖掘");
      INSTANCE = this;
   }

   public static BlockPos getBreakPos() {
      return INSTANCE.isOn() ? INSTANCE.breakPos : null;
   }

   public String getInfo() {
      return progress >= (double)1.0F ? "Done" : this.df.format(progress * (double)100.0F) + "%";
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null && !this.sync.passedMs(this.syncTime.getValue())) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }

   }

   public void onLogin() {
      this.startPacket = false;
      ghost = false;
      complete = false;
      this.breakPos = null;
      secondPos = null;
   }

   public void onDisable() {
      this.startPacket = false;
      ghost = false;
      complete = false;
      this.breakPos = null;
   }

   private void autoSwitch() {
      if (this.autoSwitch.getValue() && this.doubleBreak.getValue()) {
         int index = -1;
         if (secondPos != null) {
            float CurrentFastest = 1.0F;

            for(int i = 0; i < 9; ++i) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               float destroySpeed;
               float digSpeed;
               if (stack != ItemStack.EMPTY && (digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get(), stack)) + (destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(secondPos))) > CurrentFastest) {
                  CurrentFastest = digSpeed + destroySpeed;
                  index = i;
               }
            }
         }

         if (index != -1 && !mc.options.useKey.isPressed() && !mc.options.attackKey.isPressed() && !mc.player.isUsingItem() && this.secondTimer.passedMs(this.getBreakTime(secondPos, index, this.start.getValue()))) {
            if (index != mc.player.getInventory().selectedSlot) {
               this.mainSlot = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(index);
               this.swapped = true;
            }
         } else if (this.swapped) {
            InventoryUtil.switchToSlot(this.mainSlot);
            this.swapped = false;
         }
      }

   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (this.breakPos != null && mc.world.isAir(this.breakPos)) {
            complete = true;
         }

         if (secondPos != null) {
            int secondSlot = this.getTool(secondPos);
            if (secondSlot == -1) {
               secondSlot = mc.player.getInventory().selectedSlot;
            }

            this.secondFinalTime = this.getBreakTime(secondPos, secondSlot, (double)1.0F);
            if (!this.isAir(secondPos) && !unbreakable(secondPos)) {
               double time = this.getBreakTime(secondPos, mc.player.getInventory().selectedSlot, (double)1.0F);
               if (this.secondTimer.passedMs(time * this.timeOut.getValue())) {
                  secondPos = null;
               }
            } else {
               secondPos = null;
            }
         }

         if (this.switchBack != null && event.isPre()) {
            this.switchBack.run();
            this.switchBack = null;
         }

         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            if (mc.player.isDead()) {
               secondPos = null;
            }

            this.autoSwitch();
            if (mc.player.isCreative()) {
               this.startPacket = false;
               ghost = false;
               complete = false;
               this.breakNumber = 0;
               this.breakPos = null;
               progress = (double)0.0F;
            } else if (this.breakPos == null) {
               this.breakNumber = 0;
               this.startPacket = false;
               ghost = false;
               complete = false;
               progress = (double)0.0F;
            } else {
               int slot = this.getTool(this.breakPos);
               if (slot == -1) {
                  slot = mc.player.getInventory().selectedSlot;
               }

               this.breakFinalTime = this.getBreakTime(this.breakPos, slot);
               progress = (double)this.mineTimer.getMs() / this.breakFinalTime;
               if (this.isAir(this.breakPos)) {
                  this.breakNumber = 0;
               }

               if ((!((double)this.breakNumber > this.maxBreak.getValue() - (double)1.0F) || !(this.maxBreak.getValue() > (double)0.0F) || complete) && (this.wait.getValue() || !this.isAir(this.breakPos) || this.instant.getValue())) {
                  if (unbreakable(this.breakPos)) {
                     if (this.unbreakableCancel.getValue()) {
                        this.breakPos = null;
                        this.startPacket = false;
                        ghost = false;
                        complete = false;
                     }

                     this.breakNumber = 0;
                  } else if ((double)MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(this.breakPos.toCenterPos())) > this.range.getValue()) {
                     if (this.farCancel.getValue()) {
                        this.startPacket = false;
                        ghost = false;
                        complete = false;
                        this.breakNumber = 0;
                        this.breakPos = null;
                     }

                  } else if (!this.usingPause.getValue() || !mc.player.isUsingItem() || this.allowOffhand.getValue() && mc.player.getActiveHand() != Hand.MAIN_HAND) {
                     if (!this.pause.isPressed()) {
                        if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                           if (!this.breakPos.equals(AutoAnchor.INSTANCE.currentPos) || !(BlockUtil.getBlock(getBreakPos()) instanceof RespawnAnchorBlock)) {
                              if (this.hotBar.getValue() || EntityUtil.inInventory()) {
                                 if (this.isAir(this.breakPos)) {
                                    if (this.shouldCrystal()) {
                                       for(Direction facing : Direction.values()) {
                                          CombatUtil.attackCrystal(this.breakPos.offset(facing), this.placeRotate.getValue(), true);
                                       }
                                    }

                                    if (this.placeTimer.passedMs(this.placeDelay.getValue()) && BlockUtil.canPlace(this.breakPos) && mc.currentScreen == null) {
                                       if (this.enderChest.isPressed()) {
                                          int eChest = this.findBlock(Blocks.ENDER_CHEST);
                                          if (eChest != -1) {
                                             int oldSlot = mc.player.getInventory().selectedSlot;
                                             this.doSwap(eChest, eChest);
                                             BlockUtil.placeBlock(this.breakPos, this.placeRotate.getValue(), true);
                                             this.doSwap(oldSlot, eChest);
                                             this.placeTimer.reset();
                                          }
                                       } else {
                                          int obsidian;
                                          if (this.obsidian.isPressed() && (obsidian = this.findBlock(Blocks.OBSIDIAN)) != -1) {
                                             int hasCrystal = 0;
                                             if (this.shouldCrystal()) {
                                                for(Entity entity : BlockUtil.getEntities(new Box(this.breakPos.up()))) {
                                                   if (entity instanceof EndCrystalEntity) {
                                                      hasCrystal = 1;
                                                      break;
                                                   }
                                                }
                                             }

                                             if (hasCrystal == 0 || this.spamPlace.getValue()) {
                                                int oldSlot = mc.player.getInventory().selectedSlot;
                                                this.doSwap(obsidian, obsidian);
                                                BlockUtil.placeBlock(this.breakPos, this.placeRotate.getValue(), true);
                                                this.doSwap(oldSlot, obsidian);
                                                this.placeTimer.reset();
                                             }
                                          }
                                       }
                                    }

                                    this.breakNumber = 0;
                                 } else if (this.canPlaceCrystal(this.breakPos.up()) && this.shouldCrystal()) {
                                    if (this.placeTimer.passedMs(this.placeDelay.getValue())) {
                                       if (this.checkDamage.getValue()) {
                                          if ((double)this.mineTimer.getMs() / this.breakFinalTime >= this.crystalDamage.getValue() && !this.placeCrystal()) {
                                             return;
                                          }
                                       } else if (!this.placeCrystal()) {
                                          return;
                                       }
                                    } else if (this.startPacket) {
                                       return;
                                    }
                                 }

                                 if (this.delayTimer.passed((long)this.stopDelay.getValue())) {
                                    if (this.startPacket) {
                                       if (this.isAir(this.breakPos)) {
                                          return;
                                       }

                                       if (this.onlyGround.getValue() && !mc.player.isOnGround()) {
                                          return;
                                       }

                                       if (this.mineTimer.passed((long)this.breakFinalTime)) {
                                          if (this.endRotate.getValue() && this.shouldYawStep() && !this.faceVector(this.breakPos.toCenterPos().offset(BlockUtil.getClickSide(this.breakPos), (double)0.5F))) {
                                             return;
                                          }

                                          int old = mc.player.getInventory().selectedSlot;
                                          boolean shouldSwitch;
                                          if (this.hotBar.getValue()) {
                                             shouldSwitch = slot != old;
                                          } else {
                                             if (slot < 9) {
                                                slot += 36;
                                             }

                                             shouldSwitch = old + 36 != slot;
                                          }

                                          if (shouldSwitch) {
                                             if (this.hotBar.getValue()) {
                                                InventoryUtil.switchToSlot(slot);
                                             } else {
                                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, old, SlotActionType.SWAP, mc.player);
                                             }
                                          }

                                          final boolean shouldSwitchF = shouldSwitch;
                                          final int slotF = slot;
                                          final int oldF = old;
                                          this.switchBack = () -> {
                                             if (this.endRotate.getValue() && !this.faceVector(this.breakPos.toCenterPos().offset(BlockUtil.getClickSide(this.breakPos), (double)0.5F))) {
                                                if (shouldSwitchF) {
                                                   if (this.hotBar.getValue()) {
                                                      InventoryUtil.switchToSlot(oldF);
                                                   } else {
                                                      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slotF, oldF, SlotActionType.SWAP, mc.player);
                                                      EntityUtil.syncInventory();
                                                   }
                                                }

                                             } else {
                                                sendSequencedPacket((id) -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakPos, BlockUtil.getClickSide(this.breakPos), id));
                                                if (this.endSwing.getValue()) {
                                                   EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                                                }

                                                if (shouldSwitchF) {
                                                   if (this.hotBar.getValue()) {
                                                      InventoryUtil.switchToSlot(oldF);
                                                   } else {
                                                      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slotF, oldF, SlotActionType.SWAP, mc.player);
                                                      EntityUtil.syncInventory();
                                                   }
                                                }

                                                ++this.breakNumber;
                                                this.delayTimer.reset();
                                                this.startTime.reset();
                                                if (this.afterBreak.getValue() && this.shouldCrystal()) {
                                                   for(Direction facing : Direction.values()) {
                                                      CombatUtil.attackCrystal(this.breakPos.offset(facing), this.placeRotate.getValue(), true);
                                                   }
                                                }

                                                if (this.setAir.getValue()) {
                                                   mc.world.setBlockState(this.breakPos, Blocks.AIR.getDefaultState());
                                                }

                                                if (this.endRotate.getValue() && !this.shouldYawStep()) {
                                                   Arc.ROTATION.snapBack();
                                                }

                                                ghost = true;
                                             }
                                          };
                                          if (!this.noGhostHand.getValue()) {
                                             this.switchBack.run();
                                             this.switchBack = null;
                                          }
                                       }
                                    } else {
                                       if (!this.startTime.passed((long)this.startDelay.getValueInt())) {
                                          return;
                                       }

                                       if (!this.mineAir.getValue() && this.isAir(this.breakPos)) {
                                          return;
                                       }

                                       Direction side = BlockUtil.getClickSide(this.breakPos);
                                       if (this.rotate.getValue()) {
                                          Vec3i vec3i = side.getVector();
                                          if (!this.faceVector(this.breakPos.toCenterPos().add(new Vec3d((double)vec3i.getX() * (double)0.5F, (double)vec3i.getY() * (double)0.5F, (double)vec3i.getZ() * (double)0.5F)))) {
                                             return;
                                          }
                                       }

                                       this.mineTimer.reset();
                                       this.animationTime.reset();
                                       if (this.swing.getValue()) {
                                          EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                                       }

                                       if (this.doubleBreak.getValue()) {
                                          if (secondPos == null || this.isAir(secondPos)) {
                                             double breakTime = this.getBreakTime(this.breakPos, slot, (double)1.0F);
                                             this.secondAnim.reset();
                                             this.secondAnim.setLength((long)breakTime);
                                             this.secondTimer.reset();
                                             secondPos = this.breakPos;
                                          }

                                          this.doDoubleBreak(side);
                                       }

                                       sendSequencedPacket((id) -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, side, id));
                                       if (this.rotate.getValue() && !this.shouldYawStep()) {
                                          Arc.ROTATION.snapBack();
                                       }

                                       this.startTime.reset();
                                    }

                                 }
                              }
                           }
                        }
                     }
                  }
               } else {
                  if (this.breakPos.equals(secondPos)) {
                     secondPos = null;
                  }

                  this.startPacket = false;
                  ghost = false;
                  complete = false;
                  this.breakNumber = 0;
                  this.breakPos = null;
               }
            }
         }
      }
   }

   private void breakBlock(BlockPos breakPos) {
      mc.world.getBlockState(breakPos).getBlock().onBreak(mc.world, breakPos, mc.world.getBlockState(breakPos), mc.player);
   }

   void doDoubleBreak(Direction side) {
      sendSequencedPacket((id) -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, side, id));
      sendSequencedPacket((id) -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakPos, side, id));
   }

   boolean placeCrystal() {
      int crystal = this.findCrystal();
      if (crystal != -1) {
         int oldSlot = mc.player.getInventory().selectedSlot;
         this.doSwap(crystal, crystal);
         BlockUtil.placeCrystal(this.breakPos.up(), this.placeRotate.getValue());
         this.doSwap(oldSlot, crystal);
         this.placeTimer.reset();
         return false;
      } else {
         return true;
      }
   }

   @EventListener
   public void onAttackBlock(ClickBlockEvent event) {
      if (!nullCheck()) {
         if (!mc.player.isCreative()) {
            event.cancel();
            BlockPos pos = event.getPos();
            if (!pos.equals(this.breakPos)) {
               if (!unbreakable(pos)) {
                  if (this.breakPos == null || !this.preferWeb.getValue() || BlockUtil.getBlock(this.breakPos) != Blocks.COBWEB) {
                     if (this.breakPos == null || !this.preferHead.getValue() || !mc.player.isCrawling() || !EntityUtil.getPlayerPos(true).up().equals(this.breakPos)) {
                        if (BlockUtil.getClickSideStrict(pos) != null) {
                           if (!((double)MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.range.getValue())) {
                              this.breakPos = pos;
                              this.breakNumber = 0;
                              this.startPacket = false;
                              ghost = false;
                              complete = false;
                              this.mineTimer.reset();
                              this.animationTime.reset();
                              if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                                 Direction side = BlockUtil.getClickSide(this.breakPos);
                                 if (this.rotate.getValue()) {
                                    Vec3i vec3i = side.getVector();
                                    if (!this.faceVector(this.breakPos.toCenterPos().add(new Vec3d((double)vec3i.getX() * (double)0.5F, (double)vec3i.getY() * (double)0.5F, (double)vec3i.getZ() * (double)0.5F)))) {
                                       return;
                                    }
                                 }

                                 if (this.startTime.passed((long)this.startDelay.getValueInt())) {
                                    if (this.swing.getValue()) {
                                       EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                                    }

                                    if (this.doubleBreak.getValue()) {
                                       if (secondPos == null || this.isAir(secondPos)) {
                                          int slot2 = this.getTool(this.breakPos);
                                          if (slot2 == -1) {
                                             slot2 = mc.player.getInventory().selectedSlot;
                                          }

                                          this.secondFinalTime = this.getBreakTime(this.breakPos, slot2, (double)1.0F);
                                          this.secondAnim.reset();
                                          this.secondAnim.setLength((long)this.secondFinalTime);
                                          this.secondTimer.reset();
                                          secondPos = this.breakPos;
                                       }

                                       this.doDoubleBreak(side);
                                    }

                                    int slot;
                                    if ((slot = this.getTool(this.breakPos)) == -1) {
                                       slot = mc.player.getInventory().selectedSlot;
                                    }

                                    this.breakFinalTime = this.getBreakTime(this.breakPos, slot);
                                    sendSequencedPacket((id) -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, side, id));
                                    if (this.rotate.getValue() && !this.shouldYawStep()) {
                                       Arc.ROTATION.snapBack();
                                    }

                                    this.startTime.reset();
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void mine(BlockPos pos) {
      if (!nullCheck()) {
         if (mc.player.isCreative()) {
            mc.interactionManager.attackBlock(pos, BlockUtil.getClickSide(pos));
         } else if (this.isOff()) {
            mc.interactionManager.attackBlock(pos, BlockUtil.getClickSide(pos));
         } else if (!pos.equals(this.breakPos)) {
            if (!unbreakable(pos)) {
               if (this.breakPos == null || !this.preferWeb.getValue() || BlockUtil.getBlock(this.breakPos) != Blocks.COBWEB) {
                  if (this.breakPos == null || !this.preferHead.getValue() || !mc.player.isCrawling() || !EntityUtil.getPlayerPos(true).up().equals(this.breakPos)) {
                     if (BlockUtil.getClickSideStrict(pos) != null) {
                        if (!((double)MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.range.getValue())) {
                           this.breakPos = pos;
                           this.breakNumber = 0;
                           this.startPacket = false;
                           ghost = false;
                           complete = false;
                           this.mineTimer.reset();
                           this.animationTime.reset();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         Arc.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.sync.reset();
         this.directionVec = directionVec;
         if (Arc.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
            return true;
         } else {
            return !this.checkFov.getValue();
         }
      }
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (this.breakPos != null && mc.world.isAir(this.breakPos)) {
         complete = true;
      }

      if (!mc.player.isCreative()) {
         if (secondPos != null) {
            if (this.isAir(secondPos)) {
               secondPos = null;
               return;
            }

            if (!this.checkDouble.getValue() || !secondPos.equals(this.breakPos)) {
               this.secondAnim.setLength((long)this.secondFinalTime);
               double ease = this.secondAnim.ease(this.ease.getValue());
               if (this.box.getValue()) {
                  Render3DUtil.drawFill(matrixStack, this.getFillBox(secondPos, ease), this.doubleColor.getValue());
               }

               if (this.outline.getValue()) {
                  Render3DUtil.drawBox(matrixStack, this.getOutlineBox(secondPos, ease), this.doubleOutlineColor.getValue());
               }
            }
         }

         if (this.breakPos != null) {
            progress = (double)this.mineTimer.getMs() / this.breakFinalTime;
            this.animationTime.setLength((long)this.breakFinalTime);
            double ease = this.animationTime.ease(this.ease.getValue());
            if (unbreakable(this.breakPos)) {
               if (this.box.getValue()) {
                  Render3DUtil.drawFill(matrixStack, new Box(this.breakPos), this.startColor.getValue());
               }

               if (this.outline.getValue()) {
                  Render3DUtil.drawBox(matrixStack, new Box(this.breakPos), this.startOutlineColor.getValue());
               }

               return;
            }

            if (this.box.getValue()) {
               Render3DUtil.drawFill(matrixStack, this.getFillBox(this.breakPos, ease), this.getColor(this.animationTime.ease(this.fadeEase.getValue())));
            }

            if (this.outline.getValue()) {
               Render3DUtil.drawBox(matrixStack, this.getOutlineBox(this.breakPos, ease), this.getOutlineColor(this.animationTime.ease(this.fadeEase.getValue())));
            }

            if (this.text.getValue()) {
               if (this.isAir(this.breakPos)) {
                  Render3DUtil.drawText3D("Waiting", this.breakPos.toCenterPos(), -1);
               } else if ((double)((int)this.mineTimer.getMs()) < this.breakFinalTime) {
                  Render3DUtil.drawText3D(this.df.format(progress * (double)100.0F) + "%", this.breakPos.toCenterPos(), -1);
               } else {
                  Render3DUtil.drawText3D("100.0%", this.breakPos.toCenterPos(), -1);
               }
            }
         } else {
            progress = (double)0.0F;
         }
      } else {
         progress = (double)0.0F;
      }

   }

   private Box getFillBox(BlockPos pos, double ease) {
      switch (((Animation)this.animation.getValue()).ordinal()) {
         case 0:
            ease = ((double)1.0F - ease) / (double)2.0F;
            return (new Box(pos)).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
         case 1:
            ease = ((double)1.0F - ease) / (double)2.0F;
            return (new Box(pos)).shrink(ease, (double)0.0F, ease).shrink(-ease, (double)0.0F, -ease);
         case 2:
            return new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)pos.getY() + ease, (double)(pos.getZ() + 1));
         case 3:
            return new Box((double)pos.getX(), (double)(pos.getY() + 1) - ease, (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1));
         case 4:
            return (new Box(pos)).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
         default:
            return new Box(pos);
      }
   }

   private Box getOutlineBox(BlockPos pos, double ease) {
      ease = Math.min(ease + this.expandLine.getValue(), (double)1.0F);
      switch (((Animation)this.animation.getValue()).ordinal()) {
         case 0:
            ease = ((double)1.0F - ease) / (double)2.0F;
            return (new Box(pos)).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
         case 1:
            ease = ((double)1.0F - ease) / (double)2.0F;
            return (new Box(pos)).shrink(ease, (double)0.0F, ease).shrink(-ease, (double)0.0F, -ease);
         case 2:
            return new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)pos.getY() + ease, (double)(pos.getZ() + 1));
         case 3:
            return new Box((double)pos.getX(), (double)(pos.getY() + 1) - ease, (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1));
         case 4:
            return (new Box(pos)).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
         default:
            return new Box(pos);
      }
   }

   @EventListener(
      priority = -200
   )
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck() && !mc.player.isCreative()) {
         if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (this.bypassGround.getValue() && !mc.player.isFallFlying() && this.breakPos != null && !this.isAir(this.breakPos) && this.bypassTime.getValue() > (double)0.0F && MathHelper.sqrt((float)this.breakPos.toCenterPos().squaredDistanceTo(mc.player.getEyePos())) <= this.range.getValueFloat() + 2.0F) {
               double breakTime = this.breakFinalTime - this.bypassTime.getValue();
               if (breakTime <= (double)0.0F || this.mineTimer.passed((long)breakTime)) {
                  this.sendGroundPacket = true;
                  ((IPlayerMoveC2SPacket)event.getPacket()).setOnGround(true);
               }
            } else {
               this.sendGroundPacket = false;
            }
         } else {
            Packet<?> packet = event.getPacket();
            if (packet instanceof UpdateSelectedSlotC2SPacket) {
               UpdateSelectedSlotC2SPacket packet2 = (UpdateSelectedSlotC2SPacket)packet;
               if (packet2.getSelectedSlot() != this.lastSlot) {
                  this.lastSlot = packet2.getSelectedSlot();
                  if (this.switchReset.getValue()) {
                     this.startPacket = false;
                     ghost = false;
                     complete = false;
                     this.mineTimer.reset();
                     this.animationTime.reset();
                  }
               }
            } else {
               packet = event.getPacket();
               if (packet instanceof PlayerActionC2SPacket) {
                  PlayerActionC2SPacket packet3 = (PlayerActionC2SPacket)packet;
                  if (packet3.getAction() == Action.START_DESTROY_BLOCK) {
                     if (this.breakPos == null || !packet3.getPos().equals(this.breakPos)) {
                        return;
                     }

                     if (this.grimDisabler.getValue()) {
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, packet3.getPos(), packet3.getDirection()));
                     }

                     this.startPacket = true;
                  } else if (packet3.getAction() == Action.STOP_DESTROY_BLOCK) {
                     if (this.breakPos == null || !packet3.getPos().equals(this.breakPos)) {
                        return;
                     }

                     if (!this.instant.getValue()) {
                        this.startPacket = false;
                        ghost = false;
                        complete = false;
                     }
                  }
               }
            }
         }

      }
   }

   boolean canPlaceCrystal(BlockPos pos) {
      BlockPos obsPos = pos.down();
      BlockPos boost = obsPos.up();
      return (BlockUtil.getBlock(obsPos) == Blocks.BEDROCK || BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN) && BlockUtil.getClickSideStrict(obsPos) != null && this.noEntity(boost) && this.noEntity(boost.up()) && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost.up()));
   }

   boolean noEntity(BlockPos pos) {
      for(Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (!(entity instanceof ItemEntity) && (!(entity instanceof ArmorStandEntity) || !AntiCheat.INSTANCE.ignoreArmorStand.getValue())) {
            return false;
         }
      }

      return true;
   }

   void doSwap(int slot, int inv) {
      if (!this.inventory.getValue()) {
         InventoryUtil.switchToSlot(slot);
      } else {
         InventoryUtil.inventorySwap(inv, mc.player.getInventory().selectedSlot);
      }

   }

   int findCrystal() {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : InventoryUtil.findItem(Items.END_CRYSTAL);
   }

   int findBlock(Block block) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(block) : InventoryUtil.findBlock(block);
   }

   boolean shouldCrystal() {
      return this.crystal.getValue() && (!this.onlyHeadBomber.getValue() || this.obsidian.isPressed()) || CevBreaker.INSTANCE.isOn();
   }

   public static double getBreakTime(BlockPos pos) {
      int slot = INSTANCE.getTool(pos);
      if (slot == -1) {
         slot = mc.player.getInventory().selectedSlot;
      }

      return INSTANCE.getBreakTime(pos, slot);
   }

   double getBreakTime(BlockPos pos, int slot) {
      return this.getBreakTime(pos, slot, this.damage.getValue());
   }

   double getBreakTime(BlockPos pos, int slot, double damage) {
      return (double)(1.0F / this.getBlockStrength(pos, mc.player.getInventory().getStack(slot)) / 20.0F * 1000.0F) * damage;
   }

   float getBlockStrength(BlockPos position, ItemStack itemStack) {
      BlockState state = mc.world.getBlockState(position);
      float hardness = state.getHardness(mc.world, position);
      if (hardness < 0.0F) {
         return 0.0F;
      } else {
         float i = state.isToolRequired() && !itemStack.isSuitableFor(state) ? 100.0F : 30.0F;
         return this.getDigSpeed(state, itemStack) / hardness / i;
      }
   }

   float getDigSpeed(BlockState state, ItemStack itemStack) {
      float digSpeed = this.getDestroySpeed(state, itemStack);
      int efficiencyModifier;
      if (digSpeed > 1.0F && (efficiencyModifier = EnchantmentHelper.getLevel((RegistryEntry)mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get(), itemStack)) > 0 && !itemStack.isEmpty()) {
         digSpeed += (float)(StrictMath.pow((double)efficiencyModifier, (double)2.0F) + (double)1.0F);
      }

      if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
         digSpeed *= 1.0F + (float)(mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2F;
      }

      if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
         float var10001;
         switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
            case 0 -> var10001 = 0.3F;
            case 1 -> var10001 = 0.09F;
            case 2 -> var10001 = 0.0027F;
            default -> var10001 = 8.1E-4F;
         }

         digSpeed *= var10001;
      }

      if (mc.player.isSubmergedInWater()) {
         digSpeed *= (float)mc.player.getAttributeInstance(EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED).getValue();
      }

      boolean inWeb = this.checkWeb.getValue() && Arc.PLAYER.isInWeb(mc.player) && mc.world.getBlockState(this.breakPos).getBlock() == Blocks.COBWEB;
      if ((!mc.player.isOnGround() || inWeb) && INSTANCE.checkGround.getValue() && (!this.smart.getValue() || Criticals.INSTANCE.mode.is(Criticals.Mode.Ground) && Criticals.INSTANCE.isOn() || mc.player.isFallFlying() || inWeb)) {
         digSpeed /= 5.0F;
      }

      return digSpeed < 0.0F ? 0.0F : digSpeed;
   }

   float getDestroySpeed(BlockState state, ItemStack itemStack) {
      float destroySpeed = 1.0F;
      if (itemStack != null && !itemStack.isEmpty()) {
         destroySpeed *= itemStack.getMiningSpeedMultiplier(state);
      }

      return destroySpeed;
   }

   Color getColor(double quad) {
      int sR = this.startColor.getValue().getRed();
      int sG = this.startColor.getValue().getGreen();
      int sB = this.startColor.getValue().getBlue();
      int sA = this.startColor.getValue().getAlpha();
      int eR = this.endColor.getValue().getRed();
      int eG = this.endColor.getValue().getGreen();
      int eB = this.endColor.getValue().getBlue();
      int eA = this.endColor.getValue().getAlpha();
      return new Color((int)((double)sR + (double)(eR - sR) * quad), (int)((double)sG + (double)(eG - sG) * quad), (int)((double)sB + (double)(eB - sB) * quad), (int)((double)sA + (double)(eA - sA) * quad));
   }

   Color getOutlineColor(double quad) {
      int sR = this.startOutlineColor.getValue().getRed();
      int sG = this.startOutlineColor.getValue().getGreen();
      int sB = this.startOutlineColor.getValue().getBlue();
      int sA = this.startOutlineColor.getValue().getAlpha();
      int eR = this.endOutlineColor.getValue().getRed();
      int eG = this.endOutlineColor.getValue().getGreen();
      int eB = this.endOutlineColor.getValue().getBlue();
      int eA = this.endOutlineColor.getValue().getAlpha();
      return new Color((int)((double)sR + (double)(eR - sR) * quad), (int)((double)sG + (double)(eG - sG) * quad), (int)((double)sB + (double)(eB - sB) * quad), (int)((double)sA + (double)(eA - sA) * quad));
   }

   int getTool(BlockPos pos) {
      if (this.hotBar.getValue()) {
         int index = -1;
         float CurrentFastest = 1.0F;

         for(int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            float destroySpeed;
            float digSpeed;
            if (stack != ItemStack.EMPTY && (digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get(), stack)) + (destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos))) > CurrentFastest) {
               CurrentFastest = digSpeed + destroySpeed;
               index = i;
            }
         }

         return index;
      } else {
         AtomicInteger slot = new AtomicInteger();
         slot.set(-1);
         float CurrentFastest = 1.0F;

         for(Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            float destroySpeed;
            float digSpeed;
            if (!(((ItemStack)entry.getValue()).getItem() instanceof AirBlockItem) && (digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get(), (ItemStack)entry.getValue())) + (destroySpeed = ((ItemStack)entry.getValue()).getMiningSpeedMultiplier(mc.world.getBlockState(pos))) > CurrentFastest) {
               CurrentFastest = digSpeed + destroySpeed;
               slot.set((Integer)entry.getKey());
            }
         }

         return slot.get();
      }
   }

   private boolean shouldYawStep() {
      if (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())) {
         return this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
      } else {
         return false;
      }
   }

   boolean isAir(BlockPos breakPos) {
      return mc.world.isAir(breakPos) || BlockUtil.getBlock(breakPos) == Blocks.FIRE && BlockUtil.hasCrystal(breakPos);
   }

   public static boolean unbreakable(BlockPos blockPos) {
      Block block = mc.world.getBlockState(blockPos).getBlock();
      return !(block instanceof AirBlock) && (block.getHardness() == -1.0F || block.getHardness() == 100.0F);
   }

   public static enum Page {
      General,
      Check,
      Place,
      Rotation,
      Render;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Check, Place, Rotation, Render};
      }
   }

   private static enum Animation {
      Center,
      Grow,
      Up,
      Down,
      Oscillation,
      None;

      // $FF: synthetic method
      private static Animation[] $values() {
         return new Animation[]{Center, Grow, Up, Down, Oscillation, None};
      }
   }
}
