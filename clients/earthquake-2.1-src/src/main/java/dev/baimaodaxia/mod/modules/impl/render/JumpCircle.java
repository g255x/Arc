package dev.baimaodaxia.mod.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.render.Render3DUtil;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.impl.client.HUD;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;

public class JumpCircle extends Module {
   private final EnumSetting<Mode> mode;
   private final EnumSetting<ColorMode> colorMode;
   private final ColorSetting color;
   private final BooleanSetting easeOut;
   private final SliderSetting rotateSpeed;
   private final SliderSetting circleScale;
   private final BooleanSetting onlySelf;
   private final Identifier textureDefaultCircle;
   private final Identifier textureBubble;
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
      this.textureDefaultCircle = Identifier.of("alien", "textures/jumpcircle/circle.png");
      this.textureBubble = Identifier.of("alien", "textures/jumpcircle/hitbubble.png");
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
         for(PlayerEntity player : mc.world.getPlayers()) {
            if (!this.onlySelf.getValue() || player == mc.player) {
               UUID id = player.getUuid();
               if (player.isOnGround()) {
                  this.groundCache.add(id);
               } else if (this.groundCache.remove(id)) {
                  this.circles.add(new Circle(new Vec3d(player.getX(), (double)((int)Math.floor(player.getY())) + 0.001, player.getZ()), new Timer()));
               }
            }
         }

         long maxAgeMs = this.easeOut.getValue() ? 5000L : 6000L;
         this.circles.removeIf((c) -> c.timer.passed(maxAgeMs));
      }
   }

   public void onRender3D(MatrixStack stack) {
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
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, this.getModeTexture());
            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for(int i = this.circles.size() - 1; i >= 0; --i) {
               Circle circle = (Circle)this.circles.get(i);
               long timeMs = circle.timer.getMs();
               float colorAnim = (float)timeMs / 6000.0F;
               float sizeAnim = this.circleScale.getValueFloat() - (float)Math.pow((double)(1.0F - (float)timeMs * (this.easeOut.getValue() ? 2.0F : 1.0F) / 5000.0F), (double)4.0F);
               float opacity = 1.0F - colorAnim;
               if (!(opacity <= 0.0F) && !(sizeAnim <= 0.0F)) {
                  stack.push();
                  stack.translate(circle.pos.x - cameraPos.x, circle.pos.y - cameraPos.y, circle.pos.z - cameraPos.z);
                  stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                  stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sizeAnim * this.rotateSpeed.getValueFloat() * 1000.0F));
                  float scale = sizeAnim * 2.0F;
                  Matrix4f matrix = stack.peek().getPositionMatrix();
                  buffer.vertex(matrix, -sizeAnim, -sizeAnim + scale, 0.0F).texture(0.0F, 1.0F).color(this.applyOpacity(this.getVertexColor(270), opacity));
                  buffer.vertex(matrix, -sizeAnim + scale, -sizeAnim + scale, 0.0F).texture(1.0F, 1.0F).color(this.applyOpacity(this.getVertexColor(0), opacity));
                  buffer.vertex(matrix, -sizeAnim + scale, -sizeAnim, 0.0F).texture(1.0F, 0.0F).color(this.applyOpacity(this.getVertexColor(180), opacity));
                  buffer.vertex(matrix, -sizeAnim, -sizeAnim, 0.0F).texture(0.0F, 0.0F).color(this.applyOpacity(this.getVertexColor(90), opacity));
                  stack.pop();
               }
            }

            Render3DUtil.endBuilding(buffer);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableDepthTest();
         }
      }
   }

   private Identifier getModeTexture() {
      Identifier var10000;
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
      opacity = MathHelper.clamp(opacity, 0.0F, 1.0F);
      int a = (int)((float)(argb >> 24 & 255) * opacity);
      return a << 24 | argb & 16777215;
   }

   private static class Circle {
      private final Vec3d pos;
      private final Timer timer;

      private Circle(Vec3d pos, Timer timer) {
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
