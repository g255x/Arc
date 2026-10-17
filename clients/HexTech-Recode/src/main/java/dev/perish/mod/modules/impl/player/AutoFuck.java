package dev.perish.mod.modules.impl.player;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

public class AutoFuck extends Module {
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 500, 0, 2000));
    private long lastTime = 0L;
    private boolean sneaking = false;

    public AutoFuck() {
        super("AutoFuck", "Automatically toggle sneak state with configurable delay", Module.Category.Player);
    }

    @Override
    public void onEnable() {
        this.lastTime = 0L;
        this.sneaking = false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((double) (now - this.lastTime) >= this.delay.getValue()) {
            this.sneaking = !this.sneaking;
            ClientCommandC2SPacket.Mode mode =
                    this.sneaking
                            ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY
                            : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY;
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, mode));
            this.lastTime = now;
        }
    }
}