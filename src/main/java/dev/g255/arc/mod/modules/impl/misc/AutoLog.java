package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.TotemEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.core.impl.CommandManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class AutoLog extends Module {
   public static boolean loggedOut = false;
   private final BooleanSetting logOnEnable = this.add(new BooleanSetting("LogOnEnable", false));
   private final BooleanSetting onPop = this.add(new BooleanSetting("OnPop", true));
   private final BooleanSetting lowArmor = this.add(new BooleanSetting("LowArmor", true));
   private final BooleanSetting totemLess = this.add((new BooleanSetting("TotemLess", true)).setParent());
   private final SliderSetting totems;
   private final BooleanSetting autoDisable;
   private final BooleanSetting showReason;

   public AutoLog() {
      super("AutoLog", Module.Category.Misc);
      BooleanSetting var10009 = this.totemLess;
      Objects.requireNonNull(var10009);
      this.totems = this.add(new SliderSetting("Totems", (double)2.0F, (double)0.0F, (double)20.0F, (double)1.0F, var10009::isOpen));
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
      this.showReason = this.add(new BooleanSetting("ShowReason", false));
      this.setChinese("自动下线");
   }

   public void onEnable() {
      if (this.logOnEnable.getValue()) {
         this.disconnect("Enabled");
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      int totem;
      if (this.totemLess.getValue() && (double)(totem = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING)) <= this.totems.getValue()) {
         this.disconnect("You have too few totems (" + totem + ").");
      } else {
         if (this.lowArmor.getValue()) {
            for(ItemStack armor : mc.player.getInventory().armor) {
               if (!armor.isEmpty() && EntityUtil.getDamagePercent(armor) < 5) {
                  this.disconnect("Your armor has keyCodec durability of less than 5%.");
                  return;
               }
            }
         }

      }
   }

   @EventListener
   public void onPop(TotemEvent event) {
      if (this.onPop.getValue() && event.getPlayer() == mc.player) {
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
      CommandManager.sendMessage("§4[AutoLog] " + reason);
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(114514));
      if (this.showReason.getValue()) {
         mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[AutoLog]" + reason)));
      }

   }
}
