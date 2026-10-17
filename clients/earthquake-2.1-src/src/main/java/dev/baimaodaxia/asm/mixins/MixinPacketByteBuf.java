package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.mod.modules.impl.exploit.AntiPacket;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

@Mixin({PacketByteBuf.class})
public abstract class MixinPacketByteBuf {
   @ModifyArg(
      method = {"readNbt(Lio/netty/buffer/ByteBuf;)Lnet/minecraft/nbt/NbtCompound;"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/network/PacketByteBuf;readNbt(Lio/netty/buffer/ByteBuf;Lnet/minecraft/nbt/NbtSizeTracker;)Lnet/minecraft/nbt/NbtElement;"
)
   )
   private static NbtSizeTracker xlPackets(NbtSizeTracker sizeTracker) {
      return AntiPacket.INSTANCE.isOn() && AntiPacket.INSTANCE.decode.getValue() ? NbtSizeTracker.ofUnlimitedBytes() : sizeTracker;
   }
}
