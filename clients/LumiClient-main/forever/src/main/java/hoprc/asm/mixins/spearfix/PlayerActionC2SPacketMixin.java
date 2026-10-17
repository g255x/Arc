package hoprc.asm.mixins.spearfix;

import hoprc.api.interfaces.SpearStabPacketMarker;
import hoprc.obf.mixin.MixinObfuscate;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@MixinObfuscate
@Mixin(PlayerActionC2SPacket.class)
public class PlayerActionC2SPacketMixin implements SpearStabPacketMarker {
    @Unique
    private boolean spearfix$stab;

    @Override
    public void spearfix$markAsStab() {
        this.spearfix$stab = true;
    }

    @Override
    public boolean spearfix$isStab() {
        return this.spearfix$stab;
    }

    @Redirect(
            method = "write",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;writeEnumConstant(Ljava/lang/Enum;)Lnet/minecraft/network/PacketByteBuf;")
    )
    private PacketByteBuf spearfix$writeStabActionId(PacketByteBuf buffer, Enum<?> action) {
        if (this.spearfix$stab) {
            return buffer.writeVarInt(7);
        }
        return buffer.writeEnumConstant(action);
    }
}
