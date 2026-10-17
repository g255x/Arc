package dev.kissoo.mod.modules.impl.movement;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.TravelEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.events.impl.UpdateRotateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.player.MovementUtil;
import hoprc.asm.accessors.IFireworkRocketEntity;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.impl.client.BaritoneModule;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.path.IPathExecutor;

import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;

public class ElytraFlight extends Module {
    public static ElytraFlight INSTANCE;

    public final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Grim));
    public enum Mode { Grim, NCP }
    private final SliderSetting fireWorkDelay = this.add(new SliderSetting("FireWorkDelay", 1500.0, 200.0, 10000.0, 50.0, () -> mode.is(Mode.Grim)).setSuffix("ms"));
    private final SliderSetting packetDelay = this.add(new SliderSetting("PacketDelay", 0.0, 0.0, 20.0, 1.0, () -> mode.is(Mode.Grim)));
    private final BooleanSetting noAnimation = this.add(new BooleanSetting("NoAnimation", false, () -> mode.is(Mode.Grim)));
    private final BooleanSetting setFlag = this.add(new BooleanSetting("SetFlag", false, () -> mode.is(Mode.Grim)));
    private final SliderSetting defaultPitch = this.add(new SliderSetting("DefaultPitch", -10.0, -90.0, 90.0, 1.0, () -> mode.is(Mode.Grim)).setSuffix("°"));
    private final SliderSetting upPitch = this.add(new SliderSetting("UpPitch", -45.0, -90.0, 90.0, 1.0, () -> mode.is(Mode.Grim)).setSuffix("°"));
    private final SliderSetting downPitch = this.add(new SliderSetting("DownPitch", 45.0, -90.0, 90.0, 1.0, () -> mode.is(Mode.Grim)).setSuffix("°"));
    private final BooleanSetting controlRotation = this.add(new BooleanSetting("ControlRotation", false, () -> mode.is(Mode.Grim)));
    private final BooleanSetting airStop = this.add(new BooleanSetting("AirStop", false, () -> mode.is(Mode.Grim)));
    private final SliderSetting ncpFireWorkDelay = this.add(new SliderSetting("FireWorkDelay", 1500.0, 200.0, 10000.0, 50.0, () -> mode.is(Mode.NCP)).setSuffix("ms"));
    private final BooleanSetting ncpFirework = this.add(new BooleanSetting("Firework", true, () -> mode.is(Mode.NCP)));
    private final SliderSetting upFactor = this.add(new SliderSetting("UpFactor", 1.0, 0.0, 10.0, () -> mode.is(Mode.NCP)));
    private final SliderSetting downFactor = this.add(new SliderSetting("FallSpeed", 1.0, 0.0, 10.0, () -> mode.is(Mode.NCP)));
    private final SliderSetting speed = this.add(new SliderSetting("Speed", 1.0, 0.1, 10.0, 0.01, () -> mode.is(Mode.NCP)));
    private final SliderSetting cruiseSpeed = this.add(new SliderSetting("CruiseSpeed", 1.0, 0.1, 10.0, 0.01, () -> mode.is(Mode.NCP)));
    private final BooleanSetting cruiseSpeedRamp = this.add(new BooleanSetting("CruiseSpeedRamp", false, () -> mode.is(Mode.NCP)).setParent());
    private final SliderSetting cruiseSpeedMax = this.add(new SliderSetting("CruiseSpeedMax", 2.0, 0.1, 10.0, 0.01, cruiseSpeedRamp::isOpen));
    private final SliderSetting cruiseSpeedRampTime = this.add(new SliderSetting("CruiseRampTime", 3.0, 0.1, 10.0, 0.1, cruiseSpeedRamp::isOpen).setSuffix("s"));
    private final BooleanSetting cruiseOscillate = this.add(new BooleanSetting("CruiseOscillate", false, cruiseSpeedRamp::isOpen).setParent());
    private final SliderSetting cruiseDropSpeed = this.add(new SliderSetting("DropSpeed", 1.5, 0.1, 10.0, 0.01, cruiseOscillate::isOpen));
    private final BooleanSetting cruiseTimer = this.add(new BooleanSetting("CruiseTimer", false, () -> mode.is(Mode.NCP)).setParent());
    private final SliderSetting cruiseTimerValue = this.add(new SliderSetting("CruiseTimerSpeed", 1.0, 0.1, 10.0, 0.01, cruiseTimer::isOpen));

    // --- NCP 缓慢加速：按住 BoostBind 从 StartSpeed 以 BypassRate/每 RateTime ms 增至 Speed ---
    private final BooleanSetting slownessModifier = this.add(new BooleanSetting("SlownessModifier", false, () -> mode.is(Mode.NCP)).setParent());
    private final SliderSetting slownessSpeed = this.add(new SliderSetting("SlownessSpeed", 1.0, 0.1, 10.0, 0.01, slownessModifier::isOpen));
    private final SliderSetting slownessRampStartSpeed = this.add(new SliderSetting("StartSpeed", 0.7, 0.1, 5.0, 0.01, slownessModifier::isOpen));
    private final SliderSetting bypassRate = this.add(new SliderSetting("BypassRate", 0.05, 0.01, 0.2, 0.01, slownessModifier::isOpen));
    private final SliderSetting rateTime = this.add(new SliderSetting("RateTime", 100.0, 10.0, 1000.0, 10.0, slownessModifier::isOpen).setSuffix("ms"));
    private final BindSetting boostBind = this.add(new BindSetting("BoostBind", -1, () -> mode.is(Mode.NCP)));
    private final BooleanSetting slownessTimer = this.add(new BooleanSetting("SlownessTimer", false, slownessModifier::isOpen).setParent());
    private final SliderSetting slownessTimerValue = this.add(new SliderSetting("TimerSpeed", 1.5, 0.1, 10.0, 0.01, slownessTimer::isOpen));
    private final BooleanSetting slownessDebug = this.add(new BooleanSetting("Debug", false, slownessModifier::isOpen));
    // --- 缓慢 Lag 检测（仿 StrafeSpeed）：EntityVelocityUpdateS2CPacket 窗口计数 → 回到 StartSpeed 锁定 N 秒 → 重新加速 ---
    private final BooleanSetting slownessLagReset = this.add(new BooleanSetting("LagReset", false, slownessModifier::isOpen).setParent());
    private final SliderSetting slownessLagThreshold = this.add(new SliderSetting("LagThreshold", 3, 1, 20, 1.0, slownessLagReset::getValue));
    private final SliderSetting slownessLagWindow = this.add(new SliderSetting("LagWindow", 1.0, 0.01, 10.0, 0.01, slownessLagReset::getValue).setSuffix("s"));
    private final SliderSetting slownessLagDuration = this.add(new SliderSetting("LagDuration", 5.0, 1.0, 30.0, 0.5, slownessLagReset::getValue).setSuffix("s"));
    // --------------------------------------------------------------------
    private final BooleanSetting lagReset = this.add(new BooleanSetting("LagReset", false, () -> mode.is(Mode.NCP) && (slownessModifier.getValue() || cruiseSpeedRamp.getValue())).setParent());
    private final SliderSetting lagThreshold = this.add(new SliderSetting("LagThreshold", 3, 1, 20, 1.0, lagReset::isOpen));
    private final SliderSetting lagWindow = this.add(new SliderSetting("LagWindow", 1.0, 0.01, 10.0, 0.01, lagReset::isOpen).setSuffix("s"));
    private final SliderSetting resetDuration = this.add(new SliderSetting("ResetDuration", 1.0, 0.01, 10.0, 0.01, lagReset::isOpen).setSuffix("s"));
    // --------------------------------------------------------------------

    private final BooleanSetting speedLimit = this.add(new BooleanSetting("SpeedLimit", true, () -> mode.is(Mode.NCP)));
    private final SliderSetting maxSpeed = this.add(new SliderSetting("MaxSpeed", 2.5, 0.1, 10.0, () -> mode.is(Mode.NCP) && speedLimit.getValue()));
    private final BooleanSetting noDrag = this.add(new BooleanSetting("NoDrag", false, () -> mode.is(Mode.NCP)));
    private final SliderSetting sneakDownSpeed = this.add(new SliderSetting("DownSpeed", 1.0, 0.1, 10.0, () -> mode.is(Mode.NCP)));
    private final BooleanSetting autoMend = this.add(new BooleanSetting("AutoMend", false).setParent());
    private final SliderSetting mendThreshold = this.add(new SliderSetting("MendThreshold", 30.0, 10.0, 90.0, autoMend::isOpen).setSuffix("%"));
    private final SliderSetting mendDelay = this.add(new SliderSetting("MendDelay", 3.0, 0.0, 10.0, autoMend::isOpen).setSuffix(" ticks"));
    private final Timer fireworkTimer = new Timer();
    private boolean hasElytra = false;
    private int packetDelayCounter = 0;
    private float controlYaw = 0.0F;
    private float controlPitch = 0.0F;
    private boolean overridePitch = false;
    private boolean overrideYaw = false;
    private boolean ncpHasElytra = false;
    private BlockPos gotoTarget = null;
    private boolean isGotoActive = false;
    private boolean isBaritoneWalking = false;
    private final Timer decisionTimer = new Timer();
    private float autoPilotYaw = 0.0F;
    private float autoPilotPitch = 0.0F;
    private boolean isMending = false;
    private final Timer mendTimer = new Timer();
    private double currentBypassSpeed;
    private boolean boostBindHeld;
    private final Timer speedIncrementTimer = new Timer();
    // --- CruiseSpeed 均匀提速状态 ---
    private final Timer cruiseRampTimer = new Timer();
    private boolean cruiseRampActive = false;
    private double cruiseCurrentSpeed;
    private CruisePhase cruisePhase = CruisePhase.UP;
    private double cruiseSegStart;
    private enum CruisePhase { UP, DOWN }
    private final Object lagResetLock = new Object();
    private long lagWindowStart;
    private int lagEventCount;
    private volatile long lastLagTriggerMs;
    // --- 缓慢 Lag 检测状态（仿 StrafeSpeed 窗口计数模式）---
    private boolean slownessLagActive;
    private int slownessLagCounter;
    private long slownessLagLastUnlockMs;
    private final Timer slownessLagWindowTimer = new Timer();
    private final Timer slownessLagTimer = new Timer();

    public ElytraFlight() {
        super("ElytraFlight", Module.Category.Movement);
        this.setChinese("鞘翅飞行");
        INSTANCE = this;
    }

    public void startGoto(BlockPos target) {
        this.gotoTarget = target;
        this.isGotoActive = true;
        this.isBaritoneWalking = false;
        this.isMending = false;
        BaritoneModule.gotoPos(target);
        this.enable();
    }

    public void stopGoto() {
        if (!this.isGotoActive) return;
        this.isGotoActive = false;
        this.gotoTarget = null;
        this.isMending = false;
        if (this.isBaritoneWalking) {
            BaritoneModule.cancelEverything();
            this.isBaritoneWalking = false;
        }
    }

    @Override
    public String getInfo() {
        if (isMending) return "Mending";
        if (isSlownessLagActive()) return "LagPause";
        if (slownessDebug.getValue() && slownessModifier.getValue() && hasSlownessEffect()) {
            return String.format("%.2f->%.2f|mv=%s|%d", currentBypassSpeed, speed.getValue(),
                MovementUtil.isMoving(), boostBindHeld ? 1 : 0);
        }
        return isGotoActive ? "AutoPilot" : mode.getValue().name();
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        hasElytra = false;
        ncpHasElytra = false;
        packetDelayCounter = 0;
        fireworkTimer.setMs(99999999L);
        overridePitch = false;
        overrideYaw = false;
        resetBypassState();
        resetLagState();
        resetSlownessLagState();
        cruiseRampActive = false;
        cruisePhase = CruisePhase.UP;
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        stopGoto();
        hasElytra = false;
        ncpHasElytra = false;
        overridePitch = false;
        overrideYaw = false;
        resetBypassState();
        resetLagState();
        resetSlownessLagState();
        cruiseRampActive = false;
        cruisePhase = CruisePhase.UP;
        Forever.TIMER.reset();
    }

    private boolean isElytraEquipped() {
        for (ItemStack is : mc.player.getArmorItems()) {
            if (is.getItem() instanceof ElytraItem) return true;
        }
        return false;
    }

    // --- 缓慢加速: 按住 BoostBind → 每 RateTime ms 加 BypassRate，从 StartSpeed 增到 Speed ---
    // 与参考实现对齐：缓慢补偿优先于 lag/jump/cruise 分支，lag 冷却仅在无缓慢的巡航分支生效。
    private double getEffectiveNcpSpeed() {
        // 缓慢补偿最优先：开启且有缓慢效果时完全由 currentBypassSpeed 控制（无视跳跃/lag/巡航）
        // 在此处直接更新斜坡，保证无论事件顺序，速度总是最新值
        if (slownessModifier.getValue() && hasSlownessEffect()) {
            updateSlownessBypass();
            return Math.min(currentBypassSpeed, speed.getValue());
        }
        if (mc.options.jumpKey.isPressed()) {
            return speed.getValue();
        }
        if (isLagSpeedResetActive()) {
            return cruiseSpeed.getValue();
        }
        return getCruiseSpeed();
    }

    // --- CruiseSpeed 均匀提速：进入巡航后从 CruiseSpeed 在 CruiseRampTime 秒内线性增至 CruiseSpeedMax ---
    // 开启 CruiseOscillate 后：到达峰值后线性降到 DropSpeed，再加速回峰值，循环往复。
    // 降速时长复用 CruiseRampTime。
    private double getCruiseSpeed() {
        double base = cruiseSpeed.getValue();
        if (isLagSpeedResetActive()) {
            return base;
        }
        if (!cruiseSpeedRamp.getValue()) {
            return base;
        }
        double peak = Math.max(base, cruiseSpeedMax.getValue());
        if (!cruiseRampActive) {
            cruiseRampActive = true;
            cruisePhase = CruisePhase.UP;
            cruiseSegStart = base;
            cruiseCurrentSpeed = base;
            cruiseRampTimer.reset();
            return base;
        }

        double rampMs = Math.max(1.0, cruiseSpeedRampTime.getValue() * 1000.0);
        double elapsed = cruiseRampTimer.getMs();
        double progress = Math.min(1.0, elapsed / rampMs);

        if (!cruiseOscillate.getValue() || peak <= base) {
            if (elapsed >= rampMs) {
                cruiseCurrentSpeed = peak;
                return peak;
            }
            cruiseCurrentSpeed = base + (peak - base) * progress;
            return cruiseCurrentSpeed;
        }

        double drop = Math.min(cruiseDropSpeed.getValue(), peak);
        if (drop <= base) {
            if (elapsed >= rampMs) {
                cruiseCurrentSpeed = peak;
                return peak;
            }
            cruiseCurrentSpeed = base + (peak - base) * progress;
            return cruiseCurrentSpeed;
        }

        if (cruisePhase == CruisePhase.UP) {
            if (elapsed >= rampMs) {
                cruiseCurrentSpeed = peak;
                cruisePhase = CruisePhase.DOWN;
                cruiseSegStart = peak;
                cruiseRampTimer.reset();
                return peak;
            }
            cruiseCurrentSpeed = cruiseSegStart + (peak - cruiseSegStart) * progress;
            return cruiseCurrentSpeed;
        } else {
            if (elapsed >= rampMs) {
                cruiseCurrentSpeed = drop;
                cruisePhase = CruisePhase.UP;
                cruiseSegStart = drop;
                cruiseRampTimer.reset();
                return drop;
            }
            cruiseCurrentSpeed = peak + (drop - peak) * progress;
            return cruiseCurrentSpeed;
        }
    }

    private boolean isLagSpeedResetActive() {
        long last = lastLagTriggerMs;
        if (last <= 0L) return false;
        long now = System.currentTimeMillis();
        if (now - last < Math.max(1L, Math.round(resetDuration.getValue() * 1000.0))) {
            return true;
        }
        return false;
    }

    private void resetLagState() {
        synchronized (lagResetLock) {
            lagWindowStart = 0L;
            lagEventCount = 0;
            lastLagTriggerMs = 0L;
        }
    }

    @EventListener
    public void onReceivePacket(PacketEvent.Receive event) {
        if (!isOn() || !mode.is(Mode.NCP) || mc.player == null || mc.world == null) return;

        // --- 缓慢 Lag 检测（仿 StrafeSpeed）：EntityVelocityUpdateS2CPacket 窗口计数 ---
        // --- 缓慢 Lag 检测：仅 PlayerPositionLookS2CPacket 且位移 > 0.5 才算真正拉回 ---
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket pkt) {
            if (slownessLagReset.getValue() && slownessModifier.getValue() && mc.player != null) {
                double dx = pkt.getX() - mc.player.getX();
                double dy = pkt.getY() - mc.player.getY();
                double dz = pkt.getZ() - mc.player.getZ();
                if (Math.sqrt(dx*dx + dy*dy + dz*dz) > 0.5) {
                    handleSlownessLagDetection();
                }
            }
        }

        // --- 原有 CruiseSpeed LagReset：PlayerPositionLookS2CPacket ---
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            boolean slownessLag = lagReset.getValue() && slownessModifier.getValue();
            boolean cruiseLag = lagReset.getValue() && cruiseSpeedRamp.getValue();
            if (!slownessLag && !cruiseLag) return;

            long now = System.currentTimeMillis();
            long windowMs = Math.max(1L, Math.round(lagWindow.getValue() * 1000.0));
            synchronized (lagResetLock) {
                if (lagWindowStart == 0L || now - lagWindowStart > windowMs) {
                    lagWindowStart = now;
                    lagEventCount = 0;
                }
                lagEventCount++;
                if (lagEventCount >= lagThreshold.getValueInt()) {
                    lastLagTriggerMs = now;
                    lagWindowStart = 0L;
                    lagEventCount = 0;
                }
            }
        }
    }

    private boolean hasSlownessEffect() {
        return mc.player != null && mc.player.getStatusEffect(StatusEffects.SLOWNESS) != null;
    }

    // --- 缓慢 Lag 检测（仿 StrafeSpeed 窗口计数模式）---
    private void resetSlownessLagState() {
        slownessLagActive = false;
        slownessLagCounter = 0;
        slownessLagLastUnlockMs = 0;
        slownessLagWindowTimer.reset();
        slownessLagTimer.reset();
    }

    private void handleSlownessLagDetection() {
        if (slownessLagActive) return;

        // 解锁后冷却期：防止刚解锁就被频繁的 VelocityUpdate 包立即重新触发
        if (slownessLagLastUnlockMs > 0
                && System.currentTimeMillis() - slownessLagLastUnlockMs < (long) (slownessLagDuration.getValue() * 1000.0)) {
            return;
        }

        long windowMs = Math.max(1L, Math.round(slownessLagWindow.getValue() * 1000.0));
        if (!slownessLagWindowTimer.passedMs(windowMs)) {
            slownessLagCounter++;
        } else {
            slownessLagCounter = 1;
            slownessLagWindowTimer.reset();
        }

        if (slownessLagCounter >= slownessLagThreshold.getValueInt()) {
            slownessLagActive = true;
            slownessLagTimer.reset();
            slownessLagCounter = 0;
            currentBypassSpeed = getBypassStartSpeed();
            speedIncrementTimer.reset();
        }
    }

    private boolean isSlownessLagActive() {
        return slownessLagReset.getValue() && slownessLagActive;
    }

    private void resetBypassState() {
        currentBypassSpeed = getBypassStartSpeed();
        boostBindHeld = false;
        speedIncrementTimer.reset();
    }

    private double getBypassStartSpeed() {
        return Math.min(slownessRampStartSpeed.getValue(), speed.getValue());
    }

    // 自动加速：缓慢生效 + 移动中，从 StartSpeed 线性插值到 Speed。
    // 静止 / 失去缓慢 / lag 触发 → 重置到 StartSpeed，下次移动时重新加速。
    private int debugTick;
    private void updateSlownessBypass() {
        boolean dbg = slownessDebug.getValue();
        if (!mode.is(Mode.NCP) || !slownessModifier.getValue() || !hasSlownessEffect()) {
            if (dbg && debugTick++ % 20 == 0) sendMessage("§c[SLOW] cond fail: ncp=" + mode.is(Mode.NCP) + " mod=" + slownessModifier.getValue() + " hasEff=" + hasSlownessEffect());
            resetBypassState();
            return;
        }

        if (isSlownessLagActive()) {
            if (dbg && debugTick++ % 20 == 0) sendMessage("§6[SLOW] lag lock, spd=" + String.format("%.2f", currentBypassSpeed));
            currentBypassSpeed = getBypassStartSpeed();
            return;
        }

        if (!MovementUtil.isMoving()) {
            if (dbg && debugTick++ % 20 == 0) sendMessage("§7[SLOW] idle, spd=" + String.format("%.2f", currentBypassSpeed));
            resetBypassState();
            return;
        }

        double startSpeed = getBypassStartSpeed();
        double targetSpeed = speed.getValue();

        if (!boostBindHeld) {
            boostBindHeld = true;
            speedIncrementTimer.reset();
            currentBypassSpeed = startSpeed;
            if (dbg) sendMessage("§a[SLOW] START ramp: " + String.format("%.2f", startSpeed) + " -> " + String.format("%.2f", targetSpeed));
            return;
        }

        if (currentBypassSpeed >= targetSpeed || targetSpeed <= startSpeed) {
            currentBypassSpeed = targetSpeed;
            return;
        }

        // 线性插值：StartSpeed → Speed，总时长 = (target-start)/BypassRate * RateTime ms
        double totalSteps = (targetSpeed - startSpeed) / Math.max(0.001, bypassRate.getValue());
        double rampMs = totalSteps * rateTime.getValue();
        double elapsed = speedIncrementTimer.getMs();
        double progress = Math.min(1.0, elapsed / Math.max(1.0, rampMs));
        currentBypassSpeed = startSpeed + (targetSpeed - startSpeed) * progress;
        if (dbg && debugTick++ % 10 == 0) sendMessage("§b[SLOW] ramp: " + String.format("%.2f", currentBypassSpeed) + " | prog=" + String.format("%.0f%%", progress*100) + " | " + String.format("%.0f", elapsed) + "/" + String.format("%.0f", rampMs) + "ms");
    }
    // ----------------------------------------------------

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck() || mc.player == null || mc.world == null) return;

        boolean currentHasElytra = isElytraEquipped();
        boolean shouldWalk = !currentHasElytra;
        if (autoMend.getValue() && currentHasElytra) {
            ItemStack elytra = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            double durabilityPercent = ((double) (elytra.getMaxDamage() - elytra.getDamage()) / elytra.getMaxDamage()) * 100.0;
            int xpCount = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE);
            boolean hasMending = false;
            try {
                ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(elytra);
                hasMending = enchants.getEnchantments()
                        .contains(mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.MENDING.getRegistryRef()).getOptional(Enchantments.MENDING).get());
            } catch (Exception ignored) {}
            if (durabilityPercent < mendThreshold.getValue() && xpCount > 0 && hasMending) {
                if (!isMending) {
                    isMending = true;
                    mc.player.sendMessage(net.minecraft.text.Text.literal("§6[AutoPilot] Elytra durability is low (" + (int)durabilityPercent + "%). Landing for repairs."), false);
                }
            }

            if (isMending) {
                shouldWalk = true;

                if (mc.player.isOnGround()) {
                    mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
                    if (durabilityPercent >= 99.0 || xpCount == 0 || !hasMending) {
                        isMending = false;
                        mc.player.sendMessage(net.minecraft.text.Text.literal("§a[AutoPilot] Repair complete. Durability: " + (int)durabilityPercent + "%. Resuming flight."), false);
                    } else {
                        if (mendTimer.passed((long) (mendDelay.getValue() * 50.0))) {
                            throwExp();
                            mendTimer.reset();
                        }
                    }
                } else {
                    if (isGotoActive && !isBaritoneWalking) {
                        isBaritoneWalking = true;
                        BaritoneModule.gotoPos(gotoTarget);
                    }
                }
            }
        }
        if (isGotoActive && gotoTarget != null && mode.is(Mode.NCP) && !isMending) {
            Vec3d playerPos = mc.player.getPos();
            double distToTarget = playerPos.distanceTo(gotoTarget.toCenterPos());

            if (distToTarget < 2.0) {
                stopGoto();
                mc.player.sendMessage(net.minecraft.text.Text.literal("§a[AutoPilot] Destination reached."), false);
                return;
            }

            if (!BaritoneModule.isActive()) {
                BaritoneModule.gotoPos(gotoTarget);
            }

            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            Vec3d immediateTarget = gotoTarget.toCenterPos();

            if (!shouldWalk && baritone != null && baritone.getPathingBehavior().isPathing()) {
                IPathExecutor currentExecutor = baritone.getPathingBehavior().getCurrent();

                if (currentExecutor != null) {
                    IPath path = currentExecutor.getPath();
                    List<? extends BlockPos> pathPositions = path.positions();
                    int currentIndex = currentExecutor.getPosition();

                    int checkDistance = Math.min(8, pathPositions.size() - currentIndex - 1);

                    if (checkDistance > 0) {
                        boolean pathIsFlatOrDown = true;
                        for (int i = 1; i <= checkDistance; i++) {
                            BlockPos node = pathPositions.get(currentIndex + i);
                            if (node.getY() > mc.player.getBlockY()) {
                                pathIsFlatOrDown = false;
                                break;
                            }
                        }
                        int targetIndex = Math.min(currentIndex + 5, pathPositions.size() - 1);
                        BlockPos targetNode = pathPositions.get(targetIndex);
                        immediateTarget = new Vec3d(targetNode.getX() + 0.5, targetNode.getY() + 1.0, targetNode.getZ() + 0.5);
                        shouldWalk = true;
                        if (pathIsFlatOrDown) {
                            Vec3d startPos = mc.player.getCameraPosVec(1.0F);
                            Vec3d endPos = new Vec3d(targetNode.getX() + 0.5, targetNode.getY() + 1.62, targetNode.getZ() + 0.5);

                            RaycastContext context = new RaycastContext(
                                    startPos, endPos,
                                    RaycastContext.ShapeType.COLLIDER,
                                    RaycastContext.FluidHandling.NONE,
                                    mc.player
                            );

                            boolean hitWall = mc.world.raycast(context).getType() != HitResult.Type.MISS;
                            if (!hitWall) {
                                shouldWalk = false;
                            }
                        }
                    }
                }
            }
            double diffX = immediateTarget.x - playerPos.x;
            double diffY = immediateTarget.y - playerPos.y;
            double diffZ = immediateTarget.z - playerPos.z;
            double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

            autoPilotYaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
            autoPilotPitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));
            if (decisionTimer.passed(150L)) {
                if (shouldWalk) {
                    if (!isBaritoneWalking) {
                        isBaritoneWalking = true;
                        if (mc.player.isFallFlying()) {
                            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                        }
                        BaritoneModule.gotoPos(gotoTarget);
                    }
                } else {
                    if (isBaritoneWalking) {
                        BaritoneModule.cancelEverything();
                        isBaritoneWalking = false;
                    }
                }
                decisionTimer.reset();
            }
            if (!isBaritoneWalking && !shouldWalk && !mc.player.isFallFlying() && currentHasElytra && !isMending) {
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                } else {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    mc.player.startFallFlying();
                }
            }
            if (!isBaritoneWalking && mc.player.isFallFlying()) {
                mc.player.input.movementForward = 0.0F;
                mc.player.input.movementSideways = 0.0F;
                mc.player.input.pressingForward = false;
                mc.player.input.pressingBack = false;
                mc.player.input.pressingLeft = false;
                mc.player.input.pressingRight = false;
            }
        }

        if (mode.is(Mode.Grim)) {
            handleGrimMode();
        } else if (mode.is(Mode.NCP)) {
            handleNCPMode();
        }
    }

    @EventListener(priority = -9999)
    public void onRotation(UpdateRotateEvent event) {
        if (nullCheck()) return;
        if (isGotoActive && !isBaritoneWalking && isFallFlying() && mode.is(Mode.NCP)) {
            event.setYaw(autoPilotYaw);
            event.setPitch(autoPilotPitch);
            return;
        }
        if (isMending && mc.player.isOnGround()) {
            event.setPitch(88.0F);
            return;
        }

        if (!mode.is(Mode.Grim)) return;
        if (!controlRotation.getValue()) return;
        if (!isFallFlying()) return;

        if (overrideYaw) event.setYaw(controlYaw);
        if (overridePitch) event.setPitch(controlPitch);
    }

    @EventListener
    public void onNCPTravel(TravelEvent event) {
        if (nullCheck() || !mode.is(Mode.NCP)) return;
        if (!isElytraEquipped() || !isFallFlying() || event.isPost()) return;
        if (isMending) return;

        boolean snapping = false;
        boolean isJumping = mc.player.input.jumping;
        boolean isSneaking = mc.player.input.sneaking;
        if (isGotoActive && !isBaritoneWalking) {
            isJumping = false;
            isSneaking = false;
            setY(0.0);
        } else if (!mc.player.isOnGround() && mc.player.getVelocity().y < -0.05) {
            setY(0.0);
        }

        if (ncpFirework.getValue()) {
            if (!isGotoActive || isBaritoneWalking) {
                if (isSneaking && isJumping) {
                    setY(0.0);
                } else if (isSneaking) {
                    setY(-sneakDownSpeed.getValue());
                } else if (isJumping) {
                    setY(upFactor.getValue());
                } else {
                    setY(-3.0E-11 * downFactor.getValue());
                }
            }

            if (!snapping) {
                if (isGotoActive && !isBaritoneWalking) {
                    setX(-Math.sin(Math.toRadians(autoPilotYaw)) * getEffectiveNcpSpeed());
                    setZ(Math.cos(Math.toRadians(autoPilotYaw)) * getEffectiveNcpSpeed());
                } else {
                    double[] dir = MovementUtil.directionSpeed(getEffectiveNcpSpeed());
                    setX(dir[0]);
                    setZ(dir[1]);
                }
            }

        } else {
            Vec3d lookVec = (isGotoActive && !isBaritoneWalking)
                    ? getRotationVecFromAngles(autoPilotPitch, autoPilotYaw)
                    : getRotationVec(mc.getRenderTickCounter().getTickDelta(true));

            double lookDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
            double motionDist = Math.sqrt(getX() * getX() + getZ() * getZ());

            if (!isGotoActive || isBaritoneWalking) {
                if (isSneaking) setY(-sneakDownSpeed.getValue());
                else if (!isJumping) setY(-3.0E-11 * downFactor.getValue());
            }

            if (isJumping && (!isGotoActive || isBaritoneWalking)) {
                if (motionDist > upFactor.getValue() / upFactor.getMax()) {
                    double rawUpSpeed = motionDist * 0.01325;
                    setY(getY() + rawUpSpeed * 3.2);
                    if (!snapping) {
                        setX(getX() - lookVec.x * rawUpSpeed / lookDist);
                        setZ(getZ() - lookVec.z * rawUpSpeed / lookDist);
                    }
                } else {
                    if (!snapping) {
                        double[] dir = MovementUtil.directionSpeed(getEffectiveNcpSpeed());
                        setX(dir[0]);
                        setZ(dir[1]);
                    }
                }
            }

            if (!snapping) {
                if (lookDist > 0.0) {
                    setX(getX() + (lookVec.x / lookDist * motionDist - getX()) * 0.1);
                    setZ(getZ() + (lookVec.z / lookDist * motionDist - getZ()) * 0.1);
                }
                if (!isJumping) {
                    if (isGotoActive && !isBaritoneWalking) {
                        setX(-Math.sin(Math.toRadians(autoPilotYaw)) * getEffectiveNcpSpeed());
                        setZ(Math.cos(Math.toRadians(autoPilotYaw)) * getEffectiveNcpSpeed());
                    } else {
                        double[] dir = MovementUtil.directionSpeed(getEffectiveNcpSpeed());
                        setX(dir[0]);
                        setZ(dir[1]);
                    }
                }
            }
        }

        if (!noDrag.getValue()) {
            setY(getY() * 0.99F);
            if (!snapping) {
                setX(getX() * 0.98F);
                setZ(getZ() * 0.99F);
            }
        }

        if (!snapping) {
            double finalDist = Math.sqrt(getX() * getX() + getZ() * getZ());
            if (speedLimit.getValue() && finalDist > maxSpeed.getValue()) {
                setX(getX() * maxSpeed.getValue() / finalDist);
                setZ(getZ() * maxSpeed.getValue() / finalDist);
            }
        }

        event.cancel();
        mc.player.move(MovementType.SELF, mc.player.getVelocity());
    }

    private void handleNCPMode() {
        if (!isElytraEquipped()) return;
        boolean isSneaking = mc.options.sneakKey.isPressed();

        if (!isFallFlying()
                && !mc.player.isOnGround()
                && mc.player.getVelocity().getY() < -0.1
                && mc.player.fallDistance > 0.2F
                && !isBaritoneWalking
                && !isGotoActive
                && !isSneaking) {

            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startFallFlying();
        }

        // --- 缓慢 Lag 锁定期满 → 解除锁定，进入冷却期，防止立即重新触发
        if (slownessLagActive && slownessLagTimer.passedMs((long) (slownessLagDuration.getValue() * 1000.0))) {
            slownessLagActive = false;
            slownessLagCounter = 0;
            slownessLagWindowTimer.reset();
            slownessLagLastUnlockMs = System.currentTimeMillis(); // 冷却起点
            boostBindHeld = false;
        }

        // 失去缓慢效果时重置斜坡状态（保证下次缓慢恢复时从 StartSpeed 开始）
        if (slownessModifier.getValue() && !hasSlownessEffect()) {
            resetBypassState();
        }

        // CruiseSpeed ramp 状态管理：
        if (!isFallFlying()) {
            cruiseRampActive = false;
        } else if (mc.options.jumpKey.isPressed()) {
            cruiseRampActive = false;
        } else if (!MovementUtil.isMoving()) {
            cruiseRampActive = false;
        } else if (isLagSpeedResetActive()) {
            cruiseRampActive = false;
        } else if (!cruiseRampActive) {
            cruiseRampActive = true;
            cruisePhase = CruisePhase.UP;
            cruiseSegStart = cruiseSpeed.getValue();
            cruiseCurrentSpeed = cruiseSpeed.getValue();
            cruiseRampTimer.reset();
        }

        if (isFallFlying()) {
            boolean isSlowed = slownessModifier.getValue() && hasSlownessEffect();
            if (isSlowed && slownessTimer.getValue()) {
                Forever.TIMER.set(slownessTimerValue.getValueFloat());
            } else if (!isSlowed && cruiseTimer.getValue() && !mc.options.jumpKey.isPressed()) {
                Forever.TIMER.set(cruiseTimerValue.getValueFloat());
            } else {
                Forever.TIMER.reset();
            }
        }
        // --------------------------------

        if (isFallFlying() && MovementUtil.isMoving() && !isGotoActive) {
            handleNCPFirework();
        }
    }

    private void handleGrimMode() {
        if (mc.player.isOnGround()) {
            packetDelayCounter = 0;
            return;
        }

        hasElytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA) != -1;
        if (!hasElytra) return;

        packetDelayCounter++;
        if (packetDelayCounter <= (int) packetDelay.getValue()) return;
        packetDelayCounter = 0;

        int elytraHotbar = InventoryUtil.findItem(Items.ELYTRA);
        if (elytraHotbar != -1) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, elytraHotbar, SlotActionType.SWAP, mc.player);
            sendStartFlyingPacket();
            if (shouldUseFirework()) handleGrimFirework();
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, elytraHotbar, SlotActionType.SWAP, mc.player);
        } else {
            int elytraInventory = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
            if (elytraInventory == -1) return;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytraInventory, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            sendStartFlyingPacket();
            if (shouldUseFirework()) handleGrimFirework();
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytraInventory, 0, SlotActionType.PICKUP, mc.player);
        }

        if (controlRotation.getValue() && isFallFlying()) { updateGrimRotation(); } else { overridePitch = false; overrideYaw = false; }
    }

    private void sendStartFlyingPacket() {
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        if (setFlag.getValue()) { mc.player.startFallFlying(); }
    }

    private void updateGrimRotation() {
        boolean forward = mc.options.forwardKey.isPressed(), back = mc.options.backKey.isPressed(), left = mc.options.leftKey.isPressed(), right = mc.options.rightKey.isPressed(), jump = mc.options.jumpKey.isPressed(), sneak = mc.options.sneakKey.isPressed();
        boolean hasHorizontalInput = forward || back || left || right;
        boolean hasVerticalInput = jump || sneak;

        if (hasHorizontalInput) { overrideYaw = true; controlYaw = getSprintYaw(mc.player.getYaw()); } else { overrideYaw = false; }
        overridePitch = true;

        if (hasVerticalInput) {
            if (hasHorizontalInput) {
                if (jump && !sneak) controlPitch = (float) upPitch.getValue();
                else if (sneak && !jump) controlPitch = (float) downPitch.getValue();
                else controlPitch = (float) defaultPitch.getValue();
            } else {
                if (jump && !sneak) controlPitch = -90.0F;
                else if (sneak && !jump) controlPitch = 90.0F;
                else controlPitch = (float) defaultPitch.getValue();
            }
        } else { controlPitch = (float) defaultPitch.getValue(); }
    }

    private static float getSprintYaw(float yaw) {
        if (mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed()) {
            if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) yaw -= 45.0F;
            else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) yaw += 45.0F;
        } else if (mc.options.backKey.isPressed() && !mc.options.forwardKey.isPressed()) {
            yaw += 180.0F;
            if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) yaw += 45.0F;
            else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) yaw -= 45.0F;
        } else if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) { yaw -= 90.0F;
        } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) { yaw += 90.0F; }
        return MathHelper.wrapDegrees(yaw);
    }

    @EventListener
    public void onGrimTravel(TravelEvent event) {
        if (nullCheck() || mc.player == null) return;
        if (!mode.is(Mode.Grim)) return;
        if (!airStop.getValue()) return;
        if (!isFallFlying()) return;
        if (event.isPost()) return;

        boolean hasInput = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed();
        if (!hasInput) { MovementUtil.setMotionX(0); MovementUtil.setMotionY(0); MovementUtil.setMotionZ(0); event.cancel(); mc.player.move(MovementType.SELF, mc.player.getVelocity()); }
    }

    private boolean shouldUseFirework() { return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.jumpKey.isPressed(); }

    private void handleNCPFirework() {
        if (mc.player.isUsingItem()) return;
        if (!fireworkTimer.passed((long) ncpFireWorkDelay.getValue())) return;
        for (Entity entity : Forever.THREAD.getEntities()) { if (entity instanceof FireworkRocketEntity frw && ((IFireworkRocketEntity) frw).getShooter() == mc.player) return; }
        useFirework(); fireworkTimer.reset();
    }

    private void handleGrimFirework() {
        if (mc.player.isUsingItem()) return;
        if (!fireworkTimer.passed((long) fireWorkDelay.getValue())) return;
        for (Entity entity : Forever.THREAD.getEntities()) { if (entity instanceof FireworkRocketEntity frw && ((IFireworkRocketEntity) frw).getShooter() == mc.player) return; }
        useFirework(); fireworkTimer.reset();
    }

    private void useFirework() {
        if (mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) { sendFireworkPacket(); return; }
        int firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET);
        if (firework != -1) { InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot); sendFireworkPacket(); InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot); EntityUtil.syncInventory(); return; }
        firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
        if (firework != -1) { int oldSlot = mc.player.getInventory().selectedSlot; InventoryUtil.switchToSlot(firework); sendFireworkPacket(); InventoryUtil.switchToSlot(oldSlot); }
    }

    private void sendFireworkPacket() { sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Forever.ROTATION.getLastYaw(), Forever.ROTATION.getLastPitch())); }
    private void throwExp() {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int newSlot;
        if ((newSlot = InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE)) != -1) {
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(
                    Hand.MAIN_HAND, id,
                    Forever.ROTATION.getLastYaw(),
                    88.0F
            ));
            mc.player.swingHand(Hand.MAIN_HAND);
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
        } else if ((newSlot = InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE)) != -1) {
            InventoryUtil.switchToSlot(newSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(
                    Hand.MAIN_HAND, id,
                    Forever.ROTATION.getLastYaw(),
                    88.0F
            ));
            mc.player.swingHand(Hand.MAIN_HAND);
            InventoryUtil.switchToSlot(oldSlot);
        }
    }

    private Vec3d getRotationVecFromAngles(float pitch, float yaw) { float f = pitch * ((float) Math.PI / 180); float g = -yaw * ((float) Math.PI / 180); float h = MathHelper.cos(g); float i = MathHelper.sin(g); float j = MathHelper.cos(f); float k = MathHelper.sin(f); return new Vec3d(i * j, -k, h * j); }
    private Vec3d getRotationVec(float tickDelta) { float pitch = 0.0F; float yaw = mc.player.getYaw(tickDelta); float f = pitch * ((float) Math.PI / 180); float g = -yaw * ((float) Math.PI / 180); float h = MathHelper.cos(g); float i = MathHelper.sin(g); float j = MathHelper.cos(f); float k = MathHelper.sin(f); return new Vec3d(i * j, -k, h * j); }
    private double getX() { return MovementUtil.getMotionX(); }
    private void setX(double f) { MovementUtil.setMotionX(f); }
    private double getY() { return MovementUtil.getMotionY(); }
    private void setY(double f) { MovementUtil.setMotionY(f); }
    private double getZ() { return MovementUtil.getMotionZ(); }
    private void setZ(double f) { MovementUtil.setMotionZ(f); }
    public boolean isFallFlying() { if (mode.is(Mode.Grim)) { return mc.player != null && (mc.player.isFallFlying() || (hasElytra && !mc.player.isOnGround())); } else { return mc.player != null && mc.player.isFallFlying(); } }
    public boolean isNoAnimationActive() { return isOn() && mode.is(Mode.Grim) && noAnimation.getValue(); }
    public boolean isPacketMode() { return isOn() && mode.is(Mode.Grim); }
}