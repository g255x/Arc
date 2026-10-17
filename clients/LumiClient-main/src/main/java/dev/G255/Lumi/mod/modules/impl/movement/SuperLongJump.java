package dev.G255.Lumi.mod.modules.impl.movement;

import dev.G255.Lumi.api.events.impl.MoveEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.MovementUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.Module.Category;
import dev.G255.Lumi.mod.modules.settings.impl.BindSetting;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import dev.G255.Lumi.api.events.eventbus.EventListener;

public class SuperLongJump extends Module {
    public static SuperLongJump INSTANCE;
    private final BindSetting burstKey = this.add(new BindSetting("BurstKey", -1));
    private final SliderSetting doubleTapWindow = this.add(new SliderSetting("DoubleTap", 0.3, 0.1, 1.0, 0.05).setSuffix("s"));
    private final SliderSetting airSpeed = this.add(new SliderSetting("Speed", 0.6, 0.1, 2.0, 0.01));
    private final SliderSetting duration = this.add(new SliderSetting("Duration", 2.0, 0.1, 10.0, 0.1).setSuffix("s"));
    private final SliderSetting cooldown = this.add(new SliderSetting("Cooldown", 3.0, 0.0, 30.0, 0.5).setSuffix("s"));
    private final BooleanSetting slowFall = this.add(new BooleanSetting("SlowFall", true));
    private final SliderSetting fallSpeed = this.add(new SliderSetting("FallSpeed", 0.05, 0.01, 0.5, 0.01, this.slowFall::getValue).setSuffix("b/t"));
    private final BooleanSetting disableStrafeSpeed = this.add(new BooleanSetting("DisableStrafeSpeed", true));
    private final Timer doubleTapTimer = new Timer();
    private final Timer durationTimer = new Timer();
    private final Timer cooldownTimer = new Timer();
    private boolean wasKeyPressed = false;
    private boolean waitingForSecondTap = false;
    private boolean burstActive = false;
    private boolean savedStrafeSpeed = false;

    public SuperLongJump() {
        super("SuperLongJump", "Double-tap key to activate super long jump", Category.Movement);
        this.setChinese("");
        this.setChineseDesc("");
        INSTANCE = this;
    }

    public void onEnable() {
        this.burstActive = false;
        this.waitingForSecondTap = false;
        this.wasKeyPressed = false;
    }

    @EventListener
    public void onUpdate(UpdateEvent var1) {
        if (!nullCheck()) {
            boolean var2 = this.burstKey.isPressed();
            if (var2 && !this.wasKeyPressed) {
                if (!this.waitingForSecondTap || this.doubleTapTimer.passedMs((double)((long)(this.doubleTapWindow.getValue() * 1000.0)))) {
                    this.waitingForSecondTap = true;
                    this.doubleTapTimer.reset();
                } else if (this.cooldown.getValue() <= 0.0 || this.cooldownTimer.passedMs((double)((long)(this.cooldown.getValue() * 1000.0)))) {
                    if (this.disableStrafeSpeed.getValue() && StrafeSpeed.INSTANCE != null && StrafeSpeed.INSTANCE.isOn()) {
                        StrafeSpeed.INSTANCE.disable();
                        this.savedStrafeSpeed = true;
                    }

                    this.burstActive = true;
                    this.durationTimer.reset();
                    this.waitingForSecondTap = false;
                }
            }

            this.wasKeyPressed = var2;
            if (this.burstActive && this.durationTimer.passedMs((double)((long)(this.duration.getValue() * 1000.0)))) {
                this.burstActive = false;
                this.cooldownTimer.reset();
                if (this.savedStrafeSpeed && StrafeSpeed.INSTANCE != null && !StrafeSpeed.INSTANCE.isOn()) {
                    StrafeSpeed.INSTANCE.enable();
                }

                this.savedStrafeSpeed = false;
            }
        }
    }

    @EventListener
    public void onMove(MoveEvent var1) {
        if (!nullCheck()) {
            if (this.burstActive && this.durationTimer.passedMs((double)((long)(this.duration.getValue() * 1000.0)))) {
                this.burstActive = false;
                this.cooldownTimer.reset();
                if (this.savedStrafeSpeed && StrafeSpeed.INSTANCE != null && !StrafeSpeed.INSTANCE.isOn()) {
                    StrafeSpeed.INSTANCE.enable();
                }

                this.savedStrafeSpeed = false;
            } else if (this.burstActive) {
                if (!mc.player.isOnGround()) {
                    if (MovementUtil.isMoving()) {
                        if (this.slowFall.getValue()) {
                            double var2 = MovementUtil.getMotionY();
                            if (var2 < 0.0) {
                                MovementUtil.setMotionY(Math.max(var2, -this.fallSpeed.getValue()));
                            }
                        }

                        double[] var4 = MovementUtil.directionSpeed(this.airSpeed.getValue());
                        var1.setX(var4[0]);
                        var1.setZ(var4[1]);
                    }
                }
            }
        }
    }

    public String getInfo() {
        if (this.burstActive) {
            long var1 = (long)(this.duration.getValue() * 1000.0) - this.durationTimer.getMs();
            return String.format("%.1fs", Math.max(0.0, (double)var1 / 1000.0));
        } else {
            return null;
        }
    }
}
