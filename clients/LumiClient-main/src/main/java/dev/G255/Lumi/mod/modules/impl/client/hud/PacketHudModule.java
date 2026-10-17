package dev.G255.Lumi.mod.modules.impl.client.hud;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.PacketEvent;
import dev.G255.Lumi.api.utils.render.TextUtil;
import dev.G255.Lumi.core.impl.FontManager;
import dev.G255.Lumi.mod.modules.HudModule;
import dev.G255.Lumi.mod.modules.impl.client.ClickGui;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.ColorSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import dev.G255.Lumi.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Objects;

public class PacketHudModule extends HudModule {
    public static PacketHudModule INSTANCE;

    public enum CountMode {
        Sliding,
        Fixed
    }

    private final EnumSetting<CountMode> countMode = this.add(new EnumSetting<>("CountMode", CountMode.Sliding));
    private final SliderSetting window = this.add(new SliderSetting("Window", 1000.0, 100.0, 60000.0, 100.0));
    private final BooleanSetting customFormat = this.add(new BooleanSetting("CustomFormat", false).setParent());
    private final StringSetting format = this.add(new StringSetting("Format", "\u2193%in% \u2191%out%", this.customFormat::isOpen));
    private final BooleanSetting customColors = this.add(new BooleanSetting("CustomColors", false).setParent());
    private final ColorSetting inColor = this.add(new ColorSetting("InColor", new Color(0, 255, 0), this.customColors::isOpen));
    private final ColorSetting outColor = this.add(new ColorSetting("OutColor", new Color(255, 0, 0), this.customColors::isOpen));

    private final ArrayDeque<Long> inTimestamps = new ArrayDeque<>();
    private final ArrayDeque<Long> outTimestamps = new ArrayDeque<>();

    private long lastFixedUpdate = System.currentTimeMillis();
    private int fixedIn = 0;
    private int fixedOut = 0;
    private int displayIn = 0;
    private int displayOut = 0;

    public PacketHudModule() {
        super("PacketHud", "", 2, 100);
        INSTANCE = this;
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (countMode.getValue() == CountMode.Sliding) {
            outTimestamps.add(System.currentTimeMillis());
        } else {
            fixedOut++;
        }
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (countMode.getValue() == CountMode.Sliding) {
            inTimestamps.add(System.currentTimeMillis());
        } else {
            fixedIn++;
        }
    }

    private int slidingIn() {
        long now = System.currentTimeMillis();
        long threshold = now - (long) window.getValue();
        while (!inTimestamps.isEmpty() && inTimestamps.peek() != null && inTimestamps.peek() < threshold) {
            inTimestamps.poll();
        }
        return inTimestamps.size();
    }

    private int slidingOut() {
        long now = System.currentTimeMillis();
        long threshold = now - (long) window.getValue();
        while (!outTimestamps.isEmpty() && outTimestamps.peek() != null && outTimestamps.peek() < threshold) {
            outTimestamps.poll();
        }
        return outTimestamps.size();
    }

    private int getIn() {
        if (countMode.getValue() == CountMode.Fixed) {
            return displayIn;
        }
        return slidingIn();
    }

    private int getOut() {
        if (countMode.getValue() == CountMode.Fixed) {
            return displayOut;
        }
        return slidingOut();
    }

    public void resetCounters() {
        inTimestamps.clear();
        outTimestamps.clear();
        fixedIn = 0;
        fixedOut = 0;
        displayIn = 0;
        displayOut = 0;
        lastFixedUpdate = System.currentTimeMillis();
    }

    @Override
    public void onEnable() {
        resetCounters();
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (countMode.getValue() == CountMode.Fixed) {
            long now = System.currentTimeMillis();
            if (now - lastFixedUpdate >= (long) window.getValue()) {
                displayIn = fixedIn;
                displayOut = fixedOut;
                fixedIn = 0;
                fixedOut = 0;
                lastFixedUpdate = now;
            }
        }

        int in = getIn();
        int out = getOut();

        String text;
        if (customFormat.getValue()) {
            String fmt = format.getValue();
            text = fmt.replace("%in%", String.valueOf(in)).replace("%out%", String.valueOf(out));
        } else {
            text = "\u2193" + in + " \u2191" + out;
        }

        int w;
        if (HudSetting.useFont()) {
            w = (int)Math.ceil(FontManager.ui.getWidth(text));
        } else {
            Objects.requireNonNull(mc.textRenderer);
            w = mc.textRenderer.getWidth(text);
        }

        int h;
        if (HudSetting.useFont()) {
            h = (int)Math.ceil(FontManager.ui.getFontHeight());
        } else {
            Objects.requireNonNull(mc.textRenderer);
            h = 9;
        }

        int x = this.getHudRenderX(w);
        int y = this.getHudRenderY(h);

        int color = this.getHudColor(0.0);
        if (!customFormat.getValue() || !customColors.getValue()) {
            TextUtil.drawString(context, text, x, y, color, HudSetting.useFont(), HudSetting.useShadow());
        } else {
            drawCustomFormat(context, text, x, y, in, out);
        }

        this.setHudBounds(x, y, Math.max(1, w), Math.max(1, h));
    }

    private void drawCustomFormat(DrawContext context, String text, int x, int y, int in, int out) {
        String fmt = format.getValue();
        String inStr = String.valueOf(in);
        String outStr = String.valueOf(out);
        String inKey = "%in%";
        String outKey = "%out%";

        int inIdx = fmt.indexOf(inKey);
        int outIdx = fmt.indexOf(outKey);

        int guiRgb = getHudColor(0.0);

        if (inIdx < outIdx) {
            int posX = x;
            String beforeIn = fmt.substring(0, inIdx);
            posX = drawPart(context, beforeIn, posX, y, guiRgb);
            posX = drawPart(context, inStr, posX, y, inColor.getValue().getRGB());
            String between = fmt.substring(inIdx + inKey.length(), outIdx);
            posX = drawPart(context, between, posX, y, guiRgb);
            posX = drawPart(context, outStr, posX, y, outColor.getValue().getRGB());
            String after = fmt.substring(outIdx + outKey.length());
            drawPart(context, after, posX, y, guiRgb);
        } else if (outIdx < inIdx) {
            int posX = x;
            String beforeOut = fmt.substring(0, outIdx);
            posX = drawPart(context, beforeOut, posX, y, guiRgb);
            posX = drawPart(context, outStr, posX, y, outColor.getValue().getRGB());
            String between = fmt.substring(outIdx + outKey.length(), inIdx);
            posX = drawPart(context, between, posX, y, guiRgb);
            posX = drawPart(context, inStr, posX, y, inColor.getValue().getRGB());
            String after = fmt.substring(inIdx + inKey.length());
            drawPart(context, after, posX, y, guiRgb);
        } else {
            TextUtil.drawString(context, text, x, y, guiRgb, HudSetting.useFont(), HudSetting.useShadow());
        }
    }

    private int drawPart(DrawContext context, String part, int x, int y, int color) {
        if (part.isEmpty()) return x;
        TextUtil.drawString(context, part, x, y, color, HudSetting.useFont(), HudSetting.useShadow());
        if (HudSetting.useFont()) {
            return x + (int)Math.ceil(FontManager.ui.getWidth(part));
        } else {
            return x + mc.textRenderer.getWidth(part);
        }
    }

    private int getHudColor(double delay) {
        ClickGui gui = ClickGui.getInstance();
        return gui == null ? -1 : gui.getColor(delay).getRGB();
    }
}