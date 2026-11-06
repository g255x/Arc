package dev.g255.arc.asm.accessors;

import net.minecraft.util.Hand;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({HandSwingC2SPacket.class})
public interface IHandSwingC2SPacket {
   @Accessor("hand")
   Hand getHand();
}
