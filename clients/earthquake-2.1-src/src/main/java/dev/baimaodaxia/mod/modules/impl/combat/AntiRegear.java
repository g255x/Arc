package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.PlaceBlockEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.player.PacketMine;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.HitResult;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;

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
   public final List<BlockPos> safe;
   private BlockPos openPos;
   private int currentWindowId;
   private int currentSlot;
   private final List<BlockPos> completed;
   private final List<ItemStack> containerItems;

   public AntiRegear() {
      super("AntiRegear", Module.Category.Combat);
      this.mode = this.add(new EnumSetting("Mode", AntiRegear.Mode.MINE));
      this.safeRange = this.add(new SliderSetting("SafeRange", (double)2.0F, (double)0.0F, (double)8.0F, 0.1));
      this.range = this.add(new SliderSetting("Range", (double)5.0F, (double)0.0F, (double)8.0F, 0.1));
      this.checkSelf = this.add(new BooleanSetting("CheckSelf", true));
      this.dropDelay = this.add(new SliderSetting("DropDelay", 40, 0, 200, () -> this.mode.getValue() == AntiRegear.Mode.DROP));
      this.packetAmount = this.add(new SliderSetting("Packets", 1, 1, 27, () -> this.mode.getValue() == AntiRegear.Mode.DROP));
      this.timer = new Timer();
      this.safe = new ArrayList();
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
      if (PacketMine.getBreakPos() == null || !(mc.world.getBlockState(PacketMine.getBreakPos()).getBlock() instanceof ShulkerBoxBlock)) {
         this.cleanupSafe();
         if (this.mode.getValue() == AntiRegear.Mode.DROP) {
            if (!this.shouldPauseForUserOpen()) {
               if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                  this.resetDropState();
               } else if (this.openPos == null && this.currentWindowId == -1) {
                  ShulkerBoxBlockEntity target = this.getBlock();
                  if (target != null) {
                     this.openPos = target.getPos();
                     BlockUtil.clickBlock(this.openPos, BlockUtil.getClickSide(this.openPos), false, false);
                     this.timer.reset();
                  }
               } else {
                  this.tickDrop();
               }
            }
         } else {
            ShulkerBoxBlockEntity target = this.getBlock();
            if (target != null) {
               PacketMine.INSTANCE.mine(target.getPos());
            }

         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      Packet var4 = event.getPacket();
      if (var4 instanceof OpenScreenS2CPacket packet) {
         if (this.mode.getValue() == AntiRegear.Mode.DROP && this.openPos != null) {
            event.cancel();
            this.currentWindowId = packet.getSyncId();
            this.currentSlot = 0;
            this.containerItems.clear();
            this.timer.reset();
         }
      } else {
         var4 = event.getPacket();
         if (var4 instanceof InventoryS2CPacket packet) {
            if (this.mode.getValue() == AntiRegear.Mode.DROP && this.currentWindowId != -1 && packet.getSyncId() == this.currentWindowId) {
               event.cancel();
               this.containerItems.clear();
               List<ItemStack> stacks = packet.getContents();

               for(int i = 0; i < stacks.size() && i < 27; ++i) {
                  this.containerItems.add((ItemStack)stacks.get(i));
               }

               this.timer.reset();
            }
         }
      }

   }

   private boolean shouldPauseForUserOpen() {
      if (!mc.options.useKey.isPressed()) {
         return false;
      } else {
         HitResult var2 = mc.crosshairTarget;
         if (var2 instanceof BlockHitResult) {
            BlockHitResult bhr = (BlockHitResult)var2;
            if (bhr.getType() == HitResult.Type.BLOCK) {
               BlockPos pos = bhr.getBlockPos();
               if (!(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock)) {
                  return false;
               }

               this.safeUntil.put(pos.asLong(), System.currentTimeMillis() + 2500L);
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
      if (event.block instanceof ShulkerBoxBlock) {
         this.safeUntil.put(event.blockPos.asLong(), System.currentTimeMillis() + 250L);
      }

   }

   private void cleanupSafe() {
      long now = System.currentTimeMillis();

      for(long posLong : this.safeUntil.keySet().toLongArray()) {
         if (this.safeUntil.get(posLong) <= now || !(mc.world.getBlockState(BlockPos.fromLong(posLong)).getBlock() instanceof ShulkerBoxBlock)) {
            this.safeUntil.remove(posLong);
         }
      }

   }

   private void tickDrop() {
      if (this.openPos == null) {
         this.currentWindowId = -1;
         this.currentSlot = 0;
         this.containerItems.clear();
      } else if (!(mc.world.getBlockState(this.openPos).getBlock() instanceof ShulkerBoxBlock)) {
         this.resetDropState();
      } else if (this.currentWindowId == -1) {
         if (this.timer.passedMs((double)1500.0F)) {
            this.completed.add(this.openPos);
            this.openPos = null;
            this.timer.reset();
         }

      } else if (this.containerItems.isEmpty()) {
         if (this.timer.passedMs((double)1500.0F)) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(this.currentWindowId));
            this.completed.add(this.openPos);
            this.openPos = null;
            this.currentWindowId = -1;
            this.currentSlot = 0;
            this.containerItems.clear();
            this.timer.reset();
         }

      } else if (this.timer.passedMs((double)((long)this.dropDelay.getValue()))) {
         for(int i = 0; (double)i < this.packetAmount.getValue(); ++i) {
            if (this.currentSlot > 26) {
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(this.currentWindowId));
               this.completed.add(this.openPos);
               this.openPos = null;
               this.currentWindowId = -1;
               this.currentSlot = 0;
               this.containerItems.clear();
               break;
            }

            ItemStack cursor = mc.player.currentScreenHandler.getCursorStack().copy();
            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(this.currentWindowId, 0, this.currentSlot, 1, SlotActionType.THROW, cursor, Int2ObjectMaps.emptyMap()));
            ++this.currentSlot;
         }

         this.timer.reset();
      }
   }

   private ShulkerBoxBlockEntity getBlock() {
      long now = System.currentTimeMillis();

      for(BlockEntity entity : BlockUtil.getTileEntities()) {
         if (entity instanceof ShulkerBoxBlockEntity shulker) {
            BlockPos pos = shulker.getPos();
            double distance = (double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos()));
            if (!(distance > this.range.getValue()) && !(distance <= this.safeRange.getValue()) && (!this.checkSelf.getValue() || this.safeUntil.getOrDefault(pos.asLong(), 0L) <= now && (AutoRegear.INSTANCE == null || AutoRegear.INSTANCE.placePos == null || !pos.equals(AutoRegear.INSTANCE.placePos) || AutoRegear.INSTANCE.timeoutTimer.passed(100L))) && !this.completed.contains(pos)) {
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
