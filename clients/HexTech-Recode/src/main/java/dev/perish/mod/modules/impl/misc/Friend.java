package dev.perish.mod.modules.impl.misc;

import dev.perish.Perish;
import dev.perish.mod.modules.Module;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

public class Friend extends Module {
    public static Friend INSTANCE;

    public Friend() {
        super("Friend", Module.Category.Misc);
        this.setChinese("好友");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            this.disable();
        } else {
            if (mc.crosshairTarget instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity player) {
                Perish.FRIEND.friend(player);
            }

            this.disable();
        }
    }
}
