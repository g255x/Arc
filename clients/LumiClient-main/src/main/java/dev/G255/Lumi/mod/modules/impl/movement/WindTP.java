package dev.G255.Lumi.mod.modules.impl.movement;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.utils.combat.CombatUtil;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.InventoryUtil;
import dev.G255.Lumi.api.utils.player.MovementUtil;
import dev.G255.Lumi.api.utils.path.TPUtils;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * é£å¼¹ä¼ éï¼ä¸¢åºé£å¼¹æèªå·±å´©èµ·åï¼ä¸æ¬¡æ§ä¼ éå°åæ¹ X æ ¼å¤å®æ´æ¹åé¡¶ä¸ã?
 * é»è¾ä»¿ç§ Fly æ¨¡åç?WindJump æ¨¡å¼ï¼é£å¼¹èµ·è·?+ ç­çç?+ å¨ä½ï¼ï¼
 * åªæ¯æãå¼ºå¶ååå éãæ¢æäºãä¸æ¬?TPãã?
 *
 * é¤å­æ¨¡å¼ï¼MaceModeï¼ï¼å¼å¯ä¸æéæ¿é¤å­æ¶ï¼åæ ·ä¸¢é£å¼¹ï¼ä½ç­çç¸åä¼ éå°
 * æè¿ç©å®¶ï¼TargetRange åï¼å¨å´ X æ ¼åå¾åçå®æ´æ¹åé¡¶ä¸ã?
 */
public class WindTP extends Module {
   public static WindTP INSTANCE;

   // ä¸¢åºé£å¼¹åçç­å¾æ¶é´ (ç­çç?
   private final SliderSetting throwDelay = this.add(new SliderSetting("ThrowDelay", 300.0, 0.0, 2000.0, 10.0).setSuffix(" ms"));
   // æ®éæ¨¡å¼ï¼ååä¼ éçæ ¼æ°ï¼é¤å­æ¨¡å¼ï¼ç®æ å¨å´åå¾
   private final SliderSetting distance = this.add(new SliderSetting("Distance", 5.0, 1.0, 30.0, 1.0));

   // é¤å­æ¨¡å¼ï¼æéæ¿é¤å­å¼å¯æ¶ï¼ä¼ éå°æè¿ç©å®¶éè¿?
   private final BooleanSetting maceMode = this.add(new BooleanSetting("MaceMode", false));
   // æè¿è·ç¦»ï¼æç´¢æè¿ç©å®¶çèå´
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 15.0, 1.0, 50.0, 1.0, () -> this.maceMode.getValue()));

   private long boostStartTime = 0L;
   private boolean hasThrown = false;
   private PlayerEntity maceTarget = null;

   public WindTP() {
      super("WindTP", "Teleport using wind charge packet manipulation.", Module.Category.Movement);
      this.setChinese("");
        this.setChineseDesc("");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return String.valueOf(this.distance.getValueInt());
   }

   @Override
   public void onEnable() {
      if (nullCheck()) return;

      this.hasThrown = false;
      this.boostStartTime = 0L;
      this.maceTarget = null;

      // é¤å­æ¨¡å¼ï¼æéæ¿é¤å­ â?åéå®æè¿ç©å®¶ï¼ç­é£å¼¹çç¸ååä¼ éå°ç®æ éè¿ï¼?
      // æ¾ä¸å°ç®æ ååéå°æ åååä¼ é?
      if (this.maceMode.getValue() && mc.player.getMainHandStack().isOf(Items.MACE)) {
         this.maceTarget = CombatUtil.getClosestEnemy(this.targetRange.getValue());
      }

      // ä¸¢é£å¼¹ï¼ä¸¤ç§æ¨¡å¼é½ä¸¢ï¼ï¼å¦ææ²¡æé£å¼¹åç´æ¥å³é?
      if (!throwWindChargeDown()) {
         this.disable();
         return;
      }

      this.hasThrown = true;
      this.boostStartTime = System.currentTimeMillis();
   }

   @Override
   public void onDisable() {
      this.hasThrown = false;
      this.boostStartTime = 0L;
      this.maceTarget = null;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.world == null || !this.hasThrown || this.boostStartTime == 0L) return;

      // ç­çç¸ï¼ç¶åä¸æ¬¡æ§ä¼ é?
      if (System.currentTimeMillis() - this.boostStartTime >= (long) this.throwDelay.getValue()) {
         if (this.maceTarget != null) {
            if (!this.teleportNearTarget(this.maceTarget)) {
               sendMessage("");
            }
         } else {
            this.teleportForward();
         }
         this.disable();
      }
   }

   /**
    * ä¼ éå°åæ¹ X æ ¼å¤å®æ´æ¹åé¡¶ä¸
    */
   private void teleportForward() {
      if (mc.player == null || mc.world == null) return;

      double dist = this.distance.getValue();
      float yaw = mc.player.getYaw();
      double rad = Math.toRadians(yaw);

      // åæ¹ X æ ¼çæ°´å¹³ç®æ å?
      int tx = MathHelper.floor(mc.player.getX() - Math.sin(rad) * dist);
      int tz = MathHelper.floor(mc.player.getZ() + Math.cos(rad) * dist);

      BlockPos stand = findStandable(tx, tz, mc.player.getBlockY() + 3);
      if (stand != null) teleportToBlockTop(stand);
   }

   /**
    * ä¼ éå°ç®æ ç©å®¶å¨å´ X æ ¼åå¾åçå®æ´æ¹åé¡¶ä¸?
    */
   private boolean teleportNearTarget(PlayerEntity target) {
      if (mc.player == null || mc.world == null || target == null) return false;

      int radius = this.distance.getValueInt();
      BlockPos tPos = target.getBlockPos();
      int fromY = tPos.getY() + 2;

      // ä»ååå¾å¤æ«ï¼æ¾æè¿çå®æ´å®å¿æ¹åï¼æé¤ç®æ èªå·±ç«çé£ä¸æ ¼ï¼
      for (int r = 1; r <= radius; r++) {
         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // åªæ«å½åå?
               BlockPos stand = findStandable(tPos.getX() + dx, tPos.getZ() + dz, fromY);
               if (stand != null) {
                  teleportToBlockTop(stand);
                  return true;
               }
            }
         }
      }
      return false;
   }

   /**
    * ä»?fromY å¾ä¸æ«ï¼æ¾è¯¥åç¬¬ä¸ä¸ªå®æ´å®å¿æ¹åï¼ä¸å¶ä¸æ¹å¯ç«ç«?
    */
   private BlockPos findStandable(int x, int z, int fromY) {
      for (int y = fromY; y >= mc.world.getBottomY(); y--) {
         BlockPos block = new BlockPos(x, y, z);
         if (mc.world.getBlockState(block).isFullCube(mc.world, block)
                 && mc.world.getBlockState(block.up()).isReplaceable()) {
            return block;
         }
      }
      return null;
   }

   /**
    * ä¼ éå°æ¹åé¡¶ä¸å¹¶æ¸ç©ºéåº¦
    */
   private void teleportToBlockTop(BlockPos block) {
      Vec3d dest = new Vec3d(block.getX() + 0.5, block.getY() + 1, block.getZ() + 0.5);
      TPUtils.newTeleport(dest);
      mc.player.setPosition(dest.x, dest.y, dest.z);
      MovementUtil.setMotionX(0);
      MovementUtil.setMotionY(0);
      MovementUtil.setMotionZ(0);
   }

   /**
    * åèä¸ä¸¢å¼é£å¼?(åé¨å¤çäºç©åæ äº¤äºé»è¾) â?å?Fly.throwWindChargeDown()
    */
   private boolean throwWindChargeDown() {
      int windChargeSlot = -1;
      boolean fromInventory = false;

      if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
         windChargeSlot = mc.player.getInventory().selectedSlot;
      } else if ((windChargeSlot = InventoryUtil.findItem(Items.WIND_CHARGE)) != -1) {
         // å¨å¿«æ·æ æ¾å°
      } else if ((windChargeSlot = InventoryUtil.findItemInventorySlotFromZero(Items.WIND_CHARGE)) != -1) {
         fromInventory = true; // å¨èåæ·±å¤æ¾å?
      }

      if (windChargeSlot == -1) return false;

      float yaw = mc.player.getYaw();
      float pitch = 90.0F; // åç´åä¸

      Lumi.ROTATION.snapAt(yaw, pitch);

      if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
      } else if (fromInventory) {
         InventoryUtil.inventorySwap(windChargeSlot, mc.player.getInventory().selectedSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
         InventoryUtil.inventorySwap(windChargeSlot, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
      } else {
         int oldSlot = mc.player.getInventory().selectedSlot;
         InventoryUtil.switchToSlot(windChargeSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         mc.player.swingHand(Hand.MAIN_HAND);
         InventoryUtil.switchToSlot(oldSlot);
      }

      Lumi.ROTATION.snapBack();
      return true;
   }
}
