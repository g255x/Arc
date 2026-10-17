package dev.baimaodaxia;

import dev.baimaodaxia.api.events.eventbus.EventBus;
import dev.baimaodaxia.api.events.impl.InitEvent;
import dev.baimaodaxia.core.impl.BlurManager;
import dev.baimaodaxia.core.impl.BreakManager;
import dev.baimaodaxia.core.impl.CleanerManager;
import dev.baimaodaxia.core.impl.CommandManager;
import dev.baimaodaxia.core.impl.ConfigManager;
import dev.baimaodaxia.core.impl.FPSManager;
import dev.baimaodaxia.core.impl.FriendManager;
import dev.baimaodaxia.core.impl.HoleManager;
import dev.baimaodaxia.core.impl.MainMenuProgram;
import dev.baimaodaxia.core.impl.ModuleManager;
import dev.baimaodaxia.core.impl.NetworkManager;
import dev.baimaodaxia.core.impl.PlayerManager;
import dev.baimaodaxia.core.impl.PopManager;
import dev.baimaodaxia.core.impl.RotationManager;
import dev.baimaodaxia.core.impl.ServerManager;
import dev.baimaodaxia.core.impl.ShaderManager;
import dev.baimaodaxia.core.impl.ThreadManager;
import dev.baimaodaxia.core.impl.TimerManager;
import dev.baimaodaxia.core.impl.TradeManager;
import dev.baimaodaxia.core.impl.XrayManager;
import dev.baimaodaxia.mod.features.SpearPort;
import dev.baimaodaxia.mod.gui.MainMenuScreen;
import dev.baimaodaxia.mod.modules.impl.client.ClientSetting;
import java.io.File;
import java.lang.invoke.MethodHandles;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
public class earthquake implements ModInitializer {
   private static final int AES_KEY_SIZE = 256;
   private static final int GCM_IV_LENGTH = 12;
   private static final int GCM_TAG_LENGTH = 16;
   private static final int PBKDF2_ITERATIONS = 600000;
   private static final int SALT_LENGTH = 16;
   public static final String NAME = "earthquake";
   public static final String VERSION = "2.1";
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
   public static NetworkManager NETWORK;
   public static BlurManager BLUR;
   public static FPSManager FPS;
   public static ServerManager SERVER;
   public static ThreadManager THREAD;
   public static MainMenuProgram MAIN_MENU;
   public static boolean loaded = false;
   public static long initTime;

   public static String getPrefix() {
      return ClientSetting.INSTANCE.prefix.getValue();
   }

   public static void save() {
      CONFIG.writeToFile();
      CLEANER.save();
      FRIEND.save();
      XRAY.save();
      TRADE.save();
      System.out.println("[earthquake] Saved");
   }

   private void register() {
      EVENT_BUS.registerLambdaFactory((lookupInMethod, klass) -> (MethodHandles.Lookup)lookupInMethod.invoke((Object)null, klass, MethodHandles.lookup()));
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         if (loaded) {
            save();
         }

      }));
   }

   public void onInitialize() {
      this.register();
      dev.baimaodaxia.api.utils.rotation.RotationManager.init();
      SpearPort.init();
      MODULE = new ModuleManager();
      Alien.MODULE = MODULE;
      CONFIG = new ConfigManager();
      HOLE = new HoleManager();
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
      MAIN_MENU = new MainMenuProgram();
      Alien.CONFIG = CONFIG;
      Alien.HOLE = HOLE;
      Alien.MODULE = MODULE;
      Alien.COMMAND = COMMAND;
      Alien.FRIEND = FRIEND;
      Alien.XRAY = XRAY;
      Alien.CLEANER = CLEANER;
      Alien.TRADE = TRADE;
      Alien.ROTATION = ROTATION;
      Alien.BREAK = BREAK;
      Alien.PLAYER = PLAYER;
      Alien.POP = POP;
      Alien.TIMER = TIMER;
      Alien.SHADER = SHADER;
      Alien.BLUR = BLUR;
      Alien.FPS = FPS;
      Alien.SERVER = SERVER;
      CONFIG.load();
      THREAD = new ThreadManager();
      Alien.THREAD = THREAD;
      Alien.initTime = initTime = System.currentTimeMillis();
      loaded = true;
      Alien.loaded = true;
      EVENT_BUS.post(new InitEvent());
      String var10002 = MinecraftClient.getInstance().runDirectory.getPath();
      File folder = new File(var10002 + File.separator + "earthquake".toLowerCase() + File.separator + "cfg");
      if (!folder.exists()) {
         folder.mkdirs();
      }

      ScreenEvents.AFTER_INIT.register((ScreenEvents.AfterInit)(client, screen, scaledWidth, scaledHeight) -> {
         if (screen instanceof TitleScreen) {
            client.setScreen(new MainMenuScreen());
         }

      });
   }
}
