package dev.G255.Lumi.mod.modules.impl.player;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class ItemAlert extends Module {
    private final BooleanSetting totemAlert = this.add(new BooleanSetting("TotemAlert", true));
    private final SliderSetting totemThreshold = this.add(new SliderSetting("TotemThreshold", 2, 1, 10, 1, totemAlert::getValue));
    private final BooleanSetting potionAlert = this.add(new BooleanSetting("PotionAlert", true));
    private final SliderSetting potionThreshold = this.add(new SliderSetting("PotionThreshold", 4, 1, 20, 1, potionAlert::getValue));
    private final BooleanSetting armorAlert = this.add(new BooleanSetting("ArmorAlert", false));
    private final SliderSetting armorThreshold = this.add(new SliderSetting("ArmorThreshold", 30, 1, 100, 1, armorAlert::getValue));
    private final BooleanSetting armorSmart = this.add(new BooleanSetting("ArmorSmart", true, armorAlert::getValue));
    private final SliderSetting armorRange = this.add(new SliderSetting("ArmorRange", 8, 1, 32, 1, () -> armorAlert.getValue() && armorSmart.getValue()));
    private final SliderSetting volume = this.add(new SliderSetting("Volume", 1.0, 0.1, 2.0, 0.1));
    private final SliderSetting cooldown = this.add(new SliderSetting("Cooldown", 5000, 0, 30000));
    private long lastTotemAlertTime, lastPotionAlertTime, lastArmorAlertTime;
    private boolean wasTotemLow, wasPotionLow, wasArmorLow;

    public ItemAlert() {
        super("ItemAlert", "Alerts when items are low", Module.Category.Player);
        this.setChinese("");
        this.setChineseDesc("");
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;
        long now = System.currentTimeMillis();
        if (totemAlert.getValue() && now - lastTotemAlertTime > cooldown.getValue()) {
            int totemCount = countItem(Items.TOTEM_OF_UNDYING);
            if (totemCount > 0 && totemCount <= totemThreshold.getValueInt() && !wasTotemLow) {
                playAlert("Low Totems: " + totemCount);
                lastTotemAlertTime = now;
            }
            wasTotemLow = totemCount <= totemThreshold.getValueInt();
        }
        if (potionAlert.getValue() && now - lastPotionAlertTime > cooldown.getValue()) {
            int potCount = countItem(Items.SPLASH_POTION) + countItem(Items.LINGERING_POTION) + countItem(Items.EXPERIENCE_BOTTLE);
            if (potCount > 0 && potCount <= potionThreshold.getValueInt() && !wasPotionLow) {
                playAlert("Low Potions: " + potCount);
                lastPotionAlertTime = now;
            }
            wasPotionLow = potCount <= potionThreshold.getValueInt();
        }
        if (armorAlert.getValue() && now - lastArmorAlertTime > cooldown.getValue()) {
            if (!armorSmart.getValue() || hasNearbyEnemy()) {
                int minDurability = 100;
                for (var stack : mc.player.getInventory().armor) {
                    if (stack.getItem() instanceof ArmorItem && stack.getMaxDamage() > 0) {
                        int dura = (int) (100.0 * (1.0 - (double) stack.getDamage() / (double) stack.getMaxDamage()));
                        if (dura < minDurability) minDurability = dura;
                    }
                }
                if (minDurability <= armorThreshold.getValueInt() && !wasArmorLow) {
                    playAlert("Low Armor: " + minDurability + "%");
                    lastArmorAlertTime = now;
                }
                wasArmorLow = minDurability <= armorThreshold.getValueInt();
            }
        }
    }

    int countItem(net.minecraft.item.Item item) {
        int count = 0;
        for (int i = 0; i < 45; i++)
            if (mc.player.getInventory().getStack(i).getItem() == item)
                count += mc.player.getInventory().getStack(i).getCount();
        return count;
    }

    boolean hasNearbyEnemy() {
        for (var entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity != mc.player
                    && entity.squaredDistanceTo(mc.player) < armorRange.getValue() * armorRange.getValue()) return true;
        }
        return false;
    }

    void playAlert(String message) {
        sendMessage(message);
        try {
            mc.world.playSound(mc.player, mc.player.getBlockPos(),
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    SoundCategory.PLAYERS, (float) volume.getValue(), 1.0f);
        } catch (Throwable ignored) {}
    }

    @Override public void onDisable() { wasTotemLow = false; wasPotionLow = false; wasArmorLow = false; }
}
