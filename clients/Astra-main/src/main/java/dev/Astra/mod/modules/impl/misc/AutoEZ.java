/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.PlayerEntity
 */
package dev.Astra.mod.modules.impl.misc;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.DeathEvent;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import dev.Astra.mod.modules.settings.impl.StringSetting;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.Random;

public class AutoEZ
        extends Module {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public final List<String> bot = List.of("鼠标明天到，触摸板打的", "转人工", "收徒", "不收徒", "有真人吗", "墨镜上车", "素材局", "不接单", "接单", "征婚", "4399?", "暂时不考虑打职业", "bot?", "叫你家长大人来打", "假肢上门安装", "浪费我的网费", "不收残疾人", "下课", "自己找差距", "不接代", "代+", "这样的治好了也流口水", "人机", "人机怎么调难度啊", "只收不被0封的", "Bot吗这是", "领养", "纳亲", "正视差距", "近亲繁殖?", "我玩的是新手教程?", "来调灵敏度的", "来调参数的", "小号", "不是本人别加", "下次记得晚点玩", "随便玩玩,不带妹", "打1上车");

    public final List<String> legit = List.of("good game", "good fight", "well played", "good luck", "have fun", "nice shot", "nice try", "respect", "you fought well", "thanks for the fight", "that was fun", "well fought", "nice fight", "good fight mate", "honorable fight", "fair fight", "respectable", "you are skilled", "nice moves", "impressive", "well done", "great fight", "awesome fight", "enjoyed that", "until next time", "you played well", "good duel", "nice duel", "respected opponent", "worthy opponent");

    private final EnumSetting<Type> type = this.add(new EnumSetting<>("Type", Type.Bot));
    final StringSetting message = this.add(new StringSetting("Message", "%player% 加QQ群1092906007获取强大的Astra客户端", () -> this.type.getValue() == Type.Custom));
    final Random random = new Random();
    private final SliderSetting range = this.add(new SliderSetting("Range", 10.0, 0.0, 20.0, 0.1));
    private final SliderSetting randoms = this.add(new SliderSetting("Random", 3.0, 0.0, 20.0, 1.0));

    public AutoEZ() {
        super("AutoEZ", Module.Category.Misc);
        this.setChinese("自动嘲讽");
    }

    @EventListener
    public void onDeath(DeathEvent event) {
        // 关键空值保护
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        PlayerEntity player = event.getPlayer();
        if (player == null) return;

        // 不嘲讽自己或好友
        if (player == mc.player || Astra.FRIEND.isFriend(player)) return;

        // 范围限制
        if (range.getValue() > 0.0 && mc.player.distanceTo(player) > range.getValue()) return;

        // 生成随机后缀（仅 Bot / Custom 模式）
        String randomSuffix = "";
        if (type.getValue() != Type.Legit) {
            int len = randoms.getValueInt();
            if (len > 0) {
                randomSuffix = " " + generateRandomString(len);
            }
        }

        // 发送消息
        switch (type.getValue()) {
            case Bot:
                String botMsg = bot.get(random.nextInt(bot.size())) + " " + player.getName().getString() + randomSuffix;
                mc.getNetworkHandler().sendChatMessage(botMsg);
                break;
            case Custom:
                String customMsg = message.getValue().replace("%player%", player.getName().getString()) + randomSuffix;
                mc.getNetworkHandler().sendChatMessage(customMsg);
                break;
            case Legit:
                if (!legit.isEmpty()) {
                    mc.getNetworkHandler().sendChatMessage(legit.get(random.nextInt(legit.size())));
                }
                break;
        }
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public static enum Type {
        Bot,
        Custom,
        Legit;
    }
}