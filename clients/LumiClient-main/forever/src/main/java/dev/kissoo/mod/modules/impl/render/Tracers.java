package dev.kissoo.mod.modules.impl.render;

import dev.kissoo.Forever;
import dev.kissoo.api.utils.render.Render3DUtil;
import dev.kissoo.api.utils.world.BlockUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Tracers extends Module {
   private final ColorSetting item = this.add(new ColorSetting("Item", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting player = this.add(new ColorSetting("Player", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting chest = this.add(new ColorSetting("Chest", new Color(255, 255, 255, 100)).injectBoolean(false));
   private final ColorSetting enderChest = this.add(new ColorSetting("EnderChest", new Color(255, 100, 255, 100)).injectBoolean(false));
   private final ColorSetting shulkerBox = this.add(new ColorSetting("ShulkerBox", new Color(15, 255, 255, 100)).injectBoolean(false));
   private final ColorSetting donkey = this.add(new ColorSetting("Donkey", new Color(139, 90, 43, 100)).injectBoolean(false));
   private final ColorSetting netherPortal = this.add(new ColorSetting("NetherPortal", new Color(150, 50, 250, 100)).injectBoolean(false));

   private final Set<BlockPos> portals = new HashSet<>();
   private int lastScanAge = -1;

   public Tracers() {
      super("Tracers", Module.Category.Render);
      this.setChinese("追踪者");
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      mc.options.getBobView().setValue(false);
      if (this.item.booleanValue || this.player.booleanValue || this.donkey.booleanValue) {
         for (Entity entity : Forever.THREAD.getEntities()) {
            if (entity instanceof ItemEntity && this.item.booleanValue) {
               this.drawLine(entity.getPos(), this.item.getValue());
            } else if (entity instanceof PlayerEntity && this.player.booleanValue && entity != mc.player) {
               this.drawLine(entity.getPos(), this.player.getValue());
            } else if ((entity instanceof DonkeyEntity || entity instanceof MuleEntity) && this.donkey.booleanValue) {
               this.drawLine(entity.getPos(), this.donkey.getValue());
            }
         }
      }

      for (BlockEntity blockEntity : BlockUtil.getTileEntities()) {
         if (blockEntity instanceof ChestBlockEntity && this.chest.booleanValue) {
            this.drawLine(blockEntity.getPos().toCenterPos(), this.chest.getValue());
         } else if (blockEntity instanceof EnderChestBlockEntity && this.enderChest.booleanValue) {
            this.drawLine(blockEntity.getPos().toCenterPos(), this.enderChest.getValue());
         } else if (blockEntity instanceof ShulkerBoxBlockEntity && this.shulkerBox.booleanValue) {
            this.drawLine(blockEntity.getPos().toCenterPos(), this.shulkerBox.getValue());
         }
      }

      if (this.netherPortal.booleanValue) {
         if (mc.player.age % 40 == 0 && mc.player.age != lastScanAge) {
            lastScanAge = mc.player.age;
            portals.clear();
            BlockPos playerPos = mc.player.getBlockPos();
            int r = 32;
            for (int x = -r; x <= r; x++) {
               for (int y = -r; y <= r; y++) {
                  for (int z = -r; z <= r; z++) {
                     BlockPos pos = playerPos.add(x, y, z);
                     if (mc.world.getBlockState(pos).isOf(Blocks.NETHER_PORTAL)) {
                        portals.add(pos);
                     }
                  }
               }
            }
         }
         for (BlockPos pos : portals) {
            this.drawLine(pos.toCenterPos(), this.netherPortal.getValue());
         }
      }
   }

   private void drawLine(Vec3d pos, Color color) {
      Render3DUtil.drawLine(
              pos,
              mc.gameRenderer
                      .getCamera()
                      .getPos()
                      .add(
                              Vec3d.fromPolar(mc.player.getPitch(mc.getRenderTickCounter().getTickDelta(true)), mc.player.getYaw(mc.getRenderTickCounter().getTickDelta(true)))
                                      .multiply(0.2)
                      ),
              color
      );
   }
}