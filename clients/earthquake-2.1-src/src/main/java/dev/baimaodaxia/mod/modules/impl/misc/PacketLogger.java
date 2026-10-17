package dev.baimaodaxia.mod.modules.impl.misc;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.EntityVelocityUpdateEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.asm.accessors.ICloseHandledScreenC2SPacket;
import dev.baimaodaxia.asm.accessors.ICommonPongC2SPacket;
import dev.baimaodaxia.asm.accessors.IHandSwingC2SPacket;
import dev.baimaodaxia.asm.accessors.IPlayerActionC2SPacket;
import dev.baimaodaxia.asm.accessors.IPlayerInteractBlockC2SPacket;
import dev.baimaodaxia.asm.accessors.IPlayerInteractItemC2SPacket;
import dev.baimaodaxia.asm.accessors.IUpdateSelectedSlotC2SPacket;
import dev.baimaodaxia.core.impl.CommandManager;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.combat.Criticals;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;

public class PacketLogger extends Module {
   public static PacketLogger INSTANCE;
   private final BooleanSetting moveFull = this.add(new BooleanSetting("MoveFull", true));
   private final BooleanSetting movePos = this.add(new BooleanSetting("MovePosition", true));
   private final BooleanSetting moveLook = this.add(new BooleanSetting("MoveLook", true));
   private final BooleanSetting moveGround = this.add(new BooleanSetting("MoveGround", true));
   private final BooleanSetting vehicleMove = this.add(new BooleanSetting("VehicleMove", true));
   private final BooleanSetting playerAction = this.add(new BooleanSetting("PlayerAction", true));
   private final BooleanSetting updateSlot = this.add(new BooleanSetting("UpdateSlot", true));
   private final BooleanSetting handSwing = this.add(new BooleanSetting("HandSwing", true));
   private final BooleanSetting pong = this.add(new BooleanSetting("Pong", true));
   private final BooleanSetting interactEntity = this.add(new BooleanSetting("InteractEntity", true));
   private final BooleanSetting interactBlock = this.add(new BooleanSetting("InteractBlock", true));
   private final BooleanSetting interactItem = this.add(new BooleanSetting("InteractItem", true));
   private final BooleanSetting closeScreen = this.add(new BooleanSetting("CloseScreen", true));
   private final BooleanSetting command = this.add(new BooleanSetting("ClientCommand", true));
   private final BooleanSetting status = this.add(new BooleanSetting("ClientStatus", true));
   private final BooleanSetting clickSlot = this.add(new BooleanSetting("ClickSlot", true));
   private final BooleanSetting pickInventory = this.add(new BooleanSetting("PickInventory", true));
   private final BooleanSetting teleportConfirm = this.add(new BooleanSetting("TeleportConfirm", true));
   private final BooleanSetting s2cVelocity = this.add(new BooleanSetting("S2cVelocity", true));

   public PacketLogger() {
      super("PacketLogger", Module.Category.Misc);
      this.setChinese("数据包记录");
      INSTANCE = this;
   }

   private void logPacket(String msg, Object... args) {
      String s = String.format(msg, args);
      CommandManager.sendMessage(s);
   }

   @EventListener(
      priority = 999999
   )
   public void velocity(EntityVelocityUpdateEvent event) {
      if (this.s2cVelocity.getValue() && event.getEntity() == mc.player) {
         this.logPacket("S2C Velocity, x: %s, y: %s, z: %s, isExplosion: %s", event.getX(), event.getY(), event.getZ(), event.isExplosion());
      }

   }

   @EventListener(
      priority = -999999
   )
   public void onPacketSend(PacketEvent.Send event) {
      Packet<?> rawPacket = event.getPacket();
      if (rawPacket instanceof PlayerMoveC2SPacket.Full packet) {
         if (this.moveFull.getValue()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove Full - ");
            if (packet.changesPosition()) {
               builder.append("x: ").append(packet.getX((double)0.0F)).append(", y: ").append(packet.getY((double)0.0F)).append(", z: ").append(packet.getZ((double)0.0F)).append(" ");
            }

            if (packet.changesLook()) {
               builder.append("yaw: ").append(packet.getYaw(0.0F)).append(", pitch: ").append(packet.getPitch(0.0F)).append(" ");
            }

            builder.append(" onground: ").append(packet.isOnGround());
            this.logPacket(builder.toString());
         }
      }

      if (rawPacket instanceof PlayerMoveC2SPacket.PositionAndOnGround packet) {
         if (this.movePos.getValue()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove PosGround - ");
            if (packet.changesPosition()) {
               builder.append("x: ").append(packet.getX((double)0.0F)).append(", y: ").append(packet.getY((double)0.0F)).append(", z: ").append(packet.getZ((double)0.0F)).append(" ");
            }

            builder.append(" onground: ").append(packet.isOnGround());
            this.logPacket(builder.toString());
         }
      }

      if (rawPacket instanceof PlayerMoveC2SPacket.LookAndOnGround packet) {
         if (this.moveLook.getValue()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove LookGround - ");
            if (packet.changesLook()) {
               builder.append("yaw: ").append(packet.getYaw(0.0F)).append(", pitch: ").append(packet.getPitch(0.0F)).append(" ");
            }

            builder.append(" onground: ").append(packet.isOnGround());
            this.logPacket(builder.toString());
         }
      }

      if (rawPacket instanceof PlayerMoveC2SPacket.OnGroundOnly packet) {
         if (this.moveGround.getValue()) {
            this.logPacket("PlayerMove Ground - onground: " + packet.isOnGround());
         }
      }

      if (rawPacket instanceof VehicleMoveC2SPacket packet) {
         if (this.vehicleMove.getValue()) {
            this.logPacket("VehicleMove - x: %s, y: %s, z: %s, yaw: %s, pitch: %s", packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch());
         }
      }

      if (rawPacket instanceof PlayerActionC2SPacket packet) {
         IPlayerActionC2SPacket accessor = (IPlayerActionC2SPacket)packet;
         if (this.playerAction.getValue() && accessor.getDirection() != null) {
            this.logPacket("PlayerAction - action: %s, direction: %s, pos: %s", accessor.getAction().name(), accessor.getDirection().name(), accessor.getPos().toShortString());
         }
      }

      if (rawPacket instanceof UpdateSelectedSlotC2SPacket packet) {
         if (this.updateSlot.getValue()) {
            this.logPacket("UpdateSlot - slot: %d", ((IUpdateSelectedSlotC2SPacket)packet).getSelectedSlot());
         }
      }

      if (rawPacket instanceof HandSwingC2SPacket packet) {
         if (this.handSwing.getValue()) {
            this.logPacket("HandSwing - hand: %s", ((IHandSwingC2SPacket)packet).getHand().name());
         }
      }

      if (rawPacket instanceof CommonPongC2SPacket packet) {
         if (this.pong.getValue()) {
            this.logPacket("Pong - %d", ((ICommonPongC2SPacket)packet).getParameter());
         }
      }

      if (rawPacket instanceof PlayerInteractEntityC2SPacket packet) {
         if (this.interactEntity.getValue()) {
            this.logPacket("InteractEntity - Entity: %s, id: %s", Criticals.getEntity(packet).getName().getString(), Criticals.getEntity(packet).getId());
         }
      }

      if (rawPacket instanceof PlayerInteractBlockC2SPacket packet) {
         IPlayerInteractBlockC2SPacket accessor = (IPlayerInteractBlockC2SPacket)packet;
         if (this.interactBlock.getValue()) {
            BlockHitResult blockHitResult = accessor.getBlockHitResult();
            this.logPacket("InteractBlock - pos: %s, dir: %s, hand: %s", blockHitResult.getBlockPos().toShortString(), blockHitResult.getSide().name(), accessor.getHand().name());
         }
      }

      if (rawPacket instanceof PlayerInteractItemC2SPacket packet) {
         if (this.interactItem.getValue()) {
            this.logPacket("InteractItem - hand: %s", ((IPlayerInteractItemC2SPacket)packet).getHand().name());
         }
      }

      if (rawPacket instanceof CloseHandledScreenC2SPacket packet) {
         if (this.closeScreen.getValue()) {
            this.logPacket("CloseScreen - id: %s", ((ICloseHandledScreenC2SPacket)packet).getSyncId());
         }
      }

      if (rawPacket instanceof ClientCommandC2SPacket packet) {
         if (this.command.getValue()) {
            this.logPacket("ClientCommand - mode: %s", packet.getMode().name());
         }
      }

      if (rawPacket instanceof ClientStatusC2SPacket packet) {
         if (this.status.getValue()) {
            this.logPacket("ClientStatus - mode: %s", packet.getMode().name());
         }
      }

      if (rawPacket instanceof ClickSlotC2SPacket packet) {
         if (this.clickSlot.getValue()) {
            this.logPacket("ClickSlot - type: %s, slot: %s, button: %s, id: %s", packet.getActionType().name(), packet.getSlot(), packet.getButton(), packet.getSyncId());
         }
      }

      if (rawPacket instanceof PickFromInventoryC2SPacket packet) {
         if (this.pickInventory.getValue()) {
            this.logPacket("PickInventory - slot: %s", packet.getSlot());
         }
      }

      if (rawPacket instanceof TeleportConfirmC2SPacket packet) {
         if (this.teleportConfirm.getValue()) {
            this.logPacket("TeleportConfirm - id: %s", packet.getTeleportId());
         }
      }

   }
}
