package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.PlaceBlockEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.player.PacketMine;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.MathHelper;

public class AntiRegear extends Module {
   public static AntiRegear INSTANCE;
   public final List<BlockPos> safe = new ArrayList();
   private final SliderSetting safeRange = this.add(new SliderSetting("SafeRange", (double)2.0F, (double)0.0F, (double)8.0F, 0.1));
   private final SliderSetting range = this.add(new SliderSetting("Range", (double)5.0F, (double)0.0F, (double)8.0F, 0.1));
   private final BooleanSetting checkSelf = this.add(new BooleanSetting("CheckSelf", true));

   public AntiRegear() {
      super("AntiRegear", Module.Category.Combat);
      this.setChinese("反补给");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (PacketMine.getBreakPos() == null || !(mc.world.getBlockState(PacketMine.getBreakPos()).getBlock() instanceof ShulkerBoxBlock)) {
         this.safe.removeIf((pos) -> !(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock));
         if (this.getBlock() != null) {
            PacketMine.INSTANCE.mine(this.getBlock().getPos());
         }

      }
   }

   @EventListener
   public void onPlace(PlaceBlockEvent event) {
      if (event.block instanceof ShulkerBoxBlock) {
         this.safe.add(event.blockPos);
      }

   }

   private ShulkerBoxBlockEntity getBlock() {
      for(BlockEntity entity : BlockUtil.getTileEntities()) {
         ShulkerBoxBlockEntity shulker;
         if (entity instanceof ShulkerBoxBlockEntity && !((double)MathHelper.sqrt((float)mc.player.squaredDistanceTo((shulker = (ShulkerBoxBlockEntity)entity).getPos().toCenterPos())) <= this.safeRange.getValue()) && (!this.checkSelf.getValue() || !this.safe.contains(shulker.getPos()) && (!shulker.getPos().equals(AutoSupplyPlus.INSTANCE.placePos) || AutoSupplyPlus.INSTANCE.timeoutTimer.passed(100L))) && (double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(shulker.getPos().toCenterPos())) <= this.range.getValue()) {
            return shulker;
         }
      }

      return null;
   }
}
