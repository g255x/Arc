package dev.g255.arc.api.events.impl;

import dev.g255.arc.api.events.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;

public class VoxelShapeEvent extends Event {
   private static final VoxelShapeEvent INSTANCE = new VoxelShapeEvent();
   public VoxelShape shape;
   public BlockState state;
   public BlockPos pos;

   private VoxelShapeEvent() {
   }

   public static VoxelShapeEvent get(VoxelShape shape, BlockState state, BlockPos pos) {
      INSTANCE.setCancelled(false);
      INSTANCE.shape = shape;
      INSTANCE.state = state;
      INSTANCE.pos = pos;
      return INSTANCE;
   }
}
