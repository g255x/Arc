package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.player.EntityUtil;
import dev.g255.arc.api.utils.player.MovementUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.Iterator;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class AntiSlowFalling extends Module {
   public static AntiSlowFalling INSTANCE;
   private final BooleanSetting bypassSlowFalling = this.add(new BooleanSetting("SlowFallingCheck", false));
   private final SliderSetting y = this.add(new SliderSetting("YMove", 0.01, (double)0.0F, (double)1.0F, 0.01, () -> this.bypassSlowFalling.getValue()));
   private final SliderSetting jumpDelay = this.add(new SliderSetting("JumpTick", (double)0.0F, (double)0.0F, (double)500.0F, (double)1.0F, () -> this.bypassSlowFalling.getValue()));
   private final BooleanSetting excludeWater = this.add(new BooleanSetting("ExcludeWater", true, () -> this.bypassSlowFalling.getValue()));
   private final BooleanSetting noFallFlying = this.add(new BooleanSetting("NoFallFlying", true, () -> this.bypassSlowFalling.getValue()));
   private final BooleanSetting noWeb = this.add(new BooleanSetting("NoWeb", true, () -> this.bypassSlowFalling.getValue()));
   private final BooleanSetting springBack = this.add(new BooleanSetting("SpringBack", false, () -> this.bypassSlowFalling.getValue()));
   private final SliderSetting backDelay = this.add(new SliderSetting("BackDelay", (double)1.0F, (double)1.0F, (double)20.0F, (double)1.0F, () -> this.bypassSlowFalling.getValue() && this.springBack.getValue()));
   private final SliderSetting springBackThreshold = this.add(new SliderSetting("SBThreshold", (double)2.0F, (double)1.0F, (double)10.0F, (double)1.0F, () -> this.bypassSlowFalling.getValue() && this.springBack.getValue()));
   private final SliderSetting springBackWindow = this.add(new SliderSetting("SBWindow", (double)1.0F, (double)0.5F, (double)5.0F, 0.1, () -> this.bypassSlowFalling.getValue() && this.springBack.getValue()));
   private final Timer jumpTimer = new Timer();
   private final Timer springBackPacketTimer = new Timer();
   private final Timer sneakTimer = new Timer();
   private int springBackPacketCount = 0;
   private boolean isSneakingForSpringBack = false;
   private boolean shouldSneakForSpringBack = false;
   private SpringBackState springBackState;
   private int sneakStartTick;

   public AntiSlowFalling() {
      super("AntiSlowFalling", Module.Category.Movement);
      this.springBackState = AntiSlowFalling.SpringBackState.IDLE;
      this.sneakStartTick = 0;
      this.setChinese("反缓降");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck() && this.bypassSlowFalling.getValue()) {
         this.handleSpringBackState();
         if (this.jumpTimer.passedMs((double)((long)this.jumpDelay.getValue()))) {
            if (mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
               if (!mc.player.isSpectator()) {
                  if (EntityUtil.isInsideBlock()) {
                     if (!this.excludeWater.getValue() || !mc.player.isTouchingWater()) {
                        if (this.noFallFlying.getValue()) {
                           boolean wearingElytra = false;
                           Iterator var3 = mc.player.getArmorItems().iterator();

                           while(true) {
                              if (var3.hasNext()) {
                                 ItemStack is = (ItemStack)var3.next();
                                 if (is.getItem() != Items.ELYTRA) {
                                    continue;
                                 }

                                 wearingElytra = true;
                              }

                              if (mc.player.isFallFlying() || wearingElytra) {
                                 return;
                              }
                              break;
                           }
                        }

                        if (this.noWeb.getValue()) {
                           boolean inWeb = Arc.PLAYER.isInWeb(mc.player) || mc.world.getBlockState(EntityUtil.getPlayerPos(true)).getBlock() == Blocks.COBWEB;
                           if (inWeb) {
                              return;
                           }
                        }

                        if (mc.player.isOnGround() && mc.player.fallDistance <= 0.0F) {
                           MovementUtil.setMotionY(this.y.getValue());
                           this.jumpTimer.reset();
                        }

                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.bypassSlowFalling.getValue() && this.springBack.getValue()) {
         if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.handlePositionCorrection();
         }

      }
   }

   private void handlePositionCorrection() {
      if (mc.player != null && mc.world != null) {
         if (EntityUtil.isInsideBlock() && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            if (!this.excludeWater.getValue() || !mc.player.isTouchingWater()) {
               if (!this.noFallFlying.getValue() || !mc.player.isFallFlying()) {
                  if (this.noWeb.getValue()) {
                     boolean inWeb = Arc.PLAYER.isInWeb(mc.player) || mc.world.getBlockState(EntityUtil.getPlayerPos(true)).getBlock() == Blocks.COBWEB;
                     if (inWeb) {
                        return;
                     }
                  }

                  if (this.springBackPacketCount == 0) {
                     this.springBackPacketTimer.reset();
                  }

                  ++this.springBackPacketCount;
                  if (this.springBackPacketCount >= this.springBackThreshold.getValueInt() && !this.springBackPacketTimer.passedMs((double)((long)(this.springBackWindow.getValue() * (double)1000.0F)))) {
                     this.shouldSneakForSpringBack = true;
                     this.springBackPacketCount = 0;
                  }

               }
            }
         } else {
            this.springBackPacketCount = 0;
         }
      }
   }

   private void handleSpringBackState() {
      if (this.bypassSlowFalling.getValue() && this.springBack.getValue()) {
         if (mc.player == null) {
            this.resetSpringBackState();
         } else {
            switch (this.springBackState.ordinal()) {
               case 0:
                  if (this.shouldSneakForSpringBack) {
                     mc.options.sneakKey.setPressed(true);
                     this.isSneakingForSpringBack = true;
                     this.sneakStartTick = mc.player.age;
                     this.sneakTimer.reset();
                     this.springBackState = AntiSlowFalling.SpringBackState.SNEAKING;
                     this.shouldSneakForSpringBack = false;
                  }
                  break;
               case 1:
                  int currentTick = mc.player.age;
                  int ticksSneaking = currentTick - this.sneakStartTick;
                  if ((double)ticksSneaking >= this.backDelay.getValue()) {
                     mc.options.sneakKey.setPressed(false);
                     this.isSneakingForSpringBack = false;
                     this.springBackState = AntiSlowFalling.SpringBackState.RELEASING;
                     this.sneakTimer.reset();
                  }
                  break;
               case 2:
                  if (this.sneakTimer.passedMs((double)100.0F)) {
                     this.springBackState = AntiSlowFalling.SpringBackState.IDLE;
                  }
            }

            if (this.springBackPacketCount > 0 && this.springBackPacketTimer.passedMs((double)((long)(this.springBackWindow.getValue() * (double)1000.0F)))) {
               this.springBackPacketCount = 0;
            }

         }
      } else {
         this.resetSpringBackState();
      }
   }

   private void resetSpringBackState() {
      if (this.isSneakingForSpringBack) {
         mc.options.sneakKey.setPressed(false);
      }

      this.springBackState = AntiSlowFalling.SpringBackState.IDLE;
      this.isSneakingForSpringBack = false;
      this.shouldSneakForSpringBack = false;
      this.springBackPacketCount = 0;
   }

   public void onDisable() {
      super.onDisable();
      this.resetSpringBackState();
   }

   public void onEnable() {
      super.onEnable();
      this.resetSpringBackState();
   }

   private static enum SpringBackState {
      IDLE,
      SNEAKING,
      RELEASING;

      // $FF: synthetic method
      private static SpringBackState[] $values() {
         return new SpringBackState[]{IDLE, SNEAKING, RELEASING};
      }
   }
}
