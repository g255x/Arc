package dev.kissoo.mod.commands.impl;

import dev.kissoo.mod.commands.Command;
import dev.kissoo.mod.modules.impl.movement.ElytraFlight;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class GotoCommand extends Command {
    public GotoCommand() {
        super("goto", "[x] [y] [z] | stop");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            this.sendUsage();
            return;
        }

        String action = parameters[0].toLowerCase();

        if (action.equals("stop")) {
            ElytraFlight.INSTANCE.stopGoto();
            this.sendChatMessage("\u00a7c[AutoPilot] Navigation sequence aborted. Returning to manual control.");
            return;
        }

        if (parameters.length == 3) {
            try {
                int x = Integer.parseInt(parameters[0]);
                int y = Integer.parseInt(parameters[1]);
                int z = Integer.parseInt(parameters[2]);

                BlockPos target = new BlockPos(x, y, z);
                ElytraFlight.INSTANCE.startGoto(target);
                this.sendChatMessage(String.format("\u00a7b[AutoPilot] Trajectory locked \u00a78[\u00a7f%d, %d, %d\u00a78]\u00a7b. Engaging hybrid navigation...", x, y, z));
            } catch (NumberFormatException e) {
                this.sendChatMessage("\u00a74[AutoPilot] Critical Error: Invalid telemetry data. Coordinates must be integers.");
            }
            return;
        }

        this.sendUsage();
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        if (count == 1) {
            return new String[]{"stop", "<x>"};
        } else if (count == 2 && !seperated.get(0).equalsIgnoreCase("stop")) {
            return new String[]{"<y>"};
        } else if (count == 3 && !seperated.get(0).equalsIgnoreCase("stop")) {
            return new String[]{"<z>"};
        }
        return null;
    }
}