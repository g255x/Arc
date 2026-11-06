package dev.g255.arc.api.interfaces;

import net.minecraft.text.Text;

public interface IChatHudHook {
   void arc$addMessage(Text var1, int var2);

   void arc$addMessage(Text var1);

   void arc$addMessageOutSync(Text var1, int var2);
}
