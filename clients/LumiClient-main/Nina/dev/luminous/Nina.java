package dev.luminous;

import dev.luminous.api.events.eventbus.EventBus;
import dev.luminous.api.events.impl.InitEvent;
import dev.luminous.core.impl.BlurManager;
import dev.luminous.core.impl.BreakManager;
import dev.luminous.core.impl.CleanerManager;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.core.impl.ConfigManager;
import dev.luminous.core.impl.FPSManager;
import dev.luminous.core.impl.FriendManager;
import dev.luminous.core.impl.HoleManager;
import dev.luminous.core.impl.ModuleManager;
import dev.luminous.core.impl.PlayerManager;
import dev.luminous.core.impl.PopManager;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.core.impl.ServerManager;
import dev.luminous.core.impl.ShaderManager;
import dev.luminous.core.impl.ThreadManager;
import dev.luminous.core.impl.TimerManager;
import dev.luminous.core.impl.TradeManager;
import dev.luminous.core.impl.XrayManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import j2cc.Nativeify;
import java.io.File;
import java.lang.invoke.MethodHandles;
import net.fabricmc.api.ModInitializer;
import net.minecraft.class_310;

@Nativeify
public class Nina implements ModInitializer {
   private static final int AES_KEY_SIZE = 256;
   private static final int GCM_IV_LENGTH = 12;
   private static final int GCM_TAG_LENGTH = 16;
   private static final int PBKDF2_ITERATIONS = 600000;
   private static final int SALT_LENGTH = 16;
   public static final String NAME = "Alien";
   public static final String VERSION = "More";
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
   public static boolean loaded = false;
   public static long initTime;

   public static String getPrefix() {
      return ClientSetting.INSTANCE.prefix.getValue();
   }

   public static void save() {
      CONFIG.save();
      CLEANER.save();
      FRIEND.save();
      XRAY.save();
      TRADE.save();
      System.out.println("[Nina] Saved");
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
      dev.luminous.api.utils.rotation.RotationManager.init();
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
      String var10002 = class_310.method_1551().field_1697.getPath();
      File folder = new File(var10002 + File.separator + "Alien".toLowerCase() + File.separator + "cfg");
      if (!folder.exists()) {
         folder.mkdirs();
      }

   }
}
