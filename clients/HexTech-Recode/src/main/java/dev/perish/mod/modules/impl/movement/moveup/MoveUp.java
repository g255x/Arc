package dev.perish.mod.modules.impl.movement.moveup;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.player.MovementUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.combat.Burrow;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class MoveUp extends Module {
    public static MoveUp INSTANCE;
    public final SliderSetting rubberbandPackets =
            add(new SliderSetting("Packet", 1, 0, 9, 1));
    public final SliderSetting setPosition =
            add(new SliderSetting("SetPosition", 1, -5, 5, 0.1));
    public final SliderSetting rubberbandOffset =
            add(new SliderSetting("Offset", 1, 0, 9, 1));
    final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Jump));
    private final BooleanSetting onlyburrow = add(new BooleanSetting("OnlyBurrow", true));
    private final BooleanSetting onlyGround = add(new BooleanSetting("GroundCheck", true));
    private final BooleanSetting pEndChest = add(new BooleanSetting("EndChest", true));
    private final BooleanSetting doburrow = add(new BooleanSetting("StartBurrow", true));
    private final BooleanSetting toggle = add(new BooleanSetting("Toggle", false));

    public MoveUp() {
        super("MoveUp", Category.Movement);
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (mc.player != null && onlyGround.getValue() && !mc.player.isOnGround()) return;
        if (mc.player != null && (!onlyburrow.getValue() || Util.isBurrowed(mc.player, !pEndChest.getValue()))) {
            switch (mode.getValue()) {
                case Jump -> {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
                    //mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.9999957640154541, mc.player.getZ(), false));
                    mc.player.setPosition(mc.player.getX(), mc.player.getY() + setPosition.getValueFloat(), mc.player.getZ());
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
                }
                case Packet -> {
                    if (mc.player != null && (!onlyburrow.getValue() || Util.isBurrowed(mc.player, !pEndChest.getValue()))) {
                        double y = 0;
                        double velocity = 0.42;

                        while (y < 1.1) {
                            y += velocity;
                            velocity = (velocity - 0.08) * 0.98;
                            sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + y, mc.player.getZ(), false));
                        }
                        for (int i = 0; i < rubberbandPackets.getValueInt(); i++) {
                            sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + y + rubberbandOffset.getValueInt(), mc.player.getZ(), false));
                        }
                    }
                }
            }
        }
        if (doburrow.getValue() && !MovementUtil.isMoving()) {
            if (!Burrow.INSTANCE.isOn()) {
                Burrow.INSTANCE.enable();
            }
        }
        if (toggle.getValue()) {
            disable();
        }
    }

    public enum Mode {
        Packet,
        Jump
    }
}