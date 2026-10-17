package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.*;
import dev.kissoo.api.utils.combat.CombatUtil;
import dev.kissoo.api.utils.math.*;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.render.ColorUtil;
import dev.kissoo.api.utils.render.Render3DUtil;
import dev.kissoo.api.utils.world.BlockPosX;
import dev.kissoo.api.utils.world.BlockUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.chat.ChatClient;
import dev.kissoo.mod.modules.impl.misc.FakePlayer;
import dev.kissoo.mod.modules.impl.misc.IrcModule;
import dev.kissoo.mod.modules.impl.client.AntiCheat;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.mod.modules.impl.exploit.Phase;
import dev.kissoo.mod.modules.impl.player.SpeedMine;
import dev.kissoo.mod.modules.settings.enums.SwingSide;
import dev.kissoo.mod.modules.settings.impl.*;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class CrystalBot extends Module {

    public static CrystalBot INSTANCE;

    private enum Page {
        Place, Break, Base, Rotation, RayTrace, Prediction, Misc, Render
    }

    private enum PlaceMode {
        None, Weak, Strong, Instant
    }

    private enum ExplodeMode {
        Normal, Sync, Both, Sequential
    }

    private enum PlacePosMode {
        Single, Double
    }

    private enum SwapMode {
        None, Normal, Silent, Inventory
    }

    // Mio 移植：Silent 切手方式（Normal=直接切槽 / Alternative=库存静默互换 / Pick=选格）
    private enum SilentMode {
        Normal, Alternative, Pick
    }

    // Mio 移植：放置多点射线（提高穿墙/贴边放置成功率）
    private enum MultiPoint {
        Fast, X2, X4, X8
    }

    private enum RotationBypass {
        Off, ExtraHitbox, Point, PlaceAngle
    }

    private final EnumSetting<Page> page = this.add(new EnumSetting<>("Page", Page.Place));

    private final SliderSetting placeDelay = this.add(
            new SliderSetting("PlaceDelay", 300, 0, 1000, () -> page.is(Page.Place)).setSuffix("ms")
    );
    private final EnumSetting<PlaceMode> placeMode = this.add(
            new EnumSetting<>("PlaceMode", PlaceMode.Strong, () -> page.is(Page.Place))
    );
    private final EnumSetting<SwapMode> autoSwap = this.add(
            new EnumSetting<>("AutoSwap", SwapMode.None, () -> page.is(Page.Place))
    );
    private final EnumSetting<SilentMode> silentMode = this.add(
            new EnumSetting<>("SilentMode", SilentMode.Normal,
                    () -> page.is(Page.Place) && autoSwap.is(SwapMode.Silent))
    );
    private final SliderSetting placeRange = this.add(
            new SliderSetting("PlaceRange", 5.0, 0.0, 6.0, 0.01, () -> page.is(Page.Place)).setSuffix("m")
    );
    private final SliderSetting minPDamage = this.add(
            new SliderSetting("MinPDamage", 5.0, 0.0, 36.0, 0.1, () -> page.is(Page.Place)).setSuffix("dmg")
    );
    private final SliderSetting maxSelfDamage = this.add(
            new SliderSetting("MaxSelfDmg", 12.0, 0.0, 36.0, 0.1, () -> page.is(Page.Place)).setSuffix("dmg")
    );
    private final SliderSetting reserve = this.add(
            new SliderSetting("Reserve", 2.0, 0.0, 10.0, 0.1, () -> page.is(Page.Place)).setSuffix("hp")
    );
    private final BooleanSetting facePlace = this.add(
            new BooleanSetting("FacePlace", false, () -> page.is(Page.Place))).setParent();
    private final SliderSetting facePlaceHealth = this.add(
            new SliderSetting("FacePlaceHealth", 4.0, 0.0, 36.0, 0.1,
                    () -> page.is(Page.Place) && facePlace.isOpen()).setSuffix("hp"));
    private final SliderSetting facePlaceArmor = this.add(
            new SliderSetting("FacePlaceArmor", 10, 0, 100, 1.0,
                    () -> page.is(Page.Place) && facePlace.isOpen()));

    private final SliderSetting breakDelay = this.add(
            new SliderSetting("BreakDelay", 300, 0, 1000, () -> page.is(Page.Break)).setSuffix("ms")
    );
    private final EnumSetting<ExplodeMode> explodeMode = this.add(
            new EnumSetting<>("ExplodeMode", ExplodeMode.Normal, () -> page.is(Page.Break))
    );
    private final SliderSetting breakRange = this.add(
            new SliderSetting("BreakRange", 4.0, 0.0, 6.0, 0.01, () -> page.is(Page.Break)).setSuffix("m")
    );
    private final BooleanSetting breakRemove = this.add(
            new BooleanSetting("Remove", false, () -> page.is(Page.Break))
    );
    private final SliderSetting breakFactor = this.add(
            new SliderSetting("BreakFactor", 1, 1, 10, 1.0, () -> page.is(Page.Break))
    );
    private final BooleanSetting instantBreak = this.add(
            new BooleanSetting("InstantBreak", true, () -> page.is(Page.Break))
    );
    private final BooleanSetting syncWeb = this.add(
            new BooleanSetting("SyncWeb", false, () -> page.is(Page.Break))
    );

    private final BooleanSetting basePlace = this.add(
            new BooleanSetting("BasePlace", true, () -> page.is(Page.Base)).setParent()
    );
    private final SliderSetting basePlaceRange = this.add(
            new SliderSetting("BasePlaceRange", 5.0, 0.0, 6.0, 0.01, () -> page.is(Page.Base) && basePlace.isOpen()).setSuffix("m")
    );
    private final SliderSetting baseDelay = this.add(
            new SliderSetting("BaseDelay", 3000, 0, 10000, () -> page.is(Page.Base) && basePlace.isOpen()).setSuffix("ms")
    );
    private final SliderSetting baseMin = this.add(
            new SliderSetting("BaseMin", 6.0, 0.0, 36.0, 0.1, () -> page.is(Page.Base) && basePlace.isOpen()).setSuffix("dmg")
    );
    private final SliderSetting baseMax = this.add(
            new SliderSetting("BaseMax", 12.0, 0.0, 36.0, 0.1, () -> page.is(Page.Base) && basePlace.isOpen()).setSuffix("dmg")
    );
    private final SliderSetting overrideMax = this.add(
            new SliderSetting("OverrideMax", 8.0, 0.0, 36.0, 0.1, () -> page.is(Page.Base) && basePlace.isOpen()).setSuffix("dmg")
    );
    private final BooleanSetting baseBalance = this.add(
            new BooleanSetting("BaseBalance", true, () -> page.is(Page.Base) && basePlace.isOpen())
    );
    private final BooleanSetting onlyBelow = this.add(
            new BooleanSetting("OnlyBelow", true, () -> page.is(Page.Base) && basePlace.isOpen())
    );
    private final BooleanSetting baseInventorySwap = this.add(
            new BooleanSetting("InventorySwap", true, () -> page.is(Page.Base) && basePlace.isOpen())
    );
    private final BooleanSetting detectMining = this.add(
            new BooleanSetting("DetectMining", true, () -> page.is(Page.Base) && basePlace.isOpen())
    );
    private final BooleanSetting rotateOnPlace = this.add(
            new BooleanSetting("OnPlace", true, () -> page.is(Page.Rotation))
    );
    private final BooleanSetting rotateOnBreak = this.add(
            new BooleanSetting("OnBreak", true, () -> page.is(Page.Rotation))
    );
    private final EnumSetting<RotationBypass> rotationBypass = this.add(
            new EnumSetting<>("RotationBypass", RotationBypass.Off,
                    () -> page.is(Page.Rotation) && (rotateOnPlace.getValue() || rotateOnBreak.getValue()))
    );
    private final SliderSetting extraHitboxExpand = this.add(
            new SliderSetting("HitboxExpand", 0.5, 0.1, 2.0, 0.1,
                    () -> page.is(Page.Rotation)
                            && (rotateOnPlace.getValue() || rotateOnBreak.getValue())
                            && rotationBypass.is(RotationBypass.ExtraHitbox))
    );

    private final BooleanSetting placeTWall = this.add(
            new BooleanSetting("PlaceTWall", false, () -> page.is(Page.RayTrace)).setParent()
    );
    private final EnumSetting<MultiPoint> multipoint = this.add(
            new EnumSetting<>("MultiPoint", MultiPoint.Fast, () -> page.is(Page.RayTrace))
    );
    private final SliderSetting pWallRange = this.add(
            new SliderSetting("PWallRange", 3.0, 0.0, 6.0, 0.01,
                    () -> page.is(Page.RayTrace) && placeTWall.isOpen()).setSuffix("m")
    );
    private final BooleanSetting breakTWall = this.add(
            new BooleanSetting("BreakTWall", false, () -> page.is(Page.RayTrace)).setParent()
    );
    private final SliderSetting bWallRange = this.add(
            new SliderSetting("BWallRange", 3.0, 0.0, 6.0, 0.01,
                    () -> page.is(Page.RayTrace) && breakTWall.isOpen()).setSuffix("m")
    );

    private final BooleanSetting predict = this.add(
            new BooleanSetting("Predict", true, () -> page.is(Page.Prediction)).setParent()
    );
    private final SliderSetting predictTicks = this.add(
            new SliderSetting("PredictTicks", 4, 0, 20,
                    () -> page.is(Page.Prediction) && predict.isOpen()).setSuffix("ticks")
    );
    private final SliderSetting maxMotionXZ = this.add(
            new SliderSetting("MaxMotionXZ", 0.6, 0.0, 2.0, 0.01,
                    () -> page.is(Page.Prediction) && predict.isOpen()).setSuffix("b/t")
    );
    private final SliderSetting maxMotionY = this.add(
            new SliderSetting("MaxMotionY", 0.34, 0.0, 2.0, 0.01,
                    () -> page.is(Page.Prediction) && predict.isOpen()).setSuffix("b/t")
    );
    private final BooleanSetting selfPredict = this.add(
            new BooleanSetting("SelfPredict", true, () -> page.is(Page.Prediction)).setParent()
    );
    private final SliderSetting selfPredictTicks = this.add(
            new SliderSetting("SelfPredictTicks", 0, 0, 10,
                    () -> page.is(Page.Prediction) && selfPredict.isOpen()).setSuffix("ticks")
    );
    private final BooleanSetting autoPredict = this.add(
            new BooleanSetting("AutoPredict", false, () -> page.is(Page.Prediction))
    );

    private final BooleanSetting usingPause = this.add(
            new BooleanSetting("UsingPause", true, () -> page.is(Page.Misc))
    );
    private final BooleanSetting guiPause = this.add(
            new BooleanSetting("GuiPause", true, () -> page.is(Page.Misc))
    );
    private final BooleanSetting regearPause = this.add(
            new BooleanSetting("RegearPause", true, () -> page.is(Page.Misc))
    );
    private final BooleanSetting phasePause = this.add(
            new BooleanSetting("PhasePause", true, () -> page.is(Page.Misc))
    );
    private final BooleanSetting pushPause = this.add(
            new BooleanSetting("PushPause", true, () -> page.is(Page.Misc))
    );
    private final EnumSetting<PlacePosMode> placePosMode = this.add(
            new EnumSetting<>("PlacePosMode", PlacePosMode.Single, () -> page.is(Page.Misc))
    );
    private final SliderSetting posUpdateDelay = this.add(
            new SliderSetting("PosUpdateDelay", 50, 0, 5000, () -> page.is(Page.Misc)).setSuffix("ms")
    );
    private final SliderSetting targetRange = this.add(
            new SliderSetting("TargetRange", 12.0, 0.0, 20.0, 0.1, () -> page.is(Page.Misc)).setSuffix("m")
    );
    private final SliderSetting wallRange = this.add(
            new SliderSetting("WallRange", 6.0, 0.0, 6.0, 0.01, () -> page.is(Page.Misc)).setSuffix("m")
    );
    private final BooleanSetting balance = this.add(
            new BooleanSetting("Balance", true, () -> page.is(Page.Misc)).setParent()
    );
    private final SliderSetting balanceOffset = this.add(
            new SliderSetting("BalanceOffset", 0.0, -20.0, 20.0, 0.1,
                    () -> page.is(Page.Misc) && balance.isOpen()).setSuffix("hp")
    );

    private final BooleanSetting render = this.add(
            new BooleanSetting("Render", true, () -> page.is(Page.Render))
    );
    private final BooleanSetting shrink = this.add(
            new BooleanSetting("Shrink", true, () -> page.is(Page.Render) && render.getValue())
    );
    private final ColorSetting fillColor = this.add(
            new ColorSetting("Fill", new Color(255, 255, 255, 100),
                    () -> page.is(Page.Render) && render.getValue()).injectBoolean(true)
    );
    private final ColorSetting boxColor = this.add(
            new ColorSetting("Box", new Color(255, 255, 255, 255),
                    () -> page.is(Page.Render) && render.getValue()).injectBoolean(true)
    );
    private final SliderSetting lineWidth = this.add(
            new SliderSetting("LineWidth", 1.5, 0.01, 3.0, 0.01,
                    () -> page.is(Page.Render) && render.getValue())
    );
    private final SliderSetting sliderSpeed = this.add(
            new SliderSetting("SliderSpeed", 0.2, 0.01, 1.0, 0.01,
                    () -> page.is(Page.Render) && render.getValue())
    );
    private final SliderSetting startFadeTime = this.add(
            new SliderSetting("StartFade", 0.3, 0.0, 2.0, 0.01,
                    () -> page.is(Page.Render) && render.getValue()).setSuffix("s")
    );
    private final SliderSetting fadeSpeed = this.add(
            new SliderSetting("FadeSpeed", 0.2, 0.01, 1.0, 0.01,
                    () -> page.is(Page.Render) && render.getValue())
    );
    private final BooleanSetting renderText = this.add(
            new BooleanSetting("DamageText", true, () -> page.is(Page.Render) && render.getValue())
    );
    private final ColorSetting textColor = this.add(
            new ColorSetting("TextColor", new Color(255, 255, 255, 255),
                    () -> page.is(Page.Render) && render.getValue() && renderText.getValue())
    );
    private final BooleanSetting itemSpoof = this.add(
            new BooleanSetting("ItemSpoof", false, () -> page.is(Page.Render)).setParent()
    );
    private final EnumSetting<SpoofMode> spoofMode = this.add(
            new EnumSetting<>("SpoofMode", SpoofMode.Crystal,
                    () -> page.is(Page.Render) && itemSpoof.isOpen())
    );

    private final BooleanSetting forcePlace = this.add(
            new BooleanSetting("ForcePlace", false, () -> page.is(Page.Place))
    );
    private final BooleanSetting onlyActive = this.add(
            new BooleanSetting("OnlyActive", false,
                    () -> page.is(Page.Render) && itemSpoof.isOpen())
    );

    private boolean firstBase = true;

    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer baseTimer = new Timer();
    private final Timer posLockTimer = new Timer();
    private final Timer noPosTimer = new Timer();
    private final Timer rotationResetTimer = new Timer();
    private final DecimalFormat df = new DecimalFormat("0.0");
    private Vec3d rotationTarget = null;
    private Vec3d placeRotationPoint = null;
    private Vec3d unifiedRotationPoint = null;

    private BlockPos tempPos = null;
    private BlockPos tempBasePos = null;
    public BlockPos crystalPos = null;
    private BlockPos instantPendingPos = null; // Instant模式：本地删实体后等待音效触发下一个
    private int mioPendingBreakId = -1;          // Mio式：spawn包伤害门控后待破的水晶实体ID（onEntitySpawn 执行）
    private BlockPos basePos = null;
    private EndCrystalEntity tempBreakCrystal = null;
    private EndCrystalEntity breakCrystal = null;

    private PlayerEntity currentTarget = null;
    private float tempDamage = 0.0f;
    private float lastDamage = 0.0f;
    private float breakDamage = 0.0f;

    private PosCandidate lockedCandidate1 = null;
    private PosCandidate lockedCandidate2 = null;
    private boolean useSecondPos = false;

    private Vec3d placeVec3d = null;
    private Vec3d curVec3d = null;
    private double currentFade = 0.0;

    // ── Async VirtualThread pipeline (Mio-style) ──
    private ExecutorService executor;
    private CompletableFuture<CalcResult> calcFuture;
    private final AtomicReference<CalcResult> bestResult = new AtomicReference<>(null);

    private static class PlayerEntityPredict {
        final PlayerEntity player;
        final PlayerEntity predict;
        final Vec3d predictedPos;
        final Box predictedBox;

        PlayerEntityPredict(PlayerEntity player, double maxMotionXZ, double maxMotionY, int ticks) {
            this.player = player;
            double mxz = maxMotionXZ;
            double motionX = MathHelper.clamp(player.getX() - player.prevX, -mxz, mxz);
            double motionY = MathHelper.clamp(player.getY() - player.prevY, -maxMotionY, maxMotionY);
            double motionZ = MathHelper.clamp(player.getZ() - player.prevZ, -mxz, mxz);
            double px = player.getX() + motionX * ticks;
            double py = player.getY() + motionY * ticks;
            double pz = player.getZ() + motionZ * ticks;
            this.predictedPos = new Vec3d(px, py, pz);
            double w = player.getWidth() / 2.0;
            double h = player.getHeight();
            this.predictedBox = new Box(px - w, py, pz - w, px + w, py + h, pz + w);
            this.predict = player;
        }
    }

    private static class PosCandidate {
        final BlockPos pos;
        final PlayerEntity enemy;
        final float enemyDamage;
        final float selfDamage;
        final boolean isBase;

        PosCandidate(BlockPos pos, PlayerEntity enemy, float enemyDamage, float selfDamage, boolean isBase) {
            this.pos = pos;
            this.enemy = enemy;
            this.enemyDamage = enemyDamage;
            this.selfDamage = selfDamage;
            this.isBase = isBase;
        }
    }

    // ── Async calculation result (immutable snapshot for lock-free handoff) ──
    private static class CalcResult {
        final BlockPos tempPos;
        final BlockPos tempBasePos;
        final EndCrystalEntity tempBreakCrystal;
        final float tempDamage;
        final float breakDamage;
        final PlayerEntity currentTarget;
        final PosCandidate lockedCandidate2;

        CalcResult(BlockPos tempPos, BlockPos tempBasePos, EndCrystalEntity tempBreakCrystal,
                   float tempDamage, float breakDamage, PlayerEntity currentTarget,
                   PosCandidate lockedCandidate2) {
            this.tempPos = tempPos;
            this.tempBasePos = tempBasePos;
            this.tempBreakCrystal = tempBreakCrystal;
            this.tempDamage = tempDamage;
            this.breakDamage = breakDamage;
            this.currentTarget = currentTarget;
            this.lockedCandidate2 = lockedCandidate2;
        }
    }

    public CrystalBot() {
        super("CrystalBot", Module.Category.Combat);
        this.setChinese("水晶光环");
        INSTANCE = this;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public String getInfo() {
        if (currentTarget != null && lastDamage > 0) {
            return df.format(lastDamage);
        }
        return null;
    }

    @Override
    public void onEnable() {
        firstBase = true;
        tempPos = null;
        tempBasePos = null;
        crystalPos = null;
        basePos = null;
        tempBreakCrystal = null;
        breakCrystal = null;
        currentTarget = null;
        tempDamage = 0;
        lastDamage = 0;
        breakDamage = 0;
        lockedCandidate1 = null;
        lockedCandidate2 = null;
        useSecondPos = false;
        placeVec3d = null;
        curVec3d = null;
        instantPendingPos = null;
        mioPendingBreakId = -1;
        currentFade = 0;
        rotationTarget = null;
        placeRotationPoint = null;
        unifiedRotationPoint = null;
        noPosTimer.reset();
        posLockTimer.reset();
        placeTimer.reset();
        breakTimer.reset();
        baseTimer.reset();
        rotationResetTimer.reset();
        // Reset async pipeline
        calcFuture = null;
        bestResult.set(null);
    }

    @Override
    public void onDisable() {
        tempPos = null;
        tempBasePos = null;
        crystalPos = null;
        basePos = null;
        tempBreakCrystal = null;
        breakCrystal = null;
        currentTarget = null;
        lockedCandidate1 = null;
        lockedCandidate2 = null;
        instantPendingPos = null;
        mioPendingBreakId = -1;
        rotationTarget = null;
        placeRotationPoint = null;
        unifiedRotationPoint = null;
        CombatUtil.modifyPos = null;
        CombatUtil.modifyBlockState = null;
    }


    @EventListener
    public void onRotate(RotationEvent event) {
        if (rotationResetTimer.passedMs(300)) {
            rotationTarget = null;
        }
        if (rotationTarget != null) {
            event.setTarget(rotationTarget, 1.0F, 100.0F);
        }
    }
    private void setRotationTarget(Vec3d pos) {
        if (pos == null) return;
        this.rotationTarget = pos;
        this.rotationResetTimer.reset();
    }
    private void setPlaceRotation(Vec3d placeVec) {
        if (placeVec == null) return;
        switch (rotationBypass.getValue()) {
            case Off:
                setRotationTarget(placeVec);
                break;
            case ExtraHitbox:
                setRotationTarget(placeVec);
                this.placeRotationPoint = placeVec;
                break;
            case Point:
                this.unifiedRotationPoint = placeVec;
                setRotationTarget(placeVec);
                break;
            case PlaceAngle:
                setRotationTarget(placeVec);
                this.placeRotationPoint = placeVec;
                break;
        }
    }
    private void setBreakRotation(Vec3d crystalVec, EndCrystalEntity crystal) {
        switch (rotationBypass.getValue()) {
            case Off:
                setRotationTarget(crystalVec);
                break;
            case ExtraHitbox:
                if (this.placeRotationPoint != null) {
                    setRotationTarget(this.placeRotationPoint);
                } else {
                    setRotationTarget(new Vec3d(
                            crystal.getX(),
                            crystal.getY() - extraHitboxExpand.getValue(),
                            crystal.getZ()
                    ));
                }
                break;
            case Point:
                if (this.unifiedRotationPoint != null) {
                    setRotationTarget(this.unifiedRotationPoint);
                } else {
                    setRotationTarget(new Vec3d(
                            crystal.getX(), crystal.getY(), crystal.getZ()
                    ));
                }
                break;
            case PlaceAngle:
                if (this.placeRotationPoint != null) {
                    setRotationTarget(this.placeRotationPoint);
                } else {
                    setRotationTarget(new Vec3d(
                            crystal.getX(), crystal.getY(), crystal.getZ()
                    ));
                }
                break;
        }
    }

    private boolean canHitWithExpandedHitbox(EndCrystalEntity crystal, Vec3d lookTarget) {
        if (!rotationBypass.is(RotationBypass.ExtraHitbox)) return true;
        Box expandedBox = crystal.getBoundingBox()
                .expand(0.0, extraHitboxExpand.getValue(), 0.0)
                .offset(0.0, -extraHitboxExpand.getValue(), 0.0);
        Vec3d eyePos = mc.player.getEyePos();
        float[] angles = calculateAngles(lookTarget);
        Vec3d lookVec = getVectorForRotation(angles[1], angles[0]);
        Vec3d reachVec = eyePos.add(lookVec.multiply(breakRange.getValue()));
        return expandedBox.raycast(eyePos, reachVec).isPresent();
    }

    private float[] calculateAngles(Vec3d target) {
        Vec3d eyePos = mc.player.getEyePos();
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90.0F, 90.0F)};
    }

    private Vec3d getVectorForRotation(float pitch, float yaw) {
        float f  = MathHelper.cos(-yaw  * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw  * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d(f1 * f2, f3, f * f2);
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck()) return;
        if (event.isPost()) return;
        if (shouldPause()) {
            crystalPos = null;
            basePos = null;
            breakCrystal = null;
            currentTarget = null;
            rotationTarget = null;
            placeRotationPoint = null;
            unifiedRotationPoint = null;
            lockedCandidate1 = null;
            lockedCandidate2 = null;
            CombatUtil.modifyPos = null;
            CombatUtil.modifyBlockState = null;
            return;
        }

        // ── 同步计算（与水晶光环+一致）：每 posUpdateDelay 主线程直接算，结果立即生效 ──
        // 原异步 VirtualThread 管线有 50~200ms 结果延迟，快速 放→破→放 循环跟不上位置/破坏目标刷新 → 循环变慢
        if (posLockTimer.passedMs(posUpdateDelay.getValue())) {
            posLockTimer.reset();
            calcCrystalPos();
            CombatUtil.modifyPos = null;
            CombatUtil.modifyBlockState = null;
        }
        placeRotationPoint = null;
        unifiedRotationPoint = null;

        doInteract();
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (nullCheck()) return;

        BlockPos cpos = crystalPos;
        if (cpos != null) {
            placeVec3d = cpos.down().toCenterPos();
            noPosTimer.reset();
        }

        if (placeVec3d == null) return;

        boolean hasPos = !noPosTimer.passed((long) (startFadeTime.getValue() * 1000.0));

        if (fadeSpeed.getValue() >= 1.0) {
            currentFade = hasPos ? 0.5 : 0.0;
        } else {
            currentFade = AnimateUtil.animate(currentFade, hasPos ? 0.5 : 0.0, fadeSpeed.getValue() / 10.0);
        }

        if (currentFade <= 0.001) {
            curVec3d = null;
            return;
        }

        if (curVec3d != null && sliderSpeed.getValue() < 1.0) {
            double sp = sliderSpeed.getValue() / 10.0;
            curVec3d = new Vec3d(
                    AnimateUtil.animate(curVec3d.x, placeVec3d.x, sp),
                    AnimateUtil.animate(curVec3d.y, placeVec3d.y, sp),
                    AnimateUtil.animate(curVec3d.z, placeVec3d.z, sp)
            );
        } else {
            curVec3d = placeVec3d;
        }

        if (!render.getValue()) return;

        Box box = new Box(curVec3d, curVec3d);
        if (shrink.getValue()) {
            box = box.expand(currentFade);
        } else {
            box = box.expand(0.5);
        }

        double alphaMultiplier = currentFade * 2.0;

        if (fillColor.booleanValue) {
            Render3DUtil.drawFill(matrixStack, box,
                    ColorUtil.injectAlpha(fillColor.getValue(),
                            (int) (fillColor.getValue().getAlpha() * alphaMultiplier)));
        }
        if (boxColor.booleanValue) {
            Render3DUtil.drawBox(matrixStack, box,
                    ColorUtil.injectAlpha(boxColor.getValue(),
                            (int) (boxColor.getValue().getAlpha() * alphaMultiplier)),
                    lineWidth.getValueFloat());
        }

        if (renderText.getValue() && lastDamage > 0 && hasPos) {
            Render3DUtil.drawText3D(df.format(lastDamage), curVec3d, textColor.getValue());
        }
    }

    // Instant模式：监听爆炸音效触发下一个放置（即时爆水晶在 onEntitySpawn 里完成）
    @EventListener
    public void onReceivePacket(PacketEvent.Receive event) {
        if (nullCheck()) return;
        if (shouldPause()) return;

        // --- Mio式高DPS：spawn包 → 伤害门控即时破水晶（打破任何对目标伤害高的水晶，不只自己放的）---
        if (event.getPacket() instanceof EntitySpawnS2CPacket spawn
                && spawn.getEntityType() == EntityType.END_CRYSTAL) {
            handleMioSpawnBreak(spawn);
        }

        // --- Instant模式：爆炸音效 → 立刻放下一个水晶 ---
        if (placeMode.getValue() == PlaceMode.Instant && instantPendingPos != null) {
            if (event.getPacket() instanceof PlaySoundS2CPacket sound) {
                // 检测爆炸音效，且位置在待放置水晶附近
                Vec3d soundPos = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
                if (soundPos.squaredDistanceTo(instantPendingPos.toCenterPos()) < 9.0) {
                    instantPendingPos = null;
                    BlockPos cp = crystalPos;
                    if (cp != null && canPlaceCrystal(cp)) {
                        doPlaceCrystal(cp);
                    }
                    return;
                }
            }
        }

    }

    // Mio式高DPS：spawn包触发，即时破水晶
    // 不做爆炸伤害计算（很贵，~200次射线/次，单机下占满主线程→tick变慢→移动变慢）。
    // 用廉价距离判定：靠近自己放置位（放置时 async calc 已保证伤害达标）或靠近目标≤1.5格的水晶 → 破。
    private void handleMioSpawnBreak(EntitySpawnS2CPacket spawn) {
        if (!instantBreak.getValue()) return;
        if (placeMode.getValue() == PlaceMode.Instant) return; // 保持旧 Instant 模式流程
        PlayerEntity tgt = currentTarget;
        if (tgt == null || !tgt.isAlive()) return;

        Vec3d pos = new Vec3d(spawn.getX(), spawn.getY(), spawn.getZ());
        if (mc.player.getEyePos().distanceTo(pos) > breakRange.getValue()) return;

        BlockPos placed = crystalPos;
        boolean nearPlaced = placed != null && pos.squaredDistanceTo(placed.toCenterPos()) <= 4.0;
        boolean nearTarget = pos.squaredDistanceTo(tgt.getPos()) <= 2.25;
        if (!nearPlaced && !nearTarget) return;

        mioPendingBreakId = spawn.getEntityId();
    }

    @EventListener
    private void onEntitySpawn(EntitySpawnedEvent event) {
        if (nullCheck()) return;
        if (shouldPause()) return;
        if (basePos != null) return;

        if (!(event.getEntity() instanceof EndCrystalEntity crystal)) return;

        // Mio式：spawn包已门控的水晶 → 立即破（executeBreak 内部含 Sequential/Strong 破后立即放）
        if (mioPendingBreakId != -1 && crystal.getId() == mioPendingBreakId) {
            mioPendingBreakId = -1;
            executeBreak(crystal);
            return;
        }

        if (explodeMode.getValue() == ExplodeMode.Normal) return;

        // Instant mode: break the crystal we just placed. Match against instantPendingPos
        // (the ACTUAL placement position) rather than crystalPos, which is refreshed from
        // the async calc result and can change mid-cycle (or, in Double mode, point at the
        // other slot). The raw EntitySpawnS2CPacket path in onReceivePacket fires at
        // handlePacket HEAD, before addEntity, so getEntityById() is null there — this
        // EntitySpawnedEvent (addEntity TAIL) is the reliable instant-break point.
        if (placeMode.getValue() == PlaceMode.Instant && instantBreak.getValue()) {
            BlockPos pending = instantPendingPos;
            if (pending != null && isInPlaceBox(pending, crystal.getPos())) {
                instantBreakNow(crystal);
            }
            return;
        }

        BlockPos pos = crystalPos;
        if (pos == null) return;
        if (isInPlaceBox(pos, crystal.getPos())) {
            executeBreak(crystal);
        }
    }

    private void instantBreakNow(EndCrystalEntity crystal) {
        if (crystal == null || !crystal.isAlive()) return;

        for (int i = 0; i < breakFactor.getValueInt(); i++) {
            if (i == 0 && rotateOnBreak.getValue()) {
                Vec3d attackVec = getAttackVec(crystal.getPos());
                Vec3d breakVec = attackVec != null ? attackVec : crystal.getPos();
                setBreakRotation(breakVec, crystal);
            }
            mc.getNetworkHandler().sendPacket(
                    PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking())
            );
        }
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.resetLastAttackedTicks();
        if (breakRemove.getValue()) {
            mc.world.removeEntity(crystal.getId(), RemovalReason.KILLED);
        }
        breakTimer.reset();
        setTick();
    }

    private boolean ircGuardShouldPause() {
        if (mc.world == null || mc.player == null) return false;
        boolean ircNearby = false;
        boolean nonIrcEnemyNearby = false;
        float range = targetRange.getValueFloat();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            if (mc.player.distanceTo(player) > range) continue;
            if (FakePlayer.isFakePlayer(player)) continue;  // local fake player — not a real enemy
            if (IrcModule.shouldProtectIrcUser(player.getGameProfile().getName())) {
                ircNearby = true;
            } else if (!Forever.FRIEND.isFriend(player)) {
                nonIrcEnemyNearby = true;
            }
        }
        return ircNearby && !nonIrcEnemyNearby;
    }

    private boolean shouldPause() {
        if (ircGuardShouldPause()) return true;
        if (phasePause.getValue() && Phase.INSTANCE != null && Phase.INSTANCE.isOn()) return true;
        if (pushPause.getValue() && AutoPush.INSTANCE != null && AutoPush.INSTANCE.isActive()) return true;

        if (usingPause.getValue() && mc.player.isUsingItem()) return true;
        if (usingPause.getValue() && mc.options.useKey.isPressed()) {
            Item mainItem = mc.player.getMainHandStack().getItem();
            Item offItem = mc.player.getOffHandStack().getItem();
            if (isUseableItem(mainItem) || isUseableItem(offItem)) {
                return true;
            }
        }
        if (guiPause.getValue() && mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen) return true;
        if (regearPause.getValue() && AutoRegear.INSTANCE != null && AutoRegear.INSTANCE.isWorking()) return true;
        return false;
    }

    private boolean isUseableItem(Item item) {
        return item == Items.GOLDEN_APPLE
                || item == Items.ENCHANTED_GOLDEN_APPLE
                || item == Items.CHORUS_FRUIT
                || item == Items.POTION
                || item == Items.BOW
                || item == Items.CROSSBOW
                || item == Items.TRIDENT
                || item == Items.EXPERIENCE_BOTTLE;
    }

    public float calculateDamage(BlockPos pos, PlayerEntity player, PlayerEntity predict) {
        return ExplosionUtil.calculateDamage(
                new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
                player, predict, 6.0f
        );
    }

    public float calculateDamage(Vec3d pos, PlayerEntity player, PlayerEntity predict) {
        return ExplosionUtil.calculateDamage(pos, player, predict, 6.0f);
    }

    private float calculateBaseDamage(BlockPos pos, PlayerEntity player, PlayerEntity predict) {
        return DamageUtils.overridingExplosionDamage(
                player, predict,
                new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
                12.0f,
                pos.down(),
                Blocks.OBSIDIAN.getDefaultState()
        );
    }

    // ── 同步计算入口（水晶光环+一致）：主线程调用 calcAndReturn，结果立即应用到字段 ──
    private void calcCrystalPos() {
        CalcResult result = calcAndReturn();
        if (result != null) {
            tempDamage = result.tempDamage;
            tempBreakCrystal = result.tempBreakCrystal;
            tempPos = result.tempPos;
            tempBasePos = result.tempBasePos;
            currentTarget = result.currentTarget;
            lockedCandidate2 = result.lockedCandidate2;
            lastDamage = tempDamage;
            breakCrystal = tempBreakCrystal;
            crystalPos = tempPos;
            basePos = tempBasePos;
        }
    }

    // ── Async VirtualThread calculation (Mio-style): runs on virtual thread, returns immutable snapshot ──
    private CalcResult calcAndReturn() {
        if (mc == null || mc.world == null || mc.player == null) {
            return new CalcResult(null, null, null, 0f, 0f, null, null);
        }

        EndCrystalEntity bestBreakCrystal = null;
        float bestBreakDamage = 0.0f;
        BlockPos bestPos = null;
        float bestDamage = 0.0f;
        BlockPos bestBasePos = null;
        float bestBaseDamage = 0.0f;
        PlayerEntity bestTarget = null;
        PosCandidate secondPos = null;

        List<PlayerEntity> enemies = CombatUtil.getEnemies(targetRange.getValueFloat());
        if (enemies.isEmpty()) {
            return new CalcResult(null, null, null, 0f, 0f, null, null);
        }

        boolean needBasePlace = basePlace.getValue()
                && getObsidianSlot() != -1;

        int enemyTicks = predict.getValue() ? predictTicks.getValueInt() : 0;

        if (autoPredict.getValue() && predict.getValue()) {
            for (PlayerEntity enemy : enemies) {
                double hSpeed = Math.sqrt(
                    (enemy.getX() - enemy.prevX) * (enemy.getX() - enemy.prevX)
                    + (enemy.getZ() - enemy.prevZ) * (enemy.getZ() - enemy.prevZ)
                );
                int auto = (int) Math.round(hSpeed * 20.0 * 0.5);
                if (auto > enemyTicks) enemyTicks = auto;
            }
            enemyTicks = Math.min(enemyTicks, predictTicks.getValueInt() * 2);
        }
        int selfTicks = selfPredict.getValue() ? selfPredictTicks.getValueInt() : 0;

        List<PlayerEntityPredict> list = new ArrayList<>();
        for (PlayerEntity enemy : enemies) {
            list.add(new PlayerEntityPredict(enemy, maxMotionXZ.getValue(), maxMotionY.getValue(), enemyTicks));
        }

        PlayerEntityPredict self = new PlayerEntityPredict(mc.player, maxMotionXZ.getValue(), maxMotionY.getValue(), selfTicks);

        // Phase 0: scan existing crystals for best break target
        for (Entity entity : Forever.THREAD.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!crystal.isAlive()) continue;

            Vec3d attackVec = getAttackVec(crystal.getPos());
            if (attackVec == null) continue;
            if (!mc.player.canSee(crystal) && mc.player.getEyePos().distanceTo(attackVec) > wallRange.getValue()) continue;

            float selfDamage = calculateDamage(
                    new BlockPosX(crystal.getPos().x - 0.5, crystal.getPos().y - 1, crystal.getPos().z - 0.5),
                    self.player, self.predict
            );

            if (selfDamage > maxSelfDamage.getValue()) continue;
            if (reserve.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - reserve.getValueFloat()) continue;

            for (PlayerEntityPredict pap : list) {
                float damage = calculateDamage(
                        new BlockPosX(crystal.getPos().x - 0.5, crystal.getPos().y - 1, crystal.getPos().z - 0.5),
                        pap.player, pap.predict
                );

                if (damage > bestBreakDamage
                        && damage >= getMinDamage(pap.player)
                        && (!balance.getValue() || damage >= selfDamage + balanceOffset.getValue())) {
                    bestBreakDamage = damage;
                    bestBreakCrystal = crystal;
                    bestTarget = pap.player;
                }
            }
        }

        // Phase 1: scan placement positions (pure crystal, no base)
        for (BlockPos pos : BlockUtil.getSphere((float) placeRange.getValue() + 1.5f)) {
            if (!canPlaceCrystal(pos)) continue;

            Vec3d attackVec = getAttackVec(pos.toBottomCenterPos());
            if (attackVec == null) continue;
            if (!isMultipointVisible(pos) && mc.player.getEyePos().distanceTo(pos.toBottomCenterPos()) > wallRange.getValue()) continue;
            if (!canTouch(pos.down(), placeRange.getValue())) continue;

            float selfDamage = calculateDamage(pos, self.player, self.predict);

            for (PlayerEntityPredict pap : list) {
                float damage = calculateDamage(pos, pap.player, pap.predict);

                if (damage > bestDamage
                        && selfDamage <= maxSelfDamage.getValue()
                        && (reserve.getValue() <= 0 || selfDamage <= mc.player.getHealth() + mc.player.getAbsorptionAmount() - reserve.getValueFloat())
                        && damage >= getMinDamage(pap.player)
                        && (!balance.getValue() || damage >= selfDamage + balanceOffset.getValue())) {
                    bestTarget = pap.player;
                    bestPos = pos;
                    bestDamage = damage;
                }
            }
        }

        // Phase 2: base-place scan (only if best crystal damage is below OverrideMax)
        if (needBasePlace && bestDamage <= overrideMax.getValue()) {
            for (BlockPos pos : BlockUtil.getSphere((float) basePlaceRange.getValue() + 1.5f)) {
                if (!BlockUtil.canPlace(pos.down())) continue;
                if (!canPlaceCrystalWithBase(pos)) continue;

                Vec3d attackVec = getAttackVec(pos.toBottomCenterPos());
                if (attackVec == null) continue;
                if (!isMultipointVisible(pos) && mc.player.getEyePos().distanceTo(pos.toBottomCenterPos()) > wallRange.getValue()) continue;
                if (!canTouch(pos.down(), basePlaceRange.getValue())) continue;

                CombatUtil.modifyPos = pos.down();
                CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                float selfDamage = calculateBaseDamage(pos, self.player, self.predict);

                for (PlayerEntityPredict pap : list) {
                    if (onlyBelow.getValue() && pos.getY() - 0.5 > pap.player.getY()) continue;

                    float damage = calculateBaseDamage(pos, pap.player, pap.predict);

                    if (damage > bestDamage
                            && damage > bestBaseDamage
                            && selfDamage <= baseMax.getValue()
                            && (reserve.getValue() <= 0 || selfDamage <= mc.player.getHealth() + mc.player.getAbsorptionAmount() - reserve.getValueFloat())
                            && (damage >= EntityUtil.getHealth(pap.player) || (damage >= baseMin.getValue() && (!baseBalance.getValue() || damage >= selfDamage)))) {
                        bestTarget = pap.player;
                        bestBaseDamage = damage;
                        bestBasePos = pos.down();
                        bestPos = null; // great base found, clear normal pos
                    }
                }
                CombatUtil.modifyPos = null;
                CombatUtil.modifyBlockState = null;
            }
        }

        // Phase 3: Double mode — find second placement position
        if (placePosMode.getValue() == PlacePosMode.Double && bestPos != null) {
            for (BlockPos pos : BlockUtil.getSphere((float) placeRange.getValue() + 1.5f)) {
                if (pos.equals(bestPos)) continue;

                CombatUtil.modifyPos = null;
                CombatUtil.modifyBlockState = null;

                if (!canPlaceCrystal(pos)) continue;

                Vec3d attackVec = getAttackVec(pos.toBottomCenterPos());
                if (attackVec == null) continue;
                if (!isMultipointVisible(pos) && mc.player.getEyePos().distanceTo(pos.toBottomCenterPos()) > wallRange.getValue()) continue;
                if (!canTouch(pos.down(), placeRange.getValue())) continue;

                float selfDamage = calculateDamage(pos, self.player, self.predict);
                if (selfDamage > maxSelfDamage.getValue()) continue;
                if (reserve.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - reserve.getValueFloat()) continue;

                for (PlayerEntityPredict pap : list) {
                    float damage = calculateDamage(pos, pap.player, pap.predict);
                    if (damage >= getMinDamage(pap.player)
                            && (!balance.getValue() || damage >= selfDamage + balanceOffset.getValue())) {
                        secondPos = new PosCandidate(pos, pap.player, damage, selfDamage, false);
                        break;
                    }
                }
                if (secondPos != null) break;
            }
        }

        return new CalcResult(bestPos, bestBasePos, bestBreakCrystal, bestDamage, bestBreakDamage, bestTarget, secondPos);
    }

    private double getMinDamage(PlayerEntity target) {
        // Mio 移植 FacePlace：目标血量低或护甲烂时强制放置/破坏（门槛降到 0.6 补刀）
        if (facePlace.getValue() && target != null
                && (EntityUtil.getHealth(target) <= facePlaceHealth.getValueFloat()
                    || getArmorDurability(target) <= facePlaceArmor.getValueFloat())) {
            return 0.6;
        }
        return minPDamage.getValue();
    }

    private double getMinDamage() {
        return getMinDamage(currentTarget);
    }

    // 最低护甲耐久%（Mio FireworksHelper.get451 移植）
    private float getArmorDurability(PlayerEntity p) {
        float min = 100f;
        for (ItemStack stack : p.getInventory().armor) {
            if (stack.isEmpty()) continue;
            float dur = 100f - (int) ((1f - ((float) (stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage())) * 100f);
            min = Math.min(min, dur);
        }
        return min;
    }

    private boolean breakBlockingCrystals(BlockPos placePos) {
        if (placePos == null) return false;
        List<BlockPos> checkPositions = new ArrayList<>();
        checkPositions.add(placePos);
        checkPositions.add(placePos.up());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos offset = placePos.add(dx, 0, dz);
                checkPositions.add(offset);
                checkPositions.add(offset.up());
            }
        }
        List<EndCrystalEntity> blockingCrystals = new ArrayList<>();
        for (Entity entity : Forever.THREAD.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!crystal.isAlive()) continue;

            BlockPos crystalBlockPos = crystal.getBlockPos();

            for (BlockPos checkPos : checkPositions) {
                Box checkBox = new Box(checkPos);
                if (crystal.getBoundingBox().intersects(checkBox)) {
                    if (!blockingCrystals.contains(crystal)) {
                        blockingCrystals.add(crystal);
                    }
                    break;
                }
            }
        }

        if (blockingCrystals.isEmpty()) return false;
        EndCrystalEntity targetCrystal = null;
        double minDist = Double.MAX_VALUE;

        for (EndCrystalEntity crystal : blockingCrystals) {
            Vec3d attackVec = getAttackVec(crystal.getPos());
            if (attackVec == null) continue;

            double dist = mc.player.getEyePos().distanceTo(attackVec);
            if (dist > breakRange.getValue()) continue;
            if (rotationBypass.is(RotationBypass.ExtraHitbox) && placeRotationPoint != null) {
                if (!canHitWithExpandedHitbox(crystal, placeRotationPoint)) continue;
            }
            if (!mc.player.canSee(crystal)) {
                if (!breakTWall.getValue()) continue;
                if (dist > bWallRange.getValue()) continue;
            }

            if (dist < minDist) {
                minDist = dist;
                targetCrystal = crystal;
            }
        }

        if (targetCrystal != null) {
            if (rotateOnBreak.getValue()) {
                Vec3d attackVec = getAttackVec(targetCrystal.getPos());
                Vec3d breakVec = attackVec != null ? attackVec : targetCrystal.getPos();
                setBreakRotation(breakVec, targetCrystal);
            }

            for (int i = 0; i < breakFactor.getValueInt(); i++) {
                mc.getNetworkHandler().sendPacket(
                        PlayerInteractEntityC2SPacket.attack(targetCrystal, mc.player.isSneaking())
                );
            }
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.resetLastAttackedTicks();

            if (breakRemove.getValue()) {
                mc.world.removeEntity(targetCrystal.getId(), RemovalReason.KILLED);
            }

            breakTimer.reset();
            return true;
        }

        return false;
    }

    private void doInteract() {
        BlockPos cp = crystalPos;
        BlockPos bp = basePos;

        if (cp == null && breakCrystal == null && bp == null) return;

        if (bp != null) {
            Vec3d baseRotateVec = getBaseRotationVec(bp);
            if (baseRotateVec != null) {
                setRotationTarget(baseRotateVec);
            }
            doPlaceBase(bp);
            return;
        }

        if (breakCrystal != null) {
            if (rotateOnBreak.getValue()) {
                Vec3d attackVec = getAttackVec(breakCrystal.getPos());
                Vec3d breakVec = attackVec != null ? attackVec : breakCrystal.getPos();
                setBreakRotation(breakVec, breakCrystal);
            }
            doBreakCrystal(breakCrystal);
            breakCrystal = null;

            if (placePosMode.getValue() == PlacePosMode.Double) {
                useSecondPos = !useSecondPos;
            }
        }
        BlockPos activePlacePos = cp;
        if (placePosMode.getValue() == PlacePosMode.Double && useSecondPos && lockedCandidate2 != null) {
            activePlacePos = lockedCandidate2.pos;
        }

        // 放置位置超出触摸范围 → 跳过放置，不旋转(避免AutoWalk跟随)，等玩家靠近
        if (activePlacePos == null || !canTouch(activePlacePos.down(), placeRange.getValue())) {
            rotationTarget = null;
            return;
        }

        if (breakBlockingCrystals(activePlacePos)) {
            return;
        }

        if (rotateOnPlace.getValue()) {
            setPlaceRotation(getPlaceRotationVec(activePlacePos));
        }
        doPlaceCrystal(activePlacePos);
    }

    private Vec3d getBaseRotationVec(BlockPos pos) {
        Direction side = BlockUtil.getClickSide(pos);
        if (side == null) {
            return pos.toCenterPos();
        }
        return pos.toCenterPos().add(
                side.getVector().getX() * 0.5,
                side.getVector().getY() * 0.5,
                side.getVector().getZ() * 0.5
        );
    }

    private void doPlaceBase(BlockPos obsPos) {
        if (!firstBase && !baseTimer.passedMs(baseDelay.getValue())) return;
        if (BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN || BlockUtil.getBlock(obsPos) == Blocks.BEDROCK) return;
        if (!BlockUtil.canPlace(obsPos)) return;

        if (detectMining.getValue()) {
            BlockPos breakPos = SpeedMine.getBreakPos();
            if (breakPos != null && breakPos.equals(obsPos)) return;
        }

        int obbySlot = getObsidianSlot();
        if (obbySlot == -1) return;

        int oldSlot = mc.player.getInventory().selectedSlot;

        if (!baseInventorySwap.getValue()) {
            InventoryUtil.switchToSlot(obbySlot < 36 ? obbySlot : obbySlot - 36);
            BlockUtil.placeBlock(obsPos, false);
            InventoryUtil.switchToSlot(oldSlot);
        } else {
            boolean bypass = AntiCheat.INSTANCE != null && AntiCheat.INSTANCE.invSwapBypass.getValue();

            if (obbySlot >= 36) {
                InventoryUtil.inventorySwap(obbySlot, oldSlot);
                BlockUtil.placeBlock(obsPos, false);
                InventoryUtil.inventorySwap(obbySlot, oldSlot);
            } else {
                if (bypass) {
                    mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(obbySlot));
                    BlockUtil.placeBlock(obsPos, false);
                    InventoryUtil.switchToSlot(oldSlot);
                } else {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, obbySlot, oldSlot, SlotActionType.SWAP, mc.player);
                    mc.player.getInventory().updateItems();

                    BlockUtil.placeBlock(obsPos, false);

                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, obbySlot, oldSlot, SlotActionType.SWAP, mc.player);
                    mc.player.getInventory().updateItems();
                }
            }
            EntityUtil.syncInventory();
        }

        firstBase = false;
        baseTimer.reset();
    }

    private void doPlaceCrystal(BlockPos pos) {
        if (placeMode.getValue() == PlaceMode.None) return;
        if (!placeTimer.passedMs(placeDelay.getValue())) return;
        boolean isInstant = placeMode.getValue() == PlaceMode.Instant;
        // Instant模式：跳过hasCrystalAt检查（本地已删实体），仅检查底座
        if (!isInstant && hasCrystalAt(pos)) return;
        if (!canPlaceCrystal(pos)) return;
        if (!canTouch(pos.down(), placeRange.getValue())) return;

        boolean mainHand = mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL;
        boolean offHand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;

        int crystalSlot = -1;
        if (!mainHand && !offHand) {
            if (autoSwap.getValue() == SwapMode.None) return;
            crystalSlot = getCrystalSlot();
            if (crystalSlot == -1) return;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;
        if (crystalSlot != -1) {
            // Mio 移植 SilentMode：Normal=切槽 / Alternative=库存静默互换 / Pick=选格
            if (autoSwap.getValue() == SwapMode.Silent && silentMode.is(SilentMode.Alternative)) {
                InventoryUtil.inventorySwap(crystalSlot, oldSlot);
            } else if (autoSwap.getValue() == SwapMode.Silent && silentMode.is(SilentMode.Pick)) {
                mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(rawInventorySlot(crystalSlot)));
            } else {
                doSwap(crystalSlot, autoSwap.getValue());
            }
        }

        BlockPos obsPos = pos.down();
        Direction facing = BlockUtil.getClickSide(obsPos);
        if (facing == null) facing = Direction.UP;

        Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        if (crystalSlot != -1) hand = Hand.MAIN_HAND;

        BlockUtil.clickBlock(obsPos, facing, false, hand, SwingSide.All);

        // Instant模式：放完立刻本地删除水晶实体，骗过hasCrystalAt，等待爆炸音效触发下一个
        if (isInstant) {
            instantPendingPos = pos;
            removeLocalCrystalAt(pos);
        }

        if (crystalSlot != -1) {
            if (autoSwap.getValue() == SwapMode.Silent && silentMode.is(SilentMode.Alternative)) {
                InventoryUtil.inventorySwap(crystalSlot, oldSlot);
            } else if (autoSwap.getValue() == SwapMode.Silent && silentMode.is(SilentMode.Pick)) {
                mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(rawInventorySlot(crystalSlot)));
            } else if (autoSwap.getValue() == SwapMode.Silent) {
                InventoryUtil.switchToSlot(oldSlot);
            } else if (autoSwap.getValue() == SwapMode.Inventory) {
                InventoryUtil.inventorySwap(crystalSlot, mc.player.getInventory().selectedSlot);
                EntityUtil.syncInventory();
            }
        }

        placeTimer.reset();
    }

    // 本地删除pos上的水晶实体
    private void removeLocalCrystalAt(BlockPos pos) {
        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
        for (Entity entity : Forever.THREAD.getEntities()) {
            if (entity instanceof EndCrystalEntity && entity.getBoundingBox().intersects(box)) {
                mc.world.removeEntity(entity.getId(), RemovalReason.KILLED);
            }
        }
    }

    private void doBreakCrystal(EndCrystalEntity crystal) {
        if (crystal == null || !crystal.isAlive()) return;
        if (!breakTimer.passedMs(breakDelay.getValue())) return;
        if (!canReachForBreak(crystal)) return;
        executeBreak(crystal);
    }

    private void executeBreak(EndCrystalEntity crystal) {
        if (crystal == null || !crystal.isAlive()) return;

        for (int i = 0; i < breakFactor.getValueInt(); i++) {
            mc.getNetworkHandler().sendPacket(
                    PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking())
            );
        }
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.resetLastAttackedTicks();

        if (breakRemove.getValue()) {
            mc.world.removeEntity(crystal.getId(), RemovalReason.KILLED);
        }

        breakTimer.reset();
        setTick();

        // Sequential: place next crystal immediately after breaking
        if (explodeMode.getValue() == ExplodeMode.Sequential) {
            BlockPos cp = crystalPos;
            if (cp != null) {
                doPlaceCrystal(cp);
            }
        } else if (placeMode.getValue() == PlaceMode.Strong) {
            BlockPos cp = crystalPos;
            if (cp != null) {
                doPlaceCrystal(cp);
            }
        }
    }

    private Vec3d getAttackVec(Vec3d feetPos) {
        return MathUtil.getPointToBoxFromBottom(mc.player.getEyePos(), feetPos, breakRange.getValue(), 2.0, 0.1);
    }

    public boolean canPlaceCrystal(BlockPos pos) {
        BlockPos obsPos = pos.down();
        // ForcePlace: 跳过底座方块检查（适用于方块错位/ghost block场景）
        if (!forcePlace.getValue()) {
            if (BlockUtil.getBlock(obsPos) != Blocks.BEDROCK && BlockUtil.getBlock(obsPos) != Blocks.OBSIDIAN) return false;
        }
        if (BlockUtil.getClickSideStrict(obsPos) == null) return false;
        if (!mc.world.isAir(pos) && BlockUtil.getBlock(pos) != Blocks.FIRE) return false;
        if (ClientSetting.INSTANCE.lowVersion.getValue() && !mc.world.isAir(pos.up())) return false;
        if (!noEntityBlockCrystal(pos)) return false;
        if (!noEntityBlockCrystal(pos.up())) return false;
        return true;
    }

    private boolean canPlaceCrystalWithBase(BlockPos pos) {
        if (!mc.world.isAir(pos) && BlockUtil.getBlock(pos) != Blocks.FIRE) return false;
        if (ClientSetting.INSTANCE.lowVersion.getValue() && !mc.world.isAir(pos.up())) return false;
        if (!noEntityBlockCrystal(pos)) return false;
        if (!noEntityBlockCrystal(pos.up())) return false;
        return true;
    }

    private boolean noEntityBlockCrystal(BlockPos pos) {
        for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
            if (!entity.isAlive()) continue;
            if (entity instanceof ItemEntity) continue;
            if (entity instanceof EndCrystalEntity) continue;
            return false;
        }
        return true;
    }

    private boolean behindWall(BlockPos pos, Vec3d attackVec) {
        Vec3d crystalEyePos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.7, pos.getZ() + 0.5);
        net.minecraft.util.hit.HitResult result = mc.world.raycast(
                new net.minecraft.world.RaycastContext(
                        mc.player.getEyePos(),
                        crystalEyePos,
                        net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                        net.minecraft.world.RaycastContext.FluidHandling.NONE,
                        mc.player
                )
        );
        if (result == null || result.getType() == net.minecraft.util.hit.HitResult.Type.MISS) return false;
        return mc.player.getEyePos().distanceTo(attackVec) > wallRange.getValue();
    }

    // ── Mio 移植 MultiPoint：方块内多点射线可见性（FAST=中心 / X2 / X4=四角 / X8=八角）──
    private boolean isMultipointVisible(BlockPos pos) {
        for (Vec3d v : getMultiPointList(pos)) {
            if (isTraceVisible(mc.player.getEyePos(), v)) return true;
        }
        return false;
    }

    private boolean isTraceVisible(Vec3d start, Vec3d end) {
        if (end.distanceTo(start) > 256) return false;
        net.minecraft.util.hit.BlockHitResult hit = mc.world.raycast(
                new net.minecraft.world.RaycastContext(
                        start, end,
                        net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                        net.minecraft.world.RaycastContext.FluidHandling.NONE,
                        mc.player
                )
        );
        return hit.getType() == net.minecraft.util.hit.HitResult.Type.MISS
                || hit.getPos().equals(end)
                || hit.getBlockPos().equals(BlockPos.ofFloored(end));
    }

    private List<Vec3d> getMultiPointList(BlockPos pos) {
        List<Vec3d> base;
        switch (multipoint.getValue()) {
            case Fast:
                base = List.of(new Vec3d(0.5, 0.5, 0.5));
                break;
            case X2:
                base = List.of(new Vec3d(0.5, 0.05, 0.5), new Vec3d(0.5, 1.0, 0.5));
                break;
            case X4:
                base = List.of(new Vec3d(0.05, 0.05, 0.05), new Vec3d(0.05, 0.05, 0.95),
                        new Vec3d(0.95, 0.05, 0.05), new Vec3d(0.95, 0.05, 0.95));
                break;
            default:
                base = List.of(new Vec3d(0.05, 0.05, 0.05), new Vec3d(0.05, 0.05, 0.95),
                        new Vec3d(0.05, 0.95, 0.05), new Vec3d(0.05, 0.95, 0.95),
                        new Vec3d(0.95, 0.05, 0.05), new Vec3d(0.95, 0.05, 0.95),
                        new Vec3d(0.95, 0.95, 0.05), new Vec3d(0.95, 0.95, 0.95));
        }
        List<Vec3d> list = new ArrayList<>();
        for (Vec3d v : base) {
            Vec3d add = v.add(pos.getX(), pos.getY(), pos.getZ());
            if (multipoint.is(MultiPoint.X4) && mc.player.getEyeY() > pos.getY() + 1) {
                add = add.add(0, 0.9, 0);
            }
            list.add(add);
        }
        return list;
    }

    private boolean canTouch(BlockPos pos, double range) {
        Direction side = BlockUtil.getClickSideStrict(pos);
        if (side == null) return false;
        return pos.toCenterPos()
                .add(side.getVector().getX() * 0.5,
                        side.getVector().getY() * 0.5,
                        side.getVector().getZ() * 0.5)
                .distanceTo(mc.player.getEyePos()) <= range;
    }

    private boolean canReachForBreak(EndCrystalEntity crystal) {
        Vec3d attackVec = getAttackVec(crystal.getPos());
        if (attackVec == null) return false;
        double dist = mc.player.getEyePos().distanceTo(attackVec);
        if (dist > breakRange.getValue()) return false;
        if (rotationBypass.is(RotationBypass.ExtraHitbox) && placeRotationPoint != null) {
            if (!canHitWithExpandedHitbox(crystal, placeRotationPoint)) return false;
        }

        if (!mc.player.canSee(crystal)) {
            if (breakTWall.getValue()) return dist <= bWallRange.getValue();
            return false;
        }
        return true;
    }

    private boolean hasCrystalAt(BlockPos pos) {
        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
        for (Entity entity : Forever.THREAD.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal && crystal.isAlive()) {
                if (crystal.getBoundingBox().intersects(box)) return true;
            }
        }
        return false;
    }

    private boolean isInPlaceBox(BlockPos placePos, Vec3d crystalPos) {
        Box box = new Box(placePos.getX(), placePos.getY(), placePos.getZ(),
                placePos.getX() + 1, placePos.getY() + 2, placePos.getZ() + 1);
        return box.contains(crystalPos);
    }

    private Vec3d getPlaceRotationVec(BlockPos pos) {
        BlockPos obsPos = pos.down();
        Direction side = BlockUtil.getClickSide(obsPos);
        if (side == null) return obsPos.toCenterPos().add(0, 0.5, 0);
        return obsPos.toCenterPos().add(
                side.getVector().getX() * 0.5,
                side.getVector().getY() * 0.5,
                side.getVector().getZ() * 0.5
        );
    }

    private int getCrystalSlot() {
        if (autoSwap.getValue() == SwapMode.Silent) {
            // Mio 移植：Alternative/Pick 需要全背包找（0-44），Normal 只需热键栏
            if (silentMode.is(SilentMode.Alternative) || silentMode.is(SilentMode.Pick)) {
                return InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL);
            }
            return InventoryUtil.findItem(Items.END_CRYSTAL);
        } else if (autoSwap.getValue() == SwapMode.Normal) {
            return InventoryUtil.findItem(Items.END_CRYSTAL);
        } else if (autoSwap.getValue() == SwapMode.Inventory) {
            return InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL);
        }
        return -1;
    }

    private int rawInventorySlot(int slot) {
        return slot >= 36 ? slot - 36 : slot;
    }

    private int getObsidianSlot() {
        return baseInventorySwap.getValue()
                ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN)
                : InventoryUtil.findBlock(Blocks.OBSIDIAN);
    }

    private void doSwap(int slot, SwapMode mode) {
        if (mode == SwapMode.Silent || mode == SwapMode.Normal) {
            InventoryUtil.switchToSlot(slot);
        } else if (mode == SwapMode.Inventory) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        }
    }

    public BlockPos getPlacePos() {
        return crystalPos;
    }
    private enum SpoofMode {
        Crystal, Totem
    }
    public ItemStack getSpoofItem() {
        if (!isOn()) return null;
        if (!itemSpoof.getValue()) return null;
        if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND) return null;
        if (onlyActive.getValue() && currentTarget == null) return null;

        return spoofMode.is(SpoofMode.Crystal)
                ? new ItemStack(Items.END_CRYSTAL)
                : new ItemStack(Items.TOTEM_OF_UNDYING);
    }

    public float getCurrentDamage() {
        return lastDamage;
    }

    public void resetBreakTimer() {
        this.breakTimer.reset();
    }

    public void setTick() {
        if (syncWeb.getValue() && AutoWeb.INSTANCE != null && AutoWeb.INSTANCE.isOn()) {
            AutoWeb.force = true;
        }
    }
}
