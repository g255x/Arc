package dev.G255.Lumi.mod.modules.impl.movement;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.babbaj.pathfinder.NetherPathfinder;
import dev.babbaj.pathfinder.PathSegment;
import dev.G255.Lumi.api.events.impl.ClickBlockEvent;
import dev.G255.Lumi.api.events.impl.KeyboardInputEvent;
import dev.G255.Lumi.api.events.impl.PacketEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.events.impl.UpdateRotateEvent;
import dev.G255.Lumi.api.utils.path.BaritoneUtil;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.render.Render3DUtil;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.player.SpeedMine;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.ColorSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public class FastMoveatAi extends Module {
    public static FastMoveatAi INSTANCE;
    private static final int BODY_TUNNEL_HEIGHT = 2;
    private static final int TAKEOFF_TUNNEL_HEIGHT = 3;
    private static final double AIR_LOOKAHEAD = 24.0;
    private static final double TARGET_APPROACH_XZ_SQ = 16.0;
    private static final double REACH_EPSILON = 0.15;
    private static final double VERTICAL_ADJUST_THRESHOLD = 1.25;
    private static final double LEVEL_FLIGHT_THRESHOLD = 0.65;
    private static final double SIDE_PROBE_DISTANCE = 1.0;
    private static final double ENTRY_COMMIT_DISTANCE = 2.0;
    private static final double LOCAL_ROUTE_SCAN_DISTANCE = 6.0;
    private static final double LOCAL_ROUTE_MIN_CLEAR_DISTANCE = 2.5;
    private static final double LOCAL_ROUTE_ENTRY_DISTANCE = 1.0;
    private static final double LOCAL_ROUTE_MIN_PROGRESS = -0.25;
    private static final double HIGH_ALTITUDE_TOP_MARGIN = 16.0;
    private static final double HIGH_ALTITUDE_MIN_RAISE = 80.0;
    private static final double HIGH_ALTITUDE_DESCEND_START = 160.0;
    private static final double HIGH_ALTITUDE_DESCEND_END = 32.0;
    private static final double FLIGHT_COLLISION_STEP = 0.25;
    private static final double FLIGHT_COLLISION_MARGIN = 0.02;
    private static final int PATH_RECALCULATE_TICKS = 20;
    private static final int PATH_SYNC_CHUNK_RADIUS = 6;
    private static final int PATH_TARGET_SYNC_RADIUS = 2;
    private static final int PATH_CULL_DISTANCE_BLOCKS = 512;
    private static final int PATH_TIMEOUT_MS = 50;
    private static final double PATH_WAYPOINT_REACH_SQ = 9.0;
    private static final int PATH_WAYPOINT_LOOKAHEAD = 12;
    private static final double AXIS_STAGE_RANGE = 4.0;
    private static final int BLOCKPOS_X_SHIFT = 38;
    private static final int BLOCKPOS_Y_SHIFT = 26;
    private static final int BLOCKPOS_XZ_BITS = 26;
    private static final int BLOCKPOS_Y_BITS = 12;
    private static final int CHEST_EQUIPMENT_SLOT = 6;
    private static final int HOTBAR_SCREEN_SLOT_OFFSET = 36;
    private static final int GEAR_SWAP_COOLDOWN_TICKS = 10;
    private static final int EATING_SUPPRESS_GRACE_TICKS = 8;
    // Mining is only allowed mid-flight once the player has been genuinely wedged
    // for this many ticks with no movement â?prevents the "random digging" bug.
    private static final int STUCK_MINE_THRESHOLD = 6;
    private static final double STUCK_MOVE_EPS = 0.05;

    private final SliderSetting targetX = this.add(new SliderSetting("TargetX", 0, -30000000, 30000000, 1.0));
    private final SliderSetting targetY = this.add(new SliderSetting("TargetY", 64, -64, 320, 1.0));
    private final SliderSetting targetZ = this.add(new SliderSetting("TargetZ", 0, -30000000, 30000000, 1.0));
    private final SliderSetting arriveRange = this.add(new SliderSetting("ArriveRange", 2.0, 0.5, 8.0, 0.5));
    private final SliderSetting lowHealth = this.add(new SliderSetting("LowHealth", 12.0, 1.0, 36.0, 0.5));
    private final SliderSetting mineRange = this.add(new SliderSetting("MineRange", 4.5, 2.0, 6.0, 0.5));
    private final SliderSetting minDurability = this.add(new SliderSetting("MinDurability", 10.0, 1.0, 50.0, 1.0));
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Client));
    private final EnumSetting<AxisPriority> axisPriority = this.add(new EnumSetting<>("AxisPriority", AxisPriority.Z));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
    private final BooleanSetting renderPath = this.add(new BooleanSetting("RenderPath", true));
    private final ColorSetting pathColor = this.add(new ColorSetting("PathColor", new Color(255, 0, 0, 220), this.renderPath::getValue));
    private final SliderSetting lineWidth = this.add(new SliderSetting("LineWidth", 2.0, 0.5, 5.0, 0.1, this.renderPath::getValue));

    private boolean pathStarted;
    private int inactiveTicks;
    private BlockPos activeTarget;
    private StartResult lastStartResult = StartResult.FAILED;
    private boolean eatingGapple;
    private boolean emergencyGappleActive;
    private int eatingSuppressTicks;
    private boolean wasPressingUse;
    private int previousSlot = -1;
    private int eatTicks;
    private BlockPos miningPos;
    private Direction miningSide;
    private boolean miningStarted;
    private boolean restoreSpeedMine;
    private boolean hasCruiseY;
    private double cruiseY;
    private Vec3d entryWaypoint;
    private float targetYaw;
    private float targetPitch;
    private boolean autoInputActive;
    private boolean restoreForwardKey;
    private boolean restoreBackKey;
    private boolean restoreLeftKey;
    private boolean restoreRightKey;
    private boolean restoreJumpKey;
    private boolean restoreSneakKey;
    private boolean inputForward;
    private boolean inputBack;
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputJump;
    private boolean inputSneak;
    private int gearSwapCooldown;
    private Vec3d lastProgressPos;
    private int stuckTicks;
    private boolean groundMiningElytraDisabled;
    private long pathContext;
    private int pathContextDimension = -1;
    private int pathContextHeight = 256;
    private boolean pathUnavailableNotified;
    private BlockPos pathRouteTarget;
    private AxisStage axisStage = AxisStage.Z;
    private int axisLockedX;
    private int axisLockedZ;
    private int pathRecalculateTicks;
    private int pathWaypointIndex;
    private final List<Vec3d> pathWaypoints = new ArrayList<>();
    private final Set<Long> pathSyncedChunks = new HashSet<>();

    public FastMoveatAi() {
        super("FastMoveatAi", "Path to a configured Overworld, Nether or End coordinate.", Category.Movement);
        this.setChinese("");
        this.setChineseDesc("");
        INSTANCE = this;
    }

    public StartResult startTo(int x, int y, int z) {
        this.targetX.setValue(x);
        this.targetY.setValue(y);
        this.targetZ.setValue(z);
        if (this.isOff()) {
            this.enable();
            return this.lastStartResult;
        }
        return this.beginPath();
    }

    public void stopNavigation() {
        if (this.isOn()) {
            this.disable();
            } else {
                BaritoneUtil.cancelEverything();
                this.releaseMovementInput();
                this.stopMining();
                this.restoreGroundMiningElytra();
                this.restoreSpeedMineIfNeeded();
                this.resetState();
            }
        }

    public BlockPos getTarget() {
        return this.targetFromSettings();
    }

    @Override
    public void onEnable() {
        this.beginPath();
    }

    @Override
    public void onDisable() {
        BaritoneUtil.cancelEverything();
        this.releasePathContext();
        this.finishEating(false);
        this.releaseMovementInput();
        this.stopMining();
        this.restoreGroundMiningElytra();
        this.restoreSpeedMineIfNeeded();
        this.resetState();
    }

    @Override
    public void onLogout() {
        this.disable();
    }

    @Override
    public String getInfo() {
        BlockPos target = this.activeTarget != null ? this.activeTarget : this.targetFromSettings();
        return target.getX() + "," + target.getY() + "," + target.getZ();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.validateRuntime(true)) {
            return;
        }

        BlockPos finalTarget = this.activeTarget != null ? this.activeTarget : this.targetFromSettings();
        BlockPos target = this.getCurrentRouteTarget(finalTarget);
        if (this.handleEmergencyGapple()) {
            return;
        }

        if (!target.equals(finalTarget) && this.isArrived(target)) {
            this.advanceAxisStage();
            this.clearPathRoute();
            target = this.getCurrentRouteTarget(finalTarget);
        }

        if (this.isArrived(finalTarget)) {
            this.sendMessage("[FastMoveatAi] Arrived at " + format(finalTarget));
            if (this.autoDisable.getValue()) {
                this.disable();
            } else {
                BaritoneUtil.cancelEverything();
                this.releaseMovementInput();
                this.stopMining();
                this.restoreGroundMiningElytra();
                this.restoreSpeedMineIfNeeded();
                this.resetState();
            }
            return;
        }

        this.tickGearSwapCooldown();
        this.updateStuckTracker();
        this.ensureElytraGoto(finalTarget);
        this.equipBestElytraIfNeeded();
        if (this.mineBlockingBlock(target)) {
            return;
        }

        this.updateHumanInput(target);

        if (!this.isNavigationActive()) {
            this.inactiveTicks++;
            if (this.inactiveTicks >= 20) {
                this.pathTo(target);
            }
        } else {
            this.inactiveTicks = 0;
        }
    }

    private StartResult beginPath() {
        this.resetState();
        if (!this.validateRuntime(false)) {
            this.lastStartResult = StartResult.FAILED;
            this.disable();
            return StartResult.FAILED;
        }

        BlockPos target = this.targetFromSettings();
        if (this.isArrived(target)) {
            this.sendMessage("[FastMoveatAi] Already at " + format(target));
            this.lastStartResult = StartResult.ARRIVED;
            this.disable();
            return StartResult.ARRIVED;
        }

        this.disableSpeedMineIfNeeded();
        if (this.mode.is(Mode.path)) {
            this.ensurePathContext();
        }
        this.pathTo(target);
        this.sendMessage("[FastMoveatAi] Pathing to " + this.getDimensionName() + " " + format(target));
        this.lastStartResult = StartResult.STARTED;
        return StartResult.STARTED;
    }

    private void pathTo(BlockPos target) {
        BaritoneUtil.cancelEverything();
        if (ElytraFlight.INSTANCE != null) {
            ElytraFlight.INSTANCE.enable();
        }
        this.getCruiseY(target);
        this.activeTarget = target;
        this.resetAxisRoute();
        this.pathStarted = true;
        this.inactiveTicks = 0;
        this.clearPathRoute();
    }

    private double getCruiseY(BlockPos target) {
        if (!this.hasCruiseY) {
            this.cruiseY = this.getInitialCruiseY(target);
            this.hasCruiseY = true;
        }
        return this.cruiseY;
    }

    private double getInitialCruiseY(BlockPos target) {
        double currentY = mc.player != null ? mc.player.getY() + 1.0 : target.getY() + 1.0;
        if (!this.shouldUseHighAltitudeCruise()) {
            return currentY;
        }

        double topY = mc.world != null ? mc.world.getTopY() - HIGH_ALTITUDE_TOP_MARGIN : currentY + HIGH_ALTITUDE_MIN_RAISE;
        double raisedY = currentY + HIGH_ALTITUDE_MIN_RAISE;
        double targetSafeY = target.getY() + HIGH_ALTITUDE_MIN_RAISE;
        return MathHelper.clamp(Math.max(raisedY, targetSafeY), target.getY() + 8.0, topY);
    }

    private boolean shouldUseHighAltitudeCruise() {
        return mc.world != null
                && (mc.world.getRegistryKey() == World.OVERWORLD || mc.world.getRegistryKey() == World.END);
    }

    private void ensureElytraGoto(BlockPos target) {
        this.syncGroundMiningElytra(false);
        if (this.groundMiningElytraDisabled) {
            return;
        }
        if (ElytraFlight.INSTANCE != null && ElytraFlight.INSTANCE.isOff()) {
            ElytraFlight.INSTANCE.enable();
        }
    }

    private boolean isNavigationActive() {
        return this.pathStarted && this.activeTarget != null;
    }

    private void updateHumanInput(BlockPos target) {
        if (mc.player == null) return;

        Vec3d aim = this.getAirWaypoint(target);
        Vec3d eyes = mc.player.getEyePos();
        Vec3d diff = aim.subtract(eyes);
        double diffXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        if (diff.lengthSquared() > 1.0E-4) {
            this.targetYaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
            double verticalDelta = aim.y - (mc.player.getY() + 1.0);
            if (Math.abs(verticalDelta) <= LEVEL_FLIGHT_THRESHOLD) {
                this.targetPitch = 0.0F;
            } else {
                this.targetPitch = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(diff.y, diffXZ)), -18.0, 18.0);
            }
        } else {
            this.targetYaw = mc.player.getYaw();
            this.targetPitch = 0.0F;
        }

        this.updateMovementKeys(aim);
    }

    private void updateMovementKeys(Vec3d aim) {
        Vec3d playerPos = mc.player.getPos();
        Vec3d diff = aim.subtract(playerPos);
        double horizontalSq = diff.x * diff.x + diff.z * diff.z;
        boolean forward = false;
        boolean back = false;
        boolean left = false;
        boolean right = false;

        if (horizontalSq > 0.04) {
            float moveYaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
            float yawDelta = MathHelper.wrapDegrees(moveYaw - mc.player.getYaw());
            float absDelta = Math.abs(yawDelta);

            // Steering is done primarily by yaw; movement keys stay simple to avoid the
            // left/right jitter the old per-15Â° strafe ladder produced. Within a wide
            // forward cone just walk straight; only add a strafe nudge past a 35Â° dead
            // zone, and never strafe-in-place (that caused the side-to-side shaking).
            if (absDelta <= 135.0F) {
                forward = true;
                if (absDelta > 35.0F) {
                    if (yawDelta > 0.0F) {
                        right = true;
                    } else {
                        left = true;
                    }
                }
            } else {
                back = true;
            }
        }

        this.setMovementInput(forward, back, left, right, this.shouldPressJump(aim), this.shouldPressSneak(aim));
    }

    private void setMovementInput(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak) {
        if (!this.autoInputActive) {
            this.restoreForwardKey = mc.options.forwardKey.isPressed();
            this.restoreBackKey = mc.options.backKey.isPressed();
            this.restoreLeftKey = mc.options.leftKey.isPressed();
            this.restoreRightKey = mc.options.rightKey.isPressed();
            this.restoreJumpKey = mc.options.jumpKey.isPressed();
            this.restoreSneakKey = mc.options.sneakKey.isPressed();
            this.autoInputActive = true;
        }

        this.inputForward = forward;
        this.inputBack = back;
        this.inputLeft = left;
        this.inputRight = right;
        this.inputJump = jump;
        this.inputSneak = sneak;
        this.applyMovementInput();
    }

    private void applyMovementInput() {
        if (mc.player == null) return;

        mc.options.forwardKey.setPressed(this.inputForward);
        mc.options.backKey.setPressed(this.inputBack);
        mc.options.leftKey.setPressed(this.inputLeft);
        mc.options.rightKey.setPressed(this.inputRight);
        mc.options.jumpKey.setPressed(this.inputJump);
        mc.options.sneakKey.setPressed(this.inputSneak);

        mc.player.input.pressingForward = this.inputForward;
        mc.player.input.pressingBack = this.inputBack;
        mc.player.input.pressingLeft = this.inputLeft;
        mc.player.input.pressingRight = this.inputRight;
        mc.player.input.movementForward = this.getMovementMultiplier(this.inputForward, this.inputBack);
        mc.player.input.movementSideways = this.getMovementMultiplier(this.inputLeft, this.inputRight);
        mc.player.input.jumping = this.inputJump;
        mc.player.input.sneaking = this.inputSneak;
    }

    private boolean shouldPressJump(Vec3d aim) {
        if (mc.player == null) return false;
        double verticalDelta = aim.y - (mc.player.getY() + 1.0);
        if (!mc.player.isFallFlying()) {
            return this.hasTakeoffSpace();
        }
        return verticalDelta > VERTICAL_ADJUST_THRESHOLD;
    }

    private boolean shouldPressSneak(Vec3d aim) {
        if (mc.player == null || !mc.player.isFallFlying()) return false;
        double verticalDelta = aim.y - (mc.player.getY() + 1.0);
        return verticalDelta < -VERTICAL_ADJUST_THRESHOLD;
    }

    private boolean hasTakeoffSpace() {
        if (mc.player == null || mc.world == null) return false;
        return this.findTakeoffObstacle(mc.player.getPos()) == null;
    }

    private float getMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) return 0.0F;
        return positive ? 1.0F : -1.0F;
    }

    private void pauseMovementInput() {
        if (mc.player == null) return;
        this.setMovementInput(false, false, false, false, false, false);
    }

    private void releaseMovementInput() {
        if (!this.autoInputActive) return;

        mc.options.forwardKey.setPressed(this.restoreForwardKey);
        mc.options.backKey.setPressed(this.restoreBackKey);
        mc.options.leftKey.setPressed(this.restoreLeftKey);
        mc.options.rightKey.setPressed(this.restoreRightKey);
        mc.options.jumpKey.setPressed(this.restoreJumpKey);
        mc.options.sneakKey.setPressed(this.restoreSneakKey);

        if (mc.player != null) {
            mc.player.input.pressingForward = this.restoreForwardKey;
            mc.player.input.pressingBack = this.restoreBackKey;
            mc.player.input.pressingLeft = this.restoreLeftKey;
            mc.player.input.pressingRight = this.restoreRightKey;
            mc.player.input.movementForward = this.getMovementMultiplier(this.restoreForwardKey, this.restoreBackKey);
            mc.player.input.movementSideways = this.getMovementMultiplier(this.restoreLeftKey, this.restoreRightKey);
            mc.player.input.jumping = this.restoreJumpKey;
            mc.player.input.sneaking = this.restoreSneakKey;
        }

        this.autoInputActive = false;
        this.restoreForwardKey = false;
        this.restoreBackKey = false;
        this.restoreLeftKey = false;
        this.restoreRightKey = false;
        this.restoreJumpKey = false;
        this.restoreSneakKey = false;
        this.inputForward = false;
        this.inputBack = false;
        this.inputLeft = false;
        this.inputRight = false;
        this.inputJump = false;
        this.inputSneak = false;
    }

    @EventListener(priority = -20000)
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (!this.autoInputActive || nullCheck()) return;
        this.applyMovementInput();
    }

    @EventListener(priority = -20000)
    public void onRotate(UpdateRotateEvent event) {
        if (!this.autoInputActive || nullCheck()) return;
        event.setYaw(this.targetYaw);
        event.setPitch(this.targetPitch);
    }

    @EventListener(priority = 999999)
    public void onClickBlock(ClickBlockEvent event) {
        if (!this.isEmergencyEating()) return;
        event.cancel();
        this.suppressMiningForEating();
    }

    @EventListener(priority = 999999)
    public void onPacketSend(PacketEvent.Send event) {
        if (!this.isEmergencyEating()) return;
        if (event.getPacket() instanceof PlayerActionC2SPacket packet) {
            Action action = packet.getAction();
            if (action == Action.START_DESTROY_BLOCK || action == Action.STOP_DESTROY_BLOCK) {
                this.suppressMiningForEating();
                event.cancel();
            }
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (!this.renderPath.getValue() || nullCheck()) return;

        BlockPos target = this.activeTarget != null ? this.activeTarget : this.targetFromSettings();
        Color color = this.pathColor.getValue();
        float width = this.lineWidth.getValueFloat();

        RenderSystem.disableDepthTest();
        Vec3d start = mc.player.getPos().add(0.0, 0.2, 0.0);
        Vec3d waypoint = this.getAirWaypoint(target);
        if (this.mode.is(Mode.path) && this.pathWaypoints.size() > 1) {
            Vec3d last = start;
            for (int i = Math.max(0, this.pathWaypointIndex); i < this.pathWaypoints.size(); i++) {
                Vec3d point = this.pathWaypoints.get(i);
                this.drawPathLine(last, point, color, width);
                last = point;
            }
        } else {
            this.drawPathLine(start, waypoint, color, width);
            this.drawPathLine(waypoint, target.toCenterPos(), color, width);
        }
        RenderSystem.enableDepthTest();
    }

    private void drawPathLine(Vec3d start, Vec3d end, Color color, float width) {
        Render3DUtil.drawLine(start.x, start.y, start.z, end.x, end.y, end.z, color, width);
    }

    private boolean validateRuntime(boolean disableOnFailure) {
        if (nullCheck()) {
            return false;
        }
        if (!this.isSupportedDimension()) {
            this.sendMessage("[FastMoveatAi] This module only runs in the Overworld, Nether or End.");
            if (disableOnFailure) this.disable();
            return false;
        }
        return true;
    }

    private boolean isSupportedDimension() {
        return mc.world.getRegistryKey() == World.OVERWORLD
                || mc.world.getRegistryKey() == World.NETHER
                || mc.world.getRegistryKey() == World.END;
    }

    private String getDimensionName() {
        if (mc.world == null) return "current dimension";
        if (mc.world.getRegistryKey() == World.OVERWORLD) return "Overworld";
        if (mc.world.getRegistryKey() == World.NETHER) return "Nether";
        if (mc.world.getRegistryKey() == World.END) return "End";
        return "current dimension";
    }

    private boolean isArrived(BlockPos target) {
        if (mc.player == null) return false;
        Vec3d center = Vec3d.ofCenter(target);
        return mc.player.getPos().squaredDistanceTo(center) <= this.arriveRange.getValue() * this.arriveRange.getValue();
    }

    private BlockPos getCurrentRouteTarget(BlockPos finalTarget) {
        if (mc.player == null) return finalTarget;
        this.normalizeAxisStage(finalTarget);

        if (this.axisStage == AxisStage.Z) {
            return new BlockPos(this.axisLockedX, finalTarget.getY(), finalTarget.getZ());
        }
        if (this.axisStage == AxisStage.X) {
            return new BlockPos(finalTarget.getX(), finalTarget.getY(), this.axisLockedZ);
        }
        return finalTarget;
    }

    private void resetAxisRoute() {
        this.axisStage = this.axisPriority.is(AxisPriority.X) ? AxisStage.X : AxisStage.Z;
        if (mc.player != null) {
            this.axisLockedX = mc.player.getBlockX();
            this.axisLockedZ = mc.player.getBlockZ();
        }
    }

    private void normalizeAxisStage(BlockPos finalTarget) {
        if (mc.player == null) return;
        if (this.axisStage == AxisStage.Z && this.isAxisClose(mc.player.getZ(), finalTarget.getZ())) {
            this.axisStage = AxisStage.Final;
            this.clearPathRoute();
        } else if (this.axisStage == AxisStage.X && this.isAxisClose(mc.player.getX(), finalTarget.getX())) {
            this.axisStage = AxisStage.Final;
            this.clearPathRoute();
        }
    }

    private void advanceAxisStage() {
        if (this.axisStage != AxisStage.Final) {
            this.axisStage = AxisStage.Final;
        }
    }

    private boolean isAxisClose(double current, int target) {
        double targetCenter = target + 0.5;
        return Math.abs(current - targetCenter) <= Math.max(AXIS_STAGE_RANGE, this.arriveRange.getValue());
    }

    private boolean handleEmergencyGapple() {
        if (mc.player == null || mc.interactionManager == null) return false;

        boolean low = mc.player.getHealth() + mc.player.getAbsorptionAmount() <= this.lowHealth.getValue();
        boolean lava = (mc.player.isInLava() || mc.player.isOnFire()) && !mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE);
        boolean shouldEat = low || lava;

        if (!shouldEat) {
            if (this.eatingGapple || this.emergencyGappleActive) {
                this.finishEating(true);
            }
            if (this.eatingSuppressTicks > 0) {
                this.eatingSuppressTicks--;
            }
            return false;
        }

        this.emergencyGappleActive = true;
        this.eatingSuppressTicks = EATING_SUPPRESS_GRACE_TICKS;
        this.pauseMovementInput();
        this.suppressMiningForEating();

        if (!this.hasEnchantedGapple()) {
            return true;
        }

        if (!this.eatingGapple) {
            this.startEatingGappleState();
        }

        this.ensureGappleInHand();
        this.continueEatingGapple();
        return true;
    }

    private void startEatingGappleState() {
        this.wasPressingUse = mc.options.useKey.isPressed();
        this.previousSlot = mc.player.getInventory().selectedSlot;
        this.eatTicks = 0;
        this.eatingGapple = true;
    }

    private boolean ensureGappleInHand() {
        if (mc.player.getOffHandStack().isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            return true;
        }
        if (!mc.player.getMainHandStack().isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            int slot = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
            if (slot == -1) {
                int inventorySlot = InventoryUtil.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE);
                if (inventorySlot != -1) {
                    InventoryUtil.inventorySwap(inventorySlot, mc.player.getInventory().selectedSlot);
                    slot = mc.player.getInventory().selectedSlot;
                }
            }
            if (slot == -1) {
                return false;
            }
            InventoryUtil.switchToSlot(slot);
        }
        return true;
    }

    private void continueEatingGapple() {
        this.eatTicks++;
        this.suppressMiningForEating();
        if (!this.ensureGappleInHand()) {
            mc.options.useKey.setPressed(false);
            return;
        }
        mc.options.useKey.setPressed(true);
        Hand hand = mc.player.getOffHandStack().isOf(Items.ENCHANTED_GOLDEN_APPLE) ? Hand.OFF_HAND : Hand.MAIN_HAND;
        // Only (re)start the use action when not already eating. Re-issuing interactItem
        // every few ticks RESETS the eating progress, so the gapple never finishes â?the
        // root cause of the "can't eat the gapple" bug. Once eating, just hold useKey.
        if (!mc.player.isUsingItem()) {
            mc.interactionManager.interactItem(mc.player, hand);
        }
        boolean fireResReady = mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE);
        boolean absorptionReady = mc.player.hasStatusEffect(StatusEffects.ABSORPTION);
        if (this.eatTicks > 80 || this.eatTicks > 24 && (fireResReady || absorptionReady)) {
            this.finishEating(true);
        }
    }

    private void finishEating(boolean restoreSlot) {
        if (!this.eatingGapple && !this.emergencyGappleActive) return;
        if (this.eatingGapple && !this.wasPressingUse) {
            mc.options.useKey.setPressed(false);
        }
        if (this.eatingGapple && restoreSlot && this.previousSlot >= 0 && mc.player != null
                && mc.player.getInventory().selectedSlot != this.previousSlot) {
            InventoryUtil.switchToSlot(this.previousSlot);
        }
        this.eatingGapple = false;
        this.emergencyGappleActive = false;
        this.eatingSuppressTicks = 0;
        this.wasPressingUse = false;
        this.previousSlot = -1;
        this.eatTicks = 0;
    }

    private boolean isEmergencyEating() {
        return this.emergencyGappleActive || this.eatingGapple || this.eatingSuppressTicks > 0;
    }

    private void suppressMiningForEating() {
        if (mc.player == null) return;

        mc.options.attackKey.setPressed(false);
        this.pauseMovementInput();

        BlockPos abortPos = this.miningPos;
        Direction abortSide = this.miningSide;
        if ((abortPos == null || abortSide == null) && mc.crosshairTarget instanceof BlockHitResult hitResult
                && hitResult.getType() == HitResult.Type.BLOCK) {
            abortPos = hitResult.getBlockPos();
            abortSide = hitResult.getSide();
        }

        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        if (abortPos != null && abortSide != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, abortPos, abortSide));
        }

        this.clearMiningState();
    }

    private boolean hasEnchantedGapple() {
        return mc.player.getOffHandStack().isOf(Items.ENCHANTED_GOLDEN_APPLE)
                || mc.player.getMainHandStack().isOf(Items.ENCHANTED_GOLDEN_APPLE)
                || InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE) != -1
                || InventoryUtil.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE) != -1;
    }

    private void disableSpeedMineIfNeeded() {
        if (SpeedMine.INSTANCE != null && SpeedMine.INSTANCE.isOn()) {
            this.restoreSpeedMine = true;
            SpeedMine.INSTANCE.disable();
        }
    }

    private void restoreSpeedMineIfNeeded() {
        if (this.restoreSpeedMine && SpeedMine.INSTANCE != null && SpeedMine.INSTANCE.isOff()) {
            SpeedMine.INSTANCE.enable();
        }
        this.restoreSpeedMine = false;
    }

    private boolean mineBlockingBlock(BlockPos target) {
        if (this.entryWaypoint != null) {
            return false;
        }

        BlockPos pos = this.findBlockingBlock(target);
        if (pos == null) {
            if (this.miningPos != null) {
                this.entryWaypoint = this.getTunnelEntryWaypoint(target);
            }
            this.stopMining();
            this.restoreGroundMiningElytra();
            return false;
        }

        // Mid-flight: only dig when genuinely wedged. While still moving (or able to
        // glide/veer around), keep flying instead of tearing through blocks. Ground
        // and takeoff mining (player not gliding) are unaffected.
        if (mc.player.isFallFlying() && !this.isStuck()) {
            this.stopMining();
            return false;
        }

        if (this.equipBestPickaxeIfNeeded()) {
            return true;
        }

        this.syncGroundMiningElytra(true);
        this.pauseMovementInput();
        mc.options.attackKey.setPressed(false);
        Direction side = this.getMineSide(pos);
        if (side == null) {
            this.stopMining();
            this.restoreGroundMiningElytra();
            return false;
        }

        if (pos.equals(this.miningPos) && this.miningStarted) {
            if (!this.isMineableObstacle(pos)) {
                this.invalidatePathChunk(pos);
                this.clearMiningState();
                return true;
            }
            if (this.miningSide != side) {
                this.restartMining(pos, side);
                return true;
            }
            mc.interactionManager.updateBlockBreakingProgress(pos, side);
            mc.player.swingHand(Hand.MAIN_HAND);
            return true;
        }

        if (this.miningPos != null) {
            this.abortMining();
        }
        this.miningPos = pos;
        this.miningSide = side;
        this.miningStarted = true;
        mc.interactionManager.attackBlock(pos, side);
        mc.interactionManager.updateBlockBreakingProgress(pos, side);
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private void stopMining() {
        mc.options.attackKey.setPressed(false);
        if (this.miningPos != null) {
            this.abortMining();
        }
        this.clearMiningState();
    }

    private void clearMiningState() {
        mc.options.attackKey.setPressed(false);
        this.miningPos = null;
        this.miningSide = null;
        this.miningStarted = false;
    }

    private void restartMining(BlockPos pos, Direction side) {
        this.abortMining();
        this.miningPos = pos;
        this.miningSide = side;
        this.miningStarted = true;
        mc.interactionManager.attackBlock(pos, side);
        mc.interactionManager.updateBlockBreakingProgress(pos, side);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void abortMining() {
        if (this.miningPos == null || this.miningSide == null) return;
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.miningPos, this.miningSide));
        }
    }

    private void syncGroundMiningElytra(boolean mining) {
        if (mc.player == null || mc.world == null) return;

        boolean hasFooting = this.hasSolidBlockUnderFeet();
        if (this.groundMiningElytraDisabled) {
            if (!hasFooting) {
                this.restoreGroundMiningElytra();
            }
            return;
        }

        if (mining && hasFooting && ElytraFlight.INSTANCE != null && ElytraFlight.INSTANCE.isOn()) {
            this.getCruiseY(this.activeTarget != null ? this.activeTarget : this.targetFromSettings());
            ElytraFlight.INSTANCE.disable();
            this.groundMiningElytraDisabled = true;
        }
    }

    private void restoreGroundMiningElytra() {
        if (!this.groundMiningElytraDisabled) return;
        if (ElytraFlight.INSTANCE != null && ElytraFlight.INSTANCE.isOff()) {
            ElytraFlight.INSTANCE.enable();
        }
        this.groundMiningElytraDisabled = false;
    }

    private boolean hasSolidBlockUnderFeet() {
        if (mc.player == null || mc.world == null) return false;
        double y = mc.player.getBoundingBox().minY - 0.08;
        double minX = mc.player.getBoundingBox().minX + 0.05;
        double maxX = mc.player.getBoundingBox().maxX - 0.05;
        double minZ = mc.player.getBoundingBox().minZ + 0.05;
        double maxZ = mc.player.getBoundingBox().maxZ - 0.05;
        return this.hasFootingAt(minX, y, minZ)
                || this.hasFootingAt(minX, y, maxZ)
                || this.hasFootingAt(maxX, y, minZ)
                || this.hasFootingAt(maxX, y, maxZ)
                || this.hasFootingAt(mc.player.getX(), y, mc.player.getZ());
    }

    private boolean hasFootingAt(double x, double y, double z) {
        BlockPos pos = new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        BlockState state = mc.world.getBlockState(pos);
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.getCollisionShape(mc.world, pos).isEmpty();
    }

    private void tickGearSwapCooldown() {
        if (this.gearSwapCooldown > 0) {
            this.gearSwapCooldown--;
        }
    }

    /** Tracks how long the player has been wedged so mid-flight mining only kicks in when truly stuck. */
    private void updateStuckTracker() {
        if (mc.player == null) return;
        Vec3d pos = mc.player.getPos();
        if (this.lastProgressPos == null) {
            this.lastProgressPos = pos;
            this.stuckTicks = 0;
            return;
        }
        if (pos.squaredDistanceTo(this.lastProgressPos) > STUCK_MOVE_EPS * STUCK_MOVE_EPS) {
            this.lastProgressPos = pos;
            this.stuckTicks = 0;
        } else {
            this.stuckTicks++;
        }
    }

    private boolean isStuck() {
        return this.stuckTicks >= STUCK_MINE_THRESHOLD;
    }

    private boolean equipBestPickaxeIfNeeded() {
        if (mc.player == null || mc.interactionManager == null || this.gearSwapCooldown > 0) return false;
        ItemStack current = mc.player.getMainHandStack();
        if (current.getItem() instanceof PickaxeItem && !this.isBelowMinDurability(current)) {
            return false;
        }

        int bestSlot = this.findBestPickaxeSlot();
        if (bestSlot == -1 || bestSlot == mc.player.getInventory().selectedSlot) {
            return false;
        }

        this.stopMining();
        if (bestSlot < 9) {
            InventoryUtil.switchToSlot(bestSlot);
        } else if (EntityUtil.inInventory()) {
            InventoryUtil.inventorySwap(this.toScreenSlot(bestSlot), mc.player.getInventory().selectedSlot);
        } else {
            return false;
        }

        this.gearSwapCooldown = GEAR_SWAP_COOLDOWN_TICKS;
        return true;
    }

    private boolean equipBestElytraIfNeeded() {
        if (mc.player == null || mc.interactionManager == null || this.gearSwapCooldown > 0 || !EntityUtil.inInventory()) {
            return false;
        }

        ItemStack chestStack = mc.player.getInventory().getArmorStack(2);
        if (this.isGoodElytra(chestStack)) {
            return false;
        }

        int bestSlot = this.findBestElytraSlot();
        if (bestSlot == -1) {
            return false;
        }

        int syncId = mc.player.currentScreenHandler.syncId;
        if (bestSlot < 9) {
            mc.interactionManager.clickSlot(syncId, CHEST_EQUIPMENT_SLOT, bestSlot, SlotActionType.SWAP, mc.player);
        } else {
            int screenSlot = this.toScreenSlot(bestSlot);
            mc.interactionManager.clickSlot(syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, CHEST_EQUIPMENT_SLOT, 0, SlotActionType.PICKUP, mc.player);
            if (!chestStack.isEmpty()) {
                mc.interactionManager.clickSlot(syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
            }
        }

        mc.player.getInventory().updateItems();
        this.gearSwapCooldown = GEAR_SWAP_COOLDOWN_TICKS;
        return true;
    }

    private int findBestPickaxeSlot() {
        int bestSlot = -1;
        int bestDurability = -1;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!(stack.getItem() instanceof PickaxeItem) || this.isBelowMinDurability(stack)) continue;
            int durability = this.getRemainingDurability(stack);
            if (durability > bestDurability) {
                bestDurability = durability;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private int findBestElytraSlot() {
        int bestSlot = -1;
        int bestDurability = -1;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!this.isGoodElytra(stack)) continue;
            int durability = this.getRemainingDurability(stack);
            if (durability > bestDurability) {
                bestDurability = durability;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private boolean isGoodElytra(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof ElytraItem
                && ElytraItem.isUsable(stack)
                && !this.isBelowMinDurability(stack);
    }

    private boolean isBelowMinDurability(ItemStack stack) {
        return stack.isDamageable() && this.getRemainingDurabilityPercent(stack) <= this.minDurability.getValueInt();
    }

    private int getRemainingDurabilityPercent(ItemStack stack) {
        if (!stack.isDamageable()) return 100;
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) return 100;
        return Math.max(0, maxDamage - stack.getDamage()) * 100 / maxDamage;
    }

    private int getRemainingDurability(ItemStack stack) {
        if (!stack.isDamageable()) return Integer.MAX_VALUE;
        return Math.max(0, stack.getMaxDamage() - stack.getDamage());
    }

    private int toScreenSlot(int inventorySlot) {
        return inventorySlot < 9 ? inventorySlot + HOTBAR_SCREEN_SLOT_OFFSET : inventorySlot;
    }

    private Direction getMineSide(BlockPos pos) {
        Direction side = BlockUtil.getClickSideStrict(pos);
        if (side == null) return null;
        return this.canReachMineSide(pos, side) ? side : null;
    }

    private boolean canReachMineSide(BlockPos pos, Direction side) {
        Vec3d hitVec = pos.toCenterPos().add(
                side.getVector().getX() * 0.5,
                side.getVector().getY() * 0.5,
                side.getVector().getZ() * 0.5
        );
        double reach = mc.player.getBlockInteractionRange() + REACH_EPSILON;
        return mc.player.getEyePos().squaredDistanceTo(hitVec) <= reach * reach;
    }

    private BlockPos findBlockingBlock(BlockPos target) {
        if (mc.player == null || mc.world == null) return null;
        Vec3d feet = mc.player.getPos();

        if (!mc.player.isFallFlying()) {
            BlockPos takeoffObstacle = this.findTakeoffObstacle(feet);
            if (takeoffObstacle != null) {
                return takeoffObstacle;
            }
        }

        Vec3d dir = this.getMiningDirection(target);
        if (dir.lengthSquared() < 1.0E-4) return null;
        dir = dir.normalize();

        double max = this.mineRange.getValue();
        BlockPos currentSpaceBlock = this.findBodyTunnelObstacle(feet, max);
        if (currentSpaceBlock != null) {
            return currentSpaceBlock;
        }

        Vec3d localRoute = this.findNearbyOpenRoute(feet, dir);
        if (localRoute != null) {
            this.entryWaypoint = localRoute;
            return null;
        }

        Vec3d clearAirWaypoint = this.findClearAirWaypoint(feet, dir, max);
        if (clearAirWaypoint != null) {
            this.entryWaypoint = clearAirWaypoint;
            return null;
        }

        for (double d = 0.5; d <= max; d += 0.5) {
            if (mc.player.isFallFlying() && this.isFlightSpaceClear(feet, dir, d)) {
                continue;
            }
            BlockPos pos = this.findBodyTunnelObstacle(feet.add(dir.multiply(d)), max);
            if (pos != null) {
                return pos;
            }
        }
        return null;
    }

    private Vec3d findClearAirWaypoint(Vec3d feet, Vec3d dir, double distance) {
        Vec3d forward = feet.add(dir.multiply(distance));
        if (this.isBodyPathClear(feet, dir, distance)) {
            return this.toAirWaypoint(forward);
        }

        Vec3d side = new Vec3d(-dir.z, 0.0, dir.x).normalize();
        Vec3d left = forward.add(side.multiply(SIDE_PROBE_DISTANCE));
        if (this.isBodyPathClear(feet.add(side.multiply(SIDE_PROBE_DISTANCE)), dir, distance)) {
            return this.toAirWaypoint(left);
        }

        Vec3d right = forward.add(side.multiply(-SIDE_PROBE_DISTANCE));
        if (this.isBodyPathClear(feet.add(side.multiply(-SIDE_PROBE_DISTANCE)), dir, distance)) {
            return this.toAirWaypoint(right);
        }

        return null;
    }

    private Vec3d findNearbyOpenRoute(Vec3d feet, Vec3d targetDir) {
        Vec3d forward = this.horizontalUnit(targetDir);
        if (forward == null) return null;

        Vec3d side = new Vec3d(-forward.z, 0.0, forward.x).normalize();
        Vec3d[] candidates = new Vec3d[] {
                forward,
                this.horizontalUnit(forward.add(side)),
                this.horizontalUnit(forward.subtract(side)),
                side,
                side.multiply(-1.0),
                this.horizontalUnit(forward.multiply(-1.0).add(side)),
                this.horizontalUnit(forward.multiply(-1.0).subtract(side))
        };

        Vec3d best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Vec3d candidate : candidates) {
            if (candidate == null || !this.isNearbyRouteEnterable(feet, candidate)) continue;

            double clearDistance = this.getClearBodyDistance(feet, candidate, LOCAL_ROUTE_SCAN_DISTANCE);
            if (clearDistance < LOCAL_ROUTE_MIN_CLEAR_DISTANCE) continue;

            double progress = candidate.dotProduct(forward);
            if (progress < LOCAL_ROUTE_MIN_PROGRESS) continue;

            double score = clearDistance + progress * 3.0;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best == null) {
            return null;
        }

        double step = Math.min(LOCAL_ROUTE_SCAN_DISTANCE, Math.max(LOCAL_ROUTE_MIN_CLEAR_DISTANCE, this.getClearBodyDistance(feet, best, LOCAL_ROUTE_SCAN_DISTANCE) - 0.5));
        Vec3d routeFeet = feet.add(best.multiply(step));
        return this.toAirWaypoint(routeFeet);
    }

    private Vec3d toAirWaypoint(Vec3d feetPos) {
        if (mc.player != null && mc.player.isFallFlying()) {
            return feetPos.add(0.0, mc.player.getEyeY() - mc.player.getY(), 0.0);
        }
        return new Vec3d(feetPos.x, MathHelper.floor(feetPos.y) + 1.0, feetPos.z);
    }

    private boolean isNearbyRouteEnterable(Vec3d feet, Vec3d dir) {
        if (!this.isNavigationSpaceClear(feet.add(dir.multiply(LOCAL_ROUTE_ENTRY_DISTANCE)))) {
            return false;
        }

        Vec3d side = new Vec3d(-dir.z, 0.0, dir.x).normalize();
        Vec3d leftEntry = feet.add(dir.multiply(LOCAL_ROUTE_ENTRY_DISTANCE)).add(side.multiply(0.31));
        Vec3d rightEntry = feet.add(dir.multiply(LOCAL_ROUTE_ENTRY_DISTANCE)).add(side.multiply(-0.31));
        return this.isNavigationSpaceClear(leftEntry) && this.isNavigationSpaceClear(rightEntry);
    }

    private double getClearBodyDistance(Vec3d feet, Vec3d dir, double maxDistance) {
        double clear = 0.0;
        for (double d = 0.5; d <= maxDistance; d += 0.5) {
            if (!this.isNavigationSpaceClear(feet.add(dir.multiply(d)))) {
                break;
            }
            clear = d;
        }
        return clear;
    }

    private Vec3d horizontalUnit(Vec3d vec) {
        Vec3d horizontal = new Vec3d(vec.x, 0.0, vec.z);
        if (horizontal.lengthSquared() < 1.0E-4) {
            return null;
        }
        return horizontal.normalize();
    }

    private boolean isBodyPathClear(Vec3d startFeet, Vec3d dir, double distance) {
        if (mc.player != null && mc.player.isFallFlying()) {
            return this.isFlightSpaceClear(startFeet, dir, distance);
        }
        for (double d = 0.5; d <= distance; d += 0.5) {
            if (!this.isBodyTunnelClear(startFeet.add(dir.multiply(d)))) {
                return false;
            }
        }
        return true;
    }

    private boolean isNavigationSpaceClear(Vec3d feetPos) {
        if (mc.player != null && mc.player.isFallFlying()) {
            return this.isFlightCollisionClearAt(feetPos);
        }
        return this.isBodyTunnelClear(feetPos);
    }

    private boolean isBodyTunnelClear(Vec3d feetPos) {
        int x = MathHelper.floor(feetPos.x);
        int minY = MathHelper.floor(feetPos.y);
        int z = MathHelper.floor(feetPos.z);
        for (int y = minY; y < minY + BODY_TUNNEL_HEIGHT; y++) {
            if (this.isMineableObstacle(new BlockPos(x, y, z))) {
                return false;
            }
        }
        return true;
    }

    private Vec3d getTunnelEntryWaypoint(BlockPos target) {
        Vec3d dir = this.getMiningDirection(target);
        if (dir.lengthSquared() < 1.0E-4) {
            dir = mc.player.getRotationVec(1.0F);
        }
        dir = new Vec3d(dir.x, 0.0, dir.z);
        if (dir.lengthSquared() < 1.0E-4) {
            return null;
        }
        dir = dir.normalize();
        Vec3d feet = mc.player.getPos().add(dir.multiply(ENTRY_COMMIT_DISTANCE));
        return new Vec3d(MathHelper.floor(feet.x) + 0.5, MathHelper.floor(feet.y) + 1.0, MathHelper.floor(feet.z) + 0.5);
    }

    private BlockPos findTakeoffObstacle(Vec3d feetPos) {
        int x = MathHelper.floor(feetPos.x);
        int minY = (int)Math.floor(feetPos.y);
        int maxY = minY + TAKEOFF_TUNNEL_HEIGHT - 1;
        int z = MathHelper.floor(feetPos.z);
        BlockPos bestPos = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int y = minY; y <= maxY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!this.isReachableMineableObstacle(pos)) continue;

            double distanceSq = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos());
            if (distanceSq < bestDistanceSq) {
                bestPos = pos;
                bestDistanceSq = distanceSq;
            }
        }

        return bestPos;
    }

    private Vec3d getMiningDirection(BlockPos target) {
        Vec3d aim = this.getAirWaypoint(target);
        Vec3d from = mc.player.getPos().add(0.0, 1.0, 0.0);
        Vec3d dir = aim.subtract(from);
        if (dir.lengthSquared() > 1.0E-4) {
            return dir;
        }
        return mc.player.getRotationVec(1.0F);
    }

    private Vec3d getAirWaypoint(BlockPos target) {
        if (this.entryWaypoint != null) {
            if (mc.player.getPos().squaredDistanceTo(this.entryWaypoint) <= ENTRY_COMMIT_DISTANCE * ENTRY_COMMIT_DISTANCE) {
                this.entryWaypoint = null;
            } else {
                return this.entryWaypoint;
            }
        }

        if (this.mode.is(Mode.path)) {
            Vec3d pathWaypoint = this.getPathModeWaypoint(target);
            if (pathWaypoint != null) {
                return pathWaypoint;
            }
        }

        return this.getClientAirWaypoint(target);
    }

    private Vec3d getClientAirWaypoint(BlockPos target) {
        Vec3d playerPos = mc.player.getPos();
        double cruiseY = this.getCruiseY(target);
        double waypointY = this.getWaypointY(target, playerPos, cruiseY);
        Vec3d targetAir = new Vec3d(target.getX() + 0.5, waypointY, target.getZ() + 0.5);
        double dx = targetAir.x - playerPos.x;
        double dz = targetAir.z - playerPos.z;
        double horizontalSq = dx * dx + dz * dz;
        if (horizontalSq <= TARGET_APPROACH_XZ_SQ) {
            return target.toCenterPos().add(0.0, 0.5, 0.0);
        }

        double horizontal = Math.sqrt(horizontalSq);
        double step = Math.min(AIR_LOOKAHEAD, horizontal);
        return new Vec3d(
                playerPos.x + dx / horizontal * step,
                waypointY,
                playerPos.z + dz / horizontal * step
        );
    }

    private double getWaypointY(BlockPos target, Vec3d playerPos, double cruiseY) {
        if (!this.shouldUseHighAltitudeCruise()) {
            return cruiseY;
        }

        double targetY = target.getY() + 1.5;
        double dx = target.getX() + 0.5 - playerPos.x;
        double dz = target.getZ() + 0.5 - playerPos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal <= HIGH_ALTITUDE_DESCEND_END) {
            return targetY;
        }
        if (horizontal >= HIGH_ALTITUDE_DESCEND_START) {
            return cruiseY;
        }

        double t = (horizontal - HIGH_ALTITUDE_DESCEND_END) / (HIGH_ALTITUDE_DESCEND_START - HIGH_ALTITUDE_DESCEND_END);
        return MathHelper.lerp(t, targetY, cruiseY);
    }

    private Vec3d getPathModeWaypoint(BlockPos target) {
        if (mc.player == null || mc.world == null) return null;
        if (!this.ensurePathContext()) return null;

        this.pathRecalculateTicks--;
        if (this.pathRouteTarget == null
                || !this.pathRouteTarget.equals(target)
                || this.pathRecalculateTicks <= 0
                || this.pathWaypoints.isEmpty()) {
            this.recalculatePathRoute(target);
        }

        if (this.pathWaypoints.isEmpty()) {
            return null;
        }

        Vec3d playerPos = mc.player.getPos();
        while (this.pathWaypointIndex < this.pathWaypoints.size() - 1
                && playerPos.squaredDistanceTo(this.pathWaypoints.get(this.pathWaypointIndex)) <= PATH_WAYPOINT_REACH_SQ) {
            this.pathWaypointIndex++;
        }

        int lookahead = Math.min(this.pathWaypoints.size() - 1, this.pathWaypointIndex + PATH_WAYPOINT_LOOKAHEAD);
        for (int i = this.pathWaypointIndex; i <= lookahead; i++) {
            Vec3d waypoint = this.pathWaypoints.get(i);
            if (playerPos.squaredDistanceTo(waypoint) > PATH_WAYPOINT_REACH_SQ) {
                return waypoint;
            }
        }
        return this.pathWaypoints.get(Math.min(this.pathWaypoints.size() - 1, lookahead));
    }

    private void recalculatePathRoute(BlockPos target) {
        if (this.pathContext == 0L || mc.player == null || mc.world == null) return;

        this.pathRecalculateTicks = PATH_RECALCULATE_TICKS;
        this.pathRouteTarget = target;
        this.syncPathChunksAround(mc.player.getBlockPos(), PATH_SYNC_CHUNK_RADIUS);
        this.syncPathChunksAround(target, PATH_TARGET_SYNC_RADIUS);
        NetherPathfinder.cullFarChunks(this.pathContext, mc.player.getChunkPos().x, mc.player.getChunkPos().z, PATH_CULL_DISTANCE_BLOCKS);

        BlockPos start = mc.player.getBlockPos();
        try {
            PathSegment segment = NetherPathfinder.pathFind(
                    this.pathContext,
                    start.getX(),
                    this.toPathY(start.getY()),
                    start.getZ(),
                    target.getX(),
                    this.toPathY(target.getY()),
                    target.getZ(),
                    true,
                    true,
                    PATH_TIMEOUT_MS,
                    true
            );

            this.pathWaypoints.clear();
            this.pathWaypointIndex = 0;
            if (segment == null || segment.packed == null || segment.packed.length == 0) {
                return;
            }

            for (long packed : segment.packed) {
                this.pathWaypoints.add(this.toPathWaypoint(this.unpackPathBlockPos(packed)));
            }
        } catch (Throwable throwable) {
            this.clearPathRoute();
        }
    }

    private boolean ensurePathContext() {
        if (mc.world == null) return false;
        if (mc.world.getRegistryKey() != World.NETHER) {
            if (!this.pathUnavailableNotified) {
                this.sendMessage("[FastMoveatAi] Path mode is only supported in Nether with the bundled nether-pathfinder jar, falling back to Client mode.");
                this.pathUnavailableNotified = true;
            }
            return false;
        }
        if (!NetherPathfinder.isThisSystemSupported()) {
            if (!this.pathUnavailableNotified) {
                this.sendMessage("[FastMoveatAi] NetherPathfinder native library unavailable, falling back to Client mode.");
                this.pathUnavailableNotified = true;
            }
            return false;
        }

        int dimension = this.getPathDimension();
        int height = this.getPathDimensionHeight(dimension);
        if (this.pathContext != 0L
                && this.pathContextDimension == dimension
                && this.pathContextHeight == height) {
            return true;
        }

        this.releasePathContext();
        try {
            this.pathContext = NetherPathfinder.newContext(0L);
            this.pathContextDimension = dimension;
            this.pathContextHeight = height;
            this.pathUnavailableNotified = false;
            return this.pathContext != 0L;
        } catch (Throwable throwable) {
            this.releasePathContext();
            if (!this.pathUnavailableNotified) {
                this.sendMessage("[FastMoveatAi] Failed to initialize NetherPathfinder, falling back to Client mode.");
                this.pathUnavailableNotified = true;
            }
            return false;
        }
    }

    private int getPathDimension() {
        return 1;
    }

    private int getPathDimensionHeight(int dimension) {
        return 256;
    }

    private int toPathY(int worldY) {
        return MathHelper.clamp(worldY, 0, this.pathContextHeight - 1);
    }

    private int fromPathY(int pathY) {
        return pathY;
    }

    private BlockPos unpackPathBlockPos(long packed) {
        int x = unpackSigned(packed >> BLOCKPOS_X_SHIFT, BLOCKPOS_XZ_BITS);
        int y = unpackSigned(packed >> BLOCKPOS_Y_SHIFT, BLOCKPOS_Y_BITS);
        int z = unpackSigned(packed, BLOCKPOS_XZ_BITS);
        return new BlockPos(x, this.fromPathY(y), z);
    }

    private static int unpackSigned(long value, int bits) {
        int shift = 64 - bits;
        return (int)(value << shift >> shift);
    }

    private Vec3d toPathWaypoint(BlockPos pos) {
        double y = pos.getY() + 0.5;
        if (mc.player != null && mc.player.isFallFlying()) {
            y = pos.getY() + (mc.player.getEyeY() - mc.player.getY());
        }
        return new Vec3d(pos.getX() + 0.5, y, pos.getZ() + 0.5);
    }

    private void syncPathChunksAround(BlockPos center, int radius) {
        if (this.pathContext == 0L || mc.world == null) return;
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                this.syncPathChunk(chunkX, chunkZ);
            }
        }
    }

    private void syncPathChunk(int chunkX, int chunkZ) {
        long key = packChunkKey(chunkX, chunkZ);
        if (!mc.world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }

        WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);
        if (chunk == null) return;

        boolean[] data = new boolean[16 * 16 * this.pathContextHeight];
        for (int y = 0; y < this.pathContextHeight; y++) {
            int worldY = this.fromPathY(y);
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockPos pos = new BlockPos((chunkX << 4) + x, worldY, (chunkZ << 4) + z);
                    data[(y << 8) | (z << 4) | x] = this.isPathfinderSolid(pos);
                }
            }
        }

        try {
            NetherPathfinder.insertChunkData(this.pathContext, chunkX, chunkZ, data);
            this.pathSyncedChunks.add(key);
        } catch (Throwable ignored) {
        }
    }

    private void invalidatePathChunk(BlockPos pos) {
        if (this.pathContext == 0L) return;
        this.pathSyncedChunks.remove(packChunkKey(pos.getX() >> 4, pos.getZ() >> 4));
        this.pathRecalculateTicks = 0;
    }

    private boolean isPathfinderSolid(BlockPos pos) {
        if (mc.world == null) return true;
        BlockState state = mc.world.getBlockState(pos);
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.getCollisionShape(mc.world, pos).isEmpty();
    }

    private static long packChunkKey(int chunkX, int chunkZ) {
        return ((long)chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private void clearPathRoute() {
        this.pathRouteTarget = null;
        this.pathRecalculateTicks = 0;
        this.pathWaypointIndex = 0;
        this.pathWaypoints.clear();
    }

    private void releasePathContext() {
        if (this.pathContext != 0L) {
            try {
                NetherPathfinder.cancel(this.pathContext);
                NetherPathfinder.freeContext(this.pathContext);
            } catch (Throwable ignored) {
            }
        }
        this.pathContext = 0L;
        this.pathContextDimension = -1;
        this.pathContextHeight = 256;
        this.pathSyncedChunks.clear();
        this.clearPathRoute();
    }

    private BlockPos findBodyTunnelObstacle(Vec3d feetPos, double maxReach) {
        if (mc.player != null && mc.player.isFallFlying()) {
            return this.findFlightObstacle(feetPos, maxReach);
        }

        int x = MathHelper.floor(feetPos.x);
        int minY = (int)Math.floor(feetPos.y);
        int maxY = (int)Math.floor(feetPos.y + BODY_TUNNEL_HEIGHT - 0.01);
        int z = MathHelper.floor(feetPos.z);
        double maxDistanceSq = (maxReach + 0.75) * (maxReach + 0.75);
        BlockPos bestPos = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int y = minY; y <= maxY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!this.isReachableMineableObstacle(pos)) continue;

            double distanceSq = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos());
            if (distanceSq <= maxDistanceSq && distanceSq < bestDistanceSq) {
                bestPos = pos;
                bestDistanceSq = distanceSq;
            }
        }

        return bestPos;
    }

    private BlockPos findFlightObstacle(Vec3d feetPos, double maxReach) {
        if (this.isFlightCollisionClearAt(feetPos)) {
            return null;
        }

        Box box = this.getPlayerBoxAt(feetPos);
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX - 1.0E-7);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.floor(box.maxY - 1.0E-7);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ - 1.0E-7);
        double maxDistanceSq = (maxReach + 0.75) * (maxReach + 0.75);
        BlockPos bestPos = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!this.isReachableMineableObstacle(pos)) continue;
                    if (!this.blockIntersectsBox(pos, box)) continue;

                    double distanceSq = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos());
                    if (distanceSq <= maxDistanceSq && distanceSq < bestDistanceSq) {
                        bestPos = pos;
                        bestDistanceSq = distanceSq;
                    }
                }
            }
        }

        return bestPos;
    }

    private boolean isFlightSpaceClear(Vec3d startFeet, Vec3d dir, double distance) {
        Vec3d horizontal = this.horizontalUnit(dir);
        if (horizontal == null) {
            return this.isFlightCollisionClearAt(startFeet);
        }

        for (double d = FLIGHT_COLLISION_STEP; d <= distance; d += FLIGHT_COLLISION_STEP) {
            if (!this.isFlightCollisionClearAt(startFeet.add(horizontal.multiply(d)))) {
                return false;
            }
        }
        return this.isFlightCollisionClearAt(startFeet.add(horizontal.multiply(distance)));
    }

    private boolean isFlightCollisionClearAt(Vec3d feetPos) {
        if (mc.player == null || mc.world == null) return false;
        Box box = this.getPlayerBoxAt(feetPos).expand(-FLIGHT_COLLISION_MARGIN);
        return mc.world.getBlockCollisions(mc.player, box).iterator().hasNext() == false;
    }

    private Box getPlayerBoxAt(Vec3d feetPos) {
        Box current = mc.player.getBoundingBox();
        return current.offset(
                feetPos.x - mc.player.getX(),
                feetPos.y - current.minY,
                feetPos.z - mc.player.getZ()
        );
    }

    private boolean blockIntersectsBox(BlockPos pos, Box box) {
        BlockState state = mc.world.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(mc.world, pos);
        return !shape.isEmpty() && shape.getBoundingBox().offset(pos).intersects(box);
    }

    private boolean isMineableObstacle(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        if (state.getHardness(mc.world, pos) < 0.0F) return false;
        return !state.getCollisionShape(mc.world, pos).isEmpty()
                || state.isSolidBlock(mc.world, pos);
    }

    private boolean isReachableMineableObstacle(BlockPos pos) {
        return this.isMineableObstacle(pos) && this.getMineSide(pos) != null;
    }

    private BlockPos targetFromSettings() {
        return new BlockPos(this.targetX.getValueInt(), this.targetY.getValueInt(), this.targetZ.getValueInt());
    }

    private void resetState() {
        this.pathStarted = false;
        this.inactiveTicks = 0;
        this.activeTarget = null;
        this.axisStage = AxisStage.Z;
        this.axisLockedX = 0;
        this.axisLockedZ = 0;
        this.hasCruiseY = false;
        this.cruiseY = 0.0;
        this.entryWaypoint = null;
        this.clearPathRoute();
        this.gearSwapCooldown = 0;
        this.lastProgressPos = null;
        this.stuckTicks = 0;
        this.groundMiningElytraDisabled = false;
        this.emergencyGappleActive = false;
        this.eatingSuppressTicks = 0;
    }

    private static String format(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    public enum StartResult {
        STARTED,
        ARRIVED,
        FAILED
    }

    public enum Mode {
        Client,
        path
    }

    public enum AxisPriority {
        Z,
        X
    }

    private enum AxisStage {
        Z,
        X,
        Final
    }
}
