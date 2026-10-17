package dev.baimaodaxia.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.Render3DEvent;
import dev.baimaodaxia.api.utils.math.MathUtil;
import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.core.impl.FontManager;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.TextRadar;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.Formatting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class NameTags extends Module {
   public static NameTags INSTANCE;
   final BooleanSetting customFont = this.add(new BooleanSetting("CustomFont", true));
   final ColorSetting colorConfig = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   final ColorSetting friendConfig = this.add((new ColorSetting("Friend", new Color(155, 155, 255))).injectBoolean(true));
   final ColorSetting invisibleConfig = this.add((new ColorSetting("Invisible", new Color(200, 200, 200))).injectBoolean(true));
   final ColorSetting died = this.add((new ColorSetting("Died", new Color(180, 0, 0))).injectBoolean(true));
   final ColorSetting sneakingConfig = this.add((new ColorSetting("Sneaking", new Color(200, 200, 0))).injectBoolean(true));
   final ColorSetting rectConfig = this.add((new ColorSetting("Rectangle", new Color(0, 0, 0, 100))).injectBoolean(true));
   final BooleanSetting armorConfig = this.add((new BooleanSetting("Armor", true)).setParent());
   final BooleanSetting drawItemConfig;
   final SliderSetting offsetConfig;
   final BooleanSetting enchantmentsConfig;
   final BooleanSetting durabilityConfig;
   final BooleanSetting forceBarConfig;
   final BooleanSetting itemNameConfig;
   final BooleanSetting entityIdConfig;
   final BooleanSetting gamemodeConfig;
   final BooleanSetting pingConfig;
   final BooleanSetting healthConfig;
   final BooleanSetting totemsConfig;
   final SliderSetting scaleConfig;
   final BooleanSetting factorConfig;
   final SliderSetting scalingConfig;
   final SliderSetting distanceConfig;
   final SliderSetting heightConfig;
   final DecimalFormat df;

   public NameTags() {
      super("NameTags", "Renders info on player NameTags", Module.Category.Render);
      BooleanSetting var10006 = this.armorConfig;
      Objects.requireNonNull(var10006);
      this.drawItemConfig = this.add(new BooleanSetting("DrawItem", true, var10006::isOpen));
      BooleanSetting var10009 = this.armorConfig;
      Objects.requireNonNull(var10009);
      this.offsetConfig = this.add(new SliderSetting("Offset", (double)-20.0F, (double)-30.0F, (double)10.0F, 0.01, var10009::isOpen));
      this.enchantmentsConfig = this.add(new BooleanSetting("Enchantments", true));
      this.durabilityConfig = this.add((new BooleanSetting("Durability", true)).setParent());
      var10006 = this.durabilityConfig;
      Objects.requireNonNull(var10006);
      this.forceBarConfig = this.add(new BooleanSetting("ForceBar", true, var10006::isOpen));
      this.itemNameConfig = this.add(new BooleanSetting("ItemName", false));
      this.entityIdConfig = this.add(new BooleanSetting("EntityId", false));
      this.gamemodeConfig = this.add(new BooleanSetting("Gamemode", false));
      this.pingConfig = this.add(new BooleanSetting("Ping", true));
      this.healthConfig = this.add(new BooleanSetting("Health", true));
      this.totemsConfig = this.add(new BooleanSetting("Totems", false));
      this.scaleConfig = this.add(new SliderSetting("Scale", (double)1.0F, (double)0.0F, (double)3.0F, 0.1));
      this.factorConfig = this.add((new BooleanSetting("Factor", true)).setParent());
      var10009 = this.factorConfig;
      Objects.requireNonNull(var10009);
      this.scalingConfig = this.add(new SliderSetting("Scaling", (double)1.0F, (double)0.0F, (double)3.0F, 0.1, var10009::isOpen));
      var10009 = this.factorConfig;
      Objects.requireNonNull(var10009);
      this.distanceConfig = this.add(new SliderSetting("Distance", (double)6.0F, (double)0.0F, (double)20.0F, 0.1, var10009::isOpen));
      this.heightConfig = this.add(new SliderSetting("Height", (double)0.0F, (double)-3.0F, (double)3.0F, 0.01));
      this.df = new DecimalFormat("0.0");
      this.setChinese("名字标签");
      INSTANCE = this;
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (mc.gameRenderer != null && mc.getCameraEntity() != null) {
         Camera camera = mc.gameRenderer.getCamera();
         RenderSystem.enableBlend();
         GL11.glDepthFunc(519);
         MatrixStack matrixStack = new MatrixStack();

         for(PlayerEntity playerEntity : Alien.THREAD.getPlayers()) {
            if ((this.died.booleanValue || playerEntity.isAlive()) && (playerEntity != mc.player || !mc.options.getPerspective().isFirstPerson()) && (this.invisibleConfig.booleanValue || !playerEntity.isInvisible())) {
               String info = this.getNametagInfo(playerEntity);
               float textWidth = this.getTextWidth(info);
               if (!(textWidth <= 0.0F)) {
                  float width = textWidth / 2.0F;
                  Vec3d renderPosition = MathUtil.getRenderPosition(playerEntity, event.tickDelta);
                  double x = renderPosition.getX();
                  double y = renderPosition.getY();
                  double z = renderPosition.getZ();
                  this.renderInfo(info, width, playerEntity, x, y, z, camera, matrixStack);
               }
            }
         }

         GL11.glDepthFunc(515);
         RenderSystem.disableBlend();
      }
   }

   private void renderInfo(String info, float width, PlayerEntity entity, double x, double y, double z, Camera camera, MatrixStack matrices) {
      Vec3d pos = camera.getPos();
      double eyeY = y + (double)entity.getHeight() + (double)(entity.isSneaking() ? 0.4F : 0.43F) + (double)this.heightConfig.getValueFloat();
      float scale = (float)((double)(-0.025F * this.scaleConfig.getValueFloat()) + (this.factorConfig.getValue() && pos.squaredDistanceTo(x, eyeY, z) > (double)(this.distanceConfig.getValueFloat() * this.distanceConfig.getValueFloat()) ? (Math.sqrt(pos.squaredDistanceTo(x, eyeY, z)) - (double)this.distanceConfig.getValueFloat()) * (double)-0.0025F * (double)this.scalingConfig.getValueFloat() : (double)0.0F));
      matrices.push();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(x - pos.getX(), eyeY - pos.getY() + (double)((scale / -0.025F - 1.0F) / 4.0F), z - pos.getZ());
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.scale(scale, scale, -1.0F);
      if (this.rectConfig.booleanValue) {
         float rectW = -width - 2.0F;
         float rectH = -1.0F;
         float rectWidth = width * 2.0F + 3.0F;
         float rectHeight = this.getTextHeight() + 1.0F;
         Render2DUtil.drawRect(matrices, rectW, rectH, rectWidth, rectHeight, this.rectConfig.getValue());
      }

      int textColor = this.getNametagColor(entity);
      this.drawWithShadow(matrices, info, -width, 0.0F, textColor);
      if (this.armorConfig.getValue()) {
         this.renderItems(matrices, entity);
      }

      matrices.pop();
   }

   private void drawWithShadow(MatrixStack matrices, String info, float x, float y, int color) {
      if (this.customFont.getValue() && FontManager.ui != null) {
         FontManager.ui.drawStringWithShadow(matrices, info, (double)x, (double)y, color);
      } else {
         VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
         mc.textRenderer.draw(info, x, y, color, true, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
         immediate.draw();
      }

   }

   private void renderItems(MatrixStack matrixStack, PlayerEntity player) {
      CopyOnWriteArrayList<ItemStack> displayItems = new CopyOnWriteArrayList();
      if (!player.getOffHandStack().isEmpty()) {
         displayItems.add(player.getOffHandStack());
      }

      player.getInventory().armor.forEach((armorStack) -> {
         if (!armorStack.isEmpty()) {
            displayItems.add(armorStack);
         }

      });
      if (!player.getMainHandStack().isEmpty()) {
         displayItems.add(player.getMainHandStack());
      }

      Collections.reverse(displayItems);
      float x = 0.0F;
      int enchantCount = 0;

      for(ItemStack itemStack : displayItems) {
         x -= 8.0F;
         if (itemStack.getEnchantments().getSize() > enchantCount) {
            enchantCount = itemStack.getEnchantments().getSize();
         }
      }

      float y = this.offsetConfig.getValueFloat();

      for(ItemStack stack : displayItems) {
         GL11.glDepthFunc(519);
         if (this.drawItemConfig.getValue()) {
            this.renderItemStack(matrixStack, stack, x, y + 1.0F);
         }

         this.renderItemOverlay(matrixStack, stack, x, y + 2.5F);
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.durabilityConfig.getValue()) {
            this.renderDurability(matrixStack, stack, x + 2.0F, y + 11.5F);
         }

         if (this.enchantmentsConfig.getValue()) {
            this.renderEnchants(matrixStack, stack, x + 2.0F, y + 7.0F);
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
         x += 16.0F;
         GL11.glDepthFunc(515);
      }

      ItemStack mainStack = player.getMainHandStack();
      if (!mainStack.isEmpty() && this.itemNameConfig.getValue()) {
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         this.renderItemName(matrixStack, mainStack, y - 4.5F + this.enchantOffset(enchantCount));
         matrixStack.scale(2.0F, 2.0F, 2.0F);
      }

   }

   private void renderItemStack(MatrixStack matrixStack, ItemStack stack, float x, float y) {
      matrixStack.push();
      matrixStack.translate(x, y, 0.0F);
      matrixStack.translate(8.0F, 8.0F, 0.0F);
      matrixStack.scale(16.0F, 16.0F, 1.0E-8F);
      matrixStack.multiplyPositionMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
      DiffuseLighting.disableGuiDepthLighting();
      BakedModel model = mc.getItemRenderer().getModel(stack, mc.world, (LivingEntity)null, 0);
      VertexConsumerProvider.Immediate i = mc.getBufferBuilders().getEntityVertexConsumers();
      mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, false, matrixStack, i, 16711680, OverlayTexture.DEFAULT_UV, model);
      i.draw();
      DiffuseLighting.enableGuiDepthLighting();
      matrixStack.pop();
   }

   private void renderItemOverlay(MatrixStack matrixStack, ItemStack stack, float x, float y) {
      matrixStack.push();
      if (stack.getCount() != 1) {
         String countStr = String.valueOf(stack.getCount());
         this.drawWithShadow(matrixStack, countStr, x + 17.0F - (float)mc.textRenderer.getWidth(countStr), y + 9.0F, -1);
      }

      if (stack.isItemBarVisible() || stack.isDamageable() && this.forceBarConfig.getValue()) {
         int barStep = stack.getItemBarStep();
         int barColor = stack.getItemBarColor();
         float barX = x + 2.0F;
         float barY = y + 13.0F;
         Render2DUtil.drawRect(matrixStack, barX, barY, 13.0F, 2.0F, -16777216);
         Render2DUtil.drawRect(matrixStack, barX, barY, (float)barStep, 1.0F, barColor | -16777216);
      }

      matrixStack.pop();
   }

   private void renderDurability(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.isDamageable()) {
         int maxDamage = itemStack.getMaxDamage();
         int damage = itemStack.getDamage();
         int durability = (int)((float)(maxDamage - damage) / (float)maxDamage * 100.0F);
         this.drawWithShadow(matrixStack, durability + "%", x * 2.0F, y * 2.0F, ColorUtil.hslToColor((float)(maxDamage - damage) / (float)maxDamage * 120.0F, 100.0F, 50.0F, 1.0F).getRGB());
      }
   }

   private void renderEnchants(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
         this.drawWithShadow(matrixStack, "God", x * 2.0F, y * 2.0F, -3977663);
      } else if (itemStack.hasEnchantments()) {
         ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(itemStack);
         float yOffset = 0.0F;

         for(RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()) {
            int lvl = enchants.getLevel(enchantment);
            StringBuilder enchantString = new StringBuilder();
            String translatedName = ((Enchantment)enchantment.value()).toString().replace("Enchantment ", "");
            if (translatedName.contains("Vanish")) {
               enchantString.append("§cVan");
            } else if (translatedName.contains("Bind")) {
               enchantString.append("§cBind");
            } else {
               int maxLen = lvl > 1 ? 2 : 3;
               if (translatedName.length() > maxLen) {
                  translatedName = translatedName.substring(0, maxLen);
               }

               enchantString.append(translatedName);
               enchantString.append(lvl);
            }

            this.drawWithShadow(matrixStack, enchantString.toString(), x * 2.0F, (y + yOffset) * 2.0F, -1);
            yOffset -= 4.5F;
         }

      }
   }

   private float enchantOffset(int n) {
      if (this.enchantmentsConfig.getValue() && n > 2) {
         float value = -2.0F;
         return value - (float)(n - 3) * 4.5F;
      } else {
         return 0.0F;
      }
   }

   private void renderItemName(MatrixStack matrixStack, ItemStack itemStack, float y) {
      String itemName = itemStack.getName().getString();
      float width = (float)mc.textRenderer.getWidth(itemName) / 4.0F;
      this.drawWithShadow(matrixStack, itemName, (0.0F - width) * 2.0F, y * 2.0F, -1);
   }

   private String getNametagInfo(PlayerEntity player) {
      StringBuilder info = new StringBuilder();
      if (this.gamemodeConfig.getValue()) {
         GameMode gamemode = getEntityGamemode(player);
         if (gamemode == GameMode.CREATIVE) {
            info.append(Formatting.DARK_PURPLE).append("[C] ");
         } else if (gamemode == GameMode.SPECTATOR) {
            info.append(Formatting.GRAY).append("[I] ");
         } else {
            info.append(Formatting.YELLOW).append("[S] ");
         }
      }

      if (this.pingConfig.getValue()) {
         info.append(getEntityPing(player)).append("ms ").append(Formatting.RESET);
      }

      info.append(player.getName().getString()).append(" ");
      if (this.entityIdConfig.getValue()) {
         info.append("ID: ").append(player.getId()).append(" ");
      }

      if (this.healthConfig.getValue()) {
         double health = (double)(player.getHealth() + player.getAbsorptionAmount());
         Formatting hcolor = this.getHealthColor(health);
         info.append(hcolor).append(this.df.format(health)).append(" ");
      }

      if (this.totemsConfig.getValue()) {
         int totems = Alien.POP.getPop(player);
         if (totems > 0) {
            info.append(getPopColor(totems)).append(-totems).append(" ");
         }
      }

      return info.toString().trim();
   }

   public static String getEntityPing(PlayerEntity entity) {
      if (mc.getNetworkHandler() == null) {
         return String.valueOf(Formatting.GRAY) + "-1";
      } else {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
         if (entry == null) {
            return String.valueOf(Formatting.GRAY) + "-1";
         } else {
            int ping = entry.getLatency();
            Formatting color = ping >= 200 ? Formatting.RED : (ping >= 100 ? Formatting.YELLOW : Formatting.GREEN);
            String var10000 = color.toString();
            return var10000 + ping;
         }
      }
   }

   public static GameMode getEntityGamemode(PlayerEntity entity) {
      if (entity != null && mc.getNetworkHandler() != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
         return entry == null ? null : entry.getGameMode();
      } else {
         return null;
      }
   }

   private Formatting getHealthColor(double health) {
      if (health > (double)18.0F) {
         return Formatting.GREEN;
      } else if (health > (double)16.0F) {
         return Formatting.GOLD;
      } else if (health > (double)12.0F) {
         return Formatting.YELLOW;
      } else if (health > (double)8.0F) {
         return Formatting.RED;
      } else {
         return health > (double)4.0F ? Formatting.DARK_RED : Formatting.DARK_GRAY;
      }
   }

   private static Formatting getPopColor(int popCount) {
      if (TextRadar.INSTANCE != null && TextRadar.INSTANCE.red.getValue()) {
         return Formatting.RED;
      } else if (popCount > 10) {
         return Formatting.DARK_RED;
      } else if (popCount > 8) {
         return Formatting.RED;
      } else if (popCount > 6) {
         return Formatting.GOLD;
      } else if (popCount > 4) {
         return Formatting.YELLOW;
      } else {
         return popCount > 2 ? Formatting.DARK_GREEN : Formatting.GREEN;
      }
   }

   private int getNametagColor(PlayerEntity player) {
      if (this.friendConfig.booleanValue && player.getDisplayName() != null && Alien.FRIEND.isFriend(player)) {
         return this.friendConfig.getValue().getRGB();
      } else if (this.invisibleConfig.booleanValue && player.isInvisible()) {
         return this.invisibleConfig.getValue().getRGB();
      } else if (this.sneakingConfig.booleanValue && player.isSneaking()) {
         return this.sneakingConfig.getValue().getRGB();
      } else {
         return !player.isAlive() ? this.died.getValue().getRGB() : this.colorConfig.getValue().getRGB();
      }
   }

   private float getTextWidth(String text) {
      return this.customFont.getValue() && FontManager.ui != null ? FontManager.ui.getWidth(text) : (float)mc.textRenderer.getWidth(text);
   }

   private float getTextHeight() {
      return this.customFont.getValue() && FontManager.ui != null ? FontManager.ui.getFontHeight() : 9.0F;
   }
}
