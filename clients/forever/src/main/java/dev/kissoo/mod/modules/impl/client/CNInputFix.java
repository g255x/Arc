package dev.kissoo.mod.modules.impl.client;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.Render2DEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import hoprc.api.utils.win.ImeUtils;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.awt.Color;

public class CNInputFix extends Module {

    public static CNInputFix INSTANCE;

    public final SliderSetting  posX       = this.add(new SliderSetting ("PosX",      20.0, 0.0, 1000.0, 1.0));
    public final SliderSetting  posY       = this.add(new SliderSetting ("PosY",      20.0, 0.0, 1000.0, 1.0));
    public final BooleanSetting horizontal = this.add(new BooleanSetting("Horizontal", true));
    public final ColorSetting   bgColor    = this.add(new ColorSetting  ("BgColor",   new Color(30,  30,  40,  200)));
    public final ColorSetting   compColor  = this.add(new ColorSetting  ("CompColor", new Color(80,  200, 120)));
    public final ColorSetting   textColor  = this.add(new ColorSetting  ("TextColor", new Color(255, 255, 255)));
    public final SliderSetting  bgRadius   = this.add(new SliderSetting ("BgRadius",  4.0,  0.0, 10.0,   1.0));
    private boolean wasTypingState = true;

    public CNInputFix() {
        super("CNInputFix", Module.Category.Client);
        this.setChinese("中文输入法");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ImeUtils.startImeMonitor();
        wasTypingState = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ImeUtils.stopImeMonitor();
        ImeUtils.setImeAllowed(true);
    }

    private boolean shouldEnableIme() {
        if (mc == null || mc.currentScreen == null) {
            return false;
        }

        Screen s = mc.currentScreen;
        if (s.getFocused() instanceof TextFieldWidget) {
            return true;
        }
        if (s instanceof ChatScreen) return true;
        if (s instanceof AbstractSignEditScreen) return true;
        if (s instanceof AnvilScreen) return true;
        if (s instanceof BookEditScreen) return true;
        if (s instanceof AbstractCommandBlockScreen) return true;
        if (s instanceof JigsawBlockScreen) return true;
        if (s instanceof StructureBlockScreen) return true;
        return false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.isOn() || mc == null) return;
        boolean isTypingNow = shouldEnableIme();
        if (isTypingNow != wasTypingState) {
            wasTypingState = isTypingNow;
            ImeUtils.setImeAllowed(isTypingNow);
        }
    }

    @EventListener
    public void onRender2D(Render2DEvent event) {
        try {
            if (!this.isOn() || mc.currentScreen == null) return;
            if (!shouldEnableIme()) return;

            ImeUtils.ImeData ime = ImeUtils.getImeData();
            if (ime.composition == null || ime.composition.isEmpty()) return;

            DrawContext  ctx     = event.drawContext;
            TextRenderer tr      = mc.textRenderer;
            float        padding = 4f;
            float        radius  = this.bgRadius.getValueFloat();
            float x = this.posX.getValueFloat();
            float y = this.posY.getValueFloat();
            if (x == 20f && y == 20f) {
                x = mc.getWindow().getScaledWidth()  / 2f - 50;
                y = mc.getWindow().getScaledHeight() - 50f;
            }

            if (this.horizontal.getValue()) {
                float totalW = tr.getWidth(ime.composition + " ");
                for (String c : ime.candidates) {
                    totalW += tr.getWidth(c + "  ");
                }

                float boxW = totalW + padding * 2;
                float boxH = tr.fontHeight * 2 + padding * 3;

                Render2DUtil.drawRoundedRect(
                        ctx.getMatrices(), x, y, boxW, boxH,
                        radius, this.bgColor.getValue());
                ctx.drawTextWithShadow(tr, ime.composition,
                        (int)(x + padding), (int)(y + padding),
                        this.compColor.getValue().getRGB());
                float curX  = x + padding;
                float candY = y + padding * 2 + tr.fontHeight;
                for (String cand : ime.candidates) {
                    ctx.drawTextWithShadow(tr, cand,
                            (int)curX, (int)candY,
                            this.textColor.getValue().getRGB());
                    curX += tr.getWidth(cand + "  ");
                }

            } else {
                float maxW = tr.getWidth(ime.composition);
                for (String c : ime.candidates) {
                    float w = tr.getWidth(c);
                    if (w > maxW) maxW = w;
                }

                float boxW = maxW + padding * 2;
                float boxH = tr.fontHeight
                        + ime.candidates.size() * (tr.fontHeight + 2)
                        + padding * 2;

                Render2DUtil.drawRoundedRect(
                        ctx.getMatrices(), x, y, boxW, boxH,
                        radius, this.bgColor.getValue());

                ctx.drawTextWithShadow(tr, ime.composition,
                        (int)(x + padding), (int)(y + padding),
                        this.compColor.getValue().getRGB());

                float curY = y + padding + tr.fontHeight + 2;
                for (String cand : ime.candidates) {
                    ctx.drawTextWithShadow(tr, cand,
                            (int)(x + padding), (int)curY,
                            this.textColor.getValue().getRGB());
                    curY += tr.fontHeight + 2;
                }
            }

        } catch (Throwable t) {
        }
    }
}