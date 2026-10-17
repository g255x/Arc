package dev.perish.asm.mixins;

import dev.perish.mod.modules.impl.movement.Velocity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;
import net.minecraft.util.math.Direction;
import net.minecraft.fluid.FlowableFluid;

@Mixin(FlowableFluid.class)
public class MixinFlowableFluid {
    @Redirect(method = "getVelocity", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", ordinal = 0), require = 0)
    private boolean getVelocity_hasNext(Iterator<Direction> var9) {
        return (!Velocity.INSTANCE.isOn() || !Velocity.INSTANCE.waterPush.getValue()) && var9.hasNext();
    }
}
