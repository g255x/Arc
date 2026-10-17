package dev.perish.mod.modules.impl.combat;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClientTickEvent;
import dev.perish.api.events.impl.Render3DEvent;
import dev.perish.api.events.impl.RotationEvent;
import dev.perish.api.utils.combat.CombatUtil;
import dev.perish.api.utils.math.ExplosionUtil;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.exploit.Blink;
import dev.perish.mod.modules.impl.movement.ElytraFly;
import dev.perish.mod.modules.impl.movement.Velocity;
import dev.perish.mod.modules.settings.enums.Timing;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Hand;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AntiAnchor extends Module {
    public static AntiAnchor INSTANCE;
    private final EnumSetting<AntiAnchor.Page> page = this.add(new EnumSetting("Page", AntiAnchor.Page.General));
    // General
    private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> this.page.is(AntiAnchor.Page.General)).setSuffix("ms"));
    private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 2, 1, 8, () -> this.page.is(AntiAnchor.Page.General)));
    private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 6.0, 0.1, () -> this.page.is(AntiAnchor.Page.General)).setSuffix("m"));
    private final EnumSetting<Timing> timing = this.add(new EnumSetting("Timing", Timing.All, () -> this.page.is(AntiAnchor.Page.General)));
    private final BooleanSetting packetPlace = this.add(new BooleanSetting("PacketPlace", true, () -> this.page.is(AntiAnchor.Page.General)));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.is(AntiAnchor.Page.General)));
    private final BooleanSetting enderChest = this.add(new BooleanSetting("EnderChest", true, () -> this.page.is(AntiAnchor.Page.General)));
    private final BooleanSetting breakCrystal = this.add(new BooleanSetting("Break", true, () -> this.page.is(AntiAnchor.Page.General)).setParent());
    private final BooleanSetting eatPause = this.add(
            new BooleanSetting("EatingPause", true, () -> this.page.is(AntiAnchor.Page.General) && this.breakCrystal.isOpen())
    );
    // Check
    private final SliderSetting selfDamage = this.add(new SliderSetting("SelfDamage", 6.0, 0.0, 36.0, 0.1, () -> this.page.is(AntiAnchor.Page.Check)).setSuffix("dmg"));
    private final BooleanSetting onlyEnemy = this.add(new BooleanSetting("OnlyEnemy", true, () -> this.page.is(AntiAnchor.Page.Check)));
    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 8.0, 0.1, 12.0, 0.1, () -> this.page.is(AntiAnchor.Page.Check) && this.onlyEnemy.getValue()).setSuffix("m"));
    private final BooleanSetting sticky = this.add(new BooleanSetting("StickyFill", true, () -> this.page.is(AntiAnchor.Page.Check)).setParent());
    private final SliderSetting stickyTime = this.add(new SliderSetting("StickyTime", 5.0, 0.1, 30.0, 0.1, () -> this.page.is(AntiAnchor.Page.Check) && this.sticky.isOpen()).setSuffix("s"));
    private final BooleanSetting inAir = this.add(new BooleanSetting("InAir", true, () -> this.page.is(AntiAnchor.Page.Check)));
    private final BooleanSetting detectMining = this.add(new BooleanSetting("DetectMining", false, () -> this.page.is(AntiAnchor.Page.Check)));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.is(AntiAnchor.Page.Check)));
    // Rotate
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == AntiAnchor.Page.Rotate).setParent());
    private final BooleanSetting yawStep = this.add(
            new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.getValue() == AntiAnchor.Page.Rotate).setParent()
    );
    private final BooleanSetting whenElytra = this.add(
            new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AntiAnchor.Page.Rotate)
    );
    private final SliderSetting steps = this.add(
            new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.page.getValue() == AntiAnchor.Page.Rotate && this.yawStep.isOpen())
    );
    private final BooleanSetting checkFov = this.add(
            new BooleanSetting("OnlyLooking", true, () -> this.page.getValue() == AntiAnchor.Page.Rotate && this.yawStep.isOpen()).setParent()
    );
    private final SliderSetting fov = this.add(
            new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.checkFov.isOpen() && this.page.getValue() == AntiAnchor.Page.Rotate && this.yawStep.isOpen())
    );
    private final SliderSetting priority = this.add(
            new SliderSetting("Priority", 10, 0, 100, () -> this.page.getValue() == AntiAnchor.Page.Rotate && this.yawStep.isOpen())
    );
    // Render
    private final BooleanSetting render = this.add(new BooleanSetting("Render", true, () -> this.page.is(AntiAnchor.Page.Render)).setParent());
    private final ColorSetting box = this.add(new ColorSetting("Box", new Color(255, 40, 40, 255), () -> this.page.is(AntiAnchor.Page.Render) && this.render.isOpen()).injectBoolean(true));
    private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(255, 40, 40, 60), () -> this.page.is(AntiAnchor.Page.Render) && this.render.isOpen()).injectBoolean(true));

    private final Timer timer = new Timer();
    private final Timer stickyTimer = new Timer();
    private boolean stickyActive = false;
    private final List<BlockPos> dangerList = new ArrayList<>();
    public Vec3d directionVec = null;
    private int progress = 0;

    public AntiAnchor() {
        super("AntiAnchor", "Fills anchor damage spots around you with Obsidian", Module.Category.Combat);
        this.setChinese("反重生锚");
        INSTANCE = this;
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (this.directionVec != null && this.rotate.getValue() && this.shouldYawStep()) {
            event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
        }
    }

    @Override
    public void onDisable() {
        this.dangerList.clear();
        this.directionVec = null;
        this.stickyActive = false;
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck()) {
            return;
        }
        if (this.timing.is(Timing.Pre) && event.isPost() || this.timing.is(Timing.Post) && event.isPre()) {
            return;
        }
        if (this.inventory.getValue() && !EntityUtil.inInventory()) {
            return;
        }
        this.directionVec = null;
        this.progress = 0;
        this.calcDanger();
        if (this.getBlock() == -1) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }
        if (this.usingPause.getValue() && mc.player.isUsingItem()) {
            return;
        }
        if (!this.inAir.getValue() && !mc.player.isOnGround()) {
            return;
        }
        if (!this.timer.passed((long) this.placeDelay.getValue())) {
            return;
        }
        for (BlockPos pos : this.dangerList) {
            this.tryPlaceBlock(pos);
        }
    }

    // 计算周围哪些空位一旦被敌人放置重生锚就会对我造成致命伤害
    private void calcDanger() {
        this.dangerList.clear();
        // 一旦附近出现真实的重生锚方块就锁定，之后即使锚消失/敌人离开也继续填充，直到超时解除
        if (BlockUtil.getBlock(Blocks.RESPAWN_ANCHOR, this.range.getValueFloat()) != null) {
            this.stickyTimer.reset();
            this.stickyActive = this.sticky.getValue();
        } else if (this.stickyActive && this.stickyTimer.passedS(this.stickyTime.getValue())) {
            this.stickyActive = false;
        }
        if (this.onlyEnemy.getValue() && !this.stickyActive && CombatUtil.getEnemies(this.targetRange.getValue()).isEmpty()) {
            return;
        }
        for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {
            if (BlockUtil.getBlock(pos) == Blocks.OBSIDIAN || BlockUtil.getBlock(pos) == Blocks.BEDROCK) {
                continue;
            }
            if (!BlockUtil.canPlace(pos, this.range.getValue(), true)) {
                continue;
            }
            double damage = ExplosionUtil.anchorDamage(pos, mc.player, mc.player);
            if (damage >= this.selfDamage.getValue()) {
                this.dangerList.add(pos);
            }
        }
    }

    private void tryPlaceBlock(BlockPos pos) {
        if (pos == null || this.progress >= this.blocksPer.getValue()) {
            return;
        }
        if (this.detectMining.getValue() && Perish.BREAK.isMining(pos)) {
            return;
        }
        int block = this.getBlock();
        if (block == -1) {
            return;
        }
        Direction side = BlockUtil.getPlaceSide(pos);
        if (side == null) {
            return;
        }
        if (!BlockUtil.canPlace(pos, this.range.getValue(), true)) {
            return;
        }
        Vec3d directionVec = new Vec3d(
                pos.getX() + 0.5 + side.getVector().getX() * 0.5,
                pos.getY() + 0.5 + side.getVector().getY() * 0.5,
                pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
        );
        if (this.rotate.getValue() && !this.faceVector(directionVec)) {
            return;
        }
        if (this.breakCrystal.getValue()) {
            CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.eatPause.getValue());
        } else if (BlockUtil.hasEntity(pos, false)) {
            return;
        }

        int old = mc.player.getInventory().selectedSlot;
        this.doSwap(block);
        BlockUtil.placedPos.add(pos);
        if (BlockUtil.allowAirPlace()) {
            BlockUtil.airPlace(pos, false, Hand.MAIN_HAND, this.packetPlace.getValue());
        } else {
            BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), false, Hand.MAIN_HAND, this.packetPlace.getValue());
        }

        this.timer.reset();
        if (this.inventory.getValue()) {
            this.doSwap(block);
            EntityUtil.syncInventory();
        } else {
            this.doSwap(old);
        }

        if (this.rotate.getValue() && !this.shouldYawStep()) {
            Perish.ROTATION.snapBack();
        }

        this.progress++;
    }

    private boolean faceVector(Vec3d directionVec) {
        if (!this.shouldYawStep()) {
            Perish.ROTATION.lookAt(directionVec);
            return true;
        }
        this.directionVec = directionVec;
        return Perish.ROTATION.inFov(directionVec, this.fov.getValueFloat()) || !this.checkFov.getValue();
    }

    private boolean shouldYawStep() {
        return (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying()))
                && this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
    }

    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getBlock() {
        if (this.inventory.getValue()) {
            return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) == -1 && this.enderChest.getValue()
                    ? InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST)
                    : InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
        } else {
            return InventoryUtil.findBlock(Blocks.OBSIDIAN) == -1 && this.enderChest.getValue()
                    ? InventoryUtil.findBlock(Blocks.ENDER_CHEST)
                    : InventoryUtil.findBlock(Blocks.OBSIDIAN);
        }
    }

    @EventListener
    public void onRender(Render3DEvent event) {
        if (!this.render.getValue() || this.dangerList.isEmpty()) {
            return;
        }
        for (BlockPos pos : this.dangerList) {
            Box cbox = new Box(pos);
            if (this.fill.booleanValue) {
                event.drawFill(cbox, this.fill.getValue());
            }
            if (this.box.booleanValue) {
                event.drawBox(cbox, this.box.getValue());
            }
        }
    }

    public enum Page {
        General,
        Check,
        Rotate,
        Render
    }
}
