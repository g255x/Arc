package dev.G255.Lumi.mod.modules.impl.player;

import dev.G255.Lumi.mod.modules.impl.misc.Friend;
import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.PacketEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.core.impl.CommandManager;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public class AntiBaseKill extends Module {

    public static AntiBaseKill INSTANCE;

    private final SliderSetting checkRange = this.add(new SliderSetting("CheckRange", 30.0, 10.0, 100.0, 1.0));
    private final BooleanSetting chest = this.add(new BooleanSetting("Chest", true).setParent());
    private final SliderSetting chestCount = this.add(new SliderSetting("ChestCount", 50.0, 1.0, 500.0, 1.0, () -> chest.isOpen()));
    private final BooleanSetting sign = this.add(new BooleanSetting("Sign", true).setParent());
    private final SliderSetting signCount = this.add(new SliderSetting("SignCount", 10.0, 1.0, 100.0, 1.0, () -> sign.isOpen()));
    private final BooleanSetting donkey = this.add(new BooleanSetting("Donkey", true).setParent());
    private final SliderSetting donkeyCount = this.add(new SliderSetting("DonkeyCount", 5.0, 1.0, 50.0, 1.0, () -> donkey.isOpen()));

    private final BooleanSetting c03Check = this.add(new BooleanSetting("C03Check", true).setParent());
    private final SliderSetting c03Range = this.add(new SliderSetting("C03Range", 1000.0, 1000.0, 5000.0, 100.0, () -> c03Check.isOpen()));

    private final Set<String> tempFriends = new HashSet<>();
    private boolean isCurrentlyInBase = false;
    private int safeguardTicks = 0;

    public AntiBaseKill() {
        super("AntiBaseKill", "Auto friend players when in base", Category.Player);
        this.setChinese("防拆家");
        this.setChineseDesc("在基地范围时自动添加附近玩家为好友");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        isCurrentlyInBase = false;
        tempFriends.clear();
        safeguardTicks = 0;
        ensureFriendModuleEnabled();
    }

    @Override
    public void onDisable() {
        clearTempFriends();
        isCurrentlyInBase = false;
        safeguardTicks = 0;
    }

    private void ensureFriendModuleEnabled() {
        if (Friend.INSTANCE != null && !Friend.INSTANCE.isOn()) {
            Friend.INSTANCE.toggle();
            CommandManager.sendMessage("§7[AntiSB] §e自动开启好友模块。");
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;

        if (mc.player.age % 20 == 0) {
            ensureFriendModuleEnabled();
        }

        boolean isBase = false;

        if (safeguardTicks > 0) {
            safeguardTicks--;
            isBase = true;
        } else {
            if (!chest.getValue() && !sign.getValue() && !donkey.getValue()) {
                handleBaseState(false);
                return;
            }

            int currentChests = 0;
            int currentSigns = 0;
            int currentDonkeys = 0;
            double range = checkRange.getValue();
            double rangeSq = range * range;

            if (donkey.getValue()) {
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof DonkeyEntity && mc.player.squaredDistanceTo(entity) <= rangeSq) {
                        currentDonkeys++;
                    }
                }
            }

            if (chest.getValue() || sign.getValue()) {
                BlockPos playerPos = mc.player.getBlockPos();
                int r = (int) range;
                ChunkPos minChunk = new ChunkPos(playerPos.add(-r, 0, -r));
                ChunkPos maxChunk = new ChunkPos(playerPos.add(r, 0, r));

                for (int cx = minChunk.x; cx <= maxChunk.x; cx++) {
                    for (int cz = minChunk.z; cz <= maxChunk.z; cz++) {
                        if (mc.world.getChunkManager().isChunkLoaded(cx, cz)) {
                            WorldChunk chunk = mc.world.getChunk(cx, cz);

                            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                                if (blockEntity.getPos().getSquaredDistance(playerPos) <= rangeSq) {
                                    if (chest.getValue() && blockEntity instanceof ChestBlockEntity) {
                                        currentChests++;
                                    } else if (sign.getValue() && blockEntity instanceof SignBlockEntity) {
                                        currentSigns++;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            isBase = true;

            if (chest.getValue() && currentChests < chestCount.getValueInt()) {
                isBase = false;
            }
            if (sign.getValue() && currentSigns < signCount.getValueInt()) {
                isBase = false;
            }
            if (donkey.getValue() && currentDonkeys < donkeyCount.getValueInt()) {
                isBase = false;
            }
        }

        handleBaseState(isBase);
    }

    private void handleBaseState(boolean isBase) {
        if (isBase) {
            if (!isCurrentlyInBase) {
                CommandManager.sendMessage("§7[AntiSB] §a免战保护已开启。");
                isCurrentlyInBase = true;
                addNearbyPlayersToTempFriends();
            }
        } else {
            if (isCurrentlyInBase) {
                CommandManager.sendMessage("§7[AntiSB] §c免战保护已关闭。");
                isCurrentlyInBase = false;
                clearTempFriends();
            }
        }
    }

    private void addNearbyPlayersToTempFriends() {
        double rangeSq = checkRange.getValue() * checkRange.getValue();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            if (mc.player.squaredDistanceTo(player) <= rangeSq) {
                String playerName = player.getGameProfile().getName();
                if (!Lumi.FRIEND.isFriend(playerName) && !tempFriends.contains(playerName)) {
                    Lumi.FRIEND.add(playerName);
                    tempFriends.add(playerName);
                    CommandManager.sendMessage("§7[AntiSB] §a添加临时好友: §f" + playerName);
                }
            }
        }
    }

    private void clearTempFriends() {
        if (tempFriends.isEmpty()) return;

        for (String playerName : tempFriends) {
            Lumi.FRIEND.remove(playerName);
            CommandManager.sendMessage("§7[AntiSB] §c移除临时好友: §f" + playerName);
        }
        tempFriends.clear();
    }
}