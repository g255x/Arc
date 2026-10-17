package dev.G255.Lumi;

import dev.G255.Lumi.api.events.eventbus.EventBus;
import dev.G255.Lumi.api.events.impl.InitEvent;
import dev.G255.Lumi.core.impl.*;
import dev.G255.Lumi.mod.modules.impl.client.ClientSetting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class Lumi implements ModInitializer {
    public static final String NAME = "Lumi";
    public static final String VERSION = "1.0.0";
    public static final String CONFIG_DIR = "lumi";
    public static final EventBus EVENT_BUS = new EventBus();
    public static HoleManager HOLE;
    public static PlayerManager PLAYER;
    public static ModuleManager MODULE;
    public static CommandManager COMMAND;
    public static ConfigManager CONFIG;
    public static RotationManager ROTATION;
    public static BreakManager BREAK;
    public static PopManager POP;
    public static FriendManager FRIEND;
    public static TimerManager TIMER;
    public static ShaderManager SHADER;
    public static FPSManager FPS;
    public static ServerManager SERVER;
    public static ThreadManager THREAD;
    public static CleanerManager CLEANER;
    public static XrayManager XRAY;
    public static TradeManager TRADE;
    public static BlurManager BLUR;
    public static HudItemManager HUD_ITEM;
    public static boolean loaded;
    public static long initTime;
    public static volatile int _tickCounter;  // 出口点混淆计数器

    public static String getPrefix() {
        return ClientSetting.INSTANCE.prefix.getValue();
    }

    public static void save() {
        CONFIG.save();
        FRIEND.save();
        System.out.println("[Lumi] Saved");
    }

    private void register() {
        EVENT_BUS.registerLambdaFactory((lookupInMethod, klass) -> (MethodHandles.Lookup)lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (loaded) {
                Lumi.save();
            }
        }));
    }

    public void onInitialize() {
        this.register();
        MODULE = new ModuleManager();
        CONFIG = new ConfigManager();
        HOLE = new HoleManager();
        COMMAND = new CommandManager();
        FRIEND = new FriendManager();
        ROTATION = new RotationManager();
        BREAK = new BreakManager();
        PLAYER = new PlayerManager();
        POP = new PopManager();
        TIMER = new TimerManager();
        SHADER = new ShaderManager();
        FPS = new FPSManager();
        SERVER = new ServerManager();
        CONFIG.load();
        // region ── 许可证验证入口（反射调用，无编译期直接引用） ──
        if (!_initLicense()) {
            System.err.println("[Lumi] License verification failed.");
            return;
        }
        // endregion
        THREAD = new ThreadManager();
        CLEANER = new CleanerManager();
        XRAY = new XrayManager();
        TRADE = new TradeManager();
        BLUR = new BlurManager();
        HUD_ITEM = new HudItemManager();

        initTime = System.currentTimeMillis();
        loaded = true;
        EVENT_BUS.post(new InitEvent());
        File folder = new File(MinecraftClient.getInstance().runDirectory.getPath() + File.separator + CONFIG_DIR + File.separator + "cfg");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    static {
        loaded = false;
    }

    // region ── 许可证验证（多层反射入口，无直接类引用） ──
    private static final String LUMI_PKG = "dev.G255.Lumi.core.impl";
    private static final String _CLS_LM = LUMI_PKG + ".LicenseManager";
    private static final String _CLS_LD = LUMI_PKG + ".LicenseDialog";

    private static boolean _initLicense() {
        try {
            Class<?> _lm = Class.forName(_CLS_LM);
            Class<?> _lw = Class.forName(LUMI_PKG + ".LoadingWindow");
            Class<?> _ld = Class.forName(_CLS_LD);

            // 加载已保存的许可证信息
            Object[] _info = (Object[]) _lm.getMethod("_entry", int.class, Object[].class)
                    .invoke(null, 8, null);
            if (_info == null) _info = new Object[]{false, null, false, 0L, 0};

            boolean _ok = _info[0] instanceof Boolean && (Boolean) _info[0];
            String _sk = _info[1] instanceof String ? (String) _info[1] : null;
            boolean _sa = _info[2] instanceof Boolean && (Boolean) _info[2];
            long _at = _info[3] instanceof Number ? ((Number)_info[3]).longValue() : 0L;
            long _ed = _info[4] instanceof Number ? ((Number)_info[4]).longValue() : 0L;

            // 情况1：已激活且有效 — 显示 Loading，等待用户决定
            if (_ok && _sk != null && !_sk.isEmpty()) {
                long _expTs = _ed < 0 ? -1 : (_at + _ed * 86400L);
                String _edStr = _fmtEd(_expTs);
                _lw.getMethod("displayActivated", long.class, String.class)
                    .invoke(null, _expTs, _edStr);
                _lw.getMethod("awaitStartupDecision").invoke(null);

                // 用户点击了"更改激活码"→确认→需要重新激活
                Boolean _reauth = (Boolean) _lw.getMethod("isReauthRequested").invoke(null);
                if (Boolean.TRUE.equals(_reauth)) {
                    _lm.getMethod("_entry", int.class, Object[].class)
                        .invoke(null, 7, null);
                    _sk = null;
                    _sa = false;
                } else {
                    // 正常启动
                    return true;
                }
            }

            // 情况2：已过期（有密钥但验证不通过）→ 弹出过期对话框
            if (!_ok && _sk != null && !_sk.isEmpty()) {
                long _expTs = _ed < 0 ? -1 : (_at + _ed * 86400L);
                String _edStr = _fmtEd(_expTs);
                // 确保 Loading 已关闭
                try { _lw.getMethod("close").invoke(null); } catch (Throwable _ig) {}

                Boolean _reauth = (Boolean) _lw.getMethod("showExpiredDialog", String.class)
                    .invoke(null, _edStr);
                if (!Boolean.TRUE.equals(_reauth)) {
                    System.exit(0);
                    return false;
                }
                _lm.getMethod("_entry", int.class, Object[].class)
                    .invoke(null, 7, null);
                _sk = null;
                _sa = false;
            }

            // 情况3：首次启动（无保存的密钥）或重新激活
            _lw.getMethod("display").invoke(null);
            // 短暂显示 Loading 后关闭
            try { Thread.sleep(1200); } catch (InterruptedException _ig) {}
            _lw.getMethod("close").invoke(null);

            Object _dlg = _ld.getConstructor(String.class, boolean.class)
                    .newInstance(_sk, _sa);
            _ld.getMethod("setVisible", boolean.class).invoke(_dlg, true);
            boolean _ac = (Boolean) _ld.getMethod("isLicenseAccepted").invoke(_dlg);
            if (!_ac) {
                System.exit(0);
                return false;
            }
            // 重新加载验证状态
            _info = (Object[]) _lm.getMethod("_entry", int.class, Object[].class)
                    .invoke(null, 8, null);
            if (_info == null || !((Boolean) _info[0])) {
                return false;
            }
            return true;
        } catch (Throwable _t) {
            _t.printStackTrace();
            try {
                Class.forName(LUMI_PKG + ".LoadingWindow").getMethod("close").invoke(null);
            } catch (Throwable _ig) {}
            return true;
        }
    }

    private static String _fmtEd(long _ts) {
        if (_ts < 0) return "无限期";
        if (_ts == 0) return "已过期";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(new java.util.Date(_ts * 1000));
    }

    public static void _periodicGuard() {
        if (!loaded) return;
        try {
            Class<?> _lm = Class.forName(_CLS_LM);
            Boolean _alive = (Boolean) _lm.getMethod("_isAlive").invoke(null);
            if (_alive == null || !_alive) {
                loaded = false;
                System.err.println("[Lumi] Periodic license check failed.");
                return;
            }
            Boolean _exp = (Boolean) _lm.getMethod("_entry", int.class, Object[].class)
                .invoke(null, 11, null);
            if (_exp != null && _exp) {
                loaded = false;
                System.err.println("[Lumi] License expired.");
                try {
                    _lm.getMethod("_entry", int.class, Object[].class)
                        .invoke(null, 7, null);
                } catch (Throwable _ig) {}
            }
        } catch (Throwable _ignored) {
        }
    }
    // endregion

    public static void _setLicenseVerified() {
        loaded = true;
    }
}