package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.Module.Category;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.impl.exploit.MaceSpoof;
import dev.kissoo.mod.modules.impl.player.InteractTweaks;
import hoprc.api.events.eventbus.EventListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

public class MaceGodModule extends Module {
    public static MaceGodModule INSTANCE;

    // ── Normal parameters ──
    private final SliderSetting attackRange = this.add(new SliderSetting("AttackRange", 4.0, 1.0, 20.0, 0.1));
    private final SliderSetting apt = this.add(new SliderSetting("APT", 1.0, 1.0, 20.0, 1.0));
    private final SliderSetting smartX = this.add(new SliderSetting("SmartXZ", 3.0, 0.0, 15.0, 0.1));
    private final SliderSetting smartUp = this.add(new SliderSetting("SmartUp", 3.0, 0.0, 15.0, 0.1));
    private final SliderSetting smartDown = this.add(new SliderSetting("SmartDown", 3.0, 0.0, 15.0, 0.1));
    private final SliderSetting smartDistance = this.add(new SliderSetting("SmartDistance", 2.0, 0.0, 15.0, 0.1));
    private final BooleanSetting normalLag = this.add(new BooleanSetting("NormalLag", true));
    private final SliderSetting normalDistance = this.add(new SliderSetting("NormalDistance", 15.0, 5.0, 50.0, 0.5, () -> this.normalLag.getValue()));
    private final SliderSetting normalDelay = this.add(new SliderSetting("NormalDelay", 1000.0, 0.0, 5000.0, 10.0, () -> this.normalLag.getValue()));
    private final BooleanSetting disableInteract = this.add(new BooleanSetting("DisableInteract", true));

    // ── SmartCobweb ──
    private final BooleanSetting smartCobweb = this.add(new BooleanSetting("SmartCobweb", false));
    private final BooleanSetting webSpoof = this.add(
        new BooleanSetting("WebSpoof", true, () -> this.smartCobweb.getValue()).setParent()
    );

    // ── WebSpoof cloned parameters (children, only visible when WebSpoof expanded) ──
    private final SliderSetting webAttackRange = this.add(
        new SliderSetting("W.AttackRange", 4.0, 1.0, 20.0, 0.1, () -> this.smartCobweb.getValue() && this.webSpoof.isOpen())
    );
    private final SliderSetting webApt = this.add(
        new SliderSetting("W.APT", 1.0, 1.0, 20.0, 1.0, () -> this.smartCobweb.getValue() && this.webSpoof.isOpen())
    );
    private final SliderSetting webSmartX = this.add(
        new SliderSetting("W.SmartXZ", 3.0, 0.0, 15.0, 0.1, () -> this.smartCobweb.getValue() && this.webSpoof.isOpen())
    );
    private final SliderSetting webSmartUp = this.add(
        new SliderSetting("W.SmartUp", 3.0, 0.0, 15.0, 0.1, () -> this.smartCobweb.getValue() && this.webSpoof.isOpen())
    );
    private final SliderSetting webSmartDown = this.add(
        new SliderSetting("W.SmartDown", 3.0, 0.0, 15.0, 0.1, () -> this.smartCobweb.getValue() && this.webSpoof.isOpen())
    );
    private final SliderSetting webSmartDistance = this.add(
        new SliderSetting("W.SmartDistance", 2.0, 0.0, 15.0, 0.1, () -> this.smartCobweb.getValue() && this.webSpoof.isOpen())
    );
    private final BooleanSetting webNormalLag = this.add(
        new BooleanSetting("W.NormalLag", true, () -> this.smartCobweb.getValue() && this.webSpoof.isOpen())
    );
    private final SliderSetting webNormalDistance = this.add(
        new SliderSetting("W.NormalDistance", 15.0, 5.0, 50.0, 0.5,
            () -> this.smartCobweb.getValue() && this.webSpoof.isOpen() && this.webNormalLag.getValue())
    );
    private final SliderSetting webNormalDelay = this.add(
        new SliderSetting("W.NormalDelay", 1000.0, 0.0, 5000.0, 10.0,
            () -> this.smartCobweb.getValue() && this.webSpoof.isOpen() && this.webNormalLag.getValue())
    );

    // ── State ──
    private boolean interactDisabledByMe = false;
    private boolean webSpoofEnabledByMe = false;
    private final Timer normalTimer = new Timer();
    private final Timer webLeaveTimer = new Timer();

    public MaceGodModule() {
        super("MaceGod", "Executes Action (SmartLag + C02) at APT frequency with periodic NormalLag.", Category.Combat);
        this.setChinese("重锤神");
        INSTANCE = this;
    }

    public void onEnable() {
        if (!nullCheck()) {
            this.normalTimer.reset();
        }
        if (this.disableInteract.getValue() && InteractTweaks.INSTANCE != null && InteractTweaks.INSTANCE.isOn()) {
            InteractTweaks.INSTANCE.disable();
            this.interactDisabledByMe = true;
        }
        this.webSpoofEnabledByMe = false;
        this.webLeaveTimer.reset();
    }

    public void onDisable() {
        if (this.interactDisabledByMe && InteractTweaks.INSTANCE != null && !InteractTweaks.INSTANCE.isOn()) {
            InteractTweaks.INSTANCE.enable();
        }
        this.interactDisabledByMe = false;
        if (this.webSpoofEnabledByMe && MaceSpoof.INSTANCE != null && MaceSpoof.INSTANCE.isOn()) {
            MaceSpoof.INSTANCE.disable();
        }
        this.webSpoofEnabledByMe = false;
        if (this.smartCobweb.getValue()) {
            this.webSpoof.setValue(true);
        }
    }

    @EventListener
    public void onTick(ClientTickEvent var1) {
        if (!var1.isPost()) {
            if (!nullCheck() && mc.player.networkHandler != null && mc.getNetworkHandler() != null) {
                // SmartCobweb: detect cobweb → auto-enable WebSpoof → enable MaceSpoof
                if (this.smartCobweb.getValue()) {
                    if (Forever.PLAYER.isInWeb(mc.player)) {
                        this.webLeaveTimer.reset();
                        if (!this.webSpoof.getValue()) {
                            this.webSpoof.setValue(true);
                        }
                        if (this.webSpoof.getValue() && MaceSpoof.INSTANCE != null && !MaceSpoof.INSTANCE.isOn()) {
                            MaceSpoof.INSTANCE.enable();
                            this.webSpoofEnabledByMe = true;
                        }
                    } else if (this.webSpoof.getValue() && this.webLeaveTimer.passedMs(500.0)) {
                        this.webSpoof.setValue(false);
                        if (this.webSpoofEnabledByMe && MaceSpoof.INSTANCE != null && MaceSpoof.INSTANCE.isOn()) {
                            MaceSpoof.INSTANCE.disable();
                            this.webSpoofEnabledByMe = false;
                        }
                    }
                }

                // Main logic — use WebSpoof-cloned params when WebSpoof active
                PlayerEntity var2 = this.findTarget();
                if (var2 != null) {
                    int var3 = (int)this.effApt();

                    for (int var4 = 0; var4 < var3; var4++) {
                        this.executeAction(var2);
                    }

                    if (this.effNormalLag() && this.normalTimer.passedMs(this.effNormalDelay())) {
                        this.normalTimer.reset();
                        this.executeNormalLag();
                    }
                }
            }
        }
    }

    // ── Effective value helpers ──

    private double effAttackRange() {
        return this.webSpoof.getValue() ? this.webAttackRange.getValue() : this.attackRange.getValue();
    }

    private double effApt() {
        return this.webSpoof.getValue() ? this.webApt.getValue() : this.apt.getValue();
    }

    private double effSmartX() {
        return this.webSpoof.getValue() ? this.webSmartX.getValue() : this.smartX.getValue();
    }

    private double effSmartUp() {
        return this.webSpoof.getValue() ? this.webSmartUp.getValue() : this.smartUp.getValue();
    }

    private double effSmartDown() {
        return this.webSpoof.getValue() ? this.webSmartDown.getValue() : this.smartDown.getValue();
    }

    private double effSmartDistance() {
        return this.webSpoof.getValue() ? this.webSmartDistance.getValue() : this.smartDistance.getValue();
    }

    private boolean effNormalLag() {
        return this.webSpoof.getValue() ? this.webNormalLag.getValue() : this.normalLag.getValue();
    }

    private double effNormalDistance() {
        return this.webSpoof.getValue() ? this.webNormalDistance.getValue() : this.normalDistance.getValue();
    }

    private double effNormalDelay() {
        return this.webSpoof.getValue() ? this.webNormalDelay.getValue() : this.normalDelay.getValue();
    }

    // ── Actions ──

    private void executeAction(PlayerEntity var1) {
        this.sendSmartLag();
        this.sendC02AttackPacket(var1);
    }

    private void sendSmartLag() {
        BlockPos var1 = null;
        double var2 = Double.MAX_VALUE;
        double var4 = mc.player.getX();
        double var6 = mc.player.getY();
        double var8 = mc.player.getZ();
        double var10 = this.effSmartX();
        double var12 = this.effSmartUp();
        double var14 = this.effSmartDown();
        double var16 = this.effSmartDistance();

        for (double var18 = var4 - var10; var18 <= var4 + var10; var18++) {
            for (double var20 = var8 - var10; var20 <= var8 + var10; var20++) {
                for (double var22 = var6 - var14; var22 <= var6 + var12; var22++) {
                    BlockPos var24 = BlockPos.ofFloored(var18, var22, var20);
                    double var25 = mc.player.squaredDistanceTo(var24.toCenterPos());
                    double var27 = mc.player.squaredDistanceTo(var24.toCenterPos().add(0.0, -0.5, 0.0));
                    if (this.canMove(var24) && var27 >= var16 * var16 && var25 < var2) {
                        var1 = var24;
                        var2 = var25;
                    }
                }
            }
        }

        if (var1 != null) {
            this.sendPosAndSetCamera((double)var1.getX() + 0.5, (double)var1.getY(), (double)var1.getZ() + 0.5);
        } else {
            this.sendPosAndSetCamera(var4, var6, var8);
        }
    }

    private void executeNormalLag() {
        double var1 = mc.player.getX() + this.effNormalDistance();
        double var3 = mc.player.getZ() + this.effNormalDistance();
        mc.getNetworkHandler().sendPacket(new PositionAndOnGround(var1, mc.player.getY(), var3, false));
        mc.player.setPosition(mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private boolean canMove(BlockPos var1) {
        return mc.world.isAir(var1) && mc.world.isAir(var1.up());
    }

    private void sendPosAndSetCamera(double var1, double var3, double var5) {
        mc.getNetworkHandler().sendPacket(new PositionAndOnGround(var1, var3, var5, false));
        mc.player.setPosition(var1, var3, var5);
    }

    private void sendC02AttackPacket(PlayerEntity var1) {
        if (var1 != null) {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(var1, mc.player.isSneaking()));
        }
    }

    private PlayerEntity findTarget() {
        PlayerEntity var1 = null;
        double var2 = this.effAttackRange();
        double var4 = var2 * var2;

        for (PlayerEntity var7 : mc.world.getPlayers()) {
            if (var7 != mc.player && var7.isAlive() && !var7.isDead() && !Forever.FRIEND.isFriend(var7)) {
                double var8 = mc.player.squaredDistanceTo(var7);
                if (var8 <= var4) {
                    var1 = var7;
                    var4 = var8;
                }
            }
        }

        return var1;
    }
}
