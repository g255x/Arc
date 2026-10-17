package dev.perish.mod.modules.impl.misc;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.player.SpeedMine;
import dev.perish.mod.modules.impl.render.PlaceRender;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class Nuker extends Module {
    private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.0, 8.0, 0.1));
    private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
    private final BooleanSetting sand = this.add(new BooleanSetting("Sand", false));
    private final SliderSetting breaks = this.add(new SliderSetting("Breaks", 10, 0, 20, this.sand::getValue));

    public Nuker() {
        super("Nuker", Module.Category.Misc);
        this.setChinese("范围挖掘");
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (SpeedMine.getBreakPos() == null || mc.world.isAir(SpeedMine.getBreakPos())) {
            if (this.sand.getValue()) {
                if (!mc.player.isOnGround()) {
                    return;
                }

                int b = 0;

                for (BlockPos sand : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {
                    if (Blocks.SAND == mc.world.getBlockState(sand).getBlock() || Blocks.RED_SAND == mc.world.getBlockState(sand).getBlock()) {
                        Direction side = BlockUtil.getClickSideStrict(sand);
                        if (side != null) {
                            PlaceRender.INSTANCE.create(sand);
                            Perish.ROTATION.snapAt(sand.toCenterPos());
                            sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                            sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, sand, side, id));
                            sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                            Perish.ROTATION.snapBack();
                            if (++b >= this.breaks.getValue()) {
                                return;
                            }
                        }
                    }
                }
            } else {
                BlockPos pos = this.getBlock();
                if (pos != null) {
                    SpeedMine.INSTANCE.mine(pos);
                }
            }
        }
    }

    private BlockPos getBlock() {
        BlockPos down = null;

        for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {
            if (!mc.world.isAir(pos) && !SpeedMine.unbreakable(pos) && BlockUtil.getClickSideStrict(pos) != null) {
                if (!(pos.getY() < mc.player.getY())) {
                    return pos;
                }

                if (down == null && this.down.getValue()) {
                    down = pos;
                }
            }
        }

        return down;
    }
}
