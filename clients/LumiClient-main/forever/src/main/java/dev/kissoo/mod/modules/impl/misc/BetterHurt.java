package dev.kissoo.mod.modules.impl.misc;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.DeathEvent;
import dev.kissoo.api.events.impl.TotemEvent;
import dev.kissoo.api.utils.dglab.DglabServer;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import net.minecraft.text.Text;

import java.net.InetSocketAddress;

public class BetterHurt extends Module {

    public enum TriggerMode {
        ON_DEATH("死亡时"),
        ON_POP("爆图腾时"),
        BOTH("死亡与爆图腾");

        private final String chinese;

        TriggerMode(String chinese) {
            this.chinese = chinese;
        }

        @Override
        public String toString() {
            return chinese;
        }
    }

    public static BetterHurt INSTANCE;

    private DglabServer server = null;
    public final StringSetting address = this.add(
            new StringSetting("监听IP", "0.0.0.0")
    );
    public final SliderSetting port = this.add(
            new SliderSetting("端口", 9999, 1024, 65535, 1)
    );
    public final BooleanSetting debugMsg = this.add(
            new BooleanSetting("调试提示", true)
    );
    public final EnumSetting<TriggerMode> triggerMode = this.add(
            new EnumSetting<>("触发模式", TriggerMode.BOTH)
    );
    public final SliderSetting aStrength = this.add(
            new SliderSetting("A基础强度", 10, 0, 100, 1)
    );
    public final SliderSetting aIncrease = this.add(
            new SliderSetting("A受击增量", 5, 0, 50, 1)
    );
    public final StringSetting aWaveform = this.add(
            new StringSetting("A通道波形", "\"0A0A0A0A64646464\",\"0A0A0A0A64646464\",\"0A0A0A0A64000000\"")
    );
    public final SliderSetting bStrength = this.add(
            new SliderSetting("B基础强度", 10, 0, 100, 1)
    );
    public final SliderSetting bIncrease = this.add(
            new SliderSetting("B受击增量", 5, 0, 50, 1)
    );
    public final StringSetting bWaveform = this.add(
            new StringSetting("B通道波形", "\"0A0A0A0A64646464\",\"0A0A0A0A64646464\",\"0A0A0A0A64000000\"")
    );


    public BetterHurt() {
        super("BetterHurt", "DGLab 郊狼物理反馈联动", Module.Category.Misc);
        this.setChinese("郊狼联动");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (!nullCheck()) return;
        try {
            if (server != null) {
                server.stop();
            }
            server = new DglabServer(new InetSocketAddress(address.getValue(), (int) port.getValue()));
            server.start();

            if (debugMsg.getValue() && mc.player != null) {
                mc.player.sendMessage(Text.literal("§a[BetterHurt] 郊狼服务端已启动! 请使用郊狼App连接 ws://你的电脑局域网IP:" + (int) port.getValue()), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§c[BetterHurt] 服务端启动失败，端口可能被占用！"), false);
            }
        }
    }

    @Override
    public void onDisable() {
        if (server != null) {
            try {
                server.stop();
                server = null;
                if (debugMsg.getValue() && mc.player != null) {
                    mc.player.sendMessage(Text.literal("§e[BetterHurt] 郊狼服务端已关闭。"), false);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @EventListener
    public void onDeath(DeathEvent event) {
        if (nullCheck()) return;
        if (event.getPlayer() != mc.player) return;

        TriggerMode mode = this.triggerMode.getValue();
        if (mode == TriggerMode.ON_DEATH || mode == TriggerMode.BOTH) {
            triggerFeedback("死亡");
        }
    }

    @EventListener
    public void onTotemPop(TotemEvent event) {
        if (nullCheck()) return;
        if (event.getPlayer() != mc.player) return;

        TriggerMode mode = this.triggerMode.getValue();
        if (mode == TriggerMode.ON_POP || mode == TriggerMode.BOTH) {
            triggerFeedback("爆图腾");
        }
    }

    private void triggerFeedback(String reason) {
        if (server == null || !server.isConnected()) {
            return;
        }
        int currentA = (int) (aStrength.getValue() + aIncrease.getValue());
        int currentB = (int) (bStrength.getValue() + bIncrease.getValue());
        currentA = Math.min(currentA, 100);
        currentB = Math.min(currentB, 100);
        server.sendStrength(1, currentA);
        server.sendStrength(2, currentB);
        server.sendWaveform(1, aWaveform.getValue());
        server.sendWaveform(2, bWaveform.getValue());
        if (debugMsg.getValue() && mc.player != null) {
            mc.player.sendMessage(Text.literal(String.format("§c[BetterHurt] 触发电击! 原因: %s | 强度: A:%d B:%d", reason, currentA, currentB)), false);
        }
    }
}