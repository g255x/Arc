package dev.G255.Lumi.core.impl;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.utils.render.Render2DUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.Color;

public class LicenseScreen extends Screen {

    private static final int BUTTON_WIDTH  = 130;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP    = 12;

    private final Screen   parent;
    private final String   deviceCode;
    private final String   savedKey;
    private final boolean  savedAgreement;
    private final Runnable onSuccess;

    private TextFieldWidget keyField;
    private ButtonWidget    continueButton;
    private ButtonWidget    quitButton;
    private ButtonWidget    copyButton;
    private String statusText  = "";
    private int    statusColor = 0xFFFFFFFF;
    private boolean validating  = false;
    private boolean accepted    = false;

    // 布局坐标（由 init() 计算，render() 引用）
    private int boxX, boxY, boxW, boxH;
    private int keyFieldY, buttonY;

    public LicenseScreen(Screen parent, String deviceCode, String savedKey, boolean savedAgreement, Runnable onSuccess) {
        super(Text.literal("LumiClient - \u6FC0\u6D3B\u7801\u9A8C\u8BC1"));
        this.parent        = parent;
        this.deviceCode    = deviceCode;
        this.savedKey      = savedKey;
        this.savedAgreement = savedAgreement;
        this.onSuccess     = onSuccess;
    }

    // ---------- 布局计算 ----------

    private void layout() {
        int w = this.width;
        int h = this.height;

        this.boxW = Math.min(420, w - 24);
        this.boxH = 240;
        this.boxX = (w - this.boxW) / 2;
        this.boxY = (h - this.boxH) / 2;

        int cx = w / 2;
        int fieldW = Math.min(300, this.boxW - 40);
        int fieldH = 20;

        // 输入框Y：保持"输入激活码:"标签 + 一点间距后
        this.keyFieldY = this.boxY + 155;
        int fieldX = cx - fieldW / 2;

        if (this.keyField == null) {
            this.keyField = new TextFieldWidget(this.textRenderer, fieldX, this.keyFieldY, fieldW, fieldH, Text.empty());
            this.keyField.setMaxLength(64);
        } else {
            // 窗口缩放时更新位置大小
            this.keyField.setWidth(fieldW);
            this.keyField.setX(fieldX);
            this.keyField.setY(this.keyFieldY);
        }

        if (this.savedKey != null && !this.savedKey.isEmpty()) {
            this.keyField.setText(this.savedKey);
        }

        // 按钮
        this.buttonY = this.boxY + this.boxH - 36;
        int totalButtonsW = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int btnBaseX = cx - totalButtonsW / 2;

        if (this.continueButton == null) {
            this.continueButton = ButtonWidget.builder(Text.literal("\u7EE7\u7EED"), btn -> onContinue())
                .dimensions(btnBaseX, this.buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            this.quitButton = ButtonWidget.builder(Text.literal("\u9000\u51FA"), btn -> this.client.scheduleStop())
                .dimensions(btnBaseX + BUTTON_WIDTH + BUTTON_GAP, this.buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            this.continueButton.active = this.savedKey != null && !this.savedKey.isEmpty();
        } else {
            this.continueButton.setX(btnBaseX);
            this.continueButton.setY(this.buttonY);
            this.quitButton.setX(btnBaseX + BUTTON_WIDTH + BUTTON_GAP);
            this.quitButton.setY(this.buttonY);
        }

        // 设备码复制按钮（紧贴设备码文字右侧）
        int codeWidth = this.textRenderer.getWidth(this.deviceCode);
        int copyBtnW = 46;
        int copyBtnH = 16;
        int copyBtnX = cx + codeWidth / 2 + 6;
        int copyBtnY = this.boxY + 64 - copyBtnH / 2;
        if (this.copyButton == null) {
            this.copyButton = ButtonWidget.builder(Text.literal("\u590D\u5236"), btn -> {
                this.client.keyboard.setClipboard(this.deviceCode);
                this.statusText  = "\u5DF2\u590D\u5236\u5230\u526A\u8D34\u677F";
                this.statusColor = 0xFF55FF55;
            }).dimensions(copyBtnX, copyBtnY, copyBtnW, copyBtnH).build();
        } else {
            this.copyButton.setX(copyBtnX);
            this.copyButton.setY(copyBtnY);
        }

        // 按钮创建完成后设置文本监听（避免 NPE）
        this.keyField.setChangedListener(text ->
            this.continueButton.active = !text.trim().isEmpty() && !this.validating);
    }

    private void onContinue() {
        String key = this.keyField.getText().trim();
        if (key.isEmpty()) {
            this.statusText  = "\u8BF7\u8F93\u5165\u6FC0\u6D3B\u7801";
            this.statusColor = 0xFFFF5555;
            return;
        }
        this.validating   = true;
        this.statusText   = "\u6B63\u5728\u9A8C\u8BC1...";
        this.statusColor  = 0xFFFFFF55;
        updateButtonStates();

        new Thread(() -> {
            int result = validateKey(this.deviceCode, key);
            this.client.execute(() -> {
                this.validating = false;
                if (result == 0) {
                    saveKey(key);
                    this.accepted    = true;
                    this.statusText  = "\u9A8C\u8BC1\u6210\u529F\uFF01";
                    this.statusColor = 0xFF55FF55;
                    updateButtonStates();
                    Lumi._setLicenseVerified();
                    if (this.onSuccess != null) {
                        this.onSuccess.run();
                    }
                    this.close();
                } else {
                    String msg;
                    switch (result) {
                        case 1:  msg = "\u6FC0\u6D3B\u7801\u683C\u5F0F\u9519\u8BEF"; break;
                        case 2:  msg = "\u6FC0\u6D3B\u7801\u9A8C\u8BC1\u5931\u8D25\uFF0C\u8BF7\u8F93\u5165\u6B63\u786E\u7684\u6FC0\u6D3B\u7801"; break;
                        case 3:  msg = "\u6FC0\u6D3B\u7801\u5DF2\u8FC7\u671F"; break;
                        case 4:  msg = "\u65E0\u6CD5\u8FDE\u63A5\u9A8C\u8BC1\u670D\u52A1\u5668\uFF0C\u8BF7\u68C0\u67E5\u7F51\u7EDC\u540E\u91CD\u8BD5"; break;
                        default: msg = "\u9A8C\u8BC1\u5931\u8D25 (\u9519\u8BEF\u7801: " + result + ")";
                    }
                    this.statusText  = msg;
                    this.statusColor = 0xFFFF5555;
                    this.keyField.setText("");
                    updateButtonStates();
                }
            });
        }).start();
    }

    // ---------- 生命周期 ----------

    @Override
    protected void init() {
        layout();

        this.addSelectableChild(this.keyField);
        this.addDrawableChild(this.copyButton);
        this.addDrawableChild(this.continueButton);
        this.addDrawableChild(this.quitButton);
    }

    @Override
    public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
        this.width  = width;
        this.height = height;
        // 清除后重新 init（标准 Minecraft Screen 行为）
        this.clearChildren();
        this.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int cx = this.width / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, "LumiClient \u6FC0\u6D3B\u7801\u9A8C\u8BC1", cx, this.boxY + 18, 0xFFFFFFFF);

        context.drawCenteredTextWithShadow(this.textRenderer, "\u4F60\u7684\u8BBE\u5907\u7801:", cx, this.boxY + 48, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, this.deviceCode, cx, this.boxY + 64, 0xFFFFFF55);

        context.drawCenteredTextWithShadow(this.textRenderer, "\u8F93\u5165\u6FC0\u6D3B\u7801:", cx, this.boxY + 90, 0xFFAAAAAA);

        this.keyField.render(context, mouseX, mouseY, delta);

        if (!this.statusText.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.statusText, cx, this.boxY + this.boxH - 52, this.statusColor);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        int w = this.width;
        int h = this.height;

        Render2DUtil.drawRect(matrices, 0, 0, w, h, new Color(0, 0, 0, 200));
        Render2DUtil.drawRoundedRect(matrices, this.boxX, this.boxY, this.boxW, this.boxH, 8, new Color(30, 30, 40, 230));
        Render2DUtil.drawRoundedStroke(matrices, this.boxX, this.boxY, this.boxW, this.boxH, 8, new Color(60, 60, 80, 180), 2);
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.keyField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.keyField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused && this.keyField != null) {
            this.keyField.setFocused(false);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void updateButtonStates() {
        this.continueButton.active = !this.validating && !this.accepted;
    }

    public boolean isAccepted() {
        return this.accepted;
    }

    // ---------- 许可证验证 ----------

    private static int validateKey(String deviceCode, String key) {
        try {
            Class<?> lm = Class.forName("dev.G255.Lumi.core.impl.LicenseManager");
            return (int) lm.getMethod("_VT", String.class, String.class)
                    .invoke(null, deviceCode, key);
        } catch (Throwable t) {
            return 2;
        }
    }

    private static void saveKey(String key) {
        try {
            Class<?> lm = Class.forName("dev.G255.Lumi.core.impl.LicenseManager");
            lm.getMethod("_entry", int.class, Object[].class)
                    .invoke(null, 3, new Object[]{key, true});
        } catch (Throwable ignored) {}
    }
}