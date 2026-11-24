package dev.g255.arc.mod.modules.impl.hud;

import dev.g255.arc.mod.gui.Alignment;
import dev.g255.arc.mod.gui.HudElement;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public class PlayerModel extends Module implements HudElement {
   public static PlayerModel INSTANCE;
   private final SliderSetting modelSize = this.add(new SliderSetting("ModelSize", (double)36.0F, (double)5.0F, (double)200.0F, (double)1.0F));
   private final SliderSetting x = this.add(new SliderSetting("X", 560, 0, 1920));
   private final SliderSetting y = this.add(new SliderSetting("Y", 240, 0, 1080));
   private float rotXValue = 0.0F;
   private float rotYValue = 0.0F;

   public PlayerModel() {
      super("PlayerModel", "Render your own player model on screen", Module.Category.HUD);
      this.setChinese("玩家模型");
      INSTANCE = this;
   }

   public int getX() {
      return this.x.getValueInt();
   }

   public void setX(int nx) {
      this.x.setValue((double)nx);
   }

   public int getY() {
      return this.y.getValueInt();
   }

   public void setY(int ny) {
      this.y.setValue((double)ny);
   }

   public int getWidth() {
      return this.modelSize.getValueInt() * 2;
   }

   public int getHeight() {
      return this.modelSize.getValueInt() * 2;
   }

   public boolean isVisible() {
      return this.isOn();
   }

   public void onRender2D(DrawContext context, float tickDelta) {
      if (this.isOn() && mc.player != null && !mc.options.hudHidden) {
         if (!ClickGuiScreen.isEditingHud()) {
            this.renderPlayerModel(context);
         }
      }
   }

   public void render(DrawContext context, float tickDelta) {
      if (mc.player != null && !mc.options.hudHidden) {
         this.renderPlayerModel(context);
      }
   }

   private void renderPlayerModel(DrawContext context) {
      int size = this.modelSize.getValueInt();
      int left = this.getX();
      int top = this.getY();
      int right = left + this.getWidth();
      int bottom = top + this.getHeight();
      float centerX = (float)(left + right) / 2.0F;
      float centerY = (float)(top + bottom) / 2.0F;
      double scaledWidth = (double)mc.getWindow().getScaledWidth();
      double scaledHeight = (double)mc.getWindow().getScaledHeight();
      int windowWidth = mc.getWindow().getWidth();
      int windowHeight = mc.getWindow().getHeight();
      // 窗口最小化时 getWidth/getHeight 为 0，除法会算出 NaN/Infinity，
      // 传给 InventoryScreen.drawEntity 后就会变成 setYaw(NaN) 刷屏
      if (windowWidth <= 0 || windowHeight <= 0) {
         return;
      }

      float targetRotX = (float)((double)centerX - mc.mouse.getX() * scaledWidth / (double)windowWidth);
      float targetRotY = (float)((double)centerY - mc.mouse.getY() * scaledHeight / (double)windowHeight);
      if (!Float.isFinite(targetRotX) || !Float.isFinite(targetRotY)) {
         return;
      }

      this.rotXValue = smooth(this.rotXValue, targetRotX, 0.18F, 0.05F);
      this.rotYValue = smooth(this.rotYValue, targetRotY, 0.18F, 0.05F);
      if (!Float.isFinite(this.rotXValue) || !Float.isFinite(this.rotYValue)) {
         this.rotXValue = 0.0F;
         this.rotYValue = 0.0F;
         return;
      }

      context.getMatrices().push();
      context.getMatrices().translate(0.0F, 0.0F, 1000.0F);
      InventoryScreen.drawEntity(context, left, top, right, bottom, size, 0.0625F, this.rotXValue, this.rotYValue, mc.player);
      context.getMatrices().pop();
   }

   private static float smooth(float current, float target, float step, float threshold) {
      float diff = target - current;
      return Math.abs(diff) < threshold ? target : current + diff * step;
   }

   public Alignment getSide() {
      return Alignment.LEFT;
   }
}
