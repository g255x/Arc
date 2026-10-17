package dev.perish.mod.modules.impl.movement;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.MoveEvent;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.MovementUtil;
import dev.perish.api.utils.math.Timer;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.combat.AutoAnchor;
import dev.perish.mod.modules.impl.combat.AutoCrystal;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.effect.StatusEffects;

import static dev.perish.mod.modules.impl.combat.AutoPush.isInWeb;

public class BlockStrafe extends Module {
    public static BlockStrafe INSTANCE;
    public final SliderSetting wspeed =
            add(new SliderSetting("WebSpeed", 10., 0, 50, 0.01)).setSuffix("÷2");
    public final SliderSetting aSpeed =
            add(new SliderSetting("BlockSpeed", 10.02, 0, 30, 0.01));
    public final SliderSetting aForward =
            add(new SliderSetting("AnchorForward", 1, 0, 30, 0.25));
    public final SliderSetting bSpeed =
            add(new SliderSetting("currentPos", 10, 0, 30, 0.01));
    public final BooleanSetting noInWeb =
            add(new BooleanSetting("NoInWeb", true));
    public final BooleanSetting antiSlowFalling = this.add(new BooleanSetting("AntiSlowFalling", true));
    public final SliderSetting motionY = add(new SliderSetting("MotionY", 0.05, 0.0, 1.0, 0.001));
    public final SliderSetting delay = add(new SliderSetting("Delay", 0, 0.0, 500.0));
    public final Timer delayTimer = new Timer();

    public BlockStrafe() {
        super("BlockStrafe", Category.Movement);
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(MoveEvent event) {
        if (antiSlowFalling.getValue() &&
                mc.player != null &&
                !mc.player.isSpectator() &&
                mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) &&
                delayTimer.passedMs(delay.getValue()) &&
                EntityUtil.isInsideBlock() &&
                MovementUtil.isMoving() &&
                mc.player.isOnGround() &&
                mc.player.fallDistance <= 0) {
            MovementUtil.setMotionY(motionY.getValue());
            delayTimer.reset();
        }
    }

    @EventListener
    public void onMove(MoveEvent event) {
        if (!EntityUtil.isInsideBlock()) return;
        if (mc.player != null && noInWeb.getValue() && isInWeb(mc.player)) return;
        //use WebSpeed *? no return.
        double speed = AutoCrystal.crystalPos == null
                ? (isInWeb(mc.player)
                ? this.wspeed.getValue()

                : aSpeed.getValue())
                : bSpeed.getValue();

        double moveSpeed = 0.2873 / 100 * speed;
        double n = 0;
        if (mc.player != null) {
            n = mc.player.input.movementForward;
        }
        double n2 = 0;
        if (mc.player != null) {
            n2 = mc.player.input.movementSideways;
        }
        double n3 = 0;
        if (mc.player != null) {
            n3 = mc.player.getYaw();
        }
        if (n == 0.0 && n2 == 0.0) {
            if (AutoAnchor.INSTANCE == null || AutoAnchor.INSTANCE.currentPos == null) {
                event.setX(0.0);
                event.setZ(0.0);
            } else {
                moveSpeed = 0.2873 / 100 * aForward.getValue();
                event.setX(1 * moveSpeed * -Math.sin(Math.toRadians(n3)));
                event.setZ(1 * moveSpeed * Math.cos(Math.toRadians(n3)));
            }
            return;
        } else if (n != 0.0 && n2 != 0.0) {
            n *= Math.sin(0.7853981633974483);
            n2 *= Math.cos(0.7853981633974483);
        }
        event.setX((n * moveSpeed * -Math.sin(Math.toRadians(n3)) + n2 * moveSpeed * Math.cos(Math.toRadians(n3))));
        event.setZ((n * moveSpeed * Math.cos(Math.toRadians(n3)) - n2 * moveSpeed * -Math.sin(Math.toRadians(n3))));
    }
}
