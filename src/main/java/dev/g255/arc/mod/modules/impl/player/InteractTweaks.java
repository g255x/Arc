package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;

public class InteractTweaks extends Module {
   public static InteractTweaks INSTANCE;
   public final BooleanSetting noEntityTrace = this.add((new BooleanSetting("NoEntityTrace", true)).setParent());
   public final BooleanSetting onlyPickaxe;
   public final BooleanSetting multiTask;
   public final BooleanSetting respawn;
   public final BooleanSetting ghostHand;
   private final BooleanSetting noAbort;
   private final BooleanSetting noReset;
   private final BooleanSetting noDelay;
   private final BooleanSetting noInteract;
   private final BooleanSetting pickaxeSwitch;
   private final BooleanSetting allowSword;
   private final BooleanSetting allowPickaxe;
   private final BooleanSetting reach;
   public final SliderSetting blockRange;
   public final SliderSetting entityRange;
   private final SliderSetting delay;
   public boolean isActive;
   boolean swapped;
   int lastSlot;

   public InteractTweaks() {
      super("InteractTweaks", Module.Category.Player);
      BooleanSetting var10006 = this.noEntityTrace;
      Objects.requireNonNull(var10006);
      this.onlyPickaxe = this.add(new BooleanSetting("OnlyPickaxe", true, var10006::isOpen));
      this.multiTask = this.add(new BooleanSetting("MultiTask", true));
      this.respawn = this.add(new BooleanSetting("Respawn", true));
      this.ghostHand = this.add(new BooleanSetting("IgnoreBedrock", false));
      this.noAbort = this.add(new BooleanSetting("NoMineAbort", false));
      this.noReset = this.add(new BooleanSetting("NoMineReset", false));
      this.noDelay = this.add(new BooleanSetting("NoMineDelay", false));
      this.noInteract = this.add(new BooleanSetting("NoInteract", false));
      this.pickaxeSwitch = this.add((new BooleanSetting("SwitchEat", false)).setParent());
      var10006 = this.pickaxeSwitch;
      Objects.requireNonNull(var10006);
      this.allowSword = this.add(new BooleanSetting("Sword", true, var10006::isOpen));
      var10006 = this.pickaxeSwitch;
      Objects.requireNonNull(var10006);
      this.allowPickaxe = this.add(new BooleanSetting("Pickaxe", true, var10006::isOpen));
      this.reach = this.add(new BooleanSetting("Reach", false));
      BooleanSetting var10009 = this.reach;
      Objects.requireNonNull(var10009);
      this.blockRange = this.add(new SliderSetting("BlockRange", (double)5.0F, (double)0.0F, (double)15.0F, 0.1, var10009::getValue));
      var10009 = this.reach;
      Objects.requireNonNull(var10009);
      this.entityRange = this.add(new SliderSetting("EntityRange", (double)5.0F, (double)0.0F, (double)15.0F, 0.1, var10009::getValue));
      this.delay = this.add(new SliderSetting("UseDelay", (double)4.0F, (double)0.0F, (double)4.0F, (double)1.0F));
      this.swapped = false;
      this.lastSlot = 0;
      this.setChinese("交互调整");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.respawn.getValue() && mc.currentScreen instanceof DeathScreen) {
         mc.player.requestRespawn();
         mc.setScreen((Screen)null);
      }

      if (mc.itemUseCooldown <= 4 - this.delay.getValueInt()) {
         mc.itemUseCooldown = 0;
      }

      if (this.pickaxeSwitch.getValue()) {
         if ((!(mc.player.getMainHandStack().getItem() instanceof PickaxeItem) || !this.allowPickaxe.getValue()) && (!(mc.player.getMainHandStack().getItem() instanceof SwordItem) || !this.allowSword.getValue()) && mc.player.getMainHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE && mc.player.getMainHandStack().getItem() != Items.GOLDEN_APPLE) {
            this.swapped = false;
            return;
         }

         int gapple = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
         if (gapple == -1) {
            gapple = InventoryUtil.findItem(Items.GOLDEN_APPLE);
         }

         if (gapple == -1) {
            if (this.swapped) {
               InventoryUtil.switchToSlot(this.lastSlot);
               this.swapped = false;
            }

            return;
         }

         if (mc.options.useKey.isPressed()) {
            if ((mc.player.getMainHandStack().getItem() instanceof PickaxeItem && this.allowPickaxe.getValue() || mc.player.getMainHandStack().getItem() instanceof SwordItem && this.allowSword.getValue()) && mc.player.getOffHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE && mc.player.getMainHandStack().getItem() != Items.GOLDEN_APPLE) {
               this.lastSlot = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(gapple);
               this.swapped = true;
            }
         } else if (this.swapped) {
            InventoryUtil.switchToSlot(this.lastSlot);
            this.swapped = false;
         }
      }

   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      Packet<?> packet;
      if (!nullCheck() && this.noInteract.getValue() && (packet = event.getPacket()) instanceof PlayerInteractBlockC2SPacket) {
         PlayerInteractBlockC2SPacket packet2 = (PlayerInteractBlockC2SPacket)packet;
         Block block = mc.world.getBlockState(packet2.getBlockHitResult().getBlockPos()).getBlock();
         if (!mc.player.isSneaking() && (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof AnvilBlock)) {
            event.cancel();
         }

      }
   }

   public void onDisable() {
      this.isActive = false;
   }

   public boolean shouldPauseCombat() {
      return this.isOn() && this.pickaxeSwitch.getValue() && this.swapped;
   }

   public boolean reach() {
      return this.isOn() && this.reach.getValue();
   }

   public boolean noAbort() {
      return this.isOn() && this.noAbort.getValue() && !mc.options.useKey.isPressed();
   }

   public boolean noReset() {
      return this.isOn() && this.noReset.getValue();
   }

   public boolean noDelay() {
      return this.isOn() && this.noDelay.getValue();
   }

   public boolean multiTask() {
      return this.isOn() && this.multiTask.getValue();
   }

   public boolean noEntityTrace() {
      if (!this.isOff() && this.noEntityTrace.getValue()) {
         if (!this.onlyPickaxe.getValue()) {
            return true;
         } else {
            return mc.player.getMainHandStack().getItem() instanceof PickaxeItem || mc.player.isUsingItem() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem);
         }
      } else {
         return false;
      }
   }

   public boolean ghostHand() {
      return this.isOn() && this.ghostHand.getValue() && !mc.options.useKey.isPressed() && !mc.options.sneakKey.isPressed();
   }
}
