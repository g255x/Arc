package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager.class_4534;
import com.mojang.blaze3d.platform.GlStateManager.class_4535;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_1657;
import net.minecraft.class_243;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import net.minecraft.class_7833;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;

public class JumpCircle extends Module {
   private final EnumSetting<Mode> mode;
   private final EnumSetting<ColorMode> colorMode;
   private final ColorSetting color;
   private final BooleanSetting easeOut;
   private final SliderSetting rotateSpeed;
   private final SliderSetting circleScale;
   private final BooleanSetting onlySelf;
   private final class_2960 textureDefaultCircle;
   private final class_2960 textureBubble;
   private final List<Circle> circles;
   private final Set<UUID> groundCache;

   public JumpCircle() {
      super("JumpCircle", Module.Category.Render);
      this.mode = this.add(new EnumSetting("Mode", JumpCircle.Mode.Default));
      this.colorMode = this.add(new EnumSetting("ColorMode", JumpCircle.ColorMode.Sync));
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255), () -> this.colorMode.getValue() == JumpCircle.ColorMode.Custom));
      this.easeOut = this.add(new BooleanSetting("EaseOut", true));
      this.rotateSpeed = this.add(new SliderSetting("RotateSpeed", (double)2.0F, (double)0.5F, (double)5.0F, 0.1));
      this.circleScale = this.add(new SliderSetting("CircleScale", (double)1.0F, (double)0.5F, (double)5.0F, 0.1));
      this.onlySelf = this.add(new BooleanSetting("OnlySelf", false));
      this.textureDefaultCircle = class_2960.method_60655("alien", "textures/jumpcircle/circle.png");
      this.textureBubble = class_2960.method_60655("alien", "textures/jumpcircle/hitbubble.png");
      this.circles = new ArrayList();
      this.groundCache = new HashSet();
      this.setChinese("跳跃圈");
   }

   public void onEnable() {
      this.circles.clear();
      this.groundCache.clear();
   }

   public void onDisable() {
      this.circles.clear();
      this.groundCache.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         for(class_1657 player : mc.field_1687.method_18456()) {
            if (!this.onlySelf.getValue() || player == mc.field_1724) {
               UUID id = player.method_5667();
               if (player.method_24828()) {
                  this.groundCache.add(id);
               } else if (this.groundCache.remove(id)) {
                  this.circles.add(new Circle(new class_243(player.method_23317(), (double)((int)Math.floor(player.method_23318())) + 0.001, player.method_23321()), new Timer()));
               }
            }
         }

         long maxAgeMs = this.easeOut.getValue() ? 5000L : 6000L;
         this.circles.removeIf((c) -> c.timer.passed(maxAgeMs));
      }
   }

   public void onRender3D(class_4587 stack) {
      if (!nullCheck() && !this.circles.isEmpty()) {
         boolean hasRenderable = false;

         for(int i = this.circles.size() - 1; i >= 0; --i) {
            Circle circle = (Circle)this.circles.get(i);
            long timeMs = circle.timer.getMs();
            float colorAnim = (float)timeMs / 6000.0F;
            float sizeAnim = this.circleScale.getValueFloat() - (float)Math.pow((double)(1.0F - (float)timeMs * (this.easeOut.getValue() ? 2.0F : 1.0F) / 5000.0F), (double)4.0F);
            float opacity = 1.0F - colorAnim;
            if (opacity > 0.0F && sizeAnim > 0.0F) {
               hasRenderable = true;
               break;
            }
         }

         if (hasRenderable) {
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(class_4535.SRC_ALPHA, class_4534.ONE);
            RenderSystem.setShader(class_757::method_34543);
            RenderSystem.setShaderTexture(0, this.getModeTexture());
            class_243 cameraPos = mc.field_1773.method_19418().method_19326();
            class_287 buffer = class_289.method_1348().method_60827(class_5596.field_27382, class_290.field_1575);

            for(int i = this.circles.size() - 1; i >= 0; --i) {
               Circle circle = (Circle)this.circles.get(i);
               long timeMs = circle.timer.getMs();
               float colorAnim = (float)timeMs / 6000.0F;
               float sizeAnim = this.circleScale.getValueFloat() - (float)Math.pow((double)(1.0F - (float)timeMs * (this.easeOut.getValue() ? 2.0F : 1.0F) / 5000.0F), (double)4.0F);
               float opacity = 1.0F - colorAnim;
               if (!(opacity <= 0.0F) && !(sizeAnim <= 0.0F)) {
                  stack.method_22903();
                  stack.method_22904(circle.pos.field_1352 - cameraPos.field_1352, circle.pos.field_1351 - cameraPos.field_1351, circle.pos.field_1350 - cameraPos.field_1350);
                  stack.method_22907(class_7833.field_40714.rotationDegrees(90.0F));
                  stack.method_22907(class_7833.field_40718.rotationDegrees(sizeAnim * this.rotateSpeed.getValueFloat() * 1000.0F));
                  float scale = sizeAnim * 2.0F;
                  Matrix4f matrix = stack.method_23760().method_23761();
                  buffer.method_22918(matrix, -sizeAnim, -sizeAnim + scale, 0.0F).method_22913(0.0F, 1.0F).method_39415(this.applyOpacity(this.getVertexColor(270), opacity));
                  buffer.method_22918(matrix, -sizeAnim + scale, -sizeAnim + scale, 0.0F).method_22913(1.0F, 1.0F).method_39415(this.applyOpacity(this.getVertexColor(0), opacity));
                  buffer.method_22918(matrix, -sizeAnim + scale, -sizeAnim, 0.0F).method_22913(1.0F, 0.0F).method_39415(this.applyOpacity(this.getVertexColor(180), opacity));
                  buffer.method_22918(matrix, -sizeAnim, -sizeAnim, 0.0F).method_22913(0.0F, 0.0F).method_39415(this.applyOpacity(this.getVertexColor(90), opacity));
                  stack.method_22909();
               }
            }

            Render3DUtil.endBuilding(buffer);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableDepthTest();
         }
      }
   }

   private class_2960 getModeTexture() {
      class_2960 var10000;
      switch (((Mode)this.mode.getValue()).ordinal()) {
         case 0:
         case 2:
            var10000 = this.textureDefaultCircle;
            break;
         case 1:
            var10000 = this.textureBubble;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private int getVertexColor(int offset) {
      return this.colorMode.getValue() == JumpCircle.ColorMode.Custom ? this.color.getValue().getRGB() : this.getHudColor(offset);
   }

   private int getHudColor(int offset) {
      return HUD.INSTANCE != null ? HUD.INSTANCE.getColor((double)offset) : -1;
   }

   private int applyOpacity(int argb, float opacity) {
      opacity = class_3532.method_15363(opacity, 0.0F, 1.0F);
      int a = (int)((float)(argb >> 24 & 255) * opacity);
      return a << 24 | argb & 16777215;
   }

   private static class Circle {
      private final class_243 pos;
      private final Timer timer;

      private Circle(class_243 pos, Timer timer) {
         this.pos = pos;
         this.timer = timer;
      }
   }

   public static enum Mode {
      Default,
      Portal,
      Custom;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Default, Portal, Custom};
      }
   }

   public static enum ColorMode {
      Sync,
      Custom;

      // $FF: synthetic method
      private static ColorMode[] $values() {
         return new ColorMode[]{Sync, Custom};
      }
   }
}
