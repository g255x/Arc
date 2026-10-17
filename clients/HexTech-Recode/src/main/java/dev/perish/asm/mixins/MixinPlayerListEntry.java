package dev.perish.asm.mixins;

import dev.perish.mod.modules.impl.client.CapeModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public class MixinPlayerListEntry {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        if (MinecraftClient.getInstance().player == null) return;

        PlayerListEntry self = (PlayerListEntry) (Object) this;

        // 检查是否是本地玩家
        if (!self.getProfile().getId().equals(MinecraftClient.getInstance().player.getUuid())) {
            return;
        }

        if (CapeModule.INSTANCE == null || !CapeModule.INSTANCE.isOn()) {
            return;
        }

        SkinTextures original = cir.getReturnValue();

        // 使用更安全的方式创建 SkinTextures
        try {
            // 方法1：使用反射获取原始值
            SkinTextures newTextures = createCustomSkinTextures(original, CapeModule.INSTANCE.getSelectedTexture());
            cir.setReturnValue(newTextures);
        } catch (Exception e) {
            System.err.println("创建 SkinTextures 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private SkinTextures createCustomSkinTextures(SkinTextures original, net.minecraft.util.Identifier capeTexture) {
        // 尝试不同的构造方式
        try {
            // 现代版本的方式
            return new SkinTextures(
                    original.texture(),       // 皮肤纹理
                    original.textureUrl(),    // 皮肤URL
                    capeTexture,              // 斗篷纹理
                    original.elytraTexture(), // 鞘翅纹理
                    original.model(),         // 皮肤模型
                    original.secure()         // 是否安全
            );
        } catch (Exception e1) {
            try {
                // 旧版本的方式
                return new SkinTextures(
                        getField(original, "texture"),
                        getField(original, "textureUrl"),
                        capeTexture,
                        getField(original, "elytraTexture"),
                        getField(original, "model"),
                        getField(original, "secure")
                );
            } catch (Exception e2) {
                // 如果都不行，返回原始值
                return original;
            }
        }
    }

    private <T> T getField(Object obj, String fieldName) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(obj);
    }
}