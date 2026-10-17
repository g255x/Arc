package dev.G255.Lumi.core.impl;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class GhostSwapHandler extends ChannelDuplexHandler {
    public static final GhostSwapHandler INSTANCE = new GhostSwapHandler();
    public static final String HANDLER_NAME = "lumi_ghost_swap";

    private volatile boolean swapActive;
    private volatile int crystalSlot = -1;
    private volatile int weaponSlot = -1;

    private GhostSwapHandler() {}

    public void beginSwap(int crystal, int weapon) {
        this.swapActive = true;
        this.crystalSlot = crystal;
        this.weaponSlot = weapon;
    }

    public void endSwap() {
        this.swapActive = false;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof UpdateSelectedSlotC2SPacket slotPacket && this.swapActive) {
            int slot = slotPacket.getSelectedSlot();
            if (slot == this.crystalSlot) {
                super.write(ctx, msg, promise);
                return;
            }
            if (slot == this.weaponSlot) {
                promise.setSuccess();
                return;
            }
        }
        super.write(ctx, msg, promise);
    }
}