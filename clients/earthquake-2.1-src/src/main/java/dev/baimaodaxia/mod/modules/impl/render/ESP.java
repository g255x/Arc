package dev.baimaodaxia.mod.modules.impl.render;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.EntitySpawnedEvent;
import dev.baimaodaxia.api.utils.math.MathUtil;
import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.api.utils.render.Render3DUtil;
import dev.baimaodaxia.api.utils.render.TextUtil;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.asm.accessors.IEntity;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import java.awt.Color;
import java.util.Comparator;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class ESP extends Module {
   public static ESP INSTANCE;
   private final EnumSetting<Mode> mode;
   public final BooleanSetting box;
   private final ColorSetting endPortalFill;
   private final ColorSetting endPortalOutline;
   private final ColorSetting itemFill;
   private final ColorSetting itemOutline;
   private final ColorSetting playerFill;
   private final ColorSetting playerOutline;
   private final ColorSetting player2DFill;
   private final ColorSetting player2DOutline;
   private final BooleanSetting tags;
   private final ColorSetting tagText;
   private final ColorSetting tagBg;
   private final BooleanSetting healthBar;
   private final ColorSetting chestFill;
   private final ColorSetting chestOutline;
   private final ColorSetting enderChestFill;
   private final ColorSetting enderChestOutline;
   private final ColorSetting shulkerBoxFill;
   private final ColorSetting shulkerBoxOutline;
   public final BooleanSetting item;
   public final BooleanSetting customName;
   public final BooleanSetting count;
   private final ColorSetting text;
   public final BooleanSetting pearl;

   public ESP() {
      super("ESP", Module.Category.Render);
      this.mode = this.add(new EnumSetting("Mode", ESP.Mode.Box3D));
      this.box = this.add((new BooleanSetting("BoxESP", true)).setParent());
      Color var10005 = new Color(255, 243, 129, 100);
      BooleanSetting var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.endPortalFill = this.add((new ColorSetting("EndPortalFill", var10005, var10006::isOpen)).injectBoolean(false));
      var10005 = new Color(255, 243, 129, 100);
      var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.endPortalOutline = this.add((new ColorSetting("EndPortalOutline", var10005, var10006::isOpen)).injectBoolean(false));
      var10005 = new Color(255, 255, 255, 100);
      var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.itemFill = this.add((new ColorSetting("ItemFill", var10005, var10006::isOpen)).injectBoolean(true));
      var10005 = new Color(255, 255, 255, 100);
      var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.itemOutline = this.add((new ColorSetting("ItemOutline", var10005, var10006::isOpen)).injectBoolean(true));
      var10005 = new Color(255, 255, 255, 100);
      var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.playerFill = this.add((new ColorSetting("PlayerFill", var10005, var10006::isOpen)).injectBoolean(true));
      var10005 = new Color(255, 255, 255, 100);
      var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.playerOutline = this.add((new ColorSetting("PlayerOutline", var10005, var10006::isOpen)).injectBoolean(true));
      this.player2DFill = this.add((new ColorSetting("Player2DFill", new Color(0, 0, 0, 50), () -> this.mode.getValue() != ESP.Mode.Box3D && this.box.isOpen())).injectBoolean(true));
      this.player2DOutline = this.add((new ColorSetting("Player2DOutline", new Color(255, 255, 255, 160), () -> this.mode.getValue() != ESP.Mode.Box3D && this.box.isOpen())).injectBoolean(true));
      this.tags = this.add(new BooleanSetting("Tags", true, () -> this.mode.getValue() != ESP.Mode.Box3D));
      this.tagText = this.add(new ColorSetting("TagText", new Color(255, 255, 255, 255), () -> this.mode.getValue() != ESP.Mode.Box3D && this.tags.getValue()));
      this.tagBg = this.add(new ColorSetting("TagBg", new Color(0, 0, 0, 120), () -> this.mode.getValue() != ESP.Mode.Box3D && this.tags.getValue()));
      this.healthBar = this.add(new BooleanSetting("HealthBar", true, () -> this.mode.getValue() != ESP.Mode.Box3D));
      var10005 = new Color(255, 198, 123, 100);
      var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.chestFill = this.add((new ColorSetting("ChestFill", var10005, var10006::isOpen)).injectBoolean(false));
      var10005 = new Color(255, 198, 123, 100);
      var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.chestOutline = this.add((new ColorSetting("ChestOutline", var10005, var10006::isOpen)).injectBoolean(false));
      var10005 = new Color(255, 100, 255, 100);
      var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.enderChestFill = this.add((new ColorSetting("EnderChestFill", var10005, var10006::isOpen)).injectBoolean(false));
      var10005 = new Color(255, 100, 255, 100);
      var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.enderChestOutline = this.add((new ColorSetting("EnderChestOutline", var10005, var10006::isOpen)).injectBoolean(false));
      var10005 = new Color(15, 255, 255, 100);
      var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.shulkerBoxFill = this.add((new ColorSetting("ShulkerBoxFill", var10005, var10006::isOpen)).injectBoolean(false));
      var10005 = new Color(15, 255, 255, 100);
      var10006 = this.box;
      Objects.requireNonNull(var10006);
      this.shulkerBoxOutline = this.add((new ColorSetting("ShulkerBoxOutline", var10005, var10006::isOpen)).injectBoolean(false));
      this.item = this.add((new BooleanSetting("ItemName", false)).setParent());
      var10006 = this.item;
      Objects.requireNonNull(var10006);
      this.customName = this.add(new BooleanSetting("CustomName", false, var10006::isOpen));
      var10006 = this.item;
      Objects.requireNonNull(var10006);
      this.count = this.add(new BooleanSetting("Count", true, var10006::isOpen));
      var10005 = new Color(255, 255, 255, 255);
      var10006 = this.item;
      Objects.requireNonNull(var10006);
      this.text = this.add(new ColorSetting("Text", var10005, var10006::isOpen));
      this.pearl = this.add(new BooleanSetting("PearlOwner", true));
      this.setChinese("透视");
      INSTANCE = this;
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (!nullCheck()) {
         if ((this.mode.getValue()).render2D && this.box.getValue()) {
            boolean drawBox = this.player2DFill.booleanValue || this.player2DOutline.booleanValue;
            boolean drawTags = this.tags.getValue();
            boolean drawHealthBar = this.healthBar.getValue();
            if (drawBox || drawTags || drawHealthBar) {
               MatrixStack matrices = drawContext.getMatrices();

               for(PlayerEntity player : mc.world.getPlayers()) {
                  if (player != mc.player && !player.isDead()) {
                     Vec3d renderPos = new Vec3d(MathUtil.interpolate(player.lastRenderX, player.getX(), (double)tickDelta), MathUtil.interpolate(player.lastRenderY, player.getY(), (double)tickDelta), MathUtil.interpolate(player.lastRenderZ, player.getZ(), (double)tickDelta));
                     Box box = ((IEntity)player).getDimensions().getBoxAt(renderPos).expand((double)0.0F, 0.1, (double)0.0F);
                     float[] bounds = this.compute2DBounds(box);
                     if (bounds != null) {
                        float x1 = bounds[0];
                        float y1 = bounds[1];
                        float x2 = bounds[2];
                        float y2 = bounds[3];
                        float w = x2 - x1;
                        float h = y2 - y1;
                        if (w > 0.0F && h > 0.0F) {
                           if (drawBox) {
                              this.draw2DBox(matrices, x1, y1, w, h);
                           }

                           if (drawHealthBar) {
                              this.drawHealthBar(matrices, player, x1, y1, h);
                           }

                           if (drawTags) {
                              this.drawTag(drawContext, player, x1, y1, w);
                           }
                        }
                     }
                  }
               }

            }
         }
      }
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (this.item.getValue()) {
         for(Entity entity : Alien.THREAD.getEntities()) {
            if (entity instanceof ItemEntity) {
               ItemEntity itemEntity = (ItemEntity)entity;
               int itemCount = itemEntity.getStack().getCount();
               String s = this.count.getValue() && itemCount > 1 ? " x" + itemCount : "";
               String name = (this.customName.getValue() ? itemEntity.getStack().getName() : itemEntity.getStack().getItem().getName()).getString();
               Render3DUtil.drawText3D(name + s, ((IEntity)itemEntity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(itemEntity.lastRenderX, itemEntity.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(itemEntity.lastRenderY, itemEntity.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(itemEntity.lastRenderZ, itemEntity.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true)))).expand((double)0.0F, 0.1, (double)0.0F).getCenter().add((double)0.0F, (double)0.5F, (double)0.0F), this.text.getValue());
            }
         }
      }

      if (this.box.getValue() && (this.mode.getValue()).render3D) {
         if (this.itemFill.booleanValue || this.playerFill.booleanValue) {
            for(Entity entity : Alien.THREAD.getEntities()) {
               if (!(entity instanceof ItemEntity) || !this.itemFill.booleanValue && !this.itemOutline.booleanValue) {
                  if (entity instanceof PlayerEntity && (this.playerFill.booleanValue || this.playerOutline.booleanValue)) {
                     Color color = this.playerFill.getValue();
                     Render3DUtil.draw3DBox(matrixStack, ((IEntity)entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true)))).expand((double)0.0F, 0.1, (double)0.0F), color, this.playerOutline.getValue(), this.playerOutline.booleanValue, this.playerFill.booleanValue);
                  }
               } else {
                  Color color = this.itemFill.getValue();
                  Render3DUtil.draw3DBox(matrixStack, ((IEntity)entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true)))), color, this.itemOutline.getValue(), this.itemOutline.booleanValue, this.itemFill.booleanValue);
               }
            }
         }

         for(BlockEntity blockEntity : BlockUtil.getTileEntities()) {
            if (!(blockEntity instanceof ChestBlockEntity) || !this.chestFill.booleanValue && !this.chestOutline.booleanValue) {
               if (!(blockEntity instanceof EnderChestBlockEntity) || !this.enderChestFill.booleanValue && !this.enderChestOutline.booleanValue) {
                  if (!(blockEntity instanceof ShulkerBoxBlockEntity) || !this.shulkerBoxFill.booleanValue && !this.shulkerBoxOutline.booleanValue) {
                     if (blockEntity instanceof EndPortalBlockEntity && (this.endPortalFill.booleanValue || this.endPortalOutline.booleanValue)) {
                        Box box = new Box(blockEntity.getPos());
                        Render3DUtil.draw3DBox(matrixStack, box, this.endPortalFill.getValue(), this.endPortalOutline.getValue(), this.endPortalOutline.booleanValue, this.endPortalFill.booleanValue);
                     }
                  } else {
                     Box box = new Box(blockEntity.getPos());
                     Render3DUtil.draw3DBox(matrixStack, box, this.shulkerBoxFill.getValue(), this.shulkerBoxOutline.getValue(), this.shulkerBoxOutline.booleanValue, this.shulkerBoxFill.booleanValue);
                  }
               } else {
                  Box box = new Box(blockEntity.getPos());
                  Render3DUtil.draw3DBox(matrixStack, box, this.enderChestFill.getValue(), this.enderChestOutline.getValue(), this.enderChestOutline.booleanValue, this.enderChestFill.booleanValue);
               }
            } else {
               Box box = new Box(blockEntity.getPos());
               Render3DUtil.draw3DBox(matrixStack, box, this.chestFill.getValue(), this.chestOutline.getValue(), this.chestOutline.booleanValue, this.chestFill.booleanValue);
            }
         }
      }

   }

   private float[] compute2DBounds(Box box) {
      Vec3d[] corners = new Vec3d[]{new Vec3d(box.minX, box.minY, box.minZ), new Vec3d(box.minX, box.minY, box.maxZ), new Vec3d(box.minX, box.maxY, box.minZ), new Vec3d(box.minX, box.maxY, box.maxZ), new Vec3d(box.maxX, box.minY, box.minZ), new Vec3d(box.maxX, box.minY, box.maxZ), new Vec3d(box.maxX, box.maxY, box.minZ), new Vec3d(box.maxX, box.maxY, box.maxZ)};
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      boolean projected = false;

      for(Vec3d corner : corners) {
         Vec3d screen = TextUtil.worldSpaceToScreenSpace(corner);
         double z = screen.z;
         if (!(z < (double)0.0F) && !(z > (double)1.0F)) {
            projected = true;
            float x = (float)screen.x;
            float y = (float)screen.y;
            if (x < minX) {
               minX = x;
            }

            if (y < minY) {
               minY = y;
            }

            if (x > maxX) {
               maxX = x;
            }

            if (y > maxY) {
               maxY = y;
            }
         }
      }

      if (!projected) {
         return null;
      } else {
         return new float[]{minX, minY, maxX, maxY};
      }
   }

   private void draw2DBox(MatrixStack matrices, float x, float y, float w, float h) {
      boolean fill = this.player2DFill.booleanValue;
      boolean outline = this.player2DOutline.booleanValue;
      if (fill && outline) {
         Render2DUtil.drawRectWithOutline(matrices, x, y, w, h, this.player2DFill.getValue(), this.player2DOutline.getValue());
      } else if (fill) {
         Render2DUtil.drawRect(matrices, x, y, w, h, this.player2DFill.getValue());
      } else if (outline) {
         int c = this.player2DOutline.getValue().getRGB();
         Render2DUtil.drawLine(matrices, x, y, x + w, y, c);
         Render2DUtil.drawLine(matrices, x + w, y, x + w, y + h, c);
         Render2DUtil.drawLine(matrices, x + w, y + h, x, y + h, c);
         Render2DUtil.drawLine(matrices, x, y + h, x, y, c);
      }

   }

   private void drawHealthBar(MatrixStack matrices, PlayerEntity player, float x, float y, float height) {
      float maxHealth = player.getMaxHealth();
      if (!(maxHealth <= 0.0F)) {
         float health = MathUtil.clamp(player.getHealth(), 0.0F, maxHealth);
         float ratio = health / maxHealth;
         float barH = height * ratio;
         int bg = ColorUtil.injectAlpha(0, 140);
         Render2DUtil.drawRect(matrices, x - 3.0F, y, 2.0F, height, bg);
         int r = (int)(255.0F * (1.0F - ratio));
         int g = (int)(255.0F * ratio);
         int fg = (new Color(r, g, 0, 255)).getRGB();
         Render2DUtil.drawRect(matrices, x - 3.0F, y + (height - barH), 2.0F, barH, fg);
      }
   }

   private void drawTag(DrawContext drawContext, PlayerEntity player, float x, float y, float w) {
      String text = player.getName().getString();
      float textW = TextUtil.getWidth(text);
      float textH = TextUtil.getHeight();
      float tx = x + w * 0.5F - textW * 0.5F;
      float ty = y - textH - 2.0F;
      Render2DUtil.drawRect(drawContext.getMatrices(), tx - 2.0F, ty - 1.0F, textW + 4.0F, textH + 2.0F, this.tagBg.getValue());
      TextUtil.drawString(drawContext, text, (double)tx, (double)ty, this.tagText.getValue().getRGB(), false, true);
   }

   @EventListener
   public void onReceivePacket(EntitySpawnedEvent event) {
      if (!nullCheck()) {
         Entity entity;
         if (this.pearl.getValue() && (entity = event.getEntity()) instanceof EnderPearlEntity) {
            EnderPearlEntity pearlEntity = (EnderPearlEntity)entity;
            if (pearlEntity.getOwner() != null) {
               pearlEntity.setCustomName(pearlEntity.getOwner().getName());
               pearlEntity.setCustomNameVisible(true);
            } else {
               mc.world.getPlayers().stream().min(Comparator.comparingDouble((p) -> p.getPos().distanceTo(new Vec3d(pearlEntity.getX(), pearlEntity.getY(), pearlEntity.getZ())))).ifPresent((player) -> {
                  pearlEntity.setCustomName(player.getName());
                  pearlEntity.setCustomNameVisible(true);
               });
            }
         }

      }
   }

   private static enum Mode {
      Box3D(true, false),
      Box2D(false, true),
      Both(true, true);

      private final boolean render3D;
      private final boolean render2D;

      private Mode(boolean render3D, boolean render2D) {
         this.render3D = render3D;
         this.render2D = render2D;
      }

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Box3D, Box2D, Both};
      }
   }
}
