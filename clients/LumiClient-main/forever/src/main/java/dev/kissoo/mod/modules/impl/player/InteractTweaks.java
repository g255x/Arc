package dev.kissoo.mod.modules.impl.player;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;

public class InteractTweaks extends Module {
   public static InteractTweaks INSTANCE;
   public final BooleanSetting noEntityTrace = this.add(new BooleanSetting("NoEntityTrace", true).setParent());
   public final BooleanSetting onlyPickaxe = this.add(new BooleanSetting("OnlyPickaxe", true, this.noEntityTrace::isOpen));
   public final BooleanSetting multiTask = this.add(new BooleanSetting("MultiTask", true));
   public final BooleanSetting respawn = this.add(new BooleanSetting("Respawn", true));
   public final BooleanSetting ghostHand = this.add(new BooleanSetting("IgnoreBedrock", false));
   private final BooleanSetting noAbort = this.add(new BooleanSetting("NoMineAbort", false));
   private final BooleanSetting noReset = this.add(new BooleanSetting("NoMineReset", false));
   private final BooleanSetting noDelay = this.add(new BooleanSetting("NoMineDelay", false));
   private final BooleanSetting noInteract = this.add(new BooleanSetting("NoInteract", false));
   public final BooleanSetting pickaxeSwitch = this.add(new BooleanSetting("SwitchEat", false).setParent());
   public final BooleanSetting allowSword = this.add(new BooleanSetting("Sword", true, this.pickaxeSwitch::isOpen));
   public final BooleanSetting allowPickaxe = this.add(new BooleanSetting("Pickaxe", true, this.pickaxeSwitch::isOpen));
   public final BooleanSetting allowCrystal = this.add(new BooleanSetting("Crystal", true, this.pickaxeSwitch::isOpen));
   public final BooleanSetting allowTotem = this.add(new BooleanSetting("Totem", true, this.pickaxeSwitch::isOpen));
   private final BooleanSetting reach = this.add(new BooleanSetting("Reach", false));
   public final SliderSetting blockRange = this.add(new SliderSetting("BlockRange", 5.0, 0.0, 15.0, 0.1, this.reach::getValue));
   public final SliderSetting entityRange = this.add(new SliderSetting("EntityRange", 5.0, 0.0, 15.0, 0.1, this.reach::getValue));
   private final SliderSetting delay = this.add(new SliderSetting("UseDelay", 4.0, 0.0, 4.0, 1.0));

   private final BooleanSetting autoGapple = this.add(new BooleanSetting("AutoGapple", false).setParent());
   private final SliderSetting gappleHealth = this.add(new SliderSetting("Health", 10.0, 1.0, 20.0, 0.5, this.autoGapple::isOpen));
   private final BooleanSetting gappleOffhand = this.add(new BooleanSetting("Offhand", true, this.autoGapple::isOpen));
   private final SliderSetting gappleCooldown = this.add(new SliderSetting("Cooldown", 20, 0, 60, 1, this.autoGapple::isOpen).setSuffix("tick"));
   private final SliderSetting gappleTimeout = this.add(new SliderSetting("Timeout", 60, 30, 100, 1, this.autoGapple::isOpen).setSuffix("tick"));
   private final SliderSetting effectWait = this.add(new SliderSetting("EffectWait", 5, 0, 20, 1, this.autoGapple::isOpen).setSuffix("tick"));

   public boolean isActive;
   private boolean switchSwapped = false;
   private int switchLastSlot = 0;
   private boolean isEating = false;
   private boolean waitingForEffect = false;
   private int originalSlot = -1;
   private boolean wasPressingUse = false;
   private int cooldownTicks = 0;
   private int eatTicks = 0;
   private int waitTicks = 0;
   private boolean hadAbsorption = false;
   private float absorptionBefore = 0.0f;

   public InteractTweaks() {
      super("InteractTweaks", Module.Category.Player);
      this.setChinese("交互调整");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (nullCheck()) return;

      if (cooldownTicks > 0) {
         cooldownTicks--;
      }
      if (this.respawn.getValue() && mc.currentScreen instanceof DeathScreen) {
         mc.player.requestRespawn();
         mc.setScreen(null);
      }
      if (mc.itemUseCooldown <= 4 - this.delay.getValueInt()) {
         mc.itemUseCooldown = 0;
      }
      if (handleAutoGapple()) {
         return;
      }
      handleSwitchEat();
   }


   private boolean handleAutoGapple() {
      if (!autoGapple.getValue()) {
         if (isEating || waitingForEffect) {
            finishEating(false);
         }
         return false;
      }

      if (waitingForEffect) {
         waitTicks++;
         if (waitTicks >= effectWait.getValueInt()) {
            waitingForEffect = false;
            float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (health <= gappleHealth.getValue() && hasGapple()) {
               startEating();
               return true;
            } else {
               finishEating(true);
               return false;
            }
         }
         return true;
      }

      if (isEating) {
         continueEating();
         return true;
      }

      if (cooldownTicks > 0) {
         return false;
      }

      float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
      if (health <= gappleHealth.getValue() && hasGapple()) {
         return startEating();
      }

      return false;
   }

   private boolean startEating() {
      hadAbsorption = mc.player.hasStatusEffect(StatusEffects.ABSORPTION);
      absorptionBefore = mc.player.getAbsorptionAmount();

      wasPressingUse = mc.options.useKey.isPressed();
      eatTicks = 0;
      if (gappleOffhand.getValue() && isGapple(mc.player.getOffHandStack().getItem())) {
         isEating = true;
         originalSlot = -1;
         mc.options.useKey.setPressed(true);
         mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
         return true;
      }
      if (isGapple(mc.player.getMainHandStack().getItem())) {
         isEating = true;
         originalSlot = -1;
         mc.options.useKey.setPressed(true);
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         return true;
      }
      int gappleSlot = findGappleSlot();
      if (gappleSlot == -1) {
         return false;
      }

      originalSlot = mc.player.getInventory().selectedSlot;
      InventoryUtil.switchToSlot(gappleSlot);

      isEating = true;
      mc.options.useKey.setPressed(true);
      mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      return true;
   }

   private void continueEating() {
      eatTicks++;
      if (checkEatFinished()) {
         isEating = false;
         waitingForEffect = true;
         waitTicks = 0;
         mc.options.useKey.setPressed(false);
         if (originalSlot != -1 && originalSlot != mc.player.getInventory().selectedSlot) {
            InventoryUtil.switchToSlot(originalSlot);
            originalSlot = -1;
         }
         return;
      }

      if (eatTicks >= gappleTimeout.getValueInt()) {
         finishEating(true);
         return;
      }

      boolean mainHandGapple = isGapple(mc.player.getMainHandStack().getItem());
      boolean offHandGapple = gappleOffhand.getValue() && isGapple(mc.player.getOffHandStack().getItem());

      if (!mainHandGapple && !offHandGapple) {
         int gappleSlot = findGappleSlot();
         if (gappleSlot == -1) {
            finishEating(false);
            return;
         }
         InventoryUtil.switchToSlot(gappleSlot);
      }

      mc.options.useKey.setPressed(true);

      if (!mc.player.isUsingItem()) {
         Hand hand = offHandGapple ? Hand.OFF_HAND : Hand.MAIN_HAND;
         mc.interactionManager.interactItem(mc.player, hand);
      }
   }

   private boolean checkEatFinished() {
      if (eatTicks < 10) {
         return false;
      }

      boolean hasAbsorption = mc.player.hasStatusEffect(StatusEffects.ABSORPTION);
      float currentAbsorption = mc.player.getAbsorptionAmount();

      if (!hadAbsorption && hasAbsorption) {
         return true;
      }

      if (hadAbsorption && hasAbsorption) {
         if (currentAbsorption > absorptionBefore) {
            return true;
         }
      }

      return false;
   }

   private void finishEating(boolean setCooldown) {
      if (setCooldown) {
         cooldownTicks = gappleCooldown.getValueInt();
      }

      if (originalSlot != -1 && mc.player != null && originalSlot != mc.player.getInventory().selectedSlot) {
         InventoryUtil.switchToSlot(originalSlot);
      }
      if (!wasPressingUse) {
         mc.options.useKey.setPressed(false);
      }
      isEating = false;
      waitingForEffect = false;
      originalSlot = -1;
      wasPressingUse = false;
      eatTicks = 0;
      waitTicks = 0;
      hadAbsorption = false;
      absorptionBefore = 0.0f;
   }

   private int findGappleSlot() {
      int slot = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
      if (slot == -1) {
         slot = InventoryUtil.findItem(Items.GOLDEN_APPLE);
      }
      return slot;
   }

   private boolean isGapple(net.minecraft.item.Item item) {
      return item == Items.ENCHANTED_GOLDEN_APPLE || item == Items.GOLDEN_APPLE;
   }

   private boolean hasGapple() {
      if (gappleOffhand.getValue() && isGapple(mc.player.getOffHandStack().getItem())) return true;
      if (isGapple(mc.player.getMainHandStack().getItem())) return true;
      return findGappleSlot() != -1;
   }

   private void handleSwitchEat() {
      if (!this.pickaxeSwitch.getValue()) {
         if (switchSwapped) {
            InventoryUtil.switchToSlot(this.switchLastSlot);
            switchSwapped = false;
         }
         return;
      }

      if (switchSwapped) {
         if (mc.options.useKey.isPressed()) {
            if (!isGapple(mc.player.getMainHandStack().getItem())) {
               int gappleSlot = findGappleSlot();
               if (gappleSlot != -1) {
                  InventoryUtil.switchToSlot(gappleSlot);
               } else {
                  InventoryUtil.switchToSlot(this.switchLastSlot);
                  switchSwapped = false;
               }
            }
            return;
         } else {
            InventoryUtil.switchToSlot(this.switchLastSlot);
            switchSwapped = false;
            return;
         }
      }

      var mainItem = mc.player.getMainHandStack().getItem();

      boolean canSwitch = (mainItem instanceof PickaxeItem && this.allowPickaxe.getValue())
              || (mainItem instanceof SwordItem && this.allowSword.getValue())
              || (mainItem == Items.END_CRYSTAL && this.allowCrystal.getValue())
              || (mainItem == Items.TOTEM_OF_UNDYING && this.allowTotem.getValue());

      if (!canSwitch || !mc.options.useKey.isPressed()) {
         return;
      }

      if (isGapple(mc.player.getOffHandStack().getItem())) {
         return;
      }

      int gappleSlot = findGappleSlot();
      if (gappleSlot == -1) {
         return;
      }

      this.switchLastSlot = mc.player.getInventory().selectedSlot;
      switchSwapped = true;
      InventoryUtil.switchToSlot(gappleSlot);
   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      if (nullCheck()) return;

      if (this.noInteract.getValue() && event.getPacket() instanceof PlayerInteractBlockC2SPacket packet) {
         Block block = mc.world.getBlockState(packet.getBlockHitResult().getBlockPos()).getBlock();
         if (!mc.player.isSneaking() && (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof AnvilBlock)) {
            event.cancel();
         }
      }
   }

   @Override
   public void onDisable() {
      this.isActive = false;

      if (isEating || waitingForEffect) {
         finishEating(false);
      }

      if (switchSwapped && mc.player != null) {
         InventoryUtil.switchToSlot(this.switchLastSlot);
         switchSwapped = false;
      }

      cooldownTicks = 0;
   }

   @Override
   public String getInfo() {
      if (isEating) {
         return "Eating " + eatTicks + "t";
      }
      if (waitingForEffect) {
         return "Wait " + waitTicks + "/" + effectWait.getValueInt();
      }
      if (cooldownTicks > 0) {
         return "CD:" + cooldownTicks;
      }
      return null;
   }

   public boolean shouldPauseCombat() {
      if (!this.isOn()) {
         return false;
      }
      boolean autoGapplePause = this.autoGapple.getValue() && (isEating || waitingForEffect);
      boolean switchEatPause = this.pickaxeSwitch.getValue() && this.switchSwapped;

      return autoGapplePause || switchEatPause;
   }

   public boolean isAutoEating() {
      return isEating || waitingForEffect;
   }

   public int getCooldownTicks() {
      return cooldownTicks;
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
      if (this.isOff() || !this.noEntityTrace.getValue()) {
         return false;
      }
      if (!this.onlyPickaxe.getValue()) {
         return true;
      }
      var mainItem = mc.player.getMainHandStack().getItem();
      return mainItem instanceof PickaxeItem
              || (mc.player.isUsingItem() && !(mainItem instanceof SwordItem));
   }

   public boolean ghostHand() {
      return this.isOn() && this.ghostHand.getValue()
              && !mc.options.useKey.isPressed() && !mc.options.sneakKey.isPressed();
   }
}