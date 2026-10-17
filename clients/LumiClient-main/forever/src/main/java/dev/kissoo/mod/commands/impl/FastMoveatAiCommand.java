package dev.kissoo.mod.commands.impl;

import dev.kissoo.mod.commands.Command;
import dev.kissoo.mod.modules.impl.movement.FastMoveatAi;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class FastMoveatAiCommand extends Command {
    public FastMoveatAiCommand() {
        super("fastmoveatai", "[x] [y] [z] | ow/nether/end/ow2nether/nether2ow [x] [y] [z] | stop | status");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            this.sendUsage();
            return;
        }

        FastMoveatAi module = FastMoveatAi.INSTANCE;
        if (module == null) {
            this.sendChatMessage("[FastMoveatAi] Module is not initialized.");
            return;
        }

        String action = parameters[0].toLowerCase();
        if (action.equals("stop")) {
            module.stopNavigation();
            this.sendChatMessage("[FastMoveatAi] Navigation stopped.");
            return;
        }
        if (action.equals("status")) {
            BlockPos target = module.getTarget();
            this.sendChatMessage("[FastMoveatAi] " + (module.isOn() ? "Pathing" : "Stopped")
                    + " target=[" + target.getX() + ", " + target.getY() + ", " + target.getZ() + "]");
            return;
        }

        if (parameters.length == 3 || parameters.length == 4) {
            try {
                TargetInput target = this.parseTarget(parameters);
                this.startNavigation(module, target);
            } catch (NumberFormatException e) {
                this.sendChatMessage("[FastMoveatAi] Coordinates must be integers.");
            } catch (IllegalArgumentException e) {
                this.sendChatMessage(e.getMessage());
            }
            return;
        }

        this.sendUsage();
    }

    private TargetInput parseTarget(String[] parameters) {
        if (parameters.length == 3) {
            int x = Integer.parseInt(parameters[0]);
            int y = Integer.parseInt(parameters[1]);
            int z = Integer.parseInt(parameters[2]);
            return new TargetInput("raw", x, y, z, x, y, z);
        }

        String mode = parameters[0].toLowerCase();
        int inputX = Integer.parseInt(parameters[1]);
        int inputY = Integer.parseInt(parameters[2]);
        int inputZ = Integer.parseInt(parameters[3]);
        if (this.isOverworldMode(mode)) {
            return new TargetInput("overworld", inputX, inputY, inputZ, inputX, inputY, inputZ);
        }
        if (this.isNetherMode(mode)) {
            return new TargetInput("nether", inputX, inputY, inputZ, inputX, inputY, inputZ);
        }
        if (this.isEndMode(mode)) {
            return new TargetInput("end", inputX, inputY, inputZ, inputX, inputY, inputZ);
        }
        if (this.isOverworldToNetherMode(mode)) {
            return new TargetInput("overworld->nether", inputX, inputY, inputZ, this.scaleCoordinate(inputX, 1.0 / 8.0), inputY, this.scaleCoordinate(inputZ, 1.0 / 8.0));
        }
        if (this.isNetherToOverworldMode(mode)) {
            return new TargetInput("nether->overworld", inputX, inputY, inputZ, this.scaleCoordinate(inputX, 8.0), inputY, this.scaleCoordinate(inputZ, 8.0));
        }
        throw new IllegalArgumentException("[FastMoveatAi] Unknown coordinate mode: " + parameters[0]);
    }

    private void startNavigation(FastMoveatAi module, TargetInput target) {
        FastMoveatAi.StartResult result = module.startTo(target.targetX, target.targetY, target.targetZ);
        if (result == FastMoveatAi.StartResult.STARTED) {
            this.sendChatMessage("[FastMoveatAi] " + target.mode + " input [" + target.inputX + ", " + target.inputY + ", " + target.inputZ
                    + "] -> target [" + target.targetX + ", " + target.targetY + ", " + target.targetZ + "]");
        } else if (result == FastMoveatAi.StartResult.ARRIVED) {
            this.sendChatMessage("[FastMoveatAi] Already at target.");
        } else {
            this.sendChatMessage("[FastMoveatAi] Failed to start. Check dimension and Baritone.");
        }
    }

    private boolean isOverworldMode(String mode) {
        return mode.equals("ow") || mode.equals("overworld") || mode.equals("world");
    }

    private boolean isNetherMode(String mode) {
        return mode.equals("n") || mode.equals("nether");
    }

    private boolean isEndMode(String mode) {
        return mode.equals("end") || mode.equals("e");
    }

    private boolean isOverworldToNetherMode(String mode) {
        return mode.equals("ow2nether") || mode.equals("o2n") || mode.equals("overworld2nether");
    }

    private boolean isNetherToOverworldMode(String mode) {
        return mode.equals("nether2ow") || mode.equals("n2o") || mode.equals("nether2overworld");
    }

    private int scaleCoordinate(int coordinate, double scale) {
        return (int)Math.round(coordinate * scale);
    }

    @Override
    public String[] getAutocorrect(int count, List<String> separated) {
        if (count == 1) {
            return new String[]{"stop", "status", "ow", "nether", "end", "ow2nether", "nether2ow", "<x>"};
        }
        boolean commandMode = !separated.isEmpty()
                && (this.isOverworldMode(separated.get(0).toLowerCase())
                || this.isNetherMode(separated.get(0).toLowerCase())
                || this.isEndMode(separated.get(0).toLowerCase())
                || this.isOverworldToNetherMode(separated.get(0).toLowerCase())
                || this.isNetherToOverworldMode(separated.get(0).toLowerCase()));
        if (count == 2 && commandMode) {
            return new String[]{"<x>"};
        }
        if (count == 2 && !separated.get(0).equalsIgnoreCase("stop") && !separated.get(0).equalsIgnoreCase("status")) {
            return new String[]{"<y>"};
        }
        if (count == 3 && commandMode) {
            return new String[]{"<y>"};
        }
        if (count == 3 && !separated.get(0).equalsIgnoreCase("stop") && !separated.get(0).equalsIgnoreCase("status")) {
            return new String[]{"<z>"};
        }
        if (count == 4 && commandMode) {
            return new String[]{"<z>"};
        }
        return new String[0];
    }

    private record TargetInput(String mode, int inputX, int inputY, int inputZ, int targetX, int targetY, int targetZ) {
    }
}
