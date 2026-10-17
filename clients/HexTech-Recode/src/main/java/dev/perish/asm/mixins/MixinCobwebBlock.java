package dev.perish.asm.mixins;

import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.mod.modules.impl.movement.BlockStrafe;
import dev.perish.mod.modules.impl.movement.FastWeb;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.CobwebBlock;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CobwebBlock.class)
public class MixinCobwebBlock {
    @Inject(at = @At("HEAD"), method = "onEntityCollision", cancellable = true)
    private void onGetVelocityMultiplier(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (entity == Wrapper.mc.player
                && BlockStrafe.INSTANCE != null && BlockStrafe.INSTANCE.isOn()
                && EntityUtil.isInsideBlock()
                && !BlockStrafe.INSTANCE.noInWeb.getValue()) {
            ci.cancel();
            return;
        }
        if (FastWeb.INSTANCE.isOn() && (Wrapper.mc.options.sneakKey.isPressed() || !FastWeb.INSTANCE.onlySneak.getValue())) {
            if (FastWeb.INSTANCE.mode.is(FastWeb.Mode.Ignore)) {
                ci.cancel();
                entity.onLanding();
            } else if (FastWeb.INSTANCE.mode.is(FastWeb.Mode.Custom)) {
                ci.cancel();
                entity.slowMovement(
                        state,
                        new Vec3d(FastWeb.INSTANCE.xZSlow.getValue() / 100.0, FastWeb.INSTANCE.ySlow.getValue() / 100.0, FastWeb.INSTANCE.xZSlow.getValue() / 100.0)
                );
            }
        }
    }
}
