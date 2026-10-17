package dev.baimaodaxia.mod.gui.items.buttons;

import dev.baimaodaxia.api.utils.math.Animation;
import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.mod.gui.ClickGuiScreen;
import dev.baimaodaxia.mod.gui.items.Item;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.ClickGui;
import dev.baimaodaxia.mod.modules.settings.Setting;
import dev.baimaodaxia.mod.modules.settings.impl.BindSetting;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import dev.baimaodaxia.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
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
         if (setting instanceof BooleanSetting s) {
            newItems.add(new BooleanButton(s));
         }

         if (setting instanceof BindSetting s2) {
            newItems.add(new BindButton(s2));
         }

         if (setting instanceof StringSetting s3) {
            newItems.add(new StringButton(s3));
         }

         if (setting instanceof SliderSetting s4) {
            newItems.add(new SliderButton(s4));
         }

         if (setting instanceof EnumSetting<?> s5) {
            newItems.add(new EnumButton(s5));
         }

         if (setting instanceof ColorSetting s6) {
            newItems.add(new PickerButton(s6));
         }
      }

      this.items = newItems;
   }

   public void update() {
      for(Item item : this.items) {
         item.update();
      }

   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      String displayName = this.module.getDisplayName();
      double d = (double)(this.x + 2.3F);
      double textOffset = (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset(hovered));
      int rgb;
      int rgb2;
      if (this.getState()) {
         Color accent = ClickGui.getInstance().bgEnable.getValue();
         Color outlineColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), accent.getAlpha());
         Render2DUtil.drawRect(context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height - 0.5F, outlineColor);
         rgb = ColorUtil.injectAlpha(ClickGui.getInstance().enableTextColor.getValue(), ClickGui.getInstance().enableTextColor.getValue().getAlpha()).getRGB();
         rgb2 = ColorUtil.injectAlpha(ClickGui.getInstance().bgColor.getValue(), ClickGui.getInstance().bgEnable.getValue().getAlpha() - 100).getRGB();
      } else {
         Render2DUtil.drawRect(context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height - 0.5F, 0);
         rgb = ClickGui.getInstance().textColor.getValue().getRGB();
         rgb2 = 0;
      }

      this.drawString(displayName, d, textOffset, rgb);
      MatrixStack MatrixStackVargetMatrices = context.getMatrices();
      float f = this.x;
      float f2 = this.y;
      float f3 = this.x + (float)this.width;
      float f4 = this.y + (float)this.height - 0.5F;
      Render2DUtil.rect(MatrixStackVargetMatrices, f, f2, f3, f4, rgb2);
      if (ClickGui.getInstance().gear.getValue()) {
         this.drawString(this.subOpen ? "-" : "+", (double)(this.x + (float)this.width - 8.0F), (double)(this.y - 1.7F - (float)ClickGuiScreen.getInstance().getTextOffset(hovered)), -1);
      }

      if (this.subOpen || this.itemHeight > (double)0.0F) {
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
