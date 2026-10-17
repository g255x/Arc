package dev.perish.mod.modules.impl.player;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClientTickEvent;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.InventoryUtil;
import dev.perish.api.utils.world.BlockPosX;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.BindSetting;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.component.DataComponentTypes;

public class AutoPot extends Module {
    public static AutoPot INSTANCE;
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 5.0, 0.0, 10.0, 0.1).setSuffix("s"));
    private final BooleanSetting speed = this.add(new BooleanSetting("Speed", false));
    private final BooleanSetting resistance = this.add(new BooleanSetting("Resistance", false));
    private final BooleanSetting strength = this.add(new BooleanSetting("Strength", false));
    private final BooleanSetting slowFalling = this.add(new BooleanSetting("SlowFalling", false));
    private final SliderSetting frontTick = this.add(new SliderSetting("FrontTick", 20.0, 1.0, 100.0, 1.0).setSuffix("ticks"));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting checkPlayer = this.add(new BooleanSetting("CheckPlayer", false));
    private final SliderSetting playerRange = this.add(new SliderSetting("Range", 10.0, 1.0, 50.0, 1.0).setSuffix("m"));
    private final BindSetting speedKey = this.add(new BindSetting("SpeedKey", -1));
    private final BindSetting strengthKey = this.add(new BindSetting("StrengthKey", -1));
    private final BindSetting resistanceKey = this.add(new BindSetting("ResistanceKey", -1));
    private final Timer delayTimer = new Timer();
    private boolean throwing = false;
    private boolean turtlePress;
    private boolean speedPress;
    private boolean strengthPress;

    public AutoPot() {
        super("AutoPot", Module.Category.Player);
        this.setChinese("自动药水");
        INSTANCE = this;
        Perish.EVENT_BUS.subscribe(new AutoPot.AutoPotTick());
    }

    public static int findPotionInventorySlot(StatusEffect targetEffect) {
        for (int i = 35; i >= 0; i--) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
                PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(
                        DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT
                );

                for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                    if (effect.getEffectType().value() == targetEffect) {
                        return i < 9 ? i + 36 : i;
                    }
                }
            }
        }

        return -1;
    }

    public static int findPotion(StatusEffect targetEffect) {
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
                PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(
                        DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT
                );

                for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                    if (effect.getEffectType().value() == targetEffect) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    @Override
    public void onDisable() {
        this.throwing = false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (dev.perish.mod.modules.impl.combat.AutoMend.INSTANCE != null && dev.perish.mod.modules.impl.combat.AutoMend.INSTANCE.isOn() && dev.perish.mod.modules.impl.combat.AutoMend.INSTANCE.isThrow()) {
            return;
        }

        if (!this.inventory.getValue() || EntityUtil.inInventory()) {
            if (this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) {
                if (!this.onlyGround.getValue()
                        || mc.player.isInsideWall()
                        || (mc.player.isOnGround() || Perish.PLAYER.isInWeb(mc.player))
                        && !mc.world.isAir(new BlockPosX(mc.player.getPos().add(0.0, -1.0, 0.0)))) {
                    if (this.resistance.getValue()
                            && (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2)) {
                        this.throwing = this.checkThrow(StatusEffects.RESISTANCE.value());
                        if (this.isThrow()) {
                            if (!this.checkPlayer.getValue() || this.isPlayerInRange()) {
                                this.throwPotion(StatusEffects.RESISTANCE.value());
                                return;
                            }
                        }
                    }

                    if (this.speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                        this.throwing = this.checkThrow(StatusEffects.SPEED.value());
                        if (this.isThrow()) {
                            if (!this.checkPlayer.getValue() || this.isPlayerInRange()) {
                                this.throwPotion(StatusEffects.SPEED.value());
                                return;
                            }
                        }
                    }

                    if (this.strength.getValue() && !mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
                        this.throwing = this.checkThrow(StatusEffects.STRENGTH.value());
                        if (this.isThrow()) {
                            if (!this.checkPlayer.getValue() || this.isPlayerInRange()) {
                                this.throwPotion(StatusEffects.STRENGTH.value());
                                return;
                            }
                        }
                    }

                    if (this.slowFalling.getValue() && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                        this.throwing = this.checkThrow(StatusEffects.SLOW_FALLING.value());
                        if (this.isThrow()) {
                            if (!this.checkPlayer.getValue() || this.isPlayerInRange()) {
                                this.throwPotion(StatusEffects.SLOW_FALLING.value());
                                return;
                            }
                        }
                    }

                    // Turtle potion logic - refresh when effects are about to expire
                    {
                        StatusEffectInstance resistanceEffect = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
                        StatusEffectInstance slownessEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);

                        // Check if both resistance and slowness effects are present (turtle potion characteristics)
                        if (resistanceEffect != null && slownessEffect != null) {
                            // If either effect is about to expire based on FrontTick setting
                            if (resistanceEffect.getDuration() <= frontTick.getValue() || slownessEffect.getDuration() <= frontTick.getValue()) {
                                this.throwing = this.checkThrow(StatusEffects.RESISTANCE.value());
                                if (this.isThrow() && this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) {
                                    if (!this.checkPlayer.getValue() || this.isPlayerInRange()) {
                                        this.throwPotion(StatusEffects.RESISTANCE.value());
                                        return;
                                    }
                                }
                            }
                        } else if (resistanceEffect == null && slownessEffect == null) {
                            // If no turtle potion effects at all, try to use turtle potion
                            this.throwing = this.checkThrow(StatusEffects.RESISTANCE.value());
                            if (this.isThrow() && this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) {
                                if (!this.checkPlayer.getValue() || this.isPlayerInRange()) {
                                    this.throwPotion(StatusEffects.RESISTANCE.value());
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void throwPotion(StatusEffect targetEffect) {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int newSlot;
        if (this.inventory.getValue() && (newSlot = findPotionInventorySlot(targetEffect)) != -1) {
            Perish.ROTATION.snapAt(Perish.ROTATION.rotationYaw, 90.0F);
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Perish.ROTATION.getLastYaw(), Perish.ROTATION.getLastPitch()));
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            Perish.ROTATION.snapBack();
            this.delayTimer.reset();
        } else if ((newSlot = findPotion(targetEffect)) != -1) {
            Perish.ROTATION.snapAt(Perish.ROTATION.rotationYaw, 90.0F);
            InventoryUtil.switchToSlot(newSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Perish.ROTATION.getLastYaw(), Perish.ROTATION.getLastPitch()));
            InventoryUtil.switchToSlot(oldSlot);
            Perish.ROTATION.snapBack();
            this.delayTimer.reset();
        }
    }

    public boolean isThrow() {
        return this.throwing;
    }

    public boolean checkThrow(StatusEffect targetEffect) {
        if (!EntityUtil.inInventory()) {
            return false;
        } else {
            return (!this.usingPause.getValue() || !mc.player.isUsingItem()) && (findPotion(targetEffect) != -1 || this.inventory.getValue() && findPotionInventorySlot(targetEffect) != -1);
        }
    }

    private boolean isPlayerInRange() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && mc.player.distanceTo(player) <= playerRange.getValue() && player.getDisplayName() != null && !Perish.FRIEND.isFriend(player)) {
                return true;
            }
        }
        return false;
    }

    public class AutoPotTick {
        @EventListener
        public void onTick(ClientTickEvent event) {
            if (!Module.nullCheck() && !event.isPost()) {
                if (!AutoPot.this.inventory.getValue() || EntityUtil.inInventory()) {
                    if (Wrapper.mc.currentScreen == null) {
                        if (AutoPot.this.resistanceKey.isPressed()) {
                            if (!AutoPot.this.turtlePress && AutoPot.this.checkThrow(StatusEffects.RESISTANCE.value())) {
                                if (!AutoPot.this.checkPlayer.getValue() || AutoPot.this.isPlayerInRange()) {
                                    AutoPot.this.throwPotion(StatusEffects.RESISTANCE.value());
                                    AutoPot.this.turtlePress = true;
                                    return;
                                }
                            }
                        } else {
                            AutoPot.this.turtlePress = false;
                        }

                        if (AutoPot.this.strengthKey.isPressed()) {
                            if (!AutoPot.this.strengthPress && AutoPot.this.checkThrow(StatusEffects.STRENGTH.value())) {
                                if (!AutoPot.this.checkPlayer.getValue() || AutoPot.this.isPlayerInRange()) {
                                    AutoPot.this.throwPotion(StatusEffects.STRENGTH.value());
                                    AutoPot.this.strengthPress = true;
                                    return;
                                }
                            }
                        } else {
                            AutoPot.this.strengthPress = false;
                        }

                        if (AutoPot.this.speedKey.isPressed()) {
                            if (!AutoPot.this.speedPress && AutoPot.this.checkThrow(StatusEffects.SPEED.value())) {
                                if (!AutoPot.this.checkPlayer.getValue() || AutoPot.this.isPlayerInRange()) {
                                    AutoPot.this.throwPotion(StatusEffects.SPEED.value());
                                    AutoPot.this.speedPress = true;
                                }
                            }
                        } else {
                            AutoPot.this.speedPress = false;
                        }
                    } else {
                        AutoPot.this.speedPress = false;
                        AutoPot.this.turtlePress = false;
                        AutoPot.this.strengthPress = false;
                    }
                }
            }
        }
    }
}