package dev.baimaodaxia.mod.modules.impl.misc;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.TextColor;
import net.minecraft.client.network.PlayerListEntry;

public class ExtraTab extends Module {
   public static ExtraTab INSTANCE;
   public final SliderSetting tabSize = this.add(new SliderSetting("TabSize", 80, 50, 1000));
   public final SliderSetting tabHeight = this.add(new SliderSetting("TabHeight", 20, 20, 100));
   private final BooleanSetting self = this.add(new BooleanSetting("Self", true));
   private final ColorSetting selfColor;
   private final BooleanSetting friends;
   private final ColorSetting friendColor;
   public final BooleanSetting accurateLatency;

   public ExtraTab() {
      super("ExtraTab", Module.Category.Misc);
      Color var10005 = new Color(255, 255, 255, 100);
      BooleanSetting var10006 = this.self;
      Objects.requireNonNull(var10006);
      this.selfColor = this.add(new ColorSetting("SelfColor", var10005, var10006::getValue));
      this.friends = this.add(new BooleanSetting("Friends", true));
      var10005 = new Color(0, 255, 0);
      var10006 = this.friends;
      Objects.requireNonNull(var10006);
      this.friendColor = this.add(new ColorSetting("FriendColor", var10005, var10006::getValue));
      this.accurateLatency = this.add(new BooleanSetting("AccurateLatency", false));
      this.setChinese("更多信息");
      INSTANCE = this;
   }

   public Text getPlayerName(PlayerListEntry playerListEntry) {
      Text baseName = playerListEntry.getDisplayName();
      if (baseName == null) {
         baseName = Text.literal(playerListEntry.getProfile().getName());
      }

      TextColor finalColor = null;
      if (playerListEntry.getProfile().getId().equals(mc.player.getGameProfile().getId()) && this.self.getValue()) {
         Color c = this.selfColor.getValue();
         finalColor = TextColor.fromRgb(c.getRed() << 16 | c.getGreen() << 8 | c.getBlue());
      } else if (this.friends.getValue() && Alien.FRIEND.isFriend(playerListEntry.getProfile().getName())) {
         Color c = this.friendColor.getValue();
         finalColor = TextColor.fromRgb(c.getRed() << 16 | c.getGreen() << 8 | c.getBlue());
      }

      String nameString = baseName.getString();

      for(Formatting format : Formatting.values()) {
         if (format.isColor()) {
            nameString = nameString.replace(format.toString(), "");
         }
      }

      MutableText finalDisplayName = Text.literal(nameString);
      if (finalColor != null) {
         finalDisplayName.setStyle(baseName.getStyle().withColor(finalColor));
      } else {
         finalDisplayName.setStyle(baseName.getStyle());
      }

      return finalDisplayName;
   }
}
