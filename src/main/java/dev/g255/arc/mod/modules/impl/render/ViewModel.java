package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.HeldItemRendererEvent;
import dev.g255.arc.asm.accessors.IHeldItemRenderer;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class ViewModel extends Module {
   public static ViewModel INSTANCE;
   public final BooleanSetting mainhandSwap = this.add(new BooleanSetting("MainhandSwap", true));
   public final BooleanSetting offhandSwap = this.add(new BooleanSetting("OffhandSwap", true));
   public final SliderSetting scaleMainX = this.add(new SliderSetting("ScaleMainX", (double)1.0F, (double)0.1F, (double)5.0F, 0.01));
   public final SliderSetting scaleMainY = this.add(new SliderSetting("ScaleMainY", (double)1.0F, (double)0.1F, (double)5.0F, 0.01));
   public final SliderSetting scaleMainZ = this.add(new SliderSetting("ScaleMainZ", (double)1.0F, (double)0.1F, (double)5.0F, 0.01));
   public final SliderSetting positionMainX = this.add(new SliderSetting("PositionMainX", (double)0.0F, (double)-3.0F, (double)3.0F, 0.01));
   public final SliderSetting positionMainY = this.add(new SliderSetting("PositionMainY", (double)0.0F, (double)-3.0F, (double)3.0F, 0.01));
   public final SliderSetting positionMainZ = this.add(new SliderSetting("PositionMainZ", (double)0.0F, (double)-3.0F, (double)3.0F, 0.01));
   public final SliderSetting rotationMainX = this.add(new SliderSetting("RotationMainX", (double)0.0F, (double)-180.0F, (double)180.0F, 0.01));
   public final SliderSetting rotationMainY = this.add(new SliderSetting("RotationMainY", (double)0.0F, (double)-180.0F, (double)180.0F, 0.01));
   public final SliderSetting rotationMainZ = this.add(new SliderSetting("RotationMainZ", (double)0.0F, (double)-180.0F, (double)180.0F, 0.01));
   public final SliderSetting scaleOffX = this.add(new SliderSetting("ScaleOffX", (double)1.0F, (double)0.1F, (double)5.0F, 0.01));
   public final SliderSetting scaleOffY = this.add(new SliderSetting("ScaleOffY", (double)1.0F, (double)0.1F, (double)5.0F, 0.01));
   public final SliderSetting scaleOffZ = this.add(new SliderSetting("ScaleOffZ", (double)1.0F, (double)0.1F, (double)5.0F, 0.01));
   public final SliderSetting positionOffX = this.add(new SliderSetting("PositionOffX", (double)0.0F, (double)-3.0F, (double)3.0F, 0.01));
   public final SliderSetting positionOffY = this.add(new SliderSetting("PositionOffY", (double)0.0F, (double)-3.0F, (double)3.0F, 0.01));
   public final SliderSetting positionOffZ = this.add(new SliderSetting("PositionOffZ", (double)0.0F, (double)-3.0F, (double)3.0F, 0.01));
   public final SliderSetting rotationOffX = this.add(new SliderSetting("RotationOffX", (double)0.0F, (double)-180.0F, (double)180.0F, 0.01));
   public final SliderSetting rotationOffY = this.add(new SliderSetting("RotationOffY", (double)0.0F, (double)-180.0F, (double)180.0F, 0.01));
   public final SliderSetting rotationOffZ = this.add(new SliderSetting("RotationOffZ", (double)0.0F, (double)-180.0F, (double)180.0F, 0.01));
   public final BooleanSetting slowAnimation = this.add(new BooleanSetting("SwingSpeed", true));
   public final SliderSetting slowAnimationVal = this.add(new SliderSetting("Value", 6, 1, 50));

   public ViewModel() {
      super("ViewModel", Module.Category.Render);
      this.setChinese("手持模型");
      INSTANCE = this;
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (!this.mainhandSwap.getValue() && ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).getEquippedProgressMainHand() <= 1.0F) {
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setEquippedProgressMainHand(1.0F);
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setItemStackMainHand(mc.player.getMainHandStack());
      }

      if (!this.offhandSwap.getValue() && ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).getEquippedProgressOffHand() <= 1.0F) {
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setEquippedProgressOffHand(1.0F);
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setItemStackOffHand(mc.player.getOffHandStack());
      }

   }

   @EventListener
   private void onHeldItemRender(HeldItemRendererEvent event) {
      if (event.getHand() == Hand.MAIN_HAND) {
         event.getStack().translate(this.positionMainX.getValueFloat(), this.positionMainY.getValueFloat(), this.positionMainZ.getValueFloat());
         event.getStack().scale(this.scaleMainX.getValueFloat(), this.scaleMainY.getValueFloat(), this.scaleMainZ.getValueFloat());
         event.getStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotationMainX.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotationMainY.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotationMainZ.getValueFloat()));
      } else {
         event.getStack().translate(this.positionOffX.getValueFloat(), this.positionOffY.getValueFloat(), this.positionOffZ.getValueFloat());
         event.getStack().scale(this.scaleOffX.getValueFloat(), this.scaleOffY.getValueFloat(), this.scaleOffZ.getValueFloat());
         event.getStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotationOffX.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotationOffY.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotationOffZ.getValueFloat()));
      }

   }
}
