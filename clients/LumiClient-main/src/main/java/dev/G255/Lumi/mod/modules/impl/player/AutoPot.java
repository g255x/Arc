package dev.G255.Lumi.mod.modules.impl.player;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.Wrapper;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.combat.Aura;
import dev.G255.Lumi.mod.modules.settings.impl.BindSetting;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AutoPot extends Module {
    public static AutoPot INSTANCE;

    // éç¨è®¾ç½®
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 0.2, 0.0, 10.0, 0.1).setSuffix("s"));
    private final SliderSetting prePotTime = this.add(new SliderSetting("PrePotTime", 0.5, 0.0, 1.0, 0.01).setSuffix("s"));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false));
    private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", true));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));

    // å¬å±åé
    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 12.0, 0.0, 20.0, 0.1));
    private final SliderSetting health = this.add(new SliderSetting("Health", 16.0, 0.0, 36.0, 0.1));

    // éåº¦è¯æ°´ï¼æ å­éé¡¹ï¼?
    private final BooleanSetting speed = this.add(new BooleanSetting("Speed", true));

    // ææ§è¯æ°´ï¼æ å­éé¡¹ï¼?
    private final BooleanSetting resistance = this.add(new BooleanSetting("Resistance", true));

    // åéè¯æ°´ï¼æå­éé¡¹ï¼?
    private final BooleanSetting strength = this.add(new BooleanSetting("Strength", true).setParent());
    private final BooleanSetting onlyAuraStrength = this.add(new BooleanSetting("OnlyAura", true, () -> this.strength.isOpen()));

    // ----- å¼ºå¶å·è¯å¿«æ·é?-----
    private final BindSetting speedKey = this.add(new BindSetting("SpeedKey", -1));
    private final BindSetting resistanceKey = this.add(new BindSetting("ResistanceKey", -1));
    private final BindSetting strengthKey = this.add(new BindSetting("StrengthKey", -1));

    private final Timer delayTimer = new Timer();
    private boolean throwing = false;
    // ç¨äºé²æ­¢æé®éå¤è§¦å
    private boolean speedPressed = false;
    private boolean resistancePressed = false;
    private boolean strengthPressed = false;
    private final AutoPotTick autoPotTick;

    public AutoPot() {
        super("AutoPot", Module.Category.Player);
        this.setChinese("");
        INSTANCE = this;
        this.autoPotTick = new AutoPotTick();
        Lumi.EVENT_BUS.subscribe(this.autoPotTick);
    }

    // ========== å·¥å·æ¹æ³ ==========
    public static int findPotionInventorySlot(StatusEffect targetEffect) {
        for (int i = 35; i >= 0; --i) {
            ItemStack itemStack = AutoPot.mc.player.getInventory().getStack(i);
            if (Item.getRawId(itemStack.getItem()) != Item.getRawId(Items.SPLASH_POTION)) continue;
            PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                if (effect.getEffectType().value() != targetEffect) continue;
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    public static int findPotion(StatusEffect targetEffect) {
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = AutoPot.mc.player.getInventory().getStack(i);
            if (Item.getRawId(itemStack.getItem()) != Item.getRawId(Items.SPLASH_POTION)) continue;
            PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                if (effect.getEffectType().value() != targetEffect) continue;
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        this.throwing = false;
        this.speedPressed = false;
        this.resistancePressed = false;
        this.strengthPressed = false;
        Lumi.EVENT_BUS.unsubscribe(this.autoPotTick);
    }

    /**
     * å°é¢æ£æµï¼ä»ç¼çä½ç½®å°ç¢°æç®±åºé¨ä¸­å¿ä¸æ?0.1 æ ¼åå°çº¿æ£æµï¼ç¢°å°å®ä½åºä½æ¹åä¸ä¸æ¯èèç½åè¿å?true
     */
    private boolean isNearGround() {
        Vec3d eyePos = mc.player.getEyePos();
        // ç¢°æç®±åºé¨ä¸­å¿?
        Vec3d bottomCenter = mc.player.getBoundingBox().getCenter();
        bottomCenter = new Vec3d(bottomCenter.x, mc.player.getBoundingBox().minY, bottomCenter.z);
        // ååä¸?0.1 æ ¼ä½ä¸ºç»ç?
        Vec3d end = bottomCenter.add(0, -0.1, 0);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(eyePos, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            var blockState = mc.world.getBlockState(hit.getBlockPos());
            return blockState.isSolidBlock(mc.world, hit.getBlockPos()) && blockState.getBlock() != Blocks.COBWEB;
        }
        return false;
    }

    private boolean hasNearbyPlayer() {
        double rangeSq = this.targetRange.getValue() * this.targetRange.getValue();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.squaredDistanceTo(player) <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNearbyPlayerWithTotem() {
        double rangeSq = this.targetRange.getValue() * this.targetRange.getValue();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.squaredDistanceTo(player) > rangeSq) continue;
            if (player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                return true;
            }
        }
        return false;
    }

    // ---- å¼ºå¶å·è¯æ¹æ³ï¼å¿½ç¥æææ¡ä»¶ï¼ä»æ£æ¥å»¶è¿ï¼ ----
    private void forceThrowPotion(StatusEffect targetEffect) {
        if (!this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) return;
        if (this.usingPause.getValue() && mc.player.isUsingItem()) return;
        if (!EntityUtil.inInventory()) return;
        if (findPotion(targetEffect) == -1 && !(this.inventory.getValue() && findPotionInventorySlot(targetEffect) != -1)) return;
        this.throwPotion(targetEffect);
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.inventory.getValue() && !EntityUtil.inInventory()) return;
        if (!this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) return;
        // å°é¢æ£æµï¼ä»å½ onlyGround å¼å¯æ¶éè¦æ£æ?
        if (this.onlyGround.getValue() && !isNearGround()) return;

        // ----- éåº¦è¯æ°´ï¼èªå¨ï¼-----
        if (this.speed.getValue()) {
            boolean shouldThrow = false;
            StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
            if (speedEffect == null) {
                shouldThrow = true;
            } else if (speedEffect.getDuration() / 20.0 < this.prePotTime.getValue()) {
                shouldThrow = true;
            }
            float currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (shouldThrow && (!hasNearbyPlayer() || currentHealth <= this.health.getValue())) {
                shouldThrow = false;
            }
            if (shouldThrow && this.checkThrow(StatusEffects.SPEED.value())) {
                this.throwPotion(StatusEffects.SPEED.value());
                return;
            }
        }

        // ----- ææ§è¯æ°´ï¼èªå¨ï¼?----
        if (this.resistance.getValue()) {
            boolean shouldThrow = false;
            StatusEffectInstance resistanceEffect = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
            if (resistanceEffect == null) {
                shouldThrow = true;
            } else if (resistanceEffect.getAmplifier() < 2) {
                shouldThrow = true;
            } else if (resistanceEffect.getDuration() / 20.0 < this.prePotTime.getValue()) {
                shouldThrow = true;
            }
            if (shouldThrow && !hasNearbyPlayerWithTotem()) {
                shouldThrow = false;
            }
            if (shouldThrow && this.checkThrow(StatusEffects.RESISTANCE.value())) {
                this.throwPotion(StatusEffects.RESISTANCE.value());
                return;
            }
        }

        // ----- åéè¯æ°´ï¼èªå¨ï¼-----
        if (this.strength.getValue()) {
            boolean shouldThrow = false;
            StatusEffectInstance strengthEffect = mc.player.getStatusEffect(StatusEffects.STRENGTH);
            if (strengthEffect == null) {
                shouldThrow = true;
            } else if (strengthEffect.getDuration() / 20.0 < this.prePotTime.getValue()) {
                shouldThrow = true;
            }
            boolean auraOk = !this.onlyAuraStrength.getValue() || (Aura.target != null && Aura.target instanceof PlayerEntity);
            float currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (shouldThrow && (!auraOk || currentHealth <= this.health.getValue())) {
                shouldThrow = false;
            }
            if (shouldThrow && this.checkThrow(StatusEffects.STRENGTH.value())) {
                this.throwPotion(StatusEffects.STRENGTH.value());
                return;
            }
        }
    }

    public void throwPotion(StatusEffect targetEffect) {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int newSlot;
        if (this.inventory.getValue() && (newSlot = findPotionInventorySlot(targetEffect)) != -1) {
            Lumi.ROTATION.snapAt(Lumi.ROTATION.rotationYaw, 90.0f);
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Lumi.ROTATION.getLastYaw(), Lumi.ROTATION.getLastPitch()));
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            Lumi.ROTATION.snapBack();
            this.delayTimer.reset();
        } else if ((newSlot = findPotion(targetEffect)) != -1) {
            Lumi.ROTATION.snapAt(Lumi.ROTATION.rotationYaw, 90.0f);
            InventoryUtil.switchToSlot(newSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Lumi.ROTATION.getLastYaw(), Lumi.ROTATION.getLastPitch()));
            InventoryUtil.switchToSlot(oldSlot);
            Lumi.ROTATION.snapBack();
            this.delayTimer.reset();
        }
    }

    public boolean isThrow() {
        return this.throwing;
    }

    public boolean checkThrow(StatusEffect targetEffect) {
        if (!EntityUtil.inInventory()) return false;
        if (this.usingPause.getValue() && mc.player.isUsingItem()) return false;
        return findPotion(targetEffect) != -1 || (this.inventory.getValue() && findPotionInventorySlot(targetEffect) != -1);
    }

    // åé¨ç±»ï¼å¤çæé®å¼ºå¶å·è¯
    public class AutoPotTick {
        @EventListener
        public void onTick(ClientTickEvent event) {
            if (event.isPost() || Module.nullCheck()) return;
            if (Wrapper.mc.currentScreen != null) return;

            // éåº¦æé®
            if (AutoPot.this.speedKey.isPressed()) {
                if (!AutoPot.this.speedPressed) {
                    AutoPot.this.speedPressed = true;
                    AutoPot.this.forceThrowPotion(StatusEffects.SPEED.value());
                }
            } else {
                AutoPot.this.speedPressed = false;
            }

            // ææ§æé?
            if (AutoPot.this.resistanceKey.isPressed()) {
                if (!AutoPot.this.resistancePressed) {
                    AutoPot.this.resistancePressed = true;
                    AutoPot.this.forceThrowPotion(StatusEffects.RESISTANCE.value());
                }
            } else {
                AutoPot.this.resistancePressed = false;
            }

            // åéæé®
            if (AutoPot.this.strengthKey.isPressed()) {
                if (!AutoPot.this.strengthPressed) {
                    AutoPot.this.strengthPressed = true;
                    AutoPot.this.forceThrowPotion(StatusEffects.STRENGTH.value());
                }
            } else {
                AutoPot.this.strengthPressed = false;
            }
        }
    }
}