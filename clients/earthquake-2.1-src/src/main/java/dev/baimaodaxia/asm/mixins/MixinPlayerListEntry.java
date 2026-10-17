package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.mod.modules.impl.client.Cape;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerListEntry.class})
public abstract class MixinPlayerListEntry {
   @Inject(
      method = {"getSkinTextures"},
      at = {@At("TAIL")},
      cancellable = true
   )
   private void getSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
      PlayerListEntry playerListEntry = (PlayerListEntry)(Object)this;
      PlayerEntity localPlayer = Wrapper.mc.player;
      if (localPlayer != null && playerListEntry.getProfile().getId().equals(localPlayer.getGameProfile().getId()) && Cape.INSTANCE.isOn()) {
         SkinTextures oldTextures = (SkinTextures)cir.getReturnValue();
         Cape.Capes capeType = Cape.INSTANCE.cape.getValue();
         Identifier capeTexture = Cape.CAPES_MAP.get(capeType);
         if (capeTexture != null) {
            SkinTextures newTextures = new SkinTextures(oldTextures.texture(), oldTextures.textureUrl(), capeTexture, capeTexture, oldTextures.model(), oldTextures.secure());
            cir.setReturnValue(newTextures);
         }
      }

   }
}
