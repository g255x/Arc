package dev.perish.mod.modules.impl.client;

import dev.perish.api.utils.math.Animation;
import dev.perish.api.utils.math.Easing;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.*;

import java.awt.*;

public class ClientSetting extends Module {
    public static final Animation animation = new Animation();
    public static ClientSetting INSTANCE;
    public final EnumSetting<ClientSetting.Page> page = this.add(new EnumSetting("Page", ClientSetting.Page.Game));
    public final BooleanSetting lowVersion = this.add(new BooleanSetting("1.12", false, () -> this.page.is(ClientSetting.Page.Game)));
    public final BooleanSetting crawl = this.add(new BooleanSetting("Crawl", true, () -> this.page.is(ClientSetting.Page.Game)));
    public final BooleanSetting rotations = this.add(new BooleanSetting("ShowRotations", true, () -> this.page.is(ClientSetting.Page.Game)).setParent());
    public final BooleanSetting sync = this.add(new BooleanSetting("Sync", false, () -> this.page.is(ClientSetting.Page.Game) && this.rotations.isOpen()));
    public final BooleanSetting titleFix = this.add(new BooleanSetting("TitleFix", true, () -> this.page.is(ClientSetting.Page.Game)));
    public final BooleanSetting fuckFPSLimit = this.add(new BooleanSetting("FuckFPSLimit", true, () -> this.page.is(ClientSetting.Page.Game)));
    public final BooleanSetting optimizedCalc = this.add(new BooleanSetting("OptimizedCalc", false, () -> this.page.is(ClientSetting.Page.Game)));
    public final EnumSetting<DamageMode> damageMode = this.add(new EnumSetting<>("Damage", DamageMode.Normal, () -> this.page.is(ClientSetting.Page.Game)));
    public final EnumSetting<PredictMode> predictMode = this.add(new EnumSetting<>("PredictMode", PredictMode.Normal, () -> this.page.is(ClientSetting.Page.Game)));
    public final StringSetting prefix = this.add(new StringSetting("Prefix", ";", () -> this.page.is(ClientSetting.Page.Misc)));
    public final BooleanSetting chinese = this.add(new BooleanSetting("Chinese", false, () -> this.page.is(ClientSetting.Page.Misc)));
    public final BooleanSetting debug = this.add(new BooleanSetting("DebugException", true, () -> this.page.is(ClientSetting.Page.Misc)));
    public final BooleanSetting caughtException = this.add(new BooleanSetting("CaughtException", false, () -> this.page.is(ClientSetting.Page.Misc)).setParent());
    public final BooleanSetting log = this.add(new BooleanSetting("Log", true, () -> this.page.is(ClientSetting.Page.Misc) && this.caughtException.isOpen()));
    public final SliderSetting hotbarTime = this.add(new SliderSetting("HotbarTime", 100, 0, 1000, () -> this.page.is(ClientSetting.Page.Gui)));
    public final EnumSetting<Easing> animEase = this.add(new EnumSetting("AnimEase", Easing.CubicInOut, () -> this.page.is(ClientSetting.Page.Gui)));
    public final BooleanSetting darkening = this.add(new BooleanSetting("Darkening", true, () -> this.page.is(ClientSetting.Page.Gui)));
    public final EnumSetting<GuiMode> guiMode = this.add(new EnumSetting("GuiMode", GuiMode.Old, () -> this.page.is(ClientSetting.Page.Gui)));
    public final StringSetting hackName = this.add(
            new StringSetting("Notification", "[Perish-Hyperion]", () -> this.page.getValue() == ClientSetting.Page.Notification)
    );
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(-6710785), () -> this.page.getValue() == ClientSetting.Page.Notification));
    public final EnumSetting<ClientSetting.Style> messageStyle = this.add(
            new EnumSetting("Style", ClientSetting.Style.Mio, () -> this.page.getValue() == ClientSetting.Page.Notification)
    );
    public final BooleanSetting toggle = this.add(
            new BooleanSetting("ModuleToggle", true, () -> this.page.getValue() == ClientSetting.Page.Notification).setParent()
    );
    public final BooleanSetting onlyOne = this.add(
            new BooleanSetting("OnlyOne", false, () -> this.page.getValue() == ClientSetting.Page.Notification && this.toggle.isOpen())
    );
    public final BooleanSetting banner = this.add(
            new BooleanSetting("ToggleBanner", true, () -> this.page.getValue() == ClientSetting.Page.Notification).setParent()
    );
    public final BooleanSetting keepHistory = this.add(new BooleanSetting("KeepHistory", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
    public final BooleanSetting infiniteChat = this.add(new BooleanSetting("InfiniteChat", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
    public final BooleanSetting hideIndicator = this.add(new BooleanSetting("HideIndicator", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
    public final SliderSetting animationTime = this.add(
            new SliderSetting("AnimationTime", 300, 0, 1000, () -> this.page.getValue() == ClientSetting.Page.ChatHud)
    );
    public final EnumSetting<Easing> ease = this.add(new EnumSetting("Ease", Easing.CubicInOut, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
    public final SliderSetting animateOffset = this.add(new SliderSetting("Offset", -40, -200, 100, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
    public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
    public final BooleanSetting customChatBackground = this.add(new BooleanSetting("CustomBackground", false, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
    public final SliderSetting chatBackgroundRound = this.add(new SliderSetting("Round", 3.0, 0.0, 20.0, 1.0, () -> this.page.getValue() == ClientSetting.Page.ChatHud && this.customChatBackground.getValue()));
    public final SliderSetting chatBackgroundAlpha = this.add(new SliderSetting("Alpha", 70.0, 0.0, 100.0, 1.0, () -> this.page.getValue() == ClientSetting.Page.ChatHud && this.customChatBackground.getValue()));
    public final SliderSetting chatAnimationSpeed = this.add(new SliderSetting("AnimSpeed", 0.2, 0.01, 1.0, 0.01, () -> this.page.getValue() == ClientSetting.Page.ChatHud && this.customChatBackground.getValue()));
    private final BooleanSetting portalGui = this.add(new BooleanSetting("BlockTickNausea", true, () -> this.page.is(ClientSetting.Page.Game)));
    private final BooleanSetting hotbar = this.add(new BooleanSetting("HotbarAnim", true, () -> this.page.is(ClientSetting.Page.Gui)));

    public ClientSetting() {
        super("ClientSetting", Module.Category.Client);
        this.setChinese("客户端设置");
        INSTANCE = this;
    }

    public boolean portalGui() {
        return this.isOn() && this.portalGui.getValue();
    }

    public boolean hotbar() {
        return this.isOn() && this.hotbar.getValue();
    }

    @Override
    public void enable() {
        this.state = true;
    }

    @Override
    public void disable() {
        this.state = true;
    }

    @Override
    public boolean isOn() {
        return true;
    }

    public enum DamageMode {
        Normal,
        Mio
    }

    public enum PredictMode {
        Normal,
        Mio
    }

    public enum BannerStyle {
        iOS,
        Classic
    }

    public enum GuiMode {
        Old,
        Hyperion
    }

    public enum Page {
        Game,
        Gui,
        Misc,
        Notification,
        ChatHud
    }

    public enum StackDir {
        Down,
        Up
    }

    public enum Style {
        Mio,
        Debug,
        Lowercase,
        Normal,
        Future,
        Earth,
        Moon,
        Melon,
        Chinese,
        None
    }
}
