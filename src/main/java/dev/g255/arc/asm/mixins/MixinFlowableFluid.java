package dev.g255.arc.asm.mixins;

import dev.g255.arc.mod.modules.impl.movement.Velocity;
import java.util.Iterator;
import net.minecraft.util.math.Direction;
import net.minecraft.fluid.FlowableFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({FlowableFluid.class})
public class MixinFlowableFluid {
   @Redirect(
      method = {"getVelocity"},
      at = @At(
   value = "INVOKE",
   target = "Ljava/util/Iterator;hasNext()Z",
   ordinal = 0
),
      require = 0
   )
   private boolean getVelocity_hasNext(Iterator<Direction> var9) {
      return Velocity.INSTANCE.isOn() && Velocity.INSTANCE.waterPush.getValue() ? false : var9.hasNext();
   }
}
