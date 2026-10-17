package dev.luminous.asm.accessors;

import java.util.List;
import net.minecraft.class_338;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({class_338.class})
public interface IChatHud {
   @Mutable
   @Accessor("visibleMessages")
   void setVisibleMessages(List var1);

   @Mutable
   @Accessor("messages")
   void setMessages(List var1);
}
