package dev.g255.arc.mod.modules.impl.player;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.asm.accessors.IPlayerPositionLookS2CPacket;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.FallingBlock;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoRotate extends Module {
    public static NoRotate INSTANCE;

    private final BooleanSetting positionAdjust = this.add(new BooleanSetting("PositionAdjust", false));

    public NoRotate() {
        super("NoRotate", "Cancels the rotations server sets for you.", Category.Misc);
        this.setChinese("防转头");
        INSTANCE = this;
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (!this.positionAdjust.getValue()
                || mc.player == null
                || mc.world == null
                || mc.player.isHoldingOntoLadder()
                || !(event.getPacket() instanceof PlayerMoveC2SPacket)) {
            return;
        }

        BlockPos blockPos = mc.player.getBlockPos();
        BlockState blockState = mc.world.getBlockState(blockPos);
        if (blockState.getBlock() instanceof FallingBlock) {
            return;
        }

        VoxelShape collisionShape = blockState.getCollisionShape(mc.world, blockPos);
        if (collisionShape.isEmpty()) {
            return;
        }

        double localY = mc.player.getY() - Math.floor(mc.player.getY());
        Box collisionBox = collisionShape.getBoundingBox().offset(blockPos);
        if (localY < collisionShape.getBoundingBox().maxY
                && collisionBox.intersects(mc.player.getBoundingBox().expand(0.0625))) {
            event.cancel();
        }
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null
                || mc.isInSingleplayer()
                || mc.player.isHoldingOntoLadder()
                || !(event.getPacket() instanceof PlayerPositionLookS2CPacket packet)
                || mc.player.age <= 5) {
            return;
        }

        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        if (packet.getFlags().contains(PositionFlag.X_ROT)) {
            pitch = 0.0F;
        }
        if (packet.getFlags().contains(PositionFlag.Y_ROT)) {
            yaw = 0.0F;
        }

        ((IPlayerPositionLookS2CPacket) packet).setYaw(yaw);
        ((IPlayerPositionLookS2CPacket) packet).setPitch(pitch);
    }
}
