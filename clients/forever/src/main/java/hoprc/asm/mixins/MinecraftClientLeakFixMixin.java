package hoprc.asm.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientLeakFixMixin {

    @Shadow @Nullable public Entity targetedEntity;
    @Shadow @Nullable public HitResult crosshairTarget;

    // --- 核心修复：仅在切换世界或退出服务器时清空引用 ---
    // method_1584 是 setWorld 的底层映射名，保证跨版本注入成功
    @Inject(method = {"setWorld", "method_1584"}, at = @At("HEAD"))
    private void memoryLeakFix$resetTargetOnWorldChange(@Nullable ClientWorld world, CallbackInfo ci) {
        this.targetedEntity = null;
        this.crosshairTarget = null;
    }
}