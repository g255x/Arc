package dev.G255.Lumi.mod.modules.impl.render;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.render.skybox.CustomSkyRenderer;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.ColorSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import org.joml.Matrix4f;

public class Skybox extends Module {

    public static Skybox INSTANCE;

    public enum Mode {
        Shader,
        StarsLight
    }

    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Shader));

    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255), () -> this.mode.getValue() == Mode.Shader));
    public final ColorSetting backgroundColor = this.add(new ColorSetting("BackgroundColor", new Color(0, 0, 0, 255), () -> this.mode.getValue() == Mode.Shader));
    public final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.1, 5.0, 0.05, () -> this.mode.getValue() == Mode.Shader));
    public final SliderSetting speed = this.add(new SliderSetting("Speed", 4.0, 0.0, 20.0, 0.1, () -> this.mode.getValue() == Mode.Shader));
    public final SliderSetting opacity = this.add(new SliderSetting("Opacity", 1.0, 0.0, 1.0, 0.01, () -> this.mode.getValue() == Mode.Shader));

    public final ColorSetting horizonColor = this.add(new ColorSetting("Horizon", new Color(196, 79, 186), () -> this.mode.getValue() == Mode.StarsLight));
    public final ColorSetting nebula1Color = this.add(new ColorSetting("Nebula1", new Color(196, 79, 186), () -> this.mode.getValue() == Mode.StarsLight));
    public final ColorSetting nebula2Color = this.add(new ColorSetting("Nebula2", new Color(196, 79, 186), () -> this.mode.getValue() == Mode.StarsLight));
    public final ColorSetting uvStars1Color = this.add(new ColorSetting("UVStars1", new Color(196, 79, 186), () -> this.mode.getValue() == Mode.StarsLight));
    public final ColorSetting uvStars2Color = this.add(new ColorSetting("UVStars2", new Color(255, 255, 255), () -> this.mode.getValue() == Mode.StarsLight));
    public final ColorSetting stars1Color = this.add(new ColorSetting("Stars1", new Color(255, 255, 255), () -> this.mode.getValue() == Mode.StarsLight));
    public final ColorSetting stars2Color = this.add(new ColorSetting("Stars2", new Color(242, 163, 237), () -> this.mode.getValue() == Mode.StarsLight));
    public final BooleanSetting stars = this.add(new BooleanSetting("Stars", true, () -> this.mode.getValue() == Mode.StarsLight));
    public final SliderSetting starSize = this.add(new SliderSetting("StarSize", 1.0, 0.1, 3.0, 0.05, () -> this.mode.getValue() == Mode.StarsLight));

    private static final CustomSkyRenderer skyRenderer = new CustomSkyRenderer();

    public Skybox() {
        super("Skybox", Module.Category.Render);
        this.setChinese("");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return this.mode.getValue().name();
    }

    public void renderSky(Matrix4f positionMatrix, Matrix4f projectionMatrix, float tickDelta) {
        if (mc.world == null) {
            return;
        }

        if (this.mode.getValue() == Mode.Shader) {
            Lumi.SHADER.renderSkyboxFullscreen(
                this.color.getValue(),
                this.backgroundColor.getValue(),
                this.scale.getValueFloat(),
                this.speed.getValueFloat(),
                this.opacity.getValueFloat()
            );
        } else if (this.mode.getValue() == Mode.StarsLight) {
            skyRenderer.render(tickDelta, positionMatrix, projectionMatrix);
        }
    }
}