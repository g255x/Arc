package dev.baimaodaxia.asm.mixins;

import dev.baimaodaxia.api.interfaces.IChatHudLineHook;
import dev.baimaodaxia.api.utils.math.FadeUtils;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({ChatHudLine.Visible.class})
public class MixinChatHudLineVisible implements IChatHudLineHook {
   @Unique
   private int id = 0;
   @Unique
   private boolean sync = false;
   @Unique
   private FadeUtils fade;

   public int alienClient$getMessageId() {
      return this.id;
   }

   public void alienClient$setMessageId(int id) {
      this.id = id;
   }

   public boolean alienClient$getSync() {
      return this.sync;
   }

   public void alienClient$setSync(boolean sync) {
      this.sync = sync;
   }

   public FadeUtils alienClient$getFade() {
      return this.fade;
   }

   public void alienClient$setFade(FadeUtils fade) {
      this.fade = fade;
   }
}
