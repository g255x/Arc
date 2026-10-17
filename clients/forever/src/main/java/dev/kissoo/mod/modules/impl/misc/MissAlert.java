package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.modules.Module;
import java.util.Random;
import net.minecraft.item.Items;

public class MissAlert extends Module {
    public static MissAlert INSTANCE;
    private final Random random = new Random();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private int cachedTotems;
    private boolean wasDead;
    private String pendingMessage;
    private boolean waitingToSend;

    public MissAlert() {
        super("MissAlert", "Alerts when you or enemies miss totem pops", Module.Category.Misc);
        this.setChinese("失误提醒");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;

        // Track totem count
        int currentTotems = InventoryUtil.findItem(Items.TOTEM_OF_UNDYING);
        if (currentTotems == -1) currentTotems = 0;
        else {
            currentTotems = 0;
            for (int i = 0; i < mc.player.getInventory().size(); i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                    currentTotems += mc.player.getInventory().getStack(i).getCount();
                }
            }
        }

        if (wasDead && mc.player.isAlive() && !mc.player.isDead()) {
            wasDead = false;
            if (pendingMessage != null && waitingToSend) {
                CommandManager.sendChatMessageWidthIdNoSync(pendingMessage, hashCode());
                waitingToSend = false;
                pendingMessage = null;
            }
        }

        if (cachedTotems > 0 && currentTotems < cachedTotems && mc.player.isAlive()) {
            pendingMessage = "Missed totem! " + randomString(4);
            waitingToSend = true;
        }

        if (mc.player.isDead() || mc.player.getHealth() <= 0) {
            wasDead = true;
        }

        cachedTotems = currentTotems;
    }

    String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
