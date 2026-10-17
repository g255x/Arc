package dev.perish.mod.modules.impl.render;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.AmbientOcclusionEvent;
import dev.perish.api.events.impl.ChunkOcclusionEvent;
import dev.perish.api.events.impl.RenderBlockEntityEvent;
import dev.perish.mod.gui.windows.WindowsScreen;
import dev.perish.mod.gui.windows.impl.ItemSelectWindow;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.world.BlockView;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.block.BlockState;

public class Xray extends Module {
    private static final ThreadLocal<Mutable> EXPOSED_POS = ThreadLocal.withInitial(Mutable::new);
    public static Xray INSTANCE;    public final BooleanSetting edit = this.add(new BooleanSetting("Edit", false).injectTask(this::openGui));
    public Xray() {
        super("Xray", Module.Category.Render);
        this.setChinese("矿物透视");
        INSTANCE = this;
    }

    public static boolean shouldBlock(BlockState state) {
        return INSTANCE.isOn() && INSTANCE.isBlocked(state.getBlock());
    }

    public static boolean isExposed(BlockPos blockPos) {
        for (Direction direction : Direction.values()) {
            if (!mc.world.getBlockState(EXPOSED_POS.get().set(blockPos, direction)).isOpaque()) {
                return true;
            }
        }

        return false;
    }

    private void openGui() {
        this.edit.setValueWithoutTask(false);
        if (!nullCheck()) {
            mc.setScreen(new WindowsScreen(new ItemSelectWindow(Perish.XRAY)));
        }
    }

    @Override
    public void onEnable() {
        if (!nullCheck()) {
            mc.worldRenderer.reload();
        }
    }

    @Override
    public void onDisable() {
        mc.worldRenderer.reload();
    }

    public boolean isBlocked(Block block) {
        return !Perish.XRAY.inWhitelist(block.getTranslationKey());
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

    public boolean modifyDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, boolean returns) {
        if (!returns && !this.isBlocked(state.getBlock())) {
            BlockPos adjPos = pos.offset(facing);
            BlockState adjState = view.getBlockState(adjPos);
            return adjState.getCullingFace(view, adjPos, facing.getOpposite()) != VoxelShapes.fullCube()
                    || adjState.getBlock() != state.getBlock()
                    || isExposed(adjPos);
        } else {
            return returns;
        }
    }


}
