package dev.baimaodaxia.mod.modules.impl.render;

import dev.baimaodaxia.api.utils.math.Easing;
import dev.baimaodaxia.api.utils.math.FadeUtils;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.option.Perspective;

public class CameraClip extends Module {
   public static CameraClip INSTANCE;
   public final SliderSetting getDistance = this.add(new SliderSetting("Distance", (double)4.0F, (double)1.0F, (double)20.0F));
   public final SliderSetting animateTime = this.add(new SliderSetting("AnimationTime", 200, 0, 1000));
   private final EnumSetting<Easing> ease;
   final FadeUtils animation;
   private final BooleanSetting noFront;
   boolean first;

   public CameraClip() {
      super("CameraClip", Module.Category.Render);
      this.ease = this.add(new EnumSetting("Ease", Easing.CubicInOut));
      this.animation = new FadeUtils(300L);
      this.noFront = this.add(new BooleanSetting("NoFront", false));
      this.first = false;
      this.setChinese("摄像机穿墙");
      INSTANCE = this;
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT && this.noFront.getValue()) {
         mc.options.setPerspective(Perspective.FIRST_PERSON);
      }

      this.animation.setLength((long)this.animateTime.getValueInt());
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
      double quad = mc.options.getPerspective() == Perspective.FIRST_PERSON ? (double)1.0F - this.animation.ease(this.ease.getValue()) : this.animation.ease(this.ease.getValue());
      return this.getDistance.getValue() * quad - (double)1.0F + (double)1.0F;
   }
}
