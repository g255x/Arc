package dev.perish.asm.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.PostEffectPass;

@Mixin(PostEffectProcessor.class)
public interface IPostEffectProcessor {
    @Accessor
    List<PostEffectPass> getPasses();
}
