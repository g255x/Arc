package dev.G255.Lumi.mod.modules.impl.combat;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.Module.Category;
import dev.G255.Lumi.mod.modules.impl.player.InteractTweaks;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

/**
 * MaceGod+ï¼éé¤ç¥+ï¼?
 * å?1 tick åï¼tp å°èç©?â?æ»å» â?tp ååä½ç½®ã?
 * å©ç¨æå¡å¨ç fallDistance ç´¯ç§¯æºå¶ï¼ä½ç½®åæå°èç©ºï¼onGround=falseï¼è®©æå¡å?
 * è®¤ä¸ºç©å®¶ç¬é´ä¸å äºå·¨å¤§çé«åº¦ï¼æ­¤æ¶éé¤å»ä¸­ä¼éå¸¦æ»¡é¢å è½å æï¼éåæä½ç½®
 * æååä½ï¼åæ ?onGround=falseï¼ä¸éç½® fallDistanceï¼ï¼é¿åççæè¿èç©ºã?
 */
public class MaceGodPlus extends Module {
    public static MaceGodPlus INSTANCE;

    private final SliderSetting attackRange = this.add(new SliderSetting("AttackRange", 6.0, 1.0, 20.0, 0.1));
    private final SliderSetting voidY = this.add(new SliderSetting("VoidY", -70.0, -500.0, 320.0, 1.0));
    private final SliderSetting apt = this.add(new SliderSetting("APT", 1.0, 1.0, 20.0, 1.0));
    private final BooleanSetting autoMace = this.add(new BooleanSetting("AutoMace", true));
    private final BooleanSetting mobs = this.add(new BooleanSetting("Mobs", false));
    private final BooleanSetting disableInteract = this.add(new BooleanSetting("DisableInteract", true));

    private int prevSlot = -1;
    private boolean switchedByMe = false;
    private boolean interactDisabledByMe = false;

    public MaceGodPlus() {
        super("MaceGodPlus", "1-tick void teleport, attack, restore for massive Mace fall damage.", Category.Combat);
        this.setChinese("");
        this.setChineseDesc("");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        this.switchedByMe = false;
        this.prevSlot = -1;
        if (this.disableInteract.getValue() && InteractTweaks.INSTANCE != null && InteractTweaks.INSTANCE.isOn()) {
            InteractTweaks.INSTANCE.disable();
            this.interactDisabledByMe = true;
        }
    }

    @Override
    public void onDisable() {
        if (this.interactDisabledByMe && InteractTweaks.INSTANCE != null && !InteractTweaks.INSTANCE.isOn()) {
            InteractTweaks.INSTANCE.enable();
        }
        this.interactDisabledByMe = false;
        this.restoreSlot();
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (event.isPost()) return;
        if (nullCheck() || mc.getNetworkHandler() == null) return;

        Entity target = this.findTarget();
        if (target == null) {
            this.restoreSlot();
            return;
        }

        if (this.autoMace.getValue()) {
            this.switchToMace();
        }

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        double vy = this.voidY.getValue();

        int attacks = (int) this.apt.getValue();
        for (int i = 0; i < attacks; i++) {
            // 1) tp å°èç©ºï¼onGround=false è®©æå¡å¨ç´¯ç§¯å·¨å¤§ç?fallDistance
            this.sendPos(px, vy, pz);
            // 2) æäººï¼æ­¤æ¶æå¡å¨è®°å½ç?fallDistance å·²ææ»¡ï¼éé¤æ»¡é¢å ä¼¤
            this.attack(target);
        }
        // 3) tp ååä½ç½®ï¼åæ ?onGround=falseï¼é¿åè½å°éç½?fallDistance å¯¼è´è¢«æåè§è§?
        this.sendPos(px, py, pz);
    }

    private void attack(Entity target) {
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    private void sendPos(double x, double y, double z) {
        mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y, z, false));
    }

    private void switchToMace() {
        int maceSlot = this.findMaceSlot();
        if (maceSlot == -1) return;

        if (!this.switchedByMe) {
            this.prevSlot = mc.player.getInventory().selectedSlot;
            this.switchedByMe = true;
        }
        if (mc.player.getInventory().selectedSlot != maceSlot) {
            mc.player.getInventory().selectedSlot = maceSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
        }
    }

    private void restoreSlot() {
        if (this.switchedByMe && this.prevSlot != -1 && mc.player != null && mc.getNetworkHandler() != null) {
            if (mc.player.getInventory().selectedSlot != this.prevSlot) {
                mc.player.getInventory().selectedSlot = this.prevSlot;
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.prevSlot));
            }
        }
        this.switchedByMe = false;
        this.prevSlot = -1;
    }

    private int findMaceSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
                return i;
            }
        }
        return -1;
    }

    private Entity findTarget() {
        double range = this.attackRange.getValue();
        double rangeSq = range * range;

        // 1) ä¼åæç©å®¶ï¼éè¿æç¬¦åæ¡ä»¶çç©å®¶æ¶ï¼ä¸æ»å»æªç©
        PlayerEntity playerTarget = null;
        double playerDist = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isDead()) continue;
            if (Lumi.FRIEND.isFriend(p)) continue;

            double d = mc.player.squaredDistanceTo(p);
            if (d <= rangeSq && d < playerDist) {
                playerDist = d;
                playerTarget = p;
            }
        }
        if (playerTarget != null) return playerTarget;

        // 2) å¶æ¬¡ææªç©ï¼ä»å½å¾é?Mobs ä¸éè¿æ ç©å®¶æ?
        if (!this.mobs.getValue()) return null;

        Entity mobTarget = null;
        double mobDist = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof MobEntity mob) || e instanceof AnimalEntity) continue;
            if (!mob.isAlive() || mob.isDead()) continue;

            double d = mc.player.squaredDistanceTo(e);
            if (d <= rangeSq && d < mobDist) {
                mobDist = d;
                mobTarget = e;
            }
        }
        return mobTarget;
    }
}
