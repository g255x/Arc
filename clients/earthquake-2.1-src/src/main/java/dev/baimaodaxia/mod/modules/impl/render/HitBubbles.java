package dev.baimaodaxia.mod.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.DoAttackEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.render.Render3DUtil;
import dev.baimaodaxia.api.utils.rotation.RayCastUtil;
import dev.baimaodaxia.api.utils.rotation.Rotation;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.HUD;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;

public class HitBubbles extends Module {
   private final SliderSetting lifeTime = this.add(new SliderSetting("LifeTime", 30, 1, 150));
   private final SliderSetting range = this.add(new SliderSetting("Range", (double)6.0F, 0.1, (double)7.0F, 0.1));
   private final SliderSetting size = this.add(new SliderSetting("Size", (double)1.0F, 0.1, (double)4.0F, 0.1));
   private final EnumSetting<ColorMode> colorMode;
   private final ColorSetting color;
   private final Identifier textureBubble;
   private final List<HitBubble> bubbles;

   public HitBubbles() {
      super("HitBubbles", Module.Category.Render);
      this.colorMode = this.add(new EnumSetting("ColorMode", HitBubbles.ColorMode.Sync));
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255), () -> this.colorMode.getValue() == HitBubbles.ColorMode.Custom));
      this.textureBubble = Identifier.of("alien", "textures/jumpcircle/hitbubble.png");
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
         HitResult hitResult = RayCastUtil.rayCast(new Rotation(yaw, pitch), this.range.getValue(), 0.0F);
         if (hitResult != null && hitResult.getType() != Type.MISS) {
            Vec3d pos = hitResult.getPos();
            if (pos != null) {
               this.bubbles.add(new HitBubble(pos, -yaw, pitch, new Timer()));
            }
         }
      }
   }

   public void onRender3D(MatrixStack matrixStack) {
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
               RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
               RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
               RenderSystem.setShaderTexture(0, this.textureBubble);
               Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
               BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

               for(int i = this.bubbles.size() - 1; i >= 0; --i) {
                  HitBubble bubble = (HitBubble)this.bubbles.get(i);
                  long timeMs = bubble.life.getMs();
                  float factor = (float)timeMs / (float)lifeMs;
                  float opacity = 1.0F - factor;
                  if (!(opacity <= 0.0F)) {
                     float s = factor * 2.0F * this.size.getValueFloat();
                     float half = s / 2.0F;
                     float angle = -((float)timeMs) / 4.0F;
                     matrixStack.push();
                     matrixStack.translate(bubble.pos.x - cameraPos.x, bubble.pos.y - cameraPos.y, bubble.pos.z - cameraPos.z);
                     matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bubble.yaw));
                     matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(bubble.pitch));
                     matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
                     Matrix4f matrix = matrixStack.peek().getPositionMatrix();
                     buffer.vertex(matrix, -half, half, 0.0F).texture(0.0F, 1.0F).color(this.applyOpacity(this.getVertexColor(270), opacity));
                     buffer.vertex(matrix, half, half, 0.0F).texture(1.0F, 1.0F).color(this.applyOpacity(this.getVertexColor(0), opacity));
                     buffer.vertex(matrix, half, -half, 0.0F).texture(1.0F, 0.0F).color(this.applyOpacity(this.getVertexColor(180), opacity));
                     buffer.vertex(matrix, -half, -half, 0.0F).texture(0.0F, 0.0F).color(this.applyOpacity(this.getVertexColor(90), opacity));
                     matrixStack.pop();
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
      opacity = MathHelper.clamp(opacity, 0.0F, 1.0F);
      int a = (int)((float)(argb >> 24 & 255) * opacity);
      return a << 24 | argb & 16777215;
   }

   public static record HitBubble(Vec3d pos, float yaw, float pitch, Timer life) {
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
