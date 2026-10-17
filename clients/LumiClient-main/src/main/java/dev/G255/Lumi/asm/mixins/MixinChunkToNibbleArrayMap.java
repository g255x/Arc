package dev.G255.Lumi.asm.mixins;

import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value={ChunkToNibbleArrayMap.class})
public class MixinChunkToNibbleArrayMap {
    @ModifyVariable(method={"replaceWithCopy"}, at=@At(value="STORE"), ordinal=0)
    private ChunkNibbleArray nullSafeArray(ChunkNibbleArray array) {
        if (array == null) {
            return new ChunkNibbleArray();
        }
        return array;
    }
}