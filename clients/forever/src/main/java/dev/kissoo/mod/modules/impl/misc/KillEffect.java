package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.PacketEvent;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.DeathEvent;
import dev.kissoo.api.events.impl.TotemEvent;
import dev.kissoo.api.utils.math.MathUtil;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.combat.Criticals;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class KillEffect extends Module {
   public static KillEffect INSTANCE;
   private static final SoundEvent[] MACE_SOUNDS = new SoundEvent[]{
           SoundEvent.of(Identifier.of("forever", "mace.moan1")),
           SoundEvent.of(Identifier.of("forever", "mace.moan2")),
           SoundEvent.of(Identifier.of("forever", "mace.moan3")),
           SoundEvent.of(Identifier.of("forever", "mace.moan4"))
   };

   private final BooleanSetting removeCorpse = this.add(new BooleanSetting("RemoveCorpse", false));
   private final BooleanSetting lightning = this.add(new BooleanSetting("Lightning", true));
   private final BooleanSetting firework = this.add(new BooleanSetting("FireWork", false).setParent());
   private final SliderSetting fireworkHeight = this.add(new SliderSetting("FireworkHeight", 4.0, 1.0, 12.0, 0.1, this.firework::isOpen));
   private final BooleanSetting levelUp = this.add(new BooleanSetting("LevelUp", true).setParent());
   private final SliderSetting lMaxPitch = this.add(new SliderSetting("LMaxPitch", 1.0, 0.0, 2.0, 0.1, this.levelUp::isOpen));
   private final SliderSetting lMinPitch = this.add(new SliderSetting("LMinPitch", 1.0, 0.0, 2.0, 0.1, this.levelUp::isOpen));
   private final BooleanSetting trident = this.add(new BooleanSetting("Trident", false).setParent());
   private final SliderSetting tMaxPitch = this.add(new SliderSetting("TMaxPitch", 1.0, 0.0, 2.0, 0.1, this.trident::isOpen));
   private final SliderSetting tMinPitch = this.add(new SliderSetting("TMinPitch", 1.0, 0.0, 2.0, 0.1, this.trident::isOpen));
   private final BooleanSetting maceSound = this.add(new BooleanSetting("macesound", true));
   private final SliderSetting factor = this.add(new SliderSetting("Factor", 1.0, 1.0, 10.0, 1.0));
   private final BooleanSetting popLightning = this.add(new BooleanSetting("PopLightning", true).setParent());
   private final SliderSetting popDuration = this.add(new SliderSetting("PopDuration", 1000, 100, 5000, 1, this.popLightning::isOpen));
   private final CopyOnWriteArrayList<PopLightning> popLightnings = new CopyOnWriteArrayList<>();
   private final CopyOnWriteArrayList<LocalFirework> localFireworks = new CopyOnWriteArrayList<>();

   public KillEffect() {
      super("KillEffect", Module.Category.Misc);
      INSTANCE = this;
      this.setChinese("击杀效果");
   }

   @EventListener
   public void onPlayerDeath(DeathEvent event) {
      if (!nullCheck()) {
         PlayerEntity player = event.getPlayer();
         if (player != null) {
            for (int i = 0; i < this.factor.getValue(); i++) {
               this.doEffect(player);
            }
         }
      }
   }

   public boolean shouldHideDeathRender(LivingEntity entity) {
      return this.isOn()
              && this.removeCorpse.getValue()
              && entity instanceof PlayerEntity
              && entity != mc.player
              && entity.isDead();
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!event.isPost() || nullCheck()) {
         return;
      }

      this.localFireworks.removeIf(LocalFirework::tick);
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (event.isCancelled() || nullCheck() || !this.maceSound.getValue()) {
         return;
      }

      if (!(event.getPacket() instanceof PlayerInteractEntityC2SPacket packet)
              || Criticals.getInteractType(packet) != PlayerInteractEntityC2SPacket.InteractType.ATTACK
              || mc.player.getMainHandStack().getItem() != Items.MACE) {
         return;
      }

      this.playRandomMaceSound();
   }

   @EventListener
   public void onTotem(TotemEvent event) {
      if (nullCheck() || !this.popLightning.getValue() || event.getPlayer() == mc.player) {
         return;
      }

      LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
      lightning.refreshPositionAfterTeleport(event.getPlayer().getX(), event.getPlayer().getY(), event.getPlayer().getZ());
      lightning.setCosmetic(true);
      mc.world.playSound(mc.player, event.getPlayer().getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 10000.0F, 0.8F + mc.world.random.nextFloat() * 0.2F);
      mc.world.playSound(mc.player, event.getPlayer().getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.WEATHER, 2.0F, 0.5F + mc.world.random.nextFloat() * 0.2F);
      this.popLightnings.add(new PopLightning(lightning));
   }

   @Override
   public void onRender3D(net.minecraft.client.util.math.MatrixStack matrixStack) {
      this.popLightnings.removeIf(PopLightning::isExpired);
      for (PopLightning lightning : this.popLightnings) {
         lightning.entity.age = 1;
         lightning.entity.setCosmetic(true);

         VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
         mc.getEntityRenderDispatcher().render(
                 lightning.entity,
                 lightning.entity.getX() - mc.getEntityRenderDispatcher().camera.getPos().x,
                 lightning.entity.getY() - mc.getEntityRenderDispatcher().camera.getPos().y,
                 lightning.entity.getZ() - mc.getEntityRenderDispatcher().camera.getPos().z,
                 0.0F,
                 mc.getRenderTickCounter().getTickDelta(true),
                 matrixStack,
                 immediate,
                 0xF000F0
         );
         immediate.draw();
      }
   }

   private void playRandomMaceSound() {
      try {
         SoundEvent sound = MACE_SOUNDS[mc.world.random.nextInt(MACE_SOUNDS.length)];
         mc.getSoundManager().play(PositionedSoundInstance.master(sound, 1.0F, 1.0F));
      } catch (Exception ignored) {
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

      if (this.firework.getValue()) {
         spawnLocalFirework(x, y, z);
      }

      if (this.levelUp.getValue()) {
         mc.world
            .playSound(
               mc.player,
               x,
               y,
               z,
               SoundEvents.ENTITY_PLAYER_LEVELUP,
               SoundCategory.PLAYERS,
               100.0F,
               MathUtil.random(this.lMinPitch.getValueFloat(), this.lMaxPitch.getValueFloat())
            );
      }

      if (this.trident.getValue()) {
         mc.world
            .playSound(
               mc.player,
               x,
               y,
               z,
               SoundEvents.ITEM_TRIDENT_THUNDER,
               SoundCategory.MASTER,
               999.0F,
               MathUtil.random(this.tMinPitch.getValueFloat(), this.tMaxPitch.getValueFloat())
            );
      }
   }

   private void spawnLocalFirework(double x, double y, double z) {
      ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
      stack.set(DataComponentTypes.FIREWORKS, new FireworksComponent(127, List.of()));

      FireworkRocketEntity rocket = new FireworkRocketEntity(mc.world, x, y + 0.05, z, stack);
      rocket.setNoGravity(true);
      rocket.setVelocity(0.0, 0.28, 0.0);
      mc.world.addEntity(rocket);
      mc.world.playSound(mc.player, x, y, z, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.0F, 1.0F);

      this.localFireworks.add(new LocalFirework(rocket, y, this.fireworkHeight.getValue()));
   }

   private void explodeLocalFirework(FireworkRocketEntity rocket) {
      double x = rocket.getX();
      double y = rocket.getY();
      double z = rocket.getZ();

      mc.world.addImportantParticle(ParticleTypes.EXPLOSION_EMITTER, true, x, y, z, 0.0, 0.0, 0.0);
      for (int i = 0; i < 80; i++) {
         Vec3d velocity = new Vec3d(
                 mc.world.random.nextDouble() - 0.5,
                 mc.world.random.nextDouble() - 0.15,
                 mc.world.random.nextDouble() - 0.5
         ).normalize().multiply(0.25 + mc.world.random.nextDouble() * 0.45);
         mc.world.addImportantParticle(ParticleTypes.FIREWORK, true, x, y, z, velocity.x, velocity.y, velocity.z);
      }

      mc.world.playSound(mc.player, x, y, z, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0F, 1.0F);
      mc.world.playSound(mc.player, x, y, z, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 1.5F, 1.0F);
      rocket.discard();
   }

   private class PopLightning {
      private final LightningEntity entity;
      private final Timer timer = new Timer();

      private PopLightning(LightningEntity entity) {
         this.entity = entity;
         this.timer.reset();
      }

      private boolean isExpired() {
         return this.timer.passedMs((long) KillEffect.this.popDuration.getValue());
      }
   }

   private class LocalFirework {
      private final FireworkRocketEntity rocket;
      private final double startY;
      private final double height;
      private final int maxTicks;
      private int ticks;

      private LocalFirework(FireworkRocketEntity rocket, double startY, double height) {
         this.rocket = rocket;
         this.startY = startY;
         this.height = height;
         this.maxTicks = Math.max(1, (int) Math.ceil(height / 0.28));
      }

      private boolean tick() {
         if (KillEffect.nullCheck() || this.rocket.isRemoved()) {
            return true;
         }

         this.rocket.setNoGravity(true);
         this.rocket.setVelocity(0.0, 0.28, 0.0);
         this.ticks++;

         if (this.rocket.getY() - this.startY >= this.height || this.ticks >= this.maxTicks) {
            KillEffect.this.explodeLocalFirework(this.rocket);
            return true;
         }
         return false;
      }
   }
}
