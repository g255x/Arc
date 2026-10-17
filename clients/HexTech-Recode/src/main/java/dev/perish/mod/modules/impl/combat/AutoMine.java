package dev.perish.mod.modules.impl.combat;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.combat.CombatUtil;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.world.BlockPosX;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.exploit.Blink;
import dev.perish.mod.modules.impl.player.SpeedMine;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import java.util.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.BlockState;

public class AutoMine extends Module {
    public static final List<Block> hard = Arrays.asList(
            Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL
    );
    public static AutoMine INSTANCE;
    public final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
    public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
    private final BooleanSetting burrow = this.add(new BooleanSetting("Burrow", true));
    private final BooleanSetting head = this.add(new BooleanSetting("Head", true));
    private final BooleanSetting face = this.add(new BooleanSetting("Face", true));
    private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
    private final BooleanSetting surround = this.add(new BooleanSetting("Surround", true));
    private final BooleanSetting cevPause = this.add(new BooleanSetting("CevPause", true));
    private final BooleanSetting forceDouble = this.add(new BooleanSetting("ForceDouble", false));

    public AutoMine() {
        super("AutoMine", Module.Category.Combat);
        this.setChinese("自动挖掘");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!CevBreaker.INSTANCE.isOn() || !this.cevPause.getValue()) {
            if (!AntiCrawl.INSTANCE.work) {
                if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                    PlayerEntity player = CombatUtil.getClosestEnemy(this.targetRange.getValue());
                    if (player != null) {
                        if (SpeedMine.secondPos!=null&& !SpeedMine.secondPos.equals(SpeedMine.getBreakPos())) return;
                        this.doBreak(player);
                    }
                }
            }
        }
    }

    private void doBreak(PlayerEntity player) {
        BlockPos pos = EntityUtil.getEntityPos(player, true);
        if (SpeedMine.getBreakPos() == null
                || SpeedMine.getBreakPos().equals(SpeedMine.secondPos)
                || SpeedMine.secondPos == null
                || mc.world.isAir(SpeedMine.secondPos)
                || !this.forceDouble.getValue()) {
            // 包含 head(2.3) 防止正在挖 head 时被误切换目标
            double[] yOffset = new double[]{-0.8, 0.3, 1.1, 2.3};
            double[] xzOffset = new double[]{0.3, -0.3};

            for (PlayerEntity entity : CombatUtil.getEnemies(this.targetRange.getValue())) {
                for (double y : yOffset) {
                    for (double x : xzOffset) {
                        for (double z : xzOffset) {
                            BlockPos offsetPos = new BlockPosX(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                            if (this.canBreak(offsetPos) && offsetPos.equals(SpeedMine.getBreakPos())) {
                                return;
                            }
                        }
                    }
                }
            }

            // 优先级：burrow(脚下) → face(胸口) → head(头顶) → down(脚底)
            List<Float> yList = new ArrayList<>();
            if (this.burrow.getValue()) {
                yList.add(0.3F);
            }
            if (this.face.getValue()) {
                yList.add(1.1F);
            }
            if (this.head.getValue()) {
                yList.add(2.3F);
            }
            if (this.down.getValue()) {
                yList.add(-0.8F);
            }

            // 单次遍历，覆盖全部4个XZ组合，消除对角线漏检
            for (float yF : yList) {
                for (double offsetx : xzOffset) {
                    for (double offsetz : xzOffset) {
                        BlockPos offsetPos = new BlockPosX(player.getX() + offsetx, player.getY() + yF, player.getZ() + offsetz);
                        if (this.canBreak(offsetPos)) {
                            SpeedMine.INSTANCE.mine(offsetPos);
                            return;
                        }
                    }
                }
            }

            if (this.surround.getValue()) {
                for (Direction i : Direction.values()) {
                    if (i != Direction.UP
                            && i != Direction.DOWN
                            && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > this.range.getValue())
                            && (mc.world.isAir(pos.offset(i)) || pos.offset(i).equals(SpeedMine.getBreakPos()))
                            && this.canPlaceCrystal(pos.offset(i), false)
                            && !pos.offset(i).equals(SpeedMine.secondPos)) {
                        return;
                    }
                }

                ArrayList<BlockPos> list = new ArrayList<>();

                for (Direction ix : Direction.values()) {
                    if (ix != Direction.UP
                            && ix != Direction.DOWN
                            && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(ix).toCenterPos())) > this.range.getValue())
                            && this.canBreak(pos.offset(ix))
                            && this.canPlaceCrystal(pos.offset(ix), true)
                            && !this.isSurroundPos(pos.offset(ix))) {
                        list.add(pos.offset(ix));
                    }
                }

                if (!list.isEmpty()) {
                    // 选离敌人最近的 surround 块，水晶爆炸伤害更高
                    SpeedMine.INSTANCE.mine(list.stream().min(Comparator.comparingDouble(E -> E.getSquaredDistance(pos))).get());
                } else {
                    for (Direction ixx : Direction.values()) {
                        if (ixx != Direction.UP
                                && ixx != Direction.DOWN
                                && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(ixx).toCenterPos())) > this.range.getValue())
                                && this.canBreak(pos.offset(ixx))
                                && this.canPlaceCrystal(pos.offset(ixx), false)) {
                            list.add(pos.offset(ixx));
                        }
                    }

                    if (!list.isEmpty()) {
                        SpeedMine.INSTANCE.mine(list.stream().min(Comparator.comparingDouble(E -> E.getSquaredDistance(pos))).get());
                    }
                }
            }
        }
    }

    private boolean isSurroundPos(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (i != Direction.UP && i != Direction.DOWN) {
                BlockPos self = EntityUtil.getPlayerPos(true);
                if (self.offset(i).equals(pos)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean canPlaceCrystal(BlockPos pos, boolean block) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        return (BlockUtil.getBlock(obsPos) == Blocks.BEDROCK || BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN || !block)
                && BlockUtil.noEntityBlockCrystal(boost, true, true)
                && BlockUtil.noEntityBlockCrystal(boost.up(), true, true);
    }

    private boolean isObsidian(BlockPos pos) {
        return mc.player.getEyePos().distanceTo(pos.toCenterPos()) <= SpeedMine.INSTANCE.range.getValue()
                && hard.contains(BlockUtil.getBlock(pos))
                && BlockUtil.getClickSideStrict(pos) != null;
    }

    private boolean canBreak(BlockPos pos) {
        // 检查是否是活板门，如果是活板门且正在被SpeedMine挖掘，则跳过
        BlockState state = mc.world.getBlockState(pos);
        if (state.getBlock() instanceof TrapdoorBlock && pos.equals(SpeedMine.getBreakPos())) {
            return false;
        }

        return this.isObsidian(pos)
                && (BlockUtil.getClickSideStrict(pos) != null || pos.equals(SpeedMine.getBreakPos()))
                && (
                !pos.equals(SpeedMine.secondPos)
                        || !(mc.player.getMainHandStack().getItem() instanceof PickaxeItem)
                        && !SpeedMine.INSTANCE.autoSwitch.getValue()
                        && !SpeedMine.INSTANCE.noGhostHand.getValue()
        );
    }
}