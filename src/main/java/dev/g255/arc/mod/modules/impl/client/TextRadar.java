package dev.g255.arc.mod.modules.impl.client;

import dev.g255.arc.Arc;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.gui.Alignment;
import dev.g255.arc.mod.gui.HudElement;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.util.Formatting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class TextRadar extends Module implements HudElement {
   public static TextRadar INSTANCE;
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final BooleanSetting font = this.add(new BooleanSetting("Font", true));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final SliderSetting x = this.add(new SliderSetting("X", 0, 0, 1500));
   private final SliderSetting y = this.add(new SliderSetting("Y", 100, 0, 1000));
   private final EnumSetting<Side> side;
   private final ColorSetting color;
   private final ColorSetting friend;
   private final BooleanSetting doubleBlank;
   private final BooleanSetting health;
   private final BooleanSetting pops;
   public final BooleanSetting red;
   private final BooleanSetting getDistance;
   private final BooleanSetting effects;
   private int cachedWidth;
   private int cachedHeight;

   public TextRadar() {
      super("TextRadar", Module.Category.HUD);
      this.side = this.add(new EnumSetting("Side", TextRadar.Side.Left));
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
      this.friend = this.add((new ColorSetting("Friend")).injectBoolean(true));
      this.doubleBlank = this.add(new BooleanSetting("Double", false));
      this.health = this.add(new BooleanSetting("Health", true));
      this.pops = this.add(new BooleanSetting("Pops", true));
      this.red = this.add(new BooleanSetting("Red", false));
      this.getDistance = this.add(new BooleanSetting("Distance", true));
      this.effects = this.add(new BooleanSetting("Effects", true));
      this.cachedWidth = 60;
      this.cachedHeight = 10;
      this.setChinese("文字雷达");
      INSTANCE = this;
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (!nullCheck() && !ClickGuiScreen.isEditingHud()) {
         this.renderAt(drawContext, this.getX(), this.getY());
      }
   }

   private void renderAt(DrawContext drawContext, int startX, int startY) {
      if (!nullCheck()) {
         int currentY = startY;
         int widest = 0;
         int lines = 0;
         int lineHeight = this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
         ArrayList<AbstractClientPlayerEntity> players = new ArrayList(mc.world.getPlayers());
         players.sort(Comparator.comparingDouble((player) -> (double)mc.player.distanceTo(player)));

         for(PlayerEntity playerEntity : players) {
            if (playerEntity != mc.player) {
               StringBuilder stringBuilder = new StringBuilder();
               String blank = this.doubleBlank.getValue() ? "  " : " ";
               if (this.health.getValue()) {
                  stringBuilder.append(getHealthColor(playerEntity));
                  stringBuilder.append(this.df.format((double)(playerEntity.getHealth() + playerEntity.getAbsorptionAmount())));
                  stringBuilder.append(blank);
               }

               stringBuilder.append(Formatting.RESET);
               stringBuilder.append(playerEntity.getName().getString());
               if (this.getDistance.getValue()) {
                  stringBuilder.append(blank);
                  stringBuilder.append(Formatting.WHITE);
                  stringBuilder.append(this.df.format((double)mc.player.distanceTo(playerEntity)));
                  stringBuilder.append("m");
               }

               if (this.effects.getValue()) {
                  if (playerEntity.hasStatusEffect(StatusEffects.SLOWNESS)) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.GRAY);
                     stringBuilder.append("Lv.");
                     stringBuilder.append(playerEntity.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);
                     stringBuilder.append(blank);
                     stringBuilder.append(playerEntity.getStatusEffect(StatusEffects.SLOWNESS).getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }

                  if (playerEntity.hasStatusEffect(StatusEffects.SPEED)) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.AQUA);
                     stringBuilder.append("Lv.");
                     stringBuilder.append(playerEntity.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
                     stringBuilder.append(blank);
                     stringBuilder.append(playerEntity.getStatusEffect(StatusEffects.SPEED).getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }

                  if (playerEntity.hasStatusEffect(StatusEffects.STRENGTH)) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.DARK_RED);
                     stringBuilder.append("Lv.");
                     stringBuilder.append(playerEntity.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1);
                     stringBuilder.append(blank);
                     stringBuilder.append(playerEntity.getStatusEffect(StatusEffects.STRENGTH).getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }

                  if (playerEntity.hasStatusEffect(StatusEffects.RESISTANCE)) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.BLUE);
                     stringBuilder.append("Lv.");
                     stringBuilder.append(playerEntity.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
                     stringBuilder.append(blank);
                     stringBuilder.append(playerEntity.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }
               }

               int totemPopped;
               if (this.pops.getValue() && (totemPopped = Arc.POP.getPop(playerEntity)) > 0) {
                  stringBuilder.append(blank);
                  stringBuilder.append(getPopColor(totemPopped));
                  stringBuilder.append("-");
                  stringBuilder.append(totemPopped);
               }

               boolean isFriend;
               if (!(isFriend = Arc.FRIEND.isFriend(playerEntity)) || this.friend.booleanValue) {
                  int color = isFriend ? this.friend.getValue().getRGB() : this.color.getValue().getRGB();
                  if (this.font.getValue()) {
                     FontManager.ui.drawString(drawContext.getMatrices(), stringBuilder.toString(), (double)startX, (double)currentY, color, this.shadow.getValue());
                  } else {
                     drawContext.drawText(mc.textRenderer, stringBuilder.toString(), startX, currentY, color, this.shadow.getValue());
                  }

                  widest = Math.max(widest, this.measure(stringBuilder.toString()));
                  ++lines;
                  currentY += lineHeight;
               }
            }
         }

         if (lines > 0) {
            this.cachedWidth = Math.max(1, widest);
            this.cachedHeight = lines * lineHeight;
         }

      }
   }

   private int measure(String s) {
      String plain = s.replace("§0", "").replace("§1", "").replace("§2", "").replace("§3", "").replace("§4", "").replace("§5", "").replace("§6", "").replace("§7", "").replace("§8", "").replace("§9", "").replace("§a", "").replace("§b", "").replace("§c", "").replace("§d", "").replace("§e", "").replace("§f", "").replace("§k", "").replace("§l", "").replace("§m", "").replace("§n", "").replace("§o", "").replace("§r", "");
      return this.font.getValue() ? (int)FontManager.ui.getWidth(plain) : mc.textRenderer.getWidth(plain);
   }

   public String getDisplayName() {
      return super.getDisplayName();
   }

   public int getX() {
      int base = this.x.getValueInt();
      return this.getSide() == Alignment.RIGHT ? mc.getWindow().getScaledWidth() - base - this.getWidth() : base;
   }

   public void setX(int nx) {
      if (this.getSide() == Alignment.RIGHT) {
         this.x.setValue((double)(mc.getWindow().getScaledWidth() - nx - this.getWidth()));
      } else {
         this.x.setValue((double)nx);
      }

   }

   public int getY() {
      return this.y.getValueInt();
   }

   public void setY(int ny) {
      this.y.setValue((double)ny);
   }

   public int getWidth() {
      return this.cachedWidth;
   }

   public int getHeight() {
      return this.cachedHeight;
   }

   public boolean isVisible() {
      return this.isOn();
   }

   public void render(DrawContext drawContext, float tickDelta) {
      this.renderAt(drawContext, this.getX(), this.getY());
   }

   public Alignment getSide() {
      return this.side.getValue() == TextRadar.Side.Right ? Alignment.RIGHT : Alignment.LEFT;
   }

   public void setAlignment(Alignment alignment) {
      this.side.setValue(alignment == Alignment.RIGHT ? TextRadar.Side.Right : TextRadar.Side.Left);
   }

   public static Formatting getHealthColor(PlayerEntity player) {
      double health = (double)(player.getHealth() + player.getAbsorptionAmount());
      if (health > (double)18.0F) {
         return Formatting.GREEN;
      } else if (health > (double)16.0F) {
         return Formatting.DARK_GREEN;
      } else if (health > (double)12.0F) {
         return Formatting.YELLOW;
      } else if (health > (double)8.0F) {
         return Formatting.GOLD;
      } else {
         return health > (double)4.0F ? Formatting.RED : Formatting.DARK_RED;
      }
   }

   public static Formatting getPopColor(int totems) {
      if (INSTANCE.red.getValue()) {
         return Formatting.RED;
      } else if (totems > 10) {
         return Formatting.DARK_RED;
      } else if (totems > 8) {
         return Formatting.RED;
      } else if (totems > 6) {
         return Formatting.GOLD;
      } else if (totems > 4) {
         return Formatting.YELLOW;
      } else {
         return totems > 2 ? Formatting.DARK_GREEN : Formatting.GREEN;
      }
   }

   public static enum Side {
      Left,
      Right;

      // $FF: synthetic method
      private static Side[] $values() {
         return new Side[]{Left, Right};
      }
   }
}
