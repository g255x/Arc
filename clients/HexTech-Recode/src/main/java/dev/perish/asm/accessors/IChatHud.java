package dev.perish.asm.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.client.gui.hud.ChatHud;

@Mixin(ChatHud.class)
public interface IChatHud {
    @Mutable
    @Accessor("visibleMessages")
    void setVisibleMessages(List<Visible> var1);

    @Mutable
    @Accessor("messages")
    void setMessages(List<Visible> var1);
}
