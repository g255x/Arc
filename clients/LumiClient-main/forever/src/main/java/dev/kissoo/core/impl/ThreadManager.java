package dev.kissoo.core.impl;


import com.google.common.collect.Lists;
import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.Wrapper;
import dev.kissoo.api.utils.render.JelloUtil;
import dev.kissoo.api.utils.world.BlockUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.mod.modules.impl.combat.AutoAnchor;
import dev.kissoo.mod.modules.impl.render.HoleESP;
import dev.kissoo.mod.modules.impl.render.PlaceRender;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

public class ThreadManager implements Wrapper {
   private static volatile boolean shuttingDown;
   public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
      Thread thread = new Thread(r, "Forever-Worker");
      thread.setDaemon(true);
      return thread;
   });
   public static ThreadManager.ClientService clientService;
   public volatile Iterable<Entity> threadSafeEntityList = Collections.emptyList();
   public volatile List<AbstractClientPlayerEntity> threadSafePlayersList = Collections.emptyList();
   public volatile boolean tickRunning = false;

   public ThreadManager() {
      this.init();
   }

   
   public void init() {
      Forever.EVENT_BUS.subscribe(this);
      clientService = new ThreadManager.ClientService();
      clientService.setName("AlienClientService");
      clientService.setDaemon(true);
      clientService.start();
   }

   public Iterable<Entity> getEntities() {
      return this.threadSafeEntityList;
   }

   public List<AbstractClientPlayerEntity> getPlayers() {
      return this.threadSafePlayersList;
   }

   public void execute(Runnable runnable) {
      if (!shuttingDown) {
         EXECUTOR.execute(runnable);
      }
   }

   public static void shutdown() {
      shuttingDown = true;
      if (clientService != null) {
         clientService.interrupt();
      }
      EXECUTOR.shutdownNow();
   }

   @EventListener(priority = 200)
   public void onEvent(ClientTickEvent event) {
      Forever.POP.onUpdate();
      Forever.SERVER.onUpdate();
      if (event.isPre()) {
         JelloUtil.updateJello();
         this.tickRunning = true;
         BlockUtil.placedPos.forEach(pos -> PlaceRender.INSTANCE.create(pos));
         BlockUtil.placedPos.clear();
         Forever.PLAYER.onUpdate();
         if (!Module.nullCheck()) {
            Forever.EVENT_BUS.post(UpdateEvent.INSTANCE);
         }
      } else {
         this.tickRunning = false;
         if (mc.world == null || mc.player == null) {
            return;
         }

         this.threadSafeEntityList = Lists.newArrayList(mc.world.getEntities());
         this.threadSafePlayersList = Lists.newArrayList(mc.world.getPlayers());
      }

      if (!shuttingDown && (!clientService.isAlive() || clientService.isInterrupted())) {
         clientService = new ThreadManager.ClientService();
         clientService.setName("AlienService");
         clientService.setDaemon(true);
         clientService.start();
      }
   }

   public class ClientService extends Thread {
      public void run() {
         while (!Thread.currentThread().isInterrupted() && !shuttingDown) {
            try {
               while (ThreadManager.this.tickRunning && !Thread.currentThread().isInterrupted() && !shuttingDown) {
                  Thread.onSpinWait();
               }
               if (Thread.currentThread().isInterrupted() || shuttingDown) {
                  break;
               }
               // INSTANCE 可能在模块尚未注册完成时为 null，判空避免空转刷异常
               if (HoleESP.INSTANCE != null) {
                  HoleESP.INSTANCE.onThread();
               }
               if (AutoAnchor.INSTANCE != null) {
                  AutoAnchor.INSTANCE.onThread();
               }
            } catch (Exception var2) {
               var2.printStackTrace();
               if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred [Thread] Message: [" + var2.getMessage() + "]");
               }
               // 关键：异常后短暂休眠，防止持续抛错时死循环刷屏冲爆日志、冻结启动器
               try {
                  Thread.sleep(500L);
               } catch (InterruptedException interrupted) {
                  Thread.currentThread().interrupt();
                  break;
               }
            }
         }
      }
   }
}
