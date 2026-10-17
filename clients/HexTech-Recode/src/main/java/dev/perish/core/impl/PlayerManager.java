package dev.perish.core.impl;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.BlockActivateEvent;
import dev.perish.api.events.impl.GameLeftEvent;
import dev.perish.api.events.impl.OpenScreenEvent;
import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.world.BlockPosX;
import dev.perish.mod.modules.Module;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.inventory.Inventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Blocks;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Box;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.attribute.EntityAttributes;

public class PlayerManager implements Wrapper {
    public static Screen screenToOpen;
    public final DefaultedList<ItemStack> ENDERCHEST_ITEM = DefaultedList.ofSize(27, ItemStack.EMPTY);
    public CopyOnWriteArrayList<PlayerEntity> burrowPlayers = new CopyOnWriteArrayList<>();
    public final Map<PlayerEntity, PlayerManager.EntityAttribute> map = new ConcurrentHashMap();
    public final CopyOnWriteArrayList<PlayerEntity> inWebPlayers = new CopyOnWriteArrayList();
    public boolean known = false;
    private int echestOpenedState;

    public PlayerManager() {
        Perish.EVENT_BUS.subscribe(this);
    }

    @EventListener
    public void onLogout(GameLeftEvent event) {
        this.inWebPlayers.clear();
        this.map.clear();
        this.ENDERCHEST_ITEM.clear();
        this.known = false;
    }

    @EventListener
    private void onBlockActivate(BlockActivateEvent event) {
        if (event.blockState.getBlock() instanceof EnderChestBlock && this.echestOpenedState == 0) {
            this.echestOpenedState = 1;
        }
    }
    public boolean isInBurrow(PlayerEntity player) {
        return burrowPlayers.contains(player);
    }

    @EventListener
    private void onOpenScreenEvent(OpenScreenEvent event) {
        if (this.echestOpenedState == 1 && event.screen instanceof GenericContainerScreen) {
            this.echestOpenedState = 2;
        } else if (this.echestOpenedState != 0) {
            if (mc.currentScreen instanceof GenericContainerScreen) {
                GenericContainerScreenHandler container = ((GenericContainerScreen) mc.currentScreen).getScreenHandler();
                if (container != null) {
                    Inventory inv = container.getInventory();

                    for (int i = 0; i < 27; i++) {
                        this.ENDERCHEST_ITEM.set(i, inv.getStack(i));
                    }

                    this.known = true;
                    this.echestOpenedState = 0;
                }
            }
        }
    }

    public void onUpdate() {
        if (!Module.nullCheck()) {
            if (screenToOpen != null && mc.currentScreen == null) {
                mc.setScreen(screenToOpen);
                screenToOpen = null;
            }

            this.inWebPlayers.clear();

            for (PlayerEntity player : Perish.THREAD.getPlayers()) {
                this.map.put(player, new PlayerManager.EntityAttribute(player.getArmor(), player.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)));
                this.webUpdate(player);
            }
        }
    }

    public boolean isInWeb(PlayerEntity player) {
        return this.inWebPlayers.contains(player);
    }

    private void webUpdate(PlayerEntity player) {
        for (float x : new float[]{0.0F, 0.3F, -0.3F}) {
            for (float z : new float[]{0.0F, 0.3F, -0.3F}) {
                for (int y : new int[]{-1, 0, 1, 2}) {
                    BlockPos pos = new BlockPosX(player.getX() + x, player.getY(), player.getZ() + z).up(y);
                    if (new Box(pos).intersects(player.getBoundingBox()) && mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
                        this.inWebPlayers.add(player);
                        return;
                    }
                }
            }
        }
    }

    public record EntityAttribute(int armor, double toughness) {
    }
}
