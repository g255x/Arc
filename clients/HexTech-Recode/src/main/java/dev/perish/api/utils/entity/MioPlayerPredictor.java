package dev.perish.api.utils.entity;

import dev.perish.api.utils.Wrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.math.MathHelper;

/**
 * 每玩家 bounding-box 预判器，逻辑移植自 OpenMio Class_0308。
 *
 * 每 tick 调用 {@link #tick()}，对玩家未来 20 tick 的包围盒进行物理模拟；
 * 用 {@link #getBox(int)} 取指定 tick 后的预判包围盒。
 *
 * 物理常量（由 OpenMio 混淆常量解码得出）：
 *   GRAVITY        = -0.0784  ≈ -(0.08 * 0.98)，落地时施加（被碰撞剪裁为 0）
 *   GRAVITY_C1     =  0.08   ，在空中时 dy = (dy + 0.08) * 0.98
 *   GRAVITY_C2     =  0.98
 *   MAX_SPEED      =  0.287  ，普通行走最大水平速度
 *   SNEAK_SPEED    =  MAX_SPEED * 0.3  ，潜行
 *   MAX_CLAMP      =  MAX_SPEED * 1.4 + 0.2，水平速度上限
 *   MIN_MOVE       =  MAX_SPEED * 0.2  ，水平"几乎静止"阈值
 *   MIN_VY         =  0.1，垂直"几乎静止"阈值
 */
public class MioPlayerPredictor implements Wrapper {

    // ---- 物理常量 ----
    private static final double GRAVITY         = -0.0784;
    private static final double GRAVITY_C1      =  0.08;
    private static final double GRAVITY_C2      =  0.98;
    private static final double GROUND_OFFSET   = -0.01;   // 判断脚下是否有方块的 Y 偏移
    private static final double GROUND_STRETCH  = -0.1;    // 初始在地检测的 Y 拉伸
    private static final double MIN_VY          =  0.1;

    private static final float MAX_SPEED        = 0.287f;
    private static final float SNEAK_SPEED      = MAX_SPEED * 0.3f;
    private static final float MAX_CLAMP        = MAX_SPEED * 1.4f + 0.2f;
    private static final float MIN_MOVE         = MAX_SPEED * 0.2f;

    private final List<Box> boxes = new ArrayList<>();
    private final PlayerEntity player;

    public MioPlayerPredictor(PlayerEntity player) {
        this.player = player;
    }

    /** 每 tick 调用，重新模拟未来 20 tick 的包围盒序列。 */
    public void tick() {
        synchronized (boxes) {
            boxes.clear();

            double dx = player.getX() - player.prevX;
            double dy = player.getY() - player.prevY;
            double dz = player.getZ() - player.prevZ;
            double hSpeed = Math.hypot(dx, dz);

            // 水平速度 clamp
            float maxH = player.isSneaking() ? SNEAK_SPEED : MAX_SPEED;
            double clampedH = MathHelper.clamp(hSpeed, (double) maxH, (double) MAX_CLAMP);
            if (hSpeed != 0.0) {
                dx = dx / hSpeed * clampedH;
                dz = dz / hSpeed * clampedH;
            }

            // 若脚下有地 && 当前 dy > 0（上升），重置为重力值（预测即将下落）
            if (isOnGround(player.getBoundingBox()) && dy > 0.0) {
                dy = GRAVITY;
            }

            Box box = player.getBoundingBox();

            // 水平几乎静止时清零水平分量
            if (hSpeed < MIN_MOVE) {
                dx = 0.0;
                dz = 0.0;
            }

            // 水平 + 垂直都几乎静止：直接复制当前包围盒 21 次
            if (hSpeed < MIN_MOVE && Math.abs(dy) < MIN_VY) {
                for (int i = 0; i <= 20; i++) boxes.add(box);
                return;
            }

            // 完整模拟
            for (int i = 0; i <= 20; i++) {
                boolean groundBelow = hasCollisionBelow(box);
                if (!groundBelow) {
                    // 空中：施加减速型重力（移植自 OpenMio shiftDown）
                    dy = (dy + GRAVITY_C1) * GRAVITY_C2;
                } else {
                    // 接地：重力持续施加（碰撞检测会剪裁为 0，使玩家保持不动）
                    dy = GRAVITY;
                }

                Box stretched = box.stretch(dx, dy, dz);
                List<VoxelShape> shapes = collectCollisions(stretched);
                box = applyMovementWithCollision(box, shapes, dx, dy, dz);
                boxes.add(box);
            }
        }
    }

    /**
     * 返回 {@code ticks} 步后的预判包围盒（索引越界时取最后一帧）。
     * 若尚无数据则返回玩家当前包围盒。
     */
    public synchronized Box getBox(int ticks) {
        if (boxes.isEmpty()) return player.getBoundingBox();
        return boxes.get(Math.min(ticks, boxes.size() - 1));
    }

    // ---- 私有工具 ----

    /** 检测当前包围盒向上拉伸 GROUND_STRETCH 后是否与方块重叠（判断"脚下有地"）。 */
    private boolean isOnGround(Box box) {
        return !mc.world.isBlockSpaceEmpty(player, box.stretch(0.0, GROUND_STRETCH, 0.0));
    }

    /** 检测包围盒下移 GROUND_OFFSET 后是否与方块碰撞（每 tick 判断脚下是否贴地）。 */
    private boolean hasCollisionBelow(Box box) {
        return mc.world.getBlockCollisions(player, box.offset(0.0, GROUND_OFFSET, 0.0)).iterator().hasNext();
    }

    /** 收集 stretched 区域内的实体与方块碰撞形状。 */
    private List<VoxelShape> collectCollisions(Box stretched) {
        List<VoxelShape> shapes = new ArrayList<>();
        mc.world.getBlockCollisions(player, stretched).forEach(shapes::add);
        mc.world.getEntityCollisions(player, stretched).forEach(shapes::add);
        return shapes;
    }

    /**
     * 将 {@code box} 按 (dx, dy, dz) 移动，用 VoxelShapes 剪裁碰撞（移植自 OpenMio push）。
     */
    private static Box applyMovementWithCollision(Box box, List<VoxelShape> shapes, double dx, double dy, double dz) {
        if (shapes.isEmpty()) return box.offset(dx, dy, dz);

        // 优先处理较小轴（与 OpenMio / 原版逻辑一致）
        boolean xFirst = Math.abs(dx) < Math.abs(dz);

        if (dy != 0.0) {
            dy = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, box, shapes, dy);
            if (dy != 0.0) box = box.offset(0.0, dy, 0.0);
        }
        if (xFirst && dz != 0.0) {
            dz = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, box, shapes, dz);
            if (dz != 0.0) box = box.offset(0.0, 0.0, dz);
        }
        if (dx != 0.0) {
            dx = VoxelShapes.calculateMaxOffset(Direction.Axis.X, box, shapes, dx);
            if (!xFirst && dx != 0.0) box = box.offset(dx, 0.0, 0.0);
        }
        if (!xFirst && dz != 0.0) {
            VoxelShapes.calculateMaxOffset(Direction.Axis.Z, box, shapes, dz);
        }
        return box;
    }
}
