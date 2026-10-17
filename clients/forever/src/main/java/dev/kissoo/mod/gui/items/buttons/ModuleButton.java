package dev.kissoo.mod.gui.items.buttons;

import dev.kissoo.api.utils.math.Animation;
import dev.kissoo.api.utils.math.Easing;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.mod.gui.ClickGuiScreen;
import dev.kissoo.mod.gui.items.Item;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import dev.kissoo.mod.modules.settings.Setting;
import dev.kissoo.mod.modules.settings.impl.*;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ModuleButton extends Button {
   private final Module module;
   private List<Item> items = new ArrayList<>();
   public boolean subOpen;
   public double itemHeight;
   public final Animation animation = new Animation();

   // 供 ClickGuiScreen 收集 Shader 矩形区域用
   public float shaderX, shaderY, shaderW, shaderH;
   public boolean needsShader = false;

   public ModuleButton(Module module) {
      super(module.getName());
      this.module = module;
      this.initSettings();
   }

   public final List<Item> getItems() {
      return this.items;
   }

   public void initSettings() {
      ArrayList<Item> newItems = new ArrayList<>();
      for (Setting setting : this.module.getSettings()) {
         if (setting instanceof BooleanSetting s)  newItems.add(new BooleanButton(s));
         if (setting instanceof BindSetting s)     newItems.add(new BindButton(s));
         if (setting instanceof StringSetting s)   newItems.add(new StringButton(s));
         if (setting instanceof SliderSetting s)   newItems.add(new SliderButton(s));
         if (setting instanceof EnumSetting<?> s)  newItems.add(new EnumButton(s));
         if (setting instanceof ColorSetting s)    newItems.add(new PickerButton(s));
      }
      this.items = newItems;
   }

   @Override
   public void update() {
      for (Item item : this.items) {
         item.update();
      }
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      boolean pressed = this.getState();
      ClickGui gui = ClickGui.getInstance();
      Color enableBG = gui.enableBGColor.getValue();

      // ===== 1. 按钮背景 =====
      if (pressed && gui.toggleShader.getValue()) {
         // 【修复】：由于 Shader 已经在底层画好了，这里不需要再画 enableBG 的颜色底板
         // 如果为了防止高亮 Shader 让白字看不清，可以画一层淡淡的黑底 (Alpha=30)
         // 如果不需要可以直接把这行注释掉。
         Render2DUtil.drawRect(context.getMatrices(),
                 this.x, this.y, (float) this.width, this.height - 0.5F,
                 new Color(0, 0, 0, 30).getRGB()
         );
      } else if (pressed) {
         // 没开 Shader 时的常规开启背景
         int bgColor = hovered
                 ? new Color(enableBG.getRed(), enableBG.getGreen(), enableBG.getBlue(),
                 Math.min(255, enableBG.getAlpha() + 40)).getRGB()
                 : enableBG.getRGB();
         Render2DUtil.drawRect(context.getMatrices(),
                 this.x, this.y, (float) this.width, this.height - 0.5F, bgColor);
      } else {
         // 未开启模块时的背景
         int bgColor = hovered ? Button.hoverBGColor : Button.defaultBGColor;
         Render2DUtil.drawRect(context.getMatrices(),
                 this.x, this.y, (float) this.width, this.height - 0.5F, bgColor);
      }

      this.drawString(
              this.module.getDisplayName(),
              this.x + 2.3F,
              this.y - 2.0F - ClickGuiScreen.getInstance().getTextOffset(),
              pressed ? Button.enableTextColor : Button.defaultTextColor
      );

      if (gui.bind.booleanValue && this.module.getBindSetting().getValue() != -1) {
         String bindText = "[" + this.module.getBindSetting().getKeyString() + "]";
         context.getMatrices().push();
         context.getMatrices().scale(0.7F, 0.7F, 1.0F);
         this.drawString(
                 bindText,
                 (this.x + this.width - (gui.gear.booleanValue ? 28.0F : 14.0F)) / 0.7F,
                 (this.y - 1.0F - ClickGuiScreen.getInstance().getTextOffset()) / 0.7F,
                 gui.bind.getValue().getRGB()
         );
         context.getMatrices().pop();
      }

      if (gui.gear.booleanValue) {
         this.drawString(
                 this.subOpen ? "-" : "+",
                 this.x + this.width - 8.0F,
                 this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
                 gui.gear.getValue().getRGB()
         );
      }

      // ===== 5. 子设置展开动画 =====
      int targetH = this.subOpen ? this.getItemHeight() : 0;
      this.itemHeight = this.animation.get(
              (double) targetH,
              gui.length.getValueInt(),
              (Easing) gui.easing.getValue()
      );

      if (this.itemHeight > 0.0) {
         Color lineColor = gui.lineColor.getValue();
         Color bgColor = gui.enableBGColor.getValue();

         Render2DUtil.drawRect(context.getMatrices(),
                 this.x, this.y + this.height, (float) this.width, (float) this.itemHeight, bgColor);
         Render2DUtil.drawRect(context.getMatrices(),
                 this.x, this.y + this.height, 1.0F, (float) this.itemHeight, lineColor);
         Render2DUtil.drawRect(context.getMatrices(),
                 this.x + this.width - 1.0F, this.y + this.height, 1.0F, (float) this.itemHeight, lineColor);
         Render2DUtil.drawRect(context.getMatrices(),
                 this.x, this.y + this.height + (float) this.itemHeight - 1.0F,
                 (float) this.width, 1.0F, lineColor);

         float subY = this.height + 2;
         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.setHeight(this.height);
               item.setLocation(this.x + 1.0F, this.y + subY);
               item.setWidth(this.width - 9);
               if (subY + item.getHeight() <= this.height + this.itemHeight + 2) {
                  item.drawScreen(context, mouseX, mouseY, partialTicks);
               }
               subY += item.getHeight() + 2;
            }
         }
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (!this.items.isEmpty()) {
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.subOpen = !this.subOpen;
            sound();
         }
         if (this.subOpen) {
            for (Item item : this.items) {
               if (!item.isHidden()) {
                  item.mouseClicked(mouseX, mouseY, mouseButton);
               }
            }
         }
      }
   }

   @Override
   public void onKeyTyped(char typedChar, int keyCode) {
      super.onKeyTyped(typedChar, keyCode);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) item.onKeyTyped(typedChar, keyCode);
         }
      }
   }

   @Override
   public void onKeyPressed(int key) {
      super.onKeyPressed(key);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) item.onKeyPressed(key);
         }
      }
   }

   public int getButtonHeight() {
      return super.getHeight();
   }

   public int getItemHeight() {
      int h = 3;
      for (Item item : this.items) {
         if (!item.isHidden()) h += item.getHeight() + 2;
      }
      return h;
   }

   @Override
   public int getHeight() {
      return super.getHeight() + (int) this.itemHeight;
   }

   public Module getModule() {
      return this.module;
   }

   @Override
   public void toggle() {
      this.module.toggle();
   }

   @Override
   public boolean getState() {
      return this.module.isOn();
   }
}