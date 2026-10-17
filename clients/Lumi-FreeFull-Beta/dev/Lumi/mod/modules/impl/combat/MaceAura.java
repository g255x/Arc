package dev.Lumi.mod.modules.impl.combat;

import dev.Lumi.Lumi;
import dev.Lumi.api.events.eventbus.EventListener;
import dev.Lumi.api.events.impl.ClientTickEvent;
import dev.Lumi.api.events.impl.LookDirectionEvent;
import dev.Lumi.api.events.impl.PacketEvent;
import dev.Lumi.api.events.impl.TimerEvent;
import dev.Lumi.api.utils.combat.CombatUtil;
import dev.Lumi.api.utils.math.MathUtil;
import dev.Lumi.api.utils.math.Timer;
import dev.Lumi.api.utils.player.EntityUtil;
import dev.Lumi.api.utils.player.InventoryUtil;
import dev.Lumi.api.utils.player.MovementUtil;
import dev.Lumi.api.utils.render.Render3DUtil;
import dev.Lumi.api.utils.world.BlockPosX;
import dev.Lumi.asm.accessors.IPlayerMoveC2SPacket;
import dev.Lumi.mod.modules.Module;
import dev.Lumi.mod.modules.impl.movement.ElytraFly;
import dev.Lumi.mod.modules.settings.enums.SwingSide;
import dev.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.Lumi.mod.modules.settings.impl.ColorSetting;
import dev.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.Lumi.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_2824;
import net.minecraft.class_2828;
import net.minecraft.class_2868;
import net.minecraft.class_2879;
import net.minecraft.class_2886;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_4587;
import net.minecraft.class_640;
import net.minecraft.class_239.class_240;
import net.minecraft.class_3959.class_242;
import net.minecraft.class_3959.class_3960;

public class MaceAura extends Module {
   private static final double[] BURROW_OFFSETS = new double[]{0.4199999868869781, 0.7531999805212017, 0.9999957640154541, 1.1661092609382138};
   private static final double RETURN_STEP = (double)0.25F;
   public static MaceAura INSTANCE;
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", (double)5.0F, (double)1.0F, (double)8.0F, 0.1));
   private final SliderSetting attackRange = this.add(new SliderSetting("AttackRange", (double)3.0F, (double)1.0F, (double)8.0F, 0.1));
   private final SliderSetting triggerDistance = this.add(new SliderSetting("TriggerDistance", (double)2.0F, 0.1, (double)20.0F, 0.1));
   private final SliderSetting lagbackDistance = this.add(new SliderSetting("LagbackDistance", (double)5.0F, (double)0.0F, (double)10.0F, 0.1));
   private final SliderSetting spoofHeight = this.add(new SliderSetting("SpoofHeight", (double)25.0F, (double)0.0F, (double)2000.0F, 0.1));
   private final SliderSetting cooldownTicks = this.add(new SliderSetting("CooldownTicks", 3, 0, 20));
   private final BooleanSetting timerEnabled = this.add((new BooleanSetting("Timer", false)).setParent());
   private final SliderSetting timerSpeed;
   private final BooleanSetting weaponOnly;
   private final BooleanSetting silent;
   private final EnumSetting<RenderMode> renderMode;
   private final ColorSetting silentPlayerBox;
   private final BooleanSetting inventorySwap;
   private final BooleanSetting onlyFalling;
   private final BooleanSetting onlyAir;
   private final BooleanSetting autoAttack;
   private final BooleanSetting set;
   private final BooleanSetting setGroundSpoof;
   private final BooleanSetting attackPull;
   private final EnumSetting<LagMode> setLagMode;
   private final SliderSetting setDelay;
   private final SliderSetting setSwitchDelay;
   private final SliderSetting setLagDelay;
   private final SliderSetting setAttackDelay;
   private final SliderSetting setAttackRange;
   private final SliderSetting setLagFly;
   private final SliderSetting setSmartXZ;
   private final SliderSetting setSmartUp;
   private final SliderSetting setSmartDown;
   private final SliderSetting setSmartDistance;
   private final BooleanSetting noWait;
   private final EnumSetting<SwingSide> swingMode;
   private class_1657 target;
   private class_243 fallStart;
   private class_243 lastStablePos;
   private int cooldown;
   private boolean falling;
   private final Timer setLoopTimer;
   private final Timer setStageTimer;
   private int setStage;
   private boolean setHasSwitched;
   private boolean setHasLagged;
   private boolean setAwaitingReturn;
   private boolean sendingReturnPackets;
   private class_1657 setTarget;
   private class_243 silentCameraPos;
   private boolean silentMovementReturnLatched;
   private boolean silentElytraReturnLatched;
   private float silentYaw;
   private float silentPitch;
   private float previousSilentYaw;
   private float previousSilentPitch;

   public MaceAura() {
      super("MaceAura", Module.Category.Combat);
      BooleanSetting var10009 = this.timerEnabled;
      Objects.requireNonNull(var10009);
      this.timerSpeed = this.add((new SliderSetting("TimerSpeed", (double)2.0F, (double)1.0F, (double)100.0F, 0.1, var10009::isOpen)).setSuffix("x"));
      this.weaponOnly = this.add(new BooleanSetting("MaceOnly", true));
      this.silent = this.add(new BooleanSetting("Silent", false));
      this.renderMode = this.add(new EnumSetting("RenderMode", MaceAura.RenderMode.Normal));
      this.silentPlayerBox = this.add(new ColorSetting("SilentPlayerBox", new Color(255, 80, 80, 180), () -> this.renderMode.is(MaceAura.RenderMode.Silent)));
      this.inventorySwap = this.add(new BooleanSetting("InventorySwap", false, () -> this.silent.getValue()));
      this.onlyFalling = this.add(new BooleanSetting("OnlyFalling", true));
      this.onlyAir = this.add(new BooleanSetting("OnlyAir", true));
      this.autoAttack = this.add(new BooleanSetting("AutoAttack", true));
      this.set = this.add((new BooleanSetting("Set", false)).setParent());
      BooleanSetting var10006 = this.set;
      Objects.requireNonNull(var10006);
      this.setGroundSpoof = this.add(new BooleanSetting("GroundSpoof", true, var10006::isOpen));
      var10006 = this.set;
      Objects.requireNonNull(var10006);
      this.attackPull = this.add(new BooleanSetting("AttackPull", false, var10006::isOpen));
      LagMode var10005 = MaceAura.LagMode.Fly;
      var10006 = this.set;
      Objects.requireNonNull(var10006);
      this.setLagMode = this.add(new EnumSetting("LagMode", var10005, var10006::isOpen));
      var10009 = this.set;
      Objects.requireNonNull(var10009);
      this.setDelay = this.add((new SliderSetting("Delay", (double)400.0F, (double)100.0F, (double)3000.0F, (double)10.0F, var10009::isOpen)).setSuffix("ms"));
      var10009 = this.set;
      Objects.requireNonNull(var10009);
      this.setSwitchDelay = this.add((new SliderSetting("DelayUntilSwitch", (double)50.0F, (double)0.0F, (double)1000.0F, (double)10.0F, var10009::isOpen)).setSuffix("ms"));
      var10009 = this.set;
      Objects.requireNonNull(var10009);
      this.setLagDelay = this.add((new SliderSetting("LagDelay", (double)80.0F, (double)0.0F, (double)1000.0F, (double)10.0F, var10009::isOpen)).setSuffix("ms"));
      var10009 = this.set;
      Objects.requireNonNull(var10009);
      this.setAttackDelay = this.add((new SliderSetting("DelayUntilAttack", (double)80.0F, (double)0.0F, (double)1000.0F, (double)10.0F, var10009::isOpen)).setSuffix("ms"));
      var10009 = this.set;
      Objects.requireNonNull(var10009);
      this.setAttackRange = this.add(new SliderSetting("SetRange", (double)5.0F, (double)1.0F, (double)10.0F, 0.1, var10009::isOpen));
      this.setLagFly = this.add(new SliderSetting("LagFly", (double)4.0F, 0.1, (double)10.0F, 0.1, () -> this.set.isOpen() && this.setLagMode.is(MaceAura.LagMode.Fly)));
      this.setSmartXZ = this.add(new SliderSetting("SmartXZ", (double)3.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.set.isOpen() && this.setLagMode.is(MaceAura.LagMode.Smart)));
      this.setSmartUp = this.add(new SliderSetting("SmartUp", (double)3.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.set.isOpen() && this.setLagMode.is(MaceAura.LagMode.Smart)));
      this.setSmartDown = this.add(new SliderSetting("SmartDown", (double)3.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.set.isOpen() && this.setLagMode.is(MaceAura.LagMode.Smart)));
      this.setSmartDistance = this.add(new SliderSetting("SmartDist", (double)2.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.set.isOpen() && this.setLagMode.is(MaceAura.LagMode.Smart)));
      this.noWait = this.add(new BooleanSetting("NoWait", false));
      this.swingMode = this.add(new EnumSetting("Swing", SwingSide.All));
      this.setLoopTimer = new Timer();
      this.setStageTimer = new Timer();
      this.setChinese("风锤光环");
      INSTANCE = this;
   }

   public void onEnable() {
      this.resetSetCycle();
      this.sendingReturnPackets = false;
      this.silentCameraPos = null;
      this.silentMovementReturnLatched = false;
      this.silentElytraReturnLatched = false;
      this.resetSilentRotation();
   }

   public String getInfo() {
      return this.target == null ? null : this.target.method_5477().getString();
   }

   public void onDisable() {
      this.target = null;
      this.fallStart = null;
      this.lastStablePos = null;
      this.cooldown = 0;
      this.falling = false;
      this.resetSetCycle();
      this.sendingReturnPackets = false;
      this.silentCameraPos = null;
      this.silentMovementReturnLatched = false;
      this.silentElytraReturnLatched = false;
   }

   @EventListener
   public void onTimer(TimerEvent event) {
      if (this.timerEnabled.getValue()) {
         if (this.falling) {
            event.set(this.timerSpeed.getValueFloat());
         }
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!event.isPost() && !nullCheck()) {
         if (this.cooldown > 0) {
            --this.cooldown;
         }

         this.handleSilentReturnTriggers();
         this.updateFallTracking();
         if (this.set.getValue()) {
            this.handleSet();
         } else if (this.setStage != 0 || this.setTarget != null || this.setAwaitingReturn) {
            this.resetSetCycle();
         }

         this.target = CombatUtil.getClosestEnemy(this.targetRange.getValue());
         if (this.target != null) {
            if (this.autoAttack.getValue() && this.falling && this.isAttackable(this.target) && this.attackTarget()) {
               if (this.set.getValue() && this.attackPull.getValue()) {
                  this.throwSetWindChargeDown();
               }

               if (this.fallStart != null) {
                  this.performReturn(this.fallStart);
               }

            } else if (!this.weaponOnly.getValue() || this.canUseMace()) {
               if (this.onlyAir.getValue() && mc.field_1724.method_24828()) {
                  this.resetFallState();
               } else if (!mc.field_1724.method_5799() && !mc.field_1724.method_6101() && !mc.field_1724.method_6128()) {
                  if (!this.onlyFalling.getValue() || !(mc.field_1724.method_18798().field_1351 >= (double)0.0F)) {
                     if (this.fallStart != null && !(this.fallStart.field_1351 - mc.field_1724.method_23318() < this.triggerDistance.getValue())) {
                        if (this.cooldown <= 0) {
                           if (this.lagbackDistance.getValue() > (double)0.0F) {
                              double distanceToTarget = (double)mc.field_1724.method_5739(this.target);
                              if (distanceToTarget > this.lagbackDistance.getValue()) {
                                 return;
                              }
                           }

                           if (this.shouldReturnBeforeLanding() && this.doCycle()) {
                              this.cooldown = this.cooldownTicks.getValueInt();
                           }

                        }
                     }
                  }
               } else {
                  this.resetFallState();
               }
            }
         }
      }
   }

   private boolean doCycle() {
      class_243 start = this.fallStart;
      if (start == null) {
         return false;
      } else if (!this.noWait.getValue() && mc.field_1724.method_7261(0.5F) < 1.0F) {
         return false;
      } else {
         this.performReturn(start);
         return true;
      }
   }

   private void performReturn(class_243 start) {
      this.sendingReturnPackets = true;

      try {
         this.sendBurrowReturn(start);
      } finally {
         this.sendingReturnPackets = false;
      }

      mc.field_1724.method_5814(start.field_1352, start.field_1351, start.field_1350);
      mc.field_1724.method_18800(mc.field_1724.method_18798().field_1352, (double)0.0F, mc.field_1724.method_18798().field_1350);
      mc.field_1724.field_6017 = 0.0F;
      this.resetFallState();
      this.cooldown = this.cooldownTicks.getValueInt();
      if (this.set.getValue()) {
         this.resetSetCycle();
      }

   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck() && this.set.getValue() && this.setGroundSpoof.getValue()) {
         class_2596 var3 = event.getPacket();
         if (var3 instanceof class_2828) {
            class_2828 packet = (class_2828)var3;
            if (packet.method_12273()) {
               ((IPlayerMoveC2SPacket)packet).setOnGround(false);
            }
         }

      }
   }

   private void handleSet() {
      if (this.setAwaitingReturn && mc.field_1724.method_24828() && this.setStage == 0) {
         this.setAwaitingReturn = false;
         this.setLoopTimer.reset();
      }

      class_1657 target = this.findSetTarget(this.setAttackRange.getValue());
      if (target == null) {
         this.resetSetState();
      } else if (this.setStage == 0) {
         if (!this.setAwaitingReturn && !this.falling && this.cooldown <= 0 && this.setLoopTimer.passedMs((double)this.setDelay.getValueInt()) && this.throwSetWindChargeDown()) {
            this.setTarget = target;
            this.setAwaitingReturn = true;
            this.setStageTimer.reset();
            this.setHasSwitched = false;
            this.setHasLagged = false;
            this.setStage = 1;
         }

      } else {
         if (!this.setHasSwitched && this.setStageTimer.passedMs((double)this.setSwitchDelay.getValueInt())) {
            int maceSlot = InventoryUtil.findItem(class_1802.field_49814);
            if (maceSlot != -1 && mc.field_1724.method_31548().field_7545 != maceSlot) {
               mc.field_1724.method_31548().field_7545 = maceSlot;
               mc.method_1562().method_52787(new class_2868(maceSlot));
            }

            this.setHasSwitched = true;
         }

         if (!this.setHasLagged && this.setStageTimer.passedMs((double)this.setLagDelay.getValueInt())) {
            this.applySetLagBoost();
            this.setHasLagged = true;
         }

         if (this.setStageTimer.passedMs((double)this.setAttackDelay.getValueInt())) {
            if (this.falling && this.setTarget != null && this.setTarget.method_5805() && (double)mc.field_1724.method_5739(this.setTarget) <= this.setAttackRange.getValue()) {
               mc.method_1562().method_52787(class_2824.method_34206(this.setTarget, mc.field_1724.method_5715()));
               mc.method_1562().method_52787(new class_2879(class_1268.field_5808));
               class_243 start = this.fallStart;
               if (start != null) {
                  this.performReturn(start);
                  return;
               }
            }

            this.resetSetState();
         }

      }
   }

   private void applySetLagBoost() {
      switch (((LagMode)this.setLagMode.getValue()).ordinal()) {
         case 0:
            List<class_2338> candidates = new ArrayList();

            for(double x = mc.field_1724.method_23317() - this.setSmartXZ.getValue(); x < mc.field_1724.method_23317() + this.setSmartXZ.getValue(); ++x) {
               for(double z = mc.field_1724.method_23321() - this.setSmartXZ.getValue(); z < mc.field_1724.method_23321() + this.setSmartXZ.getValue(); ++z) {
                  for(double y = mc.field_1724.method_23318() - this.setSmartDown.getValue(); y < mc.field_1724.method_23318() + this.setSmartUp.getValue(); ++y) {
                     candidates.add(new BlockPosX(x, y, z));
                  }
               }
            }

            double bestDistance = Double.MAX_VALUE;
            class_2338 bestPos = null;

            for(class_2338 pos : candidates) {
               if (this.canSetMove(pos)) {
                  float distance = class_3532.method_15355((float)mc.field_1724.method_5707(pos.method_46558().method_1031((double)0.0F, (double)-0.5F, (double)0.0F)));
                  if (!(distance < (float)this.setSmartDistance.getValue())) {
                     double squaredDistance = mc.field_1724.method_5707(pos.method_46558());
                     if (squaredDistance < bestDistance) {
                        bestDistance = squaredDistance;
                        bestPos = pos;
                     }
                  }
               }
            }

            if (bestPos != null) {
               this.sendSetPosition((double)bestPos.method_10263() + (double)0.5F, (double)bestPos.method_10264(), (double)bestPos.method_10260() + (double)0.5F, false);
            }
            break;
         case 1:
            for(int i = 0; i < 20; ++i) {
               this.sendSetPosition(mc.field_1724.method_23317(), mc.field_1724.method_23318() + (double)1337.0F, mc.field_1724.method_23321(), false);
            }
            break;
         case 2:
            this.sendSetPosition(mc.field_1724.method_23317(), mc.field_1724.method_23318() + 2.3400880035762786, mc.field_1724.method_23321(), false);
            break;
         case 3:
            this.sendSetPosition(mc.field_1724.method_23317(), (double)-70.0F, mc.field_1724.method_23321(), false);
            break;
         case 4:
            this.sendSetPosition(mc.field_1724.method_23317(), (double)-7.0F, mc.field_1724.method_23321(), false);
            break;
         case 5:
            this.sendSetPosition(mc.field_1724.method_23317(), mc.field_1724.method_23318() + 1.9, mc.field_1724.method_23321(), false);
            break;
         case 6:
            mc.method_1562().method_52787(new class_2828.class_2831(-180.0F, -90.0F, false));
            mc.method_1562().method_52787(new class_2828.class_2831(180.0F, 90.0F, false));
            break;
         case 7:
            this.sendSetPosition(mc.field_1724.method_23317(), mc.field_1724.method_23318() + this.setLagFly.getValue(), mc.field_1724.method_23321(), false);
            break;
         case 8:
            this.sendSetPosition(mc.field_1724.method_23317(), mc.field_1724.method_23318() + 1.1001, mc.field_1724.method_23321(), false);
            this.sendSetPosition(mc.field_1724.method_23317(), mc.field_1724.method_23318() + 1.0605, mc.field_1724.method_23321(), false);
            this.sendSetPosition(mc.field_1724.method_23317(), mc.field_1724.method_23318() + 1.0802, mc.field_1724.method_23321(), false);
            this.sendSetPosition(mc.field_1724.method_23317(), mc.field_1724.method_23318() + 1.1127, mc.field_1724.method_23321(), false);
      }

   }

   private boolean throwSetWindChargeDown() {
      boolean fromInventory = false;
      int windChargeSlot;
      if (mc.field_1724.method_6047().method_7909() == class_1802.field_49098) {
         windChargeSlot = mc.field_1724.method_31548().field_7545;
      } else if ((windChargeSlot = InventoryUtil.findItem(class_1802.field_49098)) == -1) {
         if ((windChargeSlot = InventoryUtil.findItemInventorySlotFromZero(class_1802.field_49098)) == -1) {
            return false;
         }

         fromInventory = true;
      }

      float yaw = mc.field_1724.method_36454();
      float pitch = 90.0F;
      Lumi.ROTATION.snapAt(yaw, pitch);
      if (mc.field_1724.method_6047().method_7909() == class_1802.field_49098) {
         mc.method_1562().method_52787(new class_2886(class_1268.field_5808, 0, yaw, pitch));
         mc.field_1724.method_6104(class_1268.field_5808);
      } else if (fromInventory) {
         InventoryUtil.inventorySwap(windChargeSlot, mc.field_1724.method_31548().field_7545);
         mc.method_1562().method_52787(new class_2886(class_1268.field_5808, 0, yaw, pitch));
         mc.field_1724.method_6104(class_1268.field_5808);
         InventoryUtil.inventorySwap(windChargeSlot, mc.field_1724.method_31548().field_7545);
         EntityUtil.syncInventory();
      } else {
         int oldSlot = mc.field_1724.method_31548().field_7545;
         InventoryUtil.switchToSlot(windChargeSlot);
         mc.method_1562().method_52787(new class_2886(class_1268.field_5808, 0, yaw, pitch));
         mc.field_1724.method_6104(class_1268.field_5808);
         InventoryUtil.switchToSlot(oldSlot);
      }

      Lumi.ROTATION.snapBack();
      return true;
   }

   private boolean canSetMove(class_2338 pos) {
      return mc.field_1687.method_22347(pos) && mc.field_1687.method_22347(pos.method_10084());
   }

   private void sendSetPosition(double x, double y, double z, boolean onGround) {
      mc.method_1562().method_52787(new class_2828.class_2829(x, y, z, onGround));
   }

   private void resetSetState() {
      this.setStage = 0;
      this.setHasSwitched = false;
      this.setHasLagged = false;
      this.setTarget = null;
      this.setLoopTimer.reset();
   }

   private void resetSetCycle() {
      this.resetSetState();
      this.setAwaitingReturn = false;
   }

   private class_1657 findSetTarget(double maxRange) {
      class_1657 best = null;
      double bestDistance = Double.MAX_VALUE;

      for(class_1657 player : mc.field_1687.method_18456()) {
         if (player != mc.field_1724 && player.method_5805() && !Lumi.FRIEND.isFriend(player)) {
            double distance = (double)mc.field_1724.method_5739(player);
            if (!(distance > maxRange) && !(distance >= bestDistance)) {
               bestDistance = distance;
               best = player;
            }
         }
      }

      return best;
   }

   private void sendBurrowReturn(class_243 start) {
      double x = mc.field_1724.method_23317();
      double y = mc.field_1724.method_23318();
      double z = mc.field_1724.method_23321();

      for(double offset : BURROW_OFFSETS) {
         this.sendPosition(x, y + offset, z);
      }

      class_243 burrowTop = new class_243(x, y + BURROW_OFFSETS[BURROW_OFFSETS.length - 1], z);
      this.sendHorizontalStepPackets(burrowTop, start);
      class_243 spoofPos = new class_243(start.field_1352, start.field_1351 + this.spoofHeight.getValue(), start.field_1350);
      this.sendTeleportPadding(burrowTop, spoofPos);
      this.sendFullPosition(spoofPos.field_1352, spoofPos.field_1351, spoofPos.field_1350, false);
      this.sendTeleportPadding(spoofPos, start);
      this.sendFullPosition(start.field_1352, start.field_1351, start.field_1350, true);
   }

   private void sendHorizontalStepPackets(class_243 from, class_243 target) {
      double x = from.field_1352;
      double z = from.field_1350;
      double y = from.field_1351;

      while(Math.abs(x - target.field_1352) > (double)0.25F) {
         x += x > target.field_1352 ? (double)-0.25F : (double)0.25F;
         this.sendPosition(x, y, z);
      }

      while(Math.abs(z - target.field_1350) > (double)0.25F) {
         z += z > target.field_1350 ? (double)-0.25F : (double)0.25F;
         this.sendPosition(x, y, z);
      }

      if (x != target.field_1352 || z != target.field_1350) {
         this.sendPosition(target.field_1352, y, target.field_1350);
      }

   }

   private void sendTeleportPadding(class_243 from, class_243 to) {
      int packetsRequired = (int)Math.ceil(from.method_1022(to) / (double)10.0F) - 1;

      for(int i = 0; i < packetsRequired; ++i) {
         mc.method_1562().method_52787(new class_2828.class_5911(true));
      }

   }

   private void sendPosition(double x, double y, double z) {
      mc.method_1562().method_52787(new class_2828.class_2829(x, y, z, false));
   }

   private void sendFullPosition(double x, double y, double z, boolean onGround) {
      mc.method_1562().method_52787(new class_2828.class_2830(x, y, z, mc.field_1724.method_36454(), mc.field_1724.method_36455(), onGround));
   }

   private void updateFallTracking() {
      if (!mc.field_1724.method_5799() && !mc.field_1724.method_6101() && !mc.field_1724.method_6128()) {
         if (!mc.field_1724.method_24828() && !(mc.field_1724.method_18798().field_1351 >= (double)0.0F)) {
            if (!this.falling) {
               this.falling = true;
               this.fallStart = this.lastStablePos != null ? this.lastStablePos : mc.field_1724.method_19538();
               if (this.renderMode.is(MaceAura.RenderMode.Silent)) {
                  this.silentCameraPos = this.fallStart.method_1031((double)0.0F, (double)mc.field_1724.method_18381(mc.field_1724.method_18376()), (double)0.0F);
                  this.resetSilentRotation();
               }
            }

         } else {
            this.lastStablePos = mc.field_1724.method_19538();
            this.falling = false;
         }
      } else {
         this.lastStablePos = mc.field_1724.method_19538();
         this.resetFallState();
      }
   }

   private void handleSilentReturnTriggers() {
      if (!this.renderMode.is(MaceAura.RenderMode.Silent)) {
         this.silentMovementReturnLatched = false;
         this.silentElytraReturnLatched = false;
      } else {
         if (MovementUtil.isMoving()) {
            if (!this.silentMovementReturnLatched) {
               this.silentMovementReturnLatched = true;
               this.returnForSilentTrigger();
            }
         } else {
            this.silentMovementReturnLatched = false;
         }

         boolean elytraFlying = ElytraFly.INSTANCE != null && ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.isFallFlying();
         if (elytraFlying) {
            if (!this.silentElytraReturnLatched) {
               this.silentElytraReturnLatched = true;
               this.returnForSilentTrigger();
            }
         } else {
            this.silentElytraReturnLatched = false;
         }

      }
   }

   private void returnForSilentTrigger() {
      if (this.falling && this.fallStart != null) {
         this.performReturn(this.fallStart);
      }

   }

   public void onRender3D(class_4587 matrixStack) {
      if (this.isSilentRenderActive()) {
         Render3DUtil.drawBox(matrixStack, mc.field_1724.method_5829(), this.silentPlayerBox.getValue());
      }

      this.previousSilentYaw = this.silentYaw;
      this.previousSilentPitch = this.silentPitch;
   }

   @EventListener
   public void onLookDirection(LookDirectionEvent event) {
      if (this.isSilentRenderActive()) {
         this.silentYaw += (float)event.getCursorDeltaX() * 0.15F;
         this.silentPitch = class_3532.method_15363(this.silentPitch + (float)event.getCursorDeltaY() * 0.15F, -90.0F, 90.0F);
         event.cancel();
      }
   }

   public boolean isSilentRenderActive() {
      return this.isOn() && this.renderMode.is(MaceAura.RenderMode.Silent) && this.falling && this.silentCameraPos != null && this.hasHorizontalSilentCameraTarget();
   }

   public boolean shouldSuppressNoFall() {
      return this.isOn() && this.falling && this.target != null && CombatUtil.isValid(this.target);
   }

   private boolean isHorizontallyAttackable(class_1297 entity) {
      if (!CombatUtil.isValid(entity)) {
         return false;
      } else {
         class_243 hitVec = this.getAttackVec(entity);
         double x = mc.field_1724.method_23317() - hitVec.field_1352;
         double z = mc.field_1724.method_23321() - hitVec.field_1350;
         return x * x + z * z <= this.attackRange.getValue() * this.attackRange.getValue();
      }
   }

   private boolean hasHorizontalSilentCameraTarget() {
      for(class_1657 player : mc.field_1687.method_18456()) {
         if (this.isHorizontallyAttackable(player)) {
            return true;
         }
      }

      return false;
   }

   public class_243 getSilentCameraPos() {
      return this.silentCameraPos;
   }

   public float getSilentYaw() {
      return class_3532.method_16439(mc.method_60646().method_60637(true), this.previousSilentYaw, this.silentYaw);
   }

   public float getSilentPitch() {
      return class_3532.method_16439(mc.method_60646().method_60637(true), this.previousSilentPitch, this.silentPitch);
   }

   private void resetSilentRotation() {
      if (mc.field_1724 != null) {
         this.silentYaw = mc.field_1724.method_36454();
         this.silentPitch = mc.field_1724.method_36455();
         this.previousSilentYaw = this.silentYaw;
         this.previousSilentPitch = this.silentPitch;
      }
   }

   private boolean canUseMace() {
      if (mc.field_1724.method_6047().method_7909() == class_1802.field_49814) {
         return true;
      } else if (!this.silent.getValue()) {
         return false;
      } else {
         return this.getMaceSlot() != -1;
      }
   }

   private boolean isAttackable(class_1297 entity) {
      if (!CombatUtil.isValid(entity)) {
         return false;
      } else {
         class_243 hitVec = this.getAttackVec(entity);
         return mc.field_1724.method_33571().method_1022(hitVec) <= this.attackRange.getValue();
      }
   }

   private class_243 getAttackVec(class_1297 entity) {
      return MathUtil.getClosestPointToBox(mc.field_1724.method_33571(), entity.method_5829());
   }

   private boolean attackTarget() {
      int oldSlot = mc.field_1724.method_31548().field_7545;
      int maceSlot = this.getMaceSlot();
      boolean swapped = false;
      class_243 hitVec = this.getAttackVec(this.target);
      if (mc.field_1724.method_6047().method_7909() != class_1802.field_49814) {
         if (!this.silent.getValue() || maceSlot == -1) {
            return false;
         }

         this.doSwap(maceSlot);
         swapped = true;
      }

      Lumi.ROTATION.lookAt(hitVec);
      mc.method_1562().method_52787(class_2824.method_34206(this.target, mc.field_1724.method_5715()));
      mc.field_1724.method_7350();
      EntityUtil.swingHand(class_1268.field_5808, this.swingMode.getValue());
      Lumi.ROTATION.snapBack();
      if (swapped) {
         if (this.inventorySwap.getValue()) {
            this.doSwap(maceSlot);
            EntityUtil.syncInventory();
         } else {
            InventoryUtil.switchToSlot(oldSlot);
         }
      }

      return true;
   }

   private boolean shouldReturnBeforeLanding() {
      long timeToGround = this.getTimeToGroundMs();
      if (timeToGround == Long.MAX_VALUE) {
         return false;
      } else {
         return timeToGround <= 42L + (long)this.getPingMs();
      }
   }

   private long getTimeToGroundMs() {
      if (mc.field_1724.method_18798().field_1351 >= (double)0.0F) {
         return Long.MAX_VALUE;
      } else {
         class_243 from = mc.field_1724.method_19538().method_1031((double)0.0F, 0.01, (double)0.0F);
         class_239 hit = mc.field_1687.method_17742(new class_3959(from, from.method_1023((double)0.0F, (double)256.0F, (double)0.0F), class_3960.field_17558, class_242.field_1348, mc.field_1724));
         if (hit.method_17783() == class_240.field_1333) {
            return Long.MAX_VALUE;
         } else {
            double remainingDistance = mc.field_1724.method_23318() - hit.method_17784().field_1351;
            if (remainingDistance <= (double)0.0F) {
               return 0L;
            } else {
               double velocityY = mc.field_1724.method_18798().field_1351;
               double fallen = (double)0.0F;
               double millisecondsPerTick = this.timerEnabled.getValue() ? (double)50.0F / this.timerSpeed.getValue() : (double)50.0F;

               for(int tick = 1; tick <= 200; ++tick) {
                  velocityY = (velocityY - 0.08) * 0.98;
                  fallen -= velocityY;
                  if (fallen >= remainingDistance) {
                     return (long)Math.ceil((double)tick * millisecondsPerTick);
                  }
               }

               return Long.MAX_VALUE;
            }
         }
      }
   }

   private int getPingMs() {
      if (mc.method_1562() != null && mc.field_1724 != null) {
         class_640 entry = mc.method_1562().method_2871(mc.field_1724.method_5667());
         return entry == null ? 0 : entry.method_2959();
      } else {
         return 0;
      }
   }

   private void doSwap(int slot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.field_1724.method_31548().field_7545);
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private int getMaceSlot() {
      return this.inventorySwap.getValue() ? InventoryUtil.findItemInventorySlot(class_1802.field_49814) : InventoryUtil.findItem(class_1802.field_49814);
   }

   private void resetFallState() {
      this.fallStart = null;
      this.falling = false;
      this.silentCameraPos = null;
   }

   public static enum LagMode {
      Smart,
      Invalid,
      TrollHack,
      ToVoid,
      ToVoid2,
      Normal,
      Rotation,
      Fly,
      Glide;

      // $FF: synthetic method
      private static LagMode[] $values() {
         return new LagMode[]{Smart, Invalid, TrollHack, ToVoid, ToVoid2, Normal, Rotation, Fly, Glide};
      }
   }

   public static enum RenderMode {
      Normal,
      Silent;

      // $FF: synthetic method
      private static RenderMode[] $values() {
         return new RenderMode[]{Normal, Silent};
      }
   }
}
