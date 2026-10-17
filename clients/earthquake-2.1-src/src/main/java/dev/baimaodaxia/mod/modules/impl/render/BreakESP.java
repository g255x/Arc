package dev.baimaodaxia.mod.modules.impl.render;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.utils.math.Easing;
import dev.baimaodaxia.api.utils.player.InventoryUtil;
import dev.baimaodaxia.api.utils.render.ColorUtil;
import dev.baimaodaxia.api.utils.render.Render3DUtil;
import dev.baimaodaxia.core.impl.BreakManager;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.entry.RegistryEntry;

public class BreakESP extends Module {
   public static BreakESP INSTANCE;
   public final BooleanSetting font = this.add(new BooleanSetting("Font", false));
   private final BooleanSetting progress = this.add(new BooleanSetting("Progress", true));
   private final SliderSetting damage = this.add(new SliderSetting("Damage", (double)1.0F, (double)0.0F, (double)2.0F, 0.01));
   private final ColorSetting box = this.add((new ColorSetting("Box", new Color(198, 176, 12, 255))).injectBoolean(true));
   private final ColorSetting fill = this.add((new ColorSetting("Fill", new Color(198, 176, 12, 78))).injectBoolean(true));
   private final ColorSetting boxFriend = this.add((new ColorSetting("FriendBox", new Color(30, 45, 169, 255))).injectBoolean(true));
   private final ColorSetting fillFriend = this.add((new ColorSetting("FriendFill", new Color(30, 45, 169, 78))).injectBoolean(true));
   private final ColorSetting textColor = this.add(new ColorSetting("Text", new Color(255, 255, 255, 255)));
   private final ColorSetting textFriendColor = this.add(new ColorSetting("FriendText", new Color(100, 200, 255, 255)));
   private final SliderSetting fontSize = this.add(new SliderSetting("FontSize", (double)1.0F, (double)0.5F, (double)2.0F, 0.1));
   private final BooleanSetting distanceScale = this.add(new BooleanSetting("DistanceScale", false));
   private final SliderSetting scaleDistance = this.add((new SliderSetting("ScaleDistance", (double)10.0F, (double)5.0F, (double)50.0F, (double)1.0F)).setSuffix("m"));
   private final EnumSetting<Easing> ease;
   private final BooleanSetting second;
   private final ColorSetting secondBox;
   private final ColorSetting secondFill;
   final DecimalFormat df;
   final Color startColor;
   final Color endColor;
   final Color doubleColor;

   public BreakESP() {
      super("BreakESP", Module.Category.Render);
      this.ease = this.add(new EnumSetting("Ease", Easing.CubicInOut));
      this.second = this.add(new BooleanSetting("Second", true));
      this.secondBox = this.add((new ColorSetting("SecondBox", new Color(255, 255, 255, 255))).injectBoolean(true));
      this.secondFill = this.add((new ColorSetting("SecondFill", new Color(255, 255, 255, 100))).injectBoolean(true));
      this.df = new DecimalFormat("0.0");
      this.startColor = new Color(255, 6, 6);
      this.endColor = new Color(0, 255, 12);
      this.doubleColor = new Color(255, 179, 96);
      this.setChinese("挖掘显示");
      INSTANCE = this;
   }

   private Color getFillColor(PlayerEntity player) {
      return Alien.FRIEND.isFriend(player) ? this.fillFriend.getValue() : this.fill.getValue();
   }

   private Color getBoxColor(PlayerEntity player) {
      return Alien.FRIEND.isFriend(player) ? this.boxFriend.getValue() : this.box.getValue();
   }

   private Color getTextColor(PlayerEntity player) {
      return Alien.FRIEND.isFriend(player) ? this.textFriendColor.getValue() : this.textColor.getValue();
   }

   private double getScale(Vec3d pos) {
      if (!this.distanceScale.getValue()) {
         return this.fontSize.getValue();
      } else {
         double distance = mc.player.getEyePos().distanceTo(pos);
         double baseScale = this.fontSize.getValue();
         double maxDistance = this.scaleDistance.getValue();
         double scaleFactor = Math.max(0.3, (double)1.0F - distance / maxDistance * 0.7);
         return baseScale * scaleFactor;
      }
   }

   public void onRender3D(MatrixStack matrixStack) {
      for(BreakManager.BreakData breakData : Alien.BREAK.breakMap.values()) {
         if (breakData != null && breakData.getEntity() != null) {
            PlayerEntity player = (PlayerEntity)breakData.getEntity();
            double size = (double)0.5F * ((double)1.0F - breakData.fade.ease(this.ease.getValue()));
            Box cbox = (new Box(breakData.pos)).shrink(size, size, size).shrink(-size, -size, -size);
            if (this.fill.booleanValue) {
               Render3DUtil.drawFill(matrixStack, cbox, this.getFillColor(player));
            }

            if (this.box.booleanValue) {
               Render3DUtil.drawBox(matrixStack, cbox, this.getBoxColor(player));
            }

            Vec3d textPos = breakData.pos.toCenterPos().add((double)0.0F, this.progress.getValue() ? 0.15 : (double)0.0F, (double)0.0F);
            double scale = this.getScale(textPos);
            this.drawText3D(player.getName().getString(), textPos, scale, this.getTextColor(player).getRGB());
            if (this.progress.getValue()) {
               this.drawText3D(Text.of(breakData.failed ? "§4Failed" : (breakData.complete ? "Broke" : this.df.format(Math.min((double)1.0F, (double)breakData.timer.getMs() / breakData.breakTime) * (double)100.0F))), breakData.pos.toCenterPos().add((double)0.0F, -0.15, (double)0.0F), (double)0.0F, (double)0.0F, scale, breakData.complete ? (mc.world.isAir(breakData.pos) ? this.endColor : this.startColor) : ColorUtil.fadeColor(this.startColor, this.endColor, (double)breakData.timer.getMs() / breakData.breakTime));
            }
         }
      }

      if (this.second.getValue()) {
         for(int i : Alien.BREAK.doubleMap.keySet()) {
            BreakManager.BreakData breakData = (BreakManager.BreakData)Alien.BREAK.doubleMap.get(i);
            if (breakData != null && breakData.getEntity() != null && !mc.world.isAir(breakData.pos)) {
               BreakManager.BreakData singleBreakData = (BreakManager.BreakData)Alien.BREAK.breakMap.get(i);
               if (singleBreakData == null || !singleBreakData.pos.equals(breakData.pos)) {
                  double size = (double)0.5F * ((double)1.0F - breakData.fade.ease(this.ease.getValue()));
                  Box cbox = (new Box(breakData.pos)).shrink(size, size, size).shrink(-size, -size, -size);
                  if (this.secondFill.booleanValue) {
                     Render3DUtil.drawFill(matrixStack, cbox, this.secondFill.getValue());
                  }

                  if (this.secondBox.booleanValue) {
                     Render3DUtil.drawBox(matrixStack, cbox, this.secondBox.getValue());
                  }

                  Vec3d textPos = breakData.pos.toCenterPos().add((double)0.0F, 0.15, (double)0.0F);
                  double scale = this.getScale(textPos);
                  this.drawText3D(breakData.getEntity().getName().getString(), textPos, scale, -1);
                  this.drawText3D("Double", breakData.pos.toCenterPos().add((double)0.0F, -0.15, (double)0.0F), scale, this.doubleColor.getRGB());
               }
            } else {
               Alien.BREAK.doubleMap.remove(i);
            }
         }
      }

   }

   private void drawText3D(String text, Vec3d pos, double scale, int color) {
      if (this.font.getValue()) {
         Render3DUtil.drawText3DWithFont(text, pos, scale, color);
      } else {
         Render3DUtil.drawText3D(text, pos, color);
      }

   }

   private void drawText3D(Text text, Vec3d pos, double offX, double offY, double scale, Color color) {
      if (this.font.getValue()) {
         Render3DUtil.drawText3DWithFont(text.getString(), pos, scale, color.getRGB());
      } else {
         Render3DUtil.drawText3D(text, pos, offX, offY, scale, color);
      }

   }

   public static double getBreakTime(BlockPos pos, boolean extraBreak) {
      int slot = getTool(pos);
      if (slot == -1) {
         slot = mc.player.getInventory().selectedSlot;
      }

      return getBreakTime(pos, slot, extraBreak ? (double)1.0F : INSTANCE.damage.getValue());
   }

   static int getTool(BlockPos pos) {
      AtomicInteger slot = new AtomicInteger();
      slot.set(-1);
      float CurrentFastest = 1.0F;

      for(Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
         float destroySpeed;
         float digSpeed;
         if (!(((ItemStack)entry.getValue()).getItem() instanceof AirBlockItem) && (digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get(), (ItemStack)entry.getValue())) + (destroySpeed = ((ItemStack)entry.getValue()).getMiningSpeedMultiplier(mc.world.getBlockState(pos))) > CurrentFastest) {
            CurrentFastest = digSpeed + destroySpeed;
            slot.set((Integer)entry.getKey());
         }
      }

      return slot.get();
   }

   static double getBreakTime(BlockPos pos, int slot, double damage) {
      return (double)(1.0F / getBlockStrength(pos, mc.player.getInventory().getStack(slot)) / 20.0F * 1000.0F) * damage;
   }

   static float getBlockStrength(BlockPos position, ItemStack itemStack) {
      BlockState state = mc.world.getBlockState(position);
      float hardness = state.getHardness(mc.world, position);
      if (hardness < 0.0F) {
         return 0.0F;
      } else {
         float i = state.isToolRequired() && !itemStack.isSuitableFor(state) ? 100.0F : 30.0F;
         return getDigSpeed(state, itemStack) / hardness / i;
      }
   }

   static float getDigSpeed(BlockState state, ItemStack itemStack) {
      float digSpeed = getDestroySpeed(state, itemStack);
      int efficiencyModifier;
      if (digSpeed > 1.0F && (efficiencyModifier = EnchantmentHelper.getLevel((RegistryEntry)mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get(), itemStack)) > 0 && !itemStack.isEmpty()) {
         digSpeed += (float)(StrictMath.pow((double)efficiencyModifier, (double)2.0F) + (double)1.0F);
      }

      return digSpeed < 0.0F ? 0.0F : digSpeed;
   }

   static float getDestroySpeed(BlockState state, ItemStack itemStack) {
      float destroySpeed = 1.0F;
      if (itemStack != null && !itemStack.isEmpty()) {
         destroySpeed *= itemStack.getMiningSpeedMultiplier(state);
      }

      return destroySpeed;
   }
}
