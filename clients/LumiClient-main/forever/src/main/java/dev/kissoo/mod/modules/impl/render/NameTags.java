package dev.kissoo.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.Render3DEvent;
import dev.kissoo.auth.AuthFail;
import dev.kissoo.auth.KeyAuth;
import dev.kissoo.api.utils.math.MathUtil;
import dev.kissoo.api.utils.render.ColorUtil;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.chat.ChatClient;
import dev.kissoo.mod.modules.impl.hud.TextRadar;
import dev.kissoo.mod.modules.impl.misc.IrcModule;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class NameTags extends Module {
   public static NameTags INSTANCE;

   public enum Mode {
      Alien, TrollHack
   }

   public enum MainHandSide {
      Left, Right
   }

   final EnumSetting<Mode> modeConfig = this.add(new EnumSetting<>("Mode", Mode.Alien));

   final ColorSetting colorConfig = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   final ColorSetting friendConfig = this.add(new ColorSetting("Friend", new Color(155, 155, 255)).injectBoolean(true));
   final ColorSetting ircConfig    = this.add(new ColorSetting("IRC",    new Color(255, 85, 255)).injectBoolean(true));
   final ColorSetting invisibleConfig = this.add(new ColorSetting("Invisible", new Color(200, 200, 200)).injectBoolean(true));
   final ColorSetting died = this.add(new ColorSetting("Died", new Color(180, 0, 0)).injectBoolean(true));
   final ColorSetting sneakingConfig = this.add(new ColorSetting("Sneaking", new Color(200, 200, 0)).injectBoolean(true));
   final ColorSetting rectConfig = this.add(new ColorSetting("Rectangle", new Color(0, 0, 0, 100)).injectBoolean(true));

   // TrollHack 专属设置
   final ColorSetting trollHackBoxColor = this.add(new ColorSetting("TrollBoxColor", new Color(0, 0, 0, 130), () -> modeConfig.is(Mode.TrollHack)));
   final EnumSetting<MainHandSide> mainHandSideConfig = this.add(new EnumSetting<>("MainHand", MainHandSide.Right, () -> modeConfig.is(Mode.TrollHack)));

   final BooleanSetting armorConfig = this.add(new BooleanSetting("Armor", true).setParent());
   final BooleanSetting drawItemConfig = this.add(new BooleanSetting("DrawItem", true, this.armorConfig::isOpen));
   final BooleanSetting itemCountConfig = this.add(new BooleanSetting("ItemCount", true, this.armorConfig::isOpen));
   final SliderSetting offsetConfig = this.add(new SliderSetting("Offset", -20.0, -30.0, 10.0, 0.01, this.armorConfig::isOpen));
   final BooleanSetting enchantmentsConfig = this.add(new BooleanSetting("Enchantments", true));
   final BooleanSetting durabilityConfig = this.add(new BooleanSetting("Durability", true).setParent());
   final BooleanSetting forceBarConfig = this.add(new BooleanSetting("ForceBar", true, this.durabilityConfig::isOpen));
   final BooleanSetting itemNameConfig = this.add(new BooleanSetting("ItemName", false));
   final BooleanSetting entityIdConfig = this.add(new BooleanSetting("EntityId", false));
   final BooleanSetting gamemodeConfig = this.add(new BooleanSetting("Gamemode", false));
   final BooleanSetting pingConfig = this.add(new BooleanSetting("Ping", true));
   final BooleanSetting healthConfig = this.add(new BooleanSetting("Health", true));
   final BooleanSetting totemsConfig = this.add(new BooleanSetting("Totems", false));
   final SliderSetting scaleConfig = this.add(new SliderSetting("Scale", 1.0, 0.0, 3.0, 0.1));
   final BooleanSetting factorConfig = this.add(new BooleanSetting("Factor", true).setParent());
   final SliderSetting scalingConfig = this.add(new SliderSetting("Scaling", 1.0, 0.0, 3.0, 0.1, this.factorConfig::isOpen));
   final SliderSetting distanceConfig = this.add(new SliderSetting("Distance", 6.0, 0.0, 20.0, 0.1, this.factorConfig::isOpen));
   final SliderSetting heightConfig = this.add(new SliderSetting("Height", 0.0, -3.0, 3.0, 0.01));
   final DecimalFormat df = new DecimalFormat("0.0");
   private static int alienScan = 0;

   public NameTags() {
      super("NameTags", "Renders info on player NameTags", Module.Category.Render);
      this.setChinese("名字标签");
      INSTANCE = this;
      // 独立启动注册名单轮询（不依赖 IRC 模块状态）
      ChatClient.ensureUsersPolling();
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (alienScan < 400) {
         alienScan++;
         if (alienScan == 320) {
            boolean missing = false;
            try {
               if (false) missing = true;
               if (!missing && !KeyAuth.isLoginRequired()) {
                  if (!KeyAuth.isCachedAllowed() || !KeyAuth.verifySessionSeal() || KeyAuth.guardValue() == 0) missing = true;
               }
            } catch (Throwable t) {
               missing = true;
            }
             if (missing) {
                AuthFail.hardStop("nametags integrity guard");
             }
         }
      }

      if (mc.gameRenderer != null && mc.getCameraEntity() != null) {
         if (modeConfig.is(Mode.Alien)) {
            renderAlien(event);
         } else if (modeConfig.is(Mode.TrollHack)) {
            renderTrollHack(event);
         }
      }
   }

   // ==========================================
   // TrollHack 模式渲染逻辑
   // ==========================================
   private void renderTrollHack(Render3DEvent event) {
      Camera camera = mc.gameRenderer.getCamera();
      RenderSystem.enableBlend();
      GL11.glDepthFunc(519);
      MatrixStack matrixStack = new MatrixStack();

      for (PlayerEntity player : Forever.THREAD.getPlayers()) {
         if ((this.died.booleanValue || player.isAlive())
                 && (player != mc.player || !mc.options.getPerspective().isFirstPerson())
                 && (this.invisibleConfig.booleanValue || !player.isInvisible())) {

            Vec3d renderPosition = MathUtil.getRenderPosition(player, event.tickDelta);
            double x = renderPosition.getX();
            double y = renderPosition.getY();
            double z = renderPosition.getZ();

            renderTrollHackPlayer(player, x, y, z, camera, matrixStack);
         }
      }

      GL11.glDepthFunc(515);
      RenderSystem.disableBlend();
   }

   private void renderTrollHackPlayer(PlayerEntity entity, double x, double y, double z, Camera camera, MatrixStack matrices) {
      Vec3d pos = camera.getPos();
      double eyeY = y + entity.getHeight() + (entity.isSneaking() ? 0.4F : 0.43F) + this.heightConfig.getValueFloat();
      float scale = (float)(
              -0.025F * this.scaleConfig.getValueFloat()
                      + (
                      this.factorConfig.getValue() && pos.squaredDistanceTo(x, eyeY, z) > this.distanceConfig.getValueFloat() * this.distanceConfig.getValueFloat()
                              ? (Math.sqrt(pos.squaredDistanceTo(x, eyeY, z)) - this.distanceConfig.getValueFloat()) * -0.0025F * this.scalingConfig.getValueFloat()
                              : 0.0
              )
      );

      matrices.push();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(x - pos.getX(), eyeY - pos.getY() + (scale / -0.025F - 1.0F) / 4.0F, z - pos.getZ());
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.scale(scale, scale, -1.0F);

      renderTrollHackItems(matrices, entity);
      renderTrollHackNameAndHealth(matrices, entity);

      matrices.pop();
   }

   private void renderTrollHackItems(MatrixStack matrices, PlayerEntity player) {
      if (!this.armorConfig.getValue()) return;

      List<ItemStack> armorList = new ArrayList<>();
      player.getInventory().armor.forEach(armorList::add);
      Collections.reverse(armorList);

      ItemStack[] displayItems = new ItemStack[6];
      if (mainHandSideConfig.is(MainHandSide.Right)) {
         displayItems[0] = player.getOffHandStack();
         displayItems[5] = player.getMainHandStack();
      } else {
         displayItems[0] = player.getMainHandStack();
         displayItems[5] = player.getOffHandStack();
      }

      for (int i = 0; i < 4; i++) {
         displayItems[i + 1] = armorList.size() > i ? armorList.get(i) : ItemStack.EMPTY;
      }

      float[] itemXs = {-44f, -26f, -10f, 6f, 22f, 40f};
      float itemY = -24f; // 物品整体向上移，避免和下方的耐久及方框重叠

      int maxEnchantCount = 0;

      for (int i = 0; i < 6; i++) {
         ItemStack stack = displayItems[i];
         if (stack == null || stack.isEmpty()) continue;

         float currentX = itemXs[i];

         // 受 DrawItem 开关控制
         if (this.drawItemConfig.getValue()) {
            renderItemStack(matrices, stack, currentX, itemY);
         }
         renderItemOverlay(matrices, stack, currentX, itemY + 1.5f);

         // 受 Durability 开关控制
         if (stack.isDamageable() && this.durabilityConfig.getValue()) {
            int max = stack.getMaxDamage();
            int dmg = stack.getDamage();
            int percent = (int) ((float) (max - dmg) / max * 100.0F);

            int durabilityColor = ColorUtil.hslToColor((float) (max - dmg) / max * 120.0F, 100.0F, 50.0F, 1.0F).getRGB();
            String percentStr = percent + "";

            matrices.push();
            matrices.translate(currentX + 8.0f, itemY + 18f, 0); // 在物品正下方
            matrices.scale(0.6f, 0.6f, 1f);
            float strWidth = mc.textRenderer.getWidth(percentStr);
            drawWithShadow(matrices, percentStr, -strWidth / 2.0f, 0, durabilityColor);
            matrices.pop();
         }

         // 受 Enchantments 开关控制
         if (this.enchantmentsConfig.getValue()) {
            int enchSize = stack.getEnchantments().getSize();
            if (enchSize > maxEnchantCount) maxEnchantCount = enchSize;

            matrices.push();
            matrices.translate(currentX + 8.0f, itemY, 0);
            matrices.scale(0.5f, 0.5f, 1f);
            renderTrollHackEnchants(matrices, stack);
            matrices.pop();
         }
      }

      // 受 ItemName 开关控制
      ItemStack heldItem = player.getMainHandStack();
      if (!heldItem.isEmpty() && this.itemNameConfig.getValue()) {
         matrices.push();
         matrices.translate(0, itemY - 6.0f - maxEnchantCount * 4.5f, 0);
         matrices.scale(0.5f, 0.5f, 1f);
         String itemName = heldItem.getName().getString();
         float nameWidth = mc.textRenderer.getWidth(itemName);
         drawWithShadow(matrices, itemName, -nameWidth / 2.0f, 0, -1);
         matrices.pop();
      }
   }

   private void renderTrollHackEnchants(MatrixStack matrixStack, ItemStack itemStack) {
      if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
         float strWidth = mc.textRenderer.getWidth("God");
         drawWithShadow(matrixStack, "God", -strWidth / 2.0f, -9.0f, -3977663);
      } else if (itemStack.hasEnchantments()) {
         ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(itemStack);
         float yOffset = -9.0f; // 从物品正上方开始

         for (RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()) {
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
               enchantString.append(translatedName).append(lvl);
            }

            String encStr = enchantString.toString();
            float strWidth = mc.textRenderer.getWidth(encStr);
            drawWithShadow(matrixStack, encStr, -strWidth / 2.0f, yOffset, -1);
            yOffset -= 9.0f; // 一层层往上叠
         }
      }
   }

   private void renderTrollHackNameAndHealth(MatrixStack matrices, PlayerEntity player) {
      String mcName  = player.getName().getString();
      String ircPrefix = ChatClient.isIrcUser(mcName) ? IrcModule.ircStatusPrefix(mcName) : "";
      String name = ircPrefix + userTagPrefix(player) + mcName;

      // 受 Ping 开关控制
      String ping = this.pingConfig.getValue() ? getRawPing(player) + "ms" : "";

      // 受 Health 开关控制
      boolean drawHp = this.healthConfig.getValue();
      double health = player.getHealth() + player.getAbsorptionAmount();
      String hpStr = drawHp ? df.format(health) : "";

      float nameWidth = mc.textRenderer.getWidth(name);
      float pingWidth = ping.isEmpty() ? 0 : mc.textRenderer.getWidth(ping);
      float hpWidth = hpStr.isEmpty() ? 0 : mc.textRenderer.getWidth(hpStr);
      float spaceWidth = mc.textRenderer.getWidth(" ");

      float totalWidth = nameWidth;
      if (!ping.isEmpty()) totalWidth += spaceWidth + pingWidth;
      if (!hpStr.isEmpty()) totalWidth += spaceWidth + hpWidth;

      float startX = -totalWidth / 2.0F;

      // 受 Rectangle 开关控制
      if (this.rectConfig.booleanValue) {
         Render2DUtil.drawRect(matrices, startX - 2.0f, -1.0f, totalWidth + 4.0f, 10.0f, trollHackBoxColor.getValue());
      }

      float currentX = startX;

      // 1. 名字
      int nameColor = getNametagColor(player);
      drawWithShadow(matrices, name, currentX, 0, nameColor);
      currentX += nameWidth + (ping.isEmpty() && hpStr.isEmpty() ? 0 : spaceWidth);

      // 2. Ping
      if (!ping.isEmpty()) {
         drawWithShadow(matrices, ping, currentX, 0, Formatting.DARK_GRAY.getColorValue());
         currentX += pingWidth + (hpStr.isEmpty() ? 0 : spaceWidth);
      }

      // 3. 血量数值和血条
      if (drawHp) {
         int hpColor = getHealthColor(health).getColorValue();
         drawWithShadow(matrices, hpStr, currentX, 0, hpColor);

         float maxBase = 20.0f; // 绿条上限20
         float maxAbs = 18.0f;  // 黄条上限18

         float baseHp = Math.min((float)player.getHealth(), maxBase);
         float absHp = Math.min((float)player.getAbsorptionAmount(), maxAbs);

         float boxWidth = totalWidth + 4.0f;
         float greenWidth = (baseHp / maxBase) * boxWidth;
         float yellowWidth = (absHp / maxAbs) * boxWidth;

         Render2DUtil.drawRect(matrices, startX - 2.0f, 9.5f, greenWidth, 1.5f, new Color(50, 255, 50).getRGB());
         if (absHp > 0) {
            Render2DUtil.drawRect(matrices, startX - 2.0f, 8.0f, yellowWidth, 1.5f, new Color(255, 200, 0).getRGB());
         }
      }
   }

   private String getRawPing(PlayerEntity entity) {
      if (mc.getNetworkHandler() == null) return "-1";
      PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
      return entry == null ? "-1" : String.valueOf(entry.getLatency());
   }

   private Formatting getHealthColor(double health) {
      if (health > 18.0) return Formatting.GREEN;
      if (health > 16.0) return Formatting.DARK_GREEN;
      if (health > 12.0) return Formatting.YELLOW;
      if (health > 8.0) return Formatting.GOLD;
      if (health > 4.0) return Formatting.RED;
      return Formatting.DARK_RED;
   }

   // ==========================================
   // 原先的 Alien 模式 Nametag 渲染逻辑
   // ==========================================
   private void renderAlien(Render3DEvent event) {
      Camera camera = mc.gameRenderer.getCamera();
      RenderSystem.enableBlend();
      GL11.glDepthFunc(519);
      MatrixStack matrixStack = new MatrixStack();

      for (PlayerEntity player : Forever.THREAD.getPlayers()) {
         if ((this.died.booleanValue || player.isAlive())
                 && (player != mc.player || !mc.options.getPerspective().isFirstPerson())
                 && (this.invisibleConfig.booleanValue || !player.isInvisible())) {
            String info = this.getNametagInfo(player);
            Vec3d renderPosition = MathUtil.getRenderPosition(player, event.tickDelta);
            double x = renderPosition.getX();
            double y = renderPosition.getY();
            double z = renderPosition.getZ();
            int width = mc.textRenderer.getWidth(info);
            float hwidth = width / 2.0F;
            this.renderInfo(info, hwidth, player, x, y, z, camera, matrixStack);
         }
      }

      GL11.glDepthFunc(515);
      RenderSystem.disableBlend();
   }


   //=========================================//
   // 下面是通用的辅助方法 (保留原逻辑不变)
   //=========================================//

   private void renderInfo(String info, float width, PlayerEntity entity, double x, double y, double z, Camera camera, MatrixStack matrices) {
      Vec3d pos = camera.getPos();
      double eyeY = y + entity.getHeight() + (entity.isSneaking() ? 0.4F : 0.43F) + this.heightConfig.getValueFloat();
      float scale = (float)(
              -0.025F * this.scaleConfig.getValueFloat()
                      + (
                      this.factorConfig.getValue() && pos.squaredDistanceTo(x, eyeY, z) > this.distanceConfig.getValueFloat() * this.distanceConfig.getValueFloat()
                              ? (Math.sqrt(pos.squaredDistanceTo(x, eyeY, z)) - this.distanceConfig.getValueFloat()) * -0.0025F * this.scalingConfig.getValueFloat()
                              : 0.0
              )
      );
      matrices.push();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(x - pos.getX(), eyeY - pos.getY() + (scale / -0.025F - 1.0F) / 4.0F, z - pos.getZ());
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.scale(scale, scale, -1.0F);
      if (this.rectConfig.booleanValue) {
         Render2DUtil.drawRect(matrices, -width - 2.0F, -1.0F, width * 2.0F + 3.0F, 9.0F + 1.0F, this.rectConfig.getValue());
      }

      this.drawWithShadow(matrices, info, -width, 0.0F, this.getNametagColor(entity));
      if (this.armorConfig.getValue()) {
         this.renderItems(matrices, entity);
      }

      matrices.pop();
   }

   private void drawWithShadow(MatrixStack matrices, String info, float x, float y, int color) {
      Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
      mc.textRenderer.drawLayer(info, x, y, color, true, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
      immediate.draw();
      mc.textRenderer.draw(info, x, y, color, false, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
      immediate.draw();
   }

   private void renderItems(MatrixStack matrixStack, PlayerEntity player) {
      List<ItemStack> displayItems = new CopyOnWriteArrayList();
      if (!player.getOffHandStack().isEmpty()) {
         displayItems.add(player.getOffHandStack());
      }

      player.getInventory().armor.forEach(armorStack -> {
         if (!armorStack.isEmpty()) {
            displayItems.add(armorStack);
         }
      });
      if (!player.getMainHandStack().isEmpty()) {
         displayItems.add(player.getMainHandStack());
      }

      Collections.reverse(displayItems);
      float x = 0.0F;
      int n11 = 0;

      for (ItemStack stack : displayItems) {
         x -= 8.0F;
         if (stack.getEnchantments().getSize() > n11) {
            n11 = stack.getEnchantments().getSize();
         }
      }

      float y = this.offsetConfig.getValueFloat();

      for (ItemStack stackx : displayItems) {
         GL11.glDepthFunc(519);
         if (this.drawItemConfig.getValue()) {
            this.renderItemStack(matrixStack, stackx, x, y + 1.0F);
         }

         this.renderItemOverlay(matrixStack, stackx, x, y + 2.5F);
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.durabilityConfig.getValue()) {
            this.renderDurability(matrixStack, stackx, x + 2.0F, y + 11.5F);
         }

         if (this.enchantmentsConfig.getValue()) {
            this.renderEnchants(matrixStack, stackx, x + 2.0F, y + 7.0F);
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
         x += 16.0F;
         GL11.glDepthFunc(515);
      }

      ItemStack heldItem = player.getMainHandStack();
      if (!heldItem.isEmpty()) {
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.itemNameConfig.getValue()) {
            this.renderItemName(matrixStack, heldItem, y - 4.5F + this.enchantOffset(n11));
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
      }
   }

   private void renderItemStack(MatrixStack matrixStack, ItemStack stack, float x, float y) {
      matrixStack.push();
      matrixStack.translate(x, y, 0.0F);
      matrixStack.translate(8.0F, 8.0F, 0.0F);
      matrixStack.scale(16.0F, 16.0F, 1.0E-8F);
      matrixStack.multiplyPositionMatrix(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
      DiffuseLighting.disableGuiDepthLighting();
      BakedModel model = mc.getItemRenderer().getModel(stack, mc.world, null, 0);
      Immediate i = mc.getBufferBuilders().getEntityVertexConsumers();
      mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, false, matrixStack, i, 16711680, OverlayTexture.DEFAULT_UV, model);
      i.draw();
      DiffuseLighting.enableGuiDepthLighting();
      matrixStack.pop();
   }

   private void renderItemOverlay(MatrixStack matrixStack, ItemStack stack, float x, float y) {
      matrixStack.push();
      if (this.itemCountConfig.getValue() && stack.getCount() != 1) {
         String string = String.valueOf(stack.getCount());
         this.drawWithShadow(matrixStack, string, x + 17.0F - mc.textRenderer.getWidth(string), y + 9.0F, -1);
      }

      if (stack.isItemBarVisible() || stack.isDamageable() && this.forceBarConfig.getValue()) {
         int i = stack.getItemBarStep();
         int j = stack.getItemBarColor();
         float k = x + 2.0F;
         float l = y + 13.0F;
         Render2DUtil.drawRect(matrixStack, k, l, 13.0F, 2.0F, -16777216);
         Render2DUtil.drawRect(matrixStack, k, l, i, 1.0F, j | 0xFF000000);
      }

      matrixStack.pop();
   }

   private void renderDurability(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.isDamageable()) {
         int n = itemStack.getMaxDamage();
         int n2 = itemStack.getDamage();
         int durability = (int)((float)(n - n2) / n * 100.0F);
         this.drawWithShadow(
                 matrixStack, durability + "%", x * 2.0F, y * 2.0F, ColorUtil.hslToColor((float)(n - n2) / n * 120.0F, 100.0F, 50.0F, 1.0F).getRGB()
         );
      }
   }

   private void renderEnchants(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
         this.drawWithShadow(matrixStack, "God", x * 2.0F, y * 2.0F, -3977663);
      } else if (itemStack.hasEnchantments()) {
         ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(itemStack);
         float n2 = 0.0F;

         for (RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()) {
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

            this.drawWithShadow(matrixStack, enchantString.toString(), x * 2.0F, (y + n2) * 2.0F, -1);
            n2 -= 4.5F;
         }
      }
   }

   private float enchantOffset(int n) {
      if (this.enchantmentsConfig.getValue() && n > 2) {
         float value = -2.0F;
         return value - (n - 3) * 4.5F;
      } else {
         return 0.0F;
      }
   }

   private void renderItemName(MatrixStack matrixStack, ItemStack itemStack, float y) {
      String itemName = itemStack.getName().getString();
      float width = mc.textRenderer.getWidth(itemName) / 4.0F;
      this.drawWithShadow(matrixStack, itemName, (0.0F - width) * 2.0F, y * 2.0F, -1);
   }

   /** 身份前缀，统一走 ChatClient.getUserTagPrefix，与 Tab 列表(Mixin)保持一致。 */
   private String userTagPrefix(PlayerEntity player) {
      return ChatClient.getUserTagPrefix(player.getGameProfile().getName());
   }

   private String getNametagInfo(PlayerEntity player) {
      StringBuilder info = new StringBuilder();
      if (this.gamemodeConfig.getValue()) {
         if (player.isCreative()) {
            info.append(Formatting.GOLD);
            info.append("[C] ");
         } else if (player.isSpectator()) {
            info.append(Formatting.GRAY);
            info.append("[I] ");
         } else {
            info.append(Formatting.BOLD);
            info.append("[S] ");
         }
      }

      if (this.pingConfig.getValue()) {
         info.append(this.getEntityPing(player));
         info.append("ms ");
         info.append(Formatting.RESET);
      }

      String mcName = player.getName().getString();
      if (ChatClient.isIrcUser(mcName)) {
         info.append(IrcModule.ircStatusPrefix(mcName));
      }
      info.append(userTagPrefix(player));
      info.append(mcName);
      info.append(" ");
      if (this.entityIdConfig.getValue()) {
         info.append("ID: ");
         info.append(player.getId());
         info.append(" ");
      }

      if (this.healthConfig.getValue()) {
         double health = player.getHealth() + player.getAbsorptionAmount();
         Formatting hcolor;
         if (health > 18.0) {
            hcolor = Formatting.GREEN;
         } else if (health > 16.0) {
            hcolor = Formatting.DARK_GREEN;
         } else if (health > 12.0) {
            hcolor = Formatting.YELLOW;
         } else if (health > 8.0) {
            hcolor = Formatting.GOLD;
         } else if (health > 4.0) {
            hcolor = Formatting.RED;
         } else {
            hcolor = Formatting.DARK_RED;
         }

         String phealth = this.df.format(health);
         info.append(hcolor);
         info.append(phealth);
         info.append(" ");
      }

      if (this.totemsConfig.getValue()) {
         int totems = Forever.POP.getPop(player);
         if (totems > 0) {
            Formatting c = TextRadar.getPopColor(totems);
            info.append(c);
            info.append(-totems);
            info.append(" ");
         }
      }

      return info.toString().trim();
   }

   private String getEntityPing(PlayerEntity entity) {
      if (mc.getNetworkHandler() == null) {
         return "§7-1";
      } else {
         PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
         if (playerListEntry == null) {
            return "§7-1";
         } else {
            int ping = playerListEntry.getLatency();
            Formatting color;
            if (ping >= 200) {
               color = Formatting.RED;
            } else if (ping >= 100) {
               color = Formatting.YELLOW;
            } else {
               color = Formatting.GREEN;
            }

            return color.toString() + ping;
         }
      }
   }

   private int getNametagColor(PlayerEntity player) {
      String mcName = player.getGameProfile().getName();
      if (this.ircConfig.booleanValue && ChatClient.isIrcUser(mcName)) {
         return ChatClient.getIrcDisplayColor(mcName, this.ircConfig.getValue().getRGB());
      } else if (this.friendConfig.booleanValue && player.getDisplayName() != null && Forever.FRIEND.isFriend(player)) {
         return this.friendConfig.getValue().getRGB();
      } else if (this.invisibleConfig.booleanValue && player.isInvisible()) {
         return this.invisibleConfig.getValue().getRGB();
      } else if (this.sneakingConfig.booleanValue && player.isSneaking()) {
         return this.sneakingConfig.getValue().getRGB();
      } else {
         return !player.isAlive() ? this.died.getValue().getRGB() : this.colorConfig.getValue().getRGB();
      }
   }
}
