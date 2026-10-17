package dev.perish.api.utils.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

public class PlayerEntityPredict {
    public final PlayerEntity player;
    public final PlayerEntity predict;

    /** Normal 预判模式：使用 PredictUtil 模拟运动轨迹。 */
    public PlayerEntityPredict(
            PlayerEntity player, double maxMotionY, int ticks, int simulation, boolean step, boolean doubleStep, boolean jump, boolean inBlockPause
    ) {
        this.player = player;
        if (ticks > 0) {
            this.predict = new CopyPlayerEntity(player, true, maxMotionY, ticks, simulation, step, doubleStep, jump, inBlockPause);
        } else {
            this.predict = player;
        }
    }

    /**
     * Mio 预判模式：使用 {@link MioPredictManager} 返回的预判包围盒。
     * 当 {@code predictBox} 为 null 时退化为直接使用原始玩家（即无预判）。
     */
    public PlayerEntityPredict(PlayerEntity player, Box predictBox) {
        this.player = player;
        this.predict = predictBox != null ? new CopyPlayerEntity(player, predictBox) : player;
    }
}
