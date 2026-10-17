package dev.G255.Lumi.mod.modules.impl.movement;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.exploit.Phase;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.EntityPose;

public class LieDownMove extends Module {
    public final BooleanSetting autoJump = this.add(new BooleanSetting("AutoJump", true));
    public final BooleanSetting autoPhase = this.add(new BooleanSetting("AutoPhase", true).setParent());
    public final SliderSetting waitTime = this.add(new SliderSetting("WaitTime", 500, 0, 2000, 50, () -> autoPhase.isOpen()));

    private final Timer phaseTimer = new Timer();

    public LieDownMove() {
        super("CrawlHelper", Category.Movement);
        this.setChinese("");
        this.setChineseDesc("");
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (event.isPost()) return;

        boolean isCrawlingPose = mc.player.getPose() == EntityPose.SWIMMING;
        boolean isLowHeight = mc.player.getBoundingBox().getLengthY() < 1.0;
        boolean isNotInWater = !mc.player.isTouchingWater();
        boolean isReallyCrawling = isCrawlingPose && isLowHeight && isNotInWater;
        boolean isMoving = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;

        if (isReallyCrawling) {
            boolean isCollidingWall = mc.player.horizontalCollision;
            boolean isPhaseActive = Phase.INSTANCE != null && Phase.INSTANCE.isOn();
            if (autoJump.getValue() && isMoving && mc.player.isOnGround()) {
                boolean shouldSuppressJump = (autoPhase.getValue() && isCollidingWall) || isPhaseActive;
                if (!shouldSuppressJump) {
                    mc.player.jump();
                }
            }
            if (autoPhase.getValue() && isMoving) {
                if (isCollidingWall) {
                    if (phaseTimer.passedMs(waitTime.getValueInt())) {
                        if (Phase.INSTANCE != null) {
                            Phase.INSTANCE.enable();
                            phaseTimer.reset();
                        }
                    }
                } else {
                    phaseTimer.reset();
                }
            } else {
                phaseTimer.reset();
            }
        } else {
            phaseTimer.reset();
        }
    }

    @Override
    public void onEnable() {
        phaseTimer.reset();
    }
}