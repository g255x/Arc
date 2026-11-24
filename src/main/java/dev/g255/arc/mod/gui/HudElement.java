package dev.g255.arc.mod.gui;

import net.minecraft.client.gui.DrawContext;

public interface HudElement {
   String getDisplayName();

   int getX();

   void setX(int var1);

   int getY();

   void setY(int var1);

   int getWidth();

   int getHeight();

   default int getRenderHeight() {
      return this.getHeight();
   }

   boolean isVisible();

   void render(DrawContext var1, float var2);

   default boolean isEditable() {
      return true;
   }

   default Alignment getSide() {
      return Alignment.LEFT;
   }

   default void setAlignment(Alignment side) {
   }

   default int getRightX() {
      return this.getX() + this.getWidth();
   }

   default void setRightX(int rightX) {
      this.setX(rightX - this.getWidth());
   }
}
