package dev.baimaodaxia.api.events.impl;

import net.minecraft.entity.Entity;

public class RemoveEntityEvent {
   public static final RemoveEntityEvent instance = new RemoveEntityEvent();
   private Entity entity;
   private Entity.RemovalReason removalReason;

   private RemoveEntityEvent() {
   }

   public static RemoveEntityEvent get(Entity entity, Entity.RemovalReason removalReason) {
      instance.entity = entity;
      instance.removalReason = removalReason;
      return instance;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public Entity.RemovalReason getRemovalReason() {
      return this.removalReason;
   }
}
