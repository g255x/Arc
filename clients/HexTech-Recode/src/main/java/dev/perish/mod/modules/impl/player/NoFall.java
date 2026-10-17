package dev.perish.mod.modules.impl.player;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.asm.accessors.IPlayerMoveC2SPacket;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.combat.MaceAura;
import dev.perish.mod.modules.impl.exploit.BowBomb;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;

public class NoFall extends Module {
    private final EnumSetting<NoFall.NoFallMode> mode = this.add(new EnumSetting("Mode", NoFall.NoFallMode.Packet));
    private final SliderSetting distance = this.add(new SliderSetting("Distance", 3.0, 0.0, 8.0, 0.1));

    public NoFall() {
        super("NoFall", "Prevents fall damage.", Module.Category.Player);
        this.setChinese("没有摔落伤害");
    }

    @Override
    public String getInfo() {
        return this.mode.getValue().name();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!nullCheck()) {
            if (this.shouldPauseForMaceAura()) {
                return;
            }

            if (this.mode.is(NoFall.NoFallMode.Grim) && this.checkFalling()) {
                mc.getNetworkHandler()
                        .sendPacket(
                                new Full(
                                        mc.player.getX(),
                                        mc.player.getY() + 1.0E-9,
                                        mc.player.getZ(),
                                        mc.player.getYaw(),
                                        mc.player.getPitch(),
                                        false
                                )
                        );
                mc.player.onLanding();
            }
        }
    }

    private boolean checkFalling() {
        return mc.player.fallDistance > mc.player.getSafeFallDistance() && !mc.player.isOnGround() && !mc.player.isFallFlying();
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (!nullCheck()) {
            if (this.shouldPauseForMaceAura()) {
                return;
            }

            for (ItemStack is : mc.player.getArmorItems()) {
                if (is.getItem() == Items.ELYTRA) {
                    return;
                }
            }

            if (this.mode.is(NoFall.NoFallMode.Packet)) {
                if (event.getPacket() instanceof PlayerMoveC2SPacket packet && mc.player.fallDistance >= (float) this.distance.getValue() && !BowBomb.send) {
                    ((IPlayerMoveC2SPacket) packet).setOnGround(true);
                }
            }
        }
    }

    private boolean shouldPauseForMaceAura() {
        return MaceAura.INSTANCE != null && MaceAura.INSTANCE.shouldSuppressNoFall();
    }

    public enum NoFallMode {
        Packet,
        Grim
    }
}
