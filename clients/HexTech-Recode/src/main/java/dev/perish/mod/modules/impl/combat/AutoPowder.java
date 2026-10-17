package dev.perish.mod.modules.impl.combat;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.combat.CombatUtil;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoPowder extends Module {
    private final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 0.0, 0.0, 1000.0, 10.0).setSuffix("ms"));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final SliderSetting height = this.add(new SliderSetting("Height", 2.0, 0.0, 5.0, 1.0).setSuffix("m"));
    private final BooleanSetting airPlace = this.add(new BooleanSetting("AirPlace", true));
    private final BooleanSetting surround = this.add(new BooleanSetting("Surround", false));
    private final BooleanSetting burrow = this.add(new BooleanSetting("Burrow", false));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));

    private final Timer timer = new Timer();

    public AutoPowder() {
        super("AutoPowder", Category.Combat);//liwangzi AI Power.不是我写的。
        this.setChinese("自动混凝土粉末");
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
        if (target == null) return;

        if (!this.timer.passedMs(this.delay.getValue())) {
            return;
        }

        boolean surroundCheck = this.surround.getValue() && isSurrounded(target);
        boolean burrowCheck = this.burrow.getValue() && isBurrowed(target);

        if ((this.surround.getValue() || this.burrow.getValue()) && !surroundCheck && !burrowCheck) {
            return;
        }

        int slot = InventoryUtil.findClass(ConcretePowderBlock.class);
        int invSlot = -1;
        if (slot == -1 && this.inventory.getValue()) {
            invSlot = InventoryUtil.findClassInventorySlot(ConcretePowderBlock.class);
        }

        if (slot == -1 && invSlot == -1) return;

        BlockPos targetPos = EntityUtil.getEntityPos(target, true).up(this.height.getValueInt());

        if (!BlockUtil.canPlace(targetPos) && !this.airPlace.getValue()) {
            return;
        }

        if (mc.world.getBlockState(targetPos).getBlock() != Blocks.AIR) {
            return;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;

        if (slot != -1) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(invSlot, mc.player.getInventory().selectedSlot);
        }

        if (BlockUtil.canPlace(targetPos)) {
            BlockUtil.placeBlock(targetPos, this.rotate.getValue());
            this.timer.reset();
        } else if (this.airPlace.getValue()) {
            BlockUtil.airPlace(targetPos, this.rotate.getValue());
            this.timer.reset();
        }

        if (slot != -1) {
            InventoryUtil.switchToSlot(oldSlot);
        } else {
            InventoryUtil.inventorySwap(invSlot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
        }
    }

    private boolean isSurrounded(PlayerEntity player) {
        BlockPos pos = EntityUtil.getEntityPos(player, true);
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            BlockPos offset = pos.offset(dir);
            if (mc.world.getBlockState(offset).getCollisionShape(mc.world, offset).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean isBurrowed(PlayerEntity player) {
        BlockPos pos = EntityUtil.getEntityPos(player, true);
        if (!mc.world.isAir(pos)) return true;
        return !!mc.world.getBlockCollisions(player, player.getBoundingBox()).iterator().hasNext();
    }
}
