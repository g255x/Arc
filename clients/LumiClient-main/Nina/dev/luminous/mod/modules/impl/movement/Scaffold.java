package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.SendMovementPacketsEvent;
import dev.luminous.api.events.impl.SprintEvent;
import dev.luminous.api.utils.move.MoveUtils;
import dev.luminous.api.utils.rotation.RayCastUtil;
import dev.luminous.api.utils.rotation.Rotation;
import dev.luminous.api.utils.rotation.RotationManager;
import dev.luminous.api.utils.rotation.RotationUtils;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2261;
import net.minecraft.class_2302;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2356;
import net.minecraft.class_243;
import net.minecraft.class_2482;
import net.minecraft.class_2680;
import net.minecraft.class_3532;
import net.minecraft.class_3675;
import net.minecraft.class_3965;
import net.minecraft.class_4771;

public class Scaffold extends Module {
   public static final List<class_2248> blacklistedBlocks;
   private final BooleanSetting telly = this.add(new BooleanSetting("Telly", true));
   private final BooleanSetting snap = this.add(new BooleanSetting("Snap", false, () -> !this.telly.getValue()));
   private final SliderSetting rotateSpeed = this.add(new SliderSetting("Rotation Speed", (double)180.0F, (double)1.0F, (double)180.0F, (double)1.0F));
   private final SliderSetting rotateBackSpeed;
   private final SliderSetting tellyTick;
   private final BooleanSetting safeWalk;
   private int airTick;
   private int yLevel;
   private class_2338 blockPos;
   private class_2350 enumFacing;
   private int oldSlot;

   public Scaffold() {
      super("Scaffold", "Automatically places blocks under you", Module.Category.Movement);
      BooleanSetting var10009 = this.telly;
      Objects.requireNonNull(var10009);
      this.rotateBackSpeed = this.add(new SliderSetting("Rotation Back Speed", (double)180.0F, (double)1.0F, (double)180.0F, (double)1.0F, var10009::getValue));
      var10009 = this.telly;
      Objects.requireNonNull(var10009);
      this.tellyTick = this.add(new SliderSetting("Telly Ticks", (double)1.0F, (double)0.0F, (double)6.0F, (double)1.0F, var10009::getValue));
      this.safeWalk = this.add(new BooleanSetting("SafeWalk", true));
      this.oldSlot = -1;
      this.setChinese("自动搭路");
   }

   public String getInfo() {
      if (this.telly.getValue()) {
         return "Telly";
      } else {
         return this.snap.getValue() ? "Snap" : "Normal";
      }
   }

   public static class_243 getVec3(class_2338 pos, class_2350 face) {
      double x = (double)pos.method_10263() + (double)0.5F;
      double y = (double)pos.method_10264() + (double)0.5F;
      double z = (double)pos.method_10260() + (double)0.5F;
      if (face != class_2350.field_11036 && face != class_2350.field_11033) {
         y += 0.08;
      } else {
         x += getRandomDoubleInRange(0.3, -0.3);
         z += getRandomDoubleInRange(0.3, -0.3);
      }

      if (face == class_2350.field_11039 || face == class_2350.field_11034) {
         z += getRandomDoubleInRange(0.3, -0.3);
      }

      if (face == class_2350.field_11035 || face == class_2350.field_11043) {
         x += getRandomDoubleInRange(0.3, -0.3);
      }

      return new class_243(x, y, z);
   }

   private static double getRandomDoubleInRange(double min, double max) {
      return min + (max - min) * ThreadLocalRandom.current().nextDouble();
   }

   public static boolean isValidStack(class_1799 stack) {
      if (stack != null && stack.method_7909() instanceof class_1747 && stack.method_7947() > 0) {
         String string = stack.method_7964().getString();
         if (!string.contains("Click") && !string.contains("点击")) {
            class_2248 block = ((class_1747)stack.method_7909()).method_7711();
            if (block instanceof class_2356) {
               return false;
            } else if (block instanceof class_2261) {
               return false;
            } else if (block instanceof class_4771) {
               return false;
            } else if (block instanceof class_2302) {
               return false;
            } else {
               return !(block instanceof class_2482) && !blacklistedBlocks.contains(block);
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (mc.field_1724 != null && mc.field_1687 != null && event.isPre()) {
         int slotID = -1;

         for(int i = 0; i < 9; ++i) {
            class_1799 stack = mc.field_1724.method_31548().method_5438(i);
            if (isValidStack(stack)) {
               slotID = i;
               break;
            }
         }

         if (slotID != -1 && mc.field_1724.method_31548().field_7545 != slotID) {
            mc.field_1724.method_31548().field_7545 = slotID;
         }

         if (mc.field_1724.method_24828()) {
            this.yLevel = (int)Math.floor(mc.field_1724.method_23318()) - 1;
         }

         this.getBlockInfo();
         if (this.telly.getValue()) {
            if (mc.field_1724.method_24828()) {
               this.airTick = 0;
               this.blockPos = null;
               this.enumFacing = null;
               Rotation rotation = new Rotation(mc.field_1724.method_36454(), mc.field_1724.method_36455());
               RotationManager.setRotations(rotation, this.rotateBackSpeed.getValue());
            } else {
               if ((double)this.airTick >= this.tellyTick.getValue()) {
                  Rotation rotation = this.getRotation(this.blockPos, this.enumFacing);
                  RotationManager.setRotations(rotation, this.rotateSpeed.getValue());
                  this.place();
               }

               ++this.airTick;
            }
         } else {
            if (this.blockPos == null) {
               RotationManager.setRotations(new Rotation(class_3532.method_15393(mc.field_1724.method_36454() - 180.0F), 89.64F), this.rotateSpeed.getValue());
            } else if (this.onAir() || !this.snap.getValue()) {
               Rotation rotation = this.getRotation(this.blockPos, this.enumFacing);
               RotationManager.setRotations(rotation, this.rotateSpeed.getValue());
            }

            this.place();
         }

      }
   }

   @EventListener(
      priority = -200
   )
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (mc.field_1724 != null && mc.field_1687 != null) {
         if (this.telly.getValue() && MoveUtils.isMoving() && !mc.field_1724.method_5715()) {
            mc.field_1724.method_5728(true);
         }

         if (mc.field_1724.method_24828() && !mc.field_1690.field_1903.method_1434() && MoveUtils.isMoving() && this.telly.getValue()) {
            mc.field_1724.field_3913.field_3904 = true;
         }

      }
   }

   @EventListener(
      priority = -100
   )
   public void onSprint(SprintEvent event) {
      if (this.telly.getValue() && MoveUtils.isMoving() && !mc.field_1724.method_5715() && !mc.field_1724.method_6115()) {
         event.setSprint(true);
         event.cancel();
      }

   }

   public void place() {
      if (this.onAir() && this.blockPos != null && this.enumFacing != null) {
         boolean hasRotated = RayCastUtil.overBlock(RotationManager.getRotation(), this.enumFacing, this.blockPos, false);
         if (hasRotated && mc.field_1761.method_2896(mc.field_1724, class_1268.field_5808, new class_3965(getVec3(this.blockPos, this.enumFacing), this.enumFacing, this.blockPos, false)) == class_1269.field_5812) {
            mc.field_1724.method_6104(class_1268.field_5808);
         }

      }
   }

   public int getYLevel() {
      return !mc.field_1690.field_1903.method_1434() && MoveUtils.isMoving() && (double)mc.field_1724.field_6017 <= (double)0.25F && this.telly.getValue() ? this.yLevel : (int)Math.floor(mc.field_1724.method_23318()) - 1;
   }

   public void getBlockInfo() {
      class_243 baseVec = mc.field_1724.method_33571();
      class_2338 base = new class_2338(class_3532.method_15357(baseVec.field_1352), this.getYLevel(), class_3532.method_15357(baseVec.field_1350));
      int baseX = base.method_10263();
      int baseZ = base.method_10260();
      if (!this.isSolidAndNonInteractive(mc.field_1687.method_8320(base), mc.field_1687, base)) {
         if (!this.checkBlock(baseVec, base)) {
            for(int d = 1; d <= 6; ++d) {
               if (this.checkBlock(baseVec, new class_2338(baseX, this.getYLevel() - d, baseZ))) {
                  return;
               }

               for(int x = 0; x <= d; ++x) {
                  for(int z = 0; z <= d - x; ++z) {
                     int y = d - x - z;

                     for(int rev1 = 0; rev1 <= 1; ++rev1) {
                        for(int rev2 = 0; rev2 <= 1; ++rev2) {
                           if (this.checkBlock(baseVec, new class_2338(baseX + (rev1 == 0 ? x : -x), this.getYLevel() - y, baseZ + (rev2 == 0 ? z : -z)))) {
                              return;
                           }
                        }
                     }
                  }
               }
            }

         }
      }
   }

   public boolean isSolidAndNonInteractive(class_2680 state, class_1937 level, class_2338 pos) {
      boolean hasCollision = !state.method_26220(level, pos).method_1110();
      boolean hasNoMenu = state.method_26196(level, pos) == null;
      return hasCollision && hasNoMenu;
   }

   private boolean checkBlock(class_243 baseVec, class_2338 pos) {
      class_2680 state = mc.field_1687.method_8320(pos);
      if (!state.method_45474()) {
         return false;
      } else if (pos.method_10264() > this.getYLevel()) {
         return false;
      } else {
         class_243 center = new class_243((double)pos.method_10263() + (double)0.5F, (double)pos.method_10264() + (double)0.5F, (double)pos.method_10260() + (double)0.5F);

         for(class_2350 dir : class_2350.values()) {
            class_243 hit = center.method_1031((double)dir.method_10148() * (double)0.5F, (double)dir.method_10164() * (double)0.5F, (double)dir.method_10165() * (double)0.5F);
            class_2338 baseBlockPos = pos.method_10093(dir);
            if (this.isSolidAndNonInteractive(mc.field_1687.method_8320(baseBlockPos), mc.field_1687, baseBlockPos)) {
               class_243 relevant = hit.method_1020(baseVec);
               if (relevant.method_1027() <= (double)20.25F && relevant.method_1026(new class_243((double)dir.method_10148(), (double)dir.method_10164(), (double)dir.method_10165())) >= (double)0.0F && (dir.method_10153() != class_2350.field_11036 || !MoveUtils.isMoving() || mc.field_1690.field_1903.method_1434())) {
                  this.blockPos = baseBlockPos;
                  this.enumFacing = dir.method_10153();
                  return true;
               }
            }
         }

         return false;
      }
   }

   @EventListener
   public void onPacket(SendMovementPacketsEvent e) {
      if (mc.field_1724 != null && mc.field_1687 != null) {
         if (this.safeWalk.getValue() && !this.telly.getValue()) {
            boolean isHoldingShift = class_3675.method_15987(mc.method_22683().method_4490(), mc.field_1690.field_1832.method_1429().method_1444());
            boolean shouldSafewalk = mc.field_1724.method_24828() && this.isOnBlockEdge(0.3);
            mc.field_1690.field_1832.method_23481(isHoldingShift || shouldSafewalk);
         }

      }
   }

   private boolean isOnBlockEdge(double sensitivity) {
      return !mc.field_1687.method_20812(mc.field_1724, mc.field_1724.method_5829().method_989((double)0.0F, (double)-0.5F, (double)0.0F).method_1009(-sensitivity, (double)0.0F, -sensitivity)).iterator().hasNext();
   }

   public void onEnable() {
      RotationManager.init();
      if (mc.field_1724 != null) {
         this.oldSlot = mc.field_1724.method_31548().field_7545;
      }

      this.airTick = 0;
      this.blockPos = null;
      this.enumFacing = null;
   }

   public void onDisable() {
      boolean isHoldingShift = class_3675.method_15987(mc.method_22683().method_4490(), mc.field_1690.field_1832.method_1429().method_1444());
      mc.field_1690.field_1832.method_23481(isHoldingShift);
      if (mc.field_1724 != null && this.oldSlot != -1) {
         mc.field_1724.method_31548().field_7545 = this.oldSlot;
      }

   }

   public Rotation getRotation(class_2338 pos, class_2350 direction) {
      if (pos == null) {
         return new Rotation(mc.field_1724.method_36454(), mc.field_1724.method_36455());
      } else {
         Rotation rotations = this.onAir() ? RotationUtils.calculate(pos, direction) : RotationUtils.calculate(pos.method_46558());
         Rotation reverseYaw = new Rotation(class_3532.method_15393(mc.field_1724.method_36454() - 180.0F), rotations.getPitch());
         class_2350 rayFacing = direction == null ? class_2350.field_11036 : direction;
         boolean hasRotated = RayCastUtil.overBlock(reverseYaw, rayFacing, pos, false);
         return hasRotated ? reverseYaw : rotations;
      }
   }

   private boolean onAir() {
      class_243 baseVec = mc.field_1724.method_33571();
      class_2338 base = new class_2338(class_3532.method_15357(baseVec.field_1352), this.getYLevel(), class_3532.method_15357(baseVec.field_1350));
      return mc.field_1687.method_8320(base).method_45474();
   }

   public String getArrayInfo() {
      return this.telly.getValue() ? "Telly" : (this.snap.getValue() ? "Snap" : "Normal");
   }

   static {
      blacklistedBlocks = Arrays.asList(class_2246.field_10124, class_2246.field_10382, class_2246.field_10164, class_2246.field_10485, class_2246.field_10285, class_2246.field_10285, class_2246.field_10576, class_2246.field_10477, class_2246.field_10418, class_2246.field_10442, class_2246.field_10013, class_2246.field_10034, class_2246.field_10380, class_2246.field_10336, class_2246.field_10535, class_2246.field_10380, class_2246.field_10179, class_2246.field_10223, class_2246.field_10375, class_2246.field_10571, class_2246.field_10212, class_2246.field_10090, class_2246.field_10158, class_2246.field_10224, class_2246.field_10582, class_2246.field_10494, class_2246.field_10363, class_2246.field_10214, class_2246.field_10589, class_2246.field_10348, class_2246.field_10167, class_2246.field_9995, class_2246.field_10559, class_2246.field_10251, class_2246.field_10597, class_2246.field_10583, class_2246.field_9983, class_2246.field_10181, class_2246.field_10102, class_2246.field_10029, class_2246.field_10200, class_2246.field_10228, class_2246.field_9980, class_2246.field_10343, class_2246.field_46282, class_2246.field_10625, class_2246.field_10620, class_2246.field_10523, class_2246.field_10495);
   }
}
