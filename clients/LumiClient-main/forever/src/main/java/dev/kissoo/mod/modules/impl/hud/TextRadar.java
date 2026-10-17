package dev.kissoo.mod.modules.impl.hud;

import dev.kissoo.Forever;
import dev.kissoo.chat.ChatClient;
import dev.kissoo.core.impl.CommandManager;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class TextRadar extends Module {
   public static TextRadar INSTANCE;
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final BooleanSetting font = this.add(new BooleanSetting("Font", true));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final SliderSetting x = this.add(new SliderSetting("X", 0, 0, 1500));
   private final SliderSetting y = this.add(new SliderSetting("Y", 100, 0, 1000));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   private final ColorSetting friend = this.add(new ColorSetting("Friend").injectBoolean(true));
   private final BooleanSetting doubleBlank = this.add(new BooleanSetting("Double", false));
   private final BooleanSetting health = this.add(new BooleanSetting("Health", true));
   private final BooleanSetting pops = this.add(new BooleanSetting("Pops", true));
   public final BooleanSetting red = this.add(new BooleanSetting("Red", false));
   private final BooleanSetting distance = this.add(new BooleanSetting("Distance", true));
   private final BooleanSetting effects = this.add(new BooleanSetting("Effects", true));
   private final BooleanSetting armorOnly = this.add(new BooleanSetting("ArmorOnly", false));
   private final BooleanSetting ircCoords  = this.add(new BooleanSetting("IRCCoords",  false, () -> ChatClient.isAdmin()));

   public TextRadar() {
      super("TextRadar", Module.Category.HUD);
      this.setChinese("文字雷达");
      INSTANCE = this;
      ircCoords.injectTask(() -> {
         if (ircCoords.getValue() && !ChatClient.isAdmin()) {
            ircCoords.setValueWithoutTask(false);
            CommandManager.sendMessage("§c[TextRadar] 只有 Admin 才能开启 IRCCoords");
         }
      });
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      int currentY = this.y.getValueInt();
      List<AbstractClientPlayerEntity> players = new ArrayList(mc.world.getPlayers());
      players.sort(Comparator.comparingDouble(playerx -> mc.player.distanceTo(playerx)));

      for (PlayerEntity player : players) {
         if (player != mc.player) {
            // 盔甲模式：只显示有护甲的玩家
            if (this.armorOnly.getValue() && !hasArmor(player)) continue;
            StringBuilder stringBuilder = new StringBuilder();
            String blank = this.doubleBlank.getValue() ? "  " : " ";
            if (this.health.getValue()) {
               stringBuilder.append(getHealthColor(player));
               stringBuilder.append(this.df.format(player.getHealth() + player.getAbsorptionAmount()));
               stringBuilder.append(blank);
            }

            stringBuilder.append(Formatting.RESET);
            stringBuilder.append(player.getName().getString());
            if (this.distance.getValue()) {
               stringBuilder.append(blank);
               stringBuilder.append(Formatting.WHITE);
               stringBuilder.append(this.df.format(mc.player.distanceTo(player)));
               stringBuilder.append("m");
            }

            if (this.effects.getValue()) {
               if (player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(Formatting.GRAY);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(player.getStatusEffect(StatusEffects.SLOWNESS).getDuration() / 20 + 1);
                  stringBuilder.append("s");
               }

               if (player.hasStatusEffect(StatusEffects.SPEED)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(Formatting.AQUA);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(player.getStatusEffect(StatusEffects.SPEED).getDuration() / 20 + 1);
                  stringBuilder.append("s");
               }

               if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(Formatting.DARK_RED);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(player.getStatusEffect(StatusEffects.STRENGTH).getDuration() / 20 + 1);
                  stringBuilder.append("s");
               }

               if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                  stringBuilder.append(blank);
                  stringBuilder.append(Formatting.BLUE);
                  stringBuilder.append("Lv.");
                  stringBuilder.append(player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
                  stringBuilder.append(blank);
                  stringBuilder.append(player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
                  stringBuilder.append("s");
               }
            }

            if (this.pops.getValue()) {
               int totemPopped = Forever.POP.getPop(player);
               if (totemPopped > 0) {
                  stringBuilder.append(blank);
                  stringBuilder.append(getPopColor(totemPopped));
                  stringBuilder.append("-");
                  stringBuilder.append(totemPopped);
               }
            }

            boolean isFriend = Forever.FRIEND.isFriend(player);
            if (!isFriend || this.friend.booleanValue) {
               int color = isFriend ? this.friend.getValue().getRGB() : this.color.getValue().getRGB();
               if (this.font.getValue()) {
                  FontManager.ui.drawString(drawContext.getMatrices(), stringBuilder.toString(), this.x.getValueInt(), currentY, color, this.shadow.getValue());
               } else {
                  drawContext.drawText(mc.textRenderer, stringBuilder.toString(), this.x.getValueInt(), currentY, color, this.shadow.getValue());
               }

               currentY += this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
            }
         }
      }

      if (this.ircCoords.getValue() && ChatClient.isAdmin()) {
         currentY += this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
         String header = "§d--- IRC Users §7[§b" + currentDimName() + "§7]§d ---";
         if (this.font.getValue()) {
            FontManager.ui.drawString(drawContext.getMatrices(), header, this.x.getValueInt(), currentY, 0xFFFF55FF, this.shadow.getValue());
         } else {
            drawContext.drawText(mc.textRenderer, header, this.x.getValueInt(), currentY, 0xFFFF55FF, this.shadow.getValue());
         }
         currentY += this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;

         Map<String, ChatClient.IrcUserInfo> allIrc = ChatClient.getIrcUserInfo();
         Set<String> rendered = new HashSet<>();

         for (PlayerEntity player : players) {
            if (player == mc.player) continue;
            String mcName = player.getGameProfile().getName();
            if (!allIrc.containsKey(mcName)) continue;
            rendered.add(mcName);

            ChatClient.IrcUserInfo info = allIrc.get(mcName);
            String label = info.ircName().isEmpty() ? mcName : info.ircName();
            String clientLabel = info.client().isEmpty() ? "" : "\u00a7d[" + info.client() + "] ";
            String hiddenTag = info.isHidden() ? "§8[隐]§r" : "";
            String ircColor = ChatClient.isIrcAdmin(mcName) ? ChatClient.IRC_ADMIN_COLOR_CODE : "§a";
            String line = hiddenTag + clientLabel + ircColor + label + " §7XYZ §f"
                    + (int) player.getX() + " " + (int) player.getY() + " " + (int) player.getZ()
                    + " §7[§b" + currentDimName() + "§7]"
                    + " §7(§f" + this.df.format(mc.player.distanceTo(player)) + "m§7)";

            if (this.font.getValue()) {
               FontManager.ui.drawString(drawContext.getMatrices(), line, this.x.getValueInt(), currentY, 0xFFFF55FF, this.shadow.getValue());
            } else {
               drawContext.drawText(mc.textRenderer, line, this.x.getValueInt(), currentY, 0xFFFF55FF, this.shadow.getValue());
            }
            currentY += this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
         }

         for (var entry : allIrc.entrySet()) {
            if (rendered.contains(entry.getKey())) continue;
            ChatClient.IrcUserInfo info = entry.getValue();
            String label = info.ircName().isEmpty() ? entry.getKey() : info.ircName();
            String clientLabel = info.client().isEmpty() ? "" : "\u00a7d[" + info.client() + "] ";
            String hiddenTag = info.isHidden() ? "§8[隐]§r" : "";
            String dimTag = info.dim().isEmpty() ? "" : " §7[§b" + dimDisplayName(info.dim()) + "§7]";
            String ircColor = ChatClient.isIrcAdmin(entry.getKey()) ? ChatClient.IRC_ADMIN_COLOR_CODE : ChatClient.IRC_USER_COLOR_CODE;
            String line = hiddenTag + clientLabel + ircColor + label + " §7XYZ §f"
                    + info.x() + " " + info.y() + " " + info.z()
                    + dimTag + " §7(§c远程§7)";

            if (this.font.getValue()) {
               FontManager.ui.drawString(drawContext.getMatrices(), line, this.x.getValueInt(), currentY, 0xFFFF55FF, this.shadow.getValue());
            } else {
               drawContext.drawText(mc.textRenderer, line, this.x.getValueInt(), currentY, 0xFFFF55FF, this.shadow.getValue());
            }
            currentY += this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
         }
      }
   }

   /** Chinese display name for the local player's current dimension. */
   private String currentDimName() {
      if (mc.world == null) return "未知";
      return dimDisplayName(mc.world.getRegistryKey().getValue().getPath());
   }

   /** Convert a dimension registry path (e.g. "overworld") to a Chinese display name. */
   private static String dimDisplayName(String path) {
      if (path == null || path.isEmpty()) return "未知";
      return switch (path) {
         case "overworld"  -> "主世界";
         case "the_nether" -> "下界";
         case "the_end"    -> "末地";
         default           -> path;
      };
   }

   // 盔甲模式：玩家任一护甲槽非空即为"有护甲"
   private static boolean hasArmor(PlayerEntity p) {
      for (ItemStack stack : p.getInventory().armor) {
         if (!stack.isEmpty()) return true;
      }
      return false;
   }

   public static Formatting getHealthColor(PlayerEntity player) {
      double health = player.getHealth() + player.getAbsorptionAmount();
      if (health > 18.0) {
         return Formatting.GREEN;
      } else if (health > 16.0) {
         return Formatting.DARK_GREEN;
      } else if (health > 12.0) {
         return Formatting.YELLOW;
      } else if (health > 8.0) {
         return Formatting.GOLD;
      } else {
         return health > 4.0 ? Formatting.RED : Formatting.DARK_RED;
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
}
