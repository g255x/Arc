package dev.perish.core.impl;


import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.DeathEvent;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.api.events.impl.TotemEvent;
import dev.perish.api.utils.Wrapper;
import dev.perish.mod.modules.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class PopManager implements Wrapper {
    public final ConcurrentHashMap<String, Integer> popContainer = new ConcurrentHashMap<>();
    private final List<PlayerEntity> deadPlayer = new ArrayList();

    public PopManager() {
        this.init();
    }


    public void init() {
        Perish.EVENT_BUS.subscribe(this);
    }

    public int getPop(String s) {
        return this.popContainer.getOrDefault(s, 0);
    }

    public int getPop(PlayerEntity player) {
        return this.getPop(player.getName().getString());
    }

    public void onUpdate() {
        if (!Module.nullCheck()) {
            for (AbstractClientPlayerEntity player : Perish.THREAD.getPlayers()) {
                if (player == null || !player.isDead()) {
                    this.deadPlayer.remove(player);
                } else if (!this.deadPlayer.contains(player)) {
                    Perish.EVENT_BUS.post(DeathEvent.get(player));
                    this.onDeath(player);
                    this.deadPlayer.add(player);
                }
            }
        }
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!Module.nullCheck()) {
            if (event.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35 && packet.getEntity(mc.world) instanceof PlayerEntity player
            ) {
                this.onTotemPop(player);
            }
        }
    }

    public void onDeath(PlayerEntity player) {
        this.popContainer.remove(player.getName().getString());
    }

    public void onTotemPop(PlayerEntity player) {
        int l_Count = 1;
        if (this.popContainer.containsKey(player.getName().getString())) {
            l_Count = this.popContainer.get(player.getName().getString());
            this.popContainer.put(player.getName().getString(), ++l_Count);
        } else {
            this.popContainer.put(player.getName().getString(), l_Count);
        }

        Perish.EVENT_BUS.post(TotemEvent.get(player));
    }
}
