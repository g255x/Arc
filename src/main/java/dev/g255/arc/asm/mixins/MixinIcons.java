package dev.g255.arc.asm.mixins;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.InputSupplier;
import net.minecraft.client.util.Icons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Icons.class})
public class MixinIcons {
   @Unique
   private static final String[] ARC_ICONS = new String[]{"arc_16x16.png", "arc_32x32.png", "arc_48x48.png", "arc_128x128.png", "arc_256x256.png"};
   @Unique
   private static final String ARC_MAC_ICON = "arc_256x256.png";

   @Inject(
      method = {"getIcons"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void arc$getIcons(ResourcePack pack, CallbackInfoReturnable<List<InputSupplier<InputStream>>> cir) {
      List<InputSupplier<InputStream>> list = new ArrayList();

      for(String name : ARC_ICONS) {
         InputSupplier<InputStream> supplier = arc$supplier(name);
         if (supplier != null) {
            list.add(supplier);
         }
      }

      if (!list.isEmpty()) {
         cir.setReturnValue(list);
      }

   }

   @Inject(
      method = {"getMacIcon"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void arc$getMacIcon(ResourcePack pack, CallbackInfoReturnable<InputSupplier<InputStream>> cir) {
      InputSupplier<InputStream> supplier = arc$supplier("arc_256x256.png");
      if (supplier != null) {
         cir.setReturnValue(supplier);
      }

   }

   @Unique
   private static InputSupplier<InputStream> arc$supplier(String name) {
      try {
         InputStream in = MixinIcons.class.getResourceAsStream("/assets/arc/icon/" + name);
         if (in == null) {
            return null;
         } else {
            byte[] bytes;
            try {
               bytes = in.readAllBytes();
            } finally {
               in.close();
            }

            return () -> new ByteArrayInputStream(bytes);
         }
      } catch (Exception var7) {
         return null;
      }
   }
}
