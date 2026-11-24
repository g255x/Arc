package dev.g255.arc.api.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryUtil;

/**
 * 整屏着色器（背景类）。
 *
 * <p>为什么不用 Satin / 原版 postEffect：
 * <ul>
 *   <li>原版的 postEffect 路径是写死的 —— 必须放在 {@code shaders/post/*.json} +
 *       {@code shaders/program/*.json|*.fsh}，不能放到 {@code shaders/arc/} 这种自定义目录；</li>
 *   <li>加载界面（SplashOverlay）渲染的时候资源管理器还没把 mod 资源装进来，
 *       Satin 拿 {@link MinecraftClient#getResourceManager()} 去查 json 会直接失败。</li>
 * </ul>
 *
 * <p>所以这里直接从 classpath 读 {@code assets/minecraft/shaders/arc/<name>.vsh|.fsh}，
 * 自己编译链接，画一个覆盖整个裁剪空间的四边形，完全不受资源重载时机影响，
 * splash 的第一帧就能用。顶点着色器统一是 {@code quad.vsh}。
 *
 * <p>片元着色器约定（与 {@code arc/} 目录里现有的两个保持一致）：
 * <ul>
 *   <li>顶点阶段提供 {@code texCoord} 和 {@code v_TexCoord}（都是 0~1），按需取用；</li>
 *   <li>片元输出 {@code fragColor}，并且挂在 {@code #version 150} 上（core profile 不支持
 *       {@code varying} / {@code gl_FragColor}）；</li>
 *   <li>{@code g_Time}（秒）、{@code g_Screen}（像素宽高）由本类自动喂，
 *       {@code g_Texture0} 统一绑定成一张 1x1 黑图（用于做 screen 混合的底色）。</li>
 * </ul>
 *
 * <p>编译失败只打印一次日志并永久返回 null，调用方据此回退到兜底背景，不会每帧重试拖慢渲染。
 */
public final class ArcShader {
   private static final String BASE = "/assets/minecraft/shaders/arc/";
   private static final String VERTEX = "quad";
   private static final Map<String, ArcShader> CACHE = new HashMap<>();
   private static final Set<String> FAILED = new HashSet<>();
   private static final long START = System.nanoTime();

   private final String name;
   private final int program;
   private final int vao;
   private final int vbo;
   private final int blackTexture;
   private final Map<String, Integer> locations = new HashMap<>();
   /** 待上传的 uniform，draw() 里绑定好 program 之后统一提交 —— 调用方不用关心 GL 状态。 */
   private final Map<String, float[]> pending = new HashMap<>();
   /** 每帧都要传的三个内置 uniform，location 直接缓存成 int，省掉三次 Map 查找。 */
   private final int uTime;
   private final int uScreen;
   private final int uAlpha;
   private final int uTexture;

   private ArcShader(String name) {
      this.name = name;
      String vertexSource = read(VERTEX + ".vsh");
      String fragmentSource = read(name + ".fsh");
      int vertex = compile(GL20C.GL_VERTEX_SHADER, vertexSource, VERTEX + ".vsh");
      int fragment = compile(GL20C.GL_FRAGMENT_SHADER, fragmentSource, name + ".fsh");
      int handle = GL20C.glCreateProgram();
      GL20C.glAttachShader(handle, vertex);
      GL20C.glAttachShader(handle, fragment);
      GL20C.glBindAttribLocation(handle, 0, "Position");
      GL20C.glLinkProgram(handle);
      GL20C.glDeleteShader(vertex);
      GL20C.glDeleteShader(fragment);
      if (GL20C.glGetProgrami(handle, GL20C.GL_LINK_STATUS) == 0) {
         String log = GL20C.glGetProgramInfoLog(handle);
         GL20C.glDeleteProgram(handle);
         throw new IllegalStateException("ArcShader " + name + " 链接失败: " + log);
      }

      this.program = handle;
      this.vao = GlStateManager._glGenVertexArrays();
      this.vbo = GL15C.glGenBuffers();
      GlStateManager._glBindVertexArray(this.vao);
      GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vbo);
      FloatBuffer quad = MemoryUtil.memAllocFloat(8);
      quad.put(-1.0F).put(-1.0F).put(1.0F).put(-1.0F).put(-1.0F).put(1.0F).put(1.0F).put(1.0F).flip();
      GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, quad, GL15C.GL_STATIC_DRAW);
      MemoryUtil.memFree(quad);
      GlStateManager._vertexAttribPointer(0, 2, GL11C.GL_FLOAT, false, 0, 0L);
      GlStateManager._enableVertexAttribArray(0);
      GlStateManager._glBindVertexArray(0);
      GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);

      this.blackTexture = GL11C.glGenTextures();
      GlStateManager._bindTexture(this.blackTexture);
      ByteBuffer black = MemoryUtil.memCalloc(4);
      GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, 1, 1, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, black);
      MemoryUtil.memFree(black);
      GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
      GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
      GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
      GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);
      GlStateManager._bindTexture(0);

      this.uTime = GL20C.glGetUniformLocation(handle, "g_Time");
      this.uScreen = GL20C.glGetUniformLocation(handle, "g_Screen");
      this.uAlpha = GL20C.glGetUniformLocation(handle, "uAlpha");
      this.uTexture = GL20C.glGetUniformLocation(handle, "g_Texture0");
   }

   /**
    * 取（必要时编译）指定名字的着色器。名字对应 {@code assets/minecraft/shaders/arc/<name>.fsh}。
    * 编译/链接失败返回 null，且不会重复尝试。
    */
   public static ArcShader get(String name) {
      ArcShader cached = CACHE.get(name);
      if (cached != null) {
         return cached;
      }

      if (FAILED.contains(name)) {
         return null;
      }

      try {
         ArcShader shader = new ArcShader(name);
         CACHE.put(name, shader);
         return shader;
      } catch (Throwable t) {
         FAILED.add(name);
         System.out.println("[Arc] 着色器 " + name + " 不可用，改用兜底背景 -> " + t);
         return null;
      }
   }

   /** 排队一个 uniform（1~4 个 float），实际在 {@link #draw(float)} 里提交。 */
   public ArcShader set(String uniform, float... values) {
      if (uniform != null && values != null && values.length > 0) {
         this.pending.put(uniform, values.clone());
      }

      return this;
   }

   /** 整屏画这张背景，alpha 0~1 用于淡入淡出。 */
   public void draw(float alpha) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null) {
         return;
      }

      int fbWidth = mc.getWindow().getFramebufferWidth();
      int fbHeight = mc.getWindow().getFramebufferHeight();
      if (fbWidth <= 0 || fbHeight <= 0) {
         return;
      }

      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableScissor();
      ShaderProgram previous = RenderSystem.getShader();
      int previousTextureUnit = GlStateManager._getActiveTexture();
      GlStateManager._glUseProgram(this.program);

      for (Map.Entry<String, float[]> entry : this.pending.entrySet()) {
         this.upload(entry.getKey(), entry.getValue());
      }

      // 时间和分辨率每帧都变，放在队列后面覆盖调用方可能塞进来的旧值
      GL20C.glUniform1f(this.uTime, (float)(System.nanoTime() - START) / 1.0E9F);
      GL20C.glUniform3f(this.uScreen, (float)fbWidth, (float)fbHeight, 1.0F);
      GL20C.glUniform1f(this.uAlpha, alpha);
      if (this.uTexture >= 0) {
         GL20C.glUniform1i(this.uTexture, 0);
      }
      GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
      GlStateManager._bindTexture(this.blackTexture);
      GlStateManager._glBindVertexArray(this.vao);
      GL11C.glDrawArrays(GL11C.GL_TRIANGLE_STRIP, 0, 4);
      GlStateManager._glBindVertexArray(0);

      // 还原：这里必须走 ShaderProgram.unbind() + bind()，而不是只把 RenderSystem 里那个
      // shader 字段写回去。原因有三个：
      //   1) ShaderProgram 自己缓存了 activeProgramGlRef，直接 _glUseProgram 会让它和真实的
      //      GL 状态脱节，之后同一 program 再 bind() 时会跳过真正的 glUseProgram；
      //   2) unbind() 会把缓存置空、把各采样器单元解绑，bind() 再重新绑定 program、
      //      重新上传 uniform、重新按 sampler 绑纹理 —— 这正是原版每次 draw 收尾会做的事；
      //   3) 采样器重新绑定顺手把上面的 _bindTexture(blackTexture) 也一起修好了。
      if (previous != null) {
         previous.unbind();
         previous.bind();
      } else {
         GL20C.glUseProgram(0);
      }

      GlStateManager._activeTexture(previousTextureUnit);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
   }

   private void upload(String uniform, float[] values) {
      int location = this.location(uniform);
      if (location < 0) {
         return;
      }

      switch (values.length) {
         case 1:
            GL20C.glUniform1f(location, values[0]);
            break;
         case 2:
            GL20C.glUniform2f(location, values[0], values[1]);
            break;
         case 3:
            GL20C.glUniform3f(location, values[0], values[1], values[2]);
            break;
         default:
            GL20C.glUniform4f(location, values[0], values[1], values[2], values[3]);
      }
   }

   private int location(String uniform) {
      Integer cached = this.locations.get(uniform);
      if (cached != null) {
         return cached;
      }

      int location = GL20C.glGetUniformLocation(this.program, uniform);
      this.locations.put(uniform, location);
      return location;
   }

   private static int compile(int type, String source, String label) {
      int shader = GL20C.glCreateShader(type);
      GL20C.glShaderSource(shader, source);
      GL20C.glCompileShader(shader);
      if (GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) == 0) {
         String log = GL20C.glGetShaderInfoLog(shader);
         GL20C.glDeleteShader(shader);
         throw new IllegalStateException("ArcShader " + label + " 编译失败: " + log);
      }

      return shader;
   }

   private static String read(String file) {
      try (InputStream in = ArcShader.class.getResourceAsStream(BASE + file)) {
         if (in == null) {
            throw new IllegalStateException("找不到 assets" + BASE + file);
         }

         return new String(in.readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException e) {
         throw new IllegalStateException("读取 assets" + BASE + file + " 失败", e);
      }
   }

   @Override
   public String toString() {
      return "ArcShader[" + this.name + "#" + this.program + "]";
   }
}
