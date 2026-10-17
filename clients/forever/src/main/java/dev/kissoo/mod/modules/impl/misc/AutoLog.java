package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.TotemEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class AutoLog extends Module {
   public static boolean loggedOut = false;

   private final BooleanSetting logOnEnable = this.add(new BooleanSetting("LogOnEnable", false));
   private final BooleanSetting onPop       = this.add(new BooleanSetting("OnPop", true));
   private final BooleanSetting lowArmor    = this.add(new BooleanSetting("LowArmor", true));
   private final BooleanSetting totemLess   = this.add(new BooleanSetting("TotemLess", true).setParent());
   private final SliderSetting  totems      = this.add(new SliderSetting("Totems", 2.0, 0.0, 20.0, 1.0, this.totemLess::isOpen));
   private final BooleanSetting antiMace    = this.add(new BooleanSetting("AntiMace", true).setParent());
   private final SliderSetting  antiRange   = this.add(new SliderSetting("AntiRange", 15.0, 3.0, 30.0, 0.5, this.antiMace::isOpen));
   private final SliderSetting  safeRange   = this.add(new SliderSetting("SafeRange",  3.0, 1.0, 20.0, 0.5, this.antiMace::isOpen));
   private final SliderSetting  antiHRange  = this.add(new SliderSetting("AntiHRange", 8.0, 1.0, 20.0, 0.5, this.antiMace::isOpen));
   private final BooleanSetting ignoreElytra = this.add(new BooleanSetting("IgnoreElytra", true, this.antiMace::isOpen));

   private final BooleanSetting antiMaceMsg = this.add(new BooleanSetting("SendMessage", true, this.antiMace::isOpen));
   private final SliderSetting  randomLen   = this.add(new SliderSetting("MsgRandomLen", 4.0, 0.0, 10.0, 1.0, () -> this.antiMace.isOpen() && this.antiMaceMsg.getValue()));

   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));

   private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
   private final Random random = new Random();

   public AutoLog() {
      super("AutoLog", Module.Category.Misc);
      this.setChinese("自动下线");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (nullCheck()) return;

      if (this.antiMace.getValue()) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (Forever.FRIEND.isFriend(player)) continue;
            if (!player.isAlive()) continue;
            if (player.getMainHandStack().getItem() != Items.MACE) continue;
            Vec3d myPos    = mc.player.getPos();
            Vec3d enemyPos = player.getPos();
            double yDiff = enemyPos.y - myPos.y;
            double horizontalDist = Math.sqrt(
                    Math.pow(enemyPos.x - myPos.x, 2) +
                            Math.pow(enemyPos.z - myPos.z, 2)
            );
            if (yDiff <= safeRange.getValue() || yDiff > antiRange.getValue()) {
               continue;
            }
            if (horizontalDist > antiHRange.getValue()) {
               continue;
            }
            if (ignoreElytra.getValue() && isWearingElytra(player)) {
               continue;
            }
            fastLog(player);
            return;
         }
      }

      checkOriginalLogs();
   }

   private boolean isWearingElytra(PlayerEntity player) {
      ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
      return chestplate.getItem() == Items.ELYTRA;
   }

   private void fastLog(PlayerEntity attacker) {
      if (mc.getNetworkHandler() == null) return;

      String name = attacker.getGameProfile().getName();
      mc.getNetworkHandler().sendPacket(
              PlayerInteractEntityC2SPacket.attack(mc.player, false)
      );
      CommandManager.sendMessage("§4[AutoLog] §fMace: " + name);
      if (this.antiMaceMsg.getValue()) {
         try {
            mc.getNetworkHandler().sendChatMessage(
                    "@" + name + " 牺牲自己的老冯尝试使用重锤砸我被我巧妙地躲开了. "
                            + generateRandomString(randomLen.getValueInt())
            );
         } catch (Exception ignored) {}
      }
   }

   private void disconnect(String reason) {
      if (mc.getNetworkHandler() == null) return;

      loggedOut = true;
      CommandManager.sendMessage("§4[AutoLog] §f" + reason);

      mc.getNetworkHandler().sendPacket(
              PlayerInteractEntityC2SPacket.attack(mc.player, false)
      );
   }

   private void checkOriginalLogs() {
      if (this.totemLess.getValue()) {
         int totem = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
         if (totem <= this.totems.getValueInt()) {
            this.disconnect("Low Totems (" + totem + ")");
         }
      }

      if (this.lowArmor.getValue()) {
         for (ItemStack armor : mc.player.getInventory().armor) {
            if (!armor.isEmpty() && EntityUtil.getDamagePercent(armor) < 5) {
               this.disconnect("Armor Durability Low");
            }
         }
      }
   }

   @EventListener
   public void onPop(TotemEvent event) {
      if (this.onPop.getValue() && event.getPlayer() == mc.player) {
         this.disconnect("Totem Popped");
      }
   }

   @Override
   public void onLogout() {
      if (this.autoDisable.getValue()) this.disable();
   }

   @Override
   public void onEnable() {
      if (this.logOnEnable.getValue()) this.disconnect("Enabled");
   }

   private String generateRandomString(int length) {
      if (length <= 0) return "";
      StringBuilder sb = new StringBuilder("[");
      for (int i = 0; i < length; i++) {
         sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
      }
      return sb.append("]").toString();
   }
}