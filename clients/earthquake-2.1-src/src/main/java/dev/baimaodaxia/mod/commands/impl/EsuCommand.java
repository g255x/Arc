package dev.baimaodaxia.mod.commands.impl;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.mod.commands.Command;
import dev.baimaodaxia.mod.modules.Module;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import net.minecraft.text.Text;

public class EsuCommand extends Command {
   public EsuCommand() {
      super("esu", "");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length == 1) {
         Alien.THREAD.execute(() -> {
            try {
               URL url = new URL("https://api.esu.wiki/");
               BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

               String inputLine;
               while((inputLine = in.readLine()) != null) {
                  if (Module.nullCheck()) {
                     return;
                  }

                  mc.player.sendMessage(Text.of(inputLine));
               }
            } catch (Exception e) {
               e.printStackTrace();
            }

         });
      }

   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
