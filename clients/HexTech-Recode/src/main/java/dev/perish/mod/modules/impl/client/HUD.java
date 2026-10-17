package dev.perish.mod.modules.impl.client;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClientTickEvent;
import dev.perish.api.events.impl.InitEvent;
import dev.perish.api.utils.math.Animation;
import dev.perish.api.utils.math.Easing;
import dev.perish.api.utils.render.ColorUtil;
import dev.perish.core.impl.FontManager;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.DrawContext;

public class HUD extends Module {
    public static HUD INSTANCE;
    public final EnumSetting<HUD.Page> page = this.add(new EnumSetting("Page", HUD.Page.General));
    public final BooleanSetting renderingUp = this.add(new BooleanSetting("RenderingUp", false, () -> this.page.is(HUD.Page.General)));
    public final BooleanSetting font = this.add(new BooleanSetting("Font", false, () -> this.page.is(HUD.Page.General)));
    public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true, () -> this.page.is(HUD.Page.General)));
    public final BooleanSetting lowerCase = this.add(new BooleanSetting("LowerCase", false, () -> this.page.is(HUD.Page.General)));
//    public final BooleanSetting sort = this.add(new BooleanSetting("Sort", false, () -> this.page.is(HUD.Page.General)));
    public final EnumSetting<Easing> easing = this.add(new EnumSetting("Easing", Easing.CircInOut, () -> this.page.is(HUD.Page.General)));
//    public final SliderSetting enableLength = this.add(new SliderSetting("EnableLength", 200, 0, 1000, () -> this.page.is(HUD.Page.General)));
//    public final SliderSetting disableLength = this.add(new SliderSetting("DisableLength", 200, 0, 1000, () -> this.page.is(HUD.Page.General)));
//    public final SliderSetting fadeLength = this.add(new SliderSetting("FadeLength", 200, 0, 1000, () -> this.page.is(HUD.Page.General)));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(153, 153, 255), () -> this.page.is(HUD.Page.Color)));
    public final BooleanSetting blur = this.add(new BooleanSetting("Blur", false, () -> this.page.is(HUD.Page.Color)).setParent());
    public final SliderSetting radius = this.add(new SliderSetting("Radius", 10.0, 0.0, 100.0, () -> this.page.is(HUD.Page.Color) && this.blur.isOpen()));
    private final EnumSetting<HUD.ColorMode> colorMode = this.add(new EnumSetting("ColorMode", HUD.ColorMode.Pulse, () -> this.page.is(HUD.Page.Color)));
    private final SliderSetting rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", 4.0, 1.0, 10.0, 0.1, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
    private final SliderSetting saturation = this.add(new SliderSetting("Saturation", 130.0, 1.0, 255.0, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
    private final SliderSetting rainbowDelay = this.add(new SliderSetting("Delay", 350, 0, 1000, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow));
    private final ColorSetting endColor = this.add(new ColorSetting("SecondColor", new Color(153, 153, 255, 255), () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Pulse).injectBoolean(true));
    private final SliderSetting pulseSpeed = this.add(new SliderSetting("PulseSpeed", 1.0, 0.0, 5.0, 0.1, () -> this.page.is(HUD.Page.Color)));
    public final SliderSetting pulseCounter = this.add(new SliderSetting("Counter", 10, 1, 50, () -> this.page.is(HUD.Page.Color)));
    public final BooleanSetting backGround = this.add(new BooleanSetting("BackGround", false, () -> this.page.is(HUD.Page.Color)).setParent());
    public final SliderSetting width = this.add(new SliderSetting("Width", 0.0, 0.0, 15.0, () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
//    public final SliderSetting borderRadius = this.add(new SliderSetting("BorderRadius", 4.0, 0.0, 20.0, 0.1, () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
//    public final ColorSetting bgColor = this.add(new ColorSetting("BGColor", new Color(0, 0, 0, 100), () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
    public final ColorSetting rect = this.add(new ColorSetting("Rect", new Color(208, 0, 0), () -> this.page.is(HUD.Page.Color)).injectBoolean(false));
//    public final ColorSetting glow = this.add(new ColorSetting("Glow", new Color(208, 0, 100), () -> this.page.is(HUD.Page.Color)).injectBoolean(false));
    private final ArrayList<HUD.Info> moduleList = new ArrayList();

    public HUD() {
        super("HUD", Module.Category.Client);
        this.setChinese("界面");
        INSTANCE = this;
        Perish.EVENT_BUS.subscribe(new HUD.InitHandler());
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
    }

    @EventListener(priority = -999)
    public void onUpdate(ClientTickEvent event) {
        if (!nullCheck()) {
            if (event.isPost()) {
                if (dev.perish.mod.modules.impl.hud.ArrayListDisplay.INSTANCE.isOn()) {
                    HUD.Info.onUpdate(this.moduleList, true);
                }
            }
        }
    }

    private int getWidth(String s) {
        if (this.lowerCase.getValue()) {
            s = s.toLowerCase();
        }
        return this.font.getValue() ? (int) FontManager.ui.getWidth(s) : mc.textRenderer.getWidth(s);
    }

    private int getHeight() {
        return this.font.getValue() ? (int) FontManager.ui.getFontHeight() : 9;
    }

    public int getColor(double counter) {
        return this.colorMode.getValue() != HUD.ColorMode.Custom ? this.rainbow(counter).getRGB() : this.color.getValue().getRGB();
    }

    private Color rainbow(double delay) {
        if (this.colorMode.getValue() == HUD.ColorMode.Pulse) {
            return this.endColor.booleanValue
                    ? ColorUtil.pulseColor(this.color.getValue(), this.endColor.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue())
                    : ColorUtil.pulseColor(this.color.getValue(), delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
        } else if (this.colorMode.getValue() == HUD.ColorMode.Rainbow) {
            double rainbowState = Math.ceil((System.currentTimeMillis() * this.rainbowSpeed.getValue() + delay * this.rainbowDelay.getValue()) / 20.0);
            return Color.getHSBColor((float) (rainbowState % 360.0 / 360.0), this.saturation.getValueFloat() / 255.0F, 1.0F);
        } else {
            return this.color.getValue();
        }
    }

    private int getFontHeight() {
        return this.font.getValue() ? (int) FontManager.ui.getFontHeight() : 9;
    }

    private enum ColorMode {
        Custom,
        Pulse,
        Rainbow
    }

    public enum Page {
        General,
        Color
    }

    public class Info {
//        static double fontHeight;
//        static double currentY;
//        static int windowWidth;
//        static boolean fromUp;
//        static double counter;
        public final Callable<String> info;
        public final BooleanSupplier drawn;
        public final Animation animation = new Animation();
        public final Animation fadeAnimation = new Animation();
        public String string;
        public double currentX = 0.0;
        public boolean isOn;

        public Info(Callable<String> info, BooleanSupplier drawn) {
            this.info = info;
            this.drawn = drawn;
            try {
                this.string = this.info.call();
            } catch (Exception var5) {
            }
        }

        public static void onUpdate(List<HUD.Info> list, boolean sort) {
            for (HUD.Info s : list) {
                s.onUpdate();
            }
            if (sort) {
                list.sort(Comparator.comparingInt(info -> info.string == null ? 0 : -HUD.INSTANCE.getWidth(info.string)));
            }
        }

        public void onUpdate() {
            this.isOn = this.drawn.getAsBoolean();
            if (this.isOn) {
                try {
                    this.string = HUD.this.lowerCase.getValue() ? this.info.call().toLowerCase() : this.info.call();
                } catch (Exception var2) {
                    var2.printStackTrace();
                }
            }
        }
    }

    public class InitHandler {
        @EventListener
        public void onInit(InitEvent event) {
            for (Module module : Perish.MODULE.getModules()) {
                HUD.this.moduleList.add(HUD.this.new Info(module::getArrayName, () -> {
                    if (dev.perish.mod.modules.impl.hud.ArrayListDisplay.INSTANCE.onlyBind.getValue() && module.getBindSetting().getValue() == -1) return false;
                    return module.isOn() && module.drawn.getValue();
                }));
            }
            Perish.EVENT_BUS.unsubscribe(this);
        }
    }
}
