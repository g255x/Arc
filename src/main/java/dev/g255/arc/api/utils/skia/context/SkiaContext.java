package dev.g255.arc.api.utils.skia.context;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceColorFormat;
import io.github.humbleui.skija.SurfaceOrigin;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33;

public class SkiaContext {
   private static DirectContext context;
   private static Surface surface;
   private static BackendRenderTarget renderTarget;
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static int lastFramebufferId = -1;
   private static Boolean skijaAvailable = null;
   /**
    * 总开关 —— 现已<b>永久关闭</b>。
    *
    * <p>Skija 是直接包主帧缓冲的原生互操作：每帧要读回二三十个 GL 状态（每次 glGetInteger
    * 都是一次管线同步），画完再靠「先设成别的值、再设回原值」去骗过 {@code GlStateManager}
    * 的缓存，其中 {@code viewport / scissorBox} 还被 {@code +1} 撞出了范围，
    * 于是驱动每帧吐一条 {@code GL_INVALID_VALUE}（17:42 那次运行两分钟刷了 1.1 万条），
    * 帧率被拖到个位数、游戏频繁未响应。</p>
    *
    * <p>现在唯一用 Skija 的 EnemyInfo 已经整块改成原版 {@code DrawContext} + {@code TextRenderer}，
    * {@link dev.g255.arc.mod.modules.impl.render.DeathFx} 拿不到画布时会自行退化成空操作。
    * 所以这里直接关死：{@link #draw} / {@link #createSurface} 全为空实现，
    * 保证我们这边再也不会碰原始 GL 状态。</p>
    */
   public static boolean enabled = false;

   public static boolean isSkijaAvailable() {
      if (skijaAvailable == null) {
         try {
            Class.forName("io.github.humbleui.skija.DirectContext");
            skijaAvailable = true;
         } catch (Throwable t) {
            skijaAvailable = false;
            System.err.println("[Arc] Skija native library is not available on this platform. Skia-based rendering will be disabled.");
         }
      }

      return skijaAvailable;
   }

   public static Canvas getCanvas() {
      return surface != null ? surface.getCanvas() : null;
   }

   public static DirectContext getContext() {
      return context;
   }

   /**
    * 创建（或复用）包住当前主帧缓冲的 Skia 表面。
    *
    * @param guiWidth  <b>GUI 缩放后</b>的宽度（{@code window.getScaledWidth()}），仅用于判断尺寸是否有效
    * @param guiHeight <b>GUI 缩放后</b>的高度，同上
    *
    * <p><b>为什么要传 GUI 尺寸：</b>调用方画的是 GUI 坐标，这里只拿它做合法性判断。
    * 真正的表面尺寸一律取帧缓冲像素尺寸（{@code getFramebufferWidth/Height}）——
    * {@code BackendRenderTarget.makeGL(w, h, ...)} 的 w/h 必须和真实 FBO 逐像素一致，
    * 否则 Skia 的视口会和帧缓冲错开，画出来的内容被拉伸并偏移，表现就是
    * 「敌人信息明显错位、字号变大」。{@link #draw} 里再用 {@code canvas.scale(scaleFactor)}
    * 把 GUI 坐标换算成像素坐标，两者配合才能让 GUI 坐标 1:1 落到屏幕上。</p>
    */
   public static void createSurface(int guiWidth, int guiHeight) {
      if (!enabled) {
         return;
      }
      if (guiWidth > 0 && guiHeight > 0) {
         // 表面尺寸必须和真实 FBO 的像素尺寸**完全一致**：
         // 差一个像素，Skia 的视口就和帧缓冲错开，整块内容会被拉伸并偏移
         // —— 敌人信息「明显错位」就是这么来的。所以这里直接取帧缓冲尺寸，
         // 不再用 scaledWidth * scaleFactor 反算（取整会差 1~2 px）。
         int width;
         int height;
         try {
            width = MinecraftClient.getInstance().getWindow().getFramebufferWidth();
            height = MinecraftClient.getInstance().getWindow().getFramebufferHeight();
         } catch (Throwable t) {
            return;
         }

         if (isSkijaAvailable()) {
            int framebufferId;
            try {
               framebufferId = MinecraftClient.getInstance().getFramebuffer().fbo;
            } catch (Throwable var7) {
               return;
            }

            if (surface == null || renderTarget == null || width != lastWidth || height != lastHeight || framebufferId != lastFramebufferId) {
               if (context == null) {
                  try {
                     context = DirectContext.makeGL();
                  } catch (Throwable t) {
                     skijaAvailable = false;
                     System.err.println("[Arc] Failed to create Skija DirectContext. Skia-based rendering will be disabled.");
                     return;
                  }
               }

               closeSurface();

               try {
                  int sampleCount = Math.max(0, GL11.glGetInteger(32937));
                  int stencilBits = Math.max(0, GL11.glGetInteger(3415));
                  if ((surface = Surface.wrapBackendRenderTarget(
                        context,
                        renderTarget = BackendRenderTarget.makeGL(width, height, sampleCount, stencilBits, framebufferId, 32856),
                        SurfaceOrigin.BOTTOM_LEFT,
                        SurfaceColorFormat.RGBA_8888,
                        ColorSpace.getSRGB()
                     )) == null) {
                     closeSurface();
                     return;
                  }

                  lastWidth = width;
                  lastHeight = height;
                  lastFramebufferId = framebufferId;
               } catch (Throwable var5) {
                  closeSurface();
               }
            }
         }
      }
   }

   /**
    * Draws with Skia while keeping Minecraft's {@code GlStateManager} caches in sync.
    *
    * <p>Skia issues raw OpenGL calls, so every cached field inside {@code GlStateManager}
    * (texture binding, active unit, program, framebuffer, viewport, blend, ...) becomes stale.
    * Because {@code GlStateManager} skips the real GL call when its cache already holds the
    * requested value, restoring through it would be a no-op. Every restore below therefore
    * sets a deliberately different value first, forcing a real GL call, and only then sets
    * the wanted value again - that leaves both the driver state and the cache correct.</p>
    */
   public static void draw(Consumer<Canvas> drawingLogic) {
      if (enabled && isSkijaAvailable() && context != null && surface != null && drawingLogic != null) {
         int previousActiveTexture = GL11.glGetInteger(34016);
         int previousTextureBinding = GL11.glGetInteger(32873);
         int previousSamplerBinding = GL11.glGetInteger(35097);
         int previousProgram = GL11.glGetInteger(35725);
         int previousArrayBuffer = GL11.glGetInteger(34964);
         int previousElementArrayBuffer = GL11.glGetInteger(34965);
         int previousVertexArray = GL11.glGetInteger(34229);
         int previousFramebufferBinding = GL11.glGetInteger(36006);
         int previousReadFramebufferBinding = GL11.glGetInteger(36010);
         int previousRenderbufferBinding = GL11.glGetInteger(36007);
         int previousBlendSrcRgb = GL11.glGetInteger(32969);
         int previousBlendDstRgb = GL11.glGetInteger(32968);
         int previousBlendSrcAlpha = GL11.glGetInteger(32971);
         int previousBlendDstAlpha = GL11.glGetInteger(32970);
         int previousBlendEqRgb = GL11.glGetInteger(32777);
         int previousDepthFunc = GL11.glGetInteger(2932);
         int previousCullFace = GL11.glGetInteger(2885);
         int previousFrontFace = GL11.glGetInteger(2886);
         boolean blendEnabled = GL11.glIsEnabled(3042);
         boolean depthEnabled = GL11.glIsEnabled(2929);
         boolean cullEnabled = GL11.glIsEnabled(2884);
         boolean scissorEnabled = GL11.glIsEnabled(3089);
         boolean stencilEnabled = GL11.glIsEnabled(2960);
         boolean depthMask = GL11.glGetBoolean(2930);
         ByteBuffer colorMask = BufferUtils.createByteBuffer(4);
         GL11.glGetBooleanv(3107, colorMask);
         boolean colorMaskRed = colorMask.get(0) != 0;
         boolean colorMaskGreen = colorMask.get(1) != 0;
         boolean colorMaskBlue = colorMask.get(2) != 0;
         boolean colorMaskAlpha = colorMask.get(3) != 0;
         IntBuffer previousViewport = BufferUtils.createIntBuffer(4);
         IntBuffer previousScissorBox = BufferUtils.createIntBuffer(4);
         GL11.glGetIntegerv(2978, previousViewport);
         GL11.glGetIntegerv(3088, previousScissorBox);
         int unpackRowLength = GL11.glGetInteger(3314);
         int unpackSkipPixels = GL11.glGetInteger(3316);
         int unpackSkipRows = GL11.glGetInteger(3315);
         int unpackAlignment = GL11.glGetInteger(3317);
         float[] previousShaderColor = RenderSystem.getShaderColor().clone();
         RenderSystem.pixelStore(3314, 0);
         RenderSystem.pixelStore(3316, 0);
         RenderSystem.pixelStore(3315, 0);
         RenderSystem.pixelStore(3317, 4);
         context.resetGLAll();

         try {
            Canvas canvas = getCanvas();
            if (canvas != null) {
               // 用「帧缓冲像素 / GUI 尺寸」的真实比值缩放，而不是 getScaleFactor()。
               // MC 的 scaledWidth 是 ceil(framebuffer / scale) 取整来的，两者回乘常常差 1~2 px，
               // 用 scaleFactor 缩放会让画出来的内容整体偏一点（越靠右下偏得越多）。
               Window window = MinecraftClient.getInstance().getWindow();
               float scaleX = (float)lastWidth / (float)Math.max(1, window.getScaledWidth());
               float scaleY = (float)lastHeight / (float)Math.max(1, window.getScaledHeight());
               canvas.save();
               canvas.scale(scaleX, scaleY);
               drawingLogic.accept(canvas);
               canvas.restore();
               context.flush();
               return;
            }
         } finally {
            context.resetGLAll();

            // --- pixel store: force a real glPixelStorei for every parameter ---
            forcePixelStore(3314, unpackRowLength);
            forcePixelStore(3316, unpackSkipPixels);
            forcePixelStore(3315, unpackSkipRows);
            forcePixelStore(3317, unpackAlignment);

            // --- fixed function toggles ---
            if (blendEnabled) {
               GlStateManager._disableBlend();
               GlStateManager._enableBlend();
            } else {
               GlStateManager._enableBlend();
               GlStateManager._disableBlend();
            }

            forceBlendFunc(previousBlendSrcRgb, previousBlendDstRgb, previousBlendSrcAlpha, previousBlendDstAlpha, previousBlendEqRgb);

            if (depthEnabled) {
               GlStateManager._disableDepthTest();
               GlStateManager._enableDepthTest();
            } else {
               GlStateManager._enableDepthTest();
               GlStateManager._disableDepthTest();
            }

            GlStateManager._depthFunc(519);
            GlStateManager._depthFunc(previousDepthFunc);
            GlStateManager._depthMask(!depthMask);
            GlStateManager._depthMask(depthMask);

            if (cullEnabled) {
               GlStateManager._disableCull();
               GlStateManager._enableCull();
            } else {
               GlStateManager._enableCull();
               GlStateManager._disableCull();
            }

            GL11.glCullFace(previousCullFace == 1029 ? 1028 : 1029);
            GL11.glCullFace(previousCullFace);
            GL11.glFrontFace(previousFrontFace == 2304 ? 2305 : 2304);
            GL11.glFrontFace(previousFrontFace);

            if (scissorEnabled) {
               GlStateManager._enableScissorTest();
               GlStateManager._scissorBox(previousScissorBox.get(0), previousScissorBox.get(1), previousScissorBox.get(2) + 1, previousScissorBox.get(3));
               GlStateManager._scissorBox(previousScissorBox.get(0), previousScissorBox.get(1), previousScissorBox.get(2), previousScissorBox.get(3));
            } else {
               GlStateManager._disableScissorTest();
            }

            if (stencilEnabled) {
               GL11.glEnable(2960);
            } else {
               GL11.glDisable(2960);
            }

            // --- bindings: the critical part, MC caches these per texture unit / target ---
            forceActiveTexture(previousActiveTexture);
            forceBindTexture(previousTextureBinding);
            GL33.glBindSampler(0, 0);
            GL33.glBindSampler(0, previousSamplerBinding);
            forceUseProgram(previousProgram);
            forceBindBuffer(34962, previousArrayBuffer);
            forceBindBuffer(34963, previousElementArrayBuffer);
            GlStateManager._glBindVertexArray(0);
            GlStateManager._glBindVertexArray(previousVertexArray);
            forceBindFramebuffer(36160, previousFramebufferBinding);
            forceBindFramebuffer(36008, previousReadFramebufferBinding);
            GlStateManager._glBindRenderbuffer(36161, previousRenderbufferBinding);

            GlStateManager._viewport(previousViewport.get(0), previousViewport.get(1), previousViewport.get(2) + 1, previousViewport.get(3));
            GlStateManager._viewport(previousViewport.get(0), previousViewport.get(1), previousViewport.get(2), previousViewport.get(3));

            GlStateManager._colorMask(!colorMaskRed, !colorMaskGreen, !colorMaskBlue, !colorMaskAlpha);
            GlStateManager._colorMask(colorMaskRed, colorMaskGreen, colorMaskBlue, colorMaskAlpha);
            RenderSystem.setShaderColor(previousShaderColor[0], previousShaderColor[1], previousShaderColor[2], previousShaderColor[3]);
         }
      }
   }

   private static void forcePixelStore(int pname, int value) {
      RenderSystem.pixelStore(pname, value == 0 ? 1 : 0);
      RenderSystem.pixelStore(pname, value);
   }

   private static void forceBlendFunc(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha, int equation) {
      GlStateManager._blendFuncSeparate(770, 771, 770, 771);
      GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
      GlStateManager._blendEquation(equation == 32774 ? 32778 : 32774);
      GlStateManager._blendEquation(equation);
   }

   private static void forceActiveTexture(int unit) {
      int other = unit == 33984 ? 33985 : 33984;
      GlStateManager._activeTexture(other);
      GlStateManager._activeTexture(unit);
   }

   private static void forceBindTexture(int id) {
      if (id != 0) {
         GlStateManager._bindTexture(0);
         GlStateManager._bindTexture(id);
      }
   }

   private static void forceUseProgram(int id) {
      GlStateManager._glUseProgram(0);
      if (id != 0) {
         GlStateManager._glUseProgram(id);
      }
   }

   private static void forceBindBuffer(int target, int id) {
      GlStateManager._glBindBuffer(target, 0);
      if (id != 0) {
         GlStateManager._glBindBuffer(target, id);
      }
   }

   private static void forceBindFramebuffer(int target, int id) {
      int other = id == 0 ? mainFramebufferId() : 0;
      GlStateManager._glBindFramebuffer(target, other);
      GlStateManager._glBindFramebuffer(target, id);
   }

   private static int mainFramebufferId() {
      try {
         return MinecraftClient.getInstance().getFramebuffer().fbo;
      } catch (Throwable t) {
         return 0;
      }
   }

   private static void closeSurface() {
      if (surface != null) {
         surface.close();
         surface = null;
      }

      if (renderTarget != null) {
         renderTarget.close();
         renderTarget = null;
      }

      lastWidth = -1;
      lastHeight = -1;
      lastFramebufferId = -1;
   }
}
