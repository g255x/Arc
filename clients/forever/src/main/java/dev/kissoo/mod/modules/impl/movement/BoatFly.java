package dev.kissoo.mod.modules.impl.movement;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.KeyboardInputEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity; // 注意：此处的导包请根据你客户端的具体映射(Mappings)修改
import net.minecraft.entity.vehicle.BoatEntity;

public class BoatFly extends Module {
    public static BoatFly INSTANCE;
    private final SliderSetting speedConfig = this.add(new SliderSetting("Speed", 2.5, 0.1, 10.0));
    private final SliderSetting vSpeedConfig = this.add(new SliderSetting("VerticalSpeed", 1.0, 0.1, 5.0));
    private final SliderSetting doubleTapDelay = this.add(new SliderSetting("DoubleTapDelay", 300, 100, 1000)); // 双击判定的最大间隔(毫秒)
    private final Timer shiftTimer = new Timer();
    private boolean wasSneaking = false;

    public BoatFly() {
        super("BoatFly", Module.Category.Movement);
        this.setChinese("飞船");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        this.shiftTimer.reset();
        this.wasSneaking = false;
    }

    @Override
    public void onDisable() {
        if (!nullCheck() && mc.player.getVehicle() != null) {
            mc.player.getVehicle().setNoGravity(false); 
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;

        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null || !(vehicle instanceof BoatEntity)) {
            return;
        }

        vehicle.setNoGravity(true);
        double motionY = 0.0;
        if (mc.options.jumpKey.isPressed()) {
            motionY = this.vSpeedConfig.getValue();
        } else if (mc.options.sneakKey.isPressed()) {
            motionY = -this.vSpeedConfig.getValue();
        }

        double[] move = MovementUtil.directionSpeed(this.speedConfig.getValueFloat());

        vehicle.setVelocity(move[0], motionY, move[1]); 

        vehicle.setYaw(mc.player.getYaw()); 
    }

    @EventListener(priority = -100)
    public void keyboard(KeyboardInputEvent event) {
        if (nullCheck()) return;

        Entity vehicle = mc.player.getVehicle();

        if (vehicle != null && vehicle instanceof BoatEntity) {
            boolean isSneakKeyDown = mc.options.sneakKey.isPressed();

            if (isSneakKeyDown && !this.wasSneaking) {
                if (!this.shiftTimer.passed((long) this.doubleTapDelay.getValue())) {
                    mc.player.dismountVehicle();
                } else {
                    this.shiftTimer.reset();
                }
            }
            this.wasSneaking = isSneakKeyDown;
            mc.player.input.sneaking = false;
        }
    }
}