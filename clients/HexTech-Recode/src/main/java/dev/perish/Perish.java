package dev.perish;

import dev.perish.api.events.eventbus.EventBus;
import dev.perish.api.events.impl.InitEvent;
import dev.perish.api.utils.entity.MioPredictManager;
import dev.perish.core.impl.*;
import dev.perish.api.events.eventbus.LambdaListener;
import dev.perish.mod.gui.hud.HudEditorManager;
import dev.perish.mod.gui.hud.ModuleHudComponent;
import dev.perish.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.api.ModInitializer;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class Perish implements ModInitializer {
    public static final String ClientName = "Perish-Hyperion";
    public static final String VERSION = "0.1";
    public static final boolean nightly = true;
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
    public static NetworkManager NETWORK;
    public static ThreadManager THREAD;
    public static HudEditorManager HUD_EDITOR;
    public static MioPredictManager MIO_PREDICT;
    public static boolean loaded;
    public static long initTime;

    public static String getPrefix() {
        if (ClientSetting.INSTANCE == null) {
            return ".";
        }
        return ClientSetting.INSTANCE.prefix.getValue();
    }

    public static void save() {
        if (!loaded || CONFIG == null) {
            return;
        }

        System.out.println("[" + ClientName + "]" + " Saving");
        CONFIG.save();
        if (CLEANER != null) CLEANER.save();
        if (FRIEND != null) FRIEND.save();
        if (XRAY != null) XRAY.save();
        if (TRADE != null) TRADE.save();
        if (HUD_EDITOR != null) {
            HUD_EDITOR.savePositions();
        }
        System.out.println("[" + ClientName + "]" + " Saved");
    }

    @Override
    public void onInitialize() {
        load();
    }


    private void load() {
        register();
        bootstrapCore();
        createConfigFolder();
    }

    private static synchronized void bootstrapCore() {
        if (loaded) {
            return;
        }

        CONFIG = new ConfigManager();
        HOLE = new HoleManager();
        MODULE = new ModuleManager();
        COMMAND = new CommandManager();
        FRIEND = new FriendManager();
        XRAY = new XrayManager();
        CLEANER = new CleanerManager();
        TRADE = new TradeManager();
        ROTATION = new RotationManager();
        BREAK = new BreakManager();
        PLAYER = new PlayerManager();
        POP = new PopManager();
        TIMER = new TimerManager();
        SHADER = new ShaderManager();
        BLUR = new BlurManager();
        FPS = new FPSManager();
        SERVER = new ServerManager();
        NETWORK = new NetworkManager();
        THREAD = new ThreadManager();
        MIO_PREDICT = new MioPredictManager();
        HUD_EDITOR = new HudEditorManager();
        HUD_EDITOR.register(new ModuleHudComponent("arraylist", "ArrayList", 0, 0));
        HUD_EDITOR.register(new ModuleHudComponent("armor", "Armor", 0, 0));
        HUD_EDITOR.register(new ModuleHudComponent("watermark", "WaterMark", 5, 5));
        HUD_EDITOR.register(new ModuleHudComponent("infolist", "InfoList", 0, 0));
        HUD_EDITOR.register(new ModuleHudComponent("coords", "Coords", 5, 0));
        HUD_EDITOR.register(new ModuleHudComponent("potions", "Potions", 5, 100));
        HUD_EDITOR.register(new ModuleHudComponent("textradar", "TextRadar", 5, 30));
        HUD_EDITOR.register(new ModuleHudComponent("itemscounter", "Items", 100, 100));
        HUD_EDITOR.register(new ModuleHudComponent("targethud", "TargetHud", 0, 0));
        HUD_EDITOR.register(new ModuleHudComponent("keybinds", "KeyBinds", 5, 100));
        HUD_EDITOR.register(new ModuleHudComponent("crosshair", "Crosshair", 0, 0));
        HUD_EDITOR.register(new ModuleHudComponent("lyric", "Lyric", 0, 0));
        CONFIG.load();
        HUD_EDITOR.loadPositions();
        initTime = System.currentTimeMillis();
        EVENT_BUS.post(new InitEvent());
        loaded = true;
    }

    private static void createConfigFolder() {
        File folder = new File(MinecraftClient.getInstance().runDirectory.getPath() + File.separator + "Perish-Hyperion".toLowerCase() + File.separator + "cfg");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    private static void register() {
        EVENT_BUS.registerLambdaFactory(new LambdaFactoryBridge());
        Runtime.getRuntime().addShutdownHook(new Thread(Perish::onShutdown));
    }

    private static void onShutdown() {
        if (loaded) {
            save();
        }
    }

    private static final class LambdaFactoryBridge implements LambdaListener.Factory {
        @Override
        public MethodHandles.Lookup create(Method lookupInMethod, Class<?> klass) throws InvocationTargetException, IllegalAccessException {
            return (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup());
        }
    }
}
