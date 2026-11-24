package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.player.PacketMine;
import dev.g255.arc.mod.modules.impl.render.PlaceRender;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class AntiPiston extends Module {
   public static AntiPiston INSTANCE;
   private final EnumSetting<Page> page;
   private final EnumSetting<Mode> mode;
   private final SliderSetting range;
   private final SliderSetting blocksPer;
   private final SliderSetting breakDelay;
   private final BooleanSetting rotate;
   private final BooleanSetting autoOff;
   private final BooleanSetting onlyHead;
   private final BooleanSetting redstone;
   private final BooleanSetting torch;
   private final BooleanSetting repeater;
   private final BooleanSetting leverBtn;
   private final BooleanSetting detectMining;
   private final BooleanSetting usingPause;
   private final BooleanSetting inventoryPause;
   private final Timer timer;
   private int progress;
   private boolean hasBroken;

   public AntiPiston() {
      super("AntiPiston", "Packet-mines nearby pistons and redstone", Module.Category.Combat);
      this.page = this.add(new EnumSetting("Page", AntiPiston.Page.General));
      this.mode = this.add(new EnumSetting("Mode", AntiPiston.Mode.Instant, () -> this.page.is(AntiPiston.Page.General)));
      this.range = this.add(new SliderSetting("Range", (double)4.0F, (double)1.0F, (double)6.0F, 0.1, () -> this.page.is(AntiPiston.Page.General)));
      this.blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 4, () -> this.page.is(AntiPiston.Page.General)));
      this.breakDelay = this.add(new SliderSetting("BreakDelay", 50, 0, 500, () -> this.page.is(AntiPiston.Page.General)));
      this.rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.is(AntiPiston.Page.General)));
      this.autoOff = this.add(new BooleanSetting("AutoOff", false, () -> this.page.is(AntiPiston.Page.General)));
      this.onlyHead = this.add(new BooleanSetting("OnlyHead", false, () -> this.page.is(AntiPiston.Page.Filter)));
      this.redstone = this.add(new BooleanSetting("Redstone", true, () -> this.page.is(AntiPiston.Page.Filter)));
      this.torch = this.add(new BooleanSetting("Torch", false, () -> this.page.is(AntiPiston.Page.Filter)));
      this.repeater = this.add(new BooleanSetting("Repeater", true, () -> this.page.is(AntiPiston.Page.Filter)));
      this.leverBtn = this.add(new BooleanSetting("LeverButton", false, () -> this.page.is(AntiPiston.Page.Filter)));
      this.detectMining = this.add(new BooleanSetting("DetectMining", true, () -> this.page.is(AntiPiston.Page.Filter)));
      this.usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.is(AntiPiston.Page.Check)));
      this.inventoryPause = this.add(new BooleanSetting("InventoryPause", true, () -> this.page.is(AntiPiston.Page.Check)));
      this.timer = new Timer();
      this.progress = 0;
      this.hasBroken = false;
      this.setChinese("反活塞");
      INSTANCE = this;
   }

   private boolean isRedstonePart(Block b) {
      return b == Blocks.REDSTONE_WIRE || b == Blocks.REDSTONE_BLOCK || b == Blocks.REDSTONE_TORCH || b == Blocks.REDSTONE_WALL_TORCH;
   }

   private boolean isTarget(BlockPos pos, Block b) {
      if (b != Blocks.PISTON && b != Blocks.STICKY_PISTON) {
         if (!this.redstone.getValue()) {
            return false;
         } else if (this.isRedstonePart(b)) {
            return b != Blocks.REDSTONE_TORCH && b != Blocks.REDSTONE_WALL_TORCH || this.torch.getValue();
         } else if (!this.repeater.getValue() || b != Blocks.REPEATER && b != Blocks.COMPARATOR) {
            return this.leverBtn.getValue() && (b == Blocks.LEVER || b == Blocks.STONE_BUTTON || b == Blocks.OAK_BUTTON || b == Blocks.POLISHED_BLACKSTONE_BUTTON);
         } else {
            return true;
         }
      } else {
         return !this.onlyHead.getValue() || !((double)pos.getY() < PacketMine.mc.player.getY() + (double)0.5F);
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (!this.usingPause.getValue() || !PacketMine.mc.player.isUsingItem()) {
            if (this.timer.passed((long)this.breakDelay.getValue())) {
               this.progress = 0;
               ArrayList<BlockPos> sphere = BlockUtil.getSphere(this.range.getValueFloat(), PacketMine.mc.player.getEyePos());

               for(BlockPos pos : sphere) {
                  if ((double)this.progress >= this.blocksPer.getValue()) {
                     break;
                  }

                  Block b = PacketMine.mc.world.getBlockState(pos).getBlock();
                  if ((b == Blocks.PISTON || b == Blocks.STICKY_PISTON || this.isTarget(pos, b)) && !PacketMine.mc.world.isAir(pos) && !PacketMine.unbreakable(pos) && (PacketMine.getBreakPos() == null || !PacketMine.getBreakPos().equals(pos)) && (!this.detectMining.getValue() || !Arc.BREAK.isMining(pos))) {
                     if (this.mode.is(AntiPiston.Mode.Mine)) {
                        PacketMine.INSTANCE.mine(pos);
                     } else {
                        this.instantBreak(pos);
                     }

                     ++this.progress;
                     this.hasBroken = true;
                  }
               }

               if (this.autoOff.getValue() && this.hasBroken && this.progress == 0 && PacketMine.getBreakPos() == null) {
                  boolean any = false;

                  for(BlockPos pos : sphere) {
                     Block b = PacketMine.mc.world.getBlockState(pos).getBlock();
                     if (b == Blocks.PISTON || b == Blocks.STICKY_PISTON || this.isTarget(pos, b)) {
                        any = true;
                        break;
                     }
                  }

                  if (!any) {
                     this.disable();
                  }
               }

            }
         }
      }
   }

   private void instantBreak(BlockPos pos) {
      Direction side = BlockUtil.getClickSide(pos);
      Vec3d center = pos.toCenterPos();
      if (this.rotate.getValue()) {
         Arc.ROTATION.snapAt(center);
      }

      sendSequencedPacket((id) -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, side, id));
      sendSequencedPacket((id) -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, side, id));
      sendSequencedPacket((id) -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, side, id));
      if (this.rotate.getValue()) {
         Arc.ROTATION.snapBack();
      }

      PlaceRender.INSTANCE.create(pos);
      this.timer.reset();
   }

   public void onEnable() {
      this.hasBroken = false;
      this.timer.reset();
   }

   public static enum Mode {
      Instant,
      Mine;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Instant, Mine};
      }
   }

   public static enum Page {
      General,
      Filter,
      Check;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{General, Filter, Check};
      }
   }
}
