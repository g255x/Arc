package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.api.utils.render.Render3DUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.client.util.math.MatrixStack;

public class BreakerEsp extends Module {
    private final BooleanSetting burrow = add(new BooleanSetting("Burrow", true));
    private final BooleanSetting surround = add(new BooleanSetting("Surround", true));

    private final ColorSetting burrowBox = add(new ColorSetting("BurrowBox", new Color(255, 0, 0, 255)).injectBoolean(true));
    private final ColorSetting burrowFill = add(new ColorSetting("BurrowFill", new Color(255, 0, 0, 100)).injectBoolean(true));

    private final ColorSetting surroundBox = add(new ColorSetting("SurroundBox", new Color(0, 0, 255, 255)).injectBoolean(true));
    private final ColorSetting surroundFill = add(new ColorSetting("SurroundFill", new Color(0, 0, 255, 100)).injectBoolean(true));

    public BreakerEsp() {
        super("BreakerEsp", Category.Render);
        setChinese("防爆渲染");
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (mc.world == null || mc.player == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.isSpectator() || player.isDead() || player.getHealth() <= 0) continue;

            BlockPos playerPos = player.getBlockPos();

            // Burrow
            if (burrow.getValue()) {
                Block block = mc.world.getBlockState(playerPos).getBlock();
                if (isHardBlock(block)) {
                    renderBlock(matrixStack, playerPos, burrowBox, burrowFill);
                }
            }

            // Surround
            if (surround.getValue()) {
                BlockPos[] offsets = new BlockPos[]{
                        playerPos.north(),
                        playerPos.south(),
                        playerPos.east(),
                        playerPos.west()
                };

                for (BlockPos pos : offsets) {
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (isHardBlock(block)) {
                        renderBlock(matrixStack, pos, surroundBox, surroundFill);
                    }
                }
            }
        }
    }

    private boolean isHardBlock(Block block) {
        return block == Blocks.OBSIDIAN;
    }

    private void renderBlock(MatrixStack matrixStack, BlockPos pos, ColorSetting boxSetting, ColorSetting fillSetting) {
        Box box = new Box(pos);
        if (fillSetting.booleanValue) {
            Render3DUtil.drawFill(matrixStack, box, fillSetting.getValue());
        }
        if (boxSetting.booleanValue) {
            Render3DUtil.drawBox(matrixStack, box, boxSetting.getValue());
        }
    }
}
