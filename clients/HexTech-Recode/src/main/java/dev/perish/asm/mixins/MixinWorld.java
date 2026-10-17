package dev.perish.asm.mixins;

import dev.perish.Perish;
import dev.perish.api.utils.Wrapper;
import dev.perish.api.utils.combat.CombatUtil;
import dev.perish.mod.modules.impl.client.ClientSetting;
import dev.perish.mod.modules.impl.combat.SelfTrap;
import dev.perish.mod.modules.impl.player.InteractTweaks;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class MixinWorld {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    public void blockStateHook(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (Wrapper.mc.world != null && Wrapper.mc.world.isInBuildLimit(pos)) {
            if (SelfTrap.airList.contains(pos)) {
                cir.setReturnValue(Blocks.AIR.getDefaultState());
                return;
            }

            // modifyPos 虚拟方块覆盖对所有伤害模式（包括 Mio）均生效，
            // 以确保 canPlaceCrystal 等检查能正确感知 BasePlace 的虚拟黑曜石位置
            BlockPos modifyPos = CombatUtil.modifyPos;
            BlockState modifyBlockState = CombatUtil.modifyBlockState;
            if (modifyPos != null && modifyBlockState != null && pos.equals(modifyPos)) {
                cir.setReturnValue(modifyBlockState);
                return;
            }

            if (ClientSetting.INSTANCE.damageMode.is(ClientSetting.DamageMode.Normal)) {
                boolean terrainIgnore = CombatUtil.terrainIgnore;
                if (terrainIgnore) {
                    WorldChunk worldChunk = Wrapper.mc.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                    BlockState tempState = worldChunk.getBlockState(pos);
                    if (Perish.HOLE.isHard(tempState.getBlock())) {
                        return;
                    }

                    cir.setReturnValue(Blocks.AIR.getDefaultState());
                } else if (InteractTweaks.INSTANCE.isActive) {
                    WorldChunk worldChunkx = Wrapper.mc.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                    BlockState tempStatex = worldChunkx.getBlockState(pos);
                    if (tempStatex.getBlock() == Blocks.BEDROCK) {
                        cir.setReturnValue(Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }
}
