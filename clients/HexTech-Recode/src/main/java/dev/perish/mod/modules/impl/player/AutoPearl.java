package dev.perish.mod.modules.impl.player;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.RotationEvent;
import dev.perish.api.events.impl.TickEvent;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.exploit.Blink;
import dev.perish.mod.modules.impl.movement.ElytraFly;
import dev.perish.mod.modules.impl.movement.Velocity;
import dev.perish.mod.modules.settings.enums.SwingSide;
import dev.perish.mod.modules.settings.enums.Timing;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;

public class AutoPearl extends Module {
    public static AutoPearl INSTANCE;
    public static boolean throwing = false;
    public final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    public final EnumSetting<SwingSide> interactSwing = this.add(new EnumSetting("Swing", SwingSide.All));
    private final EnumSetting<Timing> timing = this.add(new EnumSetting("Timing", Timing.All));
    private final BooleanSetting rotation = this.add(new BooleanSetting("Rotation", true));
    private final BooleanSetting yawStep = this.add(new BooleanSetting("YawStep", false).setParent());
    private final BooleanSetting whenElytra = this.add(new BooleanSetting("FallFlying", true, this.yawStep::isOpen));
    private final SliderSetting steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, this.yawStep::isOpen));
    private final SliderSetting fov = this.add(new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, this.yawStep::isOpen));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 100, 0, 100, this.yawStep::isOpen));
    private final BooleanSetting sync = this.add(new BooleanSetting("Sync", true, this.yawStep::isOpen));

    public AutoPearl() {
        super("AutoPearl", Module.Category.Player);
        this.setChinese("扔珍珠");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            this.disable();
        } else {
            if (INSTANCE.inventory.getValue()) {
                if (InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL) == -1) {
                    this.disable();
                    return;
                }
            } else if (InventoryUtil.findItem(Items.ENDER_PEARL) == -1) {
                this.disable();
                return;
            }

            if (!this.shouldYawStep()) {
                if (!this.inventory.getValue() || EntityUtil.inInventory()) {
                    if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                        this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
                        this.disable();
                    }
                }
            }
        }
    }

    @EventListener
    public void onUpdate(TickEvent event) {
        if (!nullCheck()) {
            if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
                if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                    if (!this.shouldYawStep()) {
                        this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
                        this.disable();
                    } else if (Perish.ROTATION.inFov(mc.player.getYaw(), mc.player.getPitch(), this.fov.getValueFloat())) {
                        if (this.sync.getValue()) {
                            this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
                        } else {
                            throwing = true;
                            if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
                                sendSequencedPacket(
                                        id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Perish.ROTATION.getLastYaw(), Perish.ROTATION.getLastPitch())
                                );
                                EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
                            } else {
                                int pearl;
                                if (this.inventory.getValue() && (pearl = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL)) != -1) {
                                    InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                                    sendSequencedPacket(
                                            id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Perish.ROTATION.getLastYaw(), Perish.ROTATION.getLastPitch())
                                    );
                                    EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
                                    InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                                    EntityUtil.syncInventory();
                                } else if ((pearl = InventoryUtil.findItem(Items.ENDER_PEARL)) != -1) {
                                    int old = mc.player.getInventory().selectedSlot;
                                    InventoryUtil.switchToSlot(pearl);
                                    sendSequencedPacket(
                                            id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Perish.ROTATION.getLastYaw(), Perish.ROTATION.getLastPitch())
                                    );
                                    EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
                                    InventoryUtil.switchToSlot(old);
                                }
                            }

                            throwing = false;
                        }

                        this.disable();
                    }
                }
            }
        }
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (this.shouldYawStep()) {
            event.setRotation(mc.player.getYaw(), mc.player.getPitch(), this.steps.getValueFloat(), this.priority.getValueFloat());
        }
    }

    private boolean shouldYawStep() {
        return (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())) && this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
    }

    public void throwPearl(float yaw, float pitch) {
        throwing = true;
        if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
            if (this.rotation.getValue()) {
                Perish.ROTATION.snapAt(yaw, pitch);
            }

            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
            if (this.rotation.getValue()) {
                Perish.ROTATION.snapBack();
            }
        } else {
            int pearl;
            if (this.inventory.getValue() && (pearl = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL)) != -1) {
                InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                if (this.rotation.getValue()) {
                    Perish.ROTATION.snapAt(yaw, pitch);
                }

                sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
                EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
                InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                EntityUtil.syncInventory();
                if (this.rotation.getValue()) {
                    Perish.ROTATION.snapBack();
                }
            } else if ((pearl = InventoryUtil.findItem(Items.ENDER_PEARL)) != -1) {
                int old = mc.player.getInventory().selectedSlot;
                InventoryUtil.switchToSlot(pearl);
                if (this.rotation.getValue()) {
                    Perish.ROTATION.snapAt(yaw, pitch);
                }

                sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
                EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
                InventoryUtil.switchToSlot(old);
                if (this.rotation.getValue()) {
                    Perish.ROTATION.snapBack();
                }
            }
        }

        throwing = false;
    }
}
