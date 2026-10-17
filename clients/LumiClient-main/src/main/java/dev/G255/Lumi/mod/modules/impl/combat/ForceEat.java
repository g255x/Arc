package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.PacketEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class ForceEat extends Module {
    public static ForceEat INSTANCE;

    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Both));
    private final BooleanSetting autoSwitch = this.add(new BooleanSetting("AutoSwitch", true));
    private final BooleanSetting noInteract = this.add(new BooleanSetting("NoInteract", true));

    private boolean active = false;
    private boolean pendingPause = false;
    private int eatProgress = 0;
    private int delay = 0;
    private int prevSlot = -1;

    public ForceEat() {
        super("ForceEat", Category.Combat);
        this.setChinese("");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        prevSlot = mc.player.getInventory().selectedSlot;
        int appleSlot = findAppleSlot();
        if (appleSlot == -1) {
            disable();
            return;
        }
        if (autoSwitch.getValue()) {
            InventoryUtil.switchToSlot(appleSlot);
        }
        active = false;
        pendingPause = false;
        eatProgress = 0;
        delay = 1;
    }

    @Override
    public void onDisable() {
        // åéæ¾å³é®ï¼é¿åç²æ»
        mc.options.useKey.setPressed(false);
        if (nullCheck()) return;
        if (prevSlot != -1 && prevSlot < 9 && autoSwitch.getValue()) {
            InventoryUtil.switchToSlot(prevSlot);
        }
        active = false;
        pendingPause = false;
        eatProgress = 0;
        delay = 0;
        prevSlot = -1;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!isOn() || nullCheck()) return;

        if (delay > 0) {
            delay--;
            return;
        }

        // ï¿?tick èªå¨ç¡®ä¿ä¸»æææè¹æï¼å¦ï¿?autoSwitch å¼å¯ï¼
        if (autoSwitch.getValue() && !isAppleInHand()) {
            int slot = findAppleSlot();
            if (slot != -1) {
                InventoryUtil.switchToSlot(slot);
                pendingPause = false;
                active = false;
                eatProgress = 0;
                mc.options.useKey.setPressed(false);
            } else {
                disable();
                return;
            }
        }

        if (!active) {
            mc.options.useKey.setPressed(true);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            pendingPause = true;
            active = true;
            eatProgress = 0;
            return;
        }

        if (pendingPause) {
            if (mc.player.isUsingItem()) {
                pendingPause = false;
            }
            mc.options.useKey.setPressed(true);
            return;
        }

        mc.options.useKey.setPressed(true);
        eatProgress++;

        if (eatProgress >= 32) {
            if (!isAppleInHand()) {
                // åå¤ï¼æä¸­æ²¡æè¹æï¼å°è¯åæ¢
                if (autoSwitch.getValue()) {
                    int nextSlot = findAppleSlot();
                    if (nextSlot != -1) {
                        InventoryUtil.switchToSlot(nextSlot);
                        pendingPause = false;
                        eatProgress = 0;
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    } else {
                        disable();
                    }
                } else {
                    disable();
                }
            } else {
                eatProgress = 0;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (!isOn() || nullCheck() || !active) return;
        mc.options.useKey.setPressed(true);
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (!isOn() || nullCheck() || !active) return;
        Packet<?> packet = event.getPacket();
        if (noInteract.getValue()) {
            if (packet instanceof PlayerInteractItemC2SPacket && ((PlayerInteractItemC2SPacket) packet).getHand() != Hand.MAIN_HAND) {
                event.cancel();
            }
            if (packet instanceof PlayerActionC2SPacket) {
                event.cancel();
            }
        }
    }

    private int findAppleSlot() {
        return switch (mode.getValue()) {
            case GoldenApple -> InventoryUtil.findItem(Items.GOLDEN_APPLE);
            case EnchantedGoldenApple -> InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
            case Both -> {
                int slot = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
                if (slot == -1) slot = InventoryUtil.findItem(Items.GOLDEN_APPLE);
                yield slot;
            }
        };
    }

    private boolean isAppleInHand() {
        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return false;
        return switch (mode.getValue()) {
            case GoldenApple -> stack.getItem() == Items.GOLDEN_APPLE;
            case EnchantedGoldenApple -> stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
            case Both -> stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
        };
    }

    public enum Mode {
        GoldenApple("Gapple"),
        EnchantedGoldenApple("EnchantedGapple"),
        Both("Both");
        private final String name;
        Mode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
}