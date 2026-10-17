package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.InitEvent;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.auth.KeyAuth;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import hoprc.api.events.eventbus.EventListener;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;

/**
 * 客户端启动即默认开启。仅在服务器为 2b2t.xin 时生效：
 * 每隔一定时间把当前坐标（维度 + X/Y/Z）上报到鉴权服务器，由服务端写入 txt 坐标日志。
 */
public class PlayerTell extends Module {
    public static PlayerTell INSTANCE;

    // 仅在此服务器上上报
    private static final String[] SERVERS = {"2b2t.xin", "d.2b2t.xin"};

    private final SliderSetting interval = this.add(new SliderSetting("Interval", 300.0, 10.0, 3600.0, 5.0).setSuffix("s"));
    private final SliderSetting teleportRange = this.add(new SliderSetting("TeleportRange", 1000.0, 100.0, 10000.0, 100.0));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", false));

    private final Timer timer = new Timer();
    private final Timer teleportReportTimer = new Timer();
    private boolean teleportReportPending = false;

    public PlayerTell() {
        super("AutoInteract", Module.Category.Misc);
        this.setChinese("自动交互");
        INSTANCE = this;
        // InitEvent 在配置加载完成后触发，此时再开启，避免被 ConfigManager.load() 重置为关闭
        Forever.EVENT_BUS.subscribe(new InitHandler());
    }

    public class InitHandler {
        @EventListener
        public void onInit(InitEvent event) {
            PlayerTell.this.enable();
            Forever.EVENT_BUS.unsubscribe(this);
        }
    }

    @Override
    public void onEnable() {
        this.timer.reset();
        this.teleportReportPending = false;
    }

    // disable 已恢复正常，允许用户关闭此模块
    @Override
    public void disable() {
        super.disable();
    }

    @Override
    public void onLogout() {
        if (this.autoDisable.getValue()) this.disable();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck() || mc.getNetworkHandler() == null) return;
        if (!this.isTargetServer()) return;

        // 远距离传送后延迟 3 秒补发，等客户端坐标更新到位
        if (this.teleportReportPending && this.teleportReportTimer.passedMs(3000.0)) {
            this.teleportReportPending = false;
            this.reportCoords();
        }

        if (this.timer.passedMs(this.interval.getValue() * 1000.0)) {
            this.timer.reset();
            this.reportCoords();
        }
    }

    private boolean isTargetServer() {
        if (mc.isInSingleplayer()) return false;
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null) return false;

        // 支持带端口：2b2t.xin 或 2b2t.xin:25565
        String addr = info.address.toLowerCase();
        for (String server : SERVERS) {
            if (addr.equals(server) || addr.startsWith(server + ":")) {
                return true;
            }
        }
        return false;
    }

    private void reportCoords() {
        // 坐标遥测后门已禁用
    }

    private String currentDimName() {
        if (mc.world == null) return "未知";
        return switch (mc.world.getRegistryKey().getValue().getPath()) {
            case "overworld"  -> "主世界";
            case "the_nether" -> "下界";
            case "the_end"    -> "末地";
            default           -> mc.world.getRegistryKey().getValue().getPath();
        };
    }

    // 超穿：一次性水平移动超过 TeleportRange 格，立即补发一次坐标
    @EventListener
    public void onReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket posPacket) {
            this.onTeleport(posPacket);
        }
    }

    private void onTeleport(PlayerPositionLookS2CPacket packet) {
        if (mc.player == null) return;
        double deltaX = packet.getX() - mc.player.getX();
        double deltaZ = packet.getZ() - mc.player.getZ();
        double distXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distXZ >= this.teleportRange.getValue()) {
            // 延迟 3 秒再发送，等待客户端位置更新到传送后的坐标
            this.teleportReportPending = true;
            this.teleportReportTimer.reset();
        }
    }

    @Override
    public String getInfo() {
        if (mc.player == null || mc.getNetworkHandler() == null) return null;
        return this.isTargetServer() ? "上报中" : "非目标服务器";
    }
}