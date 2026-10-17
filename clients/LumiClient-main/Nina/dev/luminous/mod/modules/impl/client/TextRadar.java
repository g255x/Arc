package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import net.minecraft.class_124;
import net.minecraft.class_1294;
import net.minecraft.class_1657;
import net.minecraft.class_332;
import net.minecraft.class_742;

public class TextRadar extends Module {
   public static TextRadar INSTANCE;
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final BooleanSetting font = this.add(new BooleanSetting("Font", true));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final SliderSetting x = this.add(new SliderSetting("X", 0, 0, 1500));
   private final SliderSetting y = this.add(new SliderSetting("Y", 100, 0, 1000));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   private final ColorSetting friend = this.add((new ColorSetting("Friend")).injectBoolean(true));
   private final BooleanSetting doubleBlank = this.add(new BooleanSetting("Double", false));
   private final BooleanSetting health = this.add(new BooleanSetting("Health", true));
   private final BooleanSetting pops = this.add(new BooleanSetting("Pops", true));
   public final BooleanSetting red = this.add(new BooleanSetting("Red", false));
   private final BooleanSetting getDistance = this.add(new BooleanSetting("Distance", true));
   private final BooleanSetting effects = this.add(new BooleanSetting("Effects", true));

   public TextRadar() {
      super("TextRadar", Module.Category.Client);
      this.setChinese("文字雷达");
      INSTANCE = this;
   }

   public void onRender2D(class_332 drawContext, float tickDelta) {
      int currentY = this.y.getValueInt();
      ArrayList<class_742> players = new ArrayList(mc.field_1687.method_18456());
      players.sort(Comparator.comparingDouble((player) -> (double)mc.field_1724.method_5739(player)));

      for(class_1657 playerEntity : players) {
         if (playerEntity != mc.field_1724) {
            StringBuilder stringBuilder = new StringBuilder();
            String blank = this.doubleBlank.getValue() ? "  " : " ";
            if (this.health.getValue()) {
               stringBuilder.append(getHealthColor(playerEntity));
               stringBuilder.append(this.df.format((double)(playerEntity.method_6032() + playerEntity.method_6067())));
               stringBuilder.append(blank);
            }

            stringBuilder.append(class_124.field_1070);
            stringBuilder.append(playerEntity.method_5477().getString());
            if (this.getDistance.getValue()) {
               stringBuilder.append(blank);
               stringBuilder.append(class_124.field_1068);
               stringBuilder.append(this.df.format((double)mc.field_1724.method_5739(playerEntity)));
               stringBuilder.append("m");
            }

            if (this.effects.getValue()) {
               if (playerEntity.method_6059(class_1294.field_5909)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(class_124.field_1080);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(playerEntity.method_6112(class_1294.field_5909).method_5578() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(playerEntity.method_6112(class_1294.field_5909).method_5584() / 20 + 1);
                  stringBuilder.append("s");
               }

               if (playerEntity.method_6059(class_1294.field_5904)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(class_124.field_1075);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(playerEntity.method_6112(class_1294.field_5904).method_5578() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(playerEntity.method_6112(class_1294.field_5904).method_5584() / 20 + 1);
                  stringBuilder.append("s");
               }

               if (playerEntity.method_6059(class_1294.field_5910)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(class_124.field_1079);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(playerEntity.method_6112(class_1294.field_5910).method_5578() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(playerEntity.method_6112(class_1294.field_5910).method_5584() / 20 + 1);
                  stringBuilder.append("s");
               }

               if (playerEntity.method_6059(class_1294.field_5907)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(class_124.field_1078);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(playerEntity.method_6112(class_1294.field_5907).method_5578() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(playerEntity.method_6112(class_1294.field_5907).method_5584() / 20 + 1);
                  stringBuilder.append("s");
               }
            }

            int totemPopped;
            if (this.pops.getValue() && (totemPopped = Alien.POP.getPop(playerEntity)) > 0) {
               stringBuilder.append(blank);
               stringBuilder.append(getPopColor(totemPopped));
               stringBuilder.append("-");
               stringBuilder.append(totemPopped);
            }

            boolean isFriend;
            if (!(isFriend = Alien.FRIEND.isFriend(playerEntity)) || this.friend.booleanValue) {
               int color = isFriend ? this.friend.getValue().getRGB() : this.color.getValue().getRGB();
               if (this.font.getValue()) {
                  FontManager.ui.drawString(drawContext.method_51448(), stringBuilder.toString(), (double)this.x.getValueInt(), (double)currentY, color, this.shadow.getValue());
               } else {
                  drawContext.method_51433(mc.field_1772, stringBuilder.toString(), this.x.getValueInt(), currentY, color, this.shadow.getValue());
               }

               int n;
               if (this.font.getValue()) {
                  n = (int)FontManager.ui.getFontHeight();
               } else {
                  Objects.requireNonNull(mc.field_1772);
                  n = 9;
               }

               currentY += n;
            }
         }
      }

   }

   public static class_124 getHealthColor(class_1657 player) {
      double health = (double)(player.method_6032() + player.method_6067());
      if (health > (double)18.0F) {
         return class_124.field_1060;
      } else if (health > (double)16.0F) {
         return class_124.field_1077;
      } else if (health > (double)12.0F) {
         return class_124.field_1054;
      } else if (health > (double)8.0F) {
         return class_124.field_1065;
      } else {
         return health > (double)4.0F ? class_124.field_1061 : class_124.field_1079;
      }
   }

   public static class_124 getPopColor(int totems) {
      if (INSTANCE.red.getValue()) {
         return class_124.field_1061;
      } else if (totems > 10) {
         return class_124.field_1079;
      } else if (totems > 8) {
         return class_124.field_1061;
      } else if (totems > 6) {
         return class_124.field_1065;
      } else if (totems > 4) {
         return class_124.field_1054;
      } else {
         return totems > 2 ? class_124.field_1077 : class_124.field_1060;
      }
   }
}
