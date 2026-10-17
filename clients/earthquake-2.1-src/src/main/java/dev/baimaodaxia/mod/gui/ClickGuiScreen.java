package dev.baimaodaxia.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.mod.gui.items.Component;
import dev.baimaodaxia.mod.gui.items.Item;
import dev.baimaodaxia.mod.gui.items.buttons.ModuleButton;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.ClickGui;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ClickGuiScreen extends Screen {
   private static ClickGuiScreen INSTANCE = new ClickGuiScreen();
   private final ArrayList<Component> components = new ArrayList();

   public ClickGuiScreen() {
      super(Text.literal("earthquake"));
      this.setInstance();
      this.load();
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

   private void load() {
      int x = -84;

      for(final Module.Category category : Module.Category.values()) {
         String var10004 = category.toString();
         x += 94;
         this.components.add(new Component(var10004, category, x, 4, true) {
            public void setupItems() {
               for(Module module : earthquake.MODULE.getModules()) {
                  if (module.getCategory().equals(category)) {
                     this.addButton(new ModuleButton(module));
                  }
               }

            }
         });
      }

      this.components.forEach((components) -> components.getItems().sort(Comparator.comparing((v0) -> v0.getName())));
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      float a = (float)ClickGui.getInstance().alphaValue;
      float scale = 0.92F + 0.08F * a;
      float slideY = (1.0F - a) * 20.0F;
      if (ClickGui.getInstance().blur.getValue()) {
         earthquake.BLUR.applyFullScreenBlur(1.0F + (ClickGui.getInstance().radius.getValueFloat() - 1.0F) * a);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
      Item.context = context;
      context.getMatrices().push();
      context.getMatrices().translate(0.0F, slideY, 0.0F);
      context.getMatrices().scale(scale, scale, 1.0F);
      this.components.forEach((components) -> components.drawScreen(context, mouseX, mouseY, delta));
      context.getMatrices().pop();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int clickedButton) {
      this.components.forEach((components) -> components.mouseClicked((int)mouseX, (int)mouseY, clickedButton));
      return super.mouseClicked(mouseX, mouseY, clickedButton);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int releaseButton) {
      this.components.forEach((components) -> components.mouseReleased((int)mouseX, (int)mouseY, releaseButton));
      return super.mouseReleased(mouseX, mouseY, releaseButton);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (InputUtil.isKeyPressed(Wrapper.mc.getWindow().getHandle(), 340)) {
         if (verticalAmount < (double)0.0F) {
            this.components.forEach((component) -> component.setX(component.getX() - 15));
         } else if (verticalAmount > (double)0.0F) {
            this.components.forEach((component2) -> component2.setX(component2.getX() + 15));
         }
      } else if (verticalAmount < (double)0.0F) {
         this.components.forEach((component3) -> component3.setY(component3.getY() - 15));
      } else if (verticalAmount > (double)0.0F) {
         this.components.forEach((component4) -> component4.setY(component4.getY() + 15));
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.components.forEach((component) -> component.onKeyPressed(keyCode));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean charTyped(char chr, int modifiers) {
      this.components.forEach((component) -> component.onKeyTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }

   public boolean shouldPause() {
      return false;
   }

   public final ArrayList<Component> getComponents() {
      return this.components;
   }

   public int getTextOffset() {
      return -ClickGui.getInstance().textOffset.getValueInt() - 6;
   }

   public int getTextOffset(boolean isHovering) {
      return isHovering ? -ClickGui.getInstance().textOffset.getValueInt() - 5 : -ClickGui.getInstance().textOffset.getValueInt() - 6;
   }
}
