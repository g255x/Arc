package dev.perish.api.utils.entity;

import com.mojang.authlib.GameProfile;
import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.math.PredictUtil;
import dev.perish.asm.accessors.ILivingEntity;
import java.util.ArrayList;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class CopyPlayerEntity extends PlayerEntity {
    private final boolean onGround;

    public CopyPlayerEntity(PlayerEntity player) {
        this(player, false, 0.0, 0, 0, false, false, false, false);
    }

    public CopyPlayerEntity(PlayerEntity player, boolean effect) {
        super(Wrapper.mc.world, player.getBlockPos(), player.getYaw(), new GameProfile(player.getGameProfile().getId(), player.getGameProfile().getName()));
        this.copyPositionAndRotation(player);
        this.prevX = player.prevX;
        this.prevZ = player.prevZ;
        this.prevY = player.prevY;
        this.bodyYaw = player.bodyYaw;
        this.headYaw = player.headYaw;
        this.handSwingProgress = player.handSwingProgress;
        this.handSwingTicks = player.handSwingTicks;
        this.limbAnimator.setSpeed(player.limbAnimator.getSpeed());
        this.limbAnimator.pos = player.limbAnimator.getPos();
        ((ILivingEntity) this).setLeaningPitch(((ILivingEntity) player).getLeaningPitch());
        ((ILivingEntity) this).setLastLeaningPitch(((ILivingEntity) player).getLeaningPitch());
        this.touchingWater = player.isTouchingWater();
        this.setSneaking(player.isSneaking());
        this.setPose(player.getPose());
        this.setFlag(7, player.isFallFlying());
        this.onGround = player.isOnGround();
        this.setOnGround(this.onGround);
        this.setVelocity(player.getVelocity());
        this.getInventory().clone(player.getInventory());
        if (effect) {
            for (StatusEffectInstance se : new ArrayList<>(player.getStatusEffects())) {
                this.addStatusEffect(se);
            }
        }

        this.setAbsorptionAmountUnclamped(player.getAbsorptionAmount());
        this.setHealth(player.getHealth());
        this.setBoundingBox(player.getBoundingBox());
    }

    public CopyPlayerEntity(
            PlayerEntity player, boolean effect, double maxMotionY, int ticks, int simulation, boolean step, boolean doubleStep, boolean jump, boolean inBlockPause
    ) {
        this(player, effect);
        if (ticks > 0) {
            this.setPosition(PredictUtil.getPos(player, maxMotionY, ticks, simulation, step, doubleStep, jump, inBlockPause));
        }
    }

    /**
     * Mio 预判构造器：用 {@link MioPlayerPredictor} 输出的预判包围盒
     * 直接设定玩家位置与碰撞箱，保留其余状态（血量、护甲等）不变。
     *
     * @param player     原始玩家实体
     * @param predictBox Mio 预判管理器返回的未来 N tick 包围盒
     */
    public CopyPlayerEntity(PlayerEntity player, Box predictBox) {
        this(player, false);
        // 从 Box 的脚底中心还原出玩家位置（与 Minecraft setPosition 的脚底坐标约定一致）
        Vec3d pos = new Vec3d(
                (predictBox.minX + predictBox.maxX) / 2.0,
                predictBox.minY,
                (predictBox.minZ + predictBox.maxZ) / 2.0
        );
        this.setPosition(pos);
        // 直接覆盖包围盒以精确匹配 Mio 预判结果（含潜行/飞行等姿态差异）
        this.setBoundingBox(predictBox);
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean isSpectator() {
        return false;
    }

    public boolean isCreative() {
        return false;
    }
}
