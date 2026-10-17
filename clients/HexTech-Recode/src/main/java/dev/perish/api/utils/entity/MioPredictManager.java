package dev.perish.api.utils.entity;

import dev.perish.Perish;
import dev.perish.api.events.Event;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClientTickEvent;
import dev.perish.api.events.impl.GameLeftEvent;
import dev.perish.api.events.impl.RemoveEntityEvent;
import dev.perish.api.utils.Wrapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.client.network.AbstractClientPlayerEntity;

/**
 * 移植自 OpenMio Class_0628，管理每个在线玩家的 {@link MioPlayerPredictor}。
 *
 * 每 tick 自动更新所有玩家的预判包围盒；
 * 通过 {@link #getBox(PlayerEntity, int)} 获取某玩家指定 tick 后的预判结果。
 *
 * 单例由 {@link Perish#MIO_PREDICT} 持有。
 */
public class MioPredictManager implements Wrapper {

    /** entity ID → 对应的预判器 */
    private final Map<Integer, MioPlayerPredictor> predictors =
            Collections.synchronizedMap(new HashMap<>());

    public MioPredictManager() {
        Perish.EVENT_BUS.subscribe(this);
    }

    @EventListener
    private void onTick(ClientTickEvent event) {
        if (event.stage != Event.Stage.Pre) return;
        if (mc.world == null) return;

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            predictors.computeIfAbsent(player.getId(), id -> new MioPlayerPredictor(player));
            predictors.get(player.getId()).tick();
        }
    }

    @EventListener
    private void onRemoveEntity(RemoveEntityEvent event) {
        predictors.remove(event.getEntity().getId());
    }

    @EventListener
    private void onGameLeft(GameLeftEvent event) {
        predictors.clear();
    }

    /**
     * 返回 {@code player} 在 {@code ticks} tick 后的预判包围盒。
     * 若该玩家尚无数据，则回退到其当前包围盒。
     */
    public Box getBox(PlayerEntity player, int ticks) {
        MioPlayerPredictor predictor = predictors.get(player.getId());
        return predictor != null ? predictor.getBox(ticks) : player.getBoundingBox();
    }
}
