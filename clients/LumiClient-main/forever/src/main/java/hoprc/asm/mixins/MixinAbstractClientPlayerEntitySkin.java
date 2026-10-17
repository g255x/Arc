package hoprc.asm.mixins;

import dev.kissoo.mod.modules.impl.client.Skin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntitySkin {
   @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
   private void getSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
      Skin skin = Skin.INSTANCE;
      if (skin == null || !skin.isOn()) {
         return;
      }

      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player == null || !player.equals(this)) {
         return;
      }

      SkinTextures original = cir.getReturnValue();
      Identifier skinTexture = skin.hasSkin() ? skin.getSkinTexture() : original.texture();
      Identifier capeTexture = skin.hasCape() ? skin.getCapeTexture() : original.capeTexture();
      Identifier elytraTexture = skin.hasCape() ? skin.getCapeTexture() : original.elytraTexture();
      SkinTextures.Model model = skin.hasSkin() ? skin.getModel() : original.model();
      cir.setReturnValue(new SkinTextures(skinTexture, original.textureUrl(), capeTexture, elytraTexture, model, original.secure()));
   }
}
