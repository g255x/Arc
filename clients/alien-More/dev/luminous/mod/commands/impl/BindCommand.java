package dev.luminous.mod.commands.impl;

import dev.luminous.Nina;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.modules.Module;
import java.util.ArrayList;
import java.util.List;

public class BindCommand extends Command {
   public BindCommand() {
      super("bind", "[module] [key]");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String moduleName = parameters[0];
         Module module = Nina.MODULE.getModuleByName(moduleName);
         if (module == null) {
            this.sendChatMessage("§4Unknown module!");
         } else if (parameters.length == 1) {
            this.sendChatMessage("§fPlease specify keyCodec §bkey§f.");
         } else {
            String rkey = parameters[1];
            if (rkey == null) {
               this.sendChatMessage("§4Unknown Error");
            } else {
               if (module.setBind(rkey.toUpperCase())) {
                  String var10001 = module.getName();
                  this.sendChatMessage("§fBind for §r" + var10001 + "§f set to §r" + rkey.toUpperCase());
               }

            }
         }
      }
   }

   public String[] getAutocorrect(int count, List seperated) {
      if (count != 1) {
         return null;
      } else {
         String input = ((String)seperated.getLast()).toLowerCase();
         ArrayList<String> correct = new ArrayList();

         for(Module x : Nina.MODULE.getModules()) {
            if (input.equalsIgnoreCase(Nina.getPrefix() + "bind") || x.getName().toLowerCase().startsWith(input)) {
               correct.add(x.getName());
            }
         }

         int numCmds = correct.size();
         String[] commands = new String[numCmds];
         int i = 0;

         for(String x : correct) {
            commands[i++] = x;
         }

         return commands;
      }
   }
}
