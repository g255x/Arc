package dev.G255.Lumi.api.utils.world;

import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.client.MinecraftClient;

public class CombatBlockUtil {

    public record Swap(int slot) {}

    public static Swap swap(int slot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int oldSlot = mc.player.getInventory().selectedSlot;
        InventoryUtil.switchToSlot(slot);
        return new Swap(oldSlot);
    }

    public static void restore(Swap swap) {
        if (swap != null) {
            InventoryUtil.switchToSlot(swap.slot());
        }
    }

    public static int findItem(Item item, boolean inventory) {
        if (inventory) {
            return InventoryUtil.findItemInventorySlot(item);
        }
        return InventoryUtil.findItem(item);
    }

    public static boolean isValidTarget(Entity entity) {
        return CombatUtil.isValid(entity);
    }
}
