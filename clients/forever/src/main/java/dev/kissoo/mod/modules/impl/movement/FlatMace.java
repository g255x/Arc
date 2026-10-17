package dev.kissoo.mod.modules.impl.movement;

import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.KeyboardInputEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.events.impl.UpdateRotateEvent;
import dev.kissoo.api.utils.math.MathUtil;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.path.BaritoneUtil;
import dev.kissoo.api.utils.path.TPUtils;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.core.impl.RotationManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.Module.Category;
import dev.kissoo.mod.modules.impl.combat.Aura;
import dev.kissoo.mod.modules.impl.exploit.MaceSpoof;
import dev.kissoo.mod.modules.impl.exploit.MaceSpoof.LagMode;
import dev.kissoo.mod.modules.impl.exploit.MaceSpoof.TimerMode;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import hoprc.api.events.eventbus.EventListener;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;

public class FlatMace extends Module {
    public static FlatMace INSTANCE;
    private final SliderSetting height = this.add(new SliderSetting("Height", 5.0, 1.0, 30.0, 0.5));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 200.0, 10.0, 2000.0, 10.0));
    private final BooleanSetting holdMode = this.add(new BooleanSetting("HoldMode", true));
    private final BooleanSetting autoMaceSpoof = this.add(new BooleanSetting("AutoMaceSpoof", true));
    private final BooleanSetting internalCam = this.add(new BooleanSetting("InternalCam", false).setParent());
    private final SliderSetting camSpeed = this.add(new SliderSetting("HSpeed", 1.0, 0.0, 3.0, () -> this.internalCam.getValue()));
    private final SliderSetting camVSpeed = this.add(new SliderSetting("VSpeed", 0.42, 0.0, 3.0, () -> this.internalCam.getValue()));
    private final BooleanSetting camRotate = this.add(new BooleanSetting("Rotate", true, () -> this.internalCam.getValue()));
    private final Timer tpTimer = new Timer();
    private float fakeYaw;
    private float fakePitch;
    private float prevFakeYaw;
    private float prevFakePitch;
    private double fakeX;
    private double fakeY;
    private double fakeZ;
    private double prevFakeX;
    private double prevFakeY;
    private double prevFakeZ;
    private float playerYaw;
    private float playerPitch;
    private boolean prevMaceSpoofState;
    private boolean prevAuraState;
    private LagMode prevLagMode;
    private TimerMode prevTimerMode;
    private double prevLandDistance;

    public FlatMace() {
        super("FlatMace", Category.Movement);
        this.setChinese("平地锤");
        INSTANCE = this;
    }

    public String getInfo() {
        return String.format("+%.1f %.0fms", this.height.getValue(), this.delay.getValue());
    }

    public void onEnable() {
        this.tpTimer.reset();
        if (nullCheck()) {
            this.disable();
        } else {
            if (this.internalCam.getValue()) {
                this.playerYaw = this.getYaw();
                this.playerPitch = this.getPitch();
                this.fakePitch = this.getPitch();
                this.fakeYaw = this.getYaw();
                this.prevFakePitch = this.fakePitch;
                this.prevFakeYaw = this.fakeYaw;
                this.fakeX = mc.player.getX();
                this.fakeY = mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose());
                this.fakeZ = mc.player.getZ();
                this.prevFakeX = this.fakeX;
                this.prevFakeY = this.fakeY;
                this.prevFakeZ = this.fakeZ;
            }

            if (this.autoMaceSpoof.getValue()) {
                if (MaceSpoof.INSTANCE != null && !MaceSpoof.INSTANCE.isOn()) {
                    this.prevMaceSpoofState = true;
                    this.prevLagMode = MaceSpoof.INSTANCE.getLagMode();
                    this.prevTimerMode = MaceSpoof.INSTANCE.getTimerMode();
                    this.prevLandDistance = MaceSpoof.INSTANCE.getLandDistance();
                    MaceSpoof.INSTANCE.enable();
                    MaceSpoof.INSTANCE.setLagMode(LagMode.Progressive);
                } else {
                    this.prevMaceSpoofState = false;
                }
            }

            if (Aura.INSTANCE != null && !Aura.INSTANCE.isOn()) {
                this.prevAuraState = true;
                Aura.INSTANCE.enable();
            } else {
                this.prevAuraState = false;
            }
        }
    }

    public void onDisable() {
        boolean wasMaceSpoofEnabledByUs = this.prevMaceSpoofState;
        this.prevAuraState = false;
        this.prevMaceSpoofState = false;
        if (Aura.INSTANCE != null && Aura.INSTANCE.isOn()) {
            try {
                Aura.INSTANCE.disable();
            } catch (Throwable var4) {
            }
        }

        if (MaceSpoof.INSTANCE != null && MaceSpoof.INSTANCE.isOn()) {
            try {
                if (wasMaceSpoofEnabledByUs) {
                    MaceSpoof.INSTANCE.setLagMode(this.prevLagMode);
                    MaceSpoof.INSTANCE.setTimerMode(this.prevTimerMode);
                    MaceSpoof.INSTANCE.setLandDistance(this.prevLandDistance);
                }
            } catch (Throwable var3) {
            }

            try {
                MaceSpoof.INSTANCE.disable();
            } catch (Throwable var2) {
            }
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent var1) {
        if (this.internalCam.getValue()) {
            if (this.camRotate.getValue() && mc.cameraEntity != null && mc.cameraEntity.getPos() != null) {
                float[] var2 = RotationManager.getRotation(mc.cameraEntity.getPos());
                this.playerYaw = var2[0];
                this.playerPitch = var2[1];
            }

            if (BaritoneUtil.isPathing()) {
                double[] var3 = MovementUtil.directionSpeed(this.camSpeed.getValue());
                this.prevFakeX = this.fakeX;
                this.prevFakeY = this.fakeY;
                this.prevFakeZ = this.fakeZ;
                this.fakeX = this.fakeX + var3[0];
                this.fakeZ = this.fakeZ + var3[1];
                if (mc.options.forwardKey.isPressed()) {
                    this.fakeY = this.fakeY + this.camVSpeed.getValue();
                }

                if (mc.options.backKey.isPressed()) {
                    this.fakeY = this.fakeY - this.camVSpeed.getValue();
                }
            }
        }
    }

    @EventListener(
        priority = 200
    )
    public void onRotate(UpdateRotateEvent var1) {
        if (this.internalCam.getValue() && !nullCheck() && this.isOn()) {
            if (!BaritoneUtil.isPathing() && !var1.isModified()) {
                var1.setYawWithoutSync(this.playerYaw);
                var1.setPitchWithoutSync(this.playerPitch);
            }
        }
    }

    public void onRender3D(MatrixStack var1) {
        if (this.internalCam.getValue() && !nullCheck() && this.isOn()) {
            this.prevFakeYaw = this.fakeYaw;
            this.prevFakePitch = this.fakePitch;
            this.fakeYaw = this.getYaw();
            this.fakePitch = this.getPitch();
        }
    }

    private float getYaw() {
        return mc.player.getYaw();
    }

    private float getPitch() {
        return mc.player.getPitch();
    }

    @EventListener
    public void onKeyboardInput(KeyboardInputEvent var1) {
        if (this.internalCam.getValue() && !nullCheck() && this.isOn() && mc.player != null) {
            double[] var2 = MovementUtil.directionSpeed(this.camSpeed.getValue());
            this.prevFakeX = this.fakeX;
            this.prevFakeY = this.fakeY;
            this.prevFakeZ = this.fakeZ;
            this.fakeX = this.fakeX + var2[0];
            this.fakeZ = this.fakeZ + var2[1];
            if (mc.options.forwardKey.isPressed()) {
                this.fakeY = this.fakeY + this.camVSpeed.getValue();
            }

            if (mc.options.backKey.isPressed()) {
                this.fakeY = this.fakeY - this.camVSpeed.getValue();
            }

            mc.player.input.movementForward = 0.0F;
            mc.player.input.movementSideways = 0.0F;
            mc.player.input.jumping = false;
            mc.player.input.sneaking = false;
        }
    }

    public double getFakeX() {
        return MathUtil.interpolate(this.prevFakeX, this.fakeX, (double)mc.getRenderTickCounter().getTickDelta(true));
    }

    public double getFakeY() {
        return MathUtil.interpolate(this.prevFakeY, this.fakeY, (double)mc.getRenderTickCounter().getTickDelta(true));
    }

    public double getFakeZ() {
        return MathUtil.interpolate(this.prevFakeZ, this.fakeZ, (double)mc.getRenderTickCounter().getTickDelta(true));
    }

    public float getFakeYaw() {
        return MathUtil.interpolate(this.prevFakeYaw, this.fakeYaw, mc.getRenderTickCounter().getTickDelta(true));
    }

    public float getFakePitch() {
        return MathUtil.interpolate(this.prevFakePitch, this.fakePitch, mc.getRenderTickCounter().getTickDelta(true));
    }

    @EventListener
    public void onTick(ClientTickEvent var1) {
        if (!nullCheck() && !var1.isPost()) {
            if (!this.holdMode.getValue() || mc.options.forwardKey.isPressed()) {
                if (this.tpTimer.passedMs((double)((long)this.delay.getValue()))) {
                    double var2 = mc.player.getX();
                    double var4 = mc.player.getY() + this.height.getValue();
                    double var6 = mc.player.getZ();
                    TPUtils.newTeleport(new Vec3d(var2, var4, var6));
                    mc.player.updatePosition(var2, var4, var6);
                    this.tpTimer.reset();
                }
            }
        }
    }

}
