package dev.g255.arc;

import dev.g255.arc.api.events.eventbus.EventBus;
import dev.g255.arc.api.events.impl.InitEvent;
import dev.g255.arc.core.impl.BlurManager;
import dev.g255.arc.core.impl.BreakManager;
import dev.g255.arc.core.impl.CleanerManager;
import dev.g255.arc.core.impl.CommandManager;
import dev.g255.arc.core.impl.ConfigManager;
import dev.g255.arc.core.impl.FPSManager;
import dev.g255.arc.core.impl.FriendManager;
import dev.g255.arc.core.impl.HudItemManager;
import dev.g255.arc.core.impl.HoleManager;
import dev.g255.arc.core.impl.ModuleManager;
import dev.g255.arc.core.impl.PlayerManager;
import dev.g255.arc.core.impl.PopManager;
import dev.g255.arc.core.impl.RotationManager;
import dev.g255.arc.core.impl.ServerManager;
import dev.g255.arc.core.impl.ShaderManager;
import dev.g255.arc.core.impl.ThreadManager;
import dev.g255.arc.core.impl.TimerManager;
import dev.g255.arc.core.impl.TradeManager;
import dev.g255.arc.core.impl.XrayManager;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import java.io.File;
import java.lang.invoke.MethodHandles;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

public class Arc implements ModInitializer {
    public static final String NAME = "Arc Client";
    public static final String VERSION = "1.04-improved";
    public static final String CONFIG_DIR = "arc";
    public static final EventBus EVENT_BUS = new EventBus();
    public static HoleManager HOLE;
    public static PlayerManager PLAYER;
    public static TradeManager TRADE;
    public static CleanerManager CLEANER;
    public static HudItemManager HUD_ITEM;
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
    public static String userId;

    public static String getPrefix() {
        return ClientSetting.INSTANCE.prefix.getValue();
    }

    public static void save() {
        CONFIG.save();
        CLEANER.save();
        FRIEND.save();
        XRAY.save();
        TRADE.save();
        HUD_ITEM.save();
        System.out.println("[Arc Client] Saved");
    }

    private void register() {
        EVENT_BUS.registerLambdaFactory((lookupInMethod, klass) -> (MethodHandles.Lookup)lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (loaded) {
                Arc.save();
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
        XRAY = new XrayManager();
        CLEANER = new CleanerManager();
        TRADE = new TradeManager();
        HUD_ITEM = new HudItemManager();
        ROTATION = new RotationManager();
        BREAK = new BreakManager();
        PLAYER = new PlayerManager();
        POP = new PopManager();
        TIMER = new TimerManager();
        SHADER = new ShaderManager();
        BLUR = new BlurManager();
        FPS = new FPSManager();
        SERVER = new ServerManager();
        CONFIG.load();
        THREAD = new ThreadManager();

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
}