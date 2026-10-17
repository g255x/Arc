package dev.perish.mod.modules.impl.combat;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClientTickEvent;
import dev.perish.api.utils.combat.CombatUtil;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.api.utils.world.BlockPosX;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.exploit.Blink;
import dev.perish.mod.modules.impl.player.SpeedMine;
import dev.perish.mod.modules.settings.enums.Placement;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.FacingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.BlockState;

public class AutoPush extends Module {
    public static AutoPush INSTANCE;
    public boolean wantToPush = false;
    private static final Vec3d hitVec = null;    private final BooleanSetting general = add(new BooleanSetting("General", false).setParent().injectTask(this::generalPage));
    private final Timer timer = new Timer();    private final BooleanSetting pistonPacket = add(new BooleanSetting("PistonPacket", false, this.general::isOpen));
    public Vec3d directionVec = null;    private final BooleanSetting redStonePacket = add(new BooleanSetting("PowerPacket", true, this.general::isOpen));
    public Vec3d placeVec3d = null;
    public Vec3d curVec3d = null;
    private final EnumSetting<Placement> placement = this.add(new EnumSetting<>("Placement", Placement.Global, this.general::isOpen));
    private final BooleanSetting render = add(new BooleanSetting("Render", false, this.general::isOpen));
    private final dev.perish.mod.modules.settings.impl.ColorSetting renderBox = add(new dev.perish.mod.modules.settings.impl.ColorSetting("RenderBox", new java.awt.Color(255, 255, 255, 255), this.render::isOpen).injectBoolean(true));
    private final dev.perish.mod.modules.settings.impl.ColorSetting renderFill = add(new dev.perish.mod.modules.settings.impl.ColorSetting("RenderFill", new java.awt.Color(255, 255, 255, 100), this.render::isOpen).injectBoolean(true));
    private final SliderSetting sliderSpeed = add(new SliderSetting("SliderSpeed", 5.0, 0.1, 10.0, 0.1, this.render::isOpen));

    @EventListener
    public void onRender3D(dev.perish.api.events.impl.Render3DEvent event) {
        if (this.render.getValue() && this.placeVec3d != null) {
            if (this.curVec3d == null) {
                this.curVec3d = this.placeVec3d;
            } else {
                double speed = this.sliderSpeed.getValue() / 10.0;
                this.curVec3d = new Vec3d(
                        dev.perish.api.utils.math.AnimateUtil.animate(this.curVec3d.x, this.placeVec3d.x, speed),
                        dev.perish.api.utils.math.AnimateUtil.animate(this.curVec3d.y, this.placeVec3d.y, speed),
                        dev.perish.api.utils.math.AnimateUtil.animate(this.curVec3d.z, this.placeVec3d.z, speed)
                );
            }
            Box box = new Box(this.curVec3d.add(-0.5, -0.5, -0.5), this.curVec3d.add(0.5, 0.5, 0.5));
            if (this.renderFill.booleanValue) {
                dev.perish.api.utils.render.Render3DUtil.drawFill(event.matrixStack, box, this.renderFill.getValue());
            }
            if (this.renderBox.booleanValue) {
                dev.perish.api.utils.render.Render3DUtil.drawBox(event.matrixStack, box, this.renderBox.getValue());
            }
            dev.perish.api.utils.render.Render3DUtil.drawText3D("GET OUT!", this.curVec3d, -1);
        }
    }

    private void updateRenderPos(BlockPos pos) {
        if (this.render.getValue()) {
            this.placeVec3d = pos.toCenterPos();
            dev.perish.api.utils.world.BlockUtil.placedPos.remove(pos);
        }
    }
    private PlayerEntity displayTarget = null;    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true, this.general::isOpen));
    public AutoPush() {
        super("AutoPush", Category.Combat);
        setChinese("更好的活塞推人");
        INSTANCE = this;
    }    private final BooleanSetting helper = this.add(new BooleanSetting("Helper", true, this.general::isOpen));
    private final BooleanSetting base = this.add(new BooleanSetting("Base", true, this.general::isOpen));
    private final SliderSetting baseDepth = this.add(
            new SliderSetting("BaseDepth", 3, 1, 5, 1, () -> this.general.isOpen() && this.base.getValue())
    );

    public static boolean isTargetHere(BlockPos pos, Entity target) {
        return new Box(pos).intersects(target.getBoundingBox());
    }    private final BooleanSetting rotate = add(new BooleanSetting("Rotation", true, this.general::isOpen).setParent());

    private final BooleanSetting yawStep = this.add(
            new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.general.isOpen()).setParent()
    );
    private final BooleanSetting whenElytra = this.add(
            new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.general.isOpen())
    );
    private final SliderSetting steps = this.add(
            new SliderSetting("Steps", 0.3, 0.1, 1.0, 0.01, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.general.isOpen())
    );
    private final BooleanSetting checkFov = this.add(
            new BooleanSetting("OnlyLooking", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.general.isOpen())
    );
    private final SliderSetting fov = this.add(
            new SliderSetting(
                    "Fov",
                    20.0,
                    0.0,
                    360.0,
                    0.1,
                    () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.checkFov.getValue() && this.general.isOpen()
            )
    );
    private final SliderSetting priority = this.add(
            new SliderSetting("Priority", 10, 0, 100, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.general.isOpen())
    );

    public static boolean isInWeb(PlayerEntity player) {
        Vec3d playerPos = player.getPos();
        for (float x : new float[]{0.0f, 0.3f, -0.3f}) {
            for (float z : new float[]{0.0f, 0.3f, -0.3f}) {
                for (float y : new float[]{0.0f, 1.0f, -1.0f}) {
                    BlockPosX pos =
                            new BlockPosX(
                                    playerPos.getX() + (double) x,
                                    playerPos.getY() + (double) y,
                                    playerPos.getZ() + (double) z);
                    if (!AutoPush.isTargetHere(pos, player)
                            || AutoPush.mc.world.getBlockState(pos).getBlock() != Blocks.COBWEB) continue;
                    return true;
                }
            }
        }
        return false;
    }    private final BooleanSetting yawDeceive = add(new BooleanSetting("YawDeceive", true, this.general::isOpen));

    public static void pistonFacing(Direction i) {
        if (i == Direction.EAST) {
            Perish.ROTATION.snapAt(-90.0f, 5.0f);
        } else if (i == Direction.WEST) {
            Perish.ROTATION.snapAt(90.0f, 5.0f);
        } else if (i == Direction.NORTH) {
            Perish.ROTATION.snapAt(180.0f, 5.0f);
        } else if (i == Direction.SOUTH) {
            Perish.ROTATION.snapAt(0.0f, 5.0f);
        }
    }    private final BooleanSetting singlePlace = add(new BooleanSetting("SinglePlace", false, this.general::isOpen));
    private final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true, this.general::isOpen));

    public void generalPage() {
        this.general.setValueWithoutTask(false);
        this.general.setOpen(!this.general.isOpen());
    }    private final SliderSetting range = add(new SliderSetting("Range", 5.0, 0.0, 6.0, this.general::isOpen));

    public void checkPage() {
        this.check.setValueWithoutTask(false);
        this.check.setOpen(!this.check.isOpen());
    }    private final SliderSetting placeRange = add(new SliderSetting("PlaceRange", 5.0, 0.0, 6.0, this.general::isOpen));

    public boolean check(boolean onlyStatic) {
        return onlyStatic
                && mc.player != null
                && (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0);
    }    private final SliderSetting updateDelay = add(new SliderSetting("Delay", 100, 0, 1000, this.general::isOpen));

    @Override
    public void onEnable() {
        this.wantToPush = false;
    }

    @Override
    public void onDisable() {
        this.wantToPush = false;
        this.directionVec = null;
    }

    private final BooleanSetting check = add(new BooleanSetting("Check", false).setParent().injectTask(this::checkPage));

    private boolean faceVector(Vec3d directionVec) {
        if (!this.shouldYawStep()) {
            Perish.ROTATION.lookAt(directionVec);
            return true;
        } else {
            this.directionVec = directionVec;
            return Perish.ROTATION.inFov(directionVec, this.fov.getValueFloat()) || !this.checkFov.getValue();
        }
    }

    private boolean faceVector(BlockPos pos, Direction side) {
        Vec3d hitVec = pos.toCenterPos().add(new Vec3d(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5));
        return this.faceVector(hitVec);
    }

    private boolean shouldYawStep() {
        return (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!dev.perish.mod.modules.impl.movement.ElytraFly.INSTANCE.isOn() || !dev.perish.mod.modules.impl.movement.ElytraFly.INSTANCE.isFallFlying())) && this.yawStep.getValue() && !dev.perish.mod.modules.impl.movement.Velocity.INSTANCE.noRotation.getValue();
    }

    @EventListener
    public void onRotate(dev.perish.api.events.impl.RotationEvent event) {
        if (this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null) {
            event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        this.wantToPush = false;
        if (!nullCheck()) {
            if ((!event.isPost() || !event.isPre())) {
                wantToPush = false;
                this.directionVec = null;
                if (AutoCrystal.INSTANCE.stopPush.getValue() && AutoCrystal.INSTANCE.lastDamage >= AutoCrystal.INSTANCE.stopPushDamage.getValueFloat()) {
                    this.wantToPush = false;
                    return;
                }
                boolean hasTarget = false;
                if (!timer.passedMs(updateDelay.getValue())) return;
                if (selfGround.getValue() && !mc.player.isOnGround()) {
                    if (autoDisable.getValue()) {
                        disable();
                    }
                    return;
                }
                if (check(nomove.getValue())) {
                    return;
                }
                if (findBlock(getBlockType()) == -1 || findClass(PistonBlock.class) == -1) {
                    if (autoDisable.getValue()) disable();
                    return;
                }
                if (noEating.getValue() && mc.player.isUsingItem()) return;
                if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;

                for (PlayerEntity player : CombatUtil.getEnemies(range.getValue())) {
                    if (player.isDead() || player.getHealth() <= 0) continue;
                    this.displayTarget = player;
                    hasTarget = true;
                    if (!canPush(player)) continue;
                    for (Direction i : Direction.values()) {
                        if (i == Direction.UP || i == Direction.DOWN) continue;
                        BlockPos pos = EntityUtil.getEntityPos(player).offset(i);
                        if (isTargetHere(pos, player)) {
                            if (!mc.world.canCollide(player, new Box(pos))) {
                                if (this.doPush(EntityUtil.getEntityPos(player).offset(i.getOpposite()), i)) {
                                    timer.reset();
                                    if (this.singlePlace.getValue()) return;
                                }
                                if (this.doPush(EntityUtil.getEntityPos(player).offset(i.getOpposite()).up(), i)) {
                                    timer.reset();
                                    if (this.singlePlace.getValue()) return;
                                }
                            }
                        }
                    }

                    float[] offset = new float[]{-0.25f, 0f, 0.25f};
                    for (float x : offset) {
                        for (float z : offset) {
                            BlockPosX playerPos =
                                    new BlockPosX(player.getX() + x, player.getY() + 0.5, player.getZ() + z);
                            for (Direction i : Direction.values()) {
                                if (i == Direction.UP || i == Direction.DOWN) continue;
                                BlockPos pos = playerPos.offset(i);
                                if (isTargetHere(pos, player)) {
                                    if (!mc.world.canCollide(player, new Box(pos))) {
                                        if (this.doPush(playerPos.offset(i.getOpposite()), i)) {
                                            timer.reset();
                                            if (this.singlePlace.getValue()) return;
                                        }
                                        if (this.doPush(playerPos.offset(i.getOpposite()).up(), i)) {
                                            timer.reset();
                                            if (this.singlePlace.getValue()) return;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!mc.world.canCollide(
                            player, new Box(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ())))) {
                        for (Direction i : Direction.values()) {
                            if (i == Direction.UP || i == Direction.DOWN) continue;
                            BlockPos pos = EntityUtil.getEntityPos(player).offset(i);
                            Box box =
                                    player
                                            .getBoundingBox()
                                            .offset(new Vec3d(i.getOffsetX(), i.getOffsetY(), i.getOffsetZ()));
                            if (getBlock(pos.up()) != Blocks.PISTON_HEAD
                                    && !mc.world.canCollide(player, box.offset(0.0, 1.0, 0.0))
                                    && !isTargetHere(pos, player)) {
                                if (this.doPush(EntityUtil.getEntityPos(player).offset(i.getOpposite()).up(), i)) {
                                    timer.reset();
                                    if (this.singlePlace.getValue()) return;
                                }
                                if (this.doPush(EntityUtil.getEntityPos(player).offset(i.getOpposite()), i)) {
                                    timer.reset();
                                    if (this.singlePlace.getValue()) return;
                                }
                            }
                        }
                    }

                    for (float x : offset) {
                        for (float z : offset) {
                            BlockPosX playerPos =
                                    new BlockPosX(player.getX() + x, player.getY() + 0.5, player.getZ() + z);
                            for (Direction i : Direction.values()) {
                                if (i == Direction.UP || i == Direction.DOWN) continue;
                                BlockPos pos = playerPos.offset(i);
                                if (isTargetHere(pos, player)) {
                                    if (this.doPush(playerPos.offset(i.getOpposite()).up(), i)) {
                                        timer.reset();
                                        if (this.singlePlace.getValue()) return;
                                    }
                                    if (this.doPush(playerPos.offset(i.getOpposite()), i)) {
                                        timer.reset();
                                        if (this.singlePlace.getValue()) return;
                                    }
                                }
                            }
                        }
                    }
                }

                if (!hasTarget) {
                    this.displayTarget = null;
                }
            }
        }
    }    private final BooleanSetting nomove = this.add(new BooleanSetting("MovePause", false, this.check::isOpen));

    private boolean doPush(BlockPos pos, Direction direction) {
        Placement p = this.placement.getValue();
        if (p != Placement.Global) {
            BlockUtil.placementOverride = p;
        }
        try {
            return doPushInternal(pos, direction);
        } finally {
            BlockUtil.placementOverride = null;
        }
    }

    private boolean doPushInternal(BlockPos pos, Direction direction) {
        if (!mc.world.isAir(pos.offset(direction))) {
            return false;
        }
        // 已经有活塞了：朝向对就直接供电；朝向不对这格就废了，
        // 既放不了新活塞，也不该让 helper/base 去给它垫方块
        if (this.getBlock(pos) instanceof PistonBlock) {
            return getBlockState(pos).get(FacingBlock.FACING) == direction && this.powerExistingPiston(pos);
        }
        if (!this.isTrueFacing(pos, direction) || !this.facingCheck(pos)) {
            return false;
        }
        // 敌人站在活塞位上时 canPlace 同样是 false，但这不是"缺支撑"，
        // 垫多少方块都没用，服务端只会拒绝这次放置。直接放弃这格换下一个方向
        if (this.blockedByEntity(pos)) {
            return false;
        }
        if (!BlockUtil.canPlace(pos, this.placeRange.getValue())) {
            // 支撑方块这一 tick 还没在客户端生效，放完就返回，下一 tick 再放活塞
            BlockPos helperPos = this.getHelper(pos);
            if (helperPos != null) return this.tryPlaceObsidian(helperPos);
            BlockPos basePos = this.getBasePos(pos);
            if (basePos != null) return this.tryPlaceObsidian(basePos);
            return false;
        }
        boolean canPower = false;
        for (Direction i : Direction.values()) {
            if (this.getBlock(pos.offset(i)) == Blocks.REDSTONE_BLOCK) {
                canPower = true;
                break;
            }
        }
        if (canPower) {
            this.wantToPush = true;
            return this.doPiston(direction, pos, true);
        }
        for (Direction i : Direction.values()) {
            if (BlockUtil.canPlace(pos.offset(i), this.placeRange.getValue())) {
                canPower = true;
                break;
            }
        }
        if (canPower) {
            this.wantToPush = true;
            return this.doPiston(direction, pos, false);
        }
        if (this.downPower(pos)) return false;
        this.wantToPush = true;
        return this.doPiston(direction, pos, false);
    }

    private boolean powerExistingPiston(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (this.getBlock(pos.offset(i)) == Blocks.REDSTONE_BLOCK) {
                if (this.autoDisable.getValue()) {
                    this.disable();
                    return true;
                }
                return false;
            }
        }
        for (Direction i : Direction.values()) {
            if (BlockUtil.canPlace(pos.offset(i), this.placeRange.getValue())) {
                this.wantToPush = true;
                if (!this.doPower(pos, i)) return false;
                return true;
            }
        }
        if (this.downPower(pos)) return false;
        this.wantToPush = true;
        if (!this.doPower(pos)) return false;
        return true;
    }

    private boolean blockedByEntity(BlockPos pos) {
        if (mc.world == null || mc.player == null) return false;
        Box box = new Box(pos);
        double predict = this.entityPredict.getValue();
        double expand = this.entityExpand.getValue();
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive()) continue;
            if (entity instanceof ItemEntity
                    || entity instanceof ExperienceOrbEntity
                    || entity instanceof ArrowEntity) continue;
            Vec3d velocity = entity.getVelocity();
            // 客户端看到的敌人位置比服务端落后若干 tick，沿速度方向把碰撞箱拉长，
            // 免得活塞放进服务端认为敌人已经站到的格子里
            Box hitbox = entity
                    .getBoundingBox()
                    .stretch(velocity.x * predict, velocity.y * predict, velocity.z * predict)
                    .expand(expand);
            if (hitbox.intersects(box)) return true;
        }
        return false;
    }

    private final BooleanSetting noEating = add(new BooleanSetting("EatingPause", true, this.check::isOpen));

    private boolean facingCheck(BlockPos pos) {
        return true;
    }    private final BooleanSetting allowWeb = add(new BooleanSetting("AllowWeb", true, this.check::isOpen));

    private boolean isTrueFacing(BlockPos pos, Direction facing) {
        if (this.yawDeceive.getValue()) return true;
        Direction side = BlockUtil.getPlaceSide(pos);
        if (side == null) return false;
        Vec3d hitVec2 =
                pos.offset(side.getOpposite())
                        .toCenterPos()
                        .add(
                                new Vec3d(
                                        (double) side.getVector().getX() * 0.5,
                                        (double) side.getVector().getY() * 0.5,
                                        (double) side.getVector().getZ() * 0.5));
        return true;
    }    private final BooleanSetting selfGround = add(new BooleanSetting("SelfGround", true, this.check::isOpen));

    private boolean doPower(BlockPos pos, Direction i2) {
        if (!BlockUtil.canPlace(pos.offset(i2), this.placeRange.getValue())) {
            return true;
        }
        if (this.rotate.getValue()) {
            Direction side = BlockUtil.getPlaceSide(pos.offset(i2));
            if (side != null && !this.faceVector(pos.offset(i2).offset(side), side.getOpposite())) return false;
        }
        int old = 0;
        if (mc.player != null) {
            old = mc.player.getInventory().selectedSlot;
        }
        int power = this.findBlock(Blocks.REDSTONE_BLOCK);
        if (power == -1) return true;
        this.doSwap(power);
        BlockUtil.placeBlock(pos.offset(i2), false, this.redStonePacket.getValue());
        if (this.inventory.getValue()) {
            this.doSwap(power);
            EntityUtil.syncInventory();
        } else {
            this.doSwap(old);
        }
        return false;
    }    private final BooleanSetting onlyGround = add(new BooleanSetting("OnlyGround", false, this.check::isOpen));

    private boolean doPower(BlockPos pos) {
        Direction facing = BlockUtil.getBestNeighboring(pos, null);
        if (facing != null) {

            if (!this.doPower(pos, facing)) {
                return true;
            }
        }
        for (Direction i2 : Direction.values()) {

            if (this.doPower(pos, i2)) continue;
            return true;
        }
        return false;
    }    private final SliderSetting surroundCheck = add(new SliderSetting("SurroundCheck", 2, 0, 4, this.check::isOpen));
    private final SliderSetting entityExpand = add(new SliderSetting("EntityExpand", 0.1, 0.0, 0.5, 0.05, this.check::isOpen));
    private final SliderSetting entityPredict = add(new SliderSetting("EntityPredict", 2, 0, 5, 1, this.check::isOpen));

    private boolean downPower(BlockPos pos) {
        if (BlockUtil.getPlaceSide(pos) == null) {
            boolean noPower = true;
            for (Direction i2 : Direction.values()) {
                if (this.getBlock(pos.offset(i2)) == Blocks.REDSTONE_BLOCK) {
                    noPower = false;
                    break;
                }
            }
            if (noPower) {
                if (!BlockUtil.canPlace(pos.add(0, -1, 0), this.placeRange.getValue())) {
                    return true;
                }
                if (this.rotate.getValue()) {
                    Direction side = BlockUtil.getPlaceSide(pos.add(0, -1, 0));
                    if (side != null && !this.faceVector(pos.add(0, -1, 0).offset(side), side.getOpposite())) return true;
                }
                int old = 0;
                if (mc.player != null) {
                    old = mc.player.getInventory().selectedSlot;
                }
                int power = this.findBlock(Blocks.REDSTONE_BLOCK);
                this.doSwap(power);
                BlockUtil.placeBlock(
                        pos.add(0, -1, 0), false, this.redStonePacket.getValue());
                if (this.inventory.getValue()) {
                    this.doSwap(power);
                    EntityUtil.syncInventory();
                } else {
                    this.doSwap(old);
                }
            }
        }
        return false;
    }

    private boolean doPiston(Direction i, BlockPos pos, boolean mine) {
        if (BlockUtil.canPlace(pos, this.placeRange.getValue())) {
            int piston = this.findClass(PistonBlock.class);
            Direction side = BlockUtil.getPlaceSide(pos);
            if (this.rotate.getValue()) {
                if (!this.faceVector(pos.offset(side), side.getOpposite())) return false;
            }
            if (this.yawDeceive.getValue()) {
                AutoPush.pistonFacing(i.getOpposite());
            }
            int old = 0;
            if (mc.player != null) {
                old = mc.player.getInventory().selectedSlot;
            }
            this.doSwap(piston);
            BlockUtil.placeBlock(pos, false, this.pistonPacket.getValue());
            this.updateRenderPos(pos);
            if (this.inventory.getValue()) {
                this.doSwap(piston);
                EntityUtil.syncInventory();
            } else {
                this.doSwap(old);
            }
            if (this.rotate.getValue()) {
                if (!this.shouldYawStep()) {
                    Perish.ROTATION.lookAt(pos.offset(side), side.getOpposite());
                }
            }
            for (Direction i2 : Direction.values()) {
                if (this.getBlock(pos.offset(i2)) != Blocks.REDSTONE_BLOCK) continue;
                if (mine) {
                    this.mine(pos.offset(i2));
                }
                if (this.autoDisable.getValue()) {
                    this.disable();
                }
                return true;
            }
            this.doPower(pos);
            return true;
        }
        return false;
    }

    @Override
    public String getInfo() {
        if (this.displayTarget != null) {
            return this.displayTarget.getName().getString();
        }
        return null;
    }

    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            if (mc.player != null) {
                InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
            }
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    public int findBlock(Block blockIn) {
        if (this.inventory.getValue()) {
            return InventoryUtil.findBlockInventorySlot(blockIn);
        }
        return InventoryUtil.findBlock(blockIn);
    }

    public int findClass(Class clazz) {
        if (this.inventory.getValue()) {
            return InventoryUtil.findClassInventorySlot(clazz);
        }
        return InventoryUtil.findClass(clazz);
    }

    public BlockPos getHelper(BlockPos pos) {
        if (!this.helper.getValue()) {
            return null;
        } else {
            for (Direction i : Direction.values()) {
                if (BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite()) && this.canSupport(pos.offset(i))) {
                    return pos.offset(i);
                }
            }

            return null;
        }
    }

    public BlockPos getHelper(BlockPos pos, Direction ignore) {
        if (!this.helper.getValue()) {
            return null;
        } else {
            for (Direction i : Direction.values()) {
                if (i != ignore && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite()) && BlockUtil.canPlace(pos.offset(i), this.placeRange.getValue())) {
                    return pos.offset(i);
                }
            }

            return null;
        }
    }

    public BlockPos getBasePos(BlockPos pos) {
        if (!this.base.getValue()) {
            return null;
        }
        // 从活塞位向下找基座：找到最深一个能贴着已有方块放下的空位，
        // 逐 tick 往上垫一格，直到活塞位正下方有支撑
        int max = (int) this.baseDepth.getValue();
        for (int depth = 1; depth <= max; ++depth) {
            BlockPos below = pos.down(depth);
            if (!BlockUtil.canReplace(below)) {
                // 撞到实心方块（或本 tick 刚放下的黑曜石），它上面那格就是这次要垫的位置
                BlockPos placeAt = below.up();
                if (placeAt.equals(pos)) return null; // 活塞位正下方已经有支撑了
                return this.canSupport(placeAt) ? placeAt : null;
            }
            if (this.canSupport(below)) {
                return below;
            }
        }
        return null;
    }

    private boolean canSupport(BlockPos pos) {
        return !this.blockedByEntity(pos) && BlockUtil.canPlace(pos, this.placeRange.getValue());
    }

    private boolean tryPlaceObsidian(BlockPos pos) {
        if (pos != null) {
            int old = 0;
            if (mc.player != null) {
                old = mc.player.getInventory().selectedSlot;
            }
            int obsidian = this.findBlock(Blocks.OBSIDIAN);
            if (obsidian == -1) {
                obsidian = this.findBlock(Blocks.CRYING_OBSIDIAN);
            }
            if (obsidian != -1) {
                if (this.rotate.getValue()) {
                    Direction side = BlockUtil.getPlaceSide(pos);
                    if (side != null && !this.faceVector(pos.offset(side), side.getOpposite())) return false;
                }
                this.doSwap(obsidian);
                BlockUtil.placeBlock(pos, false, false);
                if (this.inventory.getValue()) {
                    this.doSwap(obsidian);
                    EntityUtil.syncInventory();
                } else {
                    this.doSwap(old);
                }
                return true;
            }
        }
        return false;
    }

    private void mine(BlockPos pos) {
        SpeedMine.INSTANCE.mine(pos);
    }

    private Block getBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock();
    }

    private BlockState getBlockState(BlockPos pos) {
        if (mc.world != null) {
            return mc.world.getBlockState(pos);
        }
        return null;
    }

    private Boolean canPush(PlayerEntity player) {
        if (this.onlyGround.getValue() && !player.isOnGround()) {
            return false;
        }
        if (!this.allowWeb.getValue() && isInWeb(player)) {
            return false;
        }
        int progress = 0;
        if (!mc.world.isAir(new BlockPosX(player.getX() + 1.0, player.getY() + 0.5, player.getZ()))) {
            ++progress;
        }
        if (!mc.world.isAir(new BlockPosX(player.getX() - 1.0, player.getY() + 0.5, player.getZ()))) {
            ++progress;
        }
        if (!mc.world.isAir(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ() + 1.0))) {
            ++progress;
        }
        if (!mc.world.isAir(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ() - 1.0))) {
            ++progress;
        }
        if (!mc.world.isAir(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ()))) {
            for (Direction i : Direction.values()) {
                BlockPos pos;
                if (i == Direction.UP
                        || i == Direction.DOWN
                        || (!mc.world.isAir(pos = EntityUtil.getEntityPos(player).offset(i))
                        || !mc.world.isAir(pos.up()))
                        && !isTargetHere(pos, player)) continue;
                if (!mc.world.isAir(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ()))) {
                    return true;
                }
                return (double) progress > this.surroundCheck.getValue();
            }
            return false;
        }
        if (!mc.world.canCollide(
                player, new Box(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ())))) {
            for (Direction i : Direction.values()) {
                if (i == Direction.UP || i == Direction.DOWN) continue;
                BlockPos pos = EntityUtil.getEntityPos(player).offset(i);
                Box box =
                        player
                                .getBoundingBox()
                                .offset(new Vec3d(i.getOffsetX(), i.getOffsetY(), i.getOffsetZ()));
                if (this.getBlock(pos.up()) == Blocks.PISTON_HEAD
                        || mc.world.canCollide(player, box.offset(0.0, 1.0, 0.0))
                        || isTargetHere(pos, player)
                        || !mc.world.canCollide(
                        player, new Box(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ()))))
                    continue;
                return true;
            }
        }
        return (double) progress > this.surroundCheck.getValue();
    }

    private Block getBlockType() {
        return Blocks.REDSTONE_BLOCK;
    }




































}
