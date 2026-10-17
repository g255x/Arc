package dev.luminous.mod.gui;

import dev.luminous.api.utils.render.Render2DUtil;
import java.util.Random;
import net.minecraft.class_310;
import net.minecraft.class_332;

public class Snow {
   private int x;
   private int y;
   private int fallingSpeed;
   private int size;

   public Snow(int x, int y, int fallingSpeed, int size) {
      this.x = x;
      this.y = y;
      this.fallingSpeed = fallingSpeed;
      this.size = size;
   }

   public int getX() {
      return this.x;
   }

   public void setX(int x) {
      this.x = x;
   }

   public int getY() {
      return this.y;
   }

   public void setY(int _y) {
      this.y = _y;
   }

   public void drawSnow(class_332 drawContext) {
      Render2DUtil.drawRect(drawContext.method_51448(), (float)this.getX(), (float)this.getY(), (float)this.size, (float)this.size, -1714829883);
      this.setY(this.getY() + this.fallingSpeed);
      if (this.getY() > class_310.method_1551().method_22683().method_4502() + 10 || this.getY() < -10) {
         this.setY(-10);
         Random rand = new Random();
         this.fallingSpeed = rand.nextInt(10) + 1;
         this.size = rand.nextInt(4) + 1;
      }

   }
}
