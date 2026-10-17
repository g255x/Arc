package dev.baimaodaxia.mod.commands.impl;

import dev.baimaodaxia.mod.commands.Command;
import dev.baimaodaxia.mod.modules.impl.misc.ShulkerViewer;
import java.util.List;
import net.minecraft.item.ItemStack;

public class PeekCommand extends Command {
   private static final ItemStack[] ITEMS = new ItemStack[27];

   public PeekCommand() {
      super("peek", "");
   }

   public void runCommand(String[] parameters) {
      ShulkerViewer.openContainer(mc.player.getMainHandStack(), ITEMS, true);
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
