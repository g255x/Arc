package hoprc.asm.mixins.spearfix;

import dev.kissoo.mod.spearfix.SpearFix;
import hoprc.api.interfaces.SpearStabPacketMarker;
import hoprc.obf.mixin.MixinObfuscate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MixinObfuscate
@Mixin(MinecraftClient.class)
public class SpearAttackInputMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void spearfix$onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.currentScreen != null) return;
        if (mc.getNetworkHandler() == null) return;

        ItemStack mainHand = player.getMainHandStack();
        if (!SpearFix.isRemappedSpear(mainHand)) return;
        if (player.isUsingItem()) return;

        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() == HitResult.Type.MISS) {
            PlayerActionC2SPacket packet = new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ORIGIN,
                    Direction.DOWN
            );
            ((SpearStabPacketMarker) packet).spearfix$markAsStab();
            mc.getNetworkHandler().sendPacket(packet);
            SpearFix.startStabAnimation(player, mainHand);
            cir.setReturnValue(false);
        }
    }
}
