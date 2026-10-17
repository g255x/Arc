package dev.G255.Lumi.mod.modules.impl.movement;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.KeyboardInputEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.MovementUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity; // æ³¨æï¼æ­¤å¤çå¯¼åè¯·æ ¹æ®ä½ å®¢æ·ç«¯çå·ä½æ å°(Mappings)ä¿®æ¹
import net.minecraft.entity.vehicle.BoatEntity;

public class BoatFly extends Module {
    public static BoatFly INSTANCE;
    private final SliderSetting speedConfig = this.add(new SliderSetting("Speed", 2.5, 0.1, 10.0));
    private final SliderSetting vSpeedConfig = this.add(new SliderSetting("VerticalSpeed", 1.0, 0.1, 5.0));
    private final SliderSetting doubleTapDelay = this.add(new SliderSetting("DoubleTapDelay", 300, 100, 1000)); // åå»å¤å®çæå¤§é´é?æ¯«ç§)
    private final Timer shiftTimer = new Timer();
    private boolean wasSneaking = false;

    public BoatFly() {
        super("BoatFly", "Allows flying with boats at high velocity.", Module.Category.Movement);
        this.setChinese("");
        this.setChineseDesc("");
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