package dev.kissoo.mod.modules.impl.movement;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.MoveEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.option.KeyBinding;

public class BurstStrafe extends Module {
    public static BurstStrafe INSTANCE;
    private final BindSetting burstKey = this.add(new BindSetting("BurstKey", -1));
    private final SliderSetting doubleTapWindow = this.add(new SliderSetting("DoubleTapWindow", 300, 100, 1000));
    private final SliderSetting airSpeed = this.add(new SliderSetting("AirSpeed", 1.5, 0.5, 5.0, 0.1));
    private final BooleanSetting slowFall = this.add(new BooleanSetting("SlowFall", false));
    private final SliderSetting fallSpeed = this.add(new SliderSetting("FallSpeed", 0.1, 0.0, 1.0, 0.01));
    private final Timer doubleTapTimer = new Timer();
    private boolean wasKeyPressed, waitingForSecondTap, burstActive;

    public BurstStrafe() {
        super("BurstStrafe", "Double-tap to burst strafe in air", Module.Category.Movement);
        this.setChinese("爆发侧移");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;
        boolean keyPressed = false;
        if (burstKey.getValue() >= 0 && burstKey.getValue() < mc.options.allKeys.length) {
            KeyBinding kb = mc.options.allKeys[burstKey.getValue()];
            if (kb != null) keyPressed = kb.isPressed();
        }
        if (keyPressed && !wasKeyPressed) {
            if (waitingForSecondTap && !doubleTapTimer.passedMs(doubleTapWindow.getValue())) {
                burstActive = true;
                waitingForSecondTap = false;
            } else {
                doubleTapTimer.reset();
                waitingForSecondTap = true;
            }
        }
        wasKeyPressed = keyPressed;
        if (waitingForSecondTap && doubleTapTimer.passedMs(doubleTapWindow.getValue())) {
            waitingForSecondTap = false;
        }
    }

    @EventListener
    public void onMove(MoveEvent event) {
        if (nullCheck() || !burstActive || mc.player.isOnGround()) {
            if (mc.player != null && mc.player.isOnGround()) burstActive = false;
            return;
        }
        double yaw = Math.toRadians(mc.player.getYaw());
        double forward = mc.player.input.movementForward;
        double sideways = mc.player.input.movementSideways;
        if (forward == 0 && sideways == 0) { burstActive = false; return; }
        double strafeX = -Math.sin(yaw) * sideways + Math.cos(yaw) * forward;
        double strafeZ = Math.cos(yaw) * sideways + Math.sin(yaw) * forward;
        double len = Math.sqrt(strafeX * strafeX + strafeZ * strafeZ);
        event.setX(strafeX / len * airSpeed.getValue());
        event.setZ(strafeZ / len * airSpeed.getValue());
        if (slowFall.getValue()) event.setY(-fallSpeed.getValue());
    }

    @Override
    public String getInfo() { return burstActive ? "Active" : null; }
}
