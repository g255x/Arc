package hoprc.asm.mixins;

import hoprc.api.interfaces.IShaderEffectHook;
import hoprc.asm.accessors.IPostProcessShader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostEffectProcessor.class)
public class MixinShaderEffect implements IShaderEffectHook {

   @Unique
   private List<String> fakedBufferNames;

   @Shadow
   @Final
   private Map<String, Framebuffer> targetsByName;

   @Shadow
   @Final
   private List<PostEffectPass> passes;

   @Override
   public void alienClient$addHook(String name, Framebuffer buffer) {
      if (this.fakedBufferNames == null) {
         this.fakedBufferNames = new ArrayList<>();
      }

      Framebuffer previousFramebuffer = this.targetsByName.get(name);
      if (previousFramebuffer != buffer) {
         if (previousFramebuffer != null) {
            for (PostEffectPass pass : this.passes) {
               if (pass.input == previousFramebuffer) {
                  ((IPostProcessShader)pass).setInput(buffer);
               }

               if (pass.output == previousFramebuffer) {
                  ((IPostProcessShader)pass).setOutput(buffer);
               }
            }

            this.targetsByName.remove(name);
            this.fakedBufferNames.remove(name);
         }

         this.targetsByName.put(name, buffer);
         this.fakedBufferNames.add(name);
      }
   }

   @Inject(method = "close", at = @At("HEAD"))
   void deleteFakeBuffersHook(CallbackInfo ci) {
      if (this.fakedBufferNames != null) {
         for (String fakedBufferName : this.fakedBufferNames) {
            this.targetsByName.remove(fakedBufferName);
         }
      }
   }
}