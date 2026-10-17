package dev.kissoo.mod.modules.impl.render.skybox;

import hoprc.obf.zkm.NoFlowObfuscate;
import hoprc.obf.zkm.NoReferencesObfuscate;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;

import org.joml.Matrix4f;

import java.awt.*;

@NoFlowObfuscate
@NoReferencesObfuscate
public class Skybox extends Module {

    public static Skybox INSTANCE;

    public final ColorSetting horizonColor =
            add(new ColorSetting("Horizon", new Color(196, 79, 186)));
    public final ColorSetting nebula1Color =
            add(new ColorSetting("Nebula1", new Color(196, 79, 186)));
    public final ColorSetting nebula2Color =
            add(new ColorSetting("Nebula2", new Color(196, 79, 186)));
    public final ColorSetting uvStars1Color =
            add(new ColorSetting("UVStars1", new Color(196, 79, 186)));
    public final ColorSetting uvStars2Color =
            add(new ColorSetting("UVStars2", new Color(255, 255, 255)));
    public final ColorSetting stars1Color =
            add(new ColorSetting("Stars1", new Color(255, 255, 255)));
    public final ColorSetting stars2Color =
            add(new ColorSetting("Stars2", new Color(242, 163, 237)));

    public final BooleanSetting stars =
            add(new BooleanSetting("Stars", true));

    private static final CustomSkyRenderer skyRenderer = new CustomSkyRenderer();

    public Skybox() {
        super("Skybox", "Custom skybox", Category.Render);
        INSTANCE = this;
    }

    public void renderSky(Matrix4f positionMatrix, Matrix4f projectionMatrix, float tickDelta) {
        if (mc.world == null) {
            return;
        }

        skyRenderer.render(mc.world, tickDelta, positionMatrix, projectionMatrix);
    }
}
