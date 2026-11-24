package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.render.Render3DUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;

public class Trails extends Module {
   public final BooleanSetting players = this.add(new BooleanSetting("Players", true));
   public final BooleanSetting self = this.add(new BooleanSetting("Self", true));
   public final BooleanSetting pearls = this.add(new BooleanSetting("Pearls", true));
   public final BooleanSetting arrows = this.add(new BooleanSetting("Arrows", false));
   public final BooleanSetting exp = this.add(new BooleanSetting("XP", false));
   public final SliderSetting delay = this.add((new SliderSetting("Delay", (double)0.0F, (double)0.0F, (double)5.0F, 0.05)).setSuffix("s"));
   public final SliderSetting length = this.add(new SliderSetting("Length", 80, 5, 500));
   public final SliderSetting lineWidth = this.add(new SliderSetting("LineWidth", (double)1.5F, (double)0.5F, (double)8.0F, 0.1));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 100, 200, 200)));
   public final BooleanSetting rainbow = this.add(new BooleanSetting("Rainbow", true));
   private final Map<Integer, List<Point>> history = new HashMap();
   private long lastSample = 0L;

   public Trails() {
      super("Trails", Module.Category.Render);
      this.setChinese("尾迹");
   }

   public void onToggle() {
      this.history.clear();
      this.lastSample = 0L;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         long now = System.currentTimeMillis();
         long stepMs = (long)(this.delay.getValue() * (double)1000.0F);
         if (now - this.lastSample >= stepMs) {
            this.lastSample = now;

            for(Entity entity : mc.world.getEntities()) {
               if (this.isValid(entity)) {
                  List<Point> list = (List)this.history.computeIfAbsent(entity.getId(), (k) -> new ArrayList());
                  list.add(new Point(entity.getPos()));

                  while(list.size() > this.length.getValueInt()) {
                     list.remove(0);
                  }
               }
            }

         }
      }
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (mc.player != null) {
         for(List<Point> list : this.history.values()) {
            if (list.size() >= 2) {
               for(int i = 1; i < list.size(); ++i) {
                  Point a = (Point)list.get(i - 1);
                  Point b = (Point)list.get(i);
                  Color c = this.rainbow.getValue() ? Color.getHSBColor((float)((double)i / (double)list.size()) % 1.0F, 0.9F, 1.0F) : this.color.getValue();
                  float alpha = (float)i / (float)list.size();
                  Color seg = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)((float)c.getAlpha() * alpha));
                  Render3DUtil.drawLine(a.pos, b.pos, seg);
               }
            }
         }

      }
   }

   private boolean isValid(Entity entity) {
      if (entity == mc.player) {
         return this.self.getValue();
      } else if (entity instanceof PlayerEntity) {
         return this.players.getValue();
      } else if (entity instanceof EnderPearlEntity) {
         return this.pearls.getValue();
      } else if (entity instanceof ArrowEntity) {
         return this.arrows.getValue();
      } else {
         return entity instanceof ExperienceBottleEntity ? this.exp.getValue() : false;
      }
   }

   private static final class Point {
      final Vec3d pos;

      Point(Vec3d pos) {
         this.pos = pos;
      }
   }
}
