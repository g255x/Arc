package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.Forever;
import dev.kissoo.api.utils.render.TextUtil;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.Random;

public class FPSHud extends Module implements HudElement {
    public static FPSHud INSTANCE;

    private int hudX = 2, hudY = 50;
    private AlignMode alignMode = AlignMode.LEFT;

    // --- 基础渲染设置 ---
    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));

    // --- 假 FPS 功能设置 ---
    public enum FakeMode {
        Multiplier, // 倍率模式
        Custom      // 自定义模式
    }

    // 假FPS总开关
    private final BooleanSetting enableFake = add(
            new BooleanSetting("FakeFPS", false)
    );

    // 模式选择：仅在开启 FakeFPS 时展开
    private final EnumSetting<FakeMode> fakeMode = add(
            new EnumSetting<>("FakeMode", FakeMode.Custom,
                    () -> enableFake.getValue())
    );

    // 倍率设置：仅在 开启FakeFPS 且 模式为倍率 时展开
    private final SliderSetting multiplier = add(
            new SliderSetting("Multiplier", 2.0, 1.0, 10.0, 0.1,
                    () -> enableFake.getValue() && fakeMode.is(FakeMode.Multiplier)).setSuffix("x")
    );

    // 自定义跳动设置：仅在 开启FakeFPS 且 模式为自定义 时展开
    private final SliderSetting minFps = add(
            new SliderSetting("Min", 120, 0, 9999, 1,
                    () -> enableFake.getValue() && fakeMode.is(FakeMode.Custom))
    );
    private final SliderSetting maxFps = add(
            new SliderSetting("Max", 240, 0, 9999, 1,
                    () -> enableFake.getValue() && fakeMode.is(FakeMode.Custom))
    );
    private final SliderSetting updateDelay = add(
            new SliderSetting("UpdateDelay", 500, 50, 2000, 50,
                    () -> enableFake.getValue() && fakeMode.is(FakeMode.Custom)).setSuffix("ms")
    );

    // 内部控制变量
    private int currentFakeFps = 0;
    private long lastUpdateTime = 0;
    private final Random random = new Random();

    public FPSHud() {
        super("FPS", Category.HUD);
        setChinese("FPS显示");
        INSTANCE = this;
    }

    private String getText() {
        // 如果没有开启假FPS，直接返回真实的FPS
        if (!enableFake.getValue()) {
            return "FPS §f" + Forever.FPS.getFps();
        }

        // 如果开启了假FPS，走下面的伪造逻辑
        int displayFps = 0;

        if (fakeMode.getValue() == FakeMode.Multiplier) {
            // 真实FPS 乘 倍率
            displayFps = (int) (Forever.FPS.getFps() * multiplier.getValue());
        }
        else if (fakeMode.getValue() == FakeMode.Custom) {
            // 自定义模式：带延迟的随机数生成
            if (System.currentTimeMillis() - lastUpdateTime >= updateDelay.getValue()) {
                int min = (int) minFps.getValue();
                int max = (int) maxFps.getValue();

                // 防呆设计：如果 Min 设得比 Max 大，自动交换，防止崩溃
                if (min > max) {
                    int temp = min;
                    min = max;
                    max = temp;
                }

                if (min == max) {
                    currentFakeFps = min;
                } else {
                    currentFakeFps = min + random.nextInt(max - min + 1);
                }
                lastUpdateTime = System.currentTimeMillis();
            }
            displayFps = currentFakeFps;
        }

        return "FPS §f" + displayFps;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        renderElement(drawContext, tickDelta);
    }

    @Override
    public String getElementName() {
        return getChinese() != null ? getChinese() : getName();
    }

    @Override
    public int getElementX() {
        if (alignMode == AlignMode.RIGHT) {
            return hudX - getElementWidth();
        }
        return hudX;
    }

    @Override
    public void setElementX(int x) {
        if (alignMode == AlignMode.RIGHT) {
            hudX = x + getElementWidth();
        } else {
            hudX = x;
        }
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
        return (int) FontManager.getWidth(getText()) + 4;
    }

    @Override
    public int getElementHeight() {
        return (int) FontManager.getFontHeight() + 2;
    }

    @Override
    public boolean isElementVisible() {
        return isOn();
    }

    @Override
    public AlignMode getAlignMode() {
        return alignMode;
    }

    @Override
    public void setAlignMode(AlignMode mode) {
        if (mode == alignMode) return;
        if (mode == AlignMode.RIGHT && alignMode == AlignMode.LEFT) {
            hudX = hudX + getElementWidth();
        } else if (mode == AlignMode.LEFT && alignMode == AlignMode.RIGHT) {
            hudX = hudX - getElementWidth();
        }
        alignMode = mode;
    }

    @Override
    public int getRightAnchorX() {
        if (alignMode == AlignMode.RIGHT) {
            return hudX;
        }
        return hudX + getElementWidth();
    }

    @Override
    public void setRightAnchorX(int rightX) {
        if (alignMode == AlignMode.RIGHT) {
            hudX = rightX;
        } else {
            hudX = rightX - getElementWidth();
        }
    }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        int renderX = getElementX();
        TextUtil.drawString(context, getText(), renderX, hudY, color.getValue().getRGB(), shadow.getValue());
    }
}