package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PlaceBlockEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_2338;
import net.minecraft.class_239;
import net.minecraft.class_2480;
import net.minecraft.class_2586;
import net.minecraft.class_2596;
import net.minecraft.class_2627;
import net.minecraft.class_2649;
import net.minecraft.class_2813;
import net.minecraft.class_2815;
import net.minecraft.class_3532;
import net.minecraft.class_3944;
import net.minecraft.class_3965;
import net.minecraft.class_239.class_240;

public class AntiRegear extends Module {
   public static AntiRegear INSTANCE;
   private static final long SAFE_TTL_MS = 250L;
   private static final long USER_OPEN_TTL_MS = 2500L;
   private final Long2LongOpenHashMap safeUntil = new Long2LongOpenHashMap();
   private final EnumSetting<Mode> mode;
   private final SliderSetting safeRange;
   private final SliderSetting range;
   private final BooleanSetting checkSelf;
   private final SliderSetting dropDelay;
   private final SliderSetting packetAmount;
   private final Timer timer;
   private class_2338 openPos;
   private int currentWindowId;
   private int currentSlot;
   private final List<class_2338> completed;
   private final List<class_1799> containerItems;

   public AntiRegear() {
      super("AntiRegear", Module.Category.Combat);
      this.mode = this.add(new EnumSetting("Mode", AntiRegear.Mode.MINE));
      this.safeRange = this.add(new SliderSetting("SafeRange", (double)2.0F, (double)0.0F, (double)8.0F, 0.1));
      this.range = this.add(new SliderSetting("Range", (double)5.0F, (double)0.0F, (double)8.0F, 0.1));
      this.checkSelf = this.add(new BooleanSetting("CheckSelf", true));
      this.dropDelay = this.add(new SliderSetting("DropDelay", 40, 0, 200, () -> this.mode.getValue() == AntiRegear.Mode.DROP));
      this.packetAmount = this.add(new SliderSetting("Packets", 1, 1, 27, () -> this.mode.getValue() == AntiRegear.Mode.DROP));
      this.timer = new Timer();
      this.openPos = null;
      this.currentWindowId = -1;
      this.currentSlot = 0;
      this.completed = new ArrayList();
      this.containerItems = new ArrayList();
      this.setChinese("反补给");
      INSTANCE = this;
   }

   public void onEnable() {
      this.openPos = null;
      this.currentWindowId = -1;
      this.currentSlot = 0;
      this.completed.clear();
      this.safeUntil.clear();
      this.containerItems.clear();
      this.timer.reset();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (PacketMine.getBreakPos() == null || !(mc.field_1687.method_8320(PacketMine.getBreakPos()).method_26204() instanceof class_2480)) {
         this.cleanupSafe();
         if (this.mode.getValue() == AntiRegear.Mode.DROP) {
            if (!this.shouldPauseForUserOpen()) {
               if (mc.field_1724.field_7512 != mc.field_1724.field_7498) {
                  this.resetDropState();
               } else if (this.openPos == null && this.currentWindowId == -1) {
                  class_2627 target = this.getBlock();
                  if (target != null) {
                     this.openPos = target.method_11016();
                     BlockUtil.clickBlock(this.openPos, BlockUtil.getClickSide(this.openPos), false, false);
                     this.timer.reset();
                  }
               } else {
                  this.tickDrop();
               }
            }
         } else {
            class_2627 target = this.getBlock();
            if (target != null) {
               PacketMine.INSTANCE.mine(target.method_11016());
            }

         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      class_2596 var4 = event.getPacket();
      if (var4 instanceof class_3944 packet) {
         if (this.mode.getValue() == AntiRegear.Mode.DROP && this.openPos != null) {
            event.cancel();
            this.currentWindowId = packet.method_17592();
            this.currentSlot = 0;
            this.containerItems.clear();
            this.timer.reset();
         }
      } else {
         var4 = event.getPacket();
         if (var4 instanceof class_2649 packet) {
            if (this.mode.getValue() == AntiRegear.Mode.DROP && this.currentWindowId != -1 && packet.method_11440() == this.currentWindowId) {
               event.cancel();
               this.containerItems.clear();
               List<class_1799> stacks = packet.method_11441();

               for(int i = 0; i < stacks.size() && i < 27; ++i) {
                  this.containerItems.add((class_1799)stacks.get(i));
               }

               this.timer.reset();
            }
         }
      }

   }

   private boolean shouldPauseForUserOpen() {
      if (!mc.field_1690.field_1904.method_1434()) {
         return false;
      } else {
         class_239 var2 = mc.field_1765;
         if (var2 instanceof class_3965) {
            class_3965 bhr = (class_3965)var2;
            if (bhr.method_17783() == class_240.field_1332) {
               class_2338 pos = bhr.method_17777();
               if (!(mc.field_1687.method_8320(pos).method_26204() instanceof class_2480)) {
                  return false;
               }

               this.safeUntil.put(pos.method_10063(), System.currentTimeMillis() + 2500L);
               if (this.openPos != null && this.openPos.equals(pos)) {
                  this.resetDropState();
               }

               return true;
            }
         }

         return false;
      }
   }

   private void resetDropState() {
      this.openPos = null;
      this.currentWindowId = -1;
      this.currentSlot = 0;
      this.containerItems.clear();
      this.timer.reset();
   }

   @EventListener
   public void onPlace(PlaceBlockEvent event) {
      if (event.block instanceof class_2480) {
         this.safeUntil.put(event.blockPos.method_10063(), System.currentTimeMillis() + 250L);
      }

   }

   private void cleanupSafe() {
      long now = System.currentTimeMillis();

      for(long posLong : this.safeUntil.keySet().toLongArray()) {
         if (this.safeUntil.get(posLong) <= now || !(mc.field_1687.method_8320(class_2338.method_10092(posLong)).method_26204() instanceof class_2480)) {
            this.safeUntil.remove(posLong);
         }
      }

   }

   private void tickDrop() {
      if (this.openPos == null) {
         this.currentWindowId = -1;
         this.currentSlot = 0;
         this.containerItems.clear();
      } else if (this.currentWindowId == -1) {
         if (this.timer.passedMs((double)1500.0F)) {
            this.openPos = null;
            this.timer.reset();
         }

      } else if (this.containerItems.isEmpty()) {
         if (this.timer.passedMs((double)1500.0F)) {
            mc.method_1562().method_52787(new class_2815(this.currentWindowId));
            this.openPos = null;
            this.currentWindowId = -1;
            this.currentSlot = 0;
            this.timer.reset();
         }

      } else if (this.timer.passedMs((double)((long)this.dropDelay.getValue()))) {
         for(int i = 0; (double)i < this.packetAmount.getValue(); ++i) {
            if (this.currentSlot > 26) {
               mc.method_1562().method_52787(new class_2815(this.currentWindowId));
               this.completed.add(this.openPos);
               this.openPos = null;
               this.currentWindowId = -1;
               this.currentSlot = 0;
               this.containerItems.clear();
               break;
            }

            class_1799 cursor = mc.field_1724.field_7512.method_34255().method_7972();
            mc.method_1562().method_52787(new class_2813(this.currentWindowId, 0, this.currentSlot, 1, class_1713.field_7795, cursor, Int2ObjectMaps.emptyMap()));
            ++this.currentSlot;
         }

         this.timer.reset();
      }
   }

   private class_2627 getBlock() {
      long now = System.currentTimeMillis();

      for(class_2586 entity : BlockUtil.getTileEntities()) {
         if (entity instanceof class_2627 shulker) {
            class_2338 pos = shulker.method_11016();
            double distance = (double)class_3532.method_15355((float)mc.field_1724.method_5707(pos.method_46558()));
            if (!(distance > this.range.getValue()) && !(distance <= this.safeRange.getValue()) && (!this.checkSelf.getValue() || this.safeUntil.getOrDefault(pos.method_10063(), 0L) <= now && (AutoRegear.INSTANCE == null || AutoRegear.INSTANCE.placePos == null || !pos.equals(AutoRegear.INSTANCE.placePos) || AutoRegear.INSTANCE.timeoutTimer.passed(100L))) && !this.completed.contains(pos)) {
               return shulker;
            }
         }
      }

      return null;
   }

   private static enum Mode {
      MINE,
      DROP;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{MINE, DROP};
      }
   }
}
