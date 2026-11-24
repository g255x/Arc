package dev.g255.arc.mod.gui;

import dev.g255.arc.Arc;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.core.impl.ShaderManager;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.Window;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;

/**
 * 主菜单 / 加载界面的极光背景。
 *
 * <p>移植自 OpenHachimi(Hachimi V2 还原版) 的主菜单着色器，明文版本放在
 * {@code assets/minecraft/shaders/arc/menu.fsh}，由 {@link ArcShader} 直接编译（不经过原版
 * postEffect 的 {@code shaders/post} + {@code shaders/program} 固定路径）。
 *
 * <p>着色器完全不采样输入纹理，纯程序化生成极光与星空：以 GUI 缩放尺寸作为 {@code resolution}，
 * 用 {@code time} 驱动流动与镜头微摆，用归一化后的鼠标位置 {@code mouse} 控制相机朝向。
 */
public final class MenuBackground {
   /** 相机朝向，0~1 归一化后做线性插值，跟随鼠标（与 Hachimi 的 f / g 一致）。 */
   private static double yaw = 0.5D;
   private static double pitch = 0.5D;
   /**
    * 每帧跟随比例，完全对应 Hachimi {@code TextDrawScreen.ey} 里的
    * {@code PhantomView.DH(f, mouseX / width, 0.01)}（着色器模式取 0.01，图片模式取 0.02）。
    */
   private static final double MOUSE_FOLLOW = 0.01D;
   private static long startTime = System.currentTimeMillis();

   private MenuBackground() {
   }

   /**
    * 传入的必须是 <b>GUI 缩放后的坐标</b>（0 ~ width），不是 GLFW 原始窗口像素。
    * 光标到 GUI 坐标的换算见 {@link #updateMouseFromCursor(MinecraftClient)}。
    */
   public static void updateMouse(double mouseX, double mouseY, int width, int height) {
      if (width <= 0 || height <= 0 || (mouseX == 0.0D && mouseY == 0.0D)) {
         return;
      }

      // 目标值恒在 [0,1]：即便调用方传错单位，也只会提前饱和，不会一步冲过 1 被硬截断
      double targetX = clamp01(mouseX / (double)width);
      double targetY = clamp01(mouseY / (double)height);
      yaw = approach(yaw, targetX, MOUSE_FOLLOW);
      pitch = approach(pitch, targetY, MOUSE_FOLLOW);
      yaw = clamp01(yaw);
      pitch = clamp01(pitch);
   }

   /**
    * {@code mc.mouse.getX()/getY()} 是 GLFW 的原始窗口像素（0 ~ window.getWidth()），
    * 而界面宽高是 GUI 缩放后的坐标（0 ~ window.getScaledWidth()）。两者单位相差一个
    * GUI Scale（通常 2~4），直接相除会得到 0 ~ scaleFactor：
    * 目标值在屏幕 1/scale 处就已经是 1，于是右半边 / 下半边全都撞在 clamp 上，
    * 相机早早贴死、鼠标还在继续推进 —— 这就是"右边/下边突然停住、左边/上边有缓冲"的原因。
    *
    * <p>换算公式与 vanilla 完全一致（{@code GameRenderer.render} 里给 Screen 用的就是这条）：
    * {@code gui = raw * getScaledWidth() / getWidth()}。
    */
   private static void updateMouseFromCursor(MinecraftClient mc) {
      if (mc.mouse == null) {
         return;
      }

      Window window = mc.getWindow();
      int rawWidth = window.getWidth();
      int rawHeight = window.getHeight();
      if (rawWidth <= 0 || rawHeight <= 0) {
         return;
      }

      double guiX = mc.mouse.getX() * (double)window.getScaledWidth() / (double)rawWidth;
      double guiY = mc.mouse.getY() * (double)window.getScaledHeight() / (double)rawHeight;
      updateMouse(guiX, guiY, window.getScaledWidth(), window.getScaledHeight());
   }

   /**
    * 与 Hachimi {@code PhantomView.DH} 完全等价的线性逼近：
    * {@code current + (target - current) * speed}，speed 为 1 直接到位、为 0 不动。
    */
   private static double approach(double current, double target, double speed) {
      if (speed >= 1.0D) {
         return target;
      }

      if (speed == 0.0D) {
         return current;
      }

      return current + (target - current) * speed;
   }

   private static double clamp01(double value) {
      return value < 0.0D ? 0.0D : (value > 1.0D ? 1.0D : value);
   }

   /**
    * 统一的界面背景入口：更新鼠标朝向 → 渲染极光(失败则画兜底背景)。
    * 主菜单、选项、多人游戏、选择世界、连接/加载等所有"非游戏内"界面都走这里。
    */
   public static void renderScreen(DrawContext context, int width, int height) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null) {
         updateMouseFromCursor(mc);
      }

      if (width > 0 && height > 0) {
         // 先铺一层不透明底色并立刻提交，原因有两个：
         // 1) 着色器这一帧万一没画成功（刚换分辨率、资源重载、effect 重建），
         //    底下这层底色会顶住，不会露出上一帧的界面内容；
         // 2) 极光是在 post effect 里直接写主帧缓冲的，若此前还有未提交的 GUI 顶点，
         //    两边会画在同一张图上叠成「两帧重合」，先 draw() 落地可以避免。
         context.fill(0, 0, width, height, 0xFF07060E);
         context.draw();
      }

      if (!render(context, width, height)) {
         renderFallback(context, width, height);
      }
   }

   /**
    * 压一层暗色，用于加载/提示类界面（白字在亮色极光上更好读）。
    */
   public static void dim(DrawContext context, int width, int height) {
      context.fill(0, 0, width, height, 0x4A000000);
   }

   /**
    * 把整屏替换成极光背景。成功返回 true，着色器不可用时返回 false（调用方自行回退）。
    *
    * <p>渲染方式和 OpenHachimi 一致：走 Satin 托管的 post effect
    * （{@code arc:shaders/post/menu.json} → {@code arc:menu} 程序），
    * 由原版 {@code PostEffectProcessor} 负责切换帧缓冲、绘制和还原 GL 状态。
    * 之前在这里手写 {@code glUseProgram} + {@code glDrawArrays}，
    * 收尾一旦和 {@code ShaderProgram} 的静态缓存对不上，后续整批 GUI 就画不出来，
    * 主菜单也会变成一片纯色 —— 交给原版链路就没有这个问题。</p>
    */
   public static boolean render(DrawContext context, int width, int height) {
      if (width <= 0 || height <= 0) {
         return false;
      }

      ManagedShaderEffect effect = ShaderManager.MENU;
      if (effect == null || effect.getShaderEffect() == null) {
         // 第一次进菜单时 ShaderManager 可能还没初始化，触发一次懒加载再取
         if (Arc.SHADER != null) {
            Arc.SHADER.fullNullCheck();
            effect = ShaderManager.MENU;
         }

         if (effect == null || effect.getShaderEffect() == null) {
            return false;
         }
      }

      try {
         // 原生 GL / 帧缓冲切换前，先把排队的 GUI 顶点落地，避免叠成「两帧重合」
         context.draw();
         // 与 Hachimi 完全一致：resolution 用 GUI 缩放尺寸，mouse 传 (f, 1 - g)
         effect.setUniformValue("resolution", (float)width, (float)height);
         effect.setUniformValue("time", (float)(System.currentTimeMillis() - startTime) / 1000.0F);
         effect.setUniformValue("mouse", (float)yaw, (float)(1.0D - pitch));
         effect.render(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true));
         return true;
      } catch (Throwable t) {
         return false;
      }
   }

   /**
    * 极光着色器不可用时的兜底背景（渐变色 + 斜线网格 + Gradient 着色器）。
    */
   public static void renderFallback(DrawContext context, int width, int height) {
      float w = (float)width;
      float h = (float)height;
      MatrixStack m = context.getMatrices();
      Render2DUtil.verticalGradient(m, 0.0F, 0.0F, w, h, new Color(40, 10, 60, 255), new Color(200, 20, 150, 255));
      long now = System.currentTimeMillis();
      float phase = (float)(now % 6000L) / 6000.0F;
      float angle = 0.523599F;
      float dx = (float)Math.tan((double)angle) * h;
      float base = -h;
      float spacing = 36.0F;
      float shift = phase * spacing * 4.0F;
      Color c1 = new Color(220, 60, 170, 52);
      Color c2 = new Color(160, 40, 130, 36);

      for(float i = base; i < w; i += spacing) {
         float x0 = i + shift;
         Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, c1.getRGB());
      }

      for(float i = base + spacing / 2.0F; i < w; i += spacing) {
         float x0 = i + shift * 0.8F;
         Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, c2.getRGB());
      }

      Color c3 = new Color(255, 255, 255, 18);

      for(float y = 0.0F; y <= h; y += 64.0F) {
         Render2DUtil.drawLine(m, 0.0F, y, w, y, c3.getRGB());
      }

      if (!Arc.SHADER.fullNullCheck()) {
         ManagedShaderEffect gradient = Arc.SHADER.getShader(ShaderManager.Shader.Gradient);
         gradient.setUniformValue("alpha2", 0.2F);
         gradient.setUniformValue("rgb", 0.78F, 0.05F, 0.59F);
         gradient.setUniformValue("rgb1", 0.56F, 0.06F, 0.68F);
         gradient.setUniformValue("rgb2", 0.93F, 0.12F, 0.63F);
         gradient.setUniformValue("rgb3", 0.64F, 0.0F, 0.64F);
         gradient.setUniformValue("step", 180.0F);
         gradient.setUniformValue("radius", 2.0F);
         gradient.setUniformValue("quality", 1.0F);
         gradient.setUniformValue("divider", 150.0F);
         gradient.setUniformValue("maxSample", 10.0F);
         gradient.setUniformValue("resolution", w, h);
         float t = (float)(now % 100000L) / 1000.0F;
         gradient.setUniformValue("time", t * 300.0F);
         gradient.render(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true));
      }
   }
}
