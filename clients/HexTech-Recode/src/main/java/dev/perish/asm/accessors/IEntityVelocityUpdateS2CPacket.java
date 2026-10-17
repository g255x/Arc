package dev.perish.asm.accessors;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityVelocityUpdateS2CPacket.class)
public interface IEntityVelocityUpdateS2CPacket {
    @Accessor("velocityX")
    int getX();

    @Mutable
    @Accessor("velocityX")
    void setX(int var1);

    @Accessor("velocityY")
    int getY();

    @Mutable
    @Accessor("velocityY")
    void setY(int var1);

    @Accessor("velocityZ")
    int getZ();

    @Mutable
    @Accessor("velocityZ")
    void setZ(int var1);
}
