package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.Arc;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render3DUtil;
import dev.g255.arc.core.impl.BreakManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
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
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.entry.RegistryEntry;

public class BreakESP extends Module {
   public static BreakESP INSTANCE;
   private final BooleanSetting progress = this.add(new BooleanSetting("Progress", true));
   private final SliderSetting damage = this.add(new SliderSetting("Damage", (double)1.0F, (double)0.0F, (double)2.0F, 0.01));
   private final ColorSetting box = this.add((new ColorSetting("Box", new Color(198, 176, 12, 255))).injectBoolean(true));
   private final ColorSetting fill = this.add((new ColorSetting("Fill", new Color(198, 176, 12, 78))).injectBoolean(true));
   private final ColorSetting boxFriend = this.add((new ColorSetting("FriendBox", new Color(30, 45, 169, 255))).injectBoolean(true));
   private final ColorSetting fillFriend = this.add((new ColorSetting("FriendFill", new Color(30, 45, 169, 78))).injectBoolean(true));
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
      return Arc.FRIEND.isFriend(player) ? this.fillFriend.getValue() : this.fill.getValue();
   }

   private Color getBoxColor(PlayerEntity player) {
      return Arc.FRIEND.isFriend(player) ? this.boxFriend.getValue() : this.box.getValue();
   }

   public void onRender3D(MatrixStack matrixStack) {
      for(BreakManager.BreakData breakData : Arc.BREAK.breakMap.values()) {
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

            Render3DUtil.drawText3D(player.getName().getString(), breakData.pos.toCenterPos().add((double)0.0F, this.progress.getValue() ? 0.15 : (double)0.0F, (double)0.0F), -1);
            if (this.progress.getValue()) {
               Render3DUtil.drawText3D(Text.of(breakData.failed ? "§4Failed" : (breakData.complete ? "Broke" : this.df.format(Math.min((double)1.0F, (double)breakData.timer.getMs() / breakData.breakTime) * (double)100.0F))), breakData.pos.toCenterPos().add((double)0.0F, -0.15, (double)0.0F), (double)0.0F, (double)0.0F, (double)1.0F, breakData.complete ? (mc.world.isAir(breakData.pos) ? this.endColor : this.startColor) : ColorUtil.fadeColor(this.startColor, this.endColor, (double)breakData.timer.getMs() / breakData.breakTime));
            }
         }
      }

      if (this.second.getValue()) {
         for(int i : Arc.BREAK.doubleMap.keySet()) {
            BreakManager.BreakData breakData = (BreakManager.BreakData)Arc.BREAK.doubleMap.get(i);
            if (breakData != null && breakData.getEntity() != null && !mc.world.isAir(breakData.pos)) {
               BreakManager.BreakData singleBreakData = (BreakManager.BreakData)Arc.BREAK.breakMap.get(i);
               if (singleBreakData == null || !singleBreakData.pos.equals(breakData.pos)) {
                  double size = (double)0.5F * ((double)1.0F - breakData.fade.ease(this.ease.getValue()));
                  Box cbox = (new Box(breakData.pos)).shrink(size, size, size).shrink(-size, -size, -size);
                  if (this.secondFill.booleanValue) {
                     Render3DUtil.drawFill(matrixStack, cbox, this.secondFill.getValue());
                  }

                  if (this.secondBox.booleanValue) {
                     Render3DUtil.drawBox(matrixStack, cbox, this.secondBox.getValue());
                  }

                  Render3DUtil.drawText3D(breakData.getEntity().getName().getString(), breakData.pos.toCenterPos().add((double)0.0F, 0.15, (double)0.0F), -1);
                  Render3DUtil.drawText3D("Double", breakData.pos.toCenterPos().add((double)0.0F, -0.15, (double)0.0F), this.doubleColor.getRGB());
               }
            } else {
               Arc.BREAK.doubleMap.remove(i);
            }
         }
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
