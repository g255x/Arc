package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_332;

public class ModuleButton extends Button {
   private final Module module;
   private List<Item> items = new ArrayList();
   public boolean subOpen;
   public double itemHeight;
   public final Animation animation = new Animation();

   public ModuleButton(Module module) {
      super(module.getName());
      this.module = module;
      this.initSettings();
   }

   public void initSettings() {
      ArrayList<Item> newItems = new ArrayList();

      for(Setting setting : this.module.getSettings()) {
         if (setting instanceof Setting s) {
            newItems.add(new BooleanButton((BooleanSetting)s));
         }

         if (setting instanceof Setting s) {
            newItems.add(new BindButton((BindSetting)s));
         }

         if (setting instanceof Setting s) {
            newItems.add(new StringButton((StringSetting)s));
         }

         if (setting instanceof Setting s) {
            newItems.add(new SliderButton((SliderSetting)s));
         }

         if (setting instanceof Setting s) {
            newItems.add(new EnumButton((EnumSetting)s));
         }

         if (setting instanceof Setting s) {
            newItems.add(new PickerButton((ColorSetting)s));
         }
      }

      this.items = newItems;
   }

   public void update() {
      for(Item item : this.items) {
         item.update();
      }

   }

   public void drawScreen(class_332 context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      boolean pressed = this.getState();
      Color accent = ClickGui.getInstance().activeColor.getValue();
      Color defaultColor = ClickGui.getInstance().defaultColor.getValue();
      Color hoverColor = ClickGui.getInstance().hoverColor.getValue();
      Color baseFill = pressed ? ColorUtil.injectAlpha(accent, Math.min(230, ClickGui.getInstance().hoverAlpha.getValueInt())) : (hovered ? ColorUtil.injectAlpha(hoverColor, ClickGui.getInstance().hoverAlpha.getValueInt()) : new Color(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), defaultColor.getAlpha()));
      Color border = pressed ? ColorUtil.injectAlpha(accent, 200) : (hovered ? ColorUtil.injectAlpha(accent, 160) : ColorUtil.injectAlpha(hoverColor, 90));
      float h = (float)this.height - 0.5F;
      Render2DUtil.drawRectWithOutline(context.method_51448(), this.x, this.y, (float)this.width, h, baseFill, border);
      if (pressed) {
         float pad = 1.5F;
         Render2DUtil.drawGlow(context.method_51448(), this.x - 1.5F, this.y - 1.5F, (float)this.width + 3.0F, (float)this.height + 3.0F, ColorUtil.injectAlpha(accent.getRGB(), 40));
         Render2DUtil.verticalGradient(context.method_51448(), this.x + pad, this.y + pad, this.x + (float)this.width - pad, this.y + h - pad, new Color(255, 255, 255, 32), new Color(0, 0, 0, 22));
      }

      this.drawString(this.module.getDisplayName(), (double)(this.x + 2.3F), (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()), this.getState() ? enableTextColor : defaultTextColor);
      if (ClickGui.getInstance().gear.booleanValue) {
         this.drawString(this.subOpen ? "-" : "+", (double)(this.x + (float)this.width - 8.0F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset()), ClickGui.getInstance().gear.getValue().getRGB());
      }

      if (this.subOpen || this.itemHeight > (double)0.0F) {
         if (ClickGui.getInstance().line.getValue()) {
            double itemHeight = (double)this.getItemHeight();
            int line = (new Color(220, 224, 230, 160)).getRGB();
            Render2DUtil.drawLine(context.method_51448(), this.x + 0.6F, (float)((double)(this.y + (float)this.height) + itemHeight - (double)0.5F), this.x + 0.6F, this.y + (float)this.height - 0.5F, line);
            Render2DUtil.drawLine(context.method_51448(), this.x + (float)this.width - 0.6F, (float)((double)(this.y + (float)this.height) + itemHeight - (double)0.5F), this.x + (float)this.width - 0.6F, this.y + (float)this.height - 0.5F, line);
            Render2DUtil.drawLine(context.method_51448(), this.x + 0.6F, (float)((double)(this.y + (float)this.height) + itemHeight - (double)0.5F), this.x + (float)this.width - 0.6F, (float)((double)(this.y + (float)this.height) + itemHeight - (double)0.7F), line);
         }

         float height = (float)(this.height + 2);

         for(Item item : this.items) {
            if (!item.isHidden()) {
               item.setHeight(this.height);
               item.setLocation(this.x + 1.0F, this.y + height);
               item.setWidth(this.width - 9);
               item.drawScreen(context, mouseX, mouseY, partialTicks);
               height += (float)(item.getHeight() + 2);
            }
         }
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (!this.items.isEmpty()) {
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.subOpen = !this.subOpen;
            sound();
         }

         if (this.subOpen) {
            for(Item item : this.items) {
               if (!item.isHidden()) {
                  item.mouseClicked(mouseX, mouseY, mouseButton);
               }
            }
         }
      }

   }

   public void onKeyTyped(char typedChar, int keyCode) {
      super.onKeyTyped(typedChar, keyCode);
      if (!this.items.isEmpty() && this.subOpen) {
         for(Item item : this.items) {
            if (!item.isHidden()) {
               item.onKeyTyped(typedChar, keyCode);
            }
         }
      }

   }

   public void onKeyPressed(int key) {
      super.onKeyPressed(key);
      if (!this.items.isEmpty() && this.subOpen) {
         for(Item item : this.items) {
            if (!item.isHidden()) {
               item.onKeyPressed(key);
            }
         }
      }

   }

   public int getButtonHeight() {
      return super.getHeight();
   }

   public int getItemHeight() {
      int height = 3;

      for(Item item : this.items) {
         if (!item.isHidden()) {
            height += item.getHeight() + 2;
         }
      }

      return height;
   }

   public int getHeight() {
      if (this.subOpen) {
         int height = super.getHeight();

         for(Item item : this.items) {
            if (!item.isHidden()) {
               height += item.getHeight() + 1;
            }
         }

         return height + 2;
      } else {
         return super.getHeight();
      }
   }

   public Module getModule() {
      return this.module;
   }

   public void toggle() {
      this.module.toggle();
   }

   public boolean getState() {
      return this.module.isOn();
   }
}
