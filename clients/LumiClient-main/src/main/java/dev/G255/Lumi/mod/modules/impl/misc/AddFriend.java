/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.util.hit.EntityHitResult
 *  net.minecraft.util.hit.HitResult
 */
package dev.G255.Lumi.mod.modules.impl.misc;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.mod.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class AddFriend
extends Module {
    public static AddFriend INSTANCE;

    public AddFriend() {
        super("AddFriend", Module.Category.Misc);
        this.setChinese("");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        EntityHitResult entityHitResult;
        Entity entity;
        if (AddFriend.nullCheck()) {
            this.disable();
            return;
        }
        HitResult target = AddFriend.mc.crosshairTarget;
        if (target instanceof EntityHitResult && (entity = (entityHitResult = (EntityHitResult)target).getEntity()) instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            Lumi.FRIEND.friend(player);
        }
        this.disable();
    }
}

