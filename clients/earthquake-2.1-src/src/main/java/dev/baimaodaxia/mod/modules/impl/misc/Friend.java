package dev.baimaodaxia.mod.modules.impl.misc;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.mod.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.EntityHitResult;

public class Friend extends Module {
   public static Friend INSTANCE;

   public Friend() {
      super("Friend", Module.Category.Misc);
      this.setChinese("好友");
      INSTANCE = this;
   }

   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         HitResult target = mc.crosshairTarget;
         EntityHitResult entityHitResult;
         Entity entity;
         if (target instanceof EntityHitResult && (entity = (entityHitResult = (EntityHitResult)target).getEntity()) instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            Alien.FRIEND.friend(player);
         }

         this.disable();
      }
   }
}
