package dev.g255.arc.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.g255.arc.Arc;
import dev.g255.arc.api.utils.math.MathUtil;
import dev.g255.arc.api.utils.render.TextUtil;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;
import org.joml.Vector4d;

public class TwoDESP extends Module {
   public static TwoDESP INSTANCE;
   private final EnumSetting<TwoDESPMode> page;
   public final ColorSetting armorDuraColor;
   public final ColorSetting hHealth;
   public final ColorSetting mHealth;
   public final ColorSetting lHealth;
   private final BooleanSetting outline;
   private final BooleanSetting renderHealth;
   private final BooleanSetting renderArmor;
   private final BooleanSetting renderEffects;
   private final SliderSetting durascale;
   private final BooleanSetting drawItem;
   private final BooleanSetting drawItemC;
   public final ColorSetting countColor;
   public final ColorSetting textcolor;
   private final BooleanSetting font;
   private final BooleanSetting players;
   private final BooleanSetting friends;
   private final BooleanSetting crystals;
   private final BooleanSetting creatures;
   private final BooleanSetting monsters;
   private final BooleanSetting ambients;
   private final BooleanSetting others;
   private final ColorSetting playersC;
   private final ColorSetting friendsC;
   private final ColorSetting crystalsC;
   private final ColorSetting creaturesC;
   private final ColorSetting monstersC;
   private final ColorSetting ambientsC;
   private final ColorSetting othersC;

   public TwoDESP() {
      super("TwoDESP", Module.Category.Render);
      this.page = this.add(new EnumSetting("Settings", TwoDESP.TwoDESPMode.Target));
      this.armorDuraColor = this.add(new ColorSetting("Armor Dura Color", new Color(3145472), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color));
      this.hHealth = this.add(new ColorSetting("High Health Color", new Color(0, 255, 0, 255), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color));
      this.mHealth = this.add(new ColorSetting("Mid Health Color", new Color(255, 255, 0, 255), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color));
      this.lHealth = this.add(new ColorSetting("Low Health Color", new Color(255, 0, 0, 255), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color));
      this.outline = this.add(new BooleanSetting("Outline", true, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Setting));
      this.renderHealth = this.add(new BooleanSetting("renderHealth", true, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Setting));
      this.renderArmor = this.add(new BooleanSetting("Armor Dura", true, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Setting));
      this.renderEffects = this.add(new BooleanSetting("Effects", true, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Setting));
      this.durascale = this.add(new SliderSetting("DuraScale", (double)1.0F, (double)0.0F, (double)2.0F, 0.1, () -> this.renderArmor.getValue()));
      this.drawItem = this.add(new BooleanSetting("draw Item Name", true, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Setting));
      this.drawItemC = this.add(new BooleanSetting("draw Item Count", true, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Setting && this.drawItem.getValue()));
      this.countColor = this.add(new ColorSetting("Item Count Color", new Color(255, 255, 0, 255), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color && this.drawItemC.getValue()));
      this.textcolor = this.add(new ColorSetting("Item Name Color", new Color(155, 123, 255, 255), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color && this.drawItem.getValue()));
      this.font = this.add(new BooleanSetting("CustomFont", true, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Setting));
      this.players = this.add(new BooleanSetting("Players", true, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Target));
      this.friends = this.add(new BooleanSetting("Friends", true, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Target));
      this.crystals = this.add(new BooleanSetting("Crystals", true, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Target));
      this.creatures = this.add(new BooleanSetting("Creatures", false, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Target));
      this.monsters = this.add(new BooleanSetting("Monsters", false, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Target));
      this.ambients = this.add(new BooleanSetting("Ambients", false, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Target));
      this.others = this.add(new BooleanSetting("Others", false, () -> this.page.getValue() == TwoDESP.TwoDESPMode.Target));
      this.playersC = this.add(new ColorSetting("PlayersBox", new Color(16749056), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color));
      this.friendsC = this.add(new ColorSetting("FriendsBox", new Color(3211008), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color));
      this.crystalsC = this.add(new ColorSetting("CrystalsBox", new Color(48127), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color));
      this.creaturesC = this.add(new ColorSetting("CreaturesBox", new Color(10527910), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color));
      this.monstersC = this.add(new ColorSetting("MonstersBox", new Color(16711680), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color));
      this.ambientsC = this.add(new ColorSetting("AmbientsBox", new Color(8061183), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color));
      this.othersC = this.add(new ColorSetting("OthersBox", new Color(16711778), () -> this.page.getValue() == TwoDESP.TwoDESPMode.Color));
      this.setChinese("2D透视");
      INSTANCE = this;
   }

   public void onRender2D(DrawContext context, float tickDelta) {
      if (!nullCheck()) {
         Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder bufferBuilder = null;
         boolean hasVertices = false;

         for(Entity ent : Arc.THREAD.getEntities()) {
            if (this.shouldRender(ent)) {
               if (bufferBuilder == null) {
                  bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
               }

               if (this.drawBox(bufferBuilder, ent, matrix, context)) {
                  hasVertices = true;
               }
            }
         }

         if (hasVertices && bufferBuilder != null) {
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }

         RenderSystem.disableBlend();

         for(Entity ent : Arc.THREAD.getEntities()) {
            if (this.shouldRender(ent)) {
               this.drawText(ent, context);
            }
         }

      }
   }

   public boolean shouldRender(Entity entity) {
      if (entity == null) {
         return false;
      } else if (mc.player == null) {
         return false;
      } else if (entity instanceof PlayerEntity) {
         if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) {
            return false;
         } else {
            return Arc.FRIEND.isFriend((PlayerEntity)entity) ? this.friends.getValue() : this.players.getValue();
         }
      } else if (entity instanceof EndCrystalEntity) {
         return this.crystals.getValue();
      } else {
         boolean var10000;
         switch (entity.getType().getSpawnGroup()) {
            case CREATURE:
            case WATER_CREATURE:
               var10000 = this.creatures.getValue();
               break;
            case MONSTER:
               var10000 = this.monsters.getValue();
               break;
            case AMBIENT:
            case WATER_AMBIENT:
               var10000 = this.ambients.getValue();
               break;
            default:
               var10000 = this.others.getValue();
         }

         return var10000;
      }
   }

   public Color getEntityColor(Entity entity) {
      if (entity == null) {
         return new Color(31743);
      } else if (entity instanceof PlayerEntity) {
         return Arc.FRIEND.isFriend((PlayerEntity)entity) ? this.friendsC.getValue() : this.playersC.getValue();
      } else if (entity instanceof EndCrystalEntity) {
         return this.crystalsC.getValue();
      } else {
         Color var10000;
         switch (entity.getType().getSpawnGroup()) {
            case CREATURE:
            case WATER_CREATURE:
               var10000 = this.creaturesC.getValue();
               break;
            case MONSTER:
               var10000 = this.monstersC.getValue();
               break;
            case AMBIENT:
            case WATER_AMBIENT:
               var10000 = this.ambientsC.getValue();
               break;
            default:
               var10000 = this.othersC.getValue();
         }

         return var10000;
      }
   }

   public boolean drawBox(BufferBuilder bufferBuilder, Entity ent, Matrix4f matrix, DrawContext context) {
      double x = MathUtil.interpolate(ent.lastRenderX, ent.getX(), (double)mc.getRenderTickCounter().getTickDelta(true));
      double y = MathUtil.interpolate(ent.lastRenderY, ent.getY(), (double)mc.getRenderTickCounter().getTickDelta(true));
      double z = MathUtil.interpolate(ent.lastRenderZ, ent.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true));
      Box axisAlignedBB2 = ent.getBoundingBox();
      Box axisAlignedBB = new Box(axisAlignedBB2.minX - ent.getX() + x - 0.05, axisAlignedBB2.minY - ent.getY() + y, axisAlignedBB2.minZ - ent.getZ() + z - 0.05, axisAlignedBB2.maxX - ent.getX() + x + 0.05, axisAlignedBB2.maxY - ent.getY() + y + 0.15, axisAlignedBB2.maxZ - ent.getZ() + z + 0.05);
      Vec3d[] vectors = new Vec3d[]{new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)};
      Color col = this.getEntityColor(ent);
      Vector4d position = null;

      for(Vec3d vector : vectors) {
         vector = TextUtil.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
         if (vector.z > (double)0.0F && vector.z < (double)1.0F) {
            if (position == null) {
               position = new Vector4d(vector.x, vector.y, vector.z, (double)0.0F);
            }

            position.x = Math.min(vector.x, position.x);
            position.y = Math.min(vector.y, position.y);
            position.z = Math.max(vector.x, position.z);
            position.w = Math.max(vector.y, position.w);
         }
      }

      if (position == null) {
         return false;
      } else {
         double posX = position.x;
         double posY = position.y;
         double endPosX = position.z;
         double endPosY = position.w;
         if (this.outline.getValue()) {
            this.drawRectToBuffer(bufferBuilder, matrix, (float)(posX - (double)1.0F), (float)posY, 1.5F, (float)(endPosY - posY + (double)0.5F), Color.BLACK);
            this.drawRectToBuffer(bufferBuilder, matrix, (float)(posX - (double)1.0F), (float)(posY - (double)0.5F), (float)(endPosX - posX + (double)1.5F), 1.5F, Color.BLACK);
            this.drawRectToBuffer(bufferBuilder, matrix, (float)(endPosX - (double)1.0F), (float)posY, 1.5F, (float)(endPosY - posY + (double)0.5F), Color.BLACK);
            this.drawRectToBuffer(bufferBuilder, matrix, (float)(posX - (double)1.0F), (float)(endPosY - (double)1.0F), (float)(endPosX - posX + (double)1.5F), 1.5F, Color.BLACK);
            this.drawRectToBuffer(bufferBuilder, matrix, (float)(posX - (double)0.5F), (float)posY, 0.5F, (float)(endPosY - posY), col);
            this.drawRectToBuffer(bufferBuilder, matrix, (float)posX, (float)(endPosY - (double)0.5F), (float)(endPosX - posX), 0.5F, col);
            this.drawRectToBuffer(bufferBuilder, matrix, (float)(posX - (double)0.5F), (float)posY, (float)(endPosX - posX + (double)0.5F), 0.5F, col);
            this.drawRectToBuffer(bufferBuilder, matrix, (float)(endPosX - (double)0.5F), (float)posY, 0.5F, (float)(endPosY - posY), col);
         }

         LivingEntity lent;
         if (ent instanceof LivingEntity && (lent = (LivingEntity)ent).getHealth() != 0.0F && this.renderHealth.getValue()) {
            this.drawRectToBuffer(bufferBuilder, matrix, (float)(posX - (double)4.0F), (float)posY, 1.0F, (float)(endPosY - posY), Color.BLACK);
            Color color = this.getcolor(lent.getHealth());
            float healthHeight = (float)(endPosY - (endPosY + (posY - endPosY) * (double)lent.getHealth() / (double)lent.getMaxHealth()));
            this.drawRectToBuffer(bufferBuilder, matrix, (float)(posX - (double)4.0F), (float)(endPosY - (double)healthHeight), 1.0F, healthHeight, color);
         }

         if (ent instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)ent;
            if (this.renderArmor.getValue()) {
               double height = (endPosY - posY) / (double)4.0F;
               ArrayList<ItemStack> stacks = new ArrayList();
               stacks.add((ItemStack)player.getInventory().armor.get(3));
               stacks.add((ItemStack)player.getInventory().armor.get(2));
               stacks.add((ItemStack)player.getInventory().armor.get(1));
               stacks.add((ItemStack)player.getInventory().armor.get(0));
               int i = -1;

               for(ItemStack armor : stacks) {
                  ++i;
                  if (!armor.isEmpty()) {
                     float durability = (float)(armor.getMaxDamage() - armor.getDamage());
                     int percent = (int)(durability / (float)armor.getMaxDamage() * 100.0F);
                     double finalH = height * (double)(percent / 100) * (double)this.durascale.getValueFloat();
                     finalH = Math.min(height, finalH);
                     this.drawRectToBuffer(bufferBuilder, matrix, (float)(endPosX + (double)1.5F), (float)((double)((float)posY) + height * (double)i + 1.2 * (double)(i + 1)), 1.5F, (float)finalH, this.armorDuraColor.getValue());
                  }
               }
            }
         }

         return true;
      }
   }

   private void drawRectToBuffer(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float width, float height, Color color) {
      float a = (float)color.getAlpha() / 255.0F;
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float b = (float)color.getBlue() / 255.0F;
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a);
      bufferBuilder.vertex(matrix, x, y + height, 0.0F).color(r, g, b, a);
      bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).color(r, g, b, a);
      bufferBuilder.vertex(matrix, x + width, y, 0.0F).color(r, g, b, a);
   }

   public void drawText(Entity ent, DrawContext context) {
      double x = MathUtil.interpolate(ent.lastRenderX, ent.getX(), (double)mc.getRenderTickCounter().getTickDelta(true));
      double y = MathUtil.interpolate(ent.lastRenderY, ent.getY(), (double)mc.getRenderTickCounter().getTickDelta(true));
      double z = MathUtil.interpolate(ent.lastRenderZ, ent.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true));
      Box axisAlignedBB2 = ent.getBoundingBox();
      Box axisAlignedBB = new Box(axisAlignedBB2.minX - ent.getX() + x - 0.05, axisAlignedBB2.minY - ent.getY() + y, axisAlignedBB2.minZ - ent.getZ() + z - 0.05, axisAlignedBB2.maxX - ent.getX() + x + 0.05, axisAlignedBB2.maxY - ent.getY() + y + 0.15, axisAlignedBB2.maxZ - ent.getZ() + z + 0.05);
      Vec3d[] vectors = new Vec3d[]{new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)};
      Vector4d position = null;

      for(Vec3d vector : vectors) {
         vector = TextUtil.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
         if (vector.z > (double)0.0F && vector.z < (double)1.0F) {
            if (position == null) {
               position = new Vector4d(vector.x, vector.y, vector.z, (double)0.0F);
            }

            position.x = Math.min(vector.x, position.x);
            position.y = Math.min(vector.y, position.y);
            position.z = Math.max(vector.x, position.z);
            position.w = Math.max(vector.y, position.w);
         }
      }

      if (position != null) {
         double posX = position.x;
         double posY = position.y;
         double endPosX = position.z;
         double endPosY = position.w;
         if (ent instanceof ItemEntity) {
            ItemEntity entity = (ItemEntity)ent;
            if (this.drawItem.getValue()) {
               float diff = (float)((endPosX - posX) / (double)2.0F);
               float textWidth = FontManager.ui.getWidth(entity.getDisplayName().getString());
               float tagX = (float)(posX + (double)diff - (double)(textWidth / 2.0F));
               int count = entity.getStack().getCount();
               context.drawText(mc.textRenderer, entity.getDisplayName().getString(), (int)tagX, (int)(posY - (double)10.0F), this.textcolor.getValue().getRGB(), false);
               if (this.drawItemC.getValue()) {
                  context.drawText(mc.textRenderer, " x" + count, (int)(tagX + (float)mc.textRenderer.getWidth(entity.getDisplayName().getString() + " ")), (int)posY - 10, this.countColor.getValue().getRGB(), false);
               }
            }
         }

         if (ent instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)ent;
            if (this.renderArmor.getValue()) {
               double height = (endPosY - posY) / (double)4.0F;
               ArrayList<ItemStack> stacks = new ArrayList();
               stacks.add((ItemStack)player.getInventory().armor.get(3));
               stacks.add((ItemStack)player.getInventory().armor.get(2));
               stacks.add((ItemStack)player.getInventory().armor.get(1));
               stacks.add((ItemStack)player.getInventory().armor.get(0));
               int i = -1;

               for(ItemStack armor : stacks) {
                  ++i;
                  if (!armor.isEmpty()) {
                     float durability = (float)(armor.getMaxDamage() - armor.getDamage());
                     int percent = (int)(durability / (float)armor.getMaxDamage() * 100.0F);
                     double finalH = height * ((double)percent / (double)100.0F) * (double)this.durascale.getValueFloat();
                     finalH = Math.min(height, finalH);
                     context.drawItem(armor, (int)(endPosX + (double)4.0F), (int)(posY + height * (double)i + 1.2 * (double)(i + 1) + finalH / (double)2.0F));
                  }
               }
            }

            if (this.renderEffects.getValue()) {
               float currentY = (float)posY - 20.0F;

               for(StatusEffectInstance effect : player.getStatusEffects()) {
                  StatusEffect statusEffect = (StatusEffect)effect.getEffectType().value();
                  String effectName = statusEffect.getName().getString();
                  int duration = effect.getDuration();
                  String timeText;
                  if (duration >= 72000) {
                     int hours = duration / 72000;
                     int minutes = duration % 72000 / 1200;
                     timeText = String.format("%dh%dm", hours, minutes);
                  } else if (duration >= 1200) {
                     int minutes = duration / 1200;
                     int seconds = duration % 1200 / 20;
                     timeText = String.format("%dm%ds", minutes, seconds);
                  } else {
                     int seconds = duration / 20;
                     timeText = String.format("%ds", seconds);
                  }

                  String text = effectName + " " + timeText;
                  int color = statusEffect.getColor();
                  float textWidth = this.font.getValue() ? FontManager.ui.getWidth(text) : TextUtil.getWidth(text);
                  float effectX = (float)(posX + (endPosX - posX) / (double)2.0F - (double)textWidth / (double)2.0F);
                  TextUtil.drawString(context, text, (double)effectX, (double)currentY, color, this.font.getValue(), false);
                  currentY -= 12.0F;
               }
            }
         }
      }

   }

   public Color getcolor(float health) {
      if (health >= 20.0F) {
         return this.hHealth.getValue();
      } else {
         return 20.0F > health && health > 10.0F ? this.mHealth.getValue() : this.lHealth.getValue();
      }
   }

   public static enum TwoDESPMode {
      Setting,
      Target,
      Color;

      // $FF: synthetic method
      private static TwoDESPMode[] $values() {
         return new TwoDESPMode[]{Setting, Target, Color};
      }
   }
}
