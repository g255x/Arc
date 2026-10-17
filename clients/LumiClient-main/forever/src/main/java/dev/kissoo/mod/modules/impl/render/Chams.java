package dev.kissoo.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.Render3DEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.render.CrystalRenderer;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Chams extends Module {
   public static Chams INSTANCE;
   public final BooleanSetting crystal = this.add(new BooleanSetting("Crystal", true).setParent());
   public final BooleanSetting cancelVanilla = this.add(new BooleanSetting("CancelVanilla", true, this.crystal::isOpen));
   public final BooleanSetting customRender = this.add(new BooleanSetting("CustomRender", true, this.crystal::isOpen).setParent());
   public final ColorSetting crystalOutline = this.add(
           new ColorSetting("Outline", new Color(255, 255, 255, 255), () -> this.crystal.isOpen() && this.customRender.isOpen())
   );
   public final ColorSetting crystalFill = this.add(
           new ColorSetting("Fill", new Color(255, 255, 255, 40), () -> this.crystal.isOpen() && this.customRender.isOpen())
   );
   public final SliderSetting crystalScale = this.add(
           new SliderSetting("Scale", 1.0, 0.1, 3.0, 0.01, () -> this.crystal.isOpen() && this.customRender.isOpen())
   );
   public final SliderSetting lineWidth = this.add(
           new SliderSetting("LineWidth", 1.5, 0.5, 5.0, 0.1, () -> this.crystal.isOpen() && this.customRender.isOpen())
   );
   public final SliderSetting range = this.add(
           new SliderSetting("Range", 12.0, 0.0, 30.0, 1.0, () -> this.crystal.isOpen() && this.customRender.isOpen())
   );
   public final BooleanSetting throughWall = this.add(new BooleanSetting("ThroughWall", false).setParent());
   private final BooleanSetting Crystals = this.add(new BooleanSetting("Crystals", true, this.throughWall::isOpen));
   private final BooleanSetting Players = this.add(new BooleanSetting("Players", true, this.throughWall::isOpen));
   private final BooleanSetting Mobs = this.add(new BooleanSetting("Mobs", true, this.throughWall::isOpen));
   private final BooleanSetting Animals = this.add(new BooleanSetting("Animals", true, this.throughWall::isOpen));
   private final BooleanSetting Villagers = this.add(new BooleanSetting("Villagers", true, this.throughWall::isOpen));
   private final BooleanSetting Slimes = this.add(new BooleanSetting("Slimes", true, this.throughWall::isOpen));
   public final ColorSetting hand = this.add(new ColorSetting("Hand", -1).injectBoolean(true));
   public int age;

   public Chams() {
      super("Chams", Module.Category.Render);
      this.setChinese("模型上色");
      INSTANCE = this;
   }

   public boolean shouldRenderCustomCrystal() {
      return this.isOn() && this.crystal.getValue() && this.customRender.getValue();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.age++;
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (this.hand.booleanValue) {
         applyHandColor();
      }
      if (this.shouldRenderCustomCrystal()) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         try {
            renderCustomCrystals(event);
         } finally {
            if (this.hand.booleanValue) {
               applyHandColor();
            }
         }
      }
   }

   private void applyHandColor() {
      Color color = this.hand.getValue();
      RenderSystem.setShaderColor(
              color.getRed() / 255.0F,
              color.getGreen() / 255.0F,
              color.getBlue() / 255.0F,
              color.getAlpha() / 255.0F
      );
   }

   private void renderCustomCrystals(Render3DEvent event) {
      if (mc.world == null || mc.player == null) return;

      double rangeSq = this.range.getValue() * this.range.getValue();

      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof EndCrystalEntity crystal)) continue;
         if (!crystal.isAlive()) continue;

         double distSq = mc.player.squaredDistanceTo(entity.getPos());
         if (distSq > rangeSq) continue;

         CrystalRenderer.renderCrystal(event, crystal, this);
      }
   }

   public boolean chams(Entity entity) {
      if (entity instanceof EndCrystalEntity) {
         return this.Crystals.getValue();
      } else if (entity instanceof SlimeEntity) {
         return this.Slimes.getValue();
      } else if (entity instanceof PlayerEntity) {
         return this.Players.getValue();
      } else if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
         return this.Villagers.getValue();
      } else if (entity instanceof AnimalEntity) {
         return this.Animals.getValue();
      } else {
         return entity instanceof MobEntity ? this.Mobs.getValue() : false;
      }
   }
}
