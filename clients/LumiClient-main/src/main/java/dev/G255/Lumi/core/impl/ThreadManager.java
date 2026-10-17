/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  by.radioegor146.nativeobfuscator.Native
 *  com.google.common.collect.Lists
 *  net.minecraft.client.network.AbstractClientPlayerEntity
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.BlockPos
 */
package dev.G255.Lumi.core.impl;

import com.google.common.collect.Lists;
import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.Wrapper;
import dev.G255.Lumi.api.utils.render.JelloUtil;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.client.ClientSetting;
import dev.G255.Lumi.mod.modules.impl.combat.AutoAnchor;
import dev.G255.Lumi.mod.modules.impl.combat.AutoCrystal;
import dev.G255.Lumi.mod.modules.impl.render.HoleESP;
import dev.G255.Lumi.mod.modules.impl.render.PlaceRender;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadManager
implements Wrapper {
    // use a bounded pool to avoid unbounded thread creation
    public static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public static ClientService clientService;
    public volatile Iterable<Entity> threadSafeEntityList = Collections.emptyList();
    public volatile List<AbstractClientPlayerEntity> threadSafePlayersList = Collections.emptyList();
    public volatile boolean tickRunning = false;

    public ThreadManager() {
        this.init();
    }

    public void init() {
        Lumi.EVENT_BUS.subscribe(this);
        clientService = new ClientService();
        clientService.setName("LumiClientService");
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

    @EventListener(priority=200)
    public void onEvent(ClientTickEvent event) {
        Lumi.POP.onUpdate();
        Lumi.SERVER.onUpdate();
        if (event.isPre()) {
            JelloUtil.updateJello();
            this.tickRunning = true;
            BlockUtil.placedPos.forEach(pos -> PlaceRender.INSTANCE.create((BlockPos)pos));
            BlockUtil.placedPos.clear();
            Lumi.PLAYER.onUpdate();
            if (!Module.nullCheck()) {
                Lumi.EVENT_BUS.post(UpdateEvent.INSTANCE);
            }
        } else {
            this.tickRunning = false;
            if (ThreadManager.mc.world == null || ThreadManager.mc.player == null) {
                return;
            }
            this.threadSafeEntityList = Lists.newArrayList((Iterable)ThreadManager.mc.world.getEntities());
            this.threadSafePlayersList = Lists.newArrayList((Iterable)ThreadManager.mc.world.getPlayers());
        }
        if (!clientService.isAlive() || clientService.isInterrupted()) {
            clientService = new ClientService();
            clientService.setName("LumiService");
            clientService.setDaemon(true);
            clientService.start();
        }
    }

    public class ClientService
    extends Thread {
        @Override
        public void run() {
            long lastErrorTime = 0L;
            while (true) {
                try {
                    if (ThreadManager.this.tickRunning) {
                        // when waiting for a tick, yield a bit instead of spinning
                        Thread.sleep(5);
                    } else {
                        AutoCrystal.INSTANCE.onThread();
                        HoleESP.INSTANCE.onThread();
                        AutoAnchor.INSTANCE.onThread();
                        // small pause between iterations to avoid busy loop
                        Thread.sleep(10);
                    }
                } catch (InterruptedException ie) {
                    // propagate interruption, exit thread
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    if (ClientSetting.INSTANCE.debug.getValue()) {
                        CommandManager.sendMessage("\u00a74An error has occurred [Thread] Message: [" + e.getMessage() + "]");
                    }
                    // cooldown before retrying after an error
                    try {
                        long now = System.currentTimeMillis();
                        if (now - lastErrorTime < 50L) {
                            Thread.sleep(50L - (now - lastErrorTime));
                        } else {
                            Thread.sleep(50L);
                        }
                        lastErrorTime = System.currentTimeMillis();
                    } catch (InterruptedException ie2) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
}
