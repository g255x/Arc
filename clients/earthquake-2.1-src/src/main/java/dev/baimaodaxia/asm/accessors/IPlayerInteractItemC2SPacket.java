package dev.baimaodaxia.asm.accessors;

import net.minecraft.util.Hand;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PlayerInteractItemC2SPacket.class})
public interface IPlayerInteractItemC2SPacket {
   @Accessor("hand")
   Hand getHand();
}
