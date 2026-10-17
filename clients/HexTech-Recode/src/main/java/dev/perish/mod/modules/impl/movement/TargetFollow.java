package dev.perish.mod.modules.impl.movement;

import dev.perish.Perish;
import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.UpdateRotateEvent;
import dev.perish.api.utils.math.MathUtil;
import dev.perish.core.impl.RotationManager;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.client.AntiCheat;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class TargetFollow extends Module {
    private final SliderSetting range = this.add(new SliderSetting("Range", 50.0, 1.0, 100.0, 1.0));

    public TargetFollow() {
        super("TargetFollow", Module.Category.Movement);
        this.setChinese("跟随玩家");
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        if (!AntiCheat.INSTANCE.grimServer.getValue()) {
            MoveFix.INSTANCE.enable();
        }
        if (ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.NCP)) {
            this.disable();
            sendMessage("§4TargetFollow disabled because ElytraFly mode is NCP.");
        }
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        if (!AntiCheat.INSTANCE.grimServer.getValue()) {
            MoveFix.INSTANCE.disable();
        }
        mc.options.forwardKey.setPressed(false);
    }

    @EventListener(priority = -9999)
    public void onRotation(UpdateRotateEvent event) {
        if (!nullCheck()) {
            PlayerEntity target = getClosestPlayer(this.range.getValue());
            if (target != null && ElytraFly.INSTANCE.isFallFlying()) {
                Vec3d pos = MathUtil.getClosestPointToBox(mc.player.getEyePos(), target.getBoundingBox());
                float[] rotations = RotationManager.getRotation(pos);
                event.setYaw(rotations[0]);
                event.setPitch(rotations[1]);
                mc.options.forwardKey.setPressed(true);
            }
        }
    }

    private PlayerEntity getClosestPlayer(double range) {
        PlayerEntity target = null;
        double distance = range;

        for (Entity entity : Perish.THREAD.getEntities()) {
            if (entity instanceof PlayerEntity player && entity != mc.player && !Perish.FRIEND.isFriend(player.getName().getString())) {
                if (mc.player.distanceTo(entity) <= range) {
                    if (target == null) {
                        target = player;
                        distance = mc.player.distanceTo(entity);
                    } else if (mc.player.distanceTo(entity) < distance) {
                        target = player;
                        distance = mc.player.distanceTo(entity);
                    }
                }
            }
        }
        return target;
    }

}