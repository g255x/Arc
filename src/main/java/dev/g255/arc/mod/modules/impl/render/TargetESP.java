package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.utils.render.CaptureMark;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.util.math.MatrixStack;

public class TargetESP extends Module {
   public static TargetESP INSTANCE;
   public final BooleanSetting nurikZapen = this.add(new BooleanSetting("NurikZapen", true));
   public final BooleanSetting players = this.add(new BooleanSetting("Players", true));
   public final BooleanSetting skipFriends = this.add(new BooleanSetting("SkipFriends", true));
   public final BooleanSetting skipDead = this.add(new BooleanSetting("SkipDead", true));
   public final BooleanSetting skipLogged = this.add(new BooleanSetting("SkipLogged", true));
   public final BooleanSetting autoTarget = this.add(new BooleanSetting("AutoTarget", true));
   public final SliderSetting range = this.add(new SliderSetting("Range", (double)20.0F, (double)0.0F, (double)50.0F, 0.1));
   public final SliderSetting targetCount = this.add(new SliderSetting("TargetCount", (double)1.0F, (double)1.0F, (double)10.0F, (double)1.0F));
   public final BooleanSetting rainbow = this.add(new BooleanSetting("Rainbow", false));
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(155, 123, 255, 255)));
   public static Entity autoTargetEntity;
   public static List<Entity> autoTargetEntities = new ArrayList();

   public TargetESP() {
      super("TargetESP", Module.Category.Render);
      this.setChinese("目标ESP");
      INSTANCE = this;
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         CaptureMark.tick();
         if (this.autoTarget.getValue()) {
            autoTargetEntities.clear();
            ArrayList<Entity> validEntities = new ArrayList();
            float closestDistance = Float.MAX_VALUE;

            for(Entity entity : Arc.THREAD.getEntities()) {
               if (entity instanceof PlayerEntity && entity != mc.player && this.players.getValue() && (!this.skipFriends.getValue() || !Arc.FRIEND.isFriend((PlayerEntity)entity))) {
                  float distance = entity.distanceTo(mc.player);
                  if (this.range.getValue() <= (double)0.0F || (double)distance <= this.range.getValue()) {
                     validEntities.add(entity);
                  }
               }
            }

            validEntities.sort((e1, e2) -> Float.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player)));
            int count = Math.min(this.targetCount.getValueInt(), validEntities.size());

            for(int i = 0; i < count; ++i) {
               autoTargetEntities.add((Entity)validEntities.get(i));
            }

            autoTargetEntity = autoTargetEntities.isEmpty() ? null : (Entity)autoTargetEntities.get(0);
         }

      }
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (!nullCheck() && this.nurikZapen.getValue()) {
         if (this.autoTarget.getValue() && !autoTargetEntities.isEmpty()) {
            for(Entity entity : autoTargetEntities) {
               if (this.players.getValue() && entity instanceof PlayerEntity && (!this.skipDead.getValue() || !entity.isWet()) && (!this.skipFriends.getValue() || !Arc.FRIEND.isFriend((PlayerEntity)entity)) && (this.range.getValue() <= (double)0.0F || (double)entity.distanceTo(mc.player) <= this.range.getValue())) {
                  CaptureMark.render(entity, this.color.getValue(), this.rainbow.getValue());
               }
            }
         }

      }
   }
}
