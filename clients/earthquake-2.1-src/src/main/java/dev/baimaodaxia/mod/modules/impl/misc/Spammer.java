package dev.baimaodaxia.mod.modules.impl.misc;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import dev.baimaodaxia.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

public class Spammer extends Module {
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   public final BooleanSetting checkSelf = this.add(new BooleanSetting("CheckSelf", false));
   final StringSetting message = this.add(new StringSetting("Message", "最强外挂Alien社区版免费试用 群\ud835\udff1\ud835\udff4\ud835\udff5\ud835\udfed\ud835\udff5\ud835\udfed\ud835\udff1\ud835\udff2\ud835\udfed"));
   private final Random random = new Random();
   private final SliderSetting randoms = this.add(new SliderSetting("Random", (double)3.0F, (double)0.0F, (double)20.0F, (double)1.0F));
   private final SliderSetting delay = this.add((new SliderSetting("Delay", (double)5.0F, (double)0.0F, (double)60.0F, 0.1)).setSuffix("s"));
   private final BooleanSetting tellMode = this.add(new BooleanSetting("RandomWhisper", false));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final Timer timer = new Timer();

   public Spammer() {
      super("Spammer", Module.Category.Misc);
      this.setChinese("自动刷屏");
   }

   public void onLogout() {
      if (this.autoDisable.getValue()) {
         this.disable();
      }

   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passedS(this.delay.getValue())) {
         this.timer.reset();
         Object randomString = this.generateRandomString(this.randoms.getValueInt());
         if (!((String)randomString).isEmpty()) {
            randomString = " " + (String)randomString;
         }

         if (this.tellMode.getValue()) {
            Collection players = mc.getNetworkHandler().getPlayerList();
            ArrayList list = new ArrayList(players);
            int size = list.size();
            if (size == 0) {
               return;
            }

            PlayerListEntry playerListEntry = (PlayerListEntry)list.get(this.random.nextInt(size));

            for(int i = 0; this.checkSelf.getValue() && Objects.equals(playerListEntry.getProfile().getName(), mc.player.getGameProfile().getName()); playerListEntry = (PlayerListEntry)list.get(this.random.nextInt(size))) {
               if (i > 50) {
                  return;
               }

               ++i;
            }

            ClientPlayNetworkHandler var10000 = mc.getNetworkHandler();
            String var10001 = playerListEntry.getProfile().getName();
            var10000.sendChatCommand("tell " + var10001 + " " + this.message.getValue() + (String)randomString);
         } else if (this.message.getValue().startsWith("/")) {
            ClientPlayNetworkHandler var8 = mc.getNetworkHandler();
            String var10 = this.message.getValue().replaceFirst("/", "");
            var8.sendCommand(var10 + (String)randomString);
         } else {
            ClientPlayNetworkHandler var9 = mc.getNetworkHandler();
            String var11 = this.message.getValue();
            var9.sendChatMessage(var11 + (String)randomString);
         }

      }
   }

   private String generateRandomString(int LENGTH) {
      StringBuilder sb = new StringBuilder(LENGTH);

      for(int i = 0; i < LENGTH; ++i) {
         int index = this.random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".length());
         sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(index));
      }

      return sb.toString();
   }
}
