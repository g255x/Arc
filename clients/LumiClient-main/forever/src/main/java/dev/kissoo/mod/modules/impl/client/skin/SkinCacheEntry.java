package dev.kissoo.mod.modules.impl.client.skin;

import net.minecraft.util.Identifier;

public record SkinCacheEntry(Identifier identifier, boolean slim, String playerName, long timestamp) {
   private static final long EXPIRE_TIME = 30L * 60L * 1000L;

   public boolean isExpired() {
      return System.currentTimeMillis() - this.timestamp > EXPIRE_TIME;
   }
}
