package dev.perish.mod.modules.impl.client;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.Render2DEvent;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.math.Animation;
import dev.perish.api.utils.math.Easing;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.*;
import java.awt.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public class ClickGui extends Module {
    public static ClickGui INSTANCE;
    public final EnumSetting<Pages> pages = this.add(new EnumSetting<>("Page", Pages.General));
    public final BooleanSetting simpleColors = this.add(new BooleanSetting("Simple-Color", true, () -> pages.is(Pages.General) && isOldGui()));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(156, 112, 188, 255), () -> pages.is(Pages.General) && isOldGui()));
    public final BooleanSetting showBind = this.add(new BooleanSetting("Show-Bind", true, () -> pages.is(Pages.General) && isOldGui()));
    public final StringSetting open = this.add(new StringSetting("Open", "+", () -> pages.is(Pages.General) && isOldGui()));
    public final StringSetting close = this.add(new StringSetting("Close", "-", () -> pages.is(Pages.General) && isOldGui()));
    public final BooleanSetting gear = this.add(new BooleanSetting("Gear", true, () -> pages.is(Pages.General) && isOldGui()));
    public final SliderSetting expandSpeed = this.add(new SliderSetting("ExpandSpeed", 1.0, 0.1, 5.0, 0.1, () -> pages.is(Pages.General) && isOldGui()));
    public final ColorSetting modulesColorsetting = this.add(new ColorSetting("ModulesColor", new Color(0, 80, 255, 255), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting moduleHoversetting = this.add(new ColorSetting("ModuleHover", new Color(0, 80, 255, 255), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting settingColorsetting = this.add(new ColorSetting("SettingColor", new Color(224, 224, 224, 255), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting onModulesetting = this.add(new ColorSetting("EnabledText", new Color(238, 215, 255, 255), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting offModulesetting = this.add(new ColorSetting("DisabledText", new Color(245, 245, 245, 255), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting topColorsetting = this.add(new ColorSetting("SectionColorSide", new Color(245, 245, 245, 255), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting bgColor = this.add(new ColorSetting("BgColor", new Color(60398079, true), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting skidspaceHoverColor = this.add(new ColorSetting("SkidHoverColor", new Color(255, 255, 255, 34), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting skidspaceHoverEndColor = this.add(new ColorSetting("SkidHoverEndColor", new Color(112, 156, 195, 28), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting skidspaceEnabledColor = this.add(new ColorSetting("SkidEnabledColor", new Color(181, 111, 188, 132), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting skidspaceEnabledEndColor = this.add(new ColorSetting("SkidEnabledEndColor", new Color(89, 137, 174, 132), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting skidspaceCategoryColor = this.add(new ColorSetting("SkidCategoryColor", new Color(255, 255, 255, 255), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting skidspaceCategoryCountColor = this.add(new ColorSetting("SkidCategoryCountColor", new Color(215, 206, 225, 255), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting skidspaceCategoryBgColor = this.add(new ColorSetting("SkidCategoryBgColor", new Color(0, 0, 0, 0), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting skidspaceCategoryBgEndColor = this.add(new ColorSetting("SkidCategoryBgEndColor", new Color(0, 0, 0, 0), () -> pages.is(Pages.Colors) && isOldGui()));
    public final ColorSetting hyperionAccentColor = this.add(new ColorSetting("H-Accent", new Color(255, 135, 185, 255), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionPanelBg = this.add(new ColorSetting("H-PanelBg", new Color(245, 247, 255, 150), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionSectionBg = this.add(new ColorSetting("H-SectionBg", new Color(238, 241, 250, 140), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionTextPrimary = this.add(new ColorSetting("H-TextPrimary", new Color(50, 55, 75, 255), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionTextSecondary = this.add(new ColorSetting("H-TextSecondary", new Color(115, 120, 145, 255), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionBorderColor = this.add(new ColorSetting("H-Border", new Color(190, 195, 215, 100), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionToggleOffColor = this.add(new ColorSetting("H-ToggleOff", new Color(210, 215, 230, 255), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionToggleKnobColor = this.add(new ColorSetting("H-ToggleKnob", new Color(255, 255, 255, 255), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionSliderBgColor = this.add(new ColorSetting("H-SliderBg", new Color(210, 215, 230, 255), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionSliderTrackColor = this.add(new ColorSetting("H-SliderTrack", new Color(255, 135, 185, 255), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionItemHoverColor = this.add(new ColorSetting("H-ItemHover", new Color(225, 228, 240, 220), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionItemSelectedColor = this.add(new ColorSetting("H-ItemSelected", new Color(255, 135, 185, 35), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionTextDisabledColor = this.add(new ColorSetting("H-TextDisabled", new Color(55, 60, 85, 255), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionCursorColor = this.add(new ColorSetting("H-Cursor", new Color(255, 135, 185, 255), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionKeybindPulseColor = this.add(new ColorSetting("H-KeybindPulse", new Color(255, 135, 185, 255), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final ColorSetting hyperionScrollbarColor = this.add(new ColorSetting("H-Scrollbar", new Color(255, 135, 185, 80), () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final SliderSetting hyperionBackgroundAlpha = this.add(new SliderSetting("H-BgAlpha", 73.0, 0.0, 100.0, 1.0, () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final BooleanSetting hyperionBackgroundBlur = this.add(new BooleanSetting("H-BgBlur", true, () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final SliderSetting hyperionBlurRadius = this.add(new SliderSetting("H-BlurRadius", 32.0, 0.0, 50.0, () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final BooleanSetting hyperionShowTime = this.add(new BooleanSetting("H-ShowTime", true, () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final BooleanSetting hyperionShowVersion = this.add(new BooleanSetting("H-ShowVersion", true, () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final SliderSetting hyperionAnimationSpeed = this.add(new SliderSetting("H-AnimSpeed", 1.0, 0.2, 3.0, () -> pages.is(Pages.Hyperion) && isHyperionGui()));
    public final BooleanSetting autoSave = this.add(new BooleanSetting("AutoSave", true));
    public final BooleanSetting font = this.add(new BooleanSetting("Font", true));
    public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
    public final BooleanSetting snow = this.add(new BooleanSetting("Snow", false, () -> pages.is(Pages.General) && isOldGui()));
    public final BooleanSetting vanillaBackground = this.add(new BooleanSetting("VanillaBg", false, () -> pages.is(Pages.General) && isOldGui()));
    public final BooleanSetting guiSound = this.add(new BooleanSetting("GuiSound", true));
    public final SliderSetting soundPitch = this.add(new SliderSetting("SoundPitch", 1.0, 0.0, 2.0, 0.1, () -> pages.is(Pages.General)));
    public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true, () -> pages.is(Pages.General)));
    public final SliderSetting length = this.add(new SliderSetting("Length", 126, 0, 1000, this.fade::getValue));
    public final EnumSetting<Easing> easing = this.add(new EnumSetting("Easing", Easing.SineInOut, this.fade::getValue));
    public final BooleanSetting blur = this.add(new BooleanSetting("Blur", true, () -> pages.is(Pages.General) && isOldGui()));
    public final SliderSetting radius = this.add(new SliderSetting("Radius", 58.4, 0.0, 100.0, () -> pages.is(Pages.General) && isOldGui() && this.blur.getValue()));
    public final ColorSetting tint = this.add(new ColorSetting("Tint", new Color(-1, true)).injectBoolean(false));
    public final ColorSetting endColor = this.add(new ColorSetting("End", new Color(1342903851, true), () -> this.tint.booleanValue));
    private final Animation animation = new Animation();
    public double alphaValue;
    private boolean lastSimpleColorsState;
    private Color lastPrimaryColor;

    public ClickGui() {
        super("ClickGui", Module.Category.Client);
        this.setChinese("点击界面");
        INSTANCE = this;
        Perish.EVENT_BUS.subscribe(new ClickGui.FadeOut());
    }

    private static boolean isOldGui() {
        return ClientSetting.INSTANCE.guiMode.is(ClientSetting.GuiMode.Old);
    }

    private static boolean isHyperionGui() {
        return ClientSetting.INSTANCE.guiMode.is(ClientSetting.GuiMode.Hyperion);
    }

    public static ClickGui getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            this.disable();
        } else {
            this.updateColor(true);
            if (this.guiSound.getValue() && mc.getSoundManager() != null) {
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
            }

            mc.setScreen(new dev.perish.mod.gui.skidspace.ClickGuiScreen());
        }
    }

    @Override
    public void onDisable() {
        if (mc.currentScreen instanceof dev.perish.mod.gui.skidspace.ClickGuiScreen) {
            mc.currentScreen.close();
        }

        if (this.guiSound.getValue() && mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
        }

        if (this.autoSave.getValue()) {
            Perish.save();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        //this.applyPaletteOnce();
        this.updateColor(false);
        if (!(mc.currentScreen instanceof dev.perish.mod.gui.skidspace.ClickGuiScreen)) {
            this.disable();
        }
    }

    public void updateColor() {
        this.updateColor(false);
    }

    public int getHyperionAnimDuration(int baseMs) {
        return Math.max(50, (int) (baseMs / this.hyperionAnimationSpeed.getValue()));
    }

    public boolean showHyperionTime() {
        return this.hyperionShowTime.getValue();
    }

    public boolean showHyperionVersion() {
        return this.hyperionShowVersion.getValue();
    }

    public Color getHyperionAccentDim() {
        Color c = this.hyperionAccentColor.getValue();
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 70);
    }

    public Color getHyperionItemBg() {
        Color bg = this.hyperionPanelBg.getValue();
        return new Color(
                Math.min(255, bg.getRed() + 8),
                Math.min(255, bg.getGreen() + 8),
                Math.min(255, bg.getBlue() + 12),
                200);
    }

    public Color getHyperionItemBgHover() {
        return this.hyperionItemHoverColor.getValue();
    }

    public Color getHyperionItemBgSelected() {
        return this.hyperionItemSelectedColor.getValue();
    }

    public Color getHyperionTextDisabled() {
        return this.hyperionTextDisabledColor.getValue();
    }

    public Color getHyperionPanelBgWithAlpha() {
        Color bg = this.hyperionPanelBg.getValue();
        int alpha = (int) (this.hyperionBackgroundAlpha.getValue() / 100.0 * 255.0);
        return new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), alpha);
    }

    public Color getHyperionSectionBgWithAlpha() {
        Color bg = this.hyperionSectionBg.getValue();
        int alpha = (int) (this.hyperionBackgroundAlpha.getValue() / 100.0 * 255.0 * 0.94);
        return new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), Math.min(255, alpha));
    }

    public void updateColor(boolean force) {
        Color primary = this.color.getValue();
        boolean simpleColorsEnabled = this.simpleColors.getValue();
        boolean modeChanged = this.lastSimpleColorsState != simpleColorsEnabled;
        boolean primaryChanged = this.lastPrimaryColor == null || this.lastPrimaryColor.getRGB() != primary.getRGB();

        if (simpleColorsEnabled && (force || modeChanged || primaryChanged)) {
            this.modulesColorsetting.setValue(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), this.modulesColorsetting.getValue().getAlpha()));
            this.moduleHoversetting.setValue(new Color(70, 70, 70, this.moduleHoversetting.getValue().getAlpha()));
            this.settingColorsetting.setValue(new Color(220, 220, 220, this.settingColorsetting.getValue().getAlpha()));
            this.onModulesetting.setValue(new Color(238, 215, 255, this.onModulesetting.getValue().getAlpha()));
            this.offModulesetting.setValue(new Color(245, 245, 245, this.offModulesetting.getValue().getAlpha()));
            this.topColorsetting.setValue(new Color(245, 245, 245, this.topColorsetting.getValue().getAlpha()));
            this.skidspaceHoverColor.setValue(new Color(255, 255, 255, 34));
            this.skidspaceHoverEndColor.setValue(new Color(112, 156, 195, 28));
            this.skidspaceEnabledColor.setValue(new Color(181, 111, 188, 132));
            this.skidspaceEnabledEndColor.setValue(new Color(89, 137, 174, 132));
            this.skidspaceCategoryColor.setValue(new Color(255, 255, 255, this.skidspaceCategoryColor.getValue().getAlpha()));
            this.skidspaceCategoryCountColor.setValue(new Color(215, 206, 225, this.skidspaceCategoryCountColor.getValue().getAlpha()));
            this.skidspaceCategoryBgColor.setValue(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 0));
            this.skidspaceCategoryBgEndColor.setValue(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 0));
        }

        this.lastSimpleColorsState = simpleColorsEnabled;
        this.lastPrimaryColor = new Color(primary.getRGB(), true);
    }

    public enum Pages {
        General,
        Colors,
        Hyperion
    }

    public class FadeOut {
        @EventListener(priority = -99999)
        public void onRender2D(Render2DEvent event) {
            if (ClickGui.this.fade.getValue()) {
                if (ClickGui.this.alphaValue > 0.0 || ClickGui.this.isOn()) {
                    ClickGui.this.alphaValue = ClickGui.this.animation
                            .get(ClickGui.this.isOn() ? 1.0 : 0.0, ClickGui.this.length.getValueInt(), ClickGui.this.easing.getValue());
                }

                if (ClickGui.this.alphaValue > 0.0 && !(Wrapper.mc.currentScreen instanceof dev.perish.mod.gui.skidspace.ClickGuiScreen)) {
                    event.drawContext.getMatrices().push();
                    event.drawContext.getMatrices().translate(0.0F, 0.0F, 5000.0F);
                    event.drawContext.getMatrices().pop();
                }
            } else {
                ClickGui.this.alphaValue = 1.0;
            }
        }
    }
}
