package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.Forever;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;

import java.awt.*;

public class Totem extends Module {
    public static Totem INSTANCE;
    public final BooleanSetting lockOnHotbar = this.add(new BooleanSetting("LockOnHotbar", true));
    public final SliderSetting posX = this.add(new SliderSetting("PosX", 0, 0, 1920, 1));
    public final SliderSetting posY = this.add(new SliderSetting("PosY", 150, 0, 1080, 1));
    private final int width = 16;
    private final int height = 16;

    public Totem() {
        super("Totem", Category.HUD);
        this.setChinese("图腾");
        INSTANCE = this;
        posX.setValue(Forever.CONFIG.getInt("totem_x", 0));
        posY.setValue(Forever.CONFIG.getInt("totem_y", 150));
        posX.injectTask(() -> Forever.CONFIG.set("totem_x", (int) posX.getValue()));
        posY.injectTask(() -> Forever.CONFIG.set("totem_y", (int) posY.getValue()));
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (this.mc.player == null) {
            return;
        }
        MatrixStack matrixStack = drawContext.getMatrices();
        int renderX, renderY;
        if (lockOnHotbar.getValue()) {
            renderX = this.mc.getWindow().getWidth() / 4 - width / 2;
            renderY = this.mc.getWindow().getScaledHeight() - height;
            renderY = this.mc.player.isCreative() ? (renderY - 28) : (renderY - 38);
        } else {
            renderX = (int) posX.getValue();
            renderY = (int) posY.getValue();
        }
        matrixStack.push();
        int count = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
        drawContext.drawItem(Items.TOTEM_OF_UNDYING.getDefaultStack(),
                renderX + width - 16,
                renderY + height - 16);
        String countText = String.valueOf(count);
        int textX = renderX + width - 16 + 16 - this.mc.textRenderer.getWidth(countText) + 2;
        int textY = renderY + height - 16 + 16 - 9 + 2 + 2;
        drawContext.drawText(this.mc.textRenderer,
                countText,
                textX,
                textY,
                new Color(255, 255, 255, 255).getRGB(),
                true);
        matrixStack.pop();
    }
}