package dev.g255.arc.mod.commands.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.mod.commands.Command;
import dev.g255.arc.mod.modules.Module;
import java.util.ArrayList;
import java.util.List;

public class TCommand extends Command {
   public TCommand() {
      super("t", "[module]");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String moduleName = parameters[0];
         Module module = Arc.MODULE.getModuleByName(moduleName);
         if (module == null) {
            this.sendChatMessage("§fUnknown module!");
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

         for(Module x : Arc.MODULE.getModules()) {
            if (input.equalsIgnoreCase(Arc.getPrefix() + "t") || x.getName().toLowerCase().startsWith(input)) {
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
