package dev.g255.arc.mod;

import dev.g255.arc.api.utils.Wrapper;

public class Mod implements Wrapper {
   private final String name;

   public Mod(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }
}
