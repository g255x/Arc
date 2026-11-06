package dev.g255.arc.core.impl;

import com.google.common.collect.Lists;
import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.render.JelloUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.combat.AutoAnchor;
import dev.g255.arc.mod.modules.impl.render.HoleESP;
import dev.g255.arc.mod.modules.impl.render.PlaceRender;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.entity.Entity;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class ThreadManager implements Wrapper {
   public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
   public static ClientService clientService;
   public volatile Iterable<Entity> threadSafeEntityList = Collections.emptyList();
   public volatile List<AbstractClientPlayerEntity> threadSafePlayersList = Collections.emptyList();
   public volatile boolean tickRunning = false;

   public ThreadManager() {
      this.init();
   }

   public void init() {
      Arc.EVENT_BUS.subscribe(this);
      clientService = new ClientService();
      clientService.setName("ArcClientService");
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
      EXECUTOR.execute(runnable);
   }

   @EventListener(
      priority = 200
   )
   public void onEvent(ClientTickEvent event) {
      Arc.POP.onUpdate();
      Arc.SERVER.onUpdate();
      if (event.isPre()) {
         JelloUtil.updateJello();
         this.tickRunning = true;
         BlockUtil.placedPos.forEach((pos) -> PlaceRender.INSTANCE.create(pos));
         BlockUtil.placedPos.clear();
         Arc.PLAYER.onUpdate();
         if (!Module.nullCheck()) {
            Arc.EVENT_BUS.post(UpdateEvent.INSTANCE);
         }
      } else {
         this.tickRunning = false;
         if (mc.world == null || mc.player == null) {
            return;
         }

         this.threadSafeEntityList = Lists.newArrayList(mc.world.getEntities());
         this.threadSafePlayersList = Lists.newArrayList(mc.world.getPlayers());
      }

      if (!clientService.isAlive() || clientService.isInterrupted()) {
         clientService = new ClientService();
         clientService.setName("Arc Service [G255]");
         clientService.setDaemon(true);
         clientService.start();
      }

   }

   public class ClientService extends Thread {
      public void run() {
         while(true) {
            try {
               if (ThreadManager.this.tickRunning) {
                  Thread.onSpinWait();
               } else {
                  HoleESP.INSTANCE.onThread();
                  AutoAnchor.INSTANCE.onThread();
               }
            } catch (Exception e) {
               e.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred [Thread] Message: [" + e.getMessage() + "]");
               }
            }
         }
      }
   }
}
