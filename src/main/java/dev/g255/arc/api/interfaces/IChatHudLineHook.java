package dev.g255.arc.api.interfaces;

import dev.g255.arc.api.utils.math.FadeUtils;

public interface IChatHudLineHook {
   int arc$getMessageId();

   void arc$setMessageId(int var1);

   boolean arc$getSync();

   void arc$setSync(boolean var1);

   FadeUtils arc$getFade();

   void arc$setFade(FadeUtils var1);
}
