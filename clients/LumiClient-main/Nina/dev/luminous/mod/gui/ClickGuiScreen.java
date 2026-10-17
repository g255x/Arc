package dev.luminous.mod.gui;

public class ClickGuiScreen extends BaseClickGuiScreen {
   private static ClickGuiScreen INSTANCE = new ClickGuiScreen();

   public ClickGuiScreen() {
      this.setInstance();
      this.load(10, 4);
   }

   public static ClickGuiScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClickGuiScreen();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public static final class ShorelineScreen extends BaseClickGuiScreen {
      private static ShorelineScreen INSTANCE = new ShorelineScreen();

      public ShorelineScreen() {
         this.setInstance();
         this.load(15, 15);
      }

      public static ShorelineScreen getInstance() {
         if (INSTANCE == null) {
            INSTANCE = new ShorelineScreen();
         }

         return INSTANCE;
      }

      private void setInstance() {
         INSTANCE = this;
      }
   }
}
