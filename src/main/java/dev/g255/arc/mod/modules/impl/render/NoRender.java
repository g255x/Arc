package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.api.events.Event;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.ParticleEvent;
import dev.g255.arc.api.events.impl.RenderEntityEvent;
import dev.g255.arc.api.events.impl.TickEntityEvent;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.client.particle.ExplosionLargeParticle;
import net.minecraft.client.particle.ElderGuardianAppearanceParticle;
import net.minecraft.client.particle.SpellParticle;

public class NoRender extends Module {
   public static NoRender INSTANCE;
   public final BooleanSetting fastItem = this.add((new BooleanSetting("2DItem", false)).setParent());
   public final BooleanSetting castShadow;
   public final BooleanSetting renderSidesOfItems;
   public final BooleanSetting potionsIcon;
   public final BooleanSetting weather;
   public final BooleanSetting invisible;
   public final BooleanSetting lightsUpdate;
   public final BooleanSetting potions;
   public final BooleanSetting xp;
   public final BooleanSetting arrows;
   public final BooleanSetting eggs;
   public final BooleanSetting item;
   public final BooleanSetting armorParts;
   public final BooleanSetting armorTrim;
   public final BooleanSetting armorGlint;
   public final BooleanSetting hurtCam;
   public final BooleanSetting fireOverlay;
   public final BooleanSetting waterOverlay;
   public final BooleanSetting blockOverlay;
   public final BooleanSetting portal;
   public final BooleanSetting totem;
   public final BooleanSetting nausea;
   public final BooleanSetting blindness;
   public final BooleanSetting fog;
   public final BooleanSetting darkness;
   public final BooleanSetting fireEntity;
   public final BooleanSetting antiTitle;
   public final BooleanSetting antiPlayerCollision;
   public final BooleanSetting effect;
   public final BooleanSetting elderGuardian;
   public final BooleanSetting explosions;
   public final BooleanSetting campFire;
   public final BooleanSetting fireworks;
   public final BooleanSetting guiToast;

   public NoRender() {
      super("NoRender", "Disables all overlays and potion effects.", Module.Category.Render);
      BooleanSetting var10006 = this.fastItem;
      Objects.requireNonNull(var10006);
      this.castShadow = this.add(new BooleanSetting("CastShadow", true, var10006::isOpen));
      var10006 = this.fastItem;
      Objects.requireNonNull(var10006);
      this.renderSidesOfItems = this.add(new BooleanSetting("RenderSidesOfItems", false, var10006::isOpen));
      this.potionsIcon = this.add(new BooleanSetting("PotionsIcon", false));
      this.weather = this.add(new BooleanSetting("Weather", true));
      this.invisible = this.add(new BooleanSetting("Invisible", false));
      this.lightsUpdate = this.add(new BooleanSetting("LightsUpdate", false));
      this.potions = this.add(new BooleanSetting("Potions", true));
      this.xp = this.add(new BooleanSetting("XP", true));
      this.arrows = this.add(new BooleanSetting("Arrows", false));
      this.eggs = this.add(new BooleanSetting("Eggs", false));
      this.item = this.add(new BooleanSetting("Items", false));
      this.armorParts = this.add(new BooleanSetting("ArmorParts", false));
      this.armorTrim = this.add(new BooleanSetting("ArmorTrim", false));
      this.armorGlint = this.add(new BooleanSetting("ArmorGlint", false));
      this.hurtCam = this.add(new BooleanSetting("HurtCam", true));
      this.fireOverlay = this.add(new BooleanSetting("FireOverlay", true));
      this.waterOverlay = this.add(new BooleanSetting("WaterOverlay", true));
      this.blockOverlay = this.add(new BooleanSetting("BlockOverlay", true));
      this.portal = this.add(new BooleanSetting("Portal", true));
      this.totem = this.add(new BooleanSetting("Totem", true));
      this.nausea = this.add(new BooleanSetting("Nausea", true));
      this.blindness = this.add(new BooleanSetting("Blindness", true));
      this.fog = this.add(new BooleanSetting("Fog", false));
      this.darkness = this.add(new BooleanSetting("Darkness", true));
      this.fireEntity = this.add(new BooleanSetting("EntityFire", true));
      this.antiTitle = this.add(new BooleanSetting("Title", false));
      this.antiPlayerCollision = this.add(new BooleanSetting("PlayerCollision", true));
      this.effect = this.add(new BooleanSetting("Effect", true));
      this.elderGuardian = this.add(new BooleanSetting("Guardian", false));
      this.explosions = this.add(new BooleanSetting("Explosions", true));
      this.campFire = this.add(new BooleanSetting("CampFire", false));
      this.fireworks = this.add(new BooleanSetting("Fireworks", false));
      this.guiToast = this.add(new BooleanSetting("GuiToast", false));
      this.setChinese("禁用渲染");
      INSTANCE = this;
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof TitleS2CPacket && this.antiTitle.getValue()) {
         event.setCancelled(true);
      }

   }

   @EventListener
   public void onRender(TickEntityEvent event) {
      this.cancelEvent(event.getEntity(), event);
   }

   @EventListener
   public void onRender(RenderEntityEvent event) {
      this.cancelEvent(event.getEntity(), event);
   }

   private void cancelEvent(Entity entity, Event event) {
      if (entity instanceof PotionEntity && this.potions.getValue()) {
         event.cancel();
      } else if (entity instanceof ExperienceBottleEntity && this.xp.getValue()) {
         event.cancel();
      } else if (entity instanceof ArrowEntity && this.arrows.getValue()) {
         event.cancel();
      } else if (entity instanceof EggEntity && this.eggs.getValue()) {
         event.cancel();
      } else if (entity instanceof ItemEntity && this.item.getValue()) {
         event.cancel();
      }

   }

   @EventListener
   public void onParticle(ParticleEvent event) {
      if (this.elderGuardian.getValue() && event.particle instanceof ElderGuardianAppearanceParticle) {
         event.cancel();
      } else if (this.explosions.getValue() && event.particle instanceof ExplosionLargeParticle) {
         event.cancel();
      } else if (this.campFire.getValue() && event.particle instanceof CampfireSmokeParticle) {
         event.cancel();
      } else if (!this.fireworks.getValue() || !(event.particle instanceof FireworksSparkParticle.FireworkParticle) && !(event.particle instanceof FireworksSparkParticle.Flash)) {
         if (this.effect.getValue() && event.particle instanceof SpellParticle) {
            event.cancel();
         }
      } else {
         event.cancel();
      }

   }
}
