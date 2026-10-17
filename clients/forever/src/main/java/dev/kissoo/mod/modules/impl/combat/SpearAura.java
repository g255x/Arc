package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.MoveEvent;
import dev.kissoo.api.events.impl.Render3DEvent;
import dev.kissoo.api.events.impl.UpdateRotateEvent;
import dev.kissoo.api.utils.combat.CombatUtil;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.mod.modules.settings.enums.SwingSide;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.api.utils.render.Render3DUtil;
import dev.kissoo.api.utils.world.CombatBlockUtil;
import dev.kissoo.api.utils.world.MaceHelper;
import dev.kissoo.core.impl.RotationManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.movement.ElytraFlight;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.spearfix.SpearFix;
import hoprc.api.interfaces.SpearStabPacketMarker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.Color;
import java.util.Comparator;

public class SpearAura extends Module {

    public enum Page        { General, Movement, Rotate, Render }
    public enum TargetMode  { DISTANCE, HEALTH }
    public enum AttackMode  { Charged, Normal }
    public enum RotateMode  { Silent, Client }
    public enum FlyMode     { Pass, Shuttle }
    private enum State      { IDLE, MOVING_AWAY, CHARGING, ATTACKING, RETURNING, SHUTTLE }
    private enum Side       { In, Out }

    private static final int   FALLBACK_DELAY_TICKS = 13;
    private static final int   FALLBACK_MAX_TICKS   = 250;
    private static final float FALLBACK_MIN_REL     = 4.6F;
    private static final float FALLBACK_MIN_REACH   = 2.0F;
    private static final float FALLBACK_MAX_REACH   = 4.5F;

    public static SpearAura INSTANCE;
    public static Entity target;

    public final EnumSetting<Page>       page          = this.add(new EnumSetting<>("Page",         Page.General));
    public final SliderSetting           range         = this.add(new SliderSetting("Range",         4.0,  0.1, 7.0,  0.1, () -> page.is(Page.General)));
    public final SliderSetting           targetRange   = this.add(new SliderSetting("TargetRange",   6.0,  0.1, 14.0, 0.1, () -> page.is(Page.General)));
    public final EnumSetting<AttackMode> attackMode    = this.add(new EnumSetting<>("AttackMode",    AttackMode.Charged, () -> page.is(Page.General)));
    public final BooleanSetting          holdCharge    = this.add(new BooleanSetting("HoldCharge",   true,  () -> page.is(Page.General) && attackMode.is(AttackMode.Charged)));
    public final SliderSetting           extraCharge   = this.add(new SliderSetting("ExtraCharge",   2.0,  0.0, 20.0, 1.0, () -> page.is(Page.General) && attackMode.is(AttackMode.Charged)));
    public final BooleanSetting          maceSwap      = this.add(new BooleanSetting("MaceSwap",     false, () -> page.is(Page.General) && attackMode.is(AttackMode.Normal)));
    public final BooleanSetting          maceInventory = this.add(new BooleanSetting("MaceInventory",false, () -> page.is(Page.General) && attackMode.is(AttackMode.Normal) && maceSwap.getValue()));
    public final SliderSetting           hurtTime      = this.add(new SliderSetting("HurtTime",      10.0, 0.0, 10.0, 1.0, () -> page.is(Page.General)));
    public final BooleanSetting          whileEating   = this.add(new BooleanSetting("WhileUsing",   false, () -> page.is(Page.General)));
    public final BooleanSetting          playersOnly   = this.add(new BooleanSetting("PlayersOnly",  true,  () -> page.is(Page.General)));
    public final EnumSetting<TargetMode> targetFilter  = this.add(new EnumSetting<>("TargetFilter",  TargetMode.DISTANCE, () -> page.is(Page.General)));

    public final SliderSetting  chargeDistance = this.add(new SliderSetting("ChargeDistance", 6.0, 1.0, 15.0, 0.5, () -> page.is(Page.Movement)));
    public final BooleanSetting straightLine   = this.add(new BooleanSetting("StraightLine",   true, () -> page.is(Page.Movement)));
    public final SliderSetting  lineDot        = this.add(new SliderSetting("LineDot",        0.65,0.0, 1.0, 0.05,() -> page.is(Page.Movement) && straightLine.getValue()));
    public final BooleanSetting autoAlign      = this.add(new BooleanSetting("AutoAlign",      true, () -> page.is(Page.Movement) && straightLine.getValue()));
    public final BooleanSetting returnAfter    = this.add(new BooleanSetting("ReturnAfter",    true, () -> page.is(Page.Movement)));
    public final SliderSetting  moveSpeed      = this.add(new SliderSetting("MoveSpeed",      0.28,0.05,1.5, 0.01,() -> page.is(Page.Movement)));
    public final SliderSetting  chargeSpeed    = this.add(new SliderSetting("ChargeSpeed",    0.55,0.1, 2.0, 0.01,() -> page.is(Page.Movement)));
    public final SliderSetting  reachMargin    = this.add(new SliderSetting("ReachMargin",    0.35,0.0, 2.0, 0.05,() -> page.is(Page.Movement)));
    public final BooleanSetting elytra         = this.add(new BooleanSetting("ElytraFly",      true, () -> page.is(Page.Movement)));
    public final SliderSetting  climbSpeed     = this.add(new SliderSetting("ClimbSpeed",     0.35,0.05,1.5, 0.05,() -> page.is(Page.Movement) && elytra.getValue()));
    public final SliderSetting  diveHeight     = this.add(new SliderSetting("DiveHeight",     1.5, 0.0, 8.0, 0.5, () -> page.is(Page.Movement) && elytra.getValue()));
    public final SliderSetting  maxYGap        = this.add(new SliderSetting("MaxYGap",        12.0,1.0, 32.0,1.0, () -> page.is(Page.Movement)));
    public final EnumSetting<FlyMode> flyMode  = this.add(new EnumSetting<>("FlyMode",         FlyMode.Pass, () -> page.is(Page.Movement) && elytra.getValue()));
    public final SliderSetting  launchDistance = this.add(new SliderSetting("LaunchDistance", 8.0, 3.0, 40.0,0.5, () -> page.is(Page.Movement) && elytra.getValue() && flyMode.is(FlyMode.Shuttle)));
    public final SliderSetting  launchRadius   = this.add(new SliderSetting("LaunchRadius",   1.5, 0.5, 6.0, 0.5, () -> page.is(Page.Movement) && elytra.getValue() && flyMode.is(FlyMode.Shuttle)));
    public final BooleanSetting followTarget   = this.add(new BooleanSetting("FollowTarget",   true, () -> page.is(Page.Movement) && elytra.getValue() && flyMode.is(FlyMode.Shuttle)));
    public final BooleanSetting alignSpeed     = this.add(new BooleanSetting("AlignSpeed",    true, () -> page.is(Page.Movement)));
    public final SliderSetting  speedMargin    = this.add(new SliderSetting("SpeedMargin",   1.35,1.0, 3.0, 0.05,() -> page.is(Page.Movement) && alignSpeed.getValue()));
    public final BooleanSetting avoidBlocks    = this.add(new BooleanSetting("AvoidBlocks",    true, () -> page.is(Page.Movement)));
    public final SliderSetting  avoidDistance  = this.add(new SliderSetting("AvoidDistance",  4.0, 1.0, 12.0,0.5, () -> page.is(Page.Movement) && avoidBlocks.getValue()));
    public final SliderSetting  avoidAngle     = this.add(new SliderSetting("AvoidAngle",     60.0,15.0,90.0,5.0, () -> page.is(Page.Movement) && avoidBlocks.getValue()));

    public final BooleanSetting          rotate         = this.add(new BooleanSetting("Rotate",          true, () -> page.is(Page.Rotate)));
    public final EnumSetting<RotateMode> rotateMode     = this.add(new EnumSetting<>("RotateMode",    RotateMode.Silent, () -> page.is(Page.Rotate) && rotate.getValue()));
    public final BooleanSetting          rotateMoveTarget = this.add(new BooleanSetting("RotateMovement",  true, () -> page.is(Page.Rotate) && rotate.getValue()));
    public final BooleanSetting          aimTarget        = this.add(new BooleanSetting("AimTarget",       true, () -> page.is(Page.Rotate) && rotate.getValue()));
    public final SliderSetting           priority         = this.add(new SliderSetting("Priority",        50.0, 0.0, 100.0, 1.0, () -> page.is(Page.Rotate) && rotate.getValue()));

    public final BooleanSetting targetEsp  = this.add(new BooleanSetting("TargetESP",    true, () -> page.is(Page.Render)));
    public final ColorSetting   espFill    = this.add(new ColorSetting("ESPFill",     new Color(80, 160, 255, 50),  () -> page.is(Page.Render) && targetEsp.getValue()));
    public final ColorSetting   espOutline = this.add(new ColorSetting("ESPOutline",  new Color(80, 160, 255, 200), () -> page.is(Page.Render) && targetEsp.getValue()));

    private static final float AIM_TOLERANCE = 2.0F;
    private static final int MIN_LEG_TICKS = 4;
    private static final float[] AVOID_STEPS = {12.0F, 24.0F, 36.0F, 48.0F, 60.0F, 75.0F, 90.0F};
    private static final double AVOID_SPEED_LOOKAHEAD = 6.0;
    private static final double AVOID_HALF_WIDTH = 0.45;

    private State state         = State.IDLE;
    private Vec3d moveAwayGoal  = null;
    private Vec3d returnGoal    = null;
    private Vec3d chargeAxis    = null;
    private Vec3d passDirection = null;
    private int   chargeTick    = -1;
    private int   chargeSlot    = -1;
    private int   chargeHeldSlot = -1;
    private Vec3d shuttleAxis  = null;
    private Vec3d shuttleGoal  = null;
    private Side  shuttleSide  = Side.In;
    private int   legStartTick = -1;
    private boolean crossed    = false;
    private boolean forwardHeld = false;
    private float aimYaw = 0.0F;
    private boolean aimYawSet = false;
    private boolean selfCharging = false;

    public SpearAura() {
        super("SpearAura", "Charges enemies with a spear, holding the charge through the pass", Module.Category.Combat);
        this.setChinese("长矛光环");
        INSTANCE = this;
    }

    @Override public void onEnable()  { resetState(); }
    @Override public void onDisable() { resetState(); }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck() || event.isPost()) return;
        if (!whileEating.getValue() && isUsingForeignItem()) {
            if (state != State.IDLE) resetState();
            return;
        }
        if (!hasSpear()) { resetState(); return; }

        switch (state) {
            case IDLE        -> tickIdle();
            case MOVING_AWAY -> tickMovingAway();
            case CHARGING    -> tickCharging();
            case ATTACKING   -> tickAttacking();
            case RETURNING   -> tickReturning();
            case SHUTTLE     -> tickShuttle();
        }
    }

    // --- IDLE ---
    private void tickIdle() {
        target = findTarget(targetRange.getValue());
        if (target == null) {
            releaseRotate();
            return;
        }

        if (attackMode.is(AttackMode.Normal)) {
            if (rotate.getValue() && aimTarget.getValue() && isInAimRange(target)) {
                aimAt(getAttackVec(target));
            }
            if (canAttack(target) && cooldownReady()) state = State.ATTACKING;
            return;
        }

        returnGoal = mc.player.getPos();

        if (shuttleMode()) { beginShuttle(); return; }

        Vec3d toTarget = horizontalDir(mc.player.getPos(), target.getPos());
        if (toTarget.lengthSquared() < 1.0E-6) toTarget = playerFacingHorizontal();
        chargeAxis = toTarget;
        moveAwayGoal = retreatGoal(toTarget);
        state = State.MOVING_AWAY;
    }

    // --- MOVING_AWAY ---
    private void tickMovingAway() {
        if (!isTargetValid(target)) { finishOrReturn(); return; }
        if (moveAwayGoal == null)   { resetState(); return; }

        beginCharge();

        if (straightLine.getValue() && autoAlign.getValue() && chargeAxis != null) {
            moveAwayGoal = retreatGoal(chargeAxis);
        }

        boolean atHeight = !flying() || Math.abs(mc.player.getPos().y - moveAwayGoal.y) < 1.0;
        if (hDist(mc.player.getPos(), moveAwayGoal) < 0.5 && atHeight && chargeArmed()) {
            passDirection = horizontalDir(mc.player.getPos(), target.getPos());
            if (passDirection.lengthSquared() < 1.0E-6) passDirection = playerFacingHorizontal();
            state = State.CHARGING;
            return;
        }

        applyHorizontalVelocity(moveAwayGoal, moveSpeed.getValue());
        if (rotate.getValue()) aimAt(getAttackVec(target));
    }

    // --- CHARGING ---
    private void tickCharging() {
        if (!isTargetValid(target)) { finishOrReturn(); return; }
        if (!selfCharging)          { state = State.MOVING_AWAY; return; }

        if (chargeExpired()) { startNextPass(); return; }

        beginCharge();

        Vec3d attackVec = getAttackVec(target);
        if (rotate.getValue()) aimAt(attackVec);

        double gap = eyeDist(target);
        double breakOff = minReach() + reachMargin.getValue();

        if (gap <= breakOff) {
            Vec3d through = passDirection != null ? passDirection : playerFacingHorizontal();
            if (alignSpeed.getValue() && flying()) {
                Vec3d vel = mc.player.getVelocity();
                if (Math.abs(vel.y) > 0.1) {
                    through = new Vec3d(through.x, vel.y / chargeSpeed.getValue(), through.z);
                }
            }
            Vec3d exit = mc.player.getPos().add(through);
            applyHorizontalVelocity(exit, chargeSpeed.getValue());
            mc.player.setSprinting(true);
            if (gap > breakOff * 0.5 && !isInFront()) startNextPass();
            return;
        }

        if (gap > maxReach()) {
            if (!chargeApproach()) applyHorizontalVelocity(chargeGoal(), chargeSpeed.getValue());
            mc.player.setSprinting(true);
            return;
        }

        if (straightLine.getValue()) {
            Vec3d toTarget = horizontalDir(mc.player.getPos(), target.getPos());
            if (toTarget.dotProduct(playerFacingHorizontal()) < lineDot.getValue()) return;
        }

        if (!chargeApproach()) applyHorizontalVelocity(chargeGoal(), chargeSpeed.getValue());
        mc.player.setSprinting(true);
    }

    private boolean chargeApproach() {
        if (!alignSpeed.getValue() || !flying()) return false;
        return applyAimedVelocity(target, chargeSpeed.getValue());
    }

    private Vec3d chargeGoal() {
        if (target == null) return mc.player.getPos();
        if (!flying()) return target.getPos();
        Vec3d aim = getAttackVec(target);
        return new Vec3d(aim.x, aim.y - mc.player.getEyeHeight(mc.player.getPose()), aim.z);
    }

    // --- SHUTTLE ---
    private boolean shuttleMode() {
        return elytra.getValue() && flyMode.is(FlyMode.Shuttle) && flying();
    }

    private void beginShuttle() {
        Vec3d axis = horizontalDir(target.getPos(), mc.player.getPos());
        shuttleAxis = axis.lengthSquared() < 1.0E-6 ? playerFacingHorizontal() : axis;
        shuttleSide = Side.In;
        shuttleGoal = launchPoint(shuttleSide);
        crossed = false;
        legStartTick = mc.player.age;
        state = State.SHUTTLE;
    }

    private Vec3d launchPoint(Side side) {
        Vec3d base = target.getPos();
        Vec3d offset = shuttleAxis.multiply(legDistance(side));
        return new Vec3d(base.x + offset.x, base.y + passHeight(), base.z + offset.z);
    }

    private double legDistance(Side side) {
        double inDist = Math.max(minReach() + reachMargin.getValue(),
                (minReach() + Math.min(range.getValue(), maxReach())) * 0.5);
        return side == Side.Out ? Math.max(launchDistance.getValue(), inDist + 2.0) : inDist;
    }

    private void tickShuttle() {
        if (!isTargetValid(target))            { holdForward(false); finishOrReturn(); return; }
        if (!shuttleMode())                    { resetState();       return; }
        if (shuttleGoal == null || shuttleAxis == null) { resetState(); return; }

        beginCharge();

        if (followTarget.getValue()) {
            shuttleGoal = launchPoint(shuttleSide);
        }

        if (rotate.getValue()) {
            aimAt(getAttackVec(target));
        }

        double gap = eyeDist(target);

        if (shuttleSide == Side.In && alignSpeed.getValue() && gap <= maxReach() + 2.0) {
            applyAimedVelocity(target, chargeSpeed.getValue());
        }
        if (gap <= maxReach() && gap >= minReach()) crossed = true;

        double inDist = legDistance(Side.In);
        double outDist = legDistance(Side.Out);
        double turnTolerance = Math.min(launchRadius.getValue(), (outDist - inDist) / 3.0);
        double hGap = hDist(mc.player.getPos(), target.getPos());
        boolean arrived = shuttleSide == Side.Out
                ? hGap >= outDist - turnTolerance
                : hGap <= inDist;
        if (shuttleSide == Side.In) {
            if (crossed || arrived || chargeExpired() || gap < minReach()) {
                shuttleSide = Side.Out;
                shuttleGoal = launchPoint(shuttleSide);
                legStartTick = mc.player.age;
            }
        } else if (arrived && mc.player.age - legStartTick >= MIN_LEG_TICKS) {
            if (chargeArmed()) {
                shuttleSide = Side.In;
                shuttleGoal = launchPoint(shuttleSide);
                crossed = false;
                legStartTick = mc.player.age;
            } else {
                holdForward(false);
                return;
            }
        }

        holdForward(true);
        applyHorizontalVelocity(shuttleGoal, moveSpeed.getValue());
    }

    private void holdForward(boolean down) {
        if (mc.options == null) return;
        if (down) {
            if (!mc.options.forwardKey.isPressed()) {
                mc.options.forwardKey.setPressed(true);
                forwardHeld = true;
            }
        } else if (forwardHeld) {
            mc.options.forwardKey.setPressed(false);
            forwardHeld = false;
        }
    }

    // --- ATTACKING ---
    private void tickAttacking() {
        if (!isTargetValid(target)) { resetState(); return; }
        if (rotate.getValue()) aimAt(getAttackVec(target));
        if (!cooldownReady()) return;
        if (rotate.getValue() && !isAimedAt(target)) return;

        doAttack(target);
        finishOrReturn();
    }

    // --- RETURNING ---
    private void tickReturning() {
        if (returnGoal == null) { resetState(); return; }
        Vec3d pos = mc.player.getPos();
        boolean arrived = hDist(pos, returnGoal) < 0.4
                && (!flying() || Math.abs(pos.y - returnGoal.y) < 1.0);
        if (arrived) { resetState(); return; }
        applyHorizontalVelocity(returnGoal, moveSpeed.getValue());
        if (rotate.getValue() && rotateMoveTarget.getValue()) aimAt(returnGoal);
    }

    private void finishOrReturn() {
        releaseCharge();
        target = null;
        if (returnAfter.getValue() && returnGoal != null) {
            state = State.RETURNING;
        } else {
            resetState();
        }
    }

    // --- Charge management ---
    private void beginCharge() {
        if (mc.interactionManager == null || mc.player == null) return;
        int spearSlot = findSpearSlot();
        if (spearSlot == -1) return;

        if (selfCharging) {
            if (!chargeHoldIntact()) { releaseCharge(); return; }

            if (chargeExpired()) {
                mc.options.useKey.setPressed(false);
                mc.interactionManager.stopUsingItem(mc.player);
                mc.options.useKey.setPressed(true);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                chargeTick = mc.player.age;
                return;
            }
            mc.options.useKey.setPressed(true);
            if (!mc.player.isUsingItem()) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                chargeTick = mc.player.age;
            }
            return;
        }

        chargeSlot = mc.player.getInventory().selectedSlot;
        InventoryUtil.switchToSlot(spearSlot);
        mc.options.useKey.setPressed(true);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        selfCharging = true;
        chargeTick = mc.player.age;
        chargeHeldSlot = spearSlot;
    }

    private boolean chargeHoldIntact() {
        if (mc.player == null || chargeHeldSlot < 0) return false;
        return mc.player.getInventory().selectedSlot == chargeHeldSlot
                && MaceHelper.isSpear(mc.player.getMainHandStack());
    }

    private void releaseCharge() {
        if (!selfCharging) { chargeTick = -1; chargeSlot = -1; chargeHeldSlot = -1; return; }
        if (mc.player != null && mc.interactionManager != null) {
            mc.options.useKey.setPressed(false);
            mc.interactionManager.stopUsingItem(mc.player);
            if (chargeSlot >= 0 && chargeSlot < 9 && chargeHoldIntact()) InventoryUtil.switchToSlot(chargeSlot);
        }
        selfCharging   = false;
        chargeTick     = -1;
        chargeSlot     = -1;
        chargeHeldSlot = -1;
    }

    private int chargeElapsed() {
        if (!selfCharging || chargeTick < 0 || mc.player == null) return 0;
        if (!mc.player.isUsingItem()) return 0;
        return mc.player.age - chargeTick;
    }

    private boolean chargeArmed() {
        int elapsed = chargeElapsed();
        return elapsed >= requiredChargeTicks() && elapsed <= maxChargeTicks();
    }

    private int requiredChargeTicks() {
        return FALLBACK_DELAY_TICKS + extraCharge.getValueInt();
    }

    private boolean chargeExpired() {
        return selfCharging && chargeElapsed() > maxChargeTicks();
    }

    private int maxChargeTicks() {
        return FALLBACK_MAX_TICKS;
    }

    private void startNextPass() {
        if (!holdCharge.getValue() || chargeExpired()) releaseCharge();
        Vec3d axis = horizontalDir(mc.player.getPos(), target.getPos());
        chargeAxis = axis.lengthSquared() < 1.0E-6 ? playerFacingHorizontal() : axis;
        moveAwayGoal = retreatGoal(chargeAxis);
        state = State.MOVING_AWAY;
    }

    // --- Attack ---
    private boolean sendStab() {
        if (mc.player == null || mc.getNetworkHandler() == null) return false;
        PlayerActionC2SPacket packet = new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN);
        ((SpearStabPacketMarker) packet).spearfix$markAsStab();
        mc.getNetworkHandler().sendPacket(packet);
        SpearFix.startStabAnimation(mc.player, mc.player.getMainHandStack());
        return true;
    }

    private void doAttack(Entity entity) {
        if (mc.interactionManager == null) return;

        int spearSlot = findSpearSlot();
        if (spearSlot == -1) return;

        if (!MaceHelper.isSpear(mc.player.getMainHandStack())) InventoryUtil.switchToSlot(spearSlot);
        if (!maceSwap.getValue()) {
            if (sendStab()) {
                EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
                mc.player.resetLastAttackedTicks();
            }
            return;
        }

        CombatBlockUtil.Swap spearSwap = CombatBlockUtil.swap(spearSlot);
        int maceSlot = CombatBlockUtil.findItem(Items.MACE, maceInventory.getValue());
        if (maceSlot != -1) {
            CombatBlockUtil.Swap maceSw = CombatBlockUtil.swap(maceSlot);
            mc.interactionManager.attackEntity(mc.player, entity);
            EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
            CombatBlockUtil.restore(maceSw);
        } else {
            mc.interactionManager.attackEntity(mc.player, entity);
            EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
        }
        CombatBlockUtil.restore(spearSwap);

        mc.player.resetLastAttackedTicks();
    }

    // --- Movement ---
    private void applyHorizontalVelocity(Vec3d destination, double speed) {
        Vec3d pos = mc.player.getPos();
        Vec3d dir = destination.subtract(pos);
        double hLen = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        double vy = mc.player.getVelocity().y;

        if (aimAxisMove() && hLen >= 0.01) {
            double yawRad = Math.toRadians(aimYaw);
            double fx = -Math.sin(yawRad);
            double fz = Math.cos(yawRad);
            double sign = (dir.x * fx + dir.z * fz) >= 0.0 ? 1.0 : -1.0;
            if (flying()) {
                double dyF = destination.y - pos.y;
                vy = Math.abs(dyF) > 0.25
                        ? Math.clamp(dyF, -climbSpeed.getValue(), climbSpeed.getValue())
                        : 0.0;
            }
            mc.player.setVelocity(fx * speed * sign, vy, fz * speed * sign);
            return;
        }

        if (flying()) {
            double dy = destination.y - pos.y;
            if (Math.abs(dy) > 0.25) {
                vy = Math.clamp(dy, -climbSpeed.getValue(), climbSpeed.getValue());
            } else {
                vy = 0.0;
            }
        }

        if (hLen < 0.01) {
            if (flying()) mc.player.setVelocity(0.0, vy, 0.0);
            return;
        }
        double nx = dir.x / hLen;
        double nz = dir.z / hLen;
        float deflected = avoidYaw(yawOf(new Vec3d(nx, 0.0, nz)));
        double yawRad = Math.toRadians(deflected);
        nx = -Math.sin(yawRad);
        nz = Math.cos(yawRad);
        mc.player.setVelocity(nx * speed, vy, nz * speed);
    }

    private boolean applyAimedVelocity(Entity e, double speed) {
        if (e == null || mc.player == null) return false;
        Vec3d delta = getAttackVec(e).subtract(mc.player.getEyePos());
        if (delta.lengthSquared() < 1.0E-6) return false;
        Vec3d dir = delta.normalize();

        double needed = FALLBACK_MIN_REL / 20.0 * speedMargin.getValue();
        double magnitude = Math.max(speed, needed);

        Vec3d flat = new Vec3d(dir.x, 0.0, dir.z);
        if (flat.lengthSquared() > 1.0E-6) {
            float desired = yawOf(flat);
            float steered = avoidYaw(desired);
            if (steered != desired) {
                double rad = Math.toRadians(steered);
                double hLen = flat.length();
                dir = new Vec3d(-Math.sin(rad) * hLen, dir.y, Math.cos(rad) * hLen).normalize();
            }
        }
        mc.player.setVelocity(dir.multiply(magnitude));
        return true;
    }

    private boolean flying() {
        if (mc.player == null || mc.player.isOnGround()) return false;
        ElytraFlight fly = ElytraFlight.INSTANCE;
        return elytra.getValue() && fly != null && fly.isOn() && fly.isFallFlying();
    }

    private double passHeight() {
        return flying() ? diveHeight.getValue() : 0.0;
    }

    // --- Avoid ---
    private float avoidYaw(float desired) {
        if (!avoidBlocks.getValue() || mc.world == null || mc.player == null) return desired;
        if (pathClear(desired)) return desired;

        float limit = (float) avoidAngle.getValue();
        for (float step : AVOID_STEPS) {
            if (step > limit) break;
            if (pathClear(desired + step)) return desired + step;
            if (pathClear(desired - step)) return desired - step;
        }
        return desired;
    }

    private boolean pathClear(float yaw) {
        double yawRad = Math.toRadians(yaw);
        Vec3d dir = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        Vec3d side = new Vec3d(-dir.z, 0.0, dir.x).multiply(AVOID_HALF_WIDTH);

        Vec3d vel = mc.player.getVelocity();
        double spd = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        double reach = avoidDistance.getValue() + spd * AVOID_SPEED_LOOKAHEAD;
        Vec3d step = dir.multiply(reach);

        Vec3d eye = mc.player.getEyePos();
        Vec3d low = new Vec3d(eye.x, mc.player.getPos().y + 0.3, eye.z);
        for (Vec3d origin : new Vec3d[]{eye, low}) {
            for (Vec3d offset : new Vec3d[]{Vec3d.ZERO, side, side.negate()}) {
                Vec3d from = origin.add(offset);
                if (!rayClear(from, from.add(step))) return false;
            }
        }
        return true;
    }

    private boolean rayClear(Vec3d from, Vec3d to) {
        HitResult hit = mc.world.raycast(new RaycastContext(from, to,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit == null || hit.getType() == HitResult.Type.MISS;
    }

    // --- Rotation ---
    private void aimAt(Vec3d point) {
        if (point == null || mc.player == null) return;

        float[] goal = RotationManager.getRotation(point);
        aimYaw = goal[0];
        aimYawSet = true;

        if (rotateMode.is(RotateMode.Silent)) {
            Forever.ROTATION.snapAt(goal[0], goal[1]);
            return;
        }
        mc.player.setYaw(goal[0]);
        mc.player.setPitch(goal[1]);
    }

    private void releaseRotate() {
        Forever.ROTATION.snapBack();
        aimYawSet = false;
    }

    private boolean aimAxisMove() {
        return rotate.getValue() && aimTarget.getValue() && aimYawSet;
    }

    private boolean isAimedAt(Entity e) {
        if (e == null || mc.player == null) return false;
        if (rotateMode.is(RotateMode.Client)) return true;
        float[] goal = RotationManager.getRotation(getAttackVec(e));
        return Math.abs(MathHelper.wrapDegrees(goal[0] - Forever.ROTATION.getLastYaw())) <= AIM_TOLERANCE
                && Math.abs(goal[1] - Forever.ROTATION.getLastPitch()) <= AIM_TOLERANCE;
    }

    private Vec3d playerFacingHorizontal() {
        double yaw = Math.toRadians(rotateMode.is(RotateMode.Silent) || mc.player == null
                ? Forever.ROTATION.getLastYaw()
                : mc.player.getYaw());
        return new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw)).normalize();
    }

    // --- Targeting ---
    private Entity findTarget(double searchRange) {
        Comparator<Entity> comp = targetFilter.is(TargetMode.HEALTH)
                ? Comparator.comparingDouble(e -> e instanceof LivingEntity l ? l.getHealth() + l.getAbsorptionAmount() : Double.MAX_VALUE)
                : Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e));

        Entity best = null;
        for (Entity e : Forever.THREAD.getEntities()) {
            if (!isEnemy(e) || mc.player.squaredDistanceTo(e) > searchRange * searchRange) continue;
            if (!(flying() && alignSpeed.getValue()) && Math.abs(yGap(e)) > maxYGap.getValue()) continue;
            if (best == null || comp.compare(e, best) < 0) best = e;
        }
        return best;
    }

    private boolean isEnemy(Entity e) {
        if (e == mc.player || !e.isAlive() || e.isRemoved()) return false;
        if (!(e instanceof LivingEntity) || e instanceof ArmorStandEntity) return false;
        if (!(e instanceof PlayerEntity)) return !playersOnly.getValue() && CombatBlockUtil.isValidTarget(e);
        return CombatBlockUtil.isValidTarget(e);
    }

    private boolean isTargetValid(Entity e) {
        return e != null && e.isAlive() && !e.isRemoved() && isEnemy(e);
    }

    private boolean canAttack(Entity e) {
        if (e == null) return false;
        if (eyeDist(e) > Math.min(range.getValue(), maxReach())) return false;
        if (e instanceof LivingEntity living && living.hurtTime > hurtTime.getValueInt()) return false;
        if (!whileEating.getValue() && isUsingForeignItem()) return false;
        return true;
    }

    private boolean isUsingForeignItem() {
        return mc.player != null && mc.player.isUsingItem() && !selfCharging;
    }

    private boolean cooldownReady() {
        return mc.player.getAttackCooldownProgress(0.5F) >= 1.0F;
    }

    // --- Geometry helpers ---
    private Vec3d getAttackVec(Entity entity) {
        Box box = entity.getBoundingBox();
        Vec3d eyes = mc.player.getEyePos();
        return new Vec3d(
                Math.clamp(eyes.x, box.minX, box.maxX),
                Math.clamp(eyes.y, box.minY, box.maxY),
                Math.clamp(eyes.z, box.minZ, box.maxZ)
        );
    }

    private Vec3d horizontalDir(Vec3d from, Vec3d to) {
        Vec3d delta = to.subtract(from);
        double len = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        return len < 0.01 ? Vec3d.ZERO : new Vec3d(delta.x / len, 0, delta.z / len);
    }

    private Vec3d retreatGoal(Vec3d axis) {
        double dist = Math.max(chargeDistance.getValue(), maxReach() + 1.0);
        boolean anchored = straightLine.getValue() && autoAlign.getValue();
        Vec3d anchor = anchored ? target.getPos() : mc.player.getPos();
        Vec3d goal = anchor.add(axis.multiply(-dist));
        if (!flying()) return goal;
        double baseY = anchored ? target.getPos().y : mc.player.getPos().y;
        return new Vec3d(goal.x, baseY + passHeight(), goal.z);
    }

    private boolean isInFront() {
        Vec3d toTarget = horizontalDir(mc.player.getPos(), target.getPos());
        return toTarget.lengthSquared() > 1.0E-6 && toTarget.dotProduct(playerFacingHorizontal()) > 0.0;
    }

    private double eyeDist(Entity entity) {
        Vec3d eyes = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        double dx = Math.max(0.0, Math.max(box.minX - eyes.x, eyes.x - box.maxX));
        double dy = Math.max(0.0, Math.max(box.minY - eyes.y, eyes.y - box.maxY));
        double dz = Math.max(0.0, Math.max(box.minZ - eyes.z, eyes.z - box.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double hDist(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private double yGap(Entity e) {
        if (e == null || mc.player == null) return 0.0;
        return getAttackVec(e).y - mc.player.getEyePos().y;
    }

    private double minReach() { return FALLBACK_MIN_REACH; }
    private double maxReach() { return FALLBACK_MAX_REACH; }

    private static float yawOf(Vec3d dir) {
        return (float) (Math.toDegrees(Math.atan2(-dir.x, dir.z)));
    }

    private int findSpearSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (MaceHelper.isSpear(mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private boolean hasSpear() {
        return findSpearSlot() != -1;
    }

    private boolean isInAimRange(Entity e) {
        if (e == null || mc.player == null) return false;
        return eyeDist(e) <= Math.max(range.getValue(), maxReach());
    }

    // --- Render ---
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (target == null || !targetEsp.getValue()) return;
        Box box = target.getBoundingBox();
        Render3DUtil.drawFill(event.matrixStack, box, espFill.getValue());
        Render3DUtil.drawBox(event.matrixStack, box, espOutline.getValue());
    }

    // --- Reset ---
    private void resetState() {
        releaseCharge();
        releaseRotate();
        state = State.IDLE;
        target = null;
        moveAwayGoal = null;
        returnGoal = null;
        chargeAxis = null;
        passDirection = null;
        shuttleAxis = null;
        shuttleGoal = null;
        shuttleSide = Side.In;
        legStartTick = -1;
        crossed = false;
        holdForward(false);
    }
}
