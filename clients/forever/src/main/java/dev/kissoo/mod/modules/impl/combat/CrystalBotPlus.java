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
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CrystalBotPlus extends Module {

    public static CrystalBotPlus INSTANCE;

    private enum Page {
        Place, Break, Base, Rotation, RayTrace, Prediction, Misc, Render
    }

    private enum PlaceMode {
        None, Weak, Strong
    }

    private enum ExplodeMode {
        Normal, Sync, Both
    }

    private enum PlacePosMode {
        Single, Double
    }

    private enum SwapMode {
        None, Normal, Silent, Inventory
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

    private static class PlayerEntityPredict {
        final PlayerEntity player;
        final PlayerEntity predict;
        final Vec3d predictedPos;
        final Box predictedBox;

        PlayerEntityPredict(PlayerEntity player, double maxMotionY, int ticks) {
            this.player = player;
            double mxz = 0.6;
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

    public CrystalBotPlus() {
        super("CrystalBot+", Module.Category.Combat);
        this.setChinese("水晶光环+");
        INSTANCE = this;
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

        if (posLockTimer.passedMs(posUpdateDelay.getValue())) {
            posLockTimer.reset();
            calcCrystalPos();
            CombatUtil.modifyPos = null;
            CombatUtil.modifyBlockState = null;
            lastDamage = tempDamage;
            breakCrystal = tempBreakCrystal;
            crystalPos = tempPos;
            basePos = tempBasePos;
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

    @EventListener
    private void onEntitySpawn(EntitySpawnedEvent event) {
        if (nullCheck()) return;
        if (shouldPause()) return;
        if (basePos != null) return; // 处于放底座状态时，不被生成的实体打断

        if (!(event.getEntity() instanceof EndCrystalEntity crystal)) return;
        if (explodeMode.getValue() == ExplodeMode.Normal) return;
        BlockPos pos = crystalPos;
        if (pos == null) return;
        if (isInPlaceBox(pos, crystal.getPos())) {
            executeBreak(crystal);
        }
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

    private void calcCrystalPos() {
        tempBreakCrystal = null;
        breakDamage = 0.0f;
        tempPos = null;
        tempDamage = 0.0f;
        tempBasePos = null;
        float baseDamage = 0.0f;

        List<PlayerEntity> enemies = CombatUtil.getEnemies(targetRange.getValueFloat());
        if (enemies.isEmpty()) {
            currentTarget = null;
            return;
        }

        boolean needBasePlace = basePlace.getValue()
                && getObsidianSlot() != -1;

        int enemyTicks = predict.getValue() ? predictTicks.getValueInt() : 0;
        int selfTicks = selfPredict.getValue() ? selfPredictTicks.getValueInt() : 0;

        List<PlayerEntityPredict> list = new ArrayList<>();
        for (PlayerEntity enemy : enemies) {
            list.add(new PlayerEntityPredict(enemy, maxMotionY.getValue(), enemyTicks));
        }

        PlayerEntityPredict self = new PlayerEntityPredict(mc.player, maxMotionY.getValue(), selfTicks);

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

                if (damage > breakDamage
                        && damage >= getMinDamage()
                        && (!balance.getValue() || damage >= selfDamage + balanceOffset.getValue())) {
                    breakDamage = damage;
                    tempBreakCrystal = crystal;
                    currentTarget = pap.player;
                }
            }
        }

        // 第一阶段：先找出纯粹的水晶最佳位置
        for (BlockPos pos : BlockUtil.getSphere((float) placeRange.getValue() + 1.5f)) {
            if (!canPlaceCrystal(pos)) continue;

            Vec3d attackVec = getAttackVec(pos.toBottomCenterPos());
            if (attackVec == null) continue;
            if (behindWall(pos, attackVec)) continue;
            if (!canTouch(pos.down(), placeRange.getValue())) continue;

            float selfDamage = calculateDamage(pos, self.player, self.predict);

            for (PlayerEntityPredict pap : list) {
                float damage = calculateDamage(pos, pap.player, pap.predict);

                if (damage > tempDamage
                        && selfDamage <= maxSelfDamage.getValue()
                        && (reserve.getValue() <= 0 || selfDamage <= mc.player.getHealth() + mc.player.getAbsorptionAmount() - reserve.getValueFloat())
                        && damage >= getMinDamage()
                        && (!balance.getValue() || damage >= selfDamage + balanceOffset.getValue())) {
                    currentTarget = pap.player;
                    tempPos = pos;
                    tempDamage = damage;
                }
            }
        }

        // 第二阶段：只有在【已有普通水晶最大伤害】小于等于 overrideMax 时，才考虑放底座
        if (needBasePlace && tempDamage <= overrideMax.getValue()) {
            for (BlockPos pos : BlockUtil.getSphere((float) basePlaceRange.getValue() + 1.5f)) {
                if (!BlockUtil.canPlace(pos.down())) continue;
                if (!canPlaceCrystalWithBase(pos)) continue;

                Vec3d attackVec = getAttackVec(pos.toBottomCenterPos());
                if (attackVec == null) continue;
                if (behindWall(pos, attackVec)) continue;
                if (!canTouch(pos.down(), basePlaceRange.getValue())) continue;

                CombatUtil.modifyPos = pos.down();
                CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                float selfDamage = calculateBaseDamage(pos, self.player, self.predict);

                for (PlayerEntityPredict pap : list) {
                    if (onlyBelow.getValue() && pos.getY() - 0.5 > pap.player.getY()) continue;

                    float damage = calculateBaseDamage(pos, pap.player, pap.predict);

                    if (damage > tempDamage
                            && damage > baseDamage
                            && selfDamage <= baseMax.getValue()
                            && (reserve.getValue() <= 0 || selfDamage <= mc.player.getHealth() + mc.player.getAbsorptionAmount() - reserve.getValueFloat())
                            && (damage >= EntityUtil.getHealth(pap.player) || (damage >= baseMin.getValue() && (!baseBalance.getValue() || damage >= selfDamage)))) {
                        currentTarget = pap.player;
                        baseDamage = damage;
                        tempBasePos = pos.down();
                        tempPos = null; // 找到极佳的底座，清空普通位置
                    }
                }
                CombatUtil.modifyPos = null;
                CombatUtil.modifyBlockState = null;
            }
        }

        if (placePosMode.getValue() == PlacePosMode.Double) {
            recalcSecondPos(list, self, baseDamage);
        }
    }

    private void recalcSecondPos(List<PlayerEntityPredict> list, PlayerEntityPredict self, float baseDamage) {
        lockedCandidate2 = null;
        if (tempPos == null) return;

        for (BlockPos pos : BlockUtil.getSphere((float) placeRange.getValue() + 1.5f)) {
            if (pos.equals(tempPos)) continue;

            CombatUtil.modifyPos = null;
            CombatUtil.modifyBlockState = null;

            if (!canPlaceCrystal(pos)) continue;

            Vec3d attackVec = getAttackVec(pos.toBottomCenterPos());
            if (attackVec == null) continue;
            if (behindWall(pos, attackVec)) continue;
            if (!canTouch(pos.down(), placeRange.getValue())) continue;

            float selfDamage = calculateDamage(pos, self.player, self.predict);
            if (selfDamage > maxSelfDamage.getValue()) continue;
            if (reserve.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - reserve.getValueFloat()) continue;

            for (PlayerEntityPredict pap : list) {
                float damage = calculateDamage(pos, pap.player, pap.predict);
                if (damage >= getMinDamage()
                        && (!balance.getValue() || damage >= selfDamage + balanceOffset.getValue())) {
                    lockedCandidate2 = new PosCandidate(pos, pap.player, damage, selfDamage, false);
                    return;
                }
            }
        }
    }

    private double getMinDamage() {
        return minPDamage.getValue();
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

            mc.getNetworkHandler().sendPacket(
                    PlayerInteractEntityC2SPacket.attack(targetCrystal, mc.player.isSneaking())
            );
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
        if (activePlacePos != null) {
            if (breakBlockingCrystals(activePlacePos)) {
                return;
            }
        }

        if (activePlacePos != null) {
            if (rotateOnPlace.getValue()) {
                setPlaceRotation(getPlaceRotationVec(activePlacePos));
            }
            doPlaceCrystal(activePlacePos);
        }
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
        if (hasCrystalAt(pos)) return;
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
            doSwap(crystalSlot, autoSwap.getValue());
        }

        BlockPos obsPos = pos.down();
        Direction facing = BlockUtil.getClickSide(obsPos);
        if (facing == null) facing = Direction.UP;

        Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        if (crystalSlot != -1) hand = Hand.MAIN_HAND;

        BlockUtil.clickBlock(obsPos, facing, false, hand, SwingSide.All);

        if (crystalSlot != -1) {
            if (autoSwap.getValue() == SwapMode.Silent) {
                InventoryUtil.switchToSlot(oldSlot);
            } else if (autoSwap.getValue() == SwapMode.Inventory) {
                InventoryUtil.inventorySwap(crystalSlot, mc.player.getInventory().selectedSlot);
                EntityUtil.syncInventory();
            }
        }

        placeTimer.reset();
    }

    private void doBreakCrystal(EndCrystalEntity crystal) {
        if (crystal == null || !crystal.isAlive()) return;
        if (!breakTimer.passedMs(breakDelay.getValue())) return;
        if (!canReachForBreak(crystal)) return;
        executeBreak(crystal);
    }

    private void executeBreak(EndCrystalEntity crystal) {
        if (crystal == null || !crystal.isAlive()) return;

        mc.getNetworkHandler().sendPacket(
                PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking())
        );
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.resetLastAttackedTicks();

        if (breakRemove.getValue()) {
            mc.world.removeEntity(crystal.getId(), RemovalReason.KILLED);
        }

        breakTimer.reset();
        setTick();

        if (placeMode.getValue() == PlaceMode.Strong) {
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
        if (BlockUtil.getBlock(obsPos) != Blocks.BEDROCK && BlockUtil.getBlock(obsPos) != Blocks.OBSIDIAN) return false;
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
        if (autoSwap.getValue() == SwapMode.Silent || autoSwap.getValue() == SwapMode.Normal) {
            return InventoryUtil.findItem(Items.END_CRYSTAL);
        } else if (autoSwap.getValue() == SwapMode.Inventory) {
            return InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL);
        }
        return -1;
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
