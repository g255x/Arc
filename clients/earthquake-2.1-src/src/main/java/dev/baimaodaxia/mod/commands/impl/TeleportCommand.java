package dev.baimaodaxia.mod.commands.impl;

import dev.baimaodaxia.api.utils.path.TPUtils;
import dev.baimaodaxia.mod.commands.Command;
import java.text.DecimalFormat;
import java.util.List;
import net.minecraft.util.math.Vec3d;
public class TeleportCommand extends Command {
   public TeleportCommand() {
      super("tp", "[x] [y] [z]");
   }

   public void runCommand(String[] parameters) {
      if (parameters.length != 3) {
         this.sendUsage();
      } else {
         double x;
         if (this.isNumeric(parameters[0])) {
            x = Double.parseDouble(parameters[0]);
         } else {
            if (!parameters[0].startsWith("~")) {
               this.sendUsage();
               return;
            }

            if (this.isNumeric(parameters[0].replace("~", ""))) {
               x = mc.player.getX() + Double.parseDouble(parameters[0].replace("~", ""));
            } else {
               if (!parameters[0].replace("~", "").isEmpty()) {
                  this.sendUsage();
                  return;
               }

               x = mc.player.getX();
            }
         }

         double y;
         if (this.isNumeric(parameters[1])) {
            y = Double.parseDouble(parameters[1]);
         } else {
            if (!parameters[1].startsWith("~")) {
               this.sendUsage();
               return;
            }

            if (this.isNumeric(parameters[1].replace("~", ""))) {
               y = mc.player.getY() + Double.parseDouble(parameters[1].replace("~", ""));
            } else {
               if (!parameters[1].replace("~", "").isEmpty()) {
                  this.sendUsage();
                  return;
               }

               y = mc.player.getY();
            }
         }

         double z;
         if (this.isNumeric(parameters[2])) {
            z = Double.parseDouble(parameters[2]);
         } else {
            if (!parameters[2].startsWith("~")) {
               this.sendUsage();
               return;
            }

            if (this.isNumeric(parameters[2].replace("~", ""))) {
               z = mc.player.getZ() + Double.parseDouble(parameters[2].replace("~", ""));
            } else {
               if (!parameters[2].replace("~", "").isEmpty()) {
                  this.sendUsage();
                  return;
               }

               z = mc.player.getZ();
            }
         }

         TPUtils.newTeleport(new Vec3d(x, y, z));
         mc.player.setPosition(x, y, z);
         DecimalFormat df = new DecimalFormat("0.0");
         String var10001 = df.format(x);
         this.sendChatMessage("§fTeleported to §e" + var10001 + ", " + df.format(y) + ", " + df.format(z));
      }
   }

   private boolean isNumeric(String str) {
      return str.matches("-?\\d+(\\.\\d+)?");
   }

   public String[] getAutocorrect(int count, List<String> seperated) {
      return new String[]{"~ "};
   }
}
