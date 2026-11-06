package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.EntitySpawnedEvent;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.api.utils.render.Render3DUtil;
import dev.g255.arc.api.utils.render.TextUtil;
import dev.g255.arc.api.utils.world.BlockUtil;
import dev.g255.arc.asm.accessors.IEntity;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
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
   public final EnumSetting<RenderMode> mode;
   private final BooleanSetting twoDOutline;
   private final BooleanSetting twoDHealth;
   private final ColorSetting hHealth;
   private final ColorSetting mHealth;
   private final ColorSetting lHealth;
   private final BooleanSetting twoDArmorDura;
   private final ColorSetting armorDuraColor;
   private final SliderSetting duraScale;
   public final BooleanSetting target;
   public final BooleanSetting self;
   private final ColorSetting endPortalFill;
   private final ColorSetting endPortalOutline;
   private final ColorSetting itemFill;
   private final ColorSetting itemOutline;
   private final ColorSetting playerFill;
   private final ColorSetting playerOutline;
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
      this.mode = this.add(new EnumSetting("Mode", ESP.RenderMode.ThreeD));
      this.twoDOutline = this.add(new BooleanSetting("2DOutline", true, () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.twoDHealth = this.add(new BooleanSetting("2DHealth", true, () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.hHealth = this.add(new ColorSetting("HighHealth", new Color(0, 255, 0, 255), () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.mHealth = this.add(new ColorSetting("MidHealth", new Color(255, 255, 0, 255), () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.lHealth = this.add(new ColorSetting("LowHealth", new Color(255, 0, 0, 255), () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.twoDArmorDura = this.add(new BooleanSetting("2DArmorDura", true, () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.armorDuraColor = this.add(new ColorSetting("ArmorDuraColor", new Color(3145472), () -> this.mode.getValue() == ESP.RenderMode.TwoD && this.twoDArmorDura.getValue()));
      this.duraScale = this.add(new SliderSetting("DuraScale", (double)1.0F, (double)0.0F, (double)2.0F, 0.1, () -> this.mode.getValue() == ESP.RenderMode.TwoD && this.twoDArmorDura.getValue()));
      this.target = this.add((new BooleanSetting("Target", true)).setParent());
      BooleanSetting var10006 = this.target;
      Objects.requireNonNull(var10006);
      this.self = this.add(new BooleanSetting("Self", false, var10006::isOpen));
      this.endPortalFill = this.add((new ColorSetting("EndPortalFill", new Color(255, 243, 129, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())).injectBoolean(false));
      this.endPortalOutline = this.add((new ColorSetting("EndPortalOutline", new Color(255, 243, 129, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())).injectBoolean(false));
      Color var10005 = new Color(255, 255, 255, 100);
      var10006 = this.target;
      Objects.requireNonNull(var10006);
      this.itemFill = this.add((new ColorSetting("ItemFill", var10005, var10006::isOpen)).injectBoolean(true));
      var10005 = new Color(255, 255, 255, 100);
      var10006 = this.target;
      Objects.requireNonNull(var10006);
      this.itemOutline = this.add((new ColorSetting("ItemOutline", var10005, var10006::isOpen)).injectBoolean(true));
      var10005 = new Color(255, 255, 255, 100);
      var10006 = this.target;
      Objects.requireNonNull(var10006);
      this.playerFill = this.add((new ColorSetting("PlayerFill", var10005, var10006::isOpen)).injectBoolean(true));
      var10005 = new Color(255, 255, 255, 100);
      var10006 = this.target;
      Objects.requireNonNull(var10006);
      this.playerOutline = this.add((new ColorSetting("PlayerOutline", var10005, var10006::isOpen)).injectBoolean(true));
      this.chestFill = this.add((new ColorSetting("ChestFill", new Color(255, 198, 123, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())).injectBoolean(false));
      this.chestOutline = this.add((new ColorSetting("ChestOutline", new Color(255, 198, 123, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())).injectBoolean(false));
      this.enderChestFill = this.add((new ColorSetting("EnderChestFill", new Color(255, 100, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())).injectBoolean(false));
      this.enderChestOutline = this.add((new ColorSetting("EnderChestOutline", new Color(255, 100, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())).injectBoolean(false));
      this.shulkerBoxFill = this.add((new ColorSetting("ShulkerBoxFill", new Color(15, 255, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())).injectBoolean(false));
      this.shulkerBoxOutline = this.add((new ColorSetting("ShulkerBoxOutline", new Color(15, 255, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())).injectBoolean(false));
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

   public void onRender3D(MatrixStack matrixStack) {
      if (this.mode.getValue() != ESP.RenderMode.TwoD) {
         if (this.item.getValue()) {
            for(Entity entity : Arc.THREAD.getEntities()) {
               if (entity instanceof ItemEntity) {
                  ItemEntity itemEntity = (ItemEntity)entity;
                  int itemCount = itemEntity.getStack().getCount();
                  String s = this.count.getValue() && itemCount > 1 ? " x" + itemCount : "";
                  String name = (this.customName.getValue() ? itemEntity.getStack().getName() : itemEntity.getStack().getItem().getName()).getString();
                  Render3DUtil.drawText3D(name + s, ((IEntity)itemEntity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(itemEntity.lastRenderX, itemEntity.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(itemEntity.lastRenderY, itemEntity.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)), MathUtil.interpolate(itemEntity.lastRenderZ, itemEntity.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true)))).expand((double)0.0F, 0.1, (double)0.0F).getCenter().add((double)0.0F, (double)0.5F, (double)0.0F), this.text.getValue());
               }
            }
         }

         if (this.target.getValue()) {
            if (this.itemFill.booleanValue || this.playerFill.booleanValue) {
               for(Entity entity : Arc.THREAD.getEntities()) {
                  if (!(entity instanceof ItemEntity) || !this.itemFill.booleanValue && !this.itemOutline.booleanValue) {
                     if ((entity != mc.player || this.self.getValue() && !mc.options.getPerspective().isFirstPerson()) && entity instanceof PlayerEntity && (this.playerFill.booleanValue || this.playerOutline.booleanValue)) {
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
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (this.mode.getValue() == ESP.RenderMode.TwoD) {
         if (!nullCheck()) {
            if (this.target.getValue()) {
               for(Entity entity : Arc.THREAD.getEntities()) {
                  if (entity instanceof ItemEntity) {
                     boolean outline = this.itemOutline.booleanValue;
                     boolean fill = this.itemFill.booleanValue;
                     if (outline || fill) {
                        this.draw2DForEntity(drawContext.getMatrices(), entity, this.itemOutline.getValue(), this.itemFill.getValue(), outline, fill, tickDelta, false);
                     }
                  } else if ((entity != mc.player || this.self.getValue() && !mc.options.getPerspective().isFirstPerson()) && entity instanceof PlayerEntity) {
                     boolean outline = this.playerOutline.booleanValue;
                     boolean fill = this.playerFill.booleanValue;
                     if (outline || fill) {
                        this.draw2DForEntity(drawContext.getMatrices(), entity, this.playerOutline.getValue(), this.playerFill.getValue(), outline, fill, tickDelta, true);
                     }
                  }
               }
            }

            if (this.item.getValue()) {
               for(Entity entity : Arc.THREAD.getEntities()) {
                  if (entity instanceof ItemEntity) {
                     this.draw2DItemText(drawContext, (ItemEntity)entity, tickDelta);
                  }
               }
            }

         }
      }
   }

   private void draw2DForEntity(MatrixStack matrices, Entity entity, Color outlineColor, Color fillColor, boolean outline, boolean fill, float tickDelta, boolean isPlayer) {
      double[] box = this.getEntity2DBox(entity, tickDelta);
      if (box != null) {
         float minX = (float)box[0];
         float minY = (float)box[1];
         float maxX = (float)box[2];
         float maxY = (float)box[3];
         if (!(maxX - minX <= 0.0F) && !(maxY - minY <= 0.0F)) {
            if (fill) {
               Render2DUtil.drawRect(matrices, minX, minY, maxX - minX, maxY - minY, fillColor);
            }

            if (outline) {
               this.draw2DBox(matrices, minX, minY, maxX, maxY, outlineColor);
            }

            if (this.twoDHealth.getValue() && entity instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)entity;
               if (living.getMaxHealth() > 0.0F && living.getHealth() > 0.0F) {
                  this.draw2DHealthBar(matrices, minX, minY, maxY, living);
               }
            }

            if (isPlayer && this.twoDArmorDura.getValue() && entity instanceof PlayerEntity) {
               this.draw2DArmorDura(matrices, maxX, minY, maxY, (PlayerEntity)entity);
            }

         }
      }
   }

   private void draw2DBox(MatrixStack matrices, float posX, float posY, float endPosX, float endPosY, Color col) {
      if (this.twoDOutline.getValue()) {
         Render2DUtil.drawRect(matrices, posX - 1.0F, posY, 1.5F, endPosY - posY + 0.5F, Color.BLACK);
         Render2DUtil.drawRect(matrices, posX - 1.0F, posY - 0.5F, endPosX - posX + 1.5F, 1.5F, Color.BLACK);
         Render2DUtil.drawRect(matrices, endPosX - 1.0F, posY, 1.5F, endPosY - posY + 0.5F, Color.BLACK);
         Render2DUtil.drawRect(matrices, posX - 1.0F, endPosY - 1.0F, endPosX - posX + 1.5F, 1.5F, Color.BLACK);
      }

      Render2DUtil.drawRect(matrices, posX - 0.5F, posY, 0.5F, endPosY - posY, col);
      Render2DUtil.drawRect(matrices, posX, endPosY - 0.5F, endPosX - posX, 0.5F, col);
      Render2DUtil.drawRect(matrices, posX - 0.5F, posY, endPosX - posX + 0.5F, 0.5F, col);
      Render2DUtil.drawRect(matrices, endPosX - 0.5F, posY, 0.5F, endPosY - posY, col);
   }

   private void draw2DHealthBar(MatrixStack matrices, float minX, float minY, float maxY, LivingEntity living) {
      float height = maxY - minY;
      float health = living.getHealth();
      float maxHealth = living.getMaxHealth();
      float ratio = Math.max(0.0F, Math.min(1.0F, health / maxHealth));
      Render2DUtil.drawRect(matrices, minX - 4.0F, minY, 1.0F, height, Color.BLACK);
      Render2DUtil.drawRect(matrices, minX - 4.0F, maxY - height * ratio, 1.0F, height * ratio, this.getHealthColor(health));
   }

   private Color getHealthColor(float health) {
      if (health >= 20.0F) {
         return this.hHealth.getValue();
      } else {
         return health > 10.0F ? this.mHealth.getValue() : this.lHealth.getValue();
      }
   }

   private void draw2DArmorDura(MatrixStack matrices, float maxX, float minY, float maxY, PlayerEntity player) {
      float height = maxY - minY;
      float piece = height / 4.0F;
      ArrayList<ItemStack> stacks = new ArrayList();
      stacks.add((ItemStack)player.getInventory().armor.get(3));
      stacks.add((ItemStack)player.getInventory().armor.get(2));
      stacks.add((ItemStack)player.getInventory().armor.get(1));
      stacks.add((ItemStack)player.getInventory().armor.get(0));

      for(int i = 0; i < stacks.size(); ++i) {
         ItemStack armor = (ItemStack)stacks.get(i);
         if (!armor.isEmpty() && armor.getMaxDamage() > 0) {
            float ratio = (float)(armor.getMaxDamage() - armor.getDamage()) / (float)armor.getMaxDamage();
            ratio = Math.max(0.0F, Math.min(1.0F, ratio));
            float segmentTop = minY + piece * (float)i;
            float segmentBottom = segmentTop + piece;
            float filled = piece * ratio * this.duraScale.getValueFloat();
            filled = Math.min(piece, filled);
            Render2DUtil.drawRect(matrices, maxX + 1.5F, segmentBottom - filled, 1.5F, filled, this.armorDuraColor.getValue());
         }
      }

   }

   private double[] getEntity2DBox(Entity ent, float tickDelta) {
      double x = MathUtil.interpolate(ent.lastRenderX, ent.getX(), (double)tickDelta);
      double y = MathUtil.interpolate(ent.lastRenderY, ent.getY(), (double)tickDelta);
      double z = MathUtil.interpolate(ent.lastRenderZ, ent.getZ(), (double)tickDelta);
      Box axisAlignedBB2 = ent.getBoundingBox();
      Box axisAlignedBB = new Box(axisAlignedBB2.minX - ent.getX() + x - 0.05, axisAlignedBB2.minY - ent.getY() + y, axisAlignedBB2.minZ - ent.getZ() + z - 0.05, axisAlignedBB2.maxX - ent.getX() + x + 0.05, axisAlignedBB2.maxY - ent.getY() + y + 0.15, axisAlignedBB2.maxZ - ent.getZ() + z + 0.05);
      Vec3d[] vectors = new Vec3d[]{new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)};
      double minX = Double.POSITIVE_INFINITY;
      double minY = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double maxY = Double.NEGATIVE_INFINITY;
      boolean found = false;

      for(Vec3d vector : vectors) {
         Vec3d screen = TextUtil.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
         if (screen.z > (double)0.0F && screen.z < (double)1.0F) {
            found = true;
            minX = Math.min(screen.x, minX);
            minY = Math.min(screen.y, minY);
            maxX = Math.max(screen.x, maxX);
            maxY = Math.max(screen.y, maxY);
         }
      }

      if (!found) {
         return null;
      } else {
         return new double[]{minX, minY, maxX, maxY};
      }
   }

   private void draw2DItemText(DrawContext drawContext, ItemEntity itemEntity, float tickDelta) {
      double[] box = this.getEntity2DBox(itemEntity, tickDelta);
      if (box != null) {
         int itemCount = itemEntity.getStack().getCount();
         String s = this.count.getValue() && itemCount > 1 ? " x" + itemCount : "";
         String name = (this.customName.getValue() ? itemEntity.getStack().getName() : itemEntity.getStack().getItem().getName()).getString();
         String text = name + s;
         int width = mc.textRenderer.getWidth(text);
         float x = (float)(box[0] + (box[2] - box[0]) / (double)2.0F - (double)width / (double)2.0F);
         drawContext.drawText(mc.textRenderer, text, (int)x, (int)(box[1] - (double)10.0F), this.text.getValue().getRGB(), false);
      }
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

   private static enum RenderMode {
      ThreeD,
      TwoD;

      // $FF: synthetic method
      private static RenderMode[] $values() {
         return new RenderMode[]{ThreeD, TwoD};
      }
   }
}
