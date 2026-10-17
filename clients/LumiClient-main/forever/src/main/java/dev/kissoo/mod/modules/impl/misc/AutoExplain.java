package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.DeathEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class AutoExplain extends Module {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public final EnumSetting<Type> type = this.add(new EnumSetting<>("Type", Type.Reason));

    private final SliderSetting range = this.add(new SliderSetting("Range", 20.0, 5.0, 50.0, 1.0));
    private final SliderSetting randoms = this.add(new SliderSetting("Random", 4.0, 0.0, 15.0, 1.0));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 6.0, 2.0, 30.0, 0.5).setSuffix("s"));

    private final List<String> reason = List.of(
            "鼠标刚才短了抽筋了",
            "网卡了0.5秒兄弟",
            "这把纯属手滑了",
            "电脑突然卡死我操",
            "键盘坏了按不出来",
            "这延迟也太离谱了吧",
            "刚才鼠标没电了",
            "这破网吧电脑抽风",
            "纯属失误兄弟们",
            "刚才手抖了一下",
            "我喝水去了没注意",
            "队友坑我有什么办法",
            "这配置太烂了卡死我"
    );

    private final List<String> catgirl = List.of(
            "主人@%s好棒～♡ 人家被你草饲到喷水失禁了……下面还在疯狂流水呢～",
            "呜喵～♡ 主人@%s把人家的骚穴操烂掉了……子宫都被撞得变形喷水了～",
            "啊～主人@%s……人家被你操到高潮失禁喷了一地……好淫荡哦～",
            "主人@%s……猫猫下面已经黏糊糊拉丝了……被你草饲到潮吹死亡～",
            "喵呜♡～主人@%s把奴家操到子宫高潮……死前失禁喷了好多次～",
            "主人@%s好粗好猛……人家下面被操到又红又肿一直在潮吹～",
            "呜～♡ 主人@%s……人家只是你的发情肉便器……被草到失禁去世了～",
            "主人@%s……人家下面已经彻底坏掉了……黏糊糊淫水喷得到处都是～",
            "啊啊啊～主人@%s把人家操到失禁潮吹……尿和淫水都混在一起了～",
            "主人@%s～猫猫的子宫被你撞得又麻又爽……死的时候还在喷呢♡",
            "人家被主人@%s操到大脑一片空白……只会喷水和叫主人了～",
            "呜喵～主人@%s……人家下面黏到走不动路了……被你草到高潮死亡～",
            "主人@%s好厉害……把人家操到失禁喷水到死……猫猫已经坏掉了～",
            "主人@%s……人家发情到失禁了……下面喷出来的淫水把地都弄湿了呢～",
            "喵哈♡～主人@%s把奴家的骚穴操得翻出来……潮吹到死都停不下来～",
            "人家在主人@%s面前完全就是个只会喷水的肉玩具～",
            "主人@%s……人家子宫都被你顶烂了……死前还失禁潮吹了好几次呢～",
            "呜～♡ 主人@%s……人家下面又黏又痒……被你草到喷水死亡了～",
            "主人@%s……猫猫的骚穴已经被你开发成喷水母猫了～",
            "啊啊～主人@%s……人家被你草饲到子宫痉挛……喷水到死～",
            "主人@%s……人家下面已经黏糊糊一片了……全是献给你的淫水～",
            "呜喵♡～主人@%s把人家玩坏了……死的时候还在潮吹喷尿～",
            "主人@%s……人家高潮的时候失禁喷了好远……好下贱对不起～",
            "猫猫的骚穴已经被主人@%s操得不成样子……还在不停地喷水呢～",
            "主人@%s……人家被你操到腿软失禁……下面现在还在抽搐喷水～",
            "呜～主人@%s……人家只想被你一直草……被操到喷水死亡也是幸福的～",
            "主人@%s好棒～♡ 人家下面被你操到高潮失禁……淫水喷了一地呢～",
            "啊哈～主人@%s……人家被你草到子宫灌满……死的时候还在潮吹～",
            "主人@%s……猫猫已经彻底雌堕了……被你操到失禁喷水到死～"
    );

    private final Random random = new Random();
    private long lastDeathTime = 0;
    private String pendingMessage = null;
    private boolean waitingToSend = false;

    public AutoExplain() {
        super("AutoExplain", Module.Category.Misc);
        this.setChinese("死亡解释");
    }

    @EventListener
    public void onDeath(DeathEvent event) {
        if (event.getPlayer() != mc.player) return;

        long now = System.currentTimeMillis();
        if (now - lastDeathTime < delay.getValue() * 1000L) return;
        lastDeathTime = now;

        PlayerEntity nearest = getNearestPlayer();
        if (nearest == null) return;

        String killerName = nearest.getName().getString();
        String randomString = generateRandomString(randoms.getValueInt());
        if (!randomString.isEmpty()) randomString = " " + randomString;

        String finalMessage;
        if (type.getValue() == Type.CatGirl) {
            finalMessage = String.format(
                    catgirl.get(random.nextInt(catgirl.size())), killerName
            ) + randomString;
        } else {
            String base = reason.get(random.nextInt(reason.size()));
            finalMessage = "@" + killerName + " " + base + randomString;
        }
        pendingMessage = finalMessage;
        waitingToSend = true;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!waitingToSend || pendingMessage == null) return;
        if (mc.getNetworkHandler() == null || mc.player == null) return;
        if (mc.currentScreen instanceof DeathScreen) {
            return;
        }

        String msg = pendingMessage;
        pendingMessage = null;
        waitingToSend = false;

        try {
            mc.getNetworkHandler().sendChatMessage(msg);
        } catch (Exception e) {
        }
    }

    private PlayerEntity getNearestPlayer() {
        if (mc.player == null || mc.world == null) return null;

        double r = range.getValue();
        Box box = new Box(
                mc.player.getPos().add(-r, -r, -r),
                mc.player.getPos().add(r, r, r)
        );

        return mc.world.getEntitiesByClass(PlayerEntity.class, box, p ->
                        p != mc.player && !Forever.FRIEND.isFriend(p))
                .stream()
                .min(Comparator.comparingDouble(p -> p.distanceTo(mc.player)))
                .orElse(null);
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    @Override
    public void onDisable() {
        pendingMessage = null;
        waitingToSend = false;
    }

    public enum Type {
        Reason,
        CatGirl
    }
}