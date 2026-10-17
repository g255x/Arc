package dev.baimaodaxia.asm.mixins;

import com.mojang.serialization.MapCodec;
import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.impl.BlockActivateEvent;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.state.property.Property;

@Mixin({BlockState.class})
public abstract class MixinBlockState extends AbstractBlock.AbstractBlockState {
   public MixinBlockState(Block block, Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap, MapCodec<BlockState> mapCodec) {
      super(block, propertyMap, mapCodec);
   }

   public ActionResult onUse(World world, PlayerEntity player, BlockHitResult hit) {
      earthquake.EVENT_BUS.post(BlockActivateEvent.get((BlockState)BlockState.class.cast(this)));
      return super.onUse(world, player, hit);
   }
}
