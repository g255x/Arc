package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.asm.accessors.IPlayerMoveC2SPacket;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.item.Item;

public class _32kMace extends Module {
   public static _32kMace INSTANCE;
   private final BooleanSetting groundSpoof = this.add(new BooleanSetting("GroundSpoof", true));
   private final SliderSetting tickUntilFly = this.add(new SliderSetting("TickUntilFly", (double)50.0F, (double)0.0F, (double)1000.0F, (double)1.0F));
   private final SliderSetting c03Delay = this.add(new SliderSetting("C03Delay", (double)80.0F, (double)0.0F, (double)1000.0F, (double)1.0F));
   private final SliderSetting attackDelay = this.add(new SliderSetting("AttackDelay", (double)80.0F, (double)0.0F, (double)1000.0F, (double)1.0F));
   private final SliderSetting lagHeight = this.add(new SliderSetting("LagHeight", (double)4.0F, (double)0.0F, (double)10.0F, 0.1));
   private final SliderSetting loopDelay = this.add(new SliderSetting("LoopDelay", (double)400.0F, (double)0.0F, (double)3000.0F, (double)10.0F));
   private final SliderSetting attackRange = this.add(new SliderSetting("AttackRange", (double)6.0F, (double)1.0F, (double)10.0F, 0.1));
   private final EnumSetting<RotateMode> rotateMode;
   private int state;
   private long lastTime;
   private boolean flagFly;
   private boolean flagC03;
   private int originalSlot;
   private PlayerEntity target;

   public _32kMace() {
      super("32kMace", "Wind-charge launch + fake C03 height + Mace strike loop.", Module.Category.Combat);
      this.rotateMode = this.add(new EnumSetting("RotateMode", _32kMace.RotateMode.Default));
      this.state = 0;
      this.lastTime = 0L;
      this.flagFly = false;
      this.flagC03 = false;
      this.originalSlot = -1;
      this.target = null;
      INSTANCE = this;
   }

   public void onEnable() {
      if (mc.player == null) {
         this.toggle();
      } else {
         this.originalSlot = mc.player.getInventory().selectedSlot;
         this.state = 0;
         this.lastTime = System.currentTimeMillis();
         this.flagFly = false;
         this.flagC03 = false;
         this.target = null;
      }
   }

   public void onDisable() {
      if (mc.player != null && mc.getNetworkHandler() != null && this.originalSlot >= 0 && this.originalSlot <= 8) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.originalSlot));
         mc.player.getInventory().selectedSlot = this.originalSlot;
      }

      this.originalSlot = -1;
      this.target = null;
      this.state = 0;
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (this.groundSpoof.getValue() && mc.player != null) {
         Packet var3 = event.getPacket();
         if (var3 instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket)var3;
            if (packet.isOnGround()) {
               ((IPlayerMoveC2SPacket)packet).setOnGround(false);
            }
         }

      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player != null && mc.world != null) {
         this.target = this.findTarget();
         long currentTime = System.currentTimeMillis();
         if (this.state == 0) {
            if (this.target != null) {
               this.rotateTo(this.target);
            }

            if (this.target == null) {
               return;
            }

            if ((double)(currentTime - this.lastTime) >= this.loopDelay.getValue()) {
               this.launchWindCharge();
               this.state = 1;
               this.flagFly = false;
               this.flagC03 = false;
               this.lastTime = currentTime;
            }
         } else if (this.state == 1) {
            if (!this.flagFly && (double)(currentTime - this.lastTime) >= this.tickUntilFly.getValue()) {
               this.switchToMace();
               this.flagFly = true;
            }

            if (!this.flagC03 && (double)(currentTime - this.lastTime) >= this.c03Delay.getValue()) {
               this.sendLagPacket();
               this.flagC03 = true;
            }

            if ((double)(currentTime - this.lastTime) >= this.attackDelay.getValue()) {
               if (this.target != null && this.target.isAlive() && (double)mc.player.distanceTo(this.target) <= this.attackRange.getValue()) {
                  this.rotateTo(this.target);
                  mc.interactionManager.attackEntity(mc.player, this.target);
                  mc.player.swingHand(Hand.MAIN_HAND, true);
               }

               this.state = 0;
               this.lastTime = currentTime;
            }
         }

      }
   }

   private void launchWindCharge() {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         float yaw = mc.player.getYaw();
         float pitch = 90.0F;
         int windChargeSlot = this.findItem(Items.WIND_CHARGE);
         if (windChargeSlot != -1) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(windChargeSlot));
            mc.player.getInventory().selectedSlot = windChargeSlot;
            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, yaw, pitch));
            mc.player.swingHand(Hand.MAIN_HAND, false);
         }

      }
   }

   private void switchToMace() {
      int maceSlot = this.findItem(Items.MACE);
      if (maceSlot != -1 && mc.player.getInventory().selectedSlot != maceSlot) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
         mc.player.getInventory().selectedSlot = maceSlot;
      }

   }

   private void sendLagPacket() {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + this.lagHeight.getValue(), mc.player.getZ(), false));
      }
   }

   private void rotateTo(PlayerEntity player) {
      Vec3d eyePos = player.getEyePos();
      float[] angles = this.getRotationTo(eyePos);
      RotateMode mode = this.rotateMode.getValue();
      if (mode == _32kMace.RotateMode.Default) {
         mode = _32kMace.RotateMode.OnTick;
      }

      switch (mode.ordinal()) {
         case 1:
         default:
            break;
         case 2:
            Alien.ROTATION.rotationYaw = angles[0];
            Alien.ROTATION.rotationPitch = angles[1];
            break;
         case 3:
            Alien.ROTATION.snapAt(angles[0], angles[1]);
      }

   }

   private float[] getRotationTo(Vec3d vec) {
      Vec3d eye = mc.player.getEyePos();
      double dx = vec.x - eye.x;
      double dy = vec.y - eye.y;
      double dz = vec.z - eye.z;
      double distXZ = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, distXZ)));
      return new float[]{yaw, pitch};
   }

   private int findItem(Item item) {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).isOf(item)) {
            return i;
         }
      }

      return -1;
   }

   private PlayerEntity findTarget() {
      PlayerEntity closest = null;
      double minDist = this.attackRange.getValue();

      for(PlayerEntity player : mc.world.getPlayers()) {
         if (player != mc.player && player.isAlive() && !player.isSpectator() && !Alien.FRIEND.isFriend(player.getName().getString())) {
            double dist = (double)mc.player.distanceTo(player);
            if (dist < minDist) {
               minDist = dist;
               closest = player;
            }
         }
      }

      return closest;
   }

   public static enum RotateMode {
      Default,
      None,
      Smooth,
      OnTick;

      // $FF: synthetic method
      private static RotateMode[] $values() {
         return new RotateMode[]{Default, None, Smooth, OnTick};
      }
   }
}
