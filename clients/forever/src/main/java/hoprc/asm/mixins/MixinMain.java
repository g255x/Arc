package hoprc.asm.mixins;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Main.class)
public class MixinMain {

   @Redirect(
           method = "<clinit>",
           at = @At(value = "INVOKE", target = "Ljava/lang/System;setProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
           require = 0
   )
   private static String hookStaticInit(String key, String value) {
      if ("java.awt.headless".equals(key)) {
         return System.setProperty("java.awt.headless", "false");
      }
      return System.setProperty(key, value);
   }
}
