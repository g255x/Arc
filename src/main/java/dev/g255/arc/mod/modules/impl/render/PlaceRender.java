package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.math.Easing;
import dev.g255.arc.api.utils.math.FadeUtils;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.Render3DUtil;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.HashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.client.util.math.MatrixStack;

public class PlaceRender extends Module {
   public static final HashMap<BlockPos, PlacePos> renderMap = new HashMap();
   public static PlaceRender INSTANCE;
   public final SliderSetting fadeTime = this.add(new SliderSetting("FadeTime", 500, 0, 3000));
   public final SliderSetting timeout = this.add(new SliderSetting("TimeOut", 500, 0, 3000));
   private final ColorSetting box = this.add((new ColorSetting("Box", new Color(255, 255, 255, 255))).injectBoolean(true));
   private final ColorSetting fill = this.add((new ColorSetting("Fill", new Color(255, 255, 255, 100))).injectBoolean(true));
   private final ColorSetting tryPlaceBox = this.add((new ColorSetting("TryPlaceBox", new Color(178, 178, 178, 255))).injectBoolean(true));
   private final ColorSetting tryPlaceFill = this.add((new ColorSetting("TryPlaceFill", new Color(255, 119, 119, 157))).injectBoolean(true));
   private final BooleanSetting noFail = this.add(new BooleanSetting("NoFail", false));
   private final EnumSetting<Easing> ease;
   private final EnumSetting<Mode> mode;

   public PlaceRender() {
      super("PlaceRender", Module.Category.Render);
      this.ease = this.add(new EnumSetting("Ease", Easing.CubicInOut));
      this.mode = this.add(new EnumSetting("Mode", PlaceRender.Mode.All));
      this.setChinese("放置显示");
      this.enable();
      INSTANCE = this;
   }

   public void onRender3D(MatrixStack matrixStack) {
      renderMap.values().removeIf((v) -> v.draw(matrixStack));
   }

   public void create(BlockPos pos) {
      renderMap.put(pos, new PlacePos(pos));
   }

   private static enum Mode {
      Fade,
      Shrink,
      All;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Fade, Shrink, All};
      }
   }

   public class PlacePos {
      public final FadeUtils fade;
      public final BlockPos pos;
      public final Timer timer;
      public boolean isAir;

      public PlacePos(BlockPos placePos) {
         this.fade = new FadeUtils((long)PlaceRender.this.fadeTime.getValue());
         this.pos = placePos;
         this.timer = new Timer();
         this.isAir = true;
      }

      public boolean draw(MatrixStack matrixStack) {
         if (this.isAir) {
            if (!PlaceRender.this.noFail.getValue() && Wrapper.mc.world.isAir(this.pos)) {
               if (!this.timer.passedMs(PlaceRender.this.timeout.getValue())) {
                  this.fade.reset();
                  Box aBox = new Box(this.pos);
                  if (PlaceRender.this.tryPlaceFill.booleanValue) {
                     Render3DUtil.drawFill(matrixStack, aBox, PlaceRender.this.tryPlaceFill.getValue());
                  }

                  if (PlaceRender.this.tryPlaceBox.booleanValue) {
                     Render3DUtil.drawBox(matrixStack, aBox, PlaceRender.this.tryPlaceBox.getValue());
                  }
               }

               return false;
            }

            this.isAir = false;
         }

         double quads;
         if ((quads = this.fade.ease(PlaceRender.this.ease.getValue())) == (double)1.0F) {
            return true;
         } else {
            double alpha = PlaceRender.this.mode.getValue() != PlaceRender.Mode.Fade && PlaceRender.this.mode.getValue() != PlaceRender.Mode.All ? (double)1.0F : (double)1.0F - quads;
            double size = PlaceRender.this.mode.getValue() != PlaceRender.Mode.Shrink && PlaceRender.this.mode.getValue() != PlaceRender.Mode.All ? (double)0.0F : quads;
            Box aBox = (new Box(this.pos)).expand(-size * (double)0.5F, -size * (double)0.5F, -size * (double)0.5F);
            if (PlaceRender.this.fill.booleanValue) {
               Render3DUtil.drawFill(matrixStack, aBox, ColorUtil.injectAlpha(PlaceRender.this.fill.getValue(), (int)((double)PlaceRender.this.fill.getValue().getAlpha() * alpha)));
            }

            if (PlaceRender.this.box.booleanValue) {
               Render3DUtil.drawBox(matrixStack, aBox, ColorUtil.injectAlpha(PlaceRender.this.box.getValue(), (int)((double)PlaceRender.this.box.getValue().getAlpha() * alpha)));
            }

            return false;
         }
      }
   }
}
