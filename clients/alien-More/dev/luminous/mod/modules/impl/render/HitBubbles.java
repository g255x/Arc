package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager.class_4534;
import com.mojang.blaze3d.platform.GlStateManager.class_4535;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DoAttackEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.rotation.RayCastUtil;
import dev.luminous.api.utils.rotation.Rotation;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import net.minecraft.class_7833;
import net.minecraft.class_239.class_240;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;

public class HitBubbles extends Module {
   private final SliderSetting lifeTime = this.add(new SliderSetting("LifeTime", 30, 1, 150));
   private final SliderSetting range = this.add(new SliderSetting("Range", (double)6.0F, 0.1, (double)7.0F, 0.1));
   private final SliderSetting size = this.add(new SliderSetting("Size", (double)1.0F, 0.1, (double)4.0F, 0.1));
   private final EnumSetting colorMode;
   private final ColorSetting color;
   private final class_2960 textureBubble;
   private final List bubbles;

   public HitBubbles() {
      super("HitBubbles", Module.Category.Render);
      this.colorMode = this.add(new EnumSetting("ColorMode", HitBubbles.ColorMode.Sync));
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255), () -> this.colorMode.getValue() == HitBubbles.ColorMode.Custom));
      this.textureBubble = class_2960.method_60655("alien", "textures/jumpcircle/hitbubble.png");
      this.bubbles = new ArrayList();
      this.setChinese("攻击气泡");
   }

   public void onEnable() {
      this.bubbles.clear();
   }

   public void onDisable() {
      this.bubbles.clear();
   }

   @EventListener
   public void onAttack(DoAttackEvent event) {
      if (!nullCheck() && event.isPost()) {
         float yaw = Alien.ROTATION.getLastYaw();
         float pitch = Alien.ROTATION.getLastPitch();
         class_239 hitResult = RayCastUtil.rayCast(new Rotation(yaw, pitch), this.range.getValue(), 0.0F);
         if (hitResult != null && hitResult.method_17783() != class_240.field_1333) {
            class_243 pos = hitResult.method_17784();
            if (pos != null) {
               this.bubbles.add(new HitBubble(pos, -yaw, pitch, new Timer()));
            }
         }
      }
   }

   public void onRender3D(class_4587 matrixStack) {
      if (!nullCheck() && !this.bubbles.isEmpty()) {
         long lifeMs = (long)this.lifeTime.getValueInt() * 50L;
         this.bubbles.removeIf((b) -> b.life.passedMs((double)lifeMs));
         if (!this.bubbles.isEmpty()) {
            boolean hasRenderable = false;

            for(int i = this.bubbles.size() - 1; i >= 0; --i) {
               HitBubble bubble = (HitBubble)this.bubbles.get(i);
               long timeMs = bubble.life.getMs();
               float factor = (float)timeMs / (float)lifeMs;
               float opacity = 1.0F - factor;
               if (opacity > 0.0F) {
                  hasRenderable = true;
                  break;
               }
            }

            if (hasRenderable) {
               RenderSystem.disableDepthTest();
               RenderSystem.enableBlend();
               RenderSystem.blendFunc(class_4535.SRC_ALPHA, class_4534.ONE);
               RenderSystem.setShader(class_757::method_34543);
               RenderSystem.setShaderTexture(0, this.textureBubble);
               class_243 cameraPos = mc.field_1773.method_19418().method_19326();
               class_287 buffer = class_289.method_1348().method_60827(class_5596.field_27382, class_290.field_1575);

               for(int i = this.bubbles.size() - 1; i >= 0; --i) {
                  HitBubble bubble = (HitBubble)this.bubbles.get(i);
                  long timeMs = bubble.life.getMs();
                  float factor = (float)timeMs / (float)lifeMs;
                  float opacity = 1.0F - factor;
                  if (!(opacity <= 0.0F)) {
                     float s = factor * 2.0F * this.size.getValueFloat();
                     float half = s / 2.0F;
                     float angle = -((float)timeMs) / 4.0F;
                     matrixStack.method_22903();
                     matrixStack.method_22904(bubble.pos.field_1352 - cameraPos.field_1352, bubble.pos.field_1351 - cameraPos.field_1351, bubble.pos.field_1350 - cameraPos.field_1350);
                     matrixStack.method_22907(class_7833.field_40716.rotationDegrees(bubble.yaw));
                     matrixStack.method_22907(class_7833.field_40714.rotationDegrees(bubble.pitch));
                     matrixStack.method_22907(class_7833.field_40718.rotationDegrees(angle));
                     Matrix4f matrix = matrixStack.method_23760().method_23761();
                     buffer.method_22918(matrix, -half, half, 0.0F).method_22913(0.0F, 1.0F).method_39415(this.applyOpacity(this.getVertexColor(270), opacity));
                     buffer.method_22918(matrix, half, half, 0.0F).method_22913(1.0F, 1.0F).method_39415(this.applyOpacity(this.getVertexColor(0), opacity));
                     buffer.method_22918(matrix, half, -half, 0.0F).method_22913(1.0F, 0.0F).method_39415(this.applyOpacity(this.getVertexColor(180), opacity));
                     buffer.method_22918(matrix, -half, -half, 0.0F).method_22913(0.0F, 0.0F).method_39415(this.applyOpacity(this.getVertexColor(90), opacity));
                     matrixStack.method_22909();
                  }
               }

               Render3DUtil.endBuilding(buffer);
               RenderSystem.disableBlend();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.enableDepthTest();
            }
         }
      }
   }

   private int getVertexColor(int offset) {
      return this.colorMode.getValue() == HitBubbles.ColorMode.Custom ? this.color.getValue().getRGB() : this.getHudColor(offset);
   }

   private int getHudColor(int offset) {
      return HUD.INSTANCE != null ? HUD.INSTANCE.getColor((double)offset) : -1;
   }

   private int applyOpacity(int argb, float opacity) {
      opacity = class_3532.method_15363(opacity, 0.0F, 1.0F);
      int a = (int)((float)(argb >> 24 & 255) * opacity);
      return a << 24 | argb & 16777215;
   }

   public static record HitBubble(class_243 pos, float yaw, float pitch, Timer life) {
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
