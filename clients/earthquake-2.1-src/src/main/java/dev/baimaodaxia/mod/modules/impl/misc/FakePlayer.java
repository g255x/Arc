package dev.baimaodaxia.mod.modules.impl.misc;

import com.mojang.authlib.GameProfile;
import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.api.utils.math.DamageUtils;
import dev.baimaodaxia.api.utils.world.BlockPosX;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.asm.accessors.ILivingEntity;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.combat.AutoAnchor;
import dev.baimaodaxia.mod.modules.impl.combat.AutoCrystal;
import dev.baimaodaxia.mod.modules.impl.combat.Criticals;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity.RemovalReason;
import dev.baimaodaxia.api.utils.InteractTypeUtil;

public class FakePlayer extends Module {
   public static FakePlayer INSTANCE;
   public static FakePlayerEntity fakePlayer;
   final StringSetting name = this.add(new StringSetting("Name", "FakePlayer"));
   private final BooleanSetting damage = this.add(new BooleanSetting("Damage", true));
   private final BooleanSetting autoTotem = this.add(new BooleanSetting("AutoTotem", true));
   public final BooleanSetting record = this.add(new BooleanSetting("Record", false));
   public final BooleanSetting play = this.add(new BooleanSetting("Play", false));
   final List<PlayerState> positions = new ArrayList();
   int movementTick;
   boolean lastRecordValue = false;

   public FakePlayer() {
      super("FakePlayer", Module.Category.Misc);
      this.setChinese("假人");
      INSTANCE = this;
   }

   public String getInfo() {
      return this.name.getValue();
   }

   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         fakePlayer = new FakePlayerEntity(mc.player, this.name.getValue());
         mc.world.addEntity(fakePlayer);
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (fakePlayer != null && fakePlayer.clientWorld == mc.world) {
         if (this.autoTotem.getValue()) {
            if (fakePlayer.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
               fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }

            if (fakePlayer.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
               fakePlayer.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
         }

         if (this.record.getValue() != this.lastRecordValue && this.record.getValue()) {
            this.positions.clear();
         }

         this.lastRecordValue = this.record.getValue();
         if (this.record.getValue()) {
            this.positions.add(new PlayerState(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch()));
         }

         if (this.play.getValue() && !this.positions.isEmpty()) {
            ++this.movementTick;
            if (this.movementTick >= this.positions.size()) {
               this.movementTick = 0;
            }

            PlayerState p = (PlayerState)this.positions.get(this.movementTick);
            fakePlayer.setYaw(p.yaw);
            fakePlayer.setPitch(p.pitch);
            fakePlayer.setHeadYaw(p.yaw);
            fakePlayer.updateTrackedPosition(p.x, p.y, p.z);
            fakePlayer.updateTrackedPositionAndAngles(p.x, p.y, p.z, p.yaw, p.pitch, 3);
         }

      } else {
         this.disable();
      }
   }

   public void onDisable() {
      if (fakePlayer != null) {
         fakePlayer.kill();
         fakePlayer.setRemoved(RemovalReason.KILLED);
         fakePlayer.onRemoved();
         fakePlayer = null;
      }
   }

   @EventListener
   public void onAttack(PacketEvent.Send event) {
      Packet<?> packet2 = event.getPacket();
      PlayerInteractEntityC2SPacket packet;
      if (packet2 instanceof PlayerInteractEntityC2SPacket && Criticals.getInteractType(packet = (PlayerInteractEntityC2SPacket)packet2) == InteractTypeUtil.ATTACK && Criticals.getEntity(packet) == fakePlayer) {
         mc.world.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0F, 1.0F);
         float damage = DamageUtils.getAttackDamage(mc.player, fakePlayer);
         if ((mc.player.fallDistance > 0.0F || Criticals.INSTANCE.isOn() && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground) && (mc.player.isOnGround() || !Criticals.INSTANCE.onlyGround.getValue())) && (!mc.player.isOnGround() || Criticals.INSTANCE.isOn() && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground)) && !mc.player.isClimbing() && !mc.player.isTouchingWater() && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) && !mc.player.hasVehicle()) {
            mc.world.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            mc.player.addCritParticles(fakePlayer);
         }

         if (fakePlayer.hurtTime <= 0) {
            fakePlayer.onDamaged(mc.world.getDamageSources().generic());
            if (fakePlayer.getAbsorptionAmount() >= damage) {
               fakePlayer.setAbsorptionAmount(fakePlayer.getAbsorptionAmount() - damage);
            } else {
               float damage2 = damage - fakePlayer.getAbsorptionAmount();
               fakePlayer.setAbsorptionAmount(0.0F);
               fakePlayer.setHealth(fakePlayer.getHealth() - damage2);
            }

            if (fakePlayer.isDead()) {
               Alien.POP.onTotemPop(fakePlayer);
               if (fakePlayer.tryUseTotem(mc.world.getDamageSources().generic())) {
                  fakePlayer.setHealth(10.0F);
                  (new EntityStatusS2CPacket(fakePlayer, (byte)35)).apply(mc.getNetworkHandler());
               }
            }
         }
      }

   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      Packet<?> packet;
      if (this.damage.getValue() && fakePlayer != null && fakePlayer.hurtTime <= 0 && (packet = event.getPacket()) instanceof ExplosionS2CPacket) {
         ExplosionS2CPacket explosion = (ExplosionS2CPacket)packet;
         Vec3d vec3d = new Vec3d(explosion.getX(), explosion.getY(), explosion.getZ());
         if (Math.sqrt(vec3d.squaredDistanceTo(fakePlayer.getPos())) > (double)10.0F) {
            return;
         }

         float damage = BlockUtil.getBlock(new BlockPosX(explosion.getX(), explosion.getY(), explosion.getZ())) == Blocks.RESPAWN_ANCHOR ? (float)AutoAnchor.INSTANCE.getAnchorDamage(new BlockPosX(explosion.getX(), explosion.getY(), explosion.getZ()), fakePlayer, fakePlayer) : AutoCrystal.INSTANCE.calculateDamage((Vec3d)(new Vec3d(explosion.getX(), explosion.getY(), explosion.getZ())), fakePlayer, fakePlayer);
         fakePlayer.onDamaged(mc.world.getDamageSources().generic());
         if (fakePlayer.getAbsorptionAmount() >= damage) {
            fakePlayer.setAbsorptionAmount(fakePlayer.getAbsorptionAmount() - damage);
         } else {
            float damage2 = damage - fakePlayer.getAbsorptionAmount();
            fakePlayer.setAbsorptionAmount(0.0F);
            fakePlayer.setHealth(fakePlayer.getHealth() - damage2);
         }

         if (fakePlayer.isDead()) {
            Alien.POP.onTotemPop(fakePlayer);
            if (fakePlayer.tryUseTotem(mc.world.getDamageSources().generic())) {
               fakePlayer.setHealth(10.0F);
               (new EntityStatusS2CPacket(fakePlayer, (byte)35)).apply(mc.getNetworkHandler());
            }
         }
      }

   }

   public static class FakePlayerEntity extends OtherClientPlayerEntity {
      private final boolean onGround;

      public FakePlayerEntity(PlayerEntity player, String name) {
         super(Wrapper.mc.world, new GameProfile(UUID.fromString("66666666-6666-6666-6666-666666666666"), name));
         this.copyPositionAndRotation(player);
         this.prevX = player.prevX;
         this.prevZ = player.prevZ;
         this.prevY = player.prevY;
         this.bodyYaw = player.bodyYaw;
         this.headYaw = player.headYaw;
         this.handSwingProgress = player.handSwingProgress;
         this.handSwingTicks = player.handSwingTicks;
         this.limbAnimator.setSpeed(player.limbAnimator.getSpeed());
         this.limbAnimator.pos = player.limbAnimator.getPos();
         ((ILivingEntity)this).setLeaningPitch(((ILivingEntity)player).getLeaningPitch());
         ((ILivingEntity)this).setLastLeaningPitch(((ILivingEntity)player).getLeaningPitch());
         this.touchingWater = player.isTouchingWater();
         this.setSneaking(player.isSneaking());
         this.setPose(player.getPose());
         this.setFlag(7, player.isFallFlying());
         this.onGround = player.isOnGround();
         this.setOnGround(this.onGround);
         this.getInventory().clone(player.getInventory());
         this.setAbsorptionAmountUnclamped(player.getAbsorptionAmount());
         this.setHealth(player.getHealth());
         this.setBoundingBox(player.getBoundingBox());
      }

      public boolean isOnGround() {
         return this.onGround;
      }

      public boolean isSpectator() {
         return false;
      }

      public boolean isCreative() {
         return false;
      }

      public boolean tryUseTotem(net.minecraft.entity.damage.DamageSource source) {
         return false;
      }
   }

   private static record PlayerState(double x, double y, double z, float yaw, float pitch) {
   }
}
