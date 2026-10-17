package dev.perish.core.impl;


import com.google.common.collect.Lists;
import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.ClientTickEvent;
import dev.perish.api.events.impl.GameLeftEvent;
import dev.perish.api.events.impl.InitEvent;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.render.JelloUtil;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.client.ClientSetting;
import dev.perish.mod.modules.impl.combat.AutoAnchor;
import dev.perish.mod.modules.impl.combat.AutoCrystal;
import dev.perish.mod.modules.impl.render.HoleESP;
import dev.perish.mod.modules.impl.render.PlaceRender;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.entity.Entity;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class ThreadManager implements Wrapper {
    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    public static ThreadManager.ClientService clientService;
    public volatile Iterable<Entity> threadSafeEntityList = Collections.emptyList();
    public volatile List<AbstractClientPlayerEntity> threadSafePlayersList = Collections.emptyList();
    public volatile boolean tickRunning = false;

    public ThreadManager() {
        this.init();
        Perish.EVENT_BUS.subscribe(new ThreadManager.HwidInitHandler());
    }


    public void init() {
        Perish.EVENT_BUS.subscribe(this);
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
        EXECUTOR.execute(runnable);
    }

    @EventListener
    public void onGameLeft(GameLeftEvent event) {
        this.threadSafeEntityList = Collections.emptyList();
        this.threadSafePlayersList = Collections.emptyList();
        this.tickRunning = false;
    }

    @EventListener(priority = 200)
    public void onEvent(ClientTickEvent event) {
        Perish.POP.onUpdate();
        Perish.SERVER.onUpdate();
        if (event.isPre()) {
            JelloUtil.updateJello();
            this.tickRunning = true;
            BlockUtil.placedPos.forEach(pos -> PlaceRender.INSTANCE.create(pos));
            BlockUtil.placedPos.clear();
            Perish.PLAYER.onUpdate();
            if (!Module.nullCheck()) {
                Perish.EVENT_BUS.post(UpdateEvent.INSTANCE);
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
            clientService = new ThreadManager.ClientService();
            clientService.setName("AlienService");
            clientService.setDaemon(true);
            clientService.start();
        }
    }

    public static class HwidInitHandler {
        @EventListener
        public void onInit(InitEvent event) {
            Perish.EVENT_BUS.unsubscribe(this);
        }
    }

    public class ClientService extends Thread {
        public void run() {
            while (true) {
                try {
                    while (ThreadManager.this.tickRunning) {
                        Thread.onSpinWait();
                    }

                    AutoCrystal.INSTANCE.onThread();
                    HoleESP.INSTANCE.onThread();
                    AutoAnchor.INSTANCE.onThread();
                } catch (Exception var2) {
                    var2.printStackTrace();
                    if (ClientSetting.INSTANCE.debug.getValue()) {
                        CommandManager.sendMessage("§4An error has occurred [Thread] Message: [" + var2.getMessage() + "]");
                    }
                }
            }
        }
    }
}
