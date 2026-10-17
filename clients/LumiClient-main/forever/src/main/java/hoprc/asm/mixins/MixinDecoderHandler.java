package hoprc.asm.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.handler.DecoderHandler;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(DecoderHandler.class)
public abstract class MixinDecoderHandler {

    @WrapMethod(method = "decode")
    private void wrapDecode(
            ChannelHandlerContext ctx,
            ByteBuf buf,
            List<Object> out,
            Operation<Void> original
    ) throws Exception {

        try {
            original.call(ctx, buf, out);
        } catch (Throwable t) {
            if (containsRegistryError(t)) {
                System.out.println("[Lyrae] Blocked bad packet decode.");
                return;
            }
            throw t;
        }
    }

    private boolean containsRegistryError(Throwable t) {
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null &&
                    (msg.contains("minecraft:lunge")
                            || msg.contains("Failed to get element")
                            || msg.contains("Failed to decode packet"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}