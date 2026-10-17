package dev.perish.mod.modules.impl.render;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClientTickEvent;
import dev.perish.api.events.impl.DeathEvent;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;

public class KillThunder extends Module {
    public final SliderSetting amount = this.add(new SliderSetting("Amount", 5, 1, 20, 1));
    public final SliderSetting duration = this.add(new SliderSetting("Duration", 3.0, 0.5, 10.0, 0.1));

    private final List<ThunderTask> thunderTasks = new ArrayList<>();

    public KillThunder() {
        super("KillThunder", Category.Render);
        this.setChinese("击杀雷霆");
    }

    @EventListener
    public void onPlayerDeath(DeathEvent event) {
        if (!nullCheck()) {
            PlayerEntity player = event.getPlayer();
            if (player != null && player != mc.player) {
                if (mc.player.distanceTo(player) < 20) {
                    thunderTasks.add(new ThunderTask(player.getX(), player.getY(), player.getZ(), amount.getValueInt(), duration.getValueFloat()));
                }
            }
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck()) return;

        Iterator<ThunderTask> iterator = thunderTasks.iterator();
        while (iterator.hasNext()) {
            ThunderTask task = iterator.next();
            task.tick();
            if (task.isFinished()) {
                iterator.remove();
            }
        }
    }

    private class ThunderTask {
        private final double x, y, z;
        private final int totalAmount;
        private final long durationMs;
        private final long startTime;
        private int spawnedCount = 0;

        public ThunderTask(double x, double y, double z, int totalAmount, float durationSeconds) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.totalAmount = totalAmount;
            this.durationMs = (long) (durationSeconds * 1000L);
            this.startTime = System.currentTimeMillis();
        }

        public void tick() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > durationMs) {
                while (spawnedCount < totalAmount) {
                    spawnLightning();
                    spawnedCount++;
                }
                return;
            }

            double progress = (double) elapsed / durationMs;
            int targetSpawnCount = (int) (progress * totalAmount);
            
            while (spawnedCount < targetSpawnCount && spawnedCount < totalAmount) {
                spawnLightning();
                spawnedCount++;
            }
        }

        private void spawnLightning() {
            if (mc.world != null) {
                LightningEntity lightningEntity = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
                double offsetX = (Math.random() - 0.5) * 1.5;
                double offsetZ = (Math.random() - 0.5) * 1.5;
                lightningEntity.updatePosition(x + offsetX, y, z + offsetZ);
                lightningEntity.refreshPositionAfterTeleport(x + offsetX, y, z + offsetZ);
                mc.world.addEntity(lightningEntity);
            }
        }

        public boolean isFinished() {
            return spawnedCount >= totalAmount || (System.currentTimeMillis() - startTime > durationMs);
        }
    }
}
