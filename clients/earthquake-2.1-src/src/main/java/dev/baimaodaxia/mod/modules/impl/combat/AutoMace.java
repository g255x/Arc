package dev.baimaodaxia.mod.modules.impl.combat;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.utils.player.EntityUtil;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.asm.accessors.IPlayerMoveC2SPacket;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import dev.baimaodaxia.api.utils.InteractTypeUtil;

public class AutoMace extends Module {
   public static AutoMace INSTANCE;
   private final EnumSetting<Mode> mode;
   private final BooleanSetting noCrystal;
   private final BooleanSetting inventorySwap;
   private final BooleanSetting onlyGround;
   private final SliderSetting height;
   boolean ignore;
   private PlayerInteractEntityC2SPacket lastPacket;

   public AutoMace() {
      super("AutoMace", Module.Category.Combat);
      this.mode = this.add(new EnumSetting("Mode", AutoMace.Mode.Vanilla));
      this.noCrystal = this.add(new BooleanSetting("NoCrystal", true, () -> this.mode.is(AutoMace.Mode.Swap)));
      this.inventorySwap = this.add(new BooleanSetting("InventorySwap", false, () -> this.mode.is(AutoMace.Mode.Swap)));
      this.onlyGround = this.add(new BooleanSetting("OnlyGround", true, () -> this.mode.is(AutoMace.Mode.Vanilla)));
      this.height = this.add(new SliderSetting("Height", (double)25.0F, (double)0.0F, (double)2000.0F, 0.1, () -> this.mode.is(AutoMace.Mode.Vanilla)));
      this.ignore = false;
      this.lastPacket = null;
      this.setChinese("风锤加伤");
      INSTANCE = this;
   }

   public static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
      PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
      packet.write(packetBuf);
      return mc.world == null ? null : mc.world.getEntityById(packetBuf.readVarInt());
   }

   public static int getInteractType(PlayerInteractEntityC2SPacket packet) {
      PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
      packet.write(packetBuf);
      packetBuf.readVarInt();
      return InteractTypeUtil.getInteractTypeOrdinal(packet);
   }

   @EventListener(
      priority = 200
   )
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (this.mode.is(AutoMace.Mode.Vanilla)) {
            Entity entity;
            PlayerInteractEntityC2SPacket packet;
            Packet<?> packet2;
            if (mc.player.getMainHandStack().getItem() == Items.MACE && (packet2 = event.getPacket()) instanceof PlayerInteractEntityC2SPacket && getInteractType(packet = (PlayerInteractEntityC2SPacket)packet2) == AutoMace.InteractType.ATTACK.ordinal() && !((entity = getEntity(packet)) instanceof EndCrystalEntity) && (!this.onlyGround.getValue() || mc.player.isOnGround() || mc.player.getAbilities().flying) && !mc.player.isInLava() && !mc.player.isSubmergedInWater() && entity != null) {
               for(int i = 0; i < 4; ++i) {
                  this.sendFakeY((double)0.0F);
               }

               this.sendFakeY(this.height.getValue());
               this.sendFakeY((double)0.0F);
            }
         } else if (this.mode.is(AutoMace.Mode.NCP)) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket) {
               ((IPlayerMoveC2SPacket)event.getPacket()).setOnGround(false);
            }
         } else if (this.mode.is(AutoMace.Mode.Swap)) {
            if (event.isCancelled()) {
               return;
            }

            int slot = this.getMaceSlot();
            if (slot == -1) {
               return;
            }

            if (this.ignore) {
               return;
            }

            Packet<?> packet3 = event.getPacket();
            PlayerInteractEntityC2SPacket packet;
            if (packet3 instanceof PlayerInteractEntityC2SPacket && Criticals.getInteractType(packet = (PlayerInteractEntityC2SPacket)packet3) == InteractTypeUtil.ATTACK) {
               if (this.noCrystal.getValue() && Criticals.getEntity(packet) instanceof EndCrystalEntity) {
                  return;
               }

               this.lastPacket = packet;
               this.ignore = true;
               this.doSpoof();
               this.ignore = false;
               event.cancel();
            }
         }

      }
   }

   private void doSpoof() {
      if (this.lastPacket != null) {
         int slot = this.getMaceSlot();
         if (slot != -1) {
            int old = mc.player.getInventory().selectedSlot;
            this.doSwap(slot);
            mc.getNetworkHandler().sendPacket(this.lastPacket);
            if (this.inventorySwap.getValue()) {
               this.doSwap(slot);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(old);
            }

         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }

   }

   private int getMaceSlot() {
      return this.inventorySwap.getValue() ? InventoryUtil.findItemInventorySlot(Items.MACE) : InventoryUtil.findItem(Items.MACE);
   }

   private void sendFakeY(double offset) {
      mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + offset, mc.player.getZ(), false));
   }

   public static enum Mode {
      Vanilla,
      NCP,
      Swap;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Vanilla, NCP, Swap};
      }
   }

   public static enum InteractType {
      INTERACT,
      ATTACK,
      INTERACT_AT;

      // $FF: synthetic method
      private static InteractType[] $values() {
         return new InteractType[]{INTERACT, ATTACK, INTERACT_AT};
      }
   }
}
