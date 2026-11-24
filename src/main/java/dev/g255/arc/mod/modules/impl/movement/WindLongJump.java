package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.player.MovementUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.client.network.ClientPlayerEntity;

public class WindLongJump extends Module {
   public static WindLongJump INSTANCE;
   private final SliderSetting throwDelay = this.add((new SliderSetting("ThrowDelay", (double)70.0F, (double)0.0F, (double)1000.0F, (double)10.0F)).setSuffix("ms"));
   private final SliderSetting upSpeed = this.add(new SliderSetting("UpSpeed", (double)5.0F, 0.1, (double)10.0F, 0.1));
   private final SliderSetting forwardSpeed = this.add(new SliderSetting("ForwardSpeed", (double)3.0F, (double)0.5F, (double)20.0F, 0.1));
   private final SliderSetting maintainTime = this.add((new SliderSetting("MaintainTime", (double)50.0F, (double)10.0F, (double)500.0F, (double)10.0F)).setSuffix("ms"));
   private final BooleanSetting antiKick = this.add(new BooleanSetting("AntiKick", true));
   private final Timer stopTimer = new Timer();
   private long boostStartTime;
   private boolean thrown;
   private boolean jumped;
   private boolean stopping;

   public WindLongJump() {
      super("WindJump", "Wind charge boost", Module.Category.Movement);
      this.setChinese("风弹高跳");
      INSTANCE = this;
   }

   public void onEnable() {
      if (!nullCheck()) {
         this.thrown = false;
         this.jumped = false;
         this.boostStartTime = 0L;
         this.stopping = false;
         if (!this.throwCharge()) {
            this.disable();
         } else {
            this.thrown = true;
            this.boostStartTime = System.currentTimeMillis();
         }
      }
   }

   public void onDisable() {
      boolean boosting = this.thrown && this.boostStartTime != 0L && System.currentTimeMillis() - this.boostStartTime < (long)this.throwDelay.getValue() + (long)this.maintainTime.getValue();
      this.thrown = false;
      this.jumped = false;
      this.boostStartTime = 0L;
      this.stopping = false;
      if (mc.player != null) {
         MovementUtil.setMotionX((double)0.0F);
         MovementUtil.setMotionZ((double)0.0F);
         if (boosting) {
            MovementUtil.setMotionY((double)0.0F);
         }
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent updateEvent) {
      if (!nullCheck()) {
         if (this.stopping) {
            if (this.antiKick.getValue() && this.stopTimer.passed(500L)) {
               BlockUtil.clickBlock(mc.player.getBlockPos().down(), Direction.DOWN, false, Hand.MAIN_HAND);
               this.disable();
            } else if (!this.antiKick.getValue()) {
               this.disable();
            }

         } else if (this.thrown && this.boostStartTime != 0L) {
            long l = System.currentTimeMillis() - this.boostStartTime;
            long l2 = (long)this.throwDelay.getValue();
            long l3 = l2 + (long)this.maintainTime.getValue();
            if (l >= l2 && l < l3) {
               if (!this.jumped) {
                  if (mc.player.isOnGround()) {
                     mc.player.jump();
                  }

                  MovementUtil.setMotionY(this.upSpeed.getValue());
                  this.jumped = true;
               }

               double d = Math.toRadians((double)Arc.ROTATION.rotationYaw);
               double d2 = this.forwardSpeed.getValue();
               MovementUtil.setMotionX(-Math.sin(d) * d2);
               MovementUtil.setMotionZ(Math.cos(d) * d2);
            } else if (l >= l3) {
               MovementUtil.setMotionX((double)0.0F);
               MovementUtil.setMotionZ((double)0.0F);
               this.stopping = true;
               this.stopTimer.reset();
            }

         }
      }
   }

   private boolean throwCharge() {
      int n2 = -1;
      boolean bl = false;
      ClientPlayerEntity clientPlayerEntity = mc.player;
      if (clientPlayerEntity.getMainHandStack().getItem() == Items.WIND_CHARGE) {
         n2 = clientPlayerEntity.getInventory().selectedSlot;
      } else {
         n2 = InventoryUtil.findItem(Items.WIND_CHARGE);
         if (n2 == -1 && (n2 = InventoryUtil.findItemInventorySlotFromZero(Items.WIND_CHARGE)) != -1) {
            bl = true;
         }
      }

      if (n2 == -1) {
         return false;
      } else {
         float f = Arc.ROTATION.rotationYaw;
         Arc.ROTATION.snapBack();
         Arc.ROTATION.snapAt(f, 90.0F);
         if (clientPlayerEntity.getMainHandStack().getItem() == Items.WIND_CHARGE) {
            sendSequencedPacket((n) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, n, f, 90.0F));
            clientPlayerEntity.swingHand(Hand.MAIN_HAND);
         } else if (bl) {
            InventoryUtil.inventorySwap(n2, clientPlayerEntity.getInventory().selectedSlot);
            sendSequencedPacket((n) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, n, f, 90.0F));
            clientPlayerEntity.swingHand(Hand.MAIN_HAND);
            InventoryUtil.inventorySwap(n2, clientPlayerEntity.getInventory().selectedSlot);
            EntityUtil.syncInventory();
         } else {
            int n3 = clientPlayerEntity.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(n2);
            sendSequencedPacket((n) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, n, f, 90.0F));
            clientPlayerEntity.swingHand(Hand.MAIN_HAND);
            InventoryUtil.switchToSlot(n3);
         }

         Arc.ROTATION.snapBack();
         return true;
      }
   }
}
