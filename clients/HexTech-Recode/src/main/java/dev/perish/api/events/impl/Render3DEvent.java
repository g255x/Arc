package dev.perish.api.events.impl;

import dev.perish.api.utils.render.Render3DUtil;
import java.awt.*;
import net.minecraft.util.math.Box;
import net.minecraft.client.util.math.MatrixStack;

public class Render3DEvent {
    private static final Render3DEvent INSTANCE = new Render3DEvent();
    public MatrixStack matrixStack;
    public float tickDelta;

    public static Render3DEvent get(MatrixStack matrixStack, float tickDelta) {
        INSTANCE.matrixStack = matrixStack;
        INSTANCE.tickDelta = tickDelta;
        return INSTANCE;
    }

    public void drawBox(Box box, Color color) {
        Render3DUtil.drawBox(this.matrixStack, box, color);
    }

    public void drawFill(Box box, Color color) {
        Render3DUtil.drawFill(this.matrixStack, box, color);
    }
}
