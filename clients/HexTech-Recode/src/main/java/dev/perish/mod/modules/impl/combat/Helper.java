package dev.perish.mod.modules.impl.combat;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.combat.CombatUtil;
import dev.perish.api.utils.entity.CopyPlayerEntity;
import dev.perish.api.utils.math.ExplosionUtil;
import dev.perish.api.utils.math.Timer;
import dev.perish.api.utils.path.AStarCustomPathFinder;
import dev.perish.api.utils.path.PathUtils;
import dev.perish.api.utils.path.Vec3;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.render.Render3DUtil;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.movement.moveup.Util;
import dev.perish.mod.modules.impl.player.SpeedMine;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.ColorSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;

public class Helper extends Module {
    public static Helper INSTANCE;
    private final Timer updateTimer = new Timer();
    private final EnumSetting<Page> page = this.add(new EnumSetting<>("Page", Page.General));
    private final SliderSetting targetRange = this.add(
            new SliderSetting("TargetRange", 8.0, 1.0, 16.0, 0.1, () -> this.page.is(Page.General)).setSuffix("m")
    );
    private final SliderSetting searchRange = this.add(
            new SliderSetting("SearchRange", 4.0, 1.0, 8.0, 1.0, () -> this.page.is(Page.General)).setSuffix("m")
    );
    private final SliderSetting updateDelay = this.add(
            new SliderSetting("UpdateDelay", 75.0, 0.0, 500.0, 1.0, () -> this.page.is(Page.General)).setSuffix("ms")
    );
    private final BooleanSetting damage = this.add(new BooleanSetting("Damage", true, () -> this.page.is(Page.General)));
    private final SliderSetting minDamage = this.add(
            new SliderSetting("MinDamage", 4.0, 0.0, 20.0, 0.1, () -> this.page.is(Page.General) && this.damage.getValue()).setSuffix("dmg")
    );
    private final SliderSetting maxSelf = this.add(
            new SliderSetting("MaxSelf", 10.0, 0.0, 20.0, 0.1, () -> this.page.is(Page.General)).setSuffix("dmg")
    );
    private final SliderSetting attackWeight = this.add(
            new SliderSetting("AttackWeight", 1.2, 0.0, 4.0, 0.1, () -> this.page.is(Page.General))
    );
    private final SliderSetting dangerWeight = this.add(
            new SliderSetting("DangerWeight", 1.5, 0.0, 4.0, 0.1, () -> this.page.is(Page.General))
    );
    private final SliderSetting mineWeight = this.add(
            new SliderSetting("MineWeight", 1.0, 0.0, 4.0, 0.1, () -> this.page.is(Page.General))
    );
    private final SliderSetting moveWeight = this.add(
            new SliderSetting("MoveWeight", 0.55, 0.0, 2.0, 0.05, () -> this.page.is(Page.General))
    );
    private final SliderSetting aggression = this.add(
            new SliderSetting("Aggression", 1.0, 0.0, 3.0, 0.1, () -> this.page.is(Page.General))
    );
    private final BooleanSetting render = this.add(new BooleanSetting("Render", true, () -> this.page.is(Page.Render)));
    private final BooleanSetting route = this.add(
            new BooleanSetting("Route", true, () -> this.page.is(Page.Render) && this.render.getValue())
    );
    private final SliderSetting lineWidth = this.add(
            new SliderSetting("LineWidth", 2.0, 0.5, 4.0, 0.1, () -> this.page.is(Page.Render) && this.render.getValue())
    );
    private final ColorSetting routeColor = this.add(
            new ColorSetting("RouteColor", new Color(91, 179, 255, 180), () -> this.page.is(Page.Render) && this.render.getValue())
    );
    private final ColorSetting positionBox = this.add(
            new ColorSetting("PosBox", new Color(80, 220, 120, 180), () -> this.page.is(Page.Render) && this.render.getValue())
    );
    private PlayerEntity target;
    private Vec3d bestPos;
    private BlockPos bestFeetPos;
    private List<Vec3d> routePoints = new ArrayList<>();
    private boolean selfBurrowed;

    public Helper() {
        super("Helper", Module.Category.Combat);
        this.setChinese("战斗助手");
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        this.clearRoute();
    }

    @Override
    public void onLogin() {
        this.clearRoute();
    }

    @Override
    public String getInfo() {
        return this.target == null ? null : this.target.getName().getString();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) {
            this.clearRoute();
            return;
        }
        if (!this.updateTimer.passedMs(this.updateDelay.getValue())) {
            return;
        }
        this.updateTimer.reset();
        this.recalculate();
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (!this.render.getValue()) {
            return;
        }

        if (this.route.getValue() && this.routePoints.size() > 1) {
            Vec3d last = this.routePoints.getFirst();
            for (int i = 1; i < this.routePoints.size(); i++) {
                Vec3d next = this.routePoints.get(i);
                Render3DUtil.drawLine(
                        last.x,
                        last.y + 0.05,
                        last.z,
                        next.x,
                        next.y + 0.05,
                        next.z,
                        this.routeColor.getValue(),
                        this.lineWidth.getValueFloat()
                );
                last = next;
            }
        }

        if (this.bestFeetPos != null) {
            Render3DUtil.drawBox(matrixStack, new Box(this.bestFeetPos), this.positionBox.getValue(), this.lineWidth.getValueFloat());
        }
    }

    private void recalculate() {
        this.clearRoute();
        this.target = CombatUtil.getClosestEnemy(this.targetRange.getValue());
        if (this.target == null) {
            return;
        }

        boolean targetBurrowed = this.isBurrowed(this.target);
        this.selfBurrowed = this.isBurrowed(mc.player);
        List<BlockPos> crystalSpots = this.collectCrystalSpots(this.target);
        List<BlockPos> mineSpots = this.collectMineSpots(this.target, targetBurrowed);
        List<EndCrystalEntity> liveCrystals = this.collectLiveCrystals();
        ScoreData best = this.scoreCandidate(mc.player.getPos(), crystalSpots, mineSpots, liveCrystals, targetBurrowed);

        for (Vec3d candidate : this.collectCandidatePositions()) {
            ScoreData data = this.scoreCandidate(candidate, crystalSpots, mineSpots, liveCrystals, targetBurrowed);
            if (best == null || data.score > best.score) {
                best = data;
            }
        }

        if (best == null) {
            return;
        }

        this.bestPos = best.position;
        this.bestFeetPos = BlockPos.ofFloored(best.position);
        this.routePoints = this.buildPath(best.position);
    }

    private void clearRoute() {
        this.target = null;
        this.bestPos = null;
        this.bestFeetPos = null;
        this.routePoints = new ArrayList<>();
        this.selfBurrowed = false;
    }

    private List<Vec3d> collectCandidatePositions() {
        Set<BlockPos> positions = new LinkedHashSet<>();
        BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());
        int search = this.searchRange.getValueInt();
        int[] yOffsets = new int[]{0, 1, -1};

        for (int x = -search; x <= search; x++) {
            for (int z = -search; z <= search; z++) {
                if (Math.sqrt(x * x + z * z) > this.searchRange.getValue() + 0.35) {
                    continue;
                }

                for (int yOffset : yOffsets) {
                    int y = playerPos.getY() + yOffset;
                    if (AStarCustomPathFinder.checkPositionValidity(playerPos.getX() + x, y, playerPos.getZ() + z, true)) {
                        positions.add(new BlockPos(playerPos.getX() + x, y, playerPos.getZ() + z));
                        break;
                    }
                }
            }
        }

        List<Vec3d> candidates = new ArrayList<>();
        for (BlockPos pos : positions) {
            candidates.add(new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        }
        return candidates;
    }

    private List<BlockPos> collectCrystalSpots(PlayerEntity target) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        BlockPos base = EntityUtil.getEntityPos(target, true);
        int range = Math.max(2, (int) Math.ceil(this.getCrystalPlaceRange()) + 1);
        AutoCrystal autoCrystal = AutoCrystal.INSTANCE;

        for (int x = -range; x <= range; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = base.add(x, y, z);
                    if (pos.toCenterPos().distanceTo(target.getPos()) > this.getCrystalPlaceRange() + 2.0) {
                        continue;
                    }
                    boolean canPlace = autoCrystal != null ? autoCrystal.canPlaceCrystal(pos, false, true) : BlockUtil.canPlaceCrystal(pos);
                    if (canPlace) {
                        positions.add(pos);
                    }
                }
            }
        }

        return new ArrayList<>(positions);
    }

    private List<BlockPos> collectMineSpots(PlayerEntity target, boolean targetBurrowed) {
        Set<BlockPos> spots = new LinkedHashSet<>();
        BlockPos base = EntityUtil.getEntityPos(target, true);
        if (targetBurrowed) {
            this.addMineSpot(spots, base);
        }

        this.addMineSpot(spots, base.up(2));
        for (Direction direction : Direction.Type.HORIZONTAL) {
            this.addMineSpot(spots, base.offset(direction));
            this.addMineSpot(spots, base.up().offset(direction));
        }

        return new ArrayList<>(spots);
    }

    private void addMineSpot(Set<BlockPos> spots, BlockPos pos) {
        Block block = BlockUtil.getBlock(pos);
        if (AutoMine.hard.contains(block) || Perish.BREAK.isMining(pos) || pos.equals(SpeedMine.getBreakPos()) || pos.equals(SpeedMine.secondPos)) {
            spots.add(pos);
        }
    }

    private List<EndCrystalEntity> collectLiveCrystals() {
        List<EndCrystalEntity> crystals = new ArrayList<>();
        for (Entity entity : Perish.THREAD.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal && crystal.isAlive()) {
                crystals.add(crystal);
            }
        }
        return crystals;
    }

    private ScoreData scoreCandidate(
            Vec3d candidate,
            List<BlockPos> crystalSpots,
            List<BlockPos> mineSpots,
            List<EndCrystalEntity> liveCrystals,
            boolean targetBurrowed
    ) {
        CopyPlayerEntity virtualSelf = new CopyPlayerEntity(mc.player, true);
        virtualSelf.setPosition(candidate.x, candidate.y, candidate.z);
        float threat = this.getCrystalThreat(virtualSelf, candidate, liveCrystals);
        float mineThreat = this.getMineThreat(candidate, mineSpots);
        AttackData attackData = this.getBestAttack(candidate, virtualSelf, crystalSpots, mineSpots, targetBurrowed);
        double distanceToTarget = candidate.distanceTo(this.target.getPos());
        double desiredDistance = targetBurrowed ? 2.7 : 3.6;
        double pressureBonus = Math.max(0.0, 3.5 - Math.abs(distanceToTarget - desiredDistance)) * this.aggression.getValue();
        double chaseBonus = Math.max(0.0, mc.player.getPos().distanceTo(this.target.getPos()) - distanceToTarget) * 0.4 * this.aggression.getValue();
        double movePenalty = candidate.distanceTo(mc.player.getPos()) * this.moveWeight.getValue();
        boolean insideBlock = BlockUtil.canCollide(virtualSelf, virtualSelf.getBoundingBox());
        double burrowBonus = this.selfBurrowed && !insideBlock ? 2.5 : 0.0;
        double insidePenalty = insideBlock && !this.selfBurrowed ? 1.5 : 0.0;
        double score = attackData.damage * this.attackWeight.getValue()
                + attackData.minePressure * this.mineWeight.getValue()
                + pressureBonus
                + chaseBonus
                + burrowBonus
                - threat * this.dangerWeight.getValue()
                - mineThreat * this.mineWeight.getValue()
                - movePenalty
                - insidePenalty;
        return new ScoreData(candidate, attackData.crystalPos, attackData.damage, threat, mineThreat, score);
    }

    private AttackData getBestAttack(
            Vec3d candidate,
            PlayerEntity self,
            List<BlockPos> crystalSpots,
            List<BlockPos> mineSpots,
            boolean targetBurrowed
    ) {
        float bestDamage = 0.0F;
        BlockPos bestPos = null;

        for (BlockPos pos : crystalSpots) {
            if (candidate.distanceTo(pos.toCenterPos()) > this.getCrystalPlaceRange() + 0.5) {
                continue;
            }

            float damage = this.calculateExplosionDamage(pos, this.target);
            if (!this.passesDamage(damage)) {
                continue;
            }

            float selfDamage = this.calculateExplosionDamage(pos, self);
            if (selfDamage > this.maxSelf.getValue() || selfDamage >= EntityUtil.getHealth(mc.player) - 0.5F) {
                continue;
            }

            float weightedDamage = damage - selfDamage * 0.35F;
            if (targetBurrowed && pos.getY() <= Math.floor(this.target.getY() + 0.5)) {
                weightedDamage += 1.0F;
            }
            if (weightedDamage > bestDamage) {
                bestDamage = weightedDamage;
                bestPos = pos;
            }
        }

        float minePressure = this.getMinePressure(candidate, mineSpots);
        return new AttackData(bestDamage, minePressure, bestPos);
    }

    private float getMinePressure(Vec3d candidate, List<BlockPos> mineSpots) {
        float best = 0.0F;
        for (BlockPos pos : mineSpots) {
            double distance = candidate.distanceTo(pos.toCenterPos());
            if (distance > this.getMineRange() + 1.0) {
                continue;
            }
            float pressure = (float) Math.max(0.0, this.getMineRange() + 1.0 - distance);
            if (Perish.BREAK.isMining(pos)) {
                pressure += 1.0F;
            }
            best = Math.max(best, pressure);
        }
        return best;
    }

    private float getCrystalThreat(PlayerEntity self, Vec3d candidate, List<EndCrystalEntity> liveCrystals) {
        float bestThreat = 0.0F;
        for (EndCrystalEntity crystal : liveCrystals) {
            if (candidate.distanceTo(crystal.getPos()) > this.getCrystalBreakRange() + 1.5) {
                continue;
            }
            float damage = this.calculateExplosionDamage(crystal.getPos(), self);
            if (this.passesDamage(damage)) {
                bestThreat = Math.max(bestThreat, damage);
            }
        }
        return bestThreat;
    }

    private float getMineThreat(Vec3d candidate, List<BlockPos> mineSpots) {
        BlockPos feet = BlockPos.ofFloored(candidate);
        BlockPos head = feet.up();
        float best = 0.0F;

        best = Math.max(best, this.getSingleMineThreat(candidate, feet, head, SpeedMine.getBreakPos()));
        best = Math.max(best, this.getSingleMineThreat(candidate, feet, head, SpeedMine.secondPos));

        for (BlockPos pos : mineSpots) {
            best = Math.max(best, this.getSingleMineThreat(candidate, feet, head, pos));
        }

        for (var data : Perish.BREAK.breakMap.values()) {
            best = Math.max(best, this.getSingleMineThreat(candidate, feet, head, data.pos));
        }
        for (var data : Perish.BREAK.doubleMap.values()) {
            best = Math.max(best, this.getSingleMineThreat(candidate, feet, head, data.pos));
        }
        return best;
    }

    private float getSingleMineThreat(Vec3d candidate, BlockPos feet, BlockPos head, BlockPos miningPos) {
        if (miningPos == null) {
            return 0.0F;
        }

        double distance = candidate.distanceTo(miningPos.toCenterPos());
        if (distance > this.getMineRange() + 1.5) {
            return 0.0F;
        }

        float threat = (float) Math.max(0.0, this.getMineRange() + 1.5 - distance);
        if (feet.equals(miningPos) || head.equals(miningPos)) {
            threat += 4.0F;
        }
        if (Perish.BREAK.isMining(miningPos)) {
            threat += 1.0F;
        }
        return threat;
    }

    private float calculateExplosionDamage(BlockPos pos, PlayerEntity player) {
        return this.calculateExplosionDamage(pos.toCenterPos(), player);
    }

    private float calculateExplosionDamage(Vec3d pos, PlayerEntity player) {
        AutoCrystal autoCrystal = AutoCrystal.INSTANCE;
        if (autoCrystal != null) {
            return autoCrystal.calculateDamage(pos, player, player);
        }
        return ExplosionUtil.calculateDamage(pos, player, player, 6.0F);
    }

    private boolean passesDamage(float damage) {
        return !this.damage.getValue() || damage >= this.minDamage.getValue();
    }

    private double getCrystalPlaceRange() {
        return AutoCrystal.INSTANCE != null ? AutoCrystal.INSTANCE.placeRange.getValue() : 5.0;
    }

    private double getCrystalBreakRange() {
        return AutoCrystal.INSTANCE != null ? AutoCrystal.INSTANCE.breakRange.getValue() : 4.0;
    }

    private double getMineRange() {
        if (SpeedMine.INSTANCE != null) {
            return SpeedMine.INSTANCE.range.getValue();
        }
        if (AutoMine.INSTANCE != null) {
            return AutoMine.INSTANCE.range.getValue();
        }
        return 6.0;
    }

    private boolean isBurrowed(PlayerEntity player) {
        return EntityUtil.isInsideBlock(player) || Perish.PLAYER.isInBurrow(player) || Util.isBurrowed(player, true);
    }

    private List<Vec3d> buildPath(Vec3d destination) {
        List<Vec3d> path = new ArrayList<>();
        if (destination.distanceTo(mc.player.getPos()) <= 0.6) {
            path.add(mc.player.getPos());
            path.add(destination);
            return path;
        }

        for (Vec3 vec : PathUtils.computePath(destination)) {
            path.add(new Vec3d(vec.x() + 0.0, vec.y(), vec.z() + 0.0));
        }

        if (path.isEmpty()) {
            path.add(mc.player.getPos());
            path.add(destination);
        } else {
            Vec3d first = path.getFirst();
            if (first.distanceTo(mc.player.getPos()) > 0.75) {
                path.addFirst(mc.player.getPos());
            }
            Vec3d last = path.getLast();
            if (last.distanceTo(destination) > 0.75) {
                path.add(destination);
            }
        }
        return path;
    }

    private record AttackData(float damage, float minePressure, BlockPos crystalPos) {
    }

    private record ScoreData(Vec3d position, BlockPos crystalPos, float damage, float threat, float mineThreat, double score) {
    }

    private enum Page {
        General,
        Render
    }
}
