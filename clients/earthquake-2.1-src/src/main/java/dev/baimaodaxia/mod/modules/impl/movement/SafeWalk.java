package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.KeyboardInputEvent;
import dev.baimaodaxia.api.events.impl.MoveEvent;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.util.math.Box;

public class SafeWalk extends Module {
   public static SafeWalk INSTANCE;
   private final BooleanSetting legit = this.add(new BooleanSetting("Legit", true));
   private final BooleanSetting onlyInsideBlock = this.add(new BooleanSetting("OnlyInsideBlock", false));

   public SafeWalk() {
      super("SafeWalk", Module.Category.Movement);
      this.setChinese("边缘行走");
      INSTANCE = this;
   }

   @EventListener(
      priority = -200
   )
   public void keyboard(KeyboardInputEvent event) {
      if (mc.player.isOnGround() && this.legit.getValue() && this.shouldSafeWalk()) {
         if (this.isOffsetBBEmpty(0.3, (double)-1.0F, (double)0.0F)) {
            mc.player.input.sneaking = true;
         } else if (this.isOffsetBBEmpty((double)0.0F, (double)-1.0F, 0.3)) {
            mc.player.input.sneaking = true;
         } else if (this.isOffsetBBEmpty(0.3, (double)-1.0F, 0.3)) {
            mc.player.input.sneaking = true;
         } else if (this.isOffsetBBEmpty(-0.3, (double)-1.0F, (double)0.0F)) {
            mc.player.input.sneaking = true;
         } else if (this.isOffsetBBEmpty((double)0.0F, (double)-1.0F, -0.3)) {
            mc.player.input.sneaking = true;
         } else if (this.isOffsetBBEmpty(-0.3, (double)-1.0F, -0.3)) {
            mc.player.input.sneaking = true;
         }
      }

   }

   @EventListener(
      priority = -100
   )
   public void onMove(MoveEvent event) {
      if (mc.player.isOnGround() && !this.legit.getValue() && this.shouldSafeWalk()) {
         double x = event.getX();
         double y = event.getY();
         double z = event.getZ();
         double increment = 0.05;

         while(x != (double)0.0F && this.isOffsetBBEmpty(x, (double)-1.0F, (double)0.0F)) {
            if (x < increment && x >= -increment) {
               x = (double)0.0F;
            } else if (x > (double)0.0F) {
               x -= increment;
            } else {
               x += increment;
            }
         }

         while(z != (double)0.0F && this.isOffsetBBEmpty((double)0.0F, (double)-1.0F, z)) {
            if (z < increment && z >= -increment) {
               z = (double)0.0F;
            } else if (z > (double)0.0F) {
               z -= increment;
            } else {
               z += increment;
            }
         }

         while(x != (double)0.0F && z != (double)0.0F && this.isOffsetBBEmpty(x, (double)-1.0F, z)) {
            if (x < increment && x >= -increment) {
               double var10000 = (double)0.0F;
            } else {
               x = x > (double)0.0F ? x - increment : x + increment;
            }

            if (z < increment && z >= -increment) {
               z = (double)0.0F;
            } else if (z > (double)0.0F) {
               z -= increment;
            } else {
               z += increment;
            }
         }

         event.setX(x);
         event.setY(y);
         event.setZ(z);
      }

   }

   public boolean shouldSafeWalk() {
      return !this.onlyInsideBlock.getValue() || EntityUtil.isInsideBlock();
   }

   public boolean isOffsetBBEmpty(double offsetX, double offsetY, double offsetZ) {
      Box playerBox = mc.player.getBoundingBox();
      Box box = new Box(playerBox.minX, playerBox.minY, playerBox.maxZ, playerBox.maxX, playerBox.minY + (double)0.5F, playerBox.maxZ);
      return !BlockUtil.canCollide(mc.player, box.offset(offsetX, offsetY, offsetZ));
   }
}
