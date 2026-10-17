package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.ClientTickEvent;
import dev.baimaodaxia.api.events.impl.MoveEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.utils.math.MathUtil;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TpMace extends Module {
   public static TpMace INSTANCE;
   private final Timer attackCooldown = new Timer();
   private PlayerEntity target = null;
   private long lastAttackTime = 0L;
   private boolean reset = false;
   public final SliderSetting targetRange = this.add((new SliderSetting("TargetRange", (double)10.0F, (double)3.0F, (double)30.0F, (double)1.0F)).setSuffix("m"));
   public final SliderSetting attackRange = this.add((new SliderSetting("AttackRange", (double)6.0F, (double)2.0F, (double)10.0F, 0.1)).setSuffix("m"));
   public final SliderSetting cooldownTime = this.add((new SliderSetting("Cooldown", (double)500.0F, (double)100.0F, (double)2000.0F, (double)10.0F)).setSuffix("ms"));
   public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   public final BooleanSetting track = this.add(new BooleanSetting("Track", true));
   public final SliderSetting trackSpeed = this.add((new SliderSetting("TrackSpeed", 0.2873, 0.05, (double)0.5F, 0.01)).setSuffix("格/tick"));
   public final BooleanSetting rubberband = this.add(new BooleanSetting("Lagback", true));
   public final BooleanSetting onlyOnGround = this.add(new BooleanSetting("OnlyOnGround", true));
   private final BooleanSetting friendCheck = this.add(new BooleanSetting("FriendCheck", true));
   public final BooleanSetting autoBounce = this.add(new BooleanSetting("AutoBounce", false));
   public final SliderSetting bounceHeight = this.add((new SliderSetting("BounceHeight", (double)6.0F, (double)1.0F, (double)30.0F, (double)1.0F)).setSuffix("格"));
   public final SliderSetting bounceSpeed = this.add((new SliderSetting("BounceSpeed", 0.6, 0.1, (double)3.0F, 0.05)).setSuffix("格/tick"));
   public final SliderSetting bounceRange = this.add((new SliderSetting("BounceRange", (double)1.0F, 0.1, (double)5.0F, 0.1)).setSuffix("格"));
   private boolean bouncingDown = true;

   public TpMace() {
      super("TpMace", Module.Category.Combat);
      this.setChinese("自动重锤");
      INSTANCE = this;
   }

   public void onEnable() {
      super.onEnable();
      if (!nullCheck()) {
         this.target = null;
         this.lastAttackTime = 0L;
         this.reset = false;
         this.attackCooldown.reset();
         this.bouncingDown = true;
      }
   }

   public void onDisable() {
      super.onDisable();
      this.target = null;
      this.reset = false;
      this.bouncingDown = true;
   }

   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   private boolean isTargetFriend(PlayerEntity player) {
      return !this.friendCheck.getValue() ? false : Alien.FRIEND.isFriend(player);
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (!event.isPost()) {
            if (this.attackCooldown.passedMs((double)((long)this.cooldownTime.getValue()))) {
               this.target = this.getTarget();
               if (this.target != null) {
                  if (this.isTargetFriend(this.target)) {
                     this.target = null;
                  } else if (!((double)mc.player.distanceTo(this.target) > this.attackRange.getValue())) {
                     if (!this.onlyOnGround.getValue() || mc.player.isOnGround() || this.autoBounce.getValue()) {
                        int slot = this.findMaceSlot();
                        if (slot != -1) {
                           if (this.rotate.getValue()) {
                              Vec3d hitVec = MathUtil.getClosestPointToBox(mc.player.getEyePos(), this.target.getBoundingBox());
                              if (hitVec != null) {
                                 Alien.ROTATION.lookAt(hitVec);
                              }
                           }

                           if (System.currentTimeMillis() - this.lastAttackTime > 1000L) {
                              this.attackTarget(slot);
                              this.lastAttackTime = System.currentTimeMillis();
                              this.attackCooldown.reset();
                           }

                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onMove(MoveEvent event) {
      if (!nullCheck()) {
         if (this.target != null) {
            double dX = this.target.getX() - mc.player.getX();
            double dZ = this.target.getZ() - mc.player.getZ();
            double horizontalDist = Math.sqrt(dX * dX + dZ * dZ);
            boolean inBounceRange = horizontalDist <= this.bounceRange.getValue();
            if (this.track.getValue() && !inBounceRange && !mc.player.isOnGround() && (double)mc.player.distanceTo(this.target) > this.attackRange.getValue() && System.currentTimeMillis() - this.lastAttackTime >= 200L && horizontalDist > (double)0.0F) {
               double speed = this.trackSpeed.getValue();
               event.setX(dX / horizontalDist * speed);
               event.setZ(dZ / horizontalDist * speed);
            }

            if (this.autoBounce.getValue() && !mc.player.isOnGround()) {
               if (inBounceRange) {
                  event.setX((double)0.0F);
                  event.setZ((double)0.0F);
               }

               double targetFeetY = this.target.getY();
               double targetFloor = targetFeetY + (double)0.5F;
               double targetCeiling = targetFeetY + this.bounceHeight.getValue();
               double speed = this.bounceSpeed.getValue();
               if (this.bouncingDown) {
                  double nextY = mc.player.getY() - speed;
                  if (nextY > targetFloor) {
                     event.setY(-speed);
                  } else {
                     event.setY(targetFloor - mc.player.getY());
                     this.bouncingDown = false;
                  }
               } else {
                  double nextY = mc.player.getY() + speed;
                  if (nextY < targetCeiling) {
                     event.setY(speed);
                  } else {
                     event.setY(targetCeiling - mc.player.getY());
                     this.bouncingDown = true;
                  }
               }
            }

         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof EntityStatusS2CPacket && this.reset) {
         if (mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
         }

         this.reset = false;
      }

      if (event.getPacket() instanceof PlayerPositionLookS2CPacket && this.reset) {
         if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket)event.getPacket();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(packet.getX(), packet.getY(), packet.getZ(), mc.player.isOnGround()));
         }

         this.reset = false;
      }

   }

   private void attackTarget(int slot) {
      if (mc.player != null && this.target != null) {
         int previousSlot = mc.player.getInventory().selectedSlot;
         Vec3d previous = mc.player.getPos();
         if (slot != previousSlot) {
            mc.player.getInventory().selectedSlot = slot;
         }

         mc.interactionManager.attackEntity(mc.player, this.target);
         mc.player.swingHand(Hand.MAIN_HAND);
         if (slot != previousSlot) {
            mc.player.getInventory().selectedSlot = previousSlot;
         }

         if (this.rubberband.getValue()) {
            if (mc.getNetworkHandler() != null) {
               if (this.autoBounce.getValue() && !mc.player.isOnGround()) {
                  mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), this.target.getY(), mc.player.getZ(), false));
               }

               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + (double)1.0F, mc.player.getZ(), false));
               mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(previous.x, previous.y, previous.z, false));
            }

            this.reset = true;
         }

         if (this.rotate.getValue()) {
            Alien.ROTATION.snapBack();
         }

      }
   }

   private PlayerEntity getTarget() {
      PlayerEntity optimalTarget = null;
      double closestDistance = Double.MAX_VALUE;
      float range = (float)this.targetRange.getValue();

      for(PlayerEntity player : mc.world.getPlayers()) {
         if (player != mc.player && player.isAlive() && !(player.getHealth() <= 0.0F) && !this.isTargetFriend(player) && !(mc.player.squaredDistanceTo(player) > (double)(range * range))) {
            double distance = (double)mc.player.distanceTo(player);
            if (distance < closestDistance) {
               closestDistance = distance;
               optimalTarget = player;
            }
         }
      }

      return optimalTarget;
   }

   private int findMaceSlot() {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
            return i;
         }
      }

      return -1;
   }
}
