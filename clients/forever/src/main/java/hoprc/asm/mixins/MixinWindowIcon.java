package hoprc.asm.mixins;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourcePack;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.nio.ByteBuffer;

@Mixin(Window.class)
public abstract class MixinWindowIcon {

    @Shadow
    @Final
    private long handle;

    @Inject(
            method = "setIcon(Lnet/minecraft/resource/ResourcePack;Lnet/minecraft/client/util/Icons;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forever$setCustomIcon(ResourcePack resourcePack, Icons icons, CallbackInfo ci) {
        try {
            forever$setIconFromResource("/assets/minecraft/textures/icon.png");
            ci.cancel();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Unique
    private void forever$setIconFromResource(String path) throws Exception {
        InputStream inputStream = MixinWindowIcon.class.getResourceAsStream(path);

        if (inputStream == null) {
            throw new IllegalStateException("Window icon not found: " + path);
        }

        NativeImage nativeImage = NativeImage.read(inputStream);

        try {
            int width = nativeImage.getWidth();
            int height = nativeImage.getHeight();

            ByteBuffer byteBuffer = MemoryUtil.memAlloc(width * height * 4);

            try {
                byteBuffer.asIntBuffer().put(nativeImage.copyPixelsRgba());

                try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                    GLFWImage.Buffer iconBuffer = GLFWImage.malloc(1, memoryStack);

                    iconBuffer.position(0);
                    iconBuffer.width(width);
                    iconBuffer.height(height);
                    iconBuffer.pixels(byteBuffer);
                    iconBuffer.position(0);

                    GLFW.glfwSetWindowIcon(this.handle, iconBuffer);
                }
            } finally {
                MemoryUtil.memFree(byteBuffer);
            }
        } finally {
            nativeImage.close();
            inputStream.close();
        }
    }
}