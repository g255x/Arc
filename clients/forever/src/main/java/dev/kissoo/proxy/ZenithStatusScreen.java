package dev.kissoo.proxy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * Shown after the user picks "排队代理" in {@link ZenithChoiceScreen}.
 * Kicks off install/launch, shows login progress, and auto-joins once ready.
 */
public class ZenithStatusScreen extends Screen {

    private final Screen parent;
    private final ServerInfo info;

    private boolean joining      = false;
    private boolean switching    = false;
    private ButtonWidget openLoginBtn;
    private ButtonWidget copyCodeBtn;

    public ZenithStatusScreen(Screen parent, ServerInfo info) {
        super(Text.literal("排队代理"));
        this.parent = parent;
        this.info   = info;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Start the proxy process as soon as the user lands on this screen.
        ProxyManager.ensureStartedAsync();

        openLoginBtn = ButtonWidget.builder(Text.literal("打开登录页"), b -> {
            String url = ProxyManager.getLoginUrl();
            if (!url.isEmpty()) {
                try { Util.getOperatingSystem().open(new java.net.URI(url)); }
                catch (Exception ignored) {}
            }
        }).dimensions(cx - 155, cy + 30, 150, 20).build();

        copyCodeBtn = ButtonWidget.builder(Text.literal("复制代码"), b -> {
            String code = ProxyManager.getLoginCode();
            if (!code.isEmpty()) this.client.keyboard.setClipboard(code);
        }).dimensions(cx + 5, cy + 30, 150, 20).build();

        this.addDrawableChild(openLoginBtn);
        this.addDrawableChild(copyCodeBtn);

        // 切换账号：停止代理 + 清除 MSA 缓存 + 重新启动（触发重新登录）
        this.addDrawableChild(ButtonWidget.builder(Text.literal("切换代理账号"), b -> {
            if (!switching) {
                switching = true;
                joining   = false;
                ProxyManager.switchAccount();
                // 稍等 500ms 让进程树彻底退出后再重新启动
                Thread t = new Thread(() -> {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    ProxyManager.ensureStartedAsync();
                }, "Zenith-AccountSwitch");
                t.setDaemon(true);
                t.start();
                // 重置界面状态
                switching = false;
            }
        }).dimensions(cx - 155, cy + 56, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("取消"), b -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(cx + 5, cy + 56, 150, 20).build());
    }

    @Override
    public void tick() {
        boolean login = ProxyManager.getState() == ProxyManager.State.LOGIN_REQUIRED;
        openLoginBtn.visible = login;
        copyCodeBtn.visible  = login;

        if (!joining && ProxyManager.isReady()) {
            joining = true;
            MinecraftClient mc = MinecraftClient.getInstance();
            ServerAddress local = ServerAddress.parse("localhost:" + ProxyManager.getListenPort());
            ConnectScreen.connect(parent, mc, local, info, false, null);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int cy = this.height / 2;

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§e排队代理 (ZenithProxy)"), cx, cy - 60, 0xFFFFFF);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(statusLine()), cx, cy - 38, 0xFFAAAAAA);

        if (ProxyManager.getState() == ProxyManager.State.LOGIN_REQUIRED) {
            String url  = ProxyManager.getLoginUrl();
            String code = ProxyManager.getLoginCode();
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§f首次启用：请用浏览器打开下方链接并输入代码登录你的正版账号"),
                    cx, cy - 18, 0xFFFFFF);
            if (!url.isEmpty()) {
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal("§7" + url), cx, cy - 4, 0xFF8888AA);
            }
            if (!code.isEmpty()) {
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal("代码: §a" + code), cx, cy + 10, 0xFFFFFF);
            }
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§7就绪后将自动进入服务器…"), cx, cy - 4, 0xFF888888);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String statusLine() {
        return switch (ProxyManager.getState()) {
            case INSTALLING     -> "§e正在安装代理组件（首次需下载，请耐心等待）…";
            case STARTING       -> "§e正在启动代理…";
            case LOGIN_REQUIRED -> "§b等待登录…";
            case CONNECTING     -> "§e正在连接 " + ProxyManager.getTargetHost() + " …";
            case READY          -> "§a已就绪，正在进入…";
            case FAILED         -> "§c启动失败，请检查 zenithproxy/log 下的日志";
            default             -> "§7准备中…";
        };
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // explicit cancel only
    }
}

