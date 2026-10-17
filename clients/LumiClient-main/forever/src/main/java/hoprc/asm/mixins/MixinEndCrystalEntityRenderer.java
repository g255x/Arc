package hoprc.asm.mixins;

import dev.kissoo.mod.modules.impl.render.Chams;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class MixinEndCrystalEntityRenderer extends EntityRenderer<EndCrystalEntity> {

   protected MixinEndCrystalEntityRenderer(Context ctx) {
      super(ctx);
   }

   @Inject(
           method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
           at = @At("HEAD"),
           cancellable = true
   )
   public void onRenderHead(
           EndCrystalEntity entity,
           float yaw,
           float tickDelta,
           MatrixStack matrices,
           VertexConsumerProvider vertexConsumers,
           int light,
           CallbackInfo ci
   ) {
      Chams module = Chams.INSTANCE;
      if (module != null && module.isOn() && module.crystal.getValue() && module.cancelVanilla.getValue()) {
         ci.cancel();
      }
   }

   @Redirect(
           method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"
           )
   )
   public void redirectScale(MatrixStack matrices, float x, float y, float z) {
      Chams module = Chams.INSTANCE;
      if (module != null && module.isOn() && module.crystal.getValue()) {
         float scale = module.crystalScale.getValueFloat();
         matrices.scale(x * scale, y * scale, z * scale);
      } else {
         matrices.scale(x, y, z);
      }
   }
}