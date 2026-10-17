package dev.perish.mod.modules.impl.movement;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.EntityVelocityUpdateEvent;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.api.events.impl.TickEvent;
import dev.perish.api.events.impl.UpdateRotateEvent;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.MovementUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.exploit.Blink;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import dev.perish.asm.accessors.IFireworkRocketEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Box;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class Velocity extends Module {
    public static Velocity INSTANCE;
    static boolean pushOutOfBlocks = false;
    public final BooleanSetting whileLiquid = this.add(new BooleanSetting("WhileLiquid", false));
    public final BooleanSetting whileElytra = this.add(new BooleanSetting("FallFlying", false));
    public final BooleanSetting noClimb = this.add(new BooleanSetting("NoClimb", false));
    public final BooleanSetting waterPush = this.add(new BooleanSetting("NoWaterPush", false));
    public final BooleanSetting entityPush = this.add(new BooleanSetting("NoEntityPush", true));
    public final BooleanSetting blockPush = this.add(new BooleanSetting("NoBlockPush", true));
    public final BooleanSetting fishBob = this.add(new BooleanSetting("NoFishBob", true));
    public final Timer pearlTimer = new Timer();
    private final EnumSetting<Velocity.Mode> mode = this.add(new EnumSetting("Mode", Velocity.Mode.Plain));
    public final BooleanSetting ignorePearlLag = this.add(
            new BooleanSetting("IgnorePearlLag", true, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)).setParent()
    );
    private final SliderSetting phaseTime = this.add(
            new SliderSetting("PhaseTime", 250, 0, 1000, () -> (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && this.ignorePearlLag.isOpen())
    );
    public final BooleanSetting noRotation = this.add(
            new BooleanSetting("NoRotation", false, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall))
    );
    public final BooleanSetting flagInWall = this.add(
            new BooleanSetting("FlagInWall", false, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)).setParent()
    );
    public final BooleanSetting whenPushOutOfBlocks = this.add(
            new BooleanSetting("WhilePushOut", false, () -> (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && this.flagInWall.isOpen())
    );
    public final BooleanSetting staticSetting = this.add(new BooleanSetting("Static", false, () -> this.mode.is(Velocity.Mode.Grim)));
    public final BooleanSetting cancelAll = this.add(new BooleanSetting("CancelAll", false, () -> !this.mode.is(Velocity.Mode.None)));
    private final SliderSetting horizontal = this.add(
            new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0, () -> !this.mode.is(Velocity.Mode.None) && !this.cancelAll.getValue())
    );
    private final SliderSetting vertical = this.add(
            new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0, () -> !this.mode.is(Velocity.Mode.None) && !this.cancelAll.getValue())
    );
    private final SliderSetting lagPause = this.add(
            new SliderSetting("LagPause", 50, 0, 500, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall))
    );
    private final SliderSetting v3Packets = this.add(
            new SliderSetting("V3Packets", 3, 1, 20, () -> this.mode.is(Velocity.Mode.GrimV3))
    );
    private final SliderSetting v3Duration = this.add(
            new SliderSetting("V3Duration", 2, 1, 10, () -> this.mode.is(Velocity.Mode.GrimV3))
    );
    private final SliderSetting v3Horizontal = this.add(
            new SliderSetting("V3Horizontal", 0, 0, 100, () -> this.mode.is(Velocity.Mode.GrimV3))
    );
    private final SliderSetting v3Vertical = this.add(
            new SliderSetting("V3Vertical", 100, 0, 100, () -> this.mode.is(Velocity.Mode.GrimV3))
    );
    private final Timer lagBackTimer = new Timer();
    private boolean flag;
    private int v3Ticks;

    public Velocity() {
        super("Velocity", Module.Category.Movement);
        this.setChinese("反击退");
        INSTANCE = this;
    }

    private static void pushOutOfBlocks(double x, double z) {
        BlockPos blockPos = BlockPos.ofFloored(x, mc.player.getY(), z);
        if (wouldCollideAt(blockPos)) {
            double d = x - blockPos.getX();
            double e = z - blockPos.getZ();
            Direction direction = null;
            double f = Double.MAX_VALUE;
            Direction[] directions = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

            for (Direction direction2 : directions) {
                double g = direction2.getAxis().choose(d, 0.0, e);
                double h = direction2.getDirection() == AxisDirection.POSITIVE ? 1.0 - g : g;
                if (h < f && !wouldCollideAt(blockPos.offset(direction2))) {
                    f = h;
                    direction = direction2;
                }
            }

            if (direction != null) {
                pushOutOfBlocks = true;
            }
        }
    }

    private static boolean wouldCollideAt(BlockPos pos) {
        Box box = mc.player.getBoundingBox();
        Box box2 = new Box(pos.getX(), box.minY, pos.getZ(), pos.getX() + 1.0, box.maxY, pos.getZ() + 1.0).contract(1.0E-7);
        return mc.player.getWorld().canCollide(mc.player, box2);
    }

    @Override
    public String getInfo() {
        return this.mode.is(Velocity.Mode.None)
                ? null
                : this.mode.getValue().name()
                + ", "
                + (this.cancelAll.getValue() ? "Cancel" : this.horizontal.getValueInt() + "%, " + this.vertical.getValueInt() + "%");
    }

    @EventListener
    public void onRotate(UpdateRotateEvent event) {
        if (this.noRotation()) {
            event.setRotation(Perish.ROTATION.rotationYaw, 89.0F);
        }
    }

    public boolean noRotation() {
        return this.isOn() && (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && EntityUtil.isInsideBlock() && this.noRotation.getValue();
    }

    @EventListener
    public void onVelocity(EntityVelocityUpdateEvent event) {
        if (!nullCheck()) {
            if (event.getEntity() == mc.player) {
                if (!this.mode.is(Velocity.Mode.None)) {
                    if (!mc.player.isInFluid() || this.whileLiquid.getValue()) {
                        if (!mc.player.isFallFlying() || this.whileElytra.getValue()) {
                            switch (this.mode.getValue()) {
                                case Plain:
                                default:
                                    break;
                                case GrimV3:
                                    // 烟花冲量期间放行整段速度, 不削减也不开覆盖窗口, 保住 FakeFlight 的加速推进
                                    if (this.fireworkBoosting()) {
                                        return;
                                    }

                                    // 不锚定、不冻结: 只按百分比削减击退, 你自己的行走速度分量保持不变
                                    // 收到水平击退时开启覆盖窗口, 用高频 move_player_pos 在"当前位置"顶掉服务端残余位移
                                    if (event.getX() != 0.0 || event.getZ() != 0.0) {
                                        this.v3Ticks = this.v3Duration.getValueInt();
                                    }

                                    double v3h = this.v3Horizontal.getValueInt() / 100.0;
                                    double v3v = this.v3Vertical.getValueInt() / 100.0;
                                    event.setX(event.getX() * v3h);
                                    event.setZ(event.getZ() * v3h);
                                    event.setY(event.getY() * v3v);
                                    return;
                                case Grim:
                                    if (!this.lagBackTimer.passedMs(this.lagPause.getValue())) {
                                        return;
                                    }

                                    if (!EntityUtil.isInsideBlock() && this.getPos() == null && (!this.staticSetting.getValue() || !MovementUtil.isStatic())) {
                                        return;
                                    }

                                    if (event.getX() != 0.0 || event.getZ() != 0.0) {
                                        this.flag = true;
                                    }
                                    break;
                                case Wall:
                                    if (!this.lagBackTimer.passedMs(this.lagPause.getValue())) {
                                        return;
                                    }

                                    if (!EntityUtil.isInsideBlock()) {
                                        return;
                                    }

                                    if (event.getX() != 0.0 || event.getZ() != 0.0) {
                                        this.flag = true;
                                    }
                            }

                            if (this.cancelAll.getValue()) {
                                event.cancel();
                            } else {
                                double h = this.horizontal.getValueInt() / 100.0;
                                double v = this.vertical.getValueInt() / 100.0;
                                event.setX(event.getX() * h);
                                event.setZ(event.getZ() * h);
                                event.setY(event.getY() * v);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventListener
    public void onReceivePacket(PacketEvent.Receive event) {
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket && (!this.ignorePearlLag.getValue() || this.pearlTimer.passed(this.phaseTime.getValueInt()))
        ) {
            this.lagBackTimer.reset();
        }

        // GrimV3: 收到 setback 立刻中止覆盖窗口, 接受这次回弹, 避免与服务端来回拉扯造成 lag
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.v3Ticks = 0;
        }

        if (!nullCheck()) {
            if (!mc.player.isInFluid() || this.whileLiquid.getValue()) {
                if (this.fishBob.getValue()
                        && event.getPacket() instanceof EntityStatusS2CPacket packet
                        && packet.getStatus() == 31
                        && packet.getEntity(mc.world) instanceof FishingBobberEntity fishHook
                        && fishHook.getHookedEntity() == mc.player) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventListener
    public void onUpdate(TickEvent event) {
        if (!nullCheck()) {
            if (!event.isPost() && (!mc.player.isInFluid() || this.whileLiquid.getValue())) {
                if (this.mode.is(Velocity.Mode.GrimV3)) {
                    this.applyGrimV3();
                    return;
                }

                if (this.flagInWall.getValue()) {
                    pushOutOfBlocks = false;
                    pushOutOfBlocks(mc.player.getX() - mc.player.getWidth() * 0.35, mc.player.getZ() + mc.player.getWidth() * 0.35);
                    pushOutOfBlocks(mc.player.getX() - mc.player.getWidth() * 0.35, mc.player.getZ() - mc.player.getWidth() * 0.35);
                    pushOutOfBlocks(mc.player.getX() + mc.player.getWidth() * 0.35, mc.player.getZ() - mc.player.getWidth() * 0.35);
                    pushOutOfBlocks(mc.player.getX() + mc.player.getWidth() * 0.35, mc.player.getZ() + mc.player.getWidth() * 0.35);
                }

                if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                    if (this.flag) {
                        if (this.lagBackTimer.passedMs(this.lagPause.getValue())
                                && (this.flagInWall.getValue() && (!pushOutOfBlocks || this.whenPushOutOfBlocks.getValue()) || !EntityUtil.isInsideBlock())) {
                            mc.getNetworkHandler()
                                    .sendPacket(
                                            new Full(
                                                    mc.player.getX(),
                                                    mc.player.getY(),
                                                    mc.player.getZ(),
                                                    Perish.ROTATION.rotationYaw,
                                                    Perish.ROTATION.rotationPitch,
                                                    mc.player.isOnGround()
                                            )
                                    );
                            BlockPos pos = this.getPos();
                            if (pos != null) {
                                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, mc.player.getHorizontalFacing().getOpposite()));
                            }
                        }

                        this.flag = false;
                    }
                }
            }
        }
    }

    public BlockPos getPos() {
        return mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() == Blocks.OBSIDIAN ? mc.player.getBlockPos().down() : null;
    }

    // FakeFlight 靠烟花给出加速冲量(fall-flying 期间生效), 高频 move_player_pos 会顶掉这段推进
    // 所以只要在滑翔 / 有自己活跃的烟花, 就停手并清空覆盖窗口, 不去绕烟花结束后的冲击
    private boolean fireworkBoosting() {
        if (mc.player.isFallFlying()) {
            return true;
        }
        if (mc.world == null) {
            return false;
        }
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof FireworkRocketEntity firework
                    && ((IFireworkRocketEntity) firework).getShooter() == mc.player) {
                return true;
            }
        }
        return false;
    }

    // 高频发送 move_player_pos: 在"玩家当前实时位置"重复上报, 把服务端因击退产生的残余位移覆盖掉
    // 关键点: 不锚定固定点、不改本地速度, 所以你自己的行走位移完全保留, 路径不受影响也不会 lag
    private void applyGrimV3() {
        if (this.v3Ticks <= 0) {
            return;
        }

        // 烟花冲量期间不发覆盖包, 避免顶掉 FakeFlight 烟花结束后的加速推进
        if (this.fireworkBoosting()) {
            this.v3Ticks = 0;
            return;
        }

        // 用当前实时坐标, 玩家边走边发, 覆盖的是"服务端多推的那一段", 不是把人钉在原地
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        boolean onGround = mc.player.isOnGround();

        int count = this.v3Packets.getValueInt();
        for (int i = 0; i < count; i++) {
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y, z, onGround));
        }

        this.v3Ticks--;
    }

    public enum Mode {
        Plain,
        Grim,
        Wall,
        GrimV3,
        None
    }
}
