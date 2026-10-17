package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.RotationEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import hoprc.api.events.eventbus.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class AntiHCQ extends Module {

    // ========== 设置项 ==========
    private final SliderSetting range = this.add(
            new SliderSetting("Range", 4.0, 0.0, 6.0, 0.1).setSuffix("m")
    );
    // 添加了一个打水晶的延迟，防止发包过快导致被服务器踢出
    private final SliderSetting delay = this.add(
            new SliderSetting("Delay", 100, 0, 1000).setSuffix("ms")
    );
    private final BooleanSetting rotation = this.add(
            new BooleanSetting("Rotation", true)
    );
    private final BooleanSetting onlyHole = this.add(
            new BooleanSetting("Only Hole", true)
    );

    // ========== 变量 ==========
    private final Timer breakTimer = new Timer();
    private final Timer rotationTimer = new Timer();
    private Vec3d rotationTarget = null;

    public AntiHCQ() {
        super("AntiHCQ", Category.Combat);
        this.setChinese("反邓伟业");
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

        // 如果开启了 Only Hole，但玩家当前不在洞里，就不工作
        if (onlyHole.getValue() && !Forever.HOLE.isHole(mc.player.getBlockPos())) {
            return;
        }

        // 攻击延迟检查
        if (!breakTimer.passedMs(delay.getValue())) return;

        EndCrystalEntity targetCrystal = null;
        double minDist = Double.MAX_VALUE;

        // 遍历实体寻找水晶 (使用你在 CrystalBot 中用到的线程安全实体列表)
        for (Entity entity : Forever.THREAD.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!crystal.isAlive()) continue; // 水晶必须是活着的

            double dist = mc.player.distanceTo(crystal);
            
            // 范围判断
            if (dist > range.getValue()) continue;
            
            // 视线判断 (不能穿墙打水晶)
            if (!mc.player.canSee(crystal)) continue;

            // 寻找距离最近的水晶
            if (dist < minDist) {
                minDist = dist;
                targetCrystal = crystal;
            }
        }

        // 如果找到了符合条件的水晶，执行打爆逻辑
        if (targetCrystal != null) {
            // 处理转头
            if (rotation.getValue()) {
                rotationTarget = targetCrystal.getPos();
                rotationTimer.reset();
            }

            // 发送攻击发包 (参考你的 CrystalBot 打水晶逻辑)
            mc.getNetworkHandler().sendPacket(
                    PlayerInteractEntityC2SPacket.attack(targetCrystal, mc.player.isSneaking())
            );
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.resetLastAttackedTicks();
            
            // 重置攻击冷却计时器
            breakTimer.reset();
        }
    }

    // 转头事件监听
    @EventListener
    public void onRotate(RotationEvent event) {
        // 如果 300 毫秒没有打水晶，就停止锁定视角，恢复正常转头
        if (rotationTimer.passedMs(300)) {
            rotationTarget = null;
        }
        
        // 如果存在目标且开启了转头，修改玩家封包朝向
        if (rotationTarget != null && rotation.getValue()) {
            event.setTarget(rotationTarget, 1.0F, 100.0F);
        }
    }
}