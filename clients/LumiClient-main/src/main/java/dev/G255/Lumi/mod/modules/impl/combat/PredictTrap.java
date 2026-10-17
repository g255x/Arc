package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.RotationEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.math.PredictUtil;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.world.BlockPosX;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.exploit.Blink;
import dev.G255.Lumi.mod.modules.impl.movement.ElytraFly;
import dev.G255.Lumi.mod.modules.impl.movement.Velocity;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PredictTrap extends Module {
    public static PredictTrap INSTANCE;

    public final EnumSetting<Page> page = this.add(new EnumSetting<>("Page", Page.General));
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.FastTrap, () -> this.page.getValue() == Page.General));
    private final EnumSetting<TargetMode> targetMod = this.add(new EnumSetting<>("TargetMode", TargetMode.Single, () -> this.page.getValue() == Page.General));
    private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 8.0, () -> this.page.getValue() == Page.General).setSuffix("m"));
    private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 4.0, 1.0, 6.0, () -> this.page.getValue() == Page.General).setSuffix("m"));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 500, () -> this.page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8, () -> this.page.getValue() == Page.General));
    private final SliderSetting predictTicks = this.add(new SliderSetting("PredictTicks", 2.0, 0.0, 50.0, 1.0, () -> this.page.getValue() == Page.General));
    private final SliderSetting layers = this.add(new SliderSetting("Layers", 2.0, 1.0, 5.0, 1.0, () -> this.page.getValue() == Page.General));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", false, () -> this.page.getValue() == Page.General));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.getValue() == Page.General));
    private final BooleanSetting breakCrystal = this.add(new BooleanSetting("Break", true, () -> this.page.getValue() == Page.General));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.getValue() == Page.General));
    private final BooleanSetting selfGround = this.add(new BooleanSetting("SelfGround", true, () -> this.page.getValue() == Page.General));
    private final BooleanSetting helper = this.add(new BooleanSetting("Helper", true, () -> this.page.getValue() == Page.General));
    private final BooleanSetting rotate = this.add((new BooleanSetting("Rotate", true, () -> this.page.getValue() == Page.Rotate)).setParent());
    private final BooleanSetting yawStep = this.add((new BooleanSetting("YawStep", true, () -> this.rotate.isOpen() && this.page.getValue() == Page.Rotate)).setParent());
    private final BooleanSetting whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == Page.Rotate));
    private final SliderSetting steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == Page.Rotate));
    private final BooleanSetting checkFov = this.add((new BooleanSetting("OnlyLooking", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == Page.Rotate)).setParent());
    private final SliderSetting fov = this.add(new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.checkFov.isOpen() && this.page.getValue() == Page.Rotate && this.yawStep.isOpen()));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == Page.Rotate));

    private final Timer timer = new Timer();
    private final ArrayList<BlockPos> placeList = new ArrayList<>();
    public PlayerEntity target;
    public Vec3d directionVec = null;
    private int progress = 0;

    public PredictTrap() {
        super("PredictTrap", "", Category.Combat);
        this.setChinese("");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.placeList.clear();
        this.progress = 0;
        this.target = null;

        if (this.selfGround.getValue() && !mc.player.isOnGround()) {
            return;
        }
        if (this.inventory.getValue() && !EntityUtil.inInventory()) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }
        if (this.usingPause.getValue() && mc.player.isUsingItem()) {
            return;
        }
        if (!this.timer.passed((long)this.delay.getValue())) {
            return;
        }
        if (this.getAnchor() == -1) {
            if (this.autoDisable.getValue()) {
                this.disable();
            }
            return;
        }

        if (this.targetMod.is(TargetMode.Single)) {
            this.target = CombatUtil.getClosestEnemy(this.range.getValue());
            if (this.target == null) {
                if (this.autoDisable.getValue()) {
                    this.disable();
                }
                return;
            }
            this.trapTarget(this.target);
        } else {
            boolean found = false;
            for (PlayerEntity player : CombatUtil.getEnemies(this.range.getValue())) {
                found = true;
                this.target = player;
                this.trapTarget(this.target);
            }
            if (!found && this.autoDisable.getValue()) {
                this.disable();
                this.target = null;
            }
        }
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null) {
            event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
        }
    }

    private void trapTarget(PlayerEntity target) {
        BlockPos targetPos = getTargetPosition(target);
        int headOffset = target.isCrawling() ? 1 : 2;

        if (this.isSelfTrapped(targetPos, headOffset)) {
            this.fillGaps(targetPos, headOffset);
        }

        this.buildLayer(targetPos, headOffset);
    }

    private BlockPos getTargetPosition(PlayerEntity target) {
        if (this.mode.is(Mode.PathPredict)) {
            Vec3d predicted = this.predictTicks.getValue() > 0.0
                ? PredictUtil.getPos(target, this.predictTicks.getValueInt())
                : target.getPos();
            return new BlockPosX(predicted.getX(), predicted.getY(), predicted.getZ());
        }
        return EntityUtil.getEntityPos(target);
    }

    private boolean isSelfTrapped(BlockPos feetPos, int headOffset) {
        int surrounded = 0;
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (Direction dir : dirs) {
            BlockPos checkPos = feetPos.offset(dir).up(headOffset);
            Block block = BlockUtil.getBlock(checkPos);
            if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK || block == Blocks.RESPAWN_ANCHOR) {
                surrounded++;
            }
        }
        return surrounded >= 2;
    }

    private void fillGaps(BlockPos feetPos, int headOffset) {
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

        for (int y = 0; y <= headOffset; y++) {
            for (Direction dir : dirs) {
                BlockPos checkPos = feetPos.up(y).offset(dir);
                if (BlockUtil.clientCanPlace(checkPos, this.breakCrystal.getValue())) {
                    this.tryPlaceAnchor(checkPos);
                }
            }
        }
    }

    private void buildLayer(BlockPos feetPos, int headOffset) {
        int maxLayers = this.layers.getValueInt();
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

        for (int layer = 0; layer < maxLayers; layer++) {
            int r = layer + 1;

            for (int y = 0; y <= headOffset; y++) {
                BlockPos centerY = feetPos.up(y);

                for (Direction dir : dirs) {
                    BlockPos placePos = centerY.offset(dir, r);
                    this.tryPlaceAnchor(placePos);
                }

                if (r >= 2) {
                    for (int i = 0; i < 4; i++) {
                        Direction d1 = dirs[i];
                        Direction d2 = dirs[(i + 1) % 4];
                        BlockPos diagPos = centerY.offset(d1, r).offset(d2, r - 1);
                        this.tryPlaceAnchor(diagPos);
                    }
                }
            }
        }

        for (int layer = 0; layer < maxLayers; layer++) {
            int r = layer + 1;
            BlockPos topPos = feetPos.up(headOffset + 1);
            for (Direction dir : dirs) {
                this.tryPlaceAnchor(topPos.offset(dir, r));
            }
            this.tryPlaceAnchor(topPos.offset(Direction.NORTH, r).offset(Direction.EAST, r));
            this.tryPlaceAnchor(topPos.offset(Direction.NORTH, r).offset(Direction.WEST, r));
            this.tryPlaceAnchor(topPos.offset(Direction.SOUTH, r).offset(Direction.EAST, r));
            this.tryPlaceAnchor(topPos.offset(Direction.SOUTH, r).offset(Direction.WEST, r));
        }
    }

    private void tryPlaceAnchor(BlockPos pos) {
        if (this.placeList.contains(pos)) {
            return;
        }
        if (Lumi.BREAK.isMining(pos)) {
            return;
        }
        if (AutoAnchor.INSTANCE.isOn() && pos.equals(AutoAnchor.INSTANCE.currentPos)) {
            return;
        }
        if (!((double)this.progress < this.blocksPer.getValue())) {
            return;
        }
        if (mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > this.placeRange.getValue() * this.placeRange.getValue()) {
            return;
        }
        if (!BlockUtil.canReplace(pos)) {
            return;
        }
        if (BlockUtil.hasEntity(pos, this.breakCrystal.getValue())) {
            return;
        }

        if (BlockUtil.getPlaceSide(pos) != null) {
            this.doPlace(pos);
        } else if (this.helper.getValue()) {
            BlockPos helperPos = this.getHelper(pos);
            if (helperPos != null) {
                this.tryPlaceObsidian(helperPos);
                this.doPlace(pos);
            }
        }
    }

    private void tryPlaceObsidian(BlockPos pos) {
        if (pos == null) return;
        if (this.placeList.contains(pos)) return;
        if (Lumi.BREAK.isMining(pos)) return;
        if (!BlockUtil.canPlace(pos, this.placeRange.getValue(), this.breakCrystal.getValue())) return;

        int obsidian = this.getObsidian();
        if (obsidian == -1) return;

        this.placeList.add(pos);
        CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());

        int old = mc.player.getInventory().selectedSlot;
        this.doSwap(obsidian);
        this.place(pos);
        if (this.inventory.getValue()) {
            this.doSwap(obsidian);
            EntityUtil.syncInventory();
        } else {
            this.doSwap(old);
        }
        this.timer.reset();
    }

    private void doPlace(BlockPos pos) {
        this.placeList.add(pos);
        CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());

        int anchor = this.getAnchor();
        int old = mc.player.getInventory().selectedSlot;
        this.doSwap(anchor);
        this.place(pos);
        if (this.inventory.getValue()) {
            this.doSwap(anchor);
            EntityUtil.syncInventory();
        } else {
            this.doSwap(old);
        }
        this.timer.reset();
        ++this.progress;
    }

    private void place(BlockPos pos) {
        if (BlockUtil.allowAirPlace()) {
            BlockUtil.placedPos.add(pos);
            Direction side = BlockUtil.getClickSide(pos);
            if (side == null) return;
            Vec3d directionVec = new Vec3d((double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5, (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5, (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5);
            if (this.rotate.getValue() && !this.faceVector(directionVec)) return;
            mc.player.swingHand(Hand.MAIN_HAND);
            BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
            Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
            mc.itemUseCooldown = 4;
            if (this.rotate.getValue() && !this.shouldYawStep()) {
                Lumi.ROTATION.snapBack();
            }
            return;
        }
        Direction side = BlockUtil.getPlaceSide(pos);
        if (side == null) return;
        BlockUtil.placedPos.add(pos);
        Vec3d directionVec = new Vec3d((double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5, (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5, (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5);
        if (this.rotate.getValue() && !this.faceVector(directionVec)) return;
            mc.player.swingHand(Hand.MAIN_HAND);
        BlockHitResult result = new BlockHitResult(directionVec, side.getOpposite(), pos.offset(side), false);
        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
        mc.itemUseCooldown = 4;
        if (this.rotate.getValue() && !this.shouldYawStep()) {
            Lumi.ROTATION.snapBack();
        }
    }

    private BlockPos getHelper(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (i == Direction.DOWN || i == Direction.UP) continue;
            BlockPos offset = pos.offset(i);
            if (Lumi.BREAK.isMining(offset)) continue;
            if (!BlockUtil.isStrictDirection(offset, i.getOpposite())) continue;
            if (!BlockUtil.canPlace(offset, this.placeRange.getValue(), this.breakCrystal.getValue())) continue;
            return offset;
        }
        return null;
    }

    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getAnchor() {
        if (this.inventory.getValue()) {
            return InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR);
        }
        return InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
    }

    private int getObsidian() {
        if (this.inventory.getValue()) {
            return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
        }
        return InventoryUtil.findBlock(Blocks.OBSIDIAN);
    }

    private boolean shouldYawStep() {
        if (!this.whenElytra.getValue() && (mc.player.isFallFlying() || ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.isFallFlying())) {
            return false;
        }
        return this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
    }

    public boolean faceVector(Vec3d directionVec) {
        if (!this.shouldYawStep()) {
            Lumi.ROTATION.lookAt(directionVec);
            return true;
        }
        this.directionVec = directionVec;
        return true;
    }

    @Override
    public String getInfo() {
        if (this.target != null) {
            return this.target.getName().getString();
        }
        return null;
    }

    public enum Page {
        General,
        Rotate
    }

    public enum Mode {
        PathPredict,
        FastTrap
    }

    public enum TargetMode {
        Single,
        Multi
    }
}