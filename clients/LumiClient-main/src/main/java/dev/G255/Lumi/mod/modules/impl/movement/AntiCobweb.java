package dev.G255.Lumi.mod.modules.impl.movement;

import dev.G255.Lumi.api.events.eventbus.EventListener;
import dev.G255.Lumi.api.events.impl.ClientTickEvent;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import net.minecraft.util.math.Direction;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class AntiCobweb extends Module {
    public static AntiCobweb INSTANCE;
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 500));
    private final BooleanSetting mineFeet = this.add(new BooleanSetting("MineFeet", true));
    private final BooleanSetting mineHead = this.add(new BooleanSetting("MineHead", false));
    private final Timer mineTimer = new Timer();

    public AntiCobweb() {
        super("AntiCobweb", "Prevents slowdown when walking through cobwebs.", Module.Category.Movement);
        this.setChinese("");
        this.setChineseDesc("");
        INSTANCE = this;
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck() || event.isPost()) return;
        if (!mineTimer.passedMs(delay.getValue())) return;
        BlockPos feetPos = EntityUtil.getPlayerPos(true);
        BlockPos headPos = feetPos.up();
        if (mineFeet.getValue() && isCobweb(feetPos)) {
            mc.interactionManager.attackBlock(feetPos, Direction.UP);
            mineTimer.reset();
        } else if (mineHead.getValue() && isCobweb(headPos)) {
            mc.interactionManager.attackBlock(headPos, Direction.UP);
            mineTimer.reset();
        }
    }

    boolean isCobweb(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB;
    }
}
