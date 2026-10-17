package dev.baimaodaxia.mod.modules.impl.render;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.AmbientOcclusionEvent;
import dev.baimaodaxia.api.events.impl.ChunkOcclusionEvent;
import dev.baimaodaxia.api.events.impl.RenderBlockEntityEvent;
import dev.baimaodaxia.mod.gui.windows.WindowBase;
import dev.baimaodaxia.mod.gui.windows.WindowsScreen;
import dev.baimaodaxia.mod.gui.windows.impl.ItemSelectWindow;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.world.BlockView;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.block.BlockState;

public class Xray extends Module {
   public static Xray INSTANCE;
   public final BooleanSetting edit = this.add((new BooleanSetting("Edit", false)).injectTask(this::openGui));
   private static final ThreadLocal<BlockPos.Mutable> EXPOSED_POS = ThreadLocal.withInitial(BlockPos.Mutable::new);

   public Xray() {
      super("Xray", Module.Category.Render);
      this.setChinese("矿物透视");
      INSTANCE = this;
   }

   private void openGui() {
      this.edit.setValueWithoutTask(false);
      if (!nullCheck()) {
         mc.setScreen(new WindowsScreen(new WindowBase[]{new ItemSelectWindow(Alien.XRAY)}));
      }

   }

   public void onEnable() {
      if (!nullCheck()) {
         mc.worldRenderer.reload();
      }
   }

   public void onDisable() {
      mc.worldRenderer.reload();
   }

   public boolean isBlocked(Block block) {
      return !Alien.XRAY.inWhitelist(block.getTranslationKey());
   }

   @EventListener
   private void onRenderBlockEntity(RenderBlockEntityEvent event) {
      if (this.isBlocked(event.blockEntity.getCachedState().getBlock())) {
         event.cancel();
      }

   }

   @EventListener
   private void onChunkOcclusion(ChunkOcclusionEvent event) {
      event.cancel();
   }

   @EventListener
   private void onAmbientOcclusion(AmbientOcclusionEvent event) {
      event.lightLevel = 1.0F;
   }

   public static boolean shouldBlock(BlockState state) {
      return INSTANCE.isOn() && INSTANCE.isBlocked(state.getBlock());
   }

   public boolean modifyDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, boolean returns) {
      if (!returns && !this.isBlocked(state.getBlock())) {
         BlockPos adjPos = pos.offset(facing);
         BlockState adjState = view.getBlockState(adjPos);
         return adjState.getCullingFace(view, adjPos, facing.getOpposite()) != VoxelShapes.fullCube() || adjState.getBlock() != state.getBlock() || isExposed(adjPos);
      } else {
         return returns;
      }
   }

   public static boolean isExposed(BlockPos blockPos) {
      for(Direction direction : Direction.values()) {
         if (!mc.world.getBlockState(((BlockPos.Mutable)EXPOSED_POS.get()).set(blockPos, direction)).isOpaque()) {
            return true;
         }
      }

      return false;
   }
}
