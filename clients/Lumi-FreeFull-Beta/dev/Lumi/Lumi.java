package dev.Lumi;

import dev.Lumi.api.events.eventbus.EventBus;
import dev.Lumi.api.events.impl.InitEvent;
import dev.Lumi.core.impl.BlurManager;
import dev.Lumi.core.impl.BreakManager;
import dev.Lumi.core.impl.CommandManager;
import dev.Lumi.core.impl.ConfigManager;
import dev.Lumi.core.impl.FPSManager;
import dev.Lumi.core.impl.FriendManager;
import dev.Lumi.core.impl.HoleManager;
import dev.Lumi.core.impl.LicenseDialog;
import dev.Lumi.core.impl.LicenseManager;
import dev.Lumi.core.impl.ModuleManager;
import dev.Lumi.core.impl.PlayerManager;
import dev.Lumi.core.impl.PopManager;
import dev.Lumi.core.impl.RotationManager;
import dev.Lumi.core.impl.ServerManager;
import dev.Lumi.core.impl.ShaderManager;
import dev.Lumi.core.impl.ThreadManager;
import dev.Lumi.core.impl.TimerManager;
import dev.Lumi.mod.modules.impl.client.ClientSetting;
import dev.Lumi.mod.modules.impl.misc.KillEffect;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CountDownLatch;
import javax.swing.SwingUtilities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.class_310;

public class Lumi implements ModInitializer {
   public static final String NAME = "Lumi Client";
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
      FRIEND.save();
      System.out.println("[Lumi Client] Saved");
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
      System.setProperty("java.awt.headless", "false");
      LicenseManager.LicenseInfo info = LicenseManager.loadLicenseInfo();
      if (!info.verified) {
         CountDownLatch latch = new CountDownLatch(1);
         boolean[] accepted = new boolean[]{false};
         SwingUtilities.invokeLater(() -> {
            try {
               LicenseDialog dialog = new LicenseDialog(info.savedKey, info.agreementAccepted);
               dialog.setVisible(true);
               accepted[0] = dialog.isLicenseAccepted();
            } finally {
               latch.countDown();
            }

         });

         try {
            latch.await();
         } catch (InterruptedException var5) {
            Thread.currentThread().interrupt();
         }

         if (!accepted[0]) {
            System.exit(0);
            return;
         }
      }

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
      BLUR = new BlurManager();
      FPS = new FPSManager();
      SERVER = new ServerManager();
      CONFIG.load();
      KillEffect.registerSounds();
      THREAD = new ThreadManager();
      initTime = System.currentTimeMillis();
      loaded = true;
      EVENT_BUS.post(new InitEvent());
      String var10002 = class_310.method_1551().field_1697.getPath();
      File folder = new File(var10002 + File.separator + "lumi" + File.separator + "cfg");
      if (!folder.exists()) {
         folder.mkdirs();
      }

   }
}
