package dev.g255.arc.mod.modules.impl.movement;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.TimerEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.player.MovementUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.CobwebBlock;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class FastWeb extends Module {
   public static FastWeb INSTANCE;
   public final EnumSetting<Mode> mode;
   public final BooleanSetting onlySneak;
   public final BooleanSetting grim;
   public final BooleanSetting abortPacket;
   public final SliderSetting xZSlow;
   public final SliderSetting ySlow;
   private final SliderSetting fastSpeed;
   private boolean work;

   public FastWeb() {
      super("FastWeb", "So you don't need to keep timer on keybind", Module.Category.Movement);
      this.mode = this.add(new EnumSetting("Mode", FastWeb.Mode.Vanilla));
      this.onlySneak = this.add(new BooleanSetting("OnlySneak", true));
      this.grim = this.add((new BooleanSetting("Grim", false)).setParent());
      BooleanSetting var10006 = this.grim;
      Objects.requireNonNull(var10006);
      this.abortPacket = this.add(new BooleanSetting("AbortPacket", true, var10006::isOpen));
      this.xZSlow = this.add((new SliderSetting("XZSpeed", (double)25.0F, (double)0.0F, (double)100.0F, 0.1, () -> this.mode.getValue() == FastWeb.Mode.Custom)).setSuffix("%"));
      this.ySlow = this.add((new SliderSetting("YSpeed", (double)100.0F, (double)0.0F, (double)100.0F, 0.1, () -> this.mode.getValue() == FastWeb.Mode.Custom)).setSuffix("%"));
      this.fastSpeed = this.add(new SliderSetting("Speed", (double)3.0F, (double)0.0F, (double)8.0F, () -> this.mode.getValue() == FastWeb.Mode.Vanilla || this.mode.getValue() == FastWeb.Mode.Strict));
      this.work = false;
      this.setChinese("蜘蛛网加速");
      INSTANCE = this;
   }

   public String getInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      boolean bl = this.work = !mc.player.isOnGround() && (mc.options.sneakKey.isPressed() || !this.onlySneak.getValue()) && Arc.PLAYER.isInWeb(mc.player);
      if (this.work && this.mode.is(FastWeb.Mode.Vanilla)) {
         MovementUtil.setMotionY(-this.fastSpeed.getValue());
      }

      if (this.grim.getValue() && (mc.options.sneakKey.isPressed() || !this.onlySneak.getValue())) {
         for(BlockPos pos : this.getIntersectingWebs()) {
            if (this.abortPacket.getValue()) {
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN));
            }

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
         }
      }

   }

   @EventListener(
      priority = -100
   )
   public void onTimer(TimerEvent event) {
      if (this.work && this.mode.getValue() == FastWeb.Mode.Strict) {
         event.set(this.fastSpeed.getValueFloat());
      }

   }

   public List<BlockPos> getIntersectingWebs() {
      int radius = 2;
      ArrayList<BlockPos> blocks = new ArrayList();

      for(int x = radius; x > -radius; --x) {
         for(int y = radius; y > -radius; --y) {
            for(int z = radius; z > -radius; --z) {
               BlockPos blockPos = BlockPos.ofFloored(mc.player.getX() + (double)x, mc.player.getY() + (double)y, mc.player.getZ() + (double)z);
               if ((!(mc.player.getPos().distanceTo(blockPos.toCenterPos()) > (double)1.0F) || !(mc.player.getEyePos().distanceTo(blockPos.toCenterPos()) > (double)1.0F)) && mc.world.getBlockState(blockPos).getBlock() instanceof CobwebBlock) {
                  blocks.add(blockPos);
               }
            }
         }
      }

      return blocks;
   }

   public static enum Mode {
      Vanilla,
      Strict,
      Custom,
      Ignore;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Vanilla, Strict, Custom, Ignore};
      }
   }
}
