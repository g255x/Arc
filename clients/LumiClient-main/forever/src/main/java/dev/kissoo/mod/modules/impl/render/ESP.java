package dev.kissoo.mod.modules.impl.render;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.EntitySpawnedEvent;
import dev.kissoo.api.utils.math.MathUtil;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.render.Render3DUtil;
import dev.kissoo.api.utils.world.BlockUtil;
import hoprc.asm.accessors.IEntity;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class ESP extends Module {
   public static ESP INSTANCE;
   public final BooleanSetting box = this.add(new BooleanSetting("BoxESP", true).setParent());
   private final ColorSetting endPortalFill = this.add(new ColorSetting("EndPortalFill", new Color(255, 243, 129, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting endPortalOutline = this.add(
      new ColorSetting("EndPortalOutline", new Color(255, 243, 129, 100), this.box::isOpen).injectBoolean(false)
   );
   private final ColorSetting netherPortalFill = this.add(
      new ColorSetting("NetherPortalFill", new Color(150, 50, 250, 100), this.box::isOpen).injectBoolean(false)
   );
   private final ColorSetting netherPortalOutline = this.add(
      new ColorSetting("NetherPortalOutline", new Color(150, 50, 250, 100), this.box::isOpen).injectBoolean(false)
   );
   private final SliderSetting portalRange = this.add(
      new SliderSetting("PortalRange", 32, 8, 64, 1, this.box::isOpen)
   );
   private final ColorSetting itemFill = this.add(new ColorSetting("ItemFill", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting itemOutline = this.add(new ColorSetting("ItemOutline", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting playerFill = this.add(new ColorSetting("PlayerFill", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting playerOutline = this.add(new ColorSetting("PlayerOutline", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting chestFill = this.add(new ColorSetting("ChestFill", new Color(255, 198, 123, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting chestOutline = this.add(new ColorSetting("ChestOutline", new Color(255, 198, 123, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting enderChestFill = this.add(
      new ColorSetting("EnderChestFill", new Color(255, 100, 255, 100), this.box::isOpen).injectBoolean(false)
   );
   private final ColorSetting enderChestOutline = this.add(
      new ColorSetting("EnderChestOutline", new Color(255, 100, 255, 100), this.box::isOpen).injectBoolean(false)
   );
   private final ColorSetting shulkerBoxFill = this.add(new ColorSetting("ShulkerBoxFill", new Color(15, 255, 255, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting shulkerBoxOutline = this.add(
      new ColorSetting("ShulkerBoxOutline", new Color(15, 255, 255, 100), this.box::isOpen).injectBoolean(false)
   );
   public final BooleanSetting item = this.add(new BooleanSetting("ItemName", false).setParent());
   public final BooleanSetting customName = this.add(new BooleanSetting("CustomName", false, this.item::isOpen));
   public final BooleanSetting count = this.add(new BooleanSetting("Count", true, this.item::isOpen));
   private final ColorSetting text = this.add(new ColorSetting("Text", new Color(255, 255, 255, 255), this.item::isOpen));
   public final BooleanSetting pearl = this.add(new BooleanSetting("PearlOwner", true));

   // 下界传送门方块缓存（每 1s 重扫一次，避免每帧扫描开销）
   private final Timer portalTimer = new Timer();
   private final List<BlockPos> netherPortals = new ArrayList<>();

   public ESP() {
      super("ESP", Module.Category.Render);
      this.setChinese("透视");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (this.item.getValue()) {
         for (Entity entity : Forever.THREAD.getEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
               int itemCount = itemEntity.getStack().getCount();
               String s = this.count.getValue() && itemCount > 1 ? " x" + itemCount : "";
               String name = (this.customName.getValue() ? itemEntity.getStack().getName() : itemEntity.getStack().getItem().getName()).getString();
               Render3DUtil.drawText3D(
                  name + s,
                  ((IEntity)itemEntity)
                     .getDimensions()
                     .getBoxAt(
                        new Vec3d(
                           MathUtil.interpolate(itemEntity.lastRenderX, itemEntity.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                           MathUtil.interpolate(itemEntity.lastRenderY, itemEntity.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                           MathUtil.interpolate(itemEntity.lastRenderZ, itemEntity.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true))
                        )
                     )
                     .expand(0.0, 0.1, 0.0)
                     .getCenter()
                     .add(0.0, 0.5, 0.0),
                  this.text.getValue()
               );
            }
         }
      }

      if (this.box.getValue()) {
         if (this.itemFill.booleanValue || this.playerFill.booleanValue) {
            for (Entity entityx : Forever.THREAD.getEntities()) {
               if (!(entityx instanceof ItemEntity) || !this.itemFill.booleanValue && !this.itemOutline.booleanValue) {
                  if (entityx instanceof PlayerEntity && (this.playerFill.booleanValue || this.playerOutline.booleanValue)) {
                     Color color = this.playerFill.getValue();
                     Render3DUtil.draw3DBox(
                        matrixStack,
                        ((IEntity)entityx)
                           .getDimensions()
                           .getBoxAt(
                              new Vec3d(
                                 MathUtil.interpolate(entityx.lastRenderX, entityx.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                                 MathUtil.interpolate(entityx.lastRenderY, entityx.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                                 MathUtil.interpolate(entityx.lastRenderZ, entityx.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true))
                              )
                           )
                           .expand(0.0, 0.1, 0.0),
                        color,
                        this.playerOutline.getValue(),
                        this.playerOutline.booleanValue,
                        this.playerFill.booleanValue
                     );
                  }
               } else {
                  Color color = this.itemFill.getValue();
                  Render3DUtil.draw3DBox(
                     matrixStack,
                     ((IEntity)entityx)
                        .getDimensions()
                        .getBoxAt(
                           new Vec3d(
                              MathUtil.interpolate(entityx.lastRenderX, entityx.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                              MathUtil.interpolate(entityx.lastRenderY, entityx.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                              MathUtil.interpolate(entityx.lastRenderZ, entityx.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true))
                           )
                        ),
                     color,
                     this.itemOutline.getValue(),
                     this.itemOutline.booleanValue,
                     this.itemFill.booleanValue
                  );
               }
            }
         }

         for (BlockEntity blockEntity : BlockUtil.getTileEntities()) {
            if (!(blockEntity instanceof ChestBlockEntity) || !this.chestFill.booleanValue && !this.chestOutline.booleanValue) {
               if (!(blockEntity instanceof EnderChestBlockEntity) || !this.enderChestFill.booleanValue && !this.enderChestOutline.booleanValue) {
                  if (!(blockEntity instanceof ShulkerBoxBlockEntity) || !this.shulkerBoxFill.booleanValue && !this.shulkerBoxOutline.booleanValue) {
                     if (blockEntity instanceof EndPortalBlockEntity && (this.endPortalFill.booleanValue || this.endPortalOutline.booleanValue)) {
                        Box box = new Box(blockEntity.getPos());
                        Render3DUtil.draw3DBox(
                           matrixStack,
                           box,
                           this.endPortalFill.getValue(),
                           this.endPortalOutline.getValue(),
                           this.endPortalOutline.booleanValue,
                           this.endPortalFill.booleanValue
                        );
                     }
                  } else {
                     Box box = new Box(blockEntity.getPos());
                     Render3DUtil.draw3DBox(
                        matrixStack,
                        box,
                        this.shulkerBoxFill.getValue(),
                        this.shulkerBoxOutline.getValue(),
                        this.shulkerBoxOutline.booleanValue,
                        this.shulkerBoxFill.booleanValue
                     );
                  }
               } else {
                  Box box = new Box(blockEntity.getPos());
                  Render3DUtil.draw3DBox(
                     matrixStack,
                     box,
                     this.enderChestFill.getValue(),
                     this.enderChestOutline.getValue(),
                     this.enderChestOutline.booleanValue,
                     this.enderChestFill.booleanValue
                  );
               }
            } else {
               Box box = new Box(blockEntity.getPos());
               Render3DUtil.draw3DBox(
                  matrixStack, box, this.chestFill.getValue(), this.chestOutline.getValue(), this.chestOutline.booleanValue, this.chestFill.booleanValue
               );
            }
         }

         // 下界传送门（方块，非实体）：以玩家为中心扫描，每 1s 重扫缓存
         if (this.netherPortalFill.booleanValue || this.netherPortalOutline.booleanValue) {
            if (this.portalTimer.passedMs(1000)) {
               this.portalTimer.reset();
               this.netherPortals.clear();
               int r = this.portalRange.getValueInt();
               BlockPos playerPos = mc.player.getBlockPos();
               for (int x = -r; x <= r; x++) {
                  for (int y = -r; y <= r; y++) {
                     for (int z = -r; z <= r; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        if (mc.world.getBlockState(pos).isOf(Blocks.NETHER_PORTAL)) {
                           this.netherPortals.add(pos);
                        }
                     }
                  }
               }
            }
            for (BlockPos pos : this.netherPortals) {
               Render3DUtil.draw3DBox(
                  matrixStack,
                  new Box(pos),
                  this.netherPortalFill.getValue(),
                  this.netherPortalOutline.getValue(),
                  this.netherPortalOutline.booleanValue,
                  this.netherPortalFill.booleanValue
               );
            }
         }
      }
   }

   @EventListener
   public void onReceivePacket(EntitySpawnedEvent event) {
      if (!nullCheck()) {
         if (this.pearl.getValue() && event.getEntity() instanceof EnderPearlEntity pearlEntity) {
            if (pearlEntity.getOwner() != null) {
               pearlEntity.setCustomName(pearlEntity.getOwner().getName());
               pearlEntity.setCustomNameVisible(true);
            } else {
               mc.world
                  .getPlayers()
                  .stream()
                  .min(
                     Comparator.comparingDouble(
                        p -> p.getPos().distanceTo(new Vec3d(pearlEntity.getX(), pearlEntity.getY(), pearlEntity.getZ()))
                     )
                  )
                  .ifPresent(player -> {
                     pearlEntity.setCustomName(player.getName());
                     pearlEntity.setCustomNameVisible(true);
                  });
            }
         }
      }
   }
}
