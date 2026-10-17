package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.MovementUtil;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.asm.accessors.IPlayerInteractEntityC2SPacket;
import dev.baimaodaxia.asm.accessors.IPlayerMoveC2SPacket;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.exploit.Blink;
import dev.baimaodaxia.mod.modules.impl.exploit.BowBomb;
import dev.baimaodaxia.mod.modules.impl.exploit.Phase;
import dev.baimaodaxia.mod.modules.impl.player.AutoPearl;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import dev.baimaodaxia.api.utils.InteractTypeUtil;

public class Criticals extends Module {
   public static Criticals INSTANCE;
   public final EnumSetting<Mode> mode;
   public final BooleanSetting onlyGround;
   private final BooleanSetting setOnGround;
   private final BooleanSetting blockCheck;
   private final BooleanSetting autoJump;
   private final BooleanSetting mini;
   private final SliderSetting y;
   private final BooleanSetting autoDisable;
   private final BooleanSetting crawlingDisable;
   private final BooleanSetting flight;
   boolean requireJump;

   public Criticals() {
      super("Criticals", Module.Category.Combat);
      this.mode = this.add(new EnumSetting("Mode", Criticals.Mode.OldNCP));
      this.onlyGround = this.add(new BooleanSetting("OnlyGround", true, () -> !this.mode.is(Criticals.Mode.Ground)));
      this.setOnGround = this.add(new BooleanSetting("SetNoGround", false, () -> this.mode.is(Criticals.Mode.Ground)));
      this.blockCheck = this.add(new BooleanSetting("BlockCheck", true, () -> this.mode.is(Criticals.Mode.Ground)));
      this.autoJump = this.add((new BooleanSetting("AutoJump", true, () -> this.mode.is(Criticals.Mode.Ground))).setParent());
      this.mini = this.add(new BooleanSetting("Mini", true, () -> this.mode.is(Criticals.Mode.Ground) && this.autoJump.isOpen()));
      this.y = this.add(new SliderSetting("MotionY", 0.05, (double)0.0F, (double)1.0F, 1.0E-10, () -> this.mode.is(Criticals.Mode.Ground) && this.autoJump.isOpen()));
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true, () -> this.mode.is(Criticals.Mode.Ground)));
      this.crawlingDisable = this.add(new BooleanSetting("CrawlingDisable", true, () -> this.mode.is(Criticals.Mode.Ground)));
      this.flight = this.add(new BooleanSetting("Flight", false, () -> this.mode.is(Criticals.Mode.Ground)));
      this.requireJump = false;
      this.setChinese("刀刀暴击");
      INSTANCE = this;
   }

   public String getInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!event.isCancelled()) {
         if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
            if (this.mode.is(Criticals.Mode.Ground)) {
               if (!BowBomb.send) {
                  if (!AutoPearl.throwing && !Phase.INSTANCE.isOn()) {
                     if (this.setOnGround.getValue()) {
                        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                           ((IPlayerMoveC2SPacket)event.getPacket()).setOnGround(false);
                        }

                     }
                  }
               }
            } else {
               Packet<?> packet2 = event.getPacket();
               Entity entity;
               PlayerInteractEntityC2SPacket packet;
               if (packet2 instanceof PlayerInteractEntityC2SPacket && getInteractType(packet = (PlayerInteractEntityC2SPacket)packet2) == InteractTypeUtil.ATTACK && !((entity = getEntity(packet)) instanceof EndCrystalEntity) && (!this.onlyGround.getValue() || mc.player.isOnGround() || mc.player.getAbilities().flying) && !mc.player.isInLava() && !mc.player.isTouchingWater() && entity != null) {
                  this.doCrit(entity);
               }

            }
         }
      }
   }

   public void onLogout() {
      if (this.mode.is(Criticals.Mode.Ground) && this.autoDisable.getValue()) {
         this.disable();
      }

   }

   public void onEnable() {
      if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
         this.requireJump = true;
         if (this.mode.is(Criticals.Mode.Ground)) {
            if (nullCheck()) {
               if (this.autoDisable.getValue()) {
                  this.disable();
               }
            } else if (MovementUtil.isMoving() && this.autoDisable.getValue()) {
               this.disable();
            } else if (this.crawlingDisable.getValue() && mc.player.isCrawling()) {
               this.disable();
            } else if (mc.player.isOnGround() && this.autoJump.getValue() && (!this.blockCheck.getValue() || BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).up(2))))) {
               this.jump();
            }
         }

      }
   }

   public void jump() {
      if (this.mini.getValue()) {
         MovementUtil.setMotionY(this.y.getValue());
      } else {
         mc.player.jump();
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
         if (this.mode.is(Criticals.Mode.Ground)) {
            if (this.crawlingDisable.getValue() && mc.player.isCrawling()) {
               this.disable();
            } else if (MovementUtil.isMoving() && this.autoDisable.getValue()) {
               this.disable();
            } else if (this.flight.getValue() && mc.player.fallDistance > 0.0F) {
               MovementUtil.setMotionY((double)0.0F);
               MovementUtil.setMotionX((double)0.0F);
               MovementUtil.setMotionZ((double)0.0F);
               this.requireJump = false;
            } else if (this.blockCheck.getValue() && !BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).up(2)))) {
               this.requireJump = true;
            } else if (mc.player.isOnGround() && this.autoJump.getValue() && (this.flight.getValue() || this.requireJump)) {
               this.jump();
               this.requireJump = false;
            }
         }

      }
   }

   public void doCrit(Entity entity) {
      switch (((Mode)this.mode.getValue()).ordinal()) {
         case 0:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.71875E-7, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
            break;
         case 1:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.062600301692775, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.07260029960661, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
            break;
         case 2:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + (double)0.0625F, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
            break;
         case 3:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.058293536E-5, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 9.16580235E-6, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0371854E-7, mc.player.getZ(), false));
            break;
         case 4:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0045, mc.player.getZ(), true));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.52121E-4, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.3, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.025, mc.player.getZ(), false));
            break;
         case 5:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 5.0E-4, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0E-4, mc.player.getZ(), false));
         case 6:
         default:
            break;
         case 7:
            if (MovementUtil.isMoving() || !MovementUtil.isStatic()) {
               return;
            }

            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + (double)0.0625F, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.045, mc.player.getZ(), false));
      }

   }

   public static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
      return mc.world == null ? null : mc.world.getEntityById(((IPlayerInteractEntityC2SPacket)packet).getEntityId());
   }

   public static int getInteractType(PlayerInteractEntityC2SPacket packet) {
      final int[] result = new int[1];
      packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
         public void interact(Hand hand) {
            result[0] = InteractTypeUtil.INTERACT;
         }

         public void interactAt(Hand hand, Vec3d pos) {
            result[0] = InteractTypeUtil.INTERACT_AT;
         }

         public void attack() {
            result[0] = InteractTypeUtil.ATTACK;
         }
      });
      return result[0];
   }

   public static enum Mode {
      UpdatedNCP,
      Strict,
      NCP,
      OldNCP,
      Hypixel2K22,
      Packet,
      Ground,
      BBTT;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{UpdatedNCP, Strict, NCP, OldNCP, Hypixel2K22, Packet, Ground, BBTT};
      }
   }
}
