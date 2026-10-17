package dev.kissoo;

import hoprc.api.events.eventbus.EventBus;
import dev.kissoo.api.events.impl.InitEvent;
import dev.kissoo.auth.AuthBootstrap;
import dev.kissoo.core.impl.BlurManager;
import dev.kissoo.core.impl.BreakManager;
import dev.kissoo.core.impl.CleanerManager;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.core.impl.ConfigManager;
import dev.kissoo.core.impl.FPSManager;
import dev.kissoo.core.impl.FriendManager;
import dev.kissoo.core.impl.HoleManager;
import dev.kissoo.core.impl.ModuleManager;
import dev.kissoo.core.impl.PlayerManager;
import dev.kissoo.core.impl.PopManager;
import dev.kissoo.core.impl.RotationManager;
import dev.kissoo.core.impl.ServerManager;
import hoprc.api.core.impl.ShaderManager;
import dev.kissoo.core.impl.ThreadManager;
import dev.kissoo.core.impl.TimerManager;
import dev.kissoo.core.impl.TradeManager;
import dev.kissoo.core.impl.XrayManager;
import dev.kissoo.mod.spearfix.SpearFix;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.chat.ChatClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

import java.io.File;
import java.lang.invoke.MethodHandles;

public final class Forever {
   public static final String NAME = "Forever";
   public static final EventBus EVENT_BUS = new EventBus();
   public static HoleManager HOLE;
   public static PlayerManager PLAYER;
   public static TradeManager TRADE;
   public static CleanerManager CLEANER;
   public static XrayManager XRAY;
   public static ModuleManager MODULE;
   public static CommandManager COMMAND;
   public static ConfigManager CONFIG;
   public static RotationManager ROTATION;
   public static BreakManager BREAK;
   public static PopManager POP;
   public static FriendManager FRIEND;
   public static TimerManager TIMER;
   public static ShaderManager SHADER;
   public static BlurManager BLUR;
   public static FPSManager FPS;
   public static ServerManager SERVER;
   public static ThreadManager THREAD;
   public static boolean loaded;
   public static long initTime;
    private Forever() {
    }

    public static String getPrefix() {
        return ClientSetting.INSTANCE != null ? ClientSetting.INSTANCE.prefix.getValue() : ";";
    }

    public static void save() {
        System.out.println("[Forever] Saving");
        CONFIG.save();
        CLEANER.save();
        FRIEND.save();
        XRAY.save();
        TRADE.save();
        System.out.println("[Forever] Saved");
    }

    private static void register() {
        EVENT_BUS.registerLambdaFactory((lookupInMethod, klass) -> (MethodHandles.Lookup)lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cleanupOnExit();
            if (loaded) {
                save();
            }
            // NOTE: we deliberately do NOT stop ZenithProxy here — it must keep
            // running after MC closes so it holds the queue position. The user's
            // OS / ZenithProxy itself manages its lifecycle.
        }, "Forever-Shutdown"));
    }

    private static void cleanupOnExit() {
        try {
            dev.kissoo.mod.gui.SplashScreen.ensureClosed();
        } catch (Throwable ignored) {}
        try {
            ChatClient.stop();
        } catch (Throwable ignored) {}
        try {
            ThreadManager.shutdown();
        } catch (Throwable ignored) {}
    }

    private static void closeSplashQuietly() {
        try {
            dev.kissoo.mod.gui.SplashScreen.ensureClosed();
        } catch (Throwable ignored) {}
    }


    public static synchronized boolean initializeCore(net.minecraft.client.MinecraftClient mc) {
        if (loaded) {
            return true;
        }
        if (mc == null) {
            return false;
        }
        AuthBootstrap.bootstrap();
        CONFIG   = new ConfigManager();
        SpearFix.init();
        HOLE     = new HoleManager();
        MODULE   = new ModuleManager();
        COMMAND  = new CommandManager();
        ChatClient.init();
        FRIEND   = new FriendManager();
        XRAY     = new XrayManager();
        CLEANER  = new CleanerManager();
        TRADE    = new TradeManager();
        ROTATION = new RotationManager();
        BREAK    = new BreakManager();
        PLAYER   = new PlayerManager();
        POP      = new PopManager();
        TIMER    = new TimerManager();
        SHADER   = new ShaderManager();
        BLUR     = new BlurManager();
        FPS      = new FPSManager();
        SERVER   = new ServerManager();
        THREAD   = new ThreadManager();
        CONFIG.load();

        File folder = new File(mc.runDirectory.getPath()
                + File.separator + "Alien".toLowerCase()
                + File.separator + "cfg");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        initTime = System.currentTimeMillis();
        loaded = true;
        EVENT_BUS.post(new InitEvent());
        closeSplashQuietly();
        return true;
    }


    public static void initialize() {
        dev.kissoo.core.impl.IntegrityCheck.verify(); // 防篡改 jar SHA 校验（嵌入 sha256.txt 时生效）
        SpearFix.initModelLoading();
        dev.kissoo.mod.gui.background.PhobosBackground.registerShaders();

        try {
            dev.kissoo.mod.gui.SplashScreen.showSwingSplash();
        } catch (Throwable t) {
            System.err.println("[Forever] Splash screen failed; continuing startup.");
            t.printStackTrace();
        }

        register();

        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
            if (initializeCore(mc)) return;
            // ── Manager initialisation ───────────────────────────────────────
            CONFIG   = new ConfigManager();
            SpearFix.init();
            HOLE     = new HoleManager();
            MODULE   = new ModuleManager();
            COMMAND  = new CommandManager();
            ChatClient.init();
            FRIEND   = new FriendManager();
            XRAY     = new XrayManager();
            CLEANER  = new CleanerManager();
            TRADE    = new TradeManager();
            ROTATION = new RotationManager();
            BREAK    = new BreakManager();
            PLAYER   = new PlayerManager();
            POP      = new PopManager();
            TIMER    = new TimerManager();
            SHADER   = new ShaderManager();
            BLUR     = new BlurManager();
            FPS      = new FPSManager();
            SERVER   = new ServerManager();
            THREAD   = new ThreadManager();
            CONFIG.load();

            File folder = new File(mc.runDirectory.getPath()
                    + File.separator + "Alien".toLowerCase()
                    + File.separator + "cfg");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            initTime = System.currentTimeMillis();
            loaded   = true;
            EVENT_BUS.post(new InitEvent());
            closeSplashQuietly();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> cleanupOnExit());
    }
}
