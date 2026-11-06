package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.DeathEvent;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.Objects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;

public class KillEffect extends Module {
   private final BooleanSetting lightning = this.add(new BooleanSetting("Lightning", true));
   private final BooleanSetting levelUp = this.add((new BooleanSetting("LevelUp", true)).setParent());
   private final SliderSetting lMaxPitch;
   private final SliderSetting lMinPitch;
   private final BooleanSetting trident;
   private final SliderSetting tMaxPitch;
   private final SliderSetting tMinPitch;
   private final SliderSetting factor;

   public KillEffect() {
      super("KillEffect", Module.Category.Misc);
      BooleanSetting var10009 = this.levelUp;
      Objects.requireNonNull(var10009);
      this.lMaxPitch = this.add(new SliderSetting("LMaxPitch", (double)1.0F, (double)0.0F, (double)2.0F, 0.1, var10009::isOpen));
      var10009 = this.levelUp;
      Objects.requireNonNull(var10009);
      this.lMinPitch = this.add(new SliderSetting("LMinPitch", (double)1.0F, (double)0.0F, (double)2.0F, 0.1, var10009::isOpen));
      this.trident = this.add((new BooleanSetting("Trident", false)).setParent());
      var10009 = this.trident;
      Objects.requireNonNull(var10009);
      this.tMaxPitch = this.add(new SliderSetting("TMaxPitch", (double)1.0F, (double)0.0F, (double)2.0F, 0.1, var10009::isOpen));
      var10009 = this.trident;
      Objects.requireNonNull(var10009);
      this.tMinPitch = this.add(new SliderSetting("TMinPitch", (double)1.0F, (double)0.0F, (double)2.0F, 0.1, var10009::isOpen));
      this.factor = this.add(new SliderSetting("Factor", (double)1.0F, (double)1.0F, (double)10.0F, (double)1.0F));
      this.setChinese("击杀效果");
   }

   @EventListener
   public void onPlayerDeath(DeathEvent event) {
      if (!nullCheck()) {
         PlayerEntity player = event.getPlayer();
         if (player != null) {
            for(int i = 0; (double)i < this.factor.getValue(); ++i) {
               this.doEffect(player);
            }

         }
      }
   }

   private void doEffect(PlayerEntity player) {
      double x = player.getX();
      double y = player.getY();
      double z = player.getZ();
      if (this.lightning.getValue()) {
         LightningEntity lightningEntity = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
         lightningEntity.updatePosition(x, y, z);
         lightningEntity.refreshPositionAfterTeleport(x, y, z);
         mc.world.addEntity(lightningEntity);
      }

      if (this.levelUp.getValue()) {
         mc.world.playSound(mc.player, x, y, z, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 100.0F, MathUtil.random(this.lMinPitch.getValueFloat(), this.lMaxPitch.getValueFloat()));
      }

      if (this.trident.getValue()) {
         mc.world.playSound(mc.player, x, y, z, SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.MASTER, 999.0F, MathUtil.random(this.tMinPitch.getValueFloat(), this.tMaxPitch.getValueFloat()));
      }

   }
}
