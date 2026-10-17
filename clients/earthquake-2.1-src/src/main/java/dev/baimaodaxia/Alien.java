package dev.baimaodaxia;

import dev.baimaodaxia.api.events.eventbus.EventBus;
import dev.baimaodaxia.core.impl.BlurManager;
import dev.baimaodaxia.core.impl.BreakManager;
import dev.baimaodaxia.core.impl.CleanerManager;
import dev.baimaodaxia.core.impl.CommandManager;
import dev.baimaodaxia.core.impl.ConfigManager;
import dev.baimaodaxia.core.impl.FPSManager;
import dev.baimaodaxia.core.impl.FriendManager;
import dev.baimaodaxia.core.impl.HoleManager;
import dev.baimaodaxia.core.impl.ModuleManager;
import dev.baimaodaxia.core.impl.PlayerManager;
import dev.baimaodaxia.core.impl.PopManager;
import dev.baimaodaxia.core.impl.RotationManager;
import dev.baimaodaxia.core.impl.ServerManager;
import dev.baimaodaxia.core.impl.ShaderManager;
import dev.baimaodaxia.core.impl.ThreadManager;
import dev.baimaodaxia.core.impl.TimerManager;
import dev.baimaodaxia.core.impl.TradeManager;
import dev.baimaodaxia.core.impl.XrayManager;
public class Alien {
   public static final String NAME = "earthquake";
   public static final String VERSION = "2.1";
   public static final EventBus EVENT_BUS;
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

   public static String getPrefix() {
      return earthquake.getPrefix();
   }

   public static void save() {
      earthquake.save();
   }

   static {
      EVENT_BUS = earthquake.EVENT_BUS;
      HOLE = earthquake.HOLE;
      PLAYER = earthquake.PLAYER;
      TRADE = earthquake.TRADE;
      CLEANER = earthquake.CLEANER;
      XRAY = earthquake.XRAY;
      MODULE = earthquake.MODULE;
      COMMAND = earthquake.COMMAND;
      CONFIG = earthquake.CONFIG;
      ROTATION = earthquake.ROTATION;
      BREAK = earthquake.BREAK;
      POP = earthquake.POP;
      FRIEND = earthquake.FRIEND;
      TIMER = earthquake.TIMER;
      SHADER = earthquake.SHADER;
      BLUR = earthquake.BLUR;
      FPS = earthquake.FPS;
      SERVER = earthquake.SERVER;
      THREAD = earthquake.THREAD;
      loaded = earthquake.loaded;
      initTime = earthquake.initTime;
   }
}
