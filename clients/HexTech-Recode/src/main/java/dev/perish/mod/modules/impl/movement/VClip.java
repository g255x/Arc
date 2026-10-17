package dev.perish.mod.modules.impl.movement;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

public class VClip extends Module {
    private final EnumSetting<VClip.Mode> mode = this.add(new EnumSetting("Mode", VClip.Mode.Jump));

    public VClip() {
        super("VClip", Module.Category.Movement);
        this.setChinese("纵向穿墙");
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.disable();
        switch (this.mode.getValue()) {
            case Glitch:
                double posX = mc.player.getX();
                double posY = Math.round(mc.player.getY());
                double posZ = mc.player.getZ();
                boolean onGround = mc.player.isOnGround();
                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, posY, posZ, onGround));
                double halfY = 0.005;
                posY -= halfY;
                mc.player.setPosition(posX, posY, posZ);
                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, posY, posZ, onGround));
                posY -= halfY * 300.0;
                mc.player.setPosition(posX, posY, posZ);
                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, posY, posZ, onGround));
                break;
            case Teleport:
                mc.player.setPosition(mc.player.getX(), mc.player.getY() + 3.0, mc.player.getZ());
                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
                break;
            case Jump:
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
                mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
            case Tne:
                if (Perish.nightly) {
                    BlockPos playerPos = mc.player.getBlockPos();
                    BlockPos headPos = playerPos.up(2);
                    java.util.List<BlockPos> candidatePositions = new java.util.ArrayList<>();

                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && z == 0) continue;
                            BlockPos checkPos = headPos.add(x, 0, z);
                            if (hasBlock(checkPos)) {
                                candidatePositions.add(checkPos.up());
                            }
                        }
                    }
                    if (hasBlock(headPos)) {
                        candidatePositions.add(headPos.up());
                    }
                    for (BlockPos pos : candidatePositions) {
                        if (!hasBlock(pos) && !hasBlock(pos.up()) && !hasBlock(pos.up(2))) {
                            teleport(pos);
                            disable();
                            return;
                        }
                    }
                }

        }
    }

    private boolean hasBlock(BlockPos pos) {
        return !mc.world.isAir(pos);
    }

    private void teleport(BlockPos pos) {
        if (hasBlock(pos) || hasBlock(pos.up()) || hasBlock(pos.up(2))) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        mc.player.setPosition(x, y, z);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true));
    }

    public enum Mode {
        Glitch,
        Teleport,
        Tne,
        Jump
    }
}
