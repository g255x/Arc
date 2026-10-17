package dev.perish.core.impl;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.GameLeftEvent;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.math.MathUtil;
import dev.perish.api.utils.math.Timer;
import dev.perish.mod.modules.impl.client.AntiCheat;
import dev.perish.mod.modules.impl.client.Fonts;
import dev.perish.mod.modules.impl.combat.Criticals;
import dev.perish.mod.modules.impl.misc.AutoLog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;

public class ServerManager implements Wrapper {
    public final Timer playerNull = new Timer();
    private final ArrayDeque<Float> tpsResult = new ArrayDeque(20);
    public int currentSlot = -1;
    boolean worldNull = true;
    int lastSlot;
    private long time;
    private long tickTime;
    private float tps;

    public ServerManager() {
        Perish.EVENT_BUS.subscribe(this);
    }

    public static float round2(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    @EventListener(priority = -200)
    public void onPacket(PacketEvent.Send event) {
        if (AntiCheat.INSTANCE.attackCDFix.getValue()) {
            if (event.isCancelled()) {
                return;
            }

            Packet<?> packet = event.getPacket();
            if (!(packet instanceof HandSwingC2SPacket)
                    && (!(packet instanceof PlayerInteractEntityC2SPacket) || Criticals.getInteractType((PlayerInteractEntityC2SPacket) packet) != InteractType.ATTACK)) {
                if (packet instanceof UpdateSelectedSlotC2SPacket packet2 && this.lastSlot != packet2.getSelectedSlot()) {
                    this.lastSlot = packet2.getSelectedSlot();
                    mc.player.resetLastAttackedTicks();
                }
            } else {
                mc.player.resetLastAttackedTicks();
            }
        }
    }

    @EventListener
    public void onLeft(GameLeftEvent event) {
        this.currentSlot = -1;
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet) {
            int packetSlot = packet.getSelectedSlot();
            if (AntiCheat.INSTANCE.noBadSlot.getValue() && packetSlot == this.currentSlot) {
                event.cancel();
                return;
            }

            this.currentSlot = packetSlot;
        }
    }

    public float getTPS() {
        return round2(this.tps);
    }

    public float getCurrentTPS() {
        return round2(20.0F * ((float) this.tickTime / 1000.0F));
    }

    public float getTPSFactor() {
        return this.getTPS() / 20.0F;
    }

    @EventListener(priority = 999)
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            if (this.time != 0L) {
                this.tickTime = System.currentTimeMillis() - this.time;
                if (this.tpsResult.size() > 20) {
                    this.tpsResult.poll();
                }

                this.tpsResult.add(20.0F * (1000.0F / (float) this.tickTime));
                float average = 0.0F;

                for (Float value : this.tpsResult) {
                    average += MathUtil.clamp(value, 0.0F, 20.0F);
                }

                this.tps = average / this.tpsResult.size();
            }

            this.time = System.currentTimeMillis();
        }
    }

    public void onUpdate() {
        if (mc.player == null) {
            this.playerNull.reset();
        }

        if (this.worldNull && mc.world != null) {
            Fonts.INSTANCE.refresh();
            AutoLog.loggedOut = false;
            Perish.MODULE.onLogin();
            this.worldNull = false;
        } else if (!this.worldNull && mc.world == null) {
            Perish.save();
            Perish.MODULE.onLogout();
            this.worldNull = true;
        }
    }
}
