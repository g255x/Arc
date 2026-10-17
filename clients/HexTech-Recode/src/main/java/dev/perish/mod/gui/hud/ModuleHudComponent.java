package dev.perish.mod.gui.hud;

import dev.perish.mod.modules.impl.hud.*;

public class ModuleHudComponent extends HudComponent {
    private final String moduleId;

    public ModuleHudComponent(String moduleId, String name, float defaultX, float defaultY) {
        super(moduleId, name, defaultX, defaultY);
        this.moduleId = moduleId;
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, float tickDelta) {
        pullFromModule();
    }

    @Override
    public void setX(float x) {
        super.setX(x);
        pushToModule();
    }

    @Override
    public void setY(float y) {
        super.setY(y);
        pushToModule();
    }

    @Override
    public boolean isHovering(double mouseX, double mouseY) {
        pullFromModule();
        return super.isHovering(mouseX, mouseY);
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        pullFromModule();
        return super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isVisible() {
        if (!HudEditorManager.INSTANCE.isEditMode()) return false;
        return switch (moduleId) {
            case "arraylist" -> ArrayListDisplay.INSTANCE != null && ArrayListDisplay.INSTANCE.isOn();
            case "armor" -> ArmorDisplay.INSTANCE != null && ArmorDisplay.INSTANCE.isOn();
            case "watermark" -> WaterMarkDisplay.INSTANCE != null && WaterMarkDisplay.INSTANCE.isOn();
            case "infolist" -> InfoDisplay.INSTANCE != null && InfoDisplay.INSTANCE.isOn();
            case "coords" -> CoordsDisplay.INSTANCE != null && CoordsDisplay.INSTANCE.isOn();
            case "potions" -> PotionsDisplay.INSTANCE != null && PotionsDisplay.INSTANCE.isOn();
            case "textradar" -> TextRadar.INSTANCE != null && TextRadar.INSTANCE.isOn();
            case "itemscounter" -> ItemsCounter.INSTANCE != null && ItemsCounter.INSTANCE.isOn();
            case "targethud" -> TargetHudDisplay.INSTANCE != null && TargetHudDisplay.INSTANCE.isOn();
            case "keybinds" -> KeyBindsDisplay.INSTANCE != null && KeyBindsDisplay.INSTANCE.isOn();
            case "crosshair" -> CrosshairDisplay.INSTANCE != null && CrosshairDisplay.INSTANCE.isOn();
            case "lyric" -> LyricDisplay.INSTANCE != null && LyricDisplay.INSTANCE.isOn();
            default -> false;
        };
    }

    private void pullFromModule() {
        switch (moduleId) {
            case "arraylist" -> sync(ArrayListDisplay.INSTANCE);
            case "armor" -> sync(ArmorDisplay.INSTANCE);
            case "watermark" -> sync(WaterMarkDisplay.INSTANCE);
            case "infolist" -> sync(InfoDisplay.INSTANCE);
            case "coords" -> sync(CoordsDisplay.INSTANCE);
            case "potions" -> sync(PotionsDisplay.INSTANCE);
            case "textradar" -> sync(TextRadar.INSTANCE);
            case "itemscounter" -> sync(ItemsCounter.INSTANCE);
            case "targethud" -> sync(TargetHudDisplay.INSTANCE);
            case "keybinds" -> sync(KeyBindsDisplay.INSTANCE);
            case "crosshair" -> sync(CrosshairDisplay.INSTANCE);
            case "lyric" -> sync(LyricDisplay.INSTANCE);
        }
    }

    private void pushToModule() {
        switch (moduleId) {
            case "arraylist" -> push(ArrayListDisplay.INSTANCE);
            case "armor" -> push(ArmorDisplay.INSTANCE);
            case "watermark" -> push(WaterMarkDisplay.INSTANCE);
            case "infolist" -> push(InfoDisplay.INSTANCE);
            case "coords" -> push(CoordsDisplay.INSTANCE);
            case "potions" -> push(PotionsDisplay.INSTANCE);
            case "textradar" -> push(TextRadar.INSTANCE);
            case "itemscounter" -> push(ItemsCounter.INSTANCE);
            case "targethud" -> push(TargetHudDisplay.INSTANCE);
            case "keybinds" -> push(KeyBindsDisplay.INSTANCE);
            case "crosshair" -> push(CrosshairDisplay.INSTANCE);
            case "lyric" -> push(LyricDisplay.INSTANCE);
        }
    }

    private void sync(Object module) {
        if (module instanceof IHudPosition pos) {
            this.x = pos.getHudX();
            this.y = pos.getHudY();
            this.width = Math.max(pos.getHudW(), 1);
            this.height = Math.max(pos.getHudH(), 1);
        }
    }

    private void push(Object module) {
        if (module instanceof IHudPosition pos) {
            pos.setHudPos(this.x, this.y);
        }
    }
}
