package dev.baimaodaxia.api.utils;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class InteractTypeUtil {
    public static final int INTERACT = 0;
    public static final int ATTACK = 1;
    public static final int INTERACT_AT = 2;

    public static int getInteractTypeOrdinal(Object packet) {
        int[] result = new int[1];
        result[0] = -1;
        ((PlayerInteractEntityC2SPacket) packet).handle(new PlayerInteractEntityC2SPacket.Handler() {
            public void interact(Hand hand) {
                result[0] = INTERACT;
            }

            public void interactAt(Hand hand, Vec3d pos) {
                result[0] = INTERACT_AT;
            }

            public void attack() {
                result[0] = ATTACK;
            }
        });
        return result[0];
    }
}