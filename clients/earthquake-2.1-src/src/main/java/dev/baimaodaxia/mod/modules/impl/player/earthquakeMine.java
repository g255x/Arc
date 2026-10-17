package dev.baimaodaxia.mod.modules.impl.player;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.ClickBlockEvent;
import dev.baimaodaxia.api.events.impl.Render3DEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.api.utils.render.Render3DUtil;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class earthquakeMine extends Module {
   public static earthquakeMine INSTANCE;
   private final EnumSetting<Page> page;
   public final BooleanSetting usingPause;
   private final BooleanSetting onlyMain;
   private final EnumSetting<SwitchMode> switchMode;
   private final SliderSetting range;
   private final SliderSetting maxBreaks;
   private final BooleanSetting farCancel;
   private final BooleanSetting swing;
   private final BooleanSetting instantMine;
   private final SliderSetting instantDelay;
   private final BooleanSetting fastBypass;
   private final BooleanSetting doubleBreak;
   private final BooleanSetting checkGround;
   private final BooleanSetting bypassGround;
   private final BooleanSetting clientRemove;
   private final SliderSetting switchDamage;
   private final SliderSetting switchTime;
   private final SliderSetting mineDelay;
   private final SliderSetting packetDelay;
   private final SliderSetting mineDamage;
   private final EnumSetting<RenderMode> renderMode;
   private final BooleanSetting fading;
   private final SliderSetting renderTime;
   private final SliderSetting fadeTime;
   private final ColorSetting fadeSideColor;
   private final ColorSetting fadeLineColor;
   private final ColorSetting sideStartColor;
   private final ColorSetting sideEndColor;
   private final ColorSetting lineStartColor;
   private final ColorSetting lineEndColor;
   private final ColorSetting secondSideStartColor;
   private final ColorSetting secondSideEndColor;
   private final ColorSetting secondLineStartColor;
   private final ColorSetting secondLineEndColor;
   public static BlockPos selfClickPos = null;
   public static int maxBreaksCount;
   public static int mainProgressPercent = 0;
   public static int secondProgressPercent = 0;
   public static boolean completed = false;
   public static BlockPos targetPos;
   public static BlockPos secondPos;
   private static float progress;
   private static float secondProgress;
   private long lastTime;
   private long secondLastTime;
   private long fadeLastTime;
   private static boolean started;
   private static boolean secondStarted;
   private BlockPos renderPos;
   private BlockPos secondRenderPos;
   private double renderProgress;
   private double secondRenderProgress;
   private int oldSlot;
   private final Timer bypassTimer;
   private final Timer timer;
   private final Timer secondTimer;
   public final Timer mineTimer;
   private final Timer instantTimer;
   private boolean hasSwitch;
   private boolean secondHasSwitch;

   public earthquakeMine() {
      super("earthquakeMine", Module.Category.Player);
      this.page = this.add(new EnumSetting("Page", earthquakeMine.Page.General));
      this.usingPause = this.add(new BooleanSetting("Pause On Use", true, () -> this.page.is(earthquakeMine.Page.General)));
      this.onlyMain = this.add(new BooleanSetting("Only Main", true, () -> this.page.is(earthquakeMine.Page.General) && this.usingPause.getValue()));
      this.switchMode = this.add(new EnumSetting("Switch Mode", earthquakeMine.SwitchMode.Silent, () -> this.page.is(earthquakeMine.Page.General)));
      this.range = this.add(new SliderSetting("Range", (double)6.0F, (double)0.0F, (double)12.0F, 0.1, () -> this.page.is(earthquakeMine.Page.General)));
      this.maxBreaks = this.add(new SliderSetting("Try Break Time", (double)6.0F, (double)0.0F, (double)10.0F, (double)1.0F, () -> this.page.is(earthquakeMine.Page.General)));
      this.farCancel = this.add(new BooleanSetting("Far Cancel", true, () -> this.page.is(earthquakeMine.Page.General)));
      this.swing = this.add(new BooleanSetting("Swing Hand", true, () -> this.page.is(earthquakeMine.Page.General)));
      this.instantMine = this.add(new BooleanSetting("Instant Mine", true, () -> this.page.is(earthquakeMine.Page.General)));
      this.instantDelay = this.add(new SliderSetting("Instant Delay", (double)10.0F, (double)0.0F, (double)1000.0F, (double)10.0F, () -> this.page.is(earthquakeMine.Page.General)));
      this.fastBypass = this.add(new BooleanSetting("Fast Bypass", true, () -> this.page.is(earthquakeMine.Page.General)));
      this.doubleBreak = this.add(new BooleanSetting("Double Break", false, () -> this.page.is(earthquakeMine.Page.General)));
      this.checkGround = this.add(new BooleanSetting("Check Ground", true, () -> this.page.is(earthquakeMine.Page.General)));
      this.bypassGround = this.add(new BooleanSetting("Bypass Ground", false, () -> this.page.is(earthquakeMine.Page.General)));
      this.clientRemove = this.add(new BooleanSetting("Client Remove", true, () -> this.page.is(earthquakeMine.Page.General)));
      this.switchDamage = this.add(new SliderSetting("Switch Damage", (double)95.0F, (double)0.0F, (double)100.0F, (double)1.0F, () -> this.page.is(earthquakeMine.Page.General)));
      this.switchTime = this.add(new SliderSetting("Switch Time", (double)100.0F, (double)0.0F, (double)1000.0F, (double)10.0F, () -> this.page.is(earthquakeMine.Page.General)));
      this.mineDelay = this.add(new SliderSetting("Mine Delay", (double)300.0F, (double)0.0F, (double)1000.0F, (double)10.0F, () -> this.page.is(earthquakeMine.Page.General)));
      this.packetDelay = this.add(new SliderSetting("Packet Delay", (double)200.0F, (double)0.0F, (double)1000.0F, (double)10.0F, () -> this.page.is(earthquakeMine.Page.General)));
      this.mineDamage = this.add(new SliderSetting("Damage", 0.8, (double)0.0F, (double)2.0F, 0.05, () -> this.page.is(earthquakeMine.Page.General)));
      this.renderMode = this.add(new EnumSetting("Render Mode", earthquakeMine.RenderMode.Shrink, () -> this.page.is(earthquakeMine.Page.Render)));
      this.fading = this.add(new BooleanSetting("Fading", true, () -> this.page.is(earthquakeMine.Page.Render)));
      this.renderTime = this.add(new SliderSetting("Render Time", 0.1, (double)0.0F, (double)5.0F, 0.1, () -> this.page.is(earthquakeMine.Page.Render) && this.fading.getValue()));
      this.fadeTime = this.add(new SliderSetting("Fade Time", 0.2, (double)0.0F, (double)5.0F, 0.1, () -> this.page.is(earthquakeMine.Page.Render) && this.fading.getValue()));
      this.fadeSideColor = this.add(new ColorSetting("Fade Side Color", new Color(70, 200, 155, 31), () -> this.page.is(earthquakeMine.Page.Render)));
      this.fadeLineColor = this.add(new ColorSetting("Fade Line Color", new Color(70, 200, 155, 233), () -> this.page.is(earthquakeMine.Page.Render)));
      this.sideStartColor = this.add(new ColorSetting("Side Start", new Color(255, 0, 0, 31), () -> this.page.is(earthquakeMine.Page.Render)));
      this.sideEndColor = this.add(new ColorSetting("Side End", new Color(0, 150, 10, 31), () -> this.page.is(earthquakeMine.Page.Render)));
      this.lineStartColor = this.add(new ColorSetting("Line Start", new Color(255, 0, 0, 233), () -> this.page.is(earthquakeMine.Page.Render)));
      this.lineEndColor = this.add(new ColorSetting("Line End", new Color(5, 160, 0, 233), () -> this.page.is(earthquakeMine.Page.Render)));
      this.secondSideStartColor = this.add(new ColorSetting("Second Side Start", new Color(255, 0, 0, 31), () -> this.page.is(earthquakeMine.Page.Render)));
      this.secondSideEndColor = this.add(new ColorSetting("Second Side End", new Color(0, 150, 10, 31), () -> this.page.is(earthquakeMine.Page.Render)));
      this.secondLineStartColor = this.add(new ColorSetting("Second Line Start", new Color(255, 0, 0, 233), () -> this.page.is(earthquakeMine.Page.Render)));
      this.secondLineEndColor = this.add(new ColorSetting("Second Line End", new Color(5, 160, 0, 233), () -> this.page.is(earthquakeMine.Page.Render)));
      this.oldSlot = -1;
      this.bypassTimer = new Timer();
      this.timer = new Timer();
      this.secondTimer = new Timer();
      this.mineTimer = new Timer();
      this.instantTimer = new Timer();
      this.hasSwitch = false;
      this.secondHasSwitch = false;
      this.setChinese("鬼矿");
      INSTANCE = this;
   }

   public void onEnable() {
      maxBreaksCount = 0;
      this.hasSwitch = false;
      this.secondHasSwitch = false;
      this.bypassTimer.setMs(917813L);
      this.mineTimer.setMs(917813L);
      this.instantTimer.setMs(917813L);
      this.timer.setMs(917813L);
      this.secondTimer.setMs(917813L);
      targetPos = null;
      secondPos = null;
      started = false;
      secondStarted = false;
      mainProgressPercent = 0;
      secondProgressPercent = 0;
      progress = 0.0F;
      secondProgress = 0.0F;
      this.lastTime = System.currentTimeMillis();
      this.secondLastTime = System.currentTimeMillis();
      this.fadeLastTime = System.currentTimeMillis();
      this.renderPos = null;
      this.secondRenderPos = null;
      this.renderProgress = (double)0.0F;
      this.secondRenderProgress = (double)0.0F;
   }

   public void onDisable() {
      if (this.hasSwitch) {
         InventoryUtil.switchToSlot(this.oldSlot);
         this.hasSwitch = false;
      }

      if (this.secondHasSwitch) {
         InventoryUtil.switchToSlot(this.oldSlot);
         this.secondHasSwitch = false;
      }

   }

   @EventListener
   private void onStartBreakingBlock(ClickBlockEvent event) {
      if (!nullCheck()) {
         if (this.canBreak(event.getPos())) {
            event.cancel();
            if (this.mineTimer.passedMs(this.mineDelay.getValue())) {
               selfClickPos = event.getPos();
               this.mine(event.getPos());
            }
         }
      }
   }

   public boolean isInstantMining(BlockPos pos) {
      if (this.isOn() && this.instantMine.getValue() && pos != null) {
         if (completed && targetPos != null && targetPos.equals(pos)) {
            BlockState state = mc.world.getBlockState(pos);
            return !state.isAir() && !state.isReplaceable();
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void mine(BlockPos pos) {
      this.mineTimer.reset();
      maxBreaksCount = 0;
      if (this.doubleBreak.getValue()) {
         if (targetPos != null && secondPos == null && !targetPos.equals(pos)) {
            if (completed) {
               targetPos = pos;
               secondStarted = false;
               secondProgress = 0.0F;
               secondProgressPercent = 0;
               mainProgressPercent = 0;
               started = false;
               progress = 0.0F;
               completed = false;
            } else {
               secondPos = targetPos;
               targetPos = pos;
               secondStarted = false;
               secondProgress = 0.0F;
               secondProgressPercent = 0;
               started = false;
            }
         } else if (targetPos == null || !targetPos.equals(pos)) {
            mainProgressPercent = 0;
            targetPos = pos;
            started = false;
            progress = 0.0F;
            completed = false;
         }
      } else if (!pos.equals(targetPos)) {
         mainProgressPercent = 0;
         targetPos = pos;
         started = false;
         progress = 0.0F;
         completed = false;
      }

   }

   @EventListener
   private void onRender(Render3DEvent event) {
      if (!nullCheck()) {
         long now = System.currentTimeMillis();
         double fadeDelta = (double)(now - this.fadeLastTime) / (double)1000.0F;
         this.fadeLastTime = now;
         if (targetPos == null && secondPos == null) {
            selfClickPos = null;
         }

         if (mainProgressPercent >= 100 && !this.instantMine.getValue()) {
            targetPos = null;
         }

         if (secondProgressPercent >= 100) {
            secondPos = null;
         }

         if (this.timer.passedMs(this.switchTime.getValue()) && this.hasSwitch && this.switchMode.getValue() != earthquakeMine.SwitchMode.None) {
            if (this.switchMode.is(earthquakeMine.SwitchMode.Delay)) {
               InventoryUtil.switchToSlot(this.oldSlot);
            } else if (this.switchMode.is(earthquakeMine.SwitchMode.Silent)) {
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.oldSlot));
            }

            this.hasSwitch = false;
         }

         if ((double)maxBreaksCount >= this.maxBreaks.getValue() * (double)10.0F) {
            maxBreaksCount = 0;
            targetPos = null;
         }

         if (targetPos != null) {
            this.renderPos = targetPos;
         }

         if (secondPos != null) {
            this.secondRenderPos = secondPos;
         }

         this.updateFadeProgress(fadeDelta);
         this.renderFadeBoxes(event.matrixStack);
         if (secondPos != null && this.doubleBreak.getValue()) {
            if (this.farCancel.getValue() && Math.sqrt(mc.player.getEyePos().squaredDistanceTo(secondPos.toCenterPos())) > this.range.getValue()) {
               secondPos = null;
               return;
            }

            double secondMax = (double)this.getMineTicksSecond(this.getTool(secondPos));
            double secondDelta = (double)(System.currentTimeMillis() - this.secondLastTime) / (double)1000.0F;
            secondProgressPercent = (int)((double)secondProgress / (secondMax * this.mineDamage.getValue()) * (double)100.0F);
            this.secondLastTime = System.currentTimeMillis();
            if (!secondStarted) {
               this.sendStart(secondPos);
               secondStarted = true;
               secondProgress = 0.0F;
               return;
            }

            Double secondDamage = this.mineDamage.getValue();
            if (this.checkGround.getValue() && !mc.player.isOnGround()) {
               if (this.checkGround.getValue() && !mc.player.isOnGround()) {
                  secondProgress += (float)(secondDelta * (double)4.0F);
               }
            } else {
               secondProgress += (float)(secondDelta * (double)20.0F);
            }

            this.secondBlockRender(event.matrixStack);
            if ((double)secondProgress >= secondMax * secondDamage) {
               this.sendStopSecond();
            }
         }

         if (this.doubleBreak.getValue() && (!this.usingPause.getValue() || !this.checkPause(this.onlyMain.getValue())) && ((double)secondProgressPercent >= this.switchDamage.getValue() || (double)mainProgressPercent >= this.switchDamage.getValue()) && !this.hasSwitch && secondPos != null) {
            int bestSlot = this.getTool(secondPos);
            if (!this.hasSwitch) {
               this.oldSlot = mc.player.getInventory().selectedSlot;
            }

            if (!this.switchMode.is(earthquakeMine.SwitchMode.None) && bestSlot != -1) {
               if (this.switchMode.is(earthquakeMine.SwitchMode.Delay)) {
                  InventoryUtil.switchToSlot(bestSlot);
               } else if (this.switchMode.is(earthquakeMine.SwitchMode.Silent)) {
                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
               }

               this.timer.reset();
               this.hasSwitch = true;
            }
         }

         if (targetPos != null) {
            if (this.farCancel.getValue() && Math.sqrt(mc.player.getEyePos().squaredDistanceTo(targetPos.toCenterPos())) > this.range.getValue()) {
               targetPos = null;
               return;
            }

            double max = (double)this.getMineTicks(this.getTool(targetPos));
            mainProgressPercent = (int)((double)progress / (max * this.mineDamage.getValue()) * (double)100.0F);
            if ((double)progress >= max * this.mineDamage.getValue() && completed) {
               if (this.isAir(targetPos) || mc.world.getBlockState(targetPos).isReplaceable()) {
                  maxBreaksCount = 0;
               }

               if (!this.isAir(targetPos) && !mc.world.getBlockState(targetPos).isReplaceable() && (!this.usingPause.getValue() || !this.checkPause(this.onlyMain.getValue()))) {
                  ++maxBreaksCount;
               }
            }

            if (this.instantMine.getValue() && completed) {
               Color color1 = (double)progress >= 0.95 ? this.sideEndColor.getValue() : this.sideStartColor.getValue();
               Color color2 = (double)progress >= 0.95 ? this.lineEndColor.getValue() : this.lineStartColor.getValue();
               Render3DUtil.drawFill(event.matrixStack, new Box(targetPos), color1);
               Render3DUtil.drawBox(event.matrixStack, new Box(targetPos), color2);
               if (!mc.world.getBlockState(targetPos).isAir() && !mc.world.getBlockState(targetPos).isReplaceable() && this.instantTimer.passedMs(this.instantDelay.getValue())) {
                  this.sendStop();
                  this.instantTimer.reset();
               }

               return;
            }

            double delta = (double)(System.currentTimeMillis() - this.lastTime) / (double)1000.0F;
            this.lastTime = System.currentTimeMillis();
            if (!started) {
               this.sendStart(targetPos);
               return;
            }

            Double damage = this.mineDamage.getValue();
            if (this.checkGround.getValue() && !mc.player.isOnGround()) {
               if (this.checkGround.getValue() && !mc.player.isOnGround()) {
                  progress += (float)(delta * (double)4.0F);
               }
            } else {
               progress += (float)(delta * (double)20.0F);
            }

            this.mainBlockRender(event.matrixStack);
            if ((double)progress >= max * damage) {
               this.sendStop();
               completed = true;
               if (!this.instantMine.getValue() && secondPos == null) {
                  targetPos = null;
               }
            }
         }

      }
   }

   private void sendStart(BlockPos pos) {
      mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, BlockUtil.getClickSide(pos)));
      if (this.fastBypass.getValue()) {
         BlockPos bypassPos = BlockPos.ofFloored(mc.player.getX(), (double)321.0F, mc.player.getZ());
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, bypassPos, Direction.DOWN));
      }

      if (this.doubleBreak.getValue()) {
         (new Thread(() -> {
            try {
               Thread.sleep((long)this.packetDelay.getValue());
               mc.execute(() -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, BlockUtil.getClickSide(pos))));
            } catch (InterruptedException e) {
               e.printStackTrace();
            }

         })).start();
      }

      mc.player.swingHand(Hand.MAIN_HAND);
      if (pos.equals(targetPos)) {
         started = true;
         progress = 0.0F;
      } else {
         secondStarted = true;
         secondProgress = 0.0F;
      }

   }

   private void sendStop() {
      if (!this.usingPause.getValue() || !this.checkPause(this.onlyMain.getValue())) {
         if (!this.doubleBreak.getValue() || secondPos == null) {
            int bestSlot = this.getTool(targetPos);
            if (!this.hasSwitch) {
               this.oldSlot = mc.player.getInventory().selectedSlot;
            }

            if (this.switchMode.getValue() != earthquakeMine.SwitchMode.None && bestSlot != -1) {
               if (this.switchMode.is(earthquakeMine.SwitchMode.Delay)) {
                  InventoryUtil.switchToSlot(bestSlot);
               }

               if (this.switchMode.is(earthquakeMine.SwitchMode.Silent)) {
                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
               }

               this.timer.reset();
               this.hasSwitch = true;
            }
         }

         if (this.bypassGround.getValue() && !mc.player.isFallFlying() && targetPos != null && !this.isAir(targetPos) && !mc.player.isOnGround()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0E-9, mc.player.getZ(), true));
         }

         if (this.swing.getValue()) {
            mc.player.swingHand(Hand.MAIN_HAND);
         }

         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, targetPos, BlockUtil.getClickSide(targetPos)));
         if (this.clientRemove.getValue() && targetPos != null && !this.isAir(targetPos)) {
            mc.interactionManager.attackBlock(targetPos, BlockUtil.getClickSide(targetPos));
         }

      }
   }

   private void sendStopSecond() {
      if (this.bypassGround.getValue() && !mc.player.isFallFlying() && secondPos != null && !this.isAir(secondPos) && !mc.player.isOnGround()) {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0E-9, mc.player.getZ(), true));
      }

      if (this.swing.getValue()) {
         mc.player.swingHand(Hand.MAIN_HAND);
      }

      if (this.clientRemove.getValue() && secondPos != null && !this.isAir(secondPos)) {
         mc.interactionManager.attackBlock(secondPos, BlockUtil.getClickSide(secondPos));
      }

   }

   private float getMineTicks(int slot) {
      if (targetPos == null) {
         return 20.0F;
      } else {
         BlockState state = mc.world.getBlockState(targetPos);
         float hardness = state.getHardness(mc.world, targetPos);
         if (hardness < 0.0F) {
            return Float.MAX_VALUE;
         } else if (hardness == 0.0F) {
            return 1.0F;
         } else {
            ItemStack stack = slot == -1 ? ItemStack.EMPTY : mc.player.getInventory().getStack(slot);
            boolean canHarvest = stack.isSuitableFor(state);
            float speed = stack.getMiningSpeedMultiplier(state);
            int efficiency = this.getEnchantmentLevel(stack);
            if (efficiency > 0 && speed > 1.0F) {
               speed += (float)(efficiency * efficiency + 1);
            }

            if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
               int amp = mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier();
               speed *= 1.0F + (float)(amp + 1) * 0.2F;
            }

            if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
               int amp = mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier();
               float var10001;
               switch (amp) {
                  case 0 -> var10001 = 0.3F;
                  case 1 -> var10001 = 0.09F;
                  case 2 -> var10001 = 0.0027F;
                  default -> var10001 = 8.1E-4F;
               }

               speed *= var10001;
            }

            float damage = speed / hardness / (canHarvest ? 30.0F : 100.0F);
            return damage <= 0.0F ? Float.MAX_VALUE : 1.0F / damage;
         }
      }
   }

   private float getMineTicksSecond(int slot) {
      if (secondPos == null) {
         return 20.0F;
      } else {
         BlockState state = mc.world.getBlockState(secondPos);
         float hardness = state.getHardness(mc.world, secondPos);
         if (hardness < 0.0F) {
            return Float.MAX_VALUE;
         } else if (hardness == 0.0F) {
            return 1.0F;
         } else {
            ItemStack stack = slot == -1 ? ItemStack.EMPTY : mc.player.getInventory().getStack(slot);
            boolean canHarvest = stack.isSuitableFor(state);
            float speed = stack.getMiningSpeedMultiplier(state);
            int efficiency = this.getEnchantmentLevel(stack);
            if (efficiency > 0 && speed > 1.0F) {
               speed += (float)(efficiency * efficiency + 1);
            }

            if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
               int amp = mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier();
               speed *= 1.0F + (float)(amp + 1) * 0.2F;
            }

            if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
               int amp = mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier();
               float var10001;
               switch (amp) {
                  case 0 -> var10001 = 0.3F;
                  case 1 -> var10001 = 0.09F;
                  case 2 -> var10001 = 0.0027F;
                  default -> var10001 = 8.1E-4F;
               }

               speed *= var10001;
            }

            float damage = speed / hardness / (canHarvest ? 30.0F : 100.0F);
            return damage <= 0.0F ? Float.MAX_VALUE : 1.0F / damage;
         }
      }
   }

   private int getEnchantmentLevel(ItemStack stack) {
      if (mc.world != null && !stack.isEmpty()) {
         RegistryEntry<Enchantment> efficiency = mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).orElse(null);
         return efficiency == null ? 0 : EnchantmentHelper.getLevel(efficiency, stack);
      } else {
         return 0;
      }
   }

   private boolean isAir(BlockPos breakPos) {
      return mc.world.getBlockState(breakPos).isAir() || mc.world.getBlockState(breakPos).getBlock() == Blocks.FIRE && this.hasCrystal(breakPos);
   }

   private boolean isSolidForFade(BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         return !this.isAir(pos) && !state.isReplaceable();
      }
   }

   private boolean hasCrystal(BlockPos pos) {
      for(Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (entity instanceof EndCrystalEntity endCrystal) {
            if (endCrystal.isAlive()) {
               return true;
            }
         }
      }

      return false;
   }

   private int getTool(BlockPos pos) {
      int index = -1;
      float CurrentFastest = 1.0F;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack != ItemStack.EMPTY) {
            float digSpeed = (float)this.getEnchantmentLevel(stack);
            float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));
            if (digSpeed + destroySpeed > CurrentFastest) {
               CurrentFastest = digSpeed + destroySpeed;
               index = i;
            }
         }
      }

      return index;
   }

   private boolean checkPause(boolean onlyMain) {
      return mc.options.useKey.isPressed() && (!onlyMain || mc.player.getActiveHand() == Hand.MAIN_HAND);
   }

   private boolean canBreak(BlockPos blockPos) {
      BlockState state = mc.world.getBlockState(blockPos);
      if (!mc.player.isCreative() && state.getHardness(mc.world, blockPos) < 0.0F) {
         return false;
      } else {
         return !state.getCollisionShape(mc.world, blockPos).isEmpty();
      }
   }

   private void updateFadeProgress(double delta) {
      if (!this.fading.getValue()) {
         this.renderProgress = (double)0.0F;
         this.secondRenderProgress = (double)0.0F;
      } else {
         boolean paused = this.usingPause.getValue() && this.checkPause(this.onlyMain.getValue());
         if (this.isSolidForFade(this.renderPos) && !paused) {
            this.renderProgress = this.fadeTime.getValue() + this.renderTime.getValue();
         } else {
            this.renderProgress = Math.max((double)0.0F, this.renderProgress - delta);
         }

         if (this.isSolidForFade(this.secondRenderPos) && !paused) {
            this.secondRenderProgress = this.fadeTime.getValue() + this.renderTime.getValue();
         } else {
            this.secondRenderProgress = Math.max((double)0.0F, this.secondRenderProgress - delta);
         }

      }
   }

   private void renderFadeBoxes(MatrixStack stack) {
      if (this.fading.getValue()) {
         this.renderFadeBox(stack, this.renderPos, this.renderProgress);
         this.renderFadeBox(stack, this.secondRenderPos, this.secondRenderProgress);
      }

   }

   private void renderFadeBox(MatrixStack stack, BlockPos pos, double progress) {
      if (pos != null && !(progress <= (double)0.0F) && !this.isSolidForFade(pos)) {
         Color color1 = new Color(this.fadeSideColor.getValue().getRed(), this.fadeSideColor.getValue().getGreen(), this.fadeSideColor.getValue().getBlue(), (int)Math.round((double)this.fadeSideColor.getValue().getAlpha() * Math.min((double)1.0F, progress / this.fadeTime.getValue())));
         Color color2 = new Color(this.fadeLineColor.getValue().getRed(), this.fadeLineColor.getValue().getGreen(), this.fadeLineColor.getValue().getBlue(), (int)Math.round((double)this.fadeLineColor.getValue().getAlpha() * Math.min((double)1.0F, progress / this.fadeTime.getValue())));
         Render3DUtil.drawFill(stack, new Box(pos), color1);
         Render3DUtil.drawBox(stack, new Box(pos), color2);
      }
   }

   private void mainBlockRender(MatrixStack stack) {
      if (targetPos != null) {
         double max = (double)this.getMineTicks(this.getTool(targetPos));
         double rawProgress = MathHelper.clamp((double)progress / (max * this.mineDamage.getValue()), (double)0.0F, (double)1.0F);
         Color color1 = rawProgress >= 0.95 ? this.sideEndColor.getValue() : this.sideStartColor.getValue();
         Color color2 = rawProgress >= 0.95 ? this.lineEndColor.getValue() : this.lineStartColor.getValue();
         switch (((RenderMode)this.renderMode.getValue()).ordinal()) {
            case 0:
               Render3DUtil.drawFill(stack, new Box(targetPos), color1);
               Render3DUtil.drawBox(stack, new Box(targetPos), color2);
               break;
            case 1:
               Box box = new Box(targetPos.toCenterPos().add(-rawProgress / (double)2.0F, -rawProgress / (double)2.0F, -rawProgress / (double)2.0F), targetPos.toCenterPos().add(rawProgress / (double)2.0F, rawProgress / (double)2.0F, rawProgress / (double)2.0F));
               Render3DUtil.drawFill(stack, box, color1);
               Render3DUtil.drawBox(stack, box, color2);
               break;
            case 2:
               Box box2 = (new Box(targetPos)).withMaxY((double)targetPos.getY() + rawProgress);
               Render3DUtil.drawFill(stack, box2, color1);
               Render3DUtil.drawBox(stack, box2, color2);
               break;
            case 3:
               double shrink = (double)1.0F - rawProgress;
               Box box3 = (new Box(targetPos)).shrink(shrink, shrink, shrink);
               Render3DUtil.drawFill(stack, box3, color1);
               Render3DUtil.drawBox(stack, box3, color2);
         }

      }
   }

   private void secondBlockRender(MatrixStack stack) {
      if (secondPos != null) {
         double max = (double)this.getMineTicksSecond(this.getTool(secondPos));
         double rawProgress = MathHelper.clamp((double)secondProgress / (max * this.mineDamage.getValue()), (double)0.0F, (double)1.0F);
         Color color1 = rawProgress >= 0.95 ? this.secondSideEndColor.getValue() : this.secondSideStartColor.getValue();
         Color color2 = rawProgress >= 0.95 ? this.secondLineEndColor.getValue() : this.secondLineStartColor.getValue();
         switch (((RenderMode)this.renderMode.getValue()).ordinal()) {
            case 0:
               Render3DUtil.drawFill(stack, new Box(secondPos), color1);
               Render3DUtil.drawBox(stack, new Box(secondPos), color2);
               break;
            case 1:
               Box box = new Box(secondPos.toCenterPos().add(-rawProgress / (double)2.0F, -rawProgress / (double)2.0F, -rawProgress / (double)2.0F), secondPos.toCenterPos().add(rawProgress / (double)2.0F, rawProgress / (double)2.0F, rawProgress / (double)2.0F));
               Render3DUtil.drawFill(stack, box, color1);
               Render3DUtil.drawBox(stack, box, color2);
               break;
            case 2:
               Box box2 = (new Box(secondPos)).withMaxY((double)secondPos.getY() + rawProgress);
               Render3DUtil.drawFill(stack, box2, color1);
               Render3DUtil.drawBox(stack, box2, color2);
               break;
            case 3:
               double shrink = (double)1.0F - rawProgress;
               Box box3 = (new Box(secondPos)).shrink(shrink, shrink, shrink);
               Render3DUtil.drawFill(stack, box3, color1);
               Render3DUtil.drawBox(stack, box3, color2);
         }

      }
   }

   public String getInfo() {
      return targetPos == null ? "None" : mainProgressPercent + "%";
   }

   private static enum Page {
      General,
      Render;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Render};
      }
   }

   private static enum SwitchMode {
      None,
      Delay,
      Silent;

      // $FF: synthetic method
      private static SwitchMode[] $values() {
         return new SwitchMode[]{None, Delay, Silent};
      }
   }

   private static enum RenderMode {
      Box,
      Normal,
      Grow,
      Shrink;

      // $FF: synthetic method
      private static RenderMode[] $values() {
         return new RenderMode[]{Box, Normal, Grow, Shrink};
      }
   }
}
