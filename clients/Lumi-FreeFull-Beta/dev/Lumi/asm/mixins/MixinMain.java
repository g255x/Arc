package dev.Lumi.asm.mixins;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({Main.class})
public class MixinMain {
   private static final String HEADLESS_KEY = new String(new char[]{'j', 'a', 'v', 'a', '.', 'a', 'w', 't', '.', 'h', 'e', 'a', 'd', 'l', 'e', 's', 's'});
   private static final String HEADLESS_VALUE = new String(new char[]{'f', 'a', 'l', 's', 'e'});

   @Redirect(
      method = {"<clinit>"},
      at = @At(
   value = "INVOKE",
   target = "Ljava/lang/System;setProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
),
      require = 0
   )
   private static String hookStaticInit(String key, String value) {
      return System.setProperty(HEADLESS_KEY, HEADLESS_VALUE);
   }
}
