package dev.G255.Lumi.mod.modules.impl.misc;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.TotemEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.core.impl.CommandManager;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class AutoLog
extends Module {
    public static boolean loggedOut = false;
    private final BooleanSetting logOnEnable = this.add(new BooleanSetting("LogOnEnable", false));
    private final BooleanSetting onPop = this.add(new BooleanSetting("OnPop", true));
    private final BooleanSetting lowArmor = this.add(new BooleanSetting("LowArmor", true));
    private final BooleanSetting totemLess = this.add(new BooleanSetting("TotemLess", true).setParent());
    private final SliderSetting totems = this.add(new SliderSetting("Totems", 2.0, 0.0, 20.0, 1.0, () -> ((BooleanSetting)this.totemLess).isOpen()));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
    private final BooleanSetting showReason = this.add(new BooleanSetting("ShowReason", false));

    public AutoLog() {
        super("AutoLog", "Automatically disconnects when health or pop count is low.", Module.Category.Misc);
        this.setChinese("\u81ea\u52a8\u4e0b\u7ebf");
        this.setChineseDesc("\u8840\u91cf\u6216\u7206\u56fe\u817e\u6570\u4f4e\u65f6\u81ea\u52a8\u65ad\u5f00\u8fde\u63a5");
    }

    public void onEnable() {
        if (this.logOnEnable.getValue()) {
            this.disconnect("Enabled");
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        int totem;
        if (this.totemLess.getValue() && (double)(totem = InventoryUtil.getItemCount((Item)Items.TOTEM_OF_UNDYING)) <= this.totems.getValue()) {
            this.disconnect("You have too few totems (" + totem + ").");
            return;
        }
        if (this.lowArmor.getValue()) {
            for (ItemStack armor : AutoLog.mc.player.getInventory().armor) {
                int damage;
                if (armor.isEmpty() || (damage = EntityUtil.getDamagePercent((ItemStack)armor)) >= 5) continue;
                this.disconnect("Your armor has a durability of less than 5%.");
                return;
            }
        }
    }

    @EventListener
    public void onPop(TotemEvent event) {
        if (this.onPop.getValue() && event.getPlayer() == AutoLog.mc.player) {
            this.disconnect("You poped 1 totem!");
        }
    }

    public void onLogout() {
        if (this.autoDisable.getValue()) {
            this.disable();
        }
    }

    private void disconnect(String reason) {
        loggedOut = true;
        CommandManager.sendMessage((String)("\u00a74[AutoLog] " + reason));
        mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(114514));
        if (this.showReason.getValue()) {
            AutoLog.mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket((Text)Text.literal((String)("[AutoLog]" + reason))));
        }
    }
}