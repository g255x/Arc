package dev.kissoo.mod.modules.impl.movement;

import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.InteractItemEvent;
import dev.kissoo.api.events.impl.KeyboardInputEvent;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class NoSlow extends Module {
   public static NoSlow INSTANCE;
   final Queue<ClickSlotC2SPacket> storedClicks = new LinkedList<>();
   final AtomicBoolean pause = new AtomicBoolean();

   private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Vanilla));
   private final BooleanSetting soulSand = this.add(new BooleanSetting("SoulSand", true));
   private final BooleanSetting sneak = this.add(new BooleanSetting("Sneak", false));
   private final BooleanSetting climb = this.add(new BooleanSetting("Climb", false));
   private final BooleanSetting gui = this.add(new BooleanSetting("Gui", true));
   private final BooleanSetting allowSneak = this.add(new BooleanSetting("AllowSneak", false, this.gui::getValue));
   private final EnumSetting<Bypass> clickBypass = this.add(new EnumSetting<>("GuiMoveBypass", Bypass.None));

   private final EnumSetting<GrimTickSpeed> grimTickSpeed = this.add(
           new EnumSetting<>("GT Speed", GrimTickSpeed.NORMAL, () -> mode.is(Mode.GrimTick))
   );
   private final BooleanSetting grimTickFood = this.add(
           new BooleanSetting("GT Food", true, () -> mode.is(Mode.GrimTick))
   );
   private final BooleanSetting grimTickBow = this.add(
           new BooleanSetting("GT Bow", true, () -> mode.is(Mode.GrimTick))
   );
   private final BooleanSetting grimTickCrossbow = this.add(
           new BooleanSetting("GT Crossbow", true, () -> mode.is(Mode.GrimTick))
   );

   boolean using = false;
   int delay = 0;

   private GrimStep grimStep = GrimStep.NONE;
   private int grimNoUsingItemTicks = 0;
   private final Queue<Packet<?>> grimPackets = new ConcurrentLinkedQueue<>();

   private boolean grimTickWasSlowdownActive = false;
   private boolean grimTickPausePhase = false;
   private int grimTickPauseTimer = 0;
   private static final int GRIMTICK_PAUSE_DURATION = 0;

   public NoSlow() {
      super("NoSlow", Module.Category.Movement);
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

   @Override
   public String getInfo() {
      if (this.mode.is(Mode.GrimTick)) {
         return "GrimTick-" + this.grimTickSpeed.getValue().name();
      }
      return this.mode.getValue().name();
   }

   private boolean isGrimUsable(UseAction useAction) {
      return useAction == UseAction.EAT || useAction == UseAction.DRINK;
   }

   private boolean checkGrimFood() {
      if (mc.player == null) return false;
      ItemStack mainHandItem = mc.player.getMainHandStack();
      ItemStack offhandItem = mc.player.getOffHandStack();
      return mainHandItem.isOf(Items.GOLDEN_APPLE) || offhandItem.isOf(Items.GOLDEN_APPLE)
              || mainHandItem.isOf(Items.ENCHANTED_GOLDEN_APPLE) || offhandItem.isOf(Items.ENCHANTED_GOLDEN_APPLE)
              || mainHandItem.isOf(Items.POTION) || offhandItem.isOf(Items.POTION);
   }

   private void grimReleasePackets() {
      this.grimStep = GrimStep.NONE;
      while (!this.grimPackets.isEmpty()) {
         Packet<?> p = this.grimPackets.poll();
         if (p == null || mc.getNetworkHandler() == null) continue;
         mc.getNetworkHandler().sendPacket(p);
      }
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                 Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      }
   }

   public boolean isGrimWorking() {
      return this.grimStep != GrimStep.NONE;
   }

   public boolean handleGrimSlowdown() {
      if (mc.player == null || mc.player.getActiveItem() == null) {
         return false;
      }

      UseAction activeUseAction = mc.player.getActiveItem().getUseAction();
      if (!this.isGrimUsable(activeUseAction) || mc.player.getItemUseTimeLeft() <= 0) {
         return false;
      }

      Hand oppositeHand = mc.player.getActiveHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;

      if (this.isGrimUsable(mc.player.getStackInHand(oppositeHand).getUseAction())) {
         return false;
      }

      if (this.grimStep != GrimStep.EATING) {
         mc.options.useKey.setPressed(false);
      }

      if (this.grimStep == GrimStep.NONE) {
         this.grimStep = GrimStep.CANCEL_C0F;
         boolean isInventoryOpenServerSide = mc.player.currentScreenHandler != mc.player.playerScreenHandler;
         if (isInventoryOpenServerSide) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(
                    mc.player.currentScreenHandler.syncId));
         }
         return false;
      } else if (this.grimStep == GrimStep.EATING) {
         mc.player.setSprinting(true);
         return true;
      }

      return false;
   }

   private boolean checkGrimTickFood() {
      if (mc.player == null) return false;
      ItemStack mainHandItem = mc.player.getMainHandStack();
      ItemStack offhandItem = mc.player.getOffHandStack();
      return mainHandItem.isOf(Items.GOLDEN_APPLE) || offhandItem.isOf(Items.GOLDEN_APPLE)
              || mainHandItem.isOf(Items.ENCHANTED_GOLDEN_APPLE) || offhandItem.isOf(Items.ENCHANTED_GOLDEN_APPLE)
              || mainHandItem.isOf(Items.POTION) || offhandItem.isOf(Items.POTION);
   }

   private boolean checkGrimTickItem(Item item) {
      if (mc.player == null) return false;
      ItemStack mainHandItem = mc.player.getMainHandStack();
      ItemStack offhandItem = mc.player.getOffHandStack();
      return mainHandItem.isOf(item) || offhandItem.isOf(item);
   }

   private void resetGrimTickVariables() {
      grimTickWasSlowdownActive = false;
      grimTickPausePhase = false;
      grimTickPauseTimer = 0;
   }

   public boolean handleGrimTickSlowdown() {
      if (mc.player == null) return false;
      if (!mc.player.isUsingItem()) return false;
      if (!grimTickFood.getValue() && checkGrimTickFood()) return false;
      if (!grimTickBow.getValue() && checkGrimTickItem(Items.BOW)) return false;
      if (!grimTickCrossbow.getValue() && checkGrimTickItem(Items.CROSSBOW)) return false;
      if (checkGrimTickFood() && mc.player.getItemUseTimeLeft() > 30) return false;

      int remainingTicks = mc.player.getItemUseTimeLeft();
      boolean shouldCancelSlow = this.grimTickSpeed.getValue().shouldCancelSlowdown(remainingTicks);

      if (shouldCancelSlow) {
         if (!grimTickWasSlowdownActive) {
            grimTickPausePhase = true;
            grimTickPauseTimer = GRIMTICK_PAUSE_DURATION;
            grimTickWasSlowdownActive = true;
            return false;
         }

         if (grimTickPausePhase) {
            grimTickPauseTimer--;
            if (grimTickPauseTimer <= 0) {
               grimTickPausePhase = false;
               mc.player.setSprinting(true);
               return true;
            }
            return false;
         }

         mc.player.setSprinting(true);
         return true;
      } else {
         grimTickWasSlowdownActive = false;
         grimTickPausePhase = false;
         grimTickPauseTimer = 0;
         return false;
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.grimStep = GrimStep.NONE;
      this.grimNoUsingItemTicks = 0;
      this.grimReleasePackets();
      resetGrimTickVariables();
   }

   private boolean isKeyPressedSafe(KeyBinding keyBinding) {
      if (mc.getWindow() == null) return false;
      InputUtil.Key boundKey = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
      int keyCode = boundKey.getCode();

      if (keyCode == -1 || boundKey == InputUtil.UNKNOWN_KEY) {
         return false;
      }

      if (boundKey.getCategory() == InputUtil.Type.KEYSYM) {
         return InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyCode);
      } else if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
         return org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
      }

      return false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.using = mc.player.isUsingItem();
      this.delay--;
      if (this.using) {
         this.delay = 2;
      }

      if (this.using && !mc.player.isRiding() && !mc.player.isFallFlying()) {
         if (this.mode.is(Mode.NCP)) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
         }
      }

      if (this.mode.is(Mode.Grim)) {
         if (this.grimStep != GrimStep.EATING) {
            this.grimNoUsingItemTicks = 0;
         } else {
            if (mc.player.isUsingItem()) {
               this.grimNoUsingItemTicks = 0;
            } else {
               ++this.grimNoUsingItemTicks;
               if (this.grimNoUsingItemTicks >= 5) {
                  this.grimReleasePackets();
               }
            }
         }
      }
      if (this.mode.is(Mode.GrimTick)) {
         if (!mc.player.isUsingItem()) {
            resetGrimTickVariables();
         }
      }
      if (this.gui.getValue() && !(mc.currentScreen instanceof ChatScreen)) {
         for (KeyBinding k : new KeyBinding[]{mc.options.backKey, mc.options.leftKey, mc.options.rightKey}) {
            k.setPressed(isKeyPressedSafe(k));
         }

         mc.options.jumpKey.setPressed(isKeyPressedSafe(mc.options.jumpKey));

         mc.options.forwardKey.setPressed(
                 AutoWalk.INSTANCE.forward() || isKeyPressedSafe(mc.options.forwardKey)
         );

         mc.options.sprintKey.setPressed(
                 (Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater()) || isKeyPressedSafe(mc.options.sprintKey)
         );

         if (this.allowSneak.getValue()) {
            mc.options.sneakKey.setPressed(isKeyPressedSafe(mc.options.sneakKey));
         }
      }
   }

   @EventListener(priority = 100)
   public void keyboard(KeyboardInputEvent event) {
      if (this.sneak.getValue()) {
         event.cancel();
      }

      if (this.gui.getValue() && !(mc.currentScreen instanceof ChatScreen)) {
         for (KeyBinding k : new KeyBinding[]{mc.options.backKey, mc.options.leftKey, mc.options.rightKey}) {
            k.setPressed(isKeyPressedSafe(k));
         }

         mc.options.jumpKey.setPressed(isKeyPressedSafe(mc.options.jumpKey));

         mc.options.forwardKey.setPressed(
                 AutoWalk.INSTANCE.forward() || isKeyPressedSafe(mc.options.forwardKey)
         );

         mc.options.sprintKey.setPressed(
                 (Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater()) || isKeyPressedSafe(mc.options.sprintKey)
         );

         if (this.allowSneak.getValue()) {
            mc.options.sneakKey.setPressed(isKeyPressedSafe(mc.options.sneakKey));
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
   }

   @EventListener
   public void onUse(InteractItemEvent event) {
      if (event.isPre()) {
         if (dev.kissoo.mod.modules.impl.exploit.BowSpam.INSTANCE != null
                 && dev.kissoo.mod.modules.impl.exploit.BowSpam.INSTANCE.isOn()) {
            return;
         }

         if (this.delay > 0) {
            mc.itemUseCooldown = 0;
            event.cancel();
         }
      }
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send e) {
      if (nullCheck()) return;
      if (this.mode.is(Mode.Grim)) {
         Packet<?> packet = e.getPacket();
         if (packet instanceof CommonPongC2SPacket && this.grimStep != GrimStep.NONE) {
            e.cancel();
            this.grimPackets.add(packet);
            if (this.grimStep == GrimStep.CANCEL_C0F) {
               this.grimStep = GrimStep.SWAP_HANDS;
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                       Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
            }
         }

         if (packet instanceof PlayerActionC2SPacket actionPacket) {
            if (actionPacket.getAction() == Action.RELEASE_USE_ITEM && this.grimStep == GrimStep.EATING) {
               this.grimReleasePackets();
            }
         }
      }
      if (this.mode.is(Mode.Drop)
              && e.getPacket() instanceof PlayerInteractItemC2SPacket packet
              && packet.getHand() == Hand.MAIN_HAND
              && mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD)) {
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN));
      }
      if (MovementUtil.isMoving() && !this.pause.get()) {
         if (e.getPacket() instanceof ClickSlotC2SPacket click) {
            switch (this.clickBypass.getValue()) {
               case NCP:
                  if (mc.player.isOnGround() && !mc.world.getBlockCollisions(mc.player,
                          mc.player.getBoundingBox().offset(0.0, 0.0656, 0.0)).iterator().hasNext()) {
                     if (mc.player.isSprinting()) {
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player,
                                ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                     }
                     mc.getNetworkHandler().sendPacket(new PositionAndOnGround(
                             mc.player.getX(), mc.player.getY() + 0.0656, mc.player.getZ(), false));
                  }
                  break;
               case NCP2:
                  if (mc.player.isOnGround() && !mc.world.getBlockCollisions(mc.player,
                          mc.player.getBoundingBox().offset(0.0, 2.71875E-7, 0.0)).iterator().hasNext()) {
                     if (mc.player.isSprinting()) {
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player,
                                ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                     }
                     mc.getNetworkHandler().sendPacket(new PositionAndOnGround(
                             mc.player.getX(), mc.player.getY() + 2.71875E-7, mc.player.getZ(), false));
                  }
                  break;
               case Grim:
                  if (click.getActionType() != SlotActionType.PICKUP && click.getActionType() != SlotActionType.PICKUP_ALL) {
                     mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
                  }
                  break;
               case Delay:
                  this.storedClicks.add(click);
                  e.cancel();
                  break;
            }
         }

         if (e.getPacket() instanceof CloseHandledScreenC2SPacket && this.clickBypass.is(Bypass.Delay)) {
            this.pause.set(true);
            while (!this.storedClicks.isEmpty()) {
               mc.getNetworkHandler().sendPacket(this.storedClicks.poll());
            }
            this.pause.set(false);
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive e) {
      if (this.mode.is(Mode.Grim)) {
         Packet<?> packet = e.getPacket();
         if (packet instanceof ScreenHandlerSlotUpdateS2CPacket && this.grimStep == GrimStep.SWAP_HANDS) {
            mc.options.useKey.setPressed(true);
            this.grimStep = GrimStep.EATING;
         }
      }
   }

   @EventListener
   public void onPacketSendPost(PacketEvent.Sent e) {
      if (e.getPacket() instanceof ClickSlotC2SPacket && mc.player.isSprinting() && this.clickBypass.is(Bypass.NCP)) {
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player,
                 ClientCommandC2SPacket.Mode.START_SPRINTING));
      }
   }

   public boolean noSlow() {
      if (!this.isOn()) return false;
      if (this.mode.getValue() == Mode.None) return false;

      switch (this.mode.getValue()) {
         case Grim:
            return this.handleGrimSlowdown();

         case GrimTick:
            return this.handleGrimTickSlowdown();

         case Drop:
            return this.using;

         case Vanilla:
         case NCP:
            return true;

         default:
            return false;
      }
   }

   public boolean soulSand() {
      return this.isOn() && this.soulSand.getValue();
   }

   public boolean climb() {
      return this.isOn() && this.climb.getValue();
   }

   private enum Bypass {
      None,
      NCP,
      NCP2,
      Grim,
      Delay
   }

   public enum Mode {
      Vanilla,
      NCP,
      Grim,
      GrimTick,
      Drop,
      None
   }

   private enum GrimStep {
      NONE,
      CANCEL_C0F,
      SWAP_HANDS,
      EATING
   }

   public enum GrimTickSpeed {
      NORMAL {
         @Override
         public boolean shouldCancelSlowdown(int remainingTicks) {
            return remainingTicks % 3 != 0;
         }
      },
      AGGRESSIVE {
         @Override
         public boolean shouldCancelSlowdown(int remainingTicks) {
            return remainingTicks % 4 != 0;
         }
      },
      FAST {
         @Override
         public boolean shouldCancelSlowdown(int remainingTicks) {
            return remainingTicks % 5 != 0;
         }
      },
      EXTREME {
         @Override
         public boolean shouldCancelSlowdown(int remainingTicks) {
            return remainingTicks % 6 != 0;
         }
      },
      FULL {
         @Override
         public boolean shouldCancelSlowdown(int remainingTicks) {
            return true;
         }
      };

      public abstract boolean shouldCancelSlowdown(int remainingTicks);
   }
}