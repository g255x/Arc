package hoprc.api.handler;

import dev.kissoo.mod.modules.impl.combat.AutoRegear;
import dev.kissoo.mod.modules.impl.misc.ShulkerViewer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import dev.kissoo.Forever;

/**
 * Static helper invoked from MixinHandledScreen.
 * Lives in hoprc (unobfuscated) so ASM frame computation stays clean
 * even when the dev.kissoo module classes are flow-obfuscated by ZKM.
 */
public final class HandledScreenHandler {

    private HandledScreenHandler() {}

    /**
     * @return true if the event should be cancelled and the return value set to true
     */
    public static boolean onMouseClicked(Slot focusedSlot, ItemStack[] items,
                                         net.minecraft.client.MinecraftClient mc) {
        if (focusedSlot == null) return false;
        ItemStack stack = focusedSlot.getStack();
        if (stack.isEmpty()) return false;
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) return false;
        if (!ShulkerViewer.INSTANCE.isOn()) return false;

        if (ShulkerViewer.hasItems(stack)
                || (stack.getItem() == Items.ENDER_CHEST && Forever.PLAYER.known)) {
            return ShulkerViewer.openContainer(stack, items, false);
        }
        return false;
    }

    /**
     * @return true if rendering should be cancelled
     */
    public static boolean onRenderHead(Object screen) {
        if (!(screen instanceof ShulkerBoxScreen)) return false;
        if (AutoRegear.INSTANCE == null || !AutoRegear.INSTANCE.isOn()) return false;
        return AutoRegear.INSTANCE.silentDisplay.getValue() && AutoRegear.INSTANCE.silentScreenActive;
    }

    public static void onRenderReturn(DrawContext context, int mouseX, int mouseY,
                                      Slot focusedSlot,
                                      net.minecraft.client.MinecraftClient mc) {
        if (ShulkerViewer.INSTANCE == null || !ShulkerViewer.INSTANCE.isOn()) return;
        if (!ShulkerViewer.INSTANCE.toolTips.getValue()) return;
        if (focusedSlot == null || focusedSlot.getStack().isEmpty()) return;
        if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) return;

        ItemStack stack = focusedSlot.getStack();
        if (ShulkerViewer.hasItems(stack)
                || (stack.getItem() == Items.ENDER_CHEST && Forever.PLAYER.known)) {
            ShulkerViewer.renderShulkerToolTip(context, mouseX, mouseY, stack);
        }
    }
}
