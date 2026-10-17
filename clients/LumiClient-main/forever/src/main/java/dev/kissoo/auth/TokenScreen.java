package dev.kissoo.auth;

import dev.kissoo.Forever;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class TokenScreen extends Screen {
    private TextFieldWidget tokenField;
    private String errorMessage = "";
    private boolean verifying = false;

    public TokenScreen() {
        super(Text.literal("Forever 验证"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int fieldWidth = 260;

        this.tokenField = new TextFieldWidget(
                this.textRenderer,
                centerX - fieldWidth / 2, this.height / 2 - 10,
                fieldWidth, 20,
                Text.literal("Token")
        );
        this.tokenField.setMaxLength(64);
        this.tokenField.setPlaceholder(Text.literal("输入你的 Token..."));
        this.tokenField.setEditableColor(0xFFFFFF);
        this.addSelectableChild(this.tokenField);
        this.setInitialFocus(this.tokenField);

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("验证"),
                btn -> doVerify()
        ).dimensions(centerX - 50, this.height / 2 + 20, 100, 20).build());
    }

    private void doVerify() {
        String token = tokenField.getText().trim();
        if (token.isBlank()) {
            errorMessage = "请输入 Token";
            return;
        }
        verifying = true;
        errorMessage = "验证中...";

        new Thread(() -> {
            KeyAuth.AuthResult result = KeyAuth.verifyToken(token);
            MinecraftClient.getInstance().execute(() -> {
                verifying = false;
                if (result.allowed()) {
                    KeyAuth.saveToken(token, KeyAuth.getOwner());
                    AuthBootstrap.markVerified(KeyAuth.getOwner(), KeyAuth.getHwid());
                    errorMessage = "";
                    if (this.client != null) {
                        this.client.setScreen(null);
                    }
                } else {
                    errorMessage = result.reason().isBlank() ? "验证失败" : result.reason();
                }
            });
        }, "Forever-Auth-UI").start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark background
        context.fill(0, 0, this.width, this.height, new Color(10, 10, 20).getRGB());

        String title = "Forever Client - 验证";
        context.drawCenteredTextWithShadow(this.textRenderer, title, this.width / 2, this.height / 2 - 50, 0x7C3AED);

        String hint = "请输入购买后获得的 Token";
        context.drawCenteredTextWithShadow(this.textRenderer, hint, this.width / 2, this.height / 2 - 35, 0xAAAAAA);

        this.tokenField.render(context, mouseX, mouseY, delta);

        if (!errorMessage.isBlank()) {
            int color = errorMessage.contains("中") ? 0xFFFF00 : 0xFF4444;
            context.drawCenteredTextWithShadow(this.textRenderer, errorMessage, this.width / 2, this.height / 2 + 50, color);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (!verifying) doVerify();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Can't escape this screen
    }

    @Override
    public void close() {
        // Only allow close if verified
        if (!KeyAuth.isAllowed()) {
            MinecraftClient.getInstance().scheduleStop();
        }
        super.close();
    }
}
