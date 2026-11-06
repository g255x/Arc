package dev.g255.arc.mod.commands.impl;

import dev.g255.arc.mod.commands.Command;
import dev.g255.arc.mod.modules.impl.client.HUD;
import java.util.Arrays;
import java.util.List;

public class WatermarkCommand extends Command {
   public WatermarkCommand() {
      super("watermark", "[text]");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         StringBuilder text = new StringBuilder();
         boolean first = true;

         for(String s : Arrays.stream(parameters).toList()) {
            if (first) {
               text.append(s);
               first = false;
            } else {
               text.append(" ").append(s);
            }
         }

         HUD.INSTANCE.waterMarkString.setValue(text.toString() + " %ClientName% %Version% - By G255");
      }
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
