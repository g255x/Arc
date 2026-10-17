package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.Module.Category;
import dev.kissoo.mod.modules.impl.player.InteractTweaks;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import hoprc.api.events.eventbus.EventListener;
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
 * MaceGod+（重锤神+）
 * 在 1 tick 内：tp 到虚空 → 攻击 → tp 回原位置。
 * 利用服务器的 fallDistance 累积机制：位置包掉到虚空（onGround=false）让服务器
 * 认为玩家瞬间下坠了巨大的高度，此时重锤击中会附带满额坠落加成；随后把位置
 * 拉回原位（同样 onGround=false，不重置 fallDistance），避免真的掉进虚空。
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
        this.setChinese("重锤神+");
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
            // 1) tp 到虚空：onGround=false 让服务器累积巨大的 fallDistance
            this.sendPos(px, vy, pz);
            // 2) 打人：此时服务器记录的 fallDistance 已拉满，重锤满额加伤
            this.attack(target);
        }
        // 3) tp 回原位置：同样 onGround=false，避免落地重置 fallDistance 导致被拉回视觉
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

        // 1) 优先打玩家：附近有符合条件的玩家时，不攻击怪物
        PlayerEntity playerTarget = null;
        double playerDist = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isDead()) continue;
            if (Forever.FRIEND.isFriend(p)) continue;

            double d = mc.player.squaredDistanceTo(p);
            if (d <= rangeSq && d < playerDist) {
                playerDist = d;
                playerTarget = p;
            }
        }
        if (playerTarget != null) return playerTarget;

        // 2) 其次打怪物：仅当勾选 Mobs 且附近无玩家时
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
