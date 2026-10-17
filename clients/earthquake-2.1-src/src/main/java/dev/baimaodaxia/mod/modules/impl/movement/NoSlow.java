package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.earthquake;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.InteractItemEvent;
import dev.baimaodaxia.api.events.impl.KeyboardInputEvent;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.player.MovementUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.util.Hand;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.item.Item;

public class NoSlow extends Module {
   public static NoSlow INSTANCE;
   final Queue<ClickSlotC2SPacket> storedClicks = new LinkedList();
   final AtomicBoolean pause = new AtomicBoolean();
   private final EnumSetting<Mode> mode;
   private final BooleanSetting food;
   private final BooleanSetting bow;
   private final BooleanSetting crossbow;
   private final BooleanSetting soulSand;
   private final BooleanSetting sneak;
   private final BooleanSetting climb;
   private final BooleanSetting gui;
   private final BooleanSetting allowSneak;
   private final EnumSetting<Bypass> clickBypass;
   boolean using;
   int delay;
   private int onGroundTick;

   public NoSlow() {
      super("NoSlow", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", NoSlow.Mode.Vanilla));
      this.food = this.add(new BooleanSetting("Food", true));
      this.bow = this.add(new BooleanSetting("Bow", true));
      this.crossbow = this.add(new BooleanSetting("Crossbow", true));
      this.soulSand = this.add(new BooleanSetting("SoulSand", true));
      this.sneak = this.add(new BooleanSetting("Sneak", false));
      this.climb = this.add(new BooleanSetting("Climb", false));
      this.gui = this.add(new BooleanSetting("Gui", true));
      BooleanSetting var10006 = this.gui;
      Objects.requireNonNull(var10006);
      this.allowSneak = this.add(new BooleanSetting("AllowSneak", false, var10006::getValue));
      this.clickBypass = this.add(new EnumSetting("GuiMoveBypass", NoSlow.Bypass.None));
      this.using = false;
      this.delay = 0;
      this.onGroundTick = 0;
      this.setChinese("无减速");
      INSTANCE = this;
   }

   private static float getMovementMultiplier(boolean positive, boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }

   public String getInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         this.using = mc.player.isUsingItem();
         --this.delay;
         if (this.using) {
            this.delay = 2;
         }

         if (this.using && !mc.player.isRiding() && !mc.player.isFallFlying()) {
            switch (((Mode)this.mode.getValue()).ordinal()) {
               case 1:
                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                  break;
               case 2:
                  if (mc.player.getActiveHand() == Hand.OFF_HAND) {
                     sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, earthquake.ROTATION.getLastYaw(), earthquake.ROTATION.getLastPitch()));
                  } else {
                     sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, earthquake.ROTATION.getLastYaw(), earthquake.ROTATION.getLastPitch()));
                  }
                  break;
               case 3:
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 1, 0, SlotActionType.PICKUP, mc.player);
                  if (mc.player.getActiveHand() == Hand.OFF_HAND) {
                     sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, earthquake.ROTATION.getLastYaw(), earthquake.ROTATION.getLastPitch()));
                  } else {
                     sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, earthquake.ROTATION.getLastYaw(), earthquake.ROTATION.getLastPitch()));
                  }
            }
         }

         if (mc.player.isOnGround()) {
            ++this.onGroundTick;
         } else {
            this.onGroundTick = 0;
         }

         if (this.using && this.shouldKeepSprintThisTick() && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
         }

         if (this.gui.getValue() && !(mc.currentScreen instanceof ChatScreen)) {
            for(KeyBinding k : new KeyBinding[]{mc.options.backKey, mc.options.leftKey, mc.options.rightKey}) {
               k.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey()).getCode()));
            }

            mc.options.jumpKey.setPressed(ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue() || InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.jumpKey.getBoundKeyTranslationKey()).getCode()));
            mc.options.forwardKey.setPressed(AutoWalk.INSTANCE.forward() || InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.forwardKey.getBoundKeyTranslationKey()).getCode()));
            mc.options.sprintKey.setPressed(Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater() || InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.sprintKey.getBoundKeyTranslationKey()).getCode()));
            if (this.allowSneak.getValue()) {
               mc.options.sneakKey.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode()));
            }
         }

      }
   }

   @EventListener(
      priority = 100
   )
   public void keyboard(KeyboardInputEvent event) {
      if (this.sneak.getValue()) {
         event.cancel();
      }

      if (this.gui.getValue() && !(mc.currentScreen instanceof ChatScreen)) {
         for(KeyBinding k : new KeyBinding[]{mc.options.backKey, mc.options.leftKey, mc.options.rightKey}) {
            k.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey()).getCode()));
         }

         mc.options.jumpKey.setPressed(ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue() || InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.jumpKey.getBoundKeyTranslationKey()).getCode()));
         mc.options.forwardKey.setPressed(AutoWalk.INSTANCE.forward() || InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.forwardKey.getBoundKeyTranslationKey()).getCode()));
         mc.options.sprintKey.setPressed(Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater() || InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.sprintKey.getBoundKeyTranslationKey()).getCode()));
         if (this.allowSneak.getValue()) {
            mc.options.sneakKey.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode()));
         }

         mc.player.input.pressingForward = mc.options.forwardKey.isPressed();
         mc.player.input.pressingBack = mc.options.backKey.isPressed();
         mc.player.input.pressingLeft = mc.options.leftKey.isPressed();
         mc.player.input.pressingRight = mc.options.rightKey.isPressed();
         mc.player.input.movementForward = getMovementMultiplier(mc.player.input.pressingForward, mc.player.input.pressingBack);
         mc.player.input.movementSideways = getMovementMultiplier(mc.player.input.pressingLeft, mc.player.input.pressingRight);
         mc.player.input.jumping = mc.options.jumpKey.isPressed();
         mc.player.input.sneaking = mc.options.sneakKey.isPressed();
      }

      if (this.mode.is(NoSlow.Mode.Jump) && !nullCheck() && mc.player.isOnGround() && mc.player.isUsingItem() && MovementUtil.isMoving() && this.isNoSlowAllowedForCurrentItem()) {
         mc.player.input.jumping = true;
      }

   }

   @EventListener
   public void onUse(InteractItemEvent event) {
      if (event.isPre()) {
         if (this.delay > 0) {
            mc.itemUseCooldown = 0;
            event.cancel();
         } else if (this.mode.is(NoSlow.Mode.GrimPacket) && mc.player != null && mc.player.getStackInHand(event.hand).getItem().getComponents().contains(DataComponentTypes.FOOD)) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 1, 0, SlotActionType.PICKUP, mc.player);
         }
      }

   }

   @EventListener
   public void onPacketSend(PacketEvent.Send e) {
      if (!nullCheck()) {
         PlayerInteractItemC2SPacket packet;
         Packet<?> packet2;
         if (this.mode.is(NoSlow.Mode.Drop) && (packet2 = e.getPacket()) instanceof PlayerInteractItemC2SPacket && (packet = (PlayerInteractItemC2SPacket)packet2).getHand() == Hand.MAIN_HAND && mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD)) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN));
         } else if (MovementUtil.isMoving() && !this.pause.get()) {
            packet2 = e.getPacket();
            if (packet2 instanceof ClickSlotC2SPacket) {
               ClickSlotC2SPacket click = (ClickSlotC2SPacket)packet2;
               switch (((Bypass)this.clickBypass.getValue()).ordinal()) {
                  case 1:
                     if (mc.player.isOnGround() && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset((double)0.0F, 0.0656, (double)0.0F)).iterator().hasNext()) {
                        if (mc.player.isSprinting()) {
                           mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                        }

                        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0656, mc.player.getZ(), false));
                     }
                     break;
                  case 2:
                     if (mc.player.isOnGround() && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset((double)0.0F, 2.71875E-7, (double)0.0F)).iterator().hasNext()) {
                        if (mc.player.isSprinting()) {
                           mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                        }

                        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.71875E-7, mc.player.getZ(), false));
                     }
                     break;
                  case 3:
                     if (click.getActionType() != SlotActionType.PICKUP && click.getActionType() != SlotActionType.PICKUP_ALL) {
                        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
                     }
                     break;
                  case 4:
                     this.storedClicks.add(click);
                     e.cancel();
               }
            }

            if (e.getPacket() instanceof CloseHandledScreenC2SPacket && this.clickBypass.is(NoSlow.Bypass.Delay)) {
               this.pause.set(true);

               while(!this.storedClicks.isEmpty()) {
                  mc.getNetworkHandler().sendPacket((Packet)this.storedClicks.poll());
               }

               this.pause.set(false);
            }

         }
      }
   }

   @EventListener
   public void onPacketSendPost(PacketEvent.Sent e) {
      if (e.getPacket() instanceof ClickSlotC2SPacket && mc.player.isSprinting() && this.clickBypass.is(NoSlow.Bypass.NCP)) {
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
      }

   }

   public boolean noSlow() {
      if (this.isOn() && !nullCheck() && mc.player.isUsingItem()) {
         Mode currentMode = this.mode.getValue();
         if (currentMode == NoSlow.Mode.None) {
            return false;
         } else if ((currentMode == NoSlow.Mode.Drop || currentMode == NoSlow.Mode.GrimPacket) && !this.using) {
            return false;
         } else if (!this.isNoSlowAllowedForCurrentItem()) {
            return false;
         } else {
            boolean var10000;
            switch (currentMode.ordinal()) {
               case 5 -> var10000 = this.getItemUseTimeLeft() % 3 != 0 && (!this.isFoodLimitedItem() || this.getItemUseTimeLeft() <= 30);
               case 6 -> var10000 = this.getItemUseTimeLeft() % 2 == 0 && this.getItemUseTimeLeft() <= 30;
               case 7 -> var10000 = this.getItemUseTimeLeft() % 3 == 0 && (!this.isFoodLimitedItem() || this.getItemUseTimeLeft() <= 30);
               case 8 -> var10000 = this.onGroundTick == 1 && this.getItemUseTimeLeft() <= 30;
               default -> var10000 = true;
            }

            return var10000;
         }
      } else {
         return false;
      }
   }

   public void onEnable() {
      this.onGroundTick = 0;
   }

   public void onDisable() {
      this.onGroundTick = 0;
   }

   public boolean soulSand() {
      return this.isOn() && this.soulSand.getValue();
   }

   public boolean climb() {
      return this.isOn() && this.climb.getValue();
   }

   private int getItemUseTimeLeft() {
      return mc.player.getItemUseTimeLeft();
   }

   private boolean isFoodLimitedItem() {
      ItemStack Main = mc.player.getMainHandStack();
      ItemStack off = mc.player.getOffHandStack();
      return this.isFoodLimitedItem(Main) || this.isFoodLimitedItem(off);
   }

   private boolean isFoodLimitedItem(ItemStack stack) {
      return stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE || stack.getItem() == Items.POTION;
   }

   private boolean isHolding(Item item) {
      return mc.player.getMainHandStack().getItem() == item || mc.player.getOffHandStack().getItem() == item;
   }

   private boolean isNoSlowAllowedForCurrentItem() {
      if (!this.food.getValue() && this.isFoodLimitedItem()) {
         return false;
      } else if (!this.bow.getValue() && this.isHolding(Items.BOW)) {
         return false;
      } else if (!this.crossbow.getValue() && this.isHolding(Items.CROSSBOW)) {
         return false;
      } else {
         return !this.isFoodLimitedItem() || this.getItemUseTimeLeft() <= 30;
      }
   }

   private boolean shouldKeepSprintThisTick() {
      if (this.isOn() && !nullCheck() && mc.player.isUsingItem()) {
         boolean var10000;
         switch (((Mode)this.mode.getValue()).ordinal()) {
            case 5:
            case 6:
            case 7:
            case 8:
               var10000 = this.noSlow();
               break;
            default:
               var10000 = false;
         }

         return var10000;
      } else {
         return false;
      }
   }

   public static enum Mode {
      Vanilla,
      NCP,
      Grim,
      GrimPacket,
      Drop,
      Heypixel2_3,
      Grim50,
      Grim1_3,
      Jump,
      None;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Vanilla, NCP, Grim, GrimPacket, Drop, Heypixel2_3, Grim50, Grim1_3, Jump, None};
      }
   }

   private static enum Bypass {
      None,
      NCP,
      NCP2,
      Grim,
      Delay;

      // $FF: synthetic method
      private static Bypass[] $values() {
         return new Bypass[]{None, NCP, NCP2, Grim, Delay};
      }
   }
}
