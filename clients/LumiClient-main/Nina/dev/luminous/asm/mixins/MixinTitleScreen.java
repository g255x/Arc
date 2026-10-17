package dev.luminous.asm.mixins;

import dev.luminous.mod.gui.mainmenu.mainmenu;
import net.minecraft.class_2561;
import net.minecraft.class_437;
import net.minecraft.class_442;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_442.class})
public abstract class MixinTitleScreen extends class_437 {
   protected MixinTitleScreen(class_2561 title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onInit(CallbackInfo ci) {
      if (this.field_22787 != null && this.field_22787.field_1755 instanceof class_442) {
         this.field_22787.method_1507(new mainmenu());
         ci.cancel();
      }

   }
}
