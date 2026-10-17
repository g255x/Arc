package dev.G255.Lumi.mod.modules.impl.misc;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.DeathEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoExplain
extends Module {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public final EnumSetting<Type> type = this.add(new EnumSetting("Type", (Enum)Type.Reason));
    private final SliderSetting range = this.add(new SliderSetting("Range", 20.0, 5.0, 50.0, 1.0));
    private final SliderSetting randoms = this.add(new SliderSetting("Random", 4.0, 0.0, 15.0, 1.0));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 6.0, 2.0, 30.0, 0.5).setSuffix("s"));
    private final List<String> reason = List.of("\u9f20\u6807\u521a\u624d\u77ed\u4e86\u62bd\u7b4b\u4e86", "\u7f51\u5361\u4e860.5\u79d2\u5144\u5f1f", "\u8fd9\u628a\u7eaf\u5c5e\u624b\u6ed1\u4e86", "\u7535\u8111\u7a81\u7136\u5361\u6b7b\u6211\u64cd", "\u952e\u76d8\u574f\u4e86\u6309\u4e0d\u51fa\u6765", "\u8fd9\u5ef6\u8fdf\u4e5f\u592a\u79bb\u8c31\u4e86\u5427", "\u521a\u624d\u9f20\u6807\u6ca1\u7535\u4e86", "\u8fd9\u7834\u7f51\u5427\u7535\u8111\u62bd\u98ce", "\u7eaf\u5c5e\u5931\u8bef\u5144\u5f1f\u4eec", "\u521a\u624d\u624b\u6296\u4e86\u4e00\u4e0b", "\u6211\u559d\u6c34\u53bb\u4e86\u6ca1\u6ce8\u610f", "\u961f\u53cb\u5751\u6211\u6709\u4ec0\u4e48\u529e\u6cd5", "\u8fd9\u914d\u7f6e\u592a\u70c2\u4e86\u5361\u6b7b\u6211");
    private final List<String> catgirl = List.of("\u4e3b\u4eba@%s\u597d\u68d2\uff5e\u2661 \u4eba\u5bb6\u88ab\u4f60\u8349\u9972\u5230\u55b7\u6c34\u5931\u7981\u4e86\u2026\u2026\u4e0b\u9762\u8fd8\u5728\u75af\u72c2\u6d41\u6c34\u5462\uff5e", "\u545c\u55b5\uff5e\u2661 \u4e3b\u4eba@%s\u628a\u4eba\u5bb6\u7684\u9a9a\u7a74\u64cd\u70c2\u6389\u4e86\u2026\u2026\u5b50\u5bab\u90fd\u88ab\u649e\u5f97\u53d8\u5f62\u55b7\u6c34\u4e86\uff5e", "\u554a\uff5e\u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u88ab\u4f60\u64cd\u5230\u9ad8\u6f6e\u5931\u7981\u55b7\u4e86\u4e00\u5730\u2026\u2026\u597d\u6deb\u8361\u54e6\uff5e", "\u4e3b\u4eba@%s\u2026\u2026\u732b\u732b\u4e0b\u9762\u5df2\u7ecf\u9ecf\u7cca\u7cca\u62c9\u4e1d\u4e86\u2026\u2026\u88ab\u4f60\u8349\u9972\u5230\u6f6e\u5439\u6b7b\u4ea1\uff5e", "\u55b5\u545c\u2661\uff5e\u4e3b\u4eba@%s\u628a\u5974\u5bb6\u64cd\u5230\u5b50\u5bab\u9ad8\u6f6e\u2026\u2026\u6b7b\u524d\u5931\u7981\u55b7\u4e86\u597d\u591a\u6b21\uff5e", "\u4e3b\u4eba@%s\u597d\u7c97\u597d\u731b\u2026\u2026\u4eba\u5bb6\u4e0b\u9762\u88ab\u64cd\u5230\u53c8\u7ea2\u53c8\u80bf\u4e00\u76f4\u5728\u6f6e\u5439\uff5e", "\u545c\uff5e\u2661 \u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u53ea\u662f\u4f60\u7684\u53d1\u60c5\u8089\u4fbf\u5668\u2026\u2026\u88ab\u8349\u5230\u5931\u7981\u53bb\u4e16\u4e86\uff5e", "\u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u4e0b\u9762\u5df2\u7ecf\u5f7b\u5e95\u574f\u6389\u4e86\u2026\u2026\u9ecf\u7cca\u7cca\u6deb\u6c34\u55b7\u5f97\u5230\u5904\u90fd\u662f\uff5e", "\u554a\u554a\u554a\uff5e\u4e3b\u4eba@%s\u628a\u4eba\u5bb6\u64cd\u5230\u5931\u7981\u6f6e\u5439\u2026\u2026\u5c3f\u548c\u6deb\u6c34\u90fd\u6df7\u5728\u4e00\u8d77\u4e86\uff5e", "\u4e3b\u4eba@%s\uff5e\u732b\u732b\u7684\u5b50\u5bab\u88ab\u4f60\u649e\u5f97\u53c8\u9ebb\u53c8\u723d\u2026\u2026\u6b7b\u7684\u65f6\u5019\u8fd8\u5728\u55b7\u5462\u2661", "\u4eba\u5bb6\u88ab\u4e3b\u4eba@%s\u64cd\u5230\u5927\u8111\u4e00\u7247\u7a7a\u767d\u2026\u2026\u53ea\u4f1a\u55b7\u6c34\u548c\u53eb\u4e3b\u4eba\u4e86\uff5e", "\u545c\u55b5\uff5e\u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u4e0b\u9762\u9ecf\u5230\u8d70\u4e0d\u52a8\u8def\u4e86\u2026\u2026\u88ab\u4f60\u8349\u5230\u9ad8\u6f6e\u6b7b\u4ea1\uff5e", "\u4e3b\u4eba@%s\u597d\u5389\u5bb3\u2026\u2026\u628a\u4eba\u5bb6\u64cd\u5230\u5931\u7981\u55b7\u6c34\u5230\u6b7b\u2026\u2026\u732b\u732b\u5df2\u7ecf\u574f\u6389\u4e86\uff5e", "\u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u53d1\u60c5\u5230\u5931\u7981\u4e86\u2026\u2026\u4e0b\u9762\u55b7\u51fa\u6765\u7684\u6deb\u6c34\u628a\u5730\u90fd\u5f04\u6e7f\u4e86\u5462\uff5e", "\u55b5\u54c8\u2661\uff5e\u4e3b\u4eba@%s\u628a\u5974\u5bb6\u7684\u9a9a\u7a74\u64cd\u5f97\u7ffb\u51fa\u6765\u2026\u2026\u6f6e\u5439\u5230\u6b7b\u90fd\u505c\u4e0d\u4e0b\u6765\uff5e", "\u4eba\u5bb6\u5728\u4e3b\u4eba@%s\u9762\u524d\u5b8c\u5168\u5c31\u662f\u4e2a\u53ea\u4f1a\u55b7\u6c34\u7684\u8089\u73a9\u5177\uff5e", "\u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u5b50\u5bab\u90fd\u88ab\u4f60\u9876\u70c2\u4e86\u2026\u2026\u6b7b\u524d\u8fd8\u5931\u7981\u6f6e\u5439\u4e86\u597d\u51e0\u6b21\u5462\uff5e", "\u545c\uff5e\u2661 \u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u4e0b\u9762\u53c8\u9ecf\u53c8\u75d2\u2026\u2026\u88ab\u4f60\u8349\u5230\u55b7\u6c34\u6b7b\u4ea1\u4e86\uff5e", "\u4e3b\u4eba@%s\u2026\u2026\u732b\u732b\u7684\u9a9a\u7a74\u5df2\u7ecf\u88ab\u4f60\u5f00\u53d1\u6210\u55b7\u6c34\u6bcd\u732b\u4e86\uff5e", "\u554a\u554a\uff5e\u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u88ab\u4f60\u8349\u9972\u5230\u5b50\u5bab\u75c9\u631b\u2026\u2026\u55b7\u6c34\u5230\u6b7b\uff5e", "\u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u4e0b\u9762\u5df2\u7ecf\u9ecf\u7cca\u7cca\u4e00\u7247\u4e86\u2026\u2026\u5168\u662f\u732e\u7ed9\u4f60\u7684\u6deb\u6c34\uff5e", "\u545c\u55b5\u2661\uff5e\u4e3b\u4eba@%s\u628a\u4eba\u5bb6\u73a9\u574f\u4e86\u2026\u2026\u6b7b\u7684\u65f6\u5019\u8fd8\u5728\u6f6e\u5439\u55b7\u5c3f\uff5e", "\u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u9ad8\u6f6e\u7684\u65f6\u5019\u5931\u7981\u55b7\u4e86\u597d\u8fdc\u2026\u2026\u597d\u4e0b\u8d31\u5bf9\u4e0d\u8d77\uff5e", "\u732b\u732b\u7684\u9a9a\u7a74\u5df2\u7ecf\u88ab\u4e3b\u4eba@%s\u64cd\u5f97\u4e0d\u6210\u6837\u5b50\u2026\u2026\u8fd8\u5728\u4e0d\u505c\u5730\u55b7\u6c34\u5462\uff5e", "\u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u88ab\u4f60\u64cd\u5230\u817f\u8f6f\u5931\u7981\u2026\u2026\u4e0b\u9762\u73b0\u5728\u8fd8\u5728\u62bd\u6410\u55b7\u6c34\uff5e", "\u545c\uff5e\u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u53ea\u60f3\u88ab\u4f60\u4e00\u76f4\u8349\u2026\u2026\u88ab\u64cd\u5230\u55b7\u6c34\u6b7b\u4ea1\u4e5f\u662f\u5e78\u798f\u7684\uff5e", "\u4e3b\u4eba@%s\u597d\u68d2\uff5e\u2661 \u4eba\u5bb6\u4e0b\u9762\u88ab\u4f60\u64cd\u5230\u9ad8\u6f6e\u5931\u7981\u2026\u2026\u6deb\u6c34\u55b7\u4e86\u4e00\u5730\u5462\uff5e", "\u554a\u54c8\uff5e\u4e3b\u4eba@%s\u2026\u2026\u4eba\u5bb6\u88ab\u4f60\u8349\u5230\u5b50\u5bab\u704c\u6ee1\u2026\u2026\u6b7b\u7684\u65f6\u5019\u8fd8\u5728\u6f6e\u5439\uff5e", "\u4e3b\u4eba@%s\u2026\u2026\u732b\u732b\u5df2\u7ecf\u5f7b\u5e95\u96cc\u5815\u4e86\u2026\u2026\u88ab\u4f60\u64cd\u5230\u5931\u7981\u55b7\u6c34\u5230\u6b7b\uff5e");
    private final Random random = new Random();
    private long lastDeathTime = 0L;
    private String pendingMessage = null;
    private boolean waitingToSend = false;

    public AutoExplain() {
        super("AutoExplain", "Explains cause of death in chat when you die.", Module.Category.Misc);
        this.setChinese("\u6b7b\u4ea1\u89e3\u91ca");
        this.setChineseDesc("\u6b7b\u4ea1\u65f6\u5728\u804a\u5929\u4e2d\u89e3\u91ca\u6b7b\u4ea1\u539f\u56e0");
    }

    @EventListener
    public void onDeath(DeathEvent event) {
        String finalMessage;
        if (event.getPlayer() != AutoExplain.mc.player) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((double)(now - this.lastDeathTime) < this.delay.getValue() * 1000.0) {
            return;
        }
        this.lastDeathTime = now;
        PlayerEntity nearest = this.getNearestPlayer();
        if (nearest == null) {
            return;
        }
        String killerName = nearest.getName().getString();
        Object randomString = this.generateRandomString(this.randoms.getValueInt());
        if (!((String)randomString).isEmpty()) {
            randomString = " " + (String)randomString;
        }
        if (this.type.getValue() == Type.CatGirl) {
            finalMessage = String.format(this.catgirl.get(this.random.nextInt(this.catgirl.size())), killerName) + (String)randomString;
        } else {
            String base = this.reason.get(this.random.nextInt(this.reason.size()));
            finalMessage = "@" + killerName + " " + base + (String)randomString;
        }
        this.pendingMessage = finalMessage;
        this.waitingToSend = true;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.waitingToSend || this.pendingMessage == null) {
            return;
        }
        if (mc.getNetworkHandler() == null || AutoExplain.mc.player == null) {
            return;
        }
        if (AutoExplain.mc.currentScreen instanceof DeathScreen) {
            return;
        }
        String msg = this.pendingMessage;
        this.pendingMessage = null;
        this.waitingToSend = false;
        try {
            mc.getNetworkHandler().sendChatMessage(msg);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private PlayerEntity getNearestPlayer() {
        if (AutoExplain.mc.player == null || AutoExplain.mc.world == null) {
            return null;
        }
        double r = this.range.getValue();
        Box box = new Box(AutoExplain.mc.player.getPos().add(-r, -r, -r), AutoExplain.mc.player.getPos().add(r, r, r));
        return AutoExplain.mc.world.getEntitiesByClass(PlayerEntity.class, box, p -> p != AutoExplain.mc.player && !Lumi.FRIEND.isFriend(p)).stream().min(Comparator.comparingDouble(p -> p.distanceTo((Entity)AutoExplain.mc.player))).orElse(null);
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            sb.append(CHARACTERS.charAt(this.random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    public void onDisable() {
        this.pendingMessage = null;
        this.waitingToSend = false;
    }

    public static enum Type {
        Reason,
        CatGirl;
    }
}