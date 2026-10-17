package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.RotationEvent;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class AntiHCQ extends Module {

    // ========== è®¾ç½®é¡?==========
    private final SliderSetting range = this.add(
            new SliderSetting("Range", 4.0, 0.0, 6.0, 0.1).setSuffix("m")
    );
    // æ·»å äºä¸ä¸ªææ°´æ¶çå»¶è¿ï¼é²æ­¢ååè¿å¿«å¯¼è´è¢«æå¡å¨è¸¢åº
    private final SliderSetting delay = this.add(
            new SliderSetting("Delay", 100, 0, 1000).setSuffix("ms")
    );
    private final BooleanSetting rotation = this.add(
            new BooleanSetting("Rotation", true)
    );
    private final BooleanSetting onlyHole = this.add(
            new BooleanSetting("Only Hole", true)
    );

    // ========== åé ==========
    private final Timer breakTimer = new Timer();
    private final Timer rotationTimer = new Timer();
    private Vec3d rotationTarget = null;

    public AntiHCQ() {
        super("AntiHCQ", Category.Combat);
        this.setChinese("");
        this.setChineseDesc("");
    }

    @Override
    public void onEnable() {
        rotationTarget = null;
        breakTimer.reset();
        rotationTimer.reset();
    }

    @Override
    public void onDisable() {
        rotationTarget = null;
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck()) return;

        // å¦æå¼å¯äº Only Holeï¼ä½ç©å®¶å½åä¸å¨æ´éï¼å°±ä¸å·¥ä½?
        if (onlyHole.getValue() && !Lumi.HOLE.isHole(mc.player.getBlockPos())) {
            return;
        }

        // æ»å»å»¶è¿æ£æ?
        if (!breakTimer.passedMs(delay.getValue())) return;

        EndCrystalEntity targetCrystal = null;
        double minDist = Double.MAX_VALUE;

        // éåå®ä½å¯»æ¾æ°´æ¶ (ä½¿ç¨ä½ å¨ CrystalBot ä¸­ç¨å°ççº¿ç¨å®å¨å®ä½åè¡¨)
        for (Entity entity : Lumi.THREAD.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!crystal.isAlive()) continue; // æ°´æ¶å¿é¡»æ¯æ´»çç?

            double dist = mc.player.distanceTo(crystal);
            
            // èå´å¤æ­
            if (dist > range.getValue()) continue;
            
            // è§çº¿å¤æ­ (ä¸è½ç©¿å¢ææ°´æ?
            if (!mc.player.canSee(crystal)) continue;

            // å¯»æ¾è·ç¦»æè¿çæ°´æ¶
            if (dist < minDist) {
                minDist = dist;
                targetCrystal = crystal;
            }
        }

        // å¦ææ¾å°äºç¬¦åæ¡ä»¶çæ°´æ¶ï¼æ§è¡æçé»è¾
        if (targetCrystal != null) {
            // å¤çè½¬å¤´
            if (rotation.getValue()) {
                rotationTarget = targetCrystal.getPos();
                rotationTimer.reset();
            }

            // åéæ»å»åå?(åèä½ ç?CrystalBot ææ°´æ¶é»è¾)
            mc.getNetworkHandler().sendPacket(
                    PlayerInteractEntityC2SPacket.attack(targetCrystal, mc.player.isSneaking())
            );
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.resetLastAttackedTicks();
            
            // éç½®æ»å»å·å´è®¡æ¶å?
            breakTimer.reset();
        }
    }

    // è½¬å¤´äºä»¶çå¬
    @EventListener
    public void onRotate(RotationEvent event) {
        // å¦æ 300 æ¯«ç§æ²¡æææ°´æ¶ï¼å°±åæ­¢éå®è§è§ï¼æ¢å¤æ­£å¸¸è½¬å¤´
        if (rotationTimer.passedMs(300)) {
            rotationTarget = null;
        }
        
        // å¦æå­å¨ç®æ ä¸å¼å¯äºè½¬å¤´ï¼ä¿®æ¹ç©å®¶å°åæå?
        if (rotationTarget != null && rotation.getValue()) {
            event.setTarget(rotationTarget, 1.0F, 100.0F);
        }
    }
}