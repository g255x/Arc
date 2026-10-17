package hoprc.asm.accessors;

import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClickableWidget.class)
public interface IClickableWidget {
    @Accessor("alpha")
    float getAlpha();

    @Accessor("active")
    boolean isActive();
}
