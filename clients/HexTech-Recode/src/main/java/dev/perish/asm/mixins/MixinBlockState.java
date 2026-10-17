package dev.perish.asm.mixins;

import com.mojang.serialization.MapCodec;
import dev.perish.Perish;
import dev.perish.api.events.impl.BlockActivateEvent;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public abstract class MixinBlockState extends AbstractBlockState {
    public MixinBlockState(Block block, Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap, MapCodec<BlockState> mapCodec) {
        super(block, propertyMap, mapCodec);
    }

    @Override
    public ActionResult onUse(World world, PlayerEntity player, BlockHitResult hit) {
        Perish.EVENT_BUS.post(BlockActivateEvent.get(BlockState.class.cast(this)));
        return super.onUse(world, player, hit);
    }
}
