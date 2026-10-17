package dev.perish.api.events.impl;

import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.math.MatrixStack;

public class HeldItemRendererEvent {
    private static final HeldItemRendererEvent INSTANCE = new HeldItemRendererEvent();
    private Hand hand;
    private ItemStack item;
    private float ep;
    private MatrixStack stack;

    public HeldItemRendererEvent() {
    }

    public static HeldItemRendererEvent get(Hand hand, ItemStack item, float equipProgress, MatrixStack stack) {
        INSTANCE.hand = hand;
        INSTANCE.item = item;
        INSTANCE.ep = equipProgress;
        INSTANCE.stack = stack;
        return INSTANCE;
    }

    public Hand getHand() {
        return this.hand;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public float getEp() {
        return this.ep;
    }

    public MatrixStack getStack() {
        return this.stack;
    }
}
