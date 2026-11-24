package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.Arc;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.player.MovementUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.mod.modules.impl.player.Follower;
import net.minecraft.screen.slot.SlotActionType;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.g255.arc.asm.accessors.IFireworkRocketEntity;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.MoveEvent;
import dev.g255.arc.api.events.impl.TravelEvent;
import dev.g255.arc.api.events.impl.UpdateRotateEvent;

public class FakeFlight extends Module {
    public static FakeFlight INSTANCE;

    public boolean fly = false;
    public final Timer timer = new Timer();
    public float pitch = 0.0F;
    public float yaw = 0.0F;
    private boolean fired = false;

    public final EnumSetting<UpdateMode> updateMode = this.add(new EnumSetting<>("UpdateMode", UpdateMode.Pre));
    public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true).setParent());
    public final SliderSetting upPitch = this.add(new SliderSetting("UpPitch", -90.0, -90.0, 90.0, 1.0, () -> this.rotate.isOpen()));
    public final SliderSetting downPitch = this.add(new SliderSetting("DownPitch", 90.0, -90.0, 90.0, 1.0, () -> this.rotate.isOpen()));
    public final SliderSetting moveUpPitch = this.add(new SliderSetting("MoveUpPitch", -60.0, -90.0, 90.0, 1.0, () -> this.rotate.isOpen()));
    public final SliderSetting moveDownPitch = this.add(new SliderSetting("MoveDownPitch", 60.0, -90.0, 90.0, 1.0, () -> this.rotate.isOpen()));
    public final SliderSetting movePitch = this.add(new SliderSetting("MovePitch", -2.0, -90.0, 90.0, 1.0, () -> this.rotate.isOpen()));

    private final BooleanSetting firework = this.add(new BooleanSetting("Firework", true).setParent());
    public final SliderSetting fireworkDelay = this.add(new SliderSetting("MaxDelay", 400.0, 0.0, 1000.0, 1.0, this.firework::isOpen));
    public final SliderSetting minDelay = this.add(new SliderSetting("MinDelay", 200.0, 0.0, 1000.0, 1.0, this.firework::isOpen));
    public final SliderSetting slowPass = this.add(new SliderSetting("SlowPass", 50.0, 0.0, 120.0, 1.0, this.firework::isOpen));
    private final BooleanSetting onlyOne = this.add(new BooleanSetting("OnlyOne", true, this.firework::isOpen));
    public final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true, this.firework::isOpen));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, this.firework::isOpen));
    public final BooleanSetting checkFly = this.add(new BooleanSetting("CheckFly", false, this.firework::isOpen));

    public final BooleanSetting controlMove = this.add(new BooleanSetting("ControlMove", true).setParent());
    private final BooleanSetting freeze = this.add(new BooleanSetting("Freeze", true, () -> this.controlMove.isOpen()));
    private final BooleanSetting motion = this.add(new BooleanSetting("Motion", true, () -> this.controlMove.isOpen()));
    public final EnumSetting<UpdateMode> updateModeControl = this.add(new EnumSetting<>("UpdateModeControl", UpdateMode.Pre, () -> this.controlMove.isOpen()));

    public final BooleanSetting strictStartFly = this.add(new BooleanSetting("StrictStartFly", false));
    public final BooleanSetting syncInv = this.add(new BooleanSetting("SyncInv", false));

    private int lastSlot = -1;

    public FakeFlight() {
        super("FakeFlight", Module.Category.Movement);
        this.setChinese("欺骗飞行");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (!nullCheck()) {
            this.pitch = mc.player.getPitch();
            this.yaw = mc.player.getYaw();
            Follower.INSTANCE.firstFollower = true;
            this.lastSlot = -1;
        }
    }

    @Override
    public void onDisable() {
        if (!nullCheck()) {
            this.timer.setMs(114514L);
            this.fly = false;
            this.stopFlying();
        }
    }

    @EventListener
    private void onTick(ClientTickEvent event) {
        if (nullCheck()) return;

        boolean isPre = event.isPre();
        boolean shouldUpdate;
        if (this.controlMove.getValue() && !Follower.INSTANCE.canFollow
                && !MovementUtil.isMoving() && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) {
            shouldUpdate = shouldUpdateForMode(this.updateModeControl.getValue(), isPre);
        } else {
            shouldUpdate = shouldUpdateForMode(this.updateMode.getValue(), isPre);
        }
        if (!shouldUpdate) return;

        if (!mc.player.isFallFlying()) {
            if (mc.player.isOnGround()) {
                // 真正落地才重置状态，不主动发 stopFlying 包
                // 防止服务端位置修正包瞬间触发 RELEASE_SHIFT_KEY 导致从高空坠落
                this.fly = false;
            } else {
                // 在空中但未在滑翔（服务端修正或首次启动）— 重新发起假飞
                this.startFlying();
            }
            return;
        }

        this.fly = true;
        this.handleFirework();
    }

    private boolean shouldUpdateForMode(UpdateMode mode, boolean isPre) {
        return switch (mode) {
            case All -> true;
            case Pre -> isPre;
            case Post -> !isPre;
        };
    }

    private void handleFirework() {
        if (!this.firework.getValue()) return;
        if (!shouldFireFirework()) return;

        if (this.inventorySwap.getValue()) {
            int fireworkSlot = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET);
            if (fireworkSlot == -1) return;
            int currentSlot = mc.player.getInventory().selectedSlot;
            InventoryUtil.inventorySwap(fireworkSlot, currentSlot);
            sendFireworkPacket(Hand.MAIN_HAND);
            InventoryUtil.inventorySwap(fireworkSlot, currentSlot);
            this.fired = true;
            this.timer.reset();
        } else {
            int fireworkSlot = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
            if (fireworkSlot == -1) return;
            int oldSlot = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(fireworkSlot);
            sendFireworkPacket(Hand.MAIN_HAND);
            InventoryUtil.switchToSlot(oldSlot);
            this.fired = true;
            this.timer.reset();
        }
    }

    private boolean shouldFireFirework() {
        if (this.onlyOne.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof FireworkRocketEntity fireworkRocketEntity
                        && ((IFireworkRocketEntity) fireworkRocketEntity).getShooter() == mc.player) {
                    if (this.fired) {
                        this.fired = false;
                        this.timer.reset();
                    }
                    return false;
                }
            }
        }

        double currentSpeed = getPlayerSpeed();
        boolean speedSlow = currentSpeed < this.slowPass.getValue();
        boolean isMoving = MovementUtil.isMoving();

        if (this.timer.passedMs((long) this.fireworkDelay.getValue())
                || (speedSlow && isMoving && this.timer.passedMs((long) this.minDelay.getValue()))) {
            if (this.usingPause.getValue() && mc.player.isUsingItem()) {
                return false;
            }
            if (this.inventorySwap.getValue()) {
                return InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET) != -1;
            } else {
                return InventoryUtil.findItem(Items.FIREWORK_ROCKET) != -1;
            }
        }
        return false;
    }

    private double getPlayerSpeed() {
        double x = mc.player.getX() - mc.player.prevX;
        double y = mc.player.getY() - mc.player.prevY;
        double z = mc.player.getZ() - mc.player.prevZ;
        double dist = Math.sqrt(x * x + y * y + z * z) / 1000.0;
        double div = 1.388888888888889E-5;
        float timerVal = Arc.TIMER.get();
        return dist / div * timerVal;
    }

    private void sendFireworkPacket(Hand hand) {
        sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(hand, id, Arc.ROTATION.getLastYaw(), Arc.ROTATION.getLastPitch()));
    }

    private void startFlying() {
        int invSlot = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
        if (invSlot == -1) return;
        // 临时将鞘翅放入胸甲槽
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, invSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
        // 发送 START_FALL_FLYING 包
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        mc.player.startFallFlying();
        // 还原背包
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, invSlot, 0, SlotActionType.PICKUP, mc.player);
        if (this.syncInv.getValue()) EntityUtil.syncInventory();
    }

    private void stopFlying() {
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        mc.player.stopFallFlying();
        mc.player.setSneaking(false);
    }

    @EventListener
    private void onMove(MoveEvent event) {
        if (this.fly && mc.player.isFallFlying()) {
            if (event.getY() > 0
                    && BlockUtil.canCollide(mc.player.getBoundingBox().offset(0.0, event.getY(), 0.0))) {
                event.setY(0.0);
            }

            if (this.controlMove.getValue() && this.motion.getValue()
                    && !MovementUtil.isMoving() && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()
                    && !Follower.INSTANCE.canFollow) {
                event.setX(0.0);
                event.setY(0.0);
                event.setZ(0.0);
            }
        }
    }

    @EventListener
    private void onTravel(TravelEvent event) {
        if (!nullCheck() && !event.isPost() && !Follower.INSTANCE.canFollow && mc.player.isFallFlying()) {
            if (this.controlMove.getValue() && this.freeze.getValue()
                    && !MovementUtil.isMoving() && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) {
                event.cancel();
            }
        }
    }

    @EventListener(priority = -999)
    private void onRotation(UpdateRotateEvent event) {
        if (!this.fly) return;

        if (this.rotate.getValue() || (Follower.INSTANCE.isOn() && Follower.INSTANCE.canFollow)) {
            if (Follower.INSTANCE.isOn() && Follower.INSTANCE.canFollow && Follower.INSTANCE.target != null) {
                PlayerEntity target = Follower.INSTANCE.target;
                Vec3d predictPos = predictTargetPos(target, (int) Follower.INSTANCE.predict.getValue());
                predictPos = predictPos.add(0, target.getEyeY() - target.getY(), 0);
                float[] rots = getRotation(mc.player.getEyePos(), predictPos);
                this.yaw = rots[0];
                this.pitch = Follower.INSTANCE.getFollowPitch(rots[1]);
            } else {
                if (MovementUtil.isMoving()) {
                    this.yaw = Sprint.getSprintYaw(mc.player.getYaw());
                }
                if (mc.options.jumpKey.isPressed()) {
                    this.pitch = MovementUtil.isMoving() ? this.moveUpPitch.getValueFloat() : this.upPitch.getValueFloat();
                } else if (mc.options.sneakKey.isPressed()) {
                    this.pitch = MovementUtil.isMoving() ? this.moveDownPitch.getValueFloat() : this.downPitch.getValueFloat();
                } else if (MovementUtil.isMoving()) {
                    this.pitch = this.movePitch.getValueFloat();
                }
            }

            event.setPitch(this.pitch);
            event.setYaw(this.yaw);
        }
    }

    private Vec3d predictTargetPos(PlayerEntity target, int ticks) {
        Vec3d targetPos = target.getPos();
        if (ticks <= 0) return targetPos;
        Vec3d velocity = target.getVelocity();
        return targetPos.add(velocity.multiply(ticks));
    }

    private float[] getRotation(Vec3d eyesPos, Vec3d vec) {
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
    }

    public enum UpdateMode {
        All,
        Pre,
        Post
    }
}
