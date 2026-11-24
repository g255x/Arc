package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import java.awt.Color;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.world.World;

public class SkyColor extends Module {
    public static SkyColor INSTANCE;

    public final EnumSetting<SkyMode> mode = this.add(new EnumSetting<>("Mode", SkyMode.None).injectTask(this::reloadWorld));
    public final BooleanSetting thickFog = this.add(new BooleanSetting("ThickFog", false, this::usesCustomEffects));
    public final ColorSetting fogColor = this.add(new ColorSetting("FogColor", new Color(120, 176, 255)));
    public final ColorSetting skyColor = this.add(new ColorSetting("SkyColor", new Color(120, 176, 255)));
    public final BooleanSetting forceSky = this.add(new BooleanSetting("ForceSky", true).injectTask(this::reloadWorld));
    public final BooleanSetting overworld = this.add(new BooleanSetting("Overworld", true));
    public final BooleanSetting nether = this.add(new BooleanSetting("Nether", true));
    public final BooleanSetting end = this.add(new BooleanSetting("End", true));

    public SkyColor() {
        super("SkyColor", "Mio style sky and fog coloring", Module.Category.Render);
        this.setChinese("\u5929\u7a7a\u989c\u8272");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        this.reloadWorld();
    }

    @Override
    public void onDisable() {
        this.reloadWorld();
    }

    @Override
    public void onToggle() {
        this.reloadWorld();
    }

    public boolean isActive() {
        return this.isOn() && !nullCheck() && this.isEnabledInCurrentDimension();
    }

    public boolean isFlatMode() {
        return this.mode.is(SkyMode.Flat);
    }

    public boolean isEndMode() {
        return this.mode.is(SkyMode.End);
    }

    public boolean usesCustomEffects() {
        return this.isFlatMode() || this.isEndMode();
    }

    // 决定要把当前维度的 SkyType 顶成什么，null 表示不干预。
    // 下界原本是 NONE（压根不画天空穹顶）、末地是 END（画固定的末地贴图），
    // 两者都不会去问 ClientWorld#getSkyColor，所以不顶的话 SkyColor 只在主世界生效。
    public DimensionEffects.SkyType getForcedSkyType() {
        if (!this.isActive()) {
            return null;
        }
        if (this.isEndMode()) {
            return DimensionEffects.SkyType.END;
        }
        return this.forceSky.getValue() ? DimensionEffects.SkyType.NORMAL : null;
    }

    private boolean isEnabledInCurrentDimension() {
        if (nullCheck()) {
            return false;
        }
        if (mc.world.getRegistryKey() == World.OVERWORLD) {
            return this.overworld.getValue();
        }
        if (mc.world.getRegistryKey() == World.NETHER) {
            return this.nether.getValue();
        }
        if (mc.world.getRegistryKey() == World.END) {
            return this.end.getValue();
        }
        return false;
    }

    private void reloadWorld() {
        if (!nullCheck()) {
            mc.worldRenderer.reload();
        }
    }

    public enum SkyMode {
        None,
        Flat,
        End
    }
}
