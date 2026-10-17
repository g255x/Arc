package hoprc.asm.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


@Mixin(targets = "net.minecraft.class_8039$1$1", remap = false)
public class PacketBundleHandlerMixin {
    @ModifyConstant(method = "method_48328", constant = @Constant(intValue = 4096))
    private int bypassBundleLimit(int originalLimit) {
        return Integer.MAX_VALUE;
    }
}