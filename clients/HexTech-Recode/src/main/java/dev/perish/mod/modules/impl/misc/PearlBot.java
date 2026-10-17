package dev.perish.mod.modules.impl.misc;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.api.events.impl.ReceiveMessageEvent;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.settings.impl.StringSetting;
import java.util.UUID;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.HitResult;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class PearlBot extends Module {
    public static PearlBot INSTANCE;
    private final StringSetting message = this.add(new StringSetting("Message", "escape"));
    private final StringSetting whitelist = this.add(new StringSetting("Whitelist", "Player1,Player2"));

    public PearlBot() {
        super("PearlBot", Category.Misc);
        this.setChinese("珍珠机器人");
        INSTANCE = this;
    }

    @EventListener
    public void onReceiveMessage(ReceiveMessageEvent event) {
        if (nullCheck()) return;

        String text = event.message.getString();

        // 解析IRC消息格式: [IRC] [Admin] Player: message 或 [IRC] Player: message
        // 简单匹配：查找 [IRC] 并且包含 :
        if (text.contains("[IRC]") && text.contains(":")) {
            String cleanText = text.replaceAll("§[0-9a-fk-or]", ""); // 去除颜色代码
            // 尝试提取 Player 和 Message
            // 假设格式为 ... Player: message
            int colonIndex = cleanText.indexOf(":");
            if (colonIndex > 0) {
                String content = cleanText.substring(colonIndex + 1).trim();
                String preColon = cleanText.substring(0, colonIndex).trim();

                // 从冒号前面提取玩家名，通常是最后一个空格后的部分，或者是 ] 后的部分
                // 例如 "[IRC] [Admin] Player" -> "Player"
                String sender = preColon;
                int lastSpace = preColon.lastIndexOf(" ");
                if (lastSpace != -1) {
                    sender = preColon.substring(lastSpace + 1);
                }
                // 处理可能存在的 [Admin] 前缀紧挨着名字的情况
                int lastBracket = sender.lastIndexOf("]");
                if (lastBracket != -1) {
                    sender = sender.substring(lastBracket + 1);
                }

                handleMessage(sender, content);
            }
        }
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;

        String sender = null;
        String content = null;

        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String fullText = packet.content().getString();
            handleMessage(fullText, fullText);
            return;
        } else if (event.getPacket() instanceof ChatMessageS2CPacket packet) {
            content = packet.body().content();
            UUID senderUUID = packet.sender();
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(senderUUID);
                if (entry != null) {
                    sender = entry.getProfile().getName();
                }
            }
        }

        if (content != null) {
            handleMessage(sender, content);
        }
    }

    private void handleMessage(String sender, String content) {
        String triggerMsg = message.getValue();
        if (content == null || !content.contains(triggerMsg)) return;

        String[] allowedUsers = whitelist.getValue().split(",");
        boolean isAllowed = false;

        for (String user : allowedUsers) {
            String trimmedUser = user.trim();
            if (trimmedUser.isEmpty()) continue;

            if (sender != null) {
                if (sender.equalsIgnoreCase(trimmedUser) || sender.contains(trimmedUser)) {
                    isAllowed = true;
                    break;
                }
            }
        }

        if (isAllowed) {
            triggerTrapdoor();
        }
    }

    private void triggerTrapdoor() {
        HitResult hit = mc.player.raycast(5.0, mc.getRenderTickCounter().getTickDelta(true), false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            if (mc.world.getBlockState(pos).getBlock() instanceof TrapdoorBlock) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}
