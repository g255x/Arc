package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.combat.CombatUtil;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PistonCrystalPlus extends Module {
    public static PistonCrystalPlus INSTANCE;
    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 10.0, 1.0, 20.0, 0.1));
    private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 10.0, 0.1));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 1000));
    private final SliderSetting breakDelay = this.add(new SliderSetting("BreakDelay", 50, 0, 500));
    private final SliderSetting minDamage = this.add(new SliderSetting("MinDamage", 6.0, 1.0, 20.0, 0.5));
    private final SliderSetting maxSelfDmg = this.add(new SliderSetting("MaxSelfDmg", 8.0, 0.0, 20.0, 0.5));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false));
    private final BooleanSetting onlyMain = this.add(new BooleanSetting("OnlyMain", true));
    private final BooleanSetting noSuicide = this.add(new BooleanSetting("NoSuicide", true));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    private final BooleanSetting inventory = this.add(new BooleanSetting("Inventory", false));
    private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
    private final BooleanSetting yawDeceive = this.add(new BooleanSetting("YawDeceive", false));
    private final EnumSetting<RedstoneMode> redStoneMode = this.add(new EnumSetting<>("RedStoneMode", RedstoneMode.Torch));
    private final BooleanSetting render = this.add(new BooleanSetting("Render", true));
    private final ColorSetting crystalColor = this.add(new ColorSetting("CrystalColor", new Color(218, 112, 214)));
    private final ColorSetting pistonColor = this.add(new ColorSetting("PistonColor", new Color(150, 150, 150)));
    private final ColorSetting redstoneColor = this.add(new ColorSetting("RedstoneColor", new Color(200, 50, 50)));
    private long lastAction;
    private BlockPos crystalPos, pistonPos, redstonePos;
    private BlockPos lastPiston, lastRedstone, lastCrystal;
    private Direction face;
    private final Timer breakTimer = new Timer();
    private final Map<Integer, Timer> friendTimers = new HashMap<>();
    private PlayerEntity target;

    public PistonCrystalPlus() {
        super("PistonCrystal+", "Enhanced piston crystal placement", Module.Category.Combat);
        this.setChinese("活塞水晶+");
        INSTANCE = this;
    }

    @Override public void onEnable() { target = null; crystalPos = null; pistonPos = null; redstonePos = null; }
    @Override public void onDisable() { target = null; }
    @Override public String getInfo() { return target != null ? target.getGameProfile().getName() : null; }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;
        if (usingPause.getValue() && mc.player.isUsingItem()) return;
        if (System.currentTimeMillis() - lastAction < delay.getValue()) return;

        target = CombatUtil.getClosestEnemy(targetRange.getValue());
        if (target == null) return;
        doPistonCrystal(target);
        lastAction = System.currentTimeMillis();
    }

    @EventListener
    public void onRender3D(MatrixStack matrixStack) {
        if (!render.getValue() || nullCheck()) return;
        if (crystalPos != null) dev.kissoo.api.utils.render.Render3DUtil.drawFill(matrixStack,
                new Box(crystalPos), crystalColor.getValue());
        if (pistonPos != null) dev.kissoo.api.utils.render.Render3DUtil.drawFill(matrixStack,
                new Box(pistonPos), pistonColor.getValue());
        if (redstonePos != null) dev.kissoo.api.utils.render.Render3DUtil.drawFill(matrixStack,
                new Box(redstonePos), redstoneColor.getValue());
    }

    void doPistonCrystal(PlayerEntity targetPlayer) {
        BlockPos targetPos = targetPlayer.getBlockPos().up();
        int crystalSlot = InventoryUtil.findItem(Items.END_CRYSTAL);
        BlockPos bestCrystal = null, bestPiston = null, bestRedstone = null;
        double bestDamage = minDamage.getValue();
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || dir == Direction.UP) continue;
            BlockPos pistonCandidate = targetPos.offset(dir.getOpposite(), 2);
            BlockPos crystalCandidate = targetPos.offset(dir.getOpposite());
            if (!mc.world.isAir(pistonCandidate)) continue;
            if (!mc.world.isAir(crystalCandidate)) continue;
            double selfDamage = mc.player.squaredDistanceTo(crystalCandidate.toCenterPos()) <= range.getValue() * range.getValue() ? 1 : 0;
            if (selfDamage <= maxSelfDmg.getValue() && mc.player.squaredDistanceTo(crystalCandidate.toCenterPos()) <= range.getValue() * range.getValue()) {
                bestDamage = 6.0; bestPiston = pistonCandidate; bestCrystal = crystalCandidate;
                bestRedstone = bestCrystal.offset(dir.getOpposite()); face = dir;
                break;
            }
        }
        if (bestCrystal != null) {
            crystalPos = bestCrystal; pistonPos = bestPiston; redstonePos = bestRedstone;
            int oldSlot = mc.player.getInventory().selectedSlot;
            place(bestPiston, face);
            placeCrystal(bestCrystal, crystalSlot);
            if (redStoneMode.getValue() == RedstoneMode.Torch) placeTorch(bestRedstone);
            if (mc.player.getInventory().selectedSlot != oldSlot) InventoryUtil.switchToSlot(oldSlot);
        }
    }

    void place(BlockPos pos, Direction dir) {
        if (shouldPause()) return;
        Vec3d hitVec = pos.toCenterPos().add(dir.getVector().getX() * 0.5, 0.5, dir.getVector().getZ() * 0.5);
        BlockHitResult result = new BlockHitResult(hitVec, dir.getOpposite(), pos.offset(dir.getOpposite()), false);
        if (rotate.getValue()) {
            float[] angles = dev.kissoo.core.impl.RotationManager.getRotation(mc.player.getEyePos(), hitVec);
            Forever.ROTATION.snapAt(angles[0], angles[1]);
        }
        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
        if (rotate.getValue()) Forever.ROTATION.snapBack();
    }

    boolean shouldPause() { return false; }
    public boolean checkPause(boolean isPiston) { return false; }
    public void pistonFacing(Direction dir) { this.face = dir; }

    void placeCrystal(BlockPos pos, int slot) {
        doSwap(slot);
        BlockHitResult result = new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false);
        if (rotate.getValue()) {
            float[] angles = dev.kissoo.core.impl.RotationManager.getRotation(mc.player.getEyePos(), pos.toCenterPos());
            Forever.ROTATION.snapAt(angles[0], angles[1]);
        }
        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
        if (rotate.getValue()) Forever.ROTATION.snapBack();
    }

    void placeTorch(BlockPos pos) {
        int torchSlot = InventoryUtil.findBlock(Blocks.REDSTONE_TORCH);
        if (torchSlot == -1) return;
        doSwap(torchSlot);
        BlockHitResult result = new BlockHitResult(pos.toCenterPos(), Direction.UP, pos.down(), false);
        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
    }

    void doSwap(int slot) {
        if (inventory.getValue()) InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        else InventoryUtil.switchToSlot(slot);
    }

    public enum RedstoneMode { Torch, Block, None }
}
