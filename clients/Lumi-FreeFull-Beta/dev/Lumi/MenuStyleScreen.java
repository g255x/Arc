package dev.Lumi;

import dev.Lumi.core.impl.ConfigManager;
import java.awt.Component;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_442;

public class MenuStyleScreen extends class_437 {
   private final class_437 parent;
   private final String originalStyle;
   private final boolean originalShowBg;
   private final boolean originalMouseShake;
   private final float originalShakeStrength;
   private final boolean originalShakeReverse;
   private final String originalBgPath;
   private static final String[] STYLES = new String[]{"极光", "Alien v4"};
   private static final String[] STYLE_KEYS = new String[]{"aurora", "alien"};

   public MenuStyleScreen(class_437 parent, String originalStyle) {
      super(class_2561.method_43470("主菜单风格"));
      this.parent = parent;
      this.originalStyle = originalStyle != null ? originalStyle : ConfigManager.menuStyle;
      this.originalShowBg = ConfigManager.showMenuBackground;
      this.originalMouseShake = ConfigManager.menuMouseShake;
      this.originalShakeStrength = ConfigManager.menuMouseShakeStrength;
      this.originalShakeReverse = ConfigManager.menuMouseShakeReverse;
      this.originalBgPath = ConfigManager.menuBackgroundPath;
   }

   protected void method_25426() {
      int w = this.field_22789;
      int h = this.field_22790;
      int buttonWidth = 200;
      int buttonHeight = 20;
      int spacing = 5;
      int startY = h / 2 - 80;
      String currentStyle = ConfigManager.menuStyle;

      for(int i = 0; i < STYLES.length; ++i) {
         String styleKey = STYLE_KEYS[i];
         String label = STYLES[i];
         if (styleKey.equals(currentStyle)) {
            label = "§a[✓] " + label;
         } else {
            label = "§7  " + label;
         }

         int y = startY + i * (buttonHeight + spacing);
         this.method_37063(class_4185.method_46430(class_2561.method_43470(label), (btn) -> {
            ConfigManager.menuStyle = styleKey;
            Lumi.save();
            class_310.method_1551().method_1507(new MenuStyleScreen(this.parent, this.originalStyle));
         }).method_46434((w - buttonWidth) / 2, y, buttonWidth, buttonHeight).method_46431());
      }

      int optionsY = startY + STYLES.length * (buttonHeight + spacing) + 10;
      if ("alien".equals(currentStyle)) {
         String currentBg = ConfigManager.menuBackgroundPath;
         String shortName = currentBg;
         int lastSep = Math.max(currentBg.lastIndexOf(47), currentBg.lastIndexOf(92));
         if (lastSep >= 0) {
            shortName = currentBg.substring(lastSep + 1);
         }

         String bgLabel = "§e背景图片: " + shortName;
         this.method_37063(class_4185.method_46430(class_2561.method_43470(bgLabel), (btn) -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("选择背景图片");
            chooser.setFileFilter(new FileNameExtensionFilter("图片文件 (*.png, *.jpg, *.jpeg)", new String[]{"png", "jpg", "jpeg"}));
            int result = chooser.showOpenDialog((Component)null);
            if (result == 0) {
               try {
                  File src = chooser.getSelectedFile();
                  File destDir = new File(class_310.method_1551().field_1697, "lumi");
                  if (!destDir.exists()) {
                     destDir.mkdirs();
                  }

                  long var10003 = System.currentTimeMillis();
                  File dest = new File(destDir, "menu_bg_" + var10003 + getExtension(src.getName()));
                  Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                  ConfigManager.menuBackgroundPath = dest.getAbsolutePath();
                  Lumi.save();
               } catch (Exception var7) {
               }
            }

            class_310.method_1551().method_1507(new MenuStyleScreen(this.parent, this.originalStyle));
         }).method_46434((w - buttonWidth) / 2, optionsY, buttonWidth, buttonHeight).method_46431());
         String resetBgLabel = "§7重置背景图片";
         this.method_37063(class_4185.method_46430(class_2561.method_43470(resetBgLabel), (btn) -> {
            ConfigManager.menuBackgroundPath = "ba.png";
            Lumi.save();
            class_310.method_1551().method_1507(new MenuStyleScreen(this.parent, this.originalStyle));
         }).method_46434((w - buttonWidth) / 2, optionsY + buttonHeight + spacing, buttonWidth, buttonHeight).method_46431());
         optionsY += 2 * (buttonHeight + spacing);
      }

      String bgLabel = ConfigManager.showMenuBackground ? "§a[✓] 背景: 显示" : "§7[✗] 背景: 隐藏";
      this.method_37063(class_4185.method_46430(class_2561.method_43470(bgLabel), (btn) -> {
         ConfigManager.showMenuBackground = !ConfigManager.showMenuBackground;
         Lumi.save();
         class_310.method_1551().method_1507(new MenuStyleScreen(this.parent, this.originalStyle));
      }).method_46434((w - buttonWidth) / 2, optionsY, buttonWidth, buttonHeight).method_46431());
      String shakeLabel = ConfigManager.menuMouseShake ? "§a[✓] 鼠标摇晃: 开启" : "§7[✗] 鼠标摇晃: 关闭";
      this.method_37063(class_4185.method_46430(class_2561.method_43470(shakeLabel), (btn) -> {
         ConfigManager.menuMouseShake = !ConfigManager.menuMouseShake;
         Lumi.save();
         class_310.method_1551().method_1507(new MenuStyleScreen(this.parent, this.originalStyle));
      }).method_46434((w - buttonWidth) / 2, optionsY + buttonHeight + spacing, buttonWidth, buttonHeight).method_46431());
      String var10000 = String.format("%.1f", ConfigManager.menuMouseShakeStrength);
      String strengthLabel = "摇晃幅度: " + var10000;
      this.method_37063(class_4185.method_46430(class_2561.method_43470(strengthLabel), (btn) -> {
         float next = ConfigManager.menuMouseShakeStrength + 0.5F;
         if (next > 5.0F) {
            next = 0.5F;
         }

         ConfigManager.menuMouseShakeStrength = next;
         Lumi.save();
         class_310.method_1551().method_1507(new MenuStyleScreen(this.parent, this.originalStyle));
      }).method_46434((w - buttonWidth) / 2, optionsY + 2 * (buttonHeight + spacing), buttonWidth, buttonHeight).method_46431());
      String revLabel = ConfigManager.menuMouseShakeReverse ? "§a[✓] 反转方向: 开启" : "§7[✗] 反转方向: 关闭";
      this.method_37063(class_4185.method_46430(class_2561.method_43470(revLabel), (btn) -> {
         ConfigManager.menuMouseShakeReverse = !ConfigManager.menuMouseShakeReverse;
         Lumi.save();
         class_310.method_1551().method_1507(new MenuStyleScreen(this.parent, this.originalStyle));
      }).method_46434((w - buttonWidth) / 2, optionsY + 3 * (buttonHeight + spacing), buttonWidth, buttonHeight).method_46431());
      int backY = optionsY + 4 * (buttonHeight + spacing) + 10;
      String blurLabel = ConfigManager.blurScreenBackground ? "§a[✓] 界面模糊: 开启" : "§7[✗] 界面模糊: 关闭";
      this.method_37063(class_4185.method_46430(class_2561.method_43470(blurLabel), (btn) -> {
         ConfigManager.blurScreenBackground = !ConfigManager.blurScreenBackground;
         Lumi.save();
         class_310.method_1551().method_1507(new MenuStyleScreen(this.parent, this.originalStyle));
      }).method_46434((w - buttonWidth) / 2, backY, buttonWidth, buttonHeight).method_46431());
      Object[] var10001 = new Object[]{ConfigManager.blurScreenRadius};
      String blurRadiusLabel = "模糊强度: " + String.format("%.1f", var10001);
      this.method_37063(class_4185.method_46430(class_2561.method_43470(blurRadiusLabel), (btn) -> {
         float next = ConfigManager.blurScreenRadius + 2.0F;
         if (next > 20.0F) {
            next = 2.0F;
         }

         ConfigManager.blurScreenRadius = next;
         Lumi.save();
         class_310.method_1551().method_1507(new MenuStyleScreen(this.parent, this.originalStyle));
      }).method_46434((w - buttonWidth) / 2, backY + buttonHeight + spacing, buttonWidth, buttonHeight).method_46431());
      backY = backY + 2 * (buttonHeight + spacing) + 10;
      this.method_37063(class_4185.method_46430(class_2561.method_43470("§7返回"), (btn) -> {
         if (ConfigManager.menuStyle.equals(this.originalStyle) && ConfigManager.showMenuBackground == this.originalShowBg && ConfigManager.menuMouseShake == this.originalMouseShake && ConfigManager.menuMouseShakeStrength == this.originalShakeStrength && ConfigManager.menuMouseShakeReverse == this.originalShakeReverse && ConfigManager.menuBackgroundPath.equals(this.originalBgPath)) {
            class_310.method_1551().method_1507(this.parent);
         } else {
            class_310.method_1551().method_1507(new class_442());
         }

      }).method_46434((w - 100) / 2, backY, 100, buttonHeight).method_46431());
   }

   public void method_25419() {
      if (this.field_22787 != null) {
         if (ConfigManager.menuStyle.equals(this.originalStyle) && ConfigManager.showMenuBackground == this.originalShowBg && ConfigManager.menuMouseShake == this.originalMouseShake && ConfigManager.menuMouseShakeStrength == this.originalShakeStrength && ConfigManager.menuMouseShakeReverse == this.originalShakeReverse && ConfigManager.menuBackgroundPath.equals(this.originalBgPath)) {
            this.field_22787.method_1507(this.parent);
         } else {
            this.field_22787.method_1507(new class_442());
         }
      }

   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      super.method_25394(context, mouseX, mouseY, delta);
      context.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, 20, 16777215);
   }

   public static String getCurrentStyle() {
      return ConfigManager.menuStyle;
   }

   public static void setMenuStyle(String style) {
      ConfigManager.menuStyle = style;
      Lumi.save();
   }

   private static String getExtension(String name) {
      int dot = name.lastIndexOf(46);
      return dot >= 0 ? name.substring(dot) : ".png";
   }
}
