package dev.kissoo.api.utils.world;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class MaceHelper {
    public static boolean isSpear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.isOf(Items.TRIDENT) || stack.isOf(Items.MACE);
    }
}
