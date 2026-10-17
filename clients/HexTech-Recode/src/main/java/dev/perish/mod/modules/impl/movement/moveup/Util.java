package dev.perish.mod.modules.impl.movement.moveup;

import static dev.perish.api.utils.Wrapper.mc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class Util {
    public static boolean isBurrowed(PlayerEntity entity, boolean Echest) {
        return Util.doesBoxTouchBlock(entity.getBoundingBox(), Echest);
    }

    public static boolean doesBoxTouchBlock(Box box, boolean Echest) {
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    if (mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.BEDROCK || (mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.ENDER_CHEST && Echest) || (mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.RESPAWN_ANCHOR)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
