package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public class Spiral extends Module {
   private final SliderSetting radius = this.add(new SliderSetting("Radius", (double)0.5F, 0.05, (double)3.0F, 0.05));
   private final SliderSetting speed = this.add(new SliderSetting("Speed", (double)40.0F, (double)1.0F, (double)180.0F, (double)1.0F));
   private final List<Vec3d> points = new ArrayList();
   private int index = 0;

   public Spiral() {
      super("Spiral", "Moves you in a spiral pattern.", Module.Category.Movement);
      this.setChinese("螺旋");
   }

   public void onEnable() {
      this.index = 0;
      this.points.clear();
      this.build();
   }

   private void build() {
      Vec3d pos = mc.player.getPos();
      float angle = 0.0F;

      for(int i = 0; i < 1000; ++i) {
         this.points.add(pos);
         double step = this.radius.getValue() * (double)i * 0.01;
         pos = pos.add(Math.sin(Math.toRadians((double)angle)) * step, (double)0.0F, Math.cos(Math.toRadians((double)angle)) * step);
         angle = MathHelper.wrapDegrees(angle + 17.0F);
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck() && !this.points.isEmpty()) {
         if (this.index >= this.points.size()) {
            this.disable();
         } else {
            Vec3d target = (Vec3d)this.points.get(this.index);
            Vec3d delta = target.subtract(mc.player.getPos());
            double dist = delta.horizontalLength();
            if (dist < 0.3) {
               ++this.index;
            } else {
               double moveSpeed = this.speed.getValue() / (double)20.0F;
               if (dist < moveSpeed) {
                  moveSpeed = dist;
               }

               Vec3d dir = delta.normalize();
               double x = dir.x * moveSpeed;
               double z = dir.z * moveSpeed;
               mc.player.setVelocity(x, mc.player.getVelocity().y, z);
               float yaw = (float)Math.toDegrees(Math.atan2(-x, z));
               mc.player.setYaw(yaw);
            }
         }
      }
   }
}
