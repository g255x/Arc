package dev.luminous;

import dev.luminous.api.events.eventbus.EventBus;
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

public class Alien {
   public static final String NAME = "Alien";
   public static final String VERSION = "More";
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
      return Nina.getPrefix();
   }

   public static void save() {
      Nina.save();
   }

   static {
      EVENT_BUS = Nina.EVENT_BUS;
      HOLE = Nina.HOLE;
      PLAYER = Nina.PLAYER;
      TRADE = Nina.TRADE;
      CLEANER = Nina.CLEANER;
      XRAY = Nina.XRAY;
      MODULE = Nina.MODULE;
      COMMAND = Nina.COMMAND;
      CONFIG = Nina.CONFIG;
      ROTATION = Nina.ROTATION;
      BREAK = Nina.BREAK;
      POP = Nina.POP;
      FRIEND = Nina.FRIEND;
      TIMER = Nina.TIMER;
      SHADER = Nina.SHADER;
      BLUR = Nina.BLUR;
      FPS = Nina.FPS;
      SERVER = Nina.SERVER;
      THREAD = Nina.THREAD;
      loaded = Nina.loaded;
      initTime = Nina.initTime;
   }
}
