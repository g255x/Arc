package dev.Lumi.mod.commands.impl;

import dev.Lumi.Lumi;
import dev.Lumi.mod.commands.Command;
import dev.Lumi.mod.modules.Module;
import java.util.ArrayList;
import java.util.List;

public class ToggleCommand extends Command {
   public ToggleCommand() {
      super("toggle", "[module]");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String moduleName = parameters[0];
         Module module = Lumi.MODULE.getModuleByName(moduleName);
         if (module == null) {
            this.sendChatMessage("§cUnknown module");
         } else {
            module.toggle();
         }
      }
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      if (count != 1) {
         return null;
      } else {
         String input = ((String)seperated.getLast()).toLowerCase();
         ArrayList<String> correct = new ArrayList();

         for(Module x : Lumi.MODULE.getModules()) {
            if (input.equalsIgnoreCase(Lumi.getPrefix() + "toggle") || x.getName().toLowerCase().startsWith(input)) {
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
