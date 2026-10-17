package dev.baimaodaxia.mod.modules.impl.player;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.render.Render3DUtil;
import dev.baimaodaxia.api.utils.world.BlockUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.item.BlockItem;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.util.math.MatrixStack;

public class AirPlace extends Module {
   public static AirPlace INSTANCE;
   public final BooleanSetting module = this.add(new BooleanSetting("Module", true));
   public final BooleanSetting grimBypass = this.add(new BooleanSetting("GrimBypass", false));
   public final BooleanSetting crossHair = this.add((new BooleanSetting("Crosshair", true)).setParent());
   private final SliderSetting range;
   private final ColorSetting fill;
   private final ColorSetting box;
   private BlockHitResult hit;
   private int cooldown;

   public AirPlace() {
      super("AirPlace", Module.Category.Player);
      BooleanSetting var10008 = this.crossHair;
      Objects.requireNonNull(var10008);
      this.range = this.add(new SliderSetting("Range", (double)5.0F, (double)0.0F, (double)6.0F, var10008::isOpen));
      Color var10005 = new Color(255, 0, 0, 50);
      BooleanSetting var10006 = this.crossHair;
      Objects.requireNonNull(var10006);
      this.fill = this.add((new ColorSetting("Fill", var10005, var10006::isOpen)).injectBoolean(true));
      var10005 = new Color(255, 0, 0, 100);
      var10006 = this.crossHair;
      Objects.requireNonNull(var10006);
      this.box = this.add((new ColorSetting("Box", var10005, var10006::isOpen)).injectBoolean(true));
      this.setChinese("空气放置");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.crossHair.getValue()) {
         if (this.cooldown > 0) {
            --this.cooldown;
         }

         BlockHitResult bhr;
         HitResult hitResult;
         this.hit = (hitResult = mc.getCameraEntity().raycast(this.range.getValue(), 0.0F, false)) instanceof BlockHitResult ? (bhr = (BlockHitResult)hitResult) : null;
         if (this.hit == null || !mc.world.getBlockState(this.hit.getBlockPos()).getBlock().equals(Blocks.AIR) || !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
            return;
         }

         boolean Main = mc.player.getMainHandStack().getItem() instanceof BlockItem;
         if (mc.options.useKey.isPressed() && Main && this.cooldown <= 0) {
            BlockUtil.airPlace(this.hit.getBlockPos(), false);
            this.cooldown = 2;
         }
      }

   }

   public void onRender3D(MatrixStack stack) {
      if (this.crossHair.getValue()) {
         if (this.hit == null || !mc.world.getBlockState(this.hit.getBlockPos()).getBlock().equals(Blocks.AIR) || !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
            return;
         }

         Render3DUtil.draw3DBox(stack, new Box(this.hit.getBlockPos()), this.fill.getValue(), this.box.getValue(), this.box.booleanValue, this.fill.booleanValue);
      }

   }
}
