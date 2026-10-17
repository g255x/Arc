package dev.perish.mod.modules.impl.render;

import dev.perish.api.utils.math.Easing;
import dev.perish.api.utils.math.FadeUtils;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.option.Perspective;

public class CameraClip extends Module {
    public static CameraClip INSTANCE;
    public final SliderSetting distance = this.add(new SliderSetting("Distance", 4.0, 1.0, 20.0));
    public final SliderSetting animateTime = this.add(new SliderSetting("AnimationTime", 200, 0, 1000));
    final FadeUtils animation = new FadeUtils(300L);
    private final EnumSetting<Easing> ease = this.add(new EnumSetting("Ease", Easing.CubicInOut));
    private final BooleanSetting noFront = this.add(new BooleanSetting("NoFront", false));
    boolean first = false;

    public CameraClip() {
        super("CameraClip", Module.Category.Render);
        this.setChinese("摄像机穿墙");
        INSTANCE = this;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT && this.noFront.getValue()) {
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        }

        this.animation.setLength(this.animateTime.getValueInt());
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            if (!this.first) {
                this.first = true;
                this.animation.reset();
            }
        } else if (this.first) {
            this.first = false;
            this.animation.reset();
        }
    }

    public double getDistance() {
        double quad = mc.options.getPerspective() == Perspective.FIRST_PERSON
                ? 1.0 - this.animation.ease(this.ease.getValue())
                : this.animation.ease(this.ease.getValue());
        return this.distance.getValue() * quad - 1.0 + 1.0;
    }
}
