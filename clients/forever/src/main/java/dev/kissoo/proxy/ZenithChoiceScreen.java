package dev.kissoo.proxy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/**
 * 连接到代理目标服务器时显示的选择界面。
 * 用户选择"排队代理"后才启动 ZenithProxy；选择"直接连接"则完全绕过代理。
 */
public class ZenithChoiceScreen extends Screen {

    private final Screen parent;
    private final ServerAddress address;
    private final ServerInfo info;

    public ZenithChoiceScreen(Screen parent, ServerAddress address, ServerInfo info) {
        super(Text.literal("连接方式"));
        this.parent  = parent;
        this.address = address;
        this.info    = info;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        // "排队代理" — opens ZenithStatusScreen which starts the proxy
        this.addDrawableChild(ButtonWidget.builder(Text.literal("排队代理 (ZenithProxy)"), b -> {
            MinecraftClient.getInstance().setScreen(new ZenithStatusScreen(parent, info));
        }).dimensions(cx - 155, cy + 10, 150, 20).build());

        // "直接连接" — flags zenithGate to let this one through, zero proxy involvement
        this.addDrawableChild(ButtonWidget.builder(Text.literal("直接连接"), b -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            ProxyManager.requestDirectConnect();
            ConnectScreen.connect(parent, mc, address, info, false, null);
        }).dimensions(cx + 5, cy + 10, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("取消"), b -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(cx - 75, cy + 36, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int cy = this.height / 2;

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§e" + ProxyManager.getTargetHost()), cx, cy - 40, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7请选择连接方式"), cx, cy - 20, 0xFFAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
