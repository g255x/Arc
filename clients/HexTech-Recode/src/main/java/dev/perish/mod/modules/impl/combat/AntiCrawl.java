package dev.perish.mod.modules.impl.combat;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.api.utils.world.BlockPosX;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.player.SpeedMine;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class AntiCrawl extends Module {
    public static AntiCrawl INSTANCE;
    final double[] xzOffset = new double[]{0.0, 0.3, -0.3};
    private final EnumSetting<AntiCrawl.While> whileSetting = this.add(new EnumSetting("While", AntiCrawl.While.Crawling));
    private final BooleanSetting web = this.add(new BooleanSetting("Web", true));
    public boolean work = false;

    public AntiCrawl() {
        super("AntiCrawl", Module.Category.Combat);
        this.setChinese("反趴下");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.work = false;
        if (!mc.player.isFallFlying()) {
            if (this.whileSetting.is(AntiCrawl.While.Always) && BlockUtil.getBlock(mc.player.getBlockPos()) != Blocks.BEDROCK
                    || mc.player.isCrawling()
                    || this.whileSetting.is(AntiCrawl.While.Mining) && Perish.BREAK.isMining(mc.player.getBlockPos())) {
                for (double offset : this.xzOffset) {
                    for (double offset2 : this.xzOffset) {
                        BlockPos pos = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 1.2, mc.player.getZ() + offset2);
                        if (this.canBreak(pos)) {
                            SpeedMine.INSTANCE.mine(pos);
                            this.work = true;
                            return;
                        }

                        if (this.web.getValue()) {
                            BlockPos web = new BlockPosX(mc.player.getX() + offset, mc.player.getY(), mc.player.getZ() + offset2);
                            if (mc.world.getBlockState(web).getBlock() == Blocks.COBWEB && this.canBreak(web)) {
                                SpeedMine.INSTANCE.mine(web);
                                this.work = true;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean canBreak(BlockPos pos) {
        return (BlockUtil.getClickSideStrict(pos) != null || pos.equals(SpeedMine.getBreakPos())) && !SpeedMine.unbreakable(pos) && !mc.world.isAir(pos);
    }

    private enum While {
        Crawling,
        Mining,
        Always
    }
}
