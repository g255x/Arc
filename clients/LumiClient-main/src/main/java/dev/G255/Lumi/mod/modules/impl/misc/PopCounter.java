package dev.G255.Lumi.mod.modules.impl.misc;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.TotemEvent;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import dev.G255.Lumi.mod.modules.settings.impl.StringSetting;
import java.util.List;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class PopCounter extends Module {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public final List<String> sex = List.of(
            "\u5450\u5450~\u6742\u9c7c\u54e5\u54e5\u4e0d\u4f1a\u8fd9\u6837\u5c31\u88ab\u6349\u5f04\u7684\u4e0d\u4f1a\u8bf4\u8bdd\u4e86\u5427\u2661",
            "\u563b\u563b~\u6742\u9c7c\u54e5\u54e5\u4e0d\u4f1a\u4ee5\u4e3a\u7ad6\u4e2a\u5927\u62c7\u54e5\u5c31\u80fd\u6b3a\u8d1f\u6211\u4e86\u5427~\u4e0d\u4f1a\u5427\u2661\u4e0d\u4f1a\u5427\u2661",
            "\u6742\u9c7c\u54e5\u54e5\u600e\u4e48\u53ef\u80fd\u6b3a\u8d1f\u5f97\u4e86\u522b\u4eba\u5462~\u53ea\u80fd\u6b3a\u8d1f\u81ea\u5df1\u54e6\u2661~",
            "\u54e5\u54e5\u771f\u662f\u597d\u6b3a\u8d1f\u554a\u2661\u563b\u563b~",
            "\u54ce\u2661~\u6742\u9c7c\u8bf4\u8bdd\u5c31\u662f\u65e0\u8da3\u5509~",
            "\u5450\u5450~\u6742\u9c7c\u54e5\u54e5\u53d1\u8fd9\u4e2a\u662f\u60f3\u6559\u80b2\u6211\u5417~\u563b\u563b~\u600e\u4e48\u53ef\u80fd\u554a\u2661",
            "\u4ec0\u4e48\u561b~\u5e9f\u67f4\u54e5\u54e5\u4f1a\u60f3\u8fd9\u79cd\u4e8b\u60c5\u554a~\u5514\u5443",
            "\u628a\u4f60\u80ae\u810f\u7684\u76ee\u5149\u62ff\u5f00\u5566~\u5f88\u6076\u5fc3\u54e6\u2661",
            "\u54b1\u7684\u671f\u5f85\u5c31\u662f\u88ab\u4f60\u8fd9\u6837\u7684\u7b28\u86cb\u7834\u574f\u4e86~\u2661"
    );
    public final List<String> popList = List.of(
            "\u7678\u5566! %player% \u7206\u4e86 %pop% \u4e2a\u56fe\u817e!",
            "pop%pop% \u4e86\u54e6 %player%",
            "%player% popped %pop% totems!",
            "\u53c8\u7206\u4e86\u4e00\u4e2a\uff0c%player% \u5df2\u7ecf %pop% \u4e2a\u56fe\u817e\u4e86",
            "%player% \u7684\u7b2c %pop% \u4e2a\u56fe\u817e\u7206\u4e86",
            "\u56fe\u817e\u7206\u4e86 %pop% \u4e2a\uff0c%player% \u4f60\u8fd8\u884c\u5417",
            "%pop% pops for %player%!",
            "\u7206\u56fe\u817e\u5566~ %player% \u5df2\u7ecf\u7206\u4e86 %pop% \u4e2a\u4e86",
            "\u54c7 %player% \u7684\u56fe\u817e\u7206\u4e86 %pop% \u6b21\u4e86"
    );
    private final EnumSetting<Type> type = this.add(new EnumSetting<>("Type", Type.PopList));
    private final StringSetting customMessage = this.add(new StringSetting("CustomMessage", "%player% popped %pop% totems!", () -> this.type.getValue() == Type.Custom));
    private final SliderSetting range = this.add(new SliderSetting("Range", 10.0, 0.0, 20.0, 0.1));
    private final SliderSetting randoms = this.add(new SliderSetting("Random", 3.0, 0.0, 20.0, 1.0));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 0.0, 0.0, 5.0, 0.1).setSuffix("s"));
    private final BooleanSetting ignoreSelf = this.add(new BooleanSetting("IgnoreSelf", true));
    private final BooleanSetting ignoreFriends = this.add(new BooleanSetting("IgnoreFriends", true));
    private final Random random = new Random();
    private long lastMessageTime = 0;

    public PopCounter() {
        super("PopCounter", Module.Category.Misc);
        this.setChinese("\u7206\u56fe\u817e\u5632\u8bbd");
    }

    @EventListener
    public void onTotemPop(TotemEvent event) {
        if (nullCheck()) return;
        PlayerEntity player = event.getPlayer();

        if (this.ignoreSelf.getValue() && player == mc.player) return;
        if (this.ignoreFriends.getValue() && Lumi.FRIEND.isFriend(player)) return;
        if (this.range.getValue() > 0.0 && mc.player.distanceTo(player) > this.range.getValue()) return;

        long now = System.currentTimeMillis();
        if (now - this.lastMessageTime < this.delay.getValue() * 1000) return;
        this.lastMessageTime = now;

        int popCount = Lumi.POP.getPop(player);
        String randomString = this.generateRandomString(this.randoms.getValueInt());
        if (!randomString.isEmpty()) {
            randomString = " " + randomString;
        }

        String message;
        switch (this.type.getValue()) {
            case PopList:
                message = this.popList.get(this.random.nextInt(this.popList.size()));
                break;
            case AutoSex:
                message = this.sex.get(this.random.nextInt(this.sex.size()));
                break;
            default:
                message = this.customMessage.getValue();
                break;
        }

        message = message.replace("%player%", player.getName().getString());
        message = message.replace("%pop%", String.valueOf(popCount));
        message = message + randomString;

        mc.getNetworkHandler().sendChatMessage(message);
        Lumi.POP.lastPopPlayer = player;
        Lumi.POP.lastPopCount = popCount;
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = this.random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public enum Type {
        PopList,
        Custom,
        AutoSex
    }
}