package dev.luminous.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Nina;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.Mod;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.gui.items.buttons.ModuleButton;
import dev.luminous.mod.gui.items.buttons.StringButton;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_3675;
import net.minecraft.class_437;

class BaseClickGuiScreen extends class_437 {
   protected final ArrayList<Component> components = new ArrayList();

   BaseClickGuiScreen() {
      super(class_2561.method_43470("Nina"));
   }

   protected final void load(int startX, int startY) {
      this.components.clear();
      int x = startX;
      int y = startY;

      for(final Module.Category category : Module.Category.values()) {
         this.components.add(new Component(category.toString(), category, x, y, true) {
            public void setupItems() {
               for(Module module : Nina.MODULE.getModules()) {
                  if (module.getCategory().equals(category)) {
                     this.addButton(new ModuleButton(module));
                  }
               }

            }
         });
         x += 94;
      }

      this.components.add(new Component("Search", Module.Category.Client, x, y, true) {
         private String query;

         public void setupItems() {
            this.query = "";
            this.addButton(new StringButton("Search", "", (value) -> {
               this.query = value == null ? "" : value;
               this.applyFilter();
            }));

            for(Module module : Nina.MODULE.getModules()) {
               this.addButton(new ModuleButton(module));
            }

            this.getItems().subList(1, this.getItems().size()).sort(Comparator.comparing(Mod::getName));
            this.applyFilter();
         }

         private void applyFilter() {
            String q = (this.query == null ? "" : this.query).trim().toLowerCase();

            for(Item item : this.getItems()) {
               if (item instanceof ModuleButton moduleButton) {
                  if (q.isEmpty()) {
                     moduleButton.setHidden(true);
                  } else {
                     String name = moduleButton.getModule().getName().toLowerCase();
                     String displayName = moduleButton.getModule().getDisplayName().toLowerCase();
                     moduleButton.setHidden(!name.contains(q) && !displayName.contains(q));
                  }
               }
            }

         }
      });
      this.components.forEach((component) -> {
         if (!component.getName().equals("Search")) {
            component.getItems().sort(Comparator.comparing(Mod::getName));
         }
      });
   }

   private float getSlideY() {
      ClickGui clickGui = ClickGui.getInstance();
      float progress = clickGui.style.getValue() == ClickGui.Style.Shoreline ? (float)clickGui.openValue : (float)clickGui.alphaValue;
      return (1.0F - progress) * 20.0F;
   }

   private float getScale() {
      ClickGui clickGui = ClickGui.getInstance();
      float baseScale = clickGui.scale.getValueFloat();
      if (clickGui.style.getValue() == ClickGui.Style.Shoreline) {
         return baseScale * (float)clickGui.openValue;
      } else {
         float alpha = (float)clickGui.alphaValue;
         float openScale = 0.92F + 0.08F * alpha;
         return openScale * baseScale;
      }
   }

   private int unscaleMouseX(double mouseX) {
      float scale = this.getScale();
      return scale <= 0.0F ? (int)mouseX : (int)Math.round(mouseX / (double)scale);
   }

   private int unscaleMouseY(double mouseY) {
      float scale = this.getScale();
      return scale <= 0.0F ? (int)mouseY : (int)Math.round((mouseY - (double)this.getSlideY()) / (double)scale);
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      float keyCodec = (float)ClickGui.getInstance().alphaValue;
      float scale = this.getScale();
      float slideY = this.getSlideY();
      float invScale = scale <= 0.0F ? 1.0F : 1.0F / scale;
      int scaledMouseX = (int)Math.round((double)mouseX * (double)invScale);
      int scaledMouseY = (int)Math.round(((double)mouseY - (double)slideY) * (double)invScale);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, keyCodec);
      Item.context = context;
      this.method_25420(context, mouseX, mouseY, delta);
      context.method_51448().method_22903();
      context.method_51448().method_46416(0.0F, slideY, 0.0F);
      context.method_51448().method_22905(scale, scale, 1.0F);
      this.components.forEach((components) -> components.drawScreen(context, scaledMouseX, scaledMouseY, delta));
      context.method_51448().method_22909();
      if (ClickGui.getInstance().descriptions.getValue()) {
         String description = null;

         for(Component component : this.components) {
            for(Item item : component.getItems()) {
               if (item instanceof ModuleButton) {
                  ModuleButton moduleButton = (ModuleButton)item;
                  if (!moduleButton.isHidden() && moduleButton.isHovering(scaledMouseX, scaledMouseY)) {
                     Module m = moduleButton.getModule();
                     String d = m.getDescription();
                     if (d != null && !d.isEmpty()) {
                        description = d;
                        break;
                     }

                     description = m.getDisplayName();
                     break;
                  }
               }
            }

            if (description != null) {
               break;
            }
         }

         if (description != null) {
            int pad = 4;
            int textW = ClickGui.getInstance().font.getValue() ? (int)FontManager.ui.getWidth(description) : Wrapper.mc.field_1772.method_1727(description);
            int textH = ClickGui.getInstance().font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
            int tooltipW = textW + pad * 2;
            int tooltipH = textH + pad * 2;
            int tooltipMargin = 4;
            int x = mouseX + 10;
            int y = mouseY + 10;
            int screenW = context.method_51421();
            int screenH = context.method_51443();
            if (x + tooltipW + tooltipMargin > screenW) {
               x = mouseX - 10 - tooltipW;
            }

            if (y + tooltipH + tooltipMargin > screenH) {
               y = mouseY - 10 - tooltipH;
            }

            x = Math.max(tooltipMargin, Math.min(x, screenW - tooltipW - tooltipMargin));
            y = Math.max(tooltipMargin, Math.min(y, screenH - tooltipH - tooltipMargin));
            Render2DUtil.drawRect(context.method_51448(), (float)x, (float)y, (float)tooltipW, (float)tooltipH, (new Color(0, 0, 0, 140)).getRGB());
            if (ClickGui.getInstance().font.getValue()) {
               FontManager.ui.drawString(context.method_51448(), description, (double)(x + pad), (double)(y + pad), -1, ClickGui.getInstance().shadow.getValue());
            } else {
               context.method_51433(Wrapper.mc.field_1772, description, x + pad, y + pad, -1, ClickGui.getInstance().shadow.getValue());
            }
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public boolean method_25402(double mouseX, double mouseY, int clickedButton) {
      int x = this.unscaleMouseX(mouseX);
      int y = this.unscaleMouseY(mouseY);
      this.components.forEach((components) -> components.mouseClicked(x, y, clickedButton));
      return super.method_25402(mouseX, mouseY, clickedButton);
   }

   public boolean method_25406(double mouseX, double mouseY, int releaseButton) {
      int x = this.unscaleMouseX(mouseX);
      int y = this.unscaleMouseY(mouseY);
      this.components.forEach((components) -> components.mouseReleased(x, y, releaseButton));
      return super.method_25406(mouseX, mouseY, releaseButton);
   }

   public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int delta = (int)Math.round(verticalAmount * ClickGui.getInstance().scrollSpeed.getValue());
      if (delta != 0) {
         if (class_3675.method_15987(Wrapper.mc.method_22683().method_4490(), 340)) {
            this.components.forEach((component) -> component.setX(component.getX() + delta));
         } else {
            this.components.forEach((component) -> component.setY(component.getY() + delta));
         }
      }

      return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      this.components.forEach((component) -> component.onKeyPressed(keyCode));
      return super.method_25404(keyCode, scanCode, modifiers);
   }

   public boolean method_25400(char chr, int modifiers) {
      this.components.forEach((component) -> component.onKeyTyped(chr, modifiers));
      return super.method_25400(chr, modifiers);
   }

   public boolean method_25421() {
      return false;
   }

   public final ArrayList<Component> getComponents() {
      return this.components;
   }

   public int getTextOffset() {
      return -ClickGui.getInstance().textOffset.getValueInt() - 6;
   }
}
