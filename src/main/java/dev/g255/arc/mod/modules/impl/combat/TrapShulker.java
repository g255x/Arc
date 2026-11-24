package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.PlaceBlockEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.MathHelper;

public class TrapShulker extends Module {
    public static TrapShulker INSTANCE;
    public final List<BlockPos> safe = new ArrayList<>();
    private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 6.0, 0.1));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));

    public TrapShulker() {
        super("TrapShulker", Category.Combat);//liwangzi AI Power.不是我写的。
        this.setChinese("自动堵盒");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.safe.removeIf(pos -> !(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock));

        BlockPos targetPos = findTarget();
        if (targetPos != null) {
            int obbySlot = InventoryUtil.findItem(Items.OBSIDIAN);
            if (obbySlot != -1) {
                int oldSlot = mc.player.getInventory().selectedSlot;

                InventoryUtil.switchToSlot(obbySlot);

                BlockUtil.clickBlock(targetPos, Direction.UP, this.rotate.getValue());

                InventoryUtil.switchToSlot(oldSlot);
            }
        }
    }

    @EventListener
    public void onPlace(PlaceBlockEvent event) {
        if (event.block instanceof ShulkerBoxBlock) {
            this.safe.add(event.blockPos);
        }
    }

    private BlockPos findTarget() {
        for (BlockEntity entity : BlockUtil.getTileEntities()) {
            if (entity instanceof ShulkerBoxBlockEntity shulker) {
                BlockPos pos = shulker.getPos();

                if (MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos())) > this.range.getValue()) {
                    continue;
                }

                if (!mc.world.isAir(pos.up())) {
                    continue;
                }

                if (this.safe.contains(pos)) {
                    continue;
                }

                return pos;
            }
        }
        return null;
    }
}
