package hoprc.api.live2d;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class CubismNativeRenderer {
   private final Identifier[] textures;
   private Set<String> frontBangDrawables = Set.of();
   private Set<String> hiddenDrawables = Set.of();
   private boolean flipV;
   private boolean skipMaskedDrawables;
   private boolean frontBangsFix = true;
   private boolean debugOrder;
   private boolean debugOrderLogged;

   public CubismNativeRenderer(Identifier... textures) {
      this.textures = textures;
   }

   public void setFlipV(boolean flipV) {
      this.flipV = flipV;
   }

   public void setSkipMaskedDrawables(boolean skipMaskedDrawables) {
      this.skipMaskedDrawables = skipMaskedDrawables;
   }

   public void setFrontBangsFix(boolean frontBangsFix) {
      this.frontBangsFix = frontBangsFix;
   }

   public void setFrontBangDrawables(Set<String> frontBangDrawables) {
      this.frontBangDrawables = Set.copyOf(frontBangDrawables);
   }

   public void setHiddenDrawables(Set<String> hiddenDrawables) {
      this.hiddenDrawables = Set.copyOf(hiddenDrawables);
   }

   public void setDebugOrder(boolean debugOrder) {
      if (debugOrder && !this.debugOrder) {
         this.debugOrderLogged = false;
      }
      this.debugOrder = debugOrder;
   }

   public void render(DrawContext context, CubismNativeModel model, float x, float y, float width, float height, float alpha) {
      int drawableCount = model.getDrawableCount();
      Integer[] sortedDrawables = new Integer[drawableCount];
      for (int i = 0; i < drawableCount; i++) {
         sortedDrawables[i] = i;
      }
      Arrays.sort(
         sortedDrawables,
         Comparator.comparingInt(model::getDrawableRenderOrder)
            .thenComparingInt(model::getDrawableDrawOrder)
      );
      dumpOrderOnce(model, sortedDrawables);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

      for (int drawable : sortedDrawables) {
         if (isHiddenDrawable(model, drawable)) {
            continue;
         }
         if (this.frontBangsFix && isFrontBangOverlay(model, drawable)) {
            continue;
         }
         renderDrawable(context, model, drawable, x, y, width, height, alpha);
      }
      if (this.frontBangsFix) {
         for (int drawable : sortedDrawables) {
            if (!isHiddenDrawable(model, drawable) && isFrontBangOverlay(model, drawable)) {
               renderDrawable(context, model, drawable, x, y, width, height, alpha);
            }
         }
      }

      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private void renderDrawable(
      DrawContext context,
      CubismNativeModel model,
      int drawable,
      float x,
      float y,
      float width,
      float height,
      float alpha
   ) {
      if (!model.isDrawableVisible(drawable)) {
         return;
      }
      if (this.skipMaskedDrawables && model.getDrawableMaskCount(drawable) > 0) {
         return;
      }
      int textureIndex = model.getDrawableTextureIndex(drawable);
      if (textureIndex < 0 || textureIndex >= this.textures.length) {
         return;
      }

      float opacity = model.getDrawableOpacity(drawable) * alpha;
      if (opacity <= 0.001F) {
         return;
      }

      float[] vertices = model.getDrawableVertexPositions(drawable);
      float[] uvs = model.getDrawableVertexUvs(drawable);
      short[] indices = model.getDrawableIndices(drawable);
      if (vertices.length == 0 || uvs.length == 0 || indices.length == 0) {
         return;
      }

      RenderSystem.setShaderTexture(0, this.textures[textureIndex]);
      if (model.isDrawableAdditive(drawable)) {
         GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
      } else if (model.isDrawableMultiplicative(drawable)) {
         GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA);
      } else {
         GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      }

      Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);
      float boundsWidth = model.getBoundsWidth();
      float boundsHeight = model.getBoundsHeight();
      float scale = Math.min(width / boundsWidth, height / boundsHeight);
      float drawWidth = boundsWidth * scale;
      float drawHeight = boundsHeight * scale;
      float offsetX = x + (width - drawWidth) * 0.5F;
      float offsetY = y + (height - drawHeight) * 0.5F;
      for (short index : indices) {
         int vertexIndex = index & 0xFFFF;
         int base = vertexIndex * 2;
         if (base + 1 >= vertices.length || base + 1 >= uvs.length) {
            continue;
         }

         float px = offsetX + (vertices[base] - model.getBoundsMinX()) * scale;
         float py = offsetY + (model.getBoundsMaxY() - vertices[base + 1]) * scale;
         float u = uvs[base];
         float v = this.flipV ? 1.0F - uvs[base + 1] : uvs[base + 1];
         buffer.vertex(matrix, px, py, 0.0F).texture(u, v).color(1.0F, 1.0F, 1.0F, opacity);
      }
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.defaultBlendFunc();
   }

   private boolean isFrontBangOverlay(CubismNativeModel model, int drawable) {
      return this.frontBangDrawables.contains(model.getDrawableId(drawable));
   }

   private boolean isHiddenDrawable(CubismNativeModel model, int drawable) {
      return this.hiddenDrawables.contains(model.getDrawableId(drawable));
   }

   private boolean isFrontBangsCandidate(CubismNativeModel model, int drawable) {
      float[] vertices = model.getDrawableVertexPositions(drawable);
      if (vertices.length < 6) {
         return false;
      }

      float minX = Float.POSITIVE_INFINITY;
      float maxX = Float.NEGATIVE_INFINITY;
      float minY = Float.POSITIVE_INFINITY;
      float maxY = Float.NEGATIVE_INFINITY;
      for (int i = 0; i + 1 < vertices.length; i += 2) {
         minX = Math.min(minX, vertices[i]);
         maxX = Math.max(maxX, vertices[i]);
         minY = Math.min(minY, vertices[i + 1]);
         maxY = Math.max(maxY, vertices[i + 1]);
      }

      float modelW = model.getBoundsWidth();
      float modelH = model.getBoundsHeight();
      float centerX = (minX + maxX) * 0.5F;
      float centerY = (minY + maxY) * 0.5F;
      float nx = (centerX - model.getBoundsMinX()) / modelW;
      float ny = (centerY - model.getBoundsMinY()) / modelH;
      float nw = (maxX - minX) / modelW;
      float nh = (maxY - minY) / modelH;

      return nx > 0.28F && nx < 0.52F && ny > 0.48F && ny < 0.88F && nw < 0.18F && nh > 0.10F;
   }

   private void dumpOrderOnce(CubismNativeModel model, Integer[] sortedDrawables) {
      if (!this.debugOrder || this.debugOrderLogged) {
         return;
      }

      this.debugOrderLogged = true;
      System.err.println("[NoirLive2D] drawable order dump begin");
      System.err.println("[NoirLive2D] drawable count=" + sortedDrawables.length);
      for (int rank = 0; rank < sortedDrawables.length; rank++) {
         int drawable = sortedDrawables[rank];
         float[] bounds = getDrawableBounds(model, drawable);
         float nx = (bounds[0] + bounds[2]) * 0.5F;
         float ny = (bounds[1] + bounds[3]) * 0.5F;
         float nw = bounds[2] - bounds[0];
         float nh = bounds[3] - bounds[1];
         System.err.println(String.format(
            "[NoirLive2D] rank=%02d idx=%02d id=%s part=%s render=%02d draw=%02d mask=%d visible=%s center=%.3f,%.3f size=%.3f,%.3f frontGuess=%s",
            rank,
            drawable,
            model.getDrawableId(drawable),
            model.getDrawableParentPartId(drawable),
            model.getDrawableRenderOrder(drawable),
            model.getDrawableDrawOrder(drawable),
            model.getDrawableMaskCount(drawable),
            model.isDrawableVisible(drawable),
            nx,
            ny,
            nw,
            nh,
            isFrontBangOverlay(model, drawable)
         ));
      }
      System.err.println("[NoirLive2D] drawable order dump end");
   }

   private float[] getDrawableBounds(CubismNativeModel model, int drawable) {
      float[] vertices = model.getDrawableVertexPositions(drawable);
      if (vertices.length < 2) {
         return new float[]{0.0F, 0.0F, 0.0F, 0.0F};
      }

      float minX = Float.POSITIVE_INFINITY;
      float maxX = Float.NEGATIVE_INFINITY;
      float minY = Float.POSITIVE_INFINITY;
      float maxY = Float.NEGATIVE_INFINITY;
      for (int i = 0; i + 1 < vertices.length; i += 2) {
         minX = Math.min(minX, vertices[i]);
         maxX = Math.max(maxX, vertices[i]);
         minY = Math.min(minY, vertices[i + 1]);
         maxY = Math.max(maxY, vertices[i + 1]);
      }

      float modelW = model.getBoundsWidth();
      float modelH = model.getBoundsHeight();
      return new float[]{
         (minX - model.getBoundsMinX()) / modelW,
         (minY - model.getBoundsMinY()) / modelH,
         (maxX - model.getBoundsMinX()) / modelW,
         (maxY - model.getBoundsMinY()) / modelH
      };
   }

   public void preloadTextures(MinecraftClient client) {
      for (Identifier texture : this.textures) {
         client.getTextureManager().bindTexture(texture);
         RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
         RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
      }
   }
}
