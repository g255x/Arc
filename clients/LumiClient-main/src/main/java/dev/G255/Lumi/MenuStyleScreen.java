package dev.G255.Lumi;

import dev.G255.Lumi.core.impl.ConfigManager;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class MenuStyleScreen extends Screen {
    private final Screen parent;
    private final String originalStyle;
    private final boolean originalMouseShake;
    private final float originalShakeStrength;
    private final boolean originalShakeReverse;
    private final String originalBgPath;
    private final boolean originalBlurBg;
    private final float originalBlurRadius;

    public MenuStyleScreen(Screen parent, String originalStyle) {
        super(Text.literal("LumiClient 设置"));
        this.parent = parent;
        this.originalStyle = originalStyle != null ? originalStyle : ConfigManager.menuStyle;
        this.originalMouseShake = ConfigManager.menuMouseShake;
        this.originalShakeStrength = ConfigManager.menuMouseShakeStrength;
        this.originalShakeReverse = ConfigManager.menuMouseShakeReverse;
        this.originalBgPath = ConfigManager.menuBackgroundPath;
        this.originalBlurBg = ConfigManager.blurScreenBackground;
        this.originalBlurRadius = ConfigManager.blurScreenRadius;
    }

    @Override
    protected void init() {
        int w = this.width;
        int h = this.height;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 5;
        int cx = (w - buttonWidth) / 2;
        String currentStyle = ConfigManager.menuStyle;

        int y = h / 2 - 100;

        String styleName = currentStyle.equals("aurora") ? "极光" : "Alien v4";
        String themeLabel = "主题: " + styleName;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(themeLabel), (btn) -> {
            ConfigManager.menuStyle = currentStyle.equals("aurora") ? "alien" : "aurora";
            MinecraftClient.getInstance().setScreen(new MenuStyleScreen(this.parent, this.originalStyle));
        }).dimensions(cx, y, buttonWidth, buttonHeight).build());
        y += buttonHeight + spacing;

        if ("alien".equals(currentStyle)) {
            String currentBg = ConfigManager.menuBackgroundPath;
            String shortName = currentBg;
            int lastSep = Math.max(currentBg.lastIndexOf('/'), currentBg.lastIndexOf('\\'));
            if (lastSep >= 0) {
                shortName = currentBg.substring(lastSep + 1);
            }
            String bgLabel = "背景图片: " + shortName;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(bgLabel), (btn) -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("选择背景图片");
                chooser.setFileFilter(new FileNameExtensionFilter("图片文件 (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));
                int result = chooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    try {
                        File src = chooser.getSelectedFile();
                        File destDir = new File(MinecraftClient.getInstance().runDirectory, "lumi");
                        if (!destDir.exists()) {
                            destDir.mkdirs();
                        }
                        File dest = new File(destDir, "menu_bg_" + System.currentTimeMillis() + getExtension(src.getName()));
                        Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        ConfigManager.menuBackgroundPath = dest.getAbsolutePath();
                    } catch (Exception ignored) {
                    }
                }
                MinecraftClient.getInstance().setScreen(new MenuStyleScreen(this.parent, this.originalStyle));
            }).dimensions(cx, y, buttonWidth, buttonHeight).build());
            y += buttonHeight + spacing;

            String resetBgLabel = "重置背景图片";
            this.addDrawableChild(ButtonWidget.builder(Text.literal(resetBgLabel), (btn) -> {
                ConfigManager.menuBackgroundPath = "ba.png";
                MinecraftClient.getInstance().setScreen(new MenuStyleScreen(this.parent, this.originalStyle));
            }).dimensions(cx, y, buttonWidth, buttonHeight).build());
            y += buttonHeight + spacing;
        }

        String shakeLabel = (ConfigManager.menuMouseShake ? "[+] " : "[-] ") + "鼠标摇晃";
        this.addDrawableChild(ButtonWidget.builder(Text.literal(shakeLabel), (btn) -> {
            ConfigManager.menuMouseShake = !ConfigManager.menuMouseShake;
            MinecraftClient.getInstance().setScreen(new MenuStyleScreen(this.parent, this.originalStyle));
        }).dimensions(cx, y, buttonWidth, buttonHeight).build());
        y += buttonHeight + spacing;

        double shakeFrac = (ConfigManager.menuMouseShakeStrength - 0.5) / 4.5;
        this.addDrawableChild(new LumiSlider(cx, y, buttonWidth, buttonHeight,
                "摇晃幅度: ", shakeFrac, 0.5, 5.0,
                (value) -> ConfigManager.menuMouseShakeStrength = (float) Math.round(value * 10.0) / 10.0f));
        y += buttonHeight + spacing;

        String revLabel = (ConfigManager.menuMouseShakeReverse ? "[+] " : "[-] ") + "反转方向";
        this.addDrawableChild(ButtonWidget.builder(Text.literal(revLabel), (btn) -> {
            ConfigManager.menuMouseShakeReverse = !ConfigManager.menuMouseShakeReverse;
            MinecraftClient.getInstance().setScreen(new MenuStyleScreen(this.parent, this.originalStyle));
        }).dimensions(cx, y, buttonWidth, buttonHeight).build());
        y += buttonHeight + spacing + 10;

        String blurLabel = (ConfigManager.blurScreenBackground ? "[+] " : "[-] ") + "界面模糊";
        this.addDrawableChild(ButtonWidget.builder(Text.literal(blurLabel), (btn) -> {
            ConfigManager.blurScreenBackground = !ConfigManager.blurScreenBackground;
            MinecraftClient.getInstance().setScreen(new MenuStyleScreen(this.parent, this.originalStyle));
        }).dimensions(cx, y, buttonWidth, buttonHeight).build());
        y += buttonHeight + spacing;

        double blurFrac = (ConfigManager.blurScreenRadius - 2.0) / 18.0;
        this.addDrawableChild(new LumiSlider(cx, y, buttonWidth, buttonHeight,
                "模糊强度: ", blurFrac, 2.0, 20.0,
                (value) -> ConfigManager.blurScreenRadius = (float) Math.round(value * 10.0) / 10.0f));
        y += buttonHeight + spacing + 10;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("返回"), (btn) -> {
            saveAndClose();
        }).dimensions((w - 100) / 2, y, 100, buttonHeight).build());
    }

    private void saveAndClose() {
        dev.G255.Lumi.Lumi.save();
        if (this.client != null) {
            if (nothingChanged()) {
                this.client.setScreen(this.parent);
            } else {
                this.client.setScreen(new TitleScreen());
            }
        }
    }

    private boolean nothingChanged() {
        return ConfigManager.menuStyle.equals(this.originalStyle) &&
            ConfigManager.menuMouseShake == this.originalMouseShake &&
            ConfigManager.menuMouseShakeStrength == this.originalShakeStrength &&
            ConfigManager.menuMouseShakeReverse == this.originalShakeReverse &&
            ConfigManager.menuBackgroundPath.equals(this.originalBgPath) &&
            ConfigManager.blurScreenBackground == this.originalBlurBg &&
            ConfigManager.blurScreenRadius == this.originalBlurRadius;
    }

    @Override
    public void close() {
        saveAndClose();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "LumiClient 设置", this.width / 2, 20, 0xFFFFFF);
    }

    public static String getCurrentStyle() {
        return ConfigManager.menuStyle;
    }

    public static void setMenuStyle(String style) {
        ConfigManager.menuStyle = style;
        dev.G255.Lumi.Lumi.save();
    }

    private static String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : ".png";
    }

    private static class LumiSlider extends SliderWidget {
        private final String prefix;
        private final double min;
        private final double max;
        private final java.util.function.Consumer<Double> onChanged;

        public LumiSlider(int x, int y, int width, int height, String prefix,
                          double fraction, double min, double max,
                          java.util.function.Consumer<Double> onChanged) {
            super(x, y, width, height, Text.literal(""), fraction);
            this.prefix = prefix;
            this.min = min;
            this.max = max;
            this.onChanged = onChanged;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double actual = min + this.value * (max - min);
            this.setMessage(Text.literal(prefix + String.format("%.1f", actual)));
        }

        @Override
        protected void applyValue() {
            double actual = min + this.value * (max - min);
            this.onChanged.accept(actual);
        }
    }
}