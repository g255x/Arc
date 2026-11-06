package dev.g255.arc.api.utils.path;

import com.google.common.collect.Lists;
import dev.g255.arc.api.utils.Wrapper;
import java.util.List;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TPUtils implements Wrapper {
   public static void teleportWithBack(Vec3d newPos, TeleportType type, Runnable runnable) {
      switch (type.ordinal()) {
         case 0 -> legacyTeleportWithBack(newPos, runnable);
         case 1 -> newTeleportWithBack(newPos, runnable);
      }

   }

   public static void legacyTeleportWithBack(Vec3d newPos, Runnable runnable) {
      List<Vec3> tpPath = PathUtils.computePath(newPos);
      tpPath.removeFirst();
      tpPath.forEach((vec3) -> mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(vec3.x(), vec3.y(), vec3.z(), false)));
      runnable.run();
      tpPath = Lists.reverse(tpPath);
      tpPath.removeFirst();
      tpPath.forEach((vec3) -> mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(vec3.x(), vec3.y(), vec3.z(), false)));
      mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
   }

   public static void newTeleportWithBack(Vec3d newPos, Runnable runnable) {
      int packetsRequired = (int)Math.ceil(mc.player.getPos().distanceTo(newPos) / (double)10.0F) - 1;

      for(int i = 0; i < packetsRequired; ++i) {
         mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
      }

      mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(newPos.x, newPos.y, newPos.z, true));
      runnable.run();

      for(int var4 = 0; var4 < packetsRequired; ++var4) {
         mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
      }

      mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
   }

   public static void newTeleport(Vec3d newPos) {
      int packetsRequired = (int)Math.ceil(mc.player.getPos().distanceTo(newPos) / (double)10.0F) - 1;

      for(int i = 0; i < packetsRequired; ++i) {
         mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
      }

      mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(newPos.x, newPos.y, newPos.z, true));
   }

   public static enum TeleportType {
      Legacy,
      New;

      // $FF: synthetic method
      private static TeleportType[] $values() {
         return new TeleportType[]{Legacy, New};
      }
   }
}
