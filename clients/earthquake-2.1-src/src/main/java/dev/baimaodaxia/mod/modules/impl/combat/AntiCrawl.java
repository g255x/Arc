package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.world.BlockPosX;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.player.PacketMine;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class AntiCrawl extends Module {
   public static AntiCrawl INSTANCE;
   final double[] xzOffset = new double[]{(double)0.0F, 0.3, -0.3};
   private final EnumSetting<While> whileSetting;
   private final BooleanSetting web;
   public boolean work;

   public AntiCrawl() {
      super("AntiCrawl", Module.Category.Combat);
      this.whileSetting = this.add(new EnumSetting("While", AntiCrawl.While.Crawling));
      this.web = this.add(new BooleanSetting("Web", true));
      this.work = false;
      this.setChinese("反趴下");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.work = false;
      if (!mc.player.isFallFlying()) {
         if (this.whileSetting.is(AntiCrawl.While.Always) && BlockUtil.getBlock(mc.player.getBlockPos()) != Blocks.BEDROCK || mc.player.isCrawling() || this.whileSetting.is(AntiCrawl.While.Mining) && Alien.BREAK.isMining(mc.player.getBlockPos())) {
            for(double offset : this.xzOffset) {
               for(double offset2 : this.xzOffset) {
                  BlockPosX pos = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 1.2, mc.player.getZ() + offset2);
                  if (this.canBreak(pos)) {
                     PacketMine.INSTANCE.mine(pos);
                     this.work = true;
                     return;
                  }

                  BlockPosX web;
                  if (this.web.getValue() && mc.world.getBlockState(web = new BlockPosX(mc.player.getX() + offset, mc.player.getY(), mc.player.getZ() + offset2)).getBlock() == Blocks.COBWEB && this.canBreak(web)) {
                     PacketMine.INSTANCE.mine(web);
                     this.work = true;
                     return;
                  }
               }
            }
         }

      }
   }

   private boolean canBreak(BlockPos pos) {
      return (BlockUtil.getClickSideStrict(pos) != null || pos.equals(PacketMine.getBreakPos())) && !PacketMine.unbreakable(pos) && !mc.world.isAir(pos);
   }

   private static enum While {
      Crawling,
      Mining,
      Always;

      // $FF: synthetic method
      private static While[] $values() {
         return new While[]{Crawling, Mining, Always};
      }
   }
}
