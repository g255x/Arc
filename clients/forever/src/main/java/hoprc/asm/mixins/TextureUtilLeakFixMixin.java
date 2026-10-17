package hoprc.asm.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.TextureUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.nio.ByteBuffer;
import java.nio.channels.Channel;

@Environment(EnvType.CLIENT)
@Mixin(TextureUtil.class)
public class TextureUtilLeakFixMixin {

    @WrapOperation(
            method = "readResource",
            at = {
                    @At(value = "INVOKE", target = "Ljava/nio/channels/FileChannel;read(Ljava/nio/ByteBuffer;)I"),
                    @At(value = "INVOKE", target = "Ljava/nio/channels/ReadableByteChannel;read(Ljava/nio/ByteBuffer;)I")
            }
    )
    private static int memoryLeakFix$readResourceWithoutLeak(@Coerce Channel channel, ByteBuffer byteBuf, Operation<Integer> original) {
        try {
            // 尝试正常读取
            return original.call(channel, byteBuf);
        } catch (Exception e) {
            MemoryUtil.memFree(byteBuf);
            throw e;
        }
    }
}