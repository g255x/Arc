package dev.kissoo.mod.modules.impl.hud;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.Render2DEvent;
import dev.kissoo.api.utils.math.SmoothAnimationUtil;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public class PlayerModel extends Module implements HudElement {
    public static PlayerModel INSTANCE;

    private int hudX = 560;
    private int hudY = 240;

    private final SliderSetting modelSize = this.add(new SliderSetting("ModelSize", 36.0, 5.0, 200.0, 1.0));

    private final SmoothAnimationUtil smoothLookX = new SmoothAnimationUtil(0, 0.18, 0.05);
    private final SmoothAnimationUtil smoothLookY = new SmoothAnimationUtil(0, 0.18, 0.05);

    public PlayerModel() {
        super("PlayerModel", Module.Category.HUD);
        this.setChinese("玩家模型");
        INSTANCE = this;
    }

    @Override
    public String getElementName() {
        return getChinese() != null ? getChinese() : getName();
    }

    @Override
    public int getElementX() {
        return hudX;
    }

    @Override
    public void setElementX(int x) {
        hudX = x;
    }

    @Override
    public int getElementY() {
        return hudY;
    }

    @Override
    public void setElementY(int y) {
        hudY = y;
    }

    @Override
    public int getElementWidth() {
        int size = (int) modelSize.getValueFloat();
        return size * 2;
    }

    @Override
    public int getElementHeight() {
        int size = (int) modelSize.getValueFloat();
        return size * 2;
    }

    @Override
    public boolean isElementVisible() {
        return isOn();
    }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        if (!isOn() || mc.player == null || mc.options.hudHidden) return;
        renderModel(context);
    }

    @EventListener
    public void onRender2D(Render2DEvent event) {
        if (!isOn() || mc.player == null || mc.options.hudHidden) return;
        renderModel(event.drawContext);
    }

    private void renderModel(DrawContext context) {
        int size = (int) this.modelSize.getValueFloat();

        int left = this.hudX;
        int top = this.hudY;
        int right = left + getElementWidth();
        int bottom = top + getElementHeight();
        float centerX = (left + right) / 2.0f;
        float centerY = (top + bottom) / 2.0f;

        double windowWidth = mc.getWindow().getScaledWidth();
        double windowHeight = mc.getWindow().getScaledHeight();

        float rawLookX = (float) (centerX - (mc.mouse.getX() * windowWidth / mc.getWindow().getWidth()));
        float rawLookY = (float) (centerY - (mc.mouse.getY() * windowHeight / mc.getWindow().getHeight()));

        smoothLookX.setTarget(rawLookX);
        smoothLookY.setTarget(rawLookY);

        float lookX = (float) smoothLookX.update();
        float lookY = (float) smoothLookY.update();

        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 0.0F, 1000.0F);

        InventoryScreen.drawEntity(
                context,
                left,
                top,
                right,
                bottom,
                size,
                0.0625f,
                lookX,
                lookY,
                mc.player
        );

        context.getMatrices().pop();
    }
}