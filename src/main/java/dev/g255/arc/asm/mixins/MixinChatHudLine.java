package dev.g255.arc.asm.mixins;

import dev.g255.arc.api.interfaces.IChatHudLineHook;
import dev.g255.arc.api.utils.math.FadeUtils;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({ChatHudLine.class})
public class MixinChatHudLine implements IChatHudLineHook {
   @Unique
   private int id = 0;
   @Unique
   private boolean sync = false;
   @Unique
   private FadeUtils fade;

   public int arc$getMessageId() {
      return this.id;
   }

   public void arc$setMessageId(int id) {
      this.id = id;
   }

   public boolean arc$getSync() {
      return this.sync;
   }

   public void arc$setSync(boolean sync) {
      this.sync = sync;
   }

   public FadeUtils arc$getFade() {
      return this.fade;
   }

   public void arc$setFade(FadeUtils fade) {
      this.fade = fade;
   }
}
