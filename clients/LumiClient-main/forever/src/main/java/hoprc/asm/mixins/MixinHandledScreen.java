package hoprc.asm.mixins;

import hoprc.api.handler.HandledScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
   @Unique
   private static final ItemStack[] ITEMS = new ItemStack[27];
   @Shadow
   @Nullable
   protected Slot focusedSlot;
   @Shadow
   protected int x;
   @Shadow
   protected int y;

   protected MixinHandledScreen(Text title) {
      super(title);
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (button == 2 && HandledScreenHandler.onMouseClicked(this.focusedSlot, ITEMS, this.client)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (HandledScreenHandler.onRenderHead(this)) ci.cancel();
   }

   @Inject(method = "render", at = @At("RETURN"))
   private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      HandledScreenHandler.onRenderReturn(context, mouseX, mouseY, this.focusedSlot, this.client);
   }

   @Shadow
   public abstract void renderBackground(DrawContext var1, int var2, int var3, float var4);
}
