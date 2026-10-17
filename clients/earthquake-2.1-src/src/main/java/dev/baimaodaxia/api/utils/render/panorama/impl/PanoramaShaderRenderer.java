package dev.baimaodaxia.api.utils.render.panorama.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.api.utils.render.panorama.GlobalState;
import dev.baimaodaxia.api.utils.render.panorama.PanoramaRenderer;
import dev.baimaodaxia.api.utils.render.panorama.ScalableCanvas;
import java.util.Collections;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.lwjgl.opengl.GL30;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.gl.VertexBuffer.Usage;

public final class PanoramaShaderRenderer implements PanoramaRenderer {
   private final VertexBuffer buffer;
   private final Identifier texture;
   private final int program;
   private final int timeLoc;
   private final int mouseLoc;
   private final int resolutionLoc;
   private final int imageLoc;
   private final int backbufferLoc;
   private final int frameLoc;
   private final int persistentFrameLoc;
   private final int speedLoc;
   private final ScalableCanvas canvas;
   private final TextureManager manager;

   public PanoramaShaderRenderer(String vertex, String fragment, Identifier texture) {
      int vert = this.compileShader(vertex, 35633);
      int frag = this.compileShader(fragment, 35632);
      this.canvas = new ScalableCanvas();
      this.manager = MinecraftClient.getInstance().getTextureManager();
      this.buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
      this.program = GlStateManager.glCreateProgram();
      this.texture = texture;
      GlStateManager.glAttachShader(this.program, vert);
      GlStateManager.glAttachShader(this.program, frag);
      GlStateManager.glLinkProgram(this.program);
      if (GlStateManager.glGetProgrami(this.program, 35714) == 0) {
         String log = GlStateManager.glGetProgramInfoLog(this.program, 1024);
         throw new RuntimeException("Failed to link shader program! Caused by: " + log);
      } else {
         GlStateManager.glDeleteShader(vert);
         GlStateManager.glDeleteShader(frag);
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(-1.0F, -1.0F, 1.0F).texture(0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
         builder.vertex(1.0F, -1.0F, 1.0F).texture(1.0F, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
         builder.vertex(1.0F, 1.0F, 1.0F).texture(1.0F, 1.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
         builder.vertex(-1.0F, 1.0F, 1.0F).texture(0.0F, 1.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
         this.buffer.bind();
         this.buffer.upload(builder.end());
         VertexBuffer.unbind();
         this.timeLoc = GL30.glGetUniformLocation(this.program, "time");
         this.mouseLoc = GL30.glGetUniformLocation(this.program, "mouse");
         this.resolutionLoc = GL30.glGetUniformLocation(this.program, "resolution");
         this.imageLoc = GL30.glGetUniformLocation(this.program, "image");
         this.backbufferLoc = GL30.glGetUniformLocation(this.program, "backbuffer");
         this.frameLoc = GL30.glGetUniformLocation(this.program, "frame");
         this.persistentFrameLoc = GL30.glGetUniformLocation(this.program, "persistent_frame");
         this.speedLoc = GL30.glGetUniformLocation(this.program, "speed");
      }
   }

   private int compileShader(String source, int type) {
      int shader = GlStateManager.glCreateShader(type);
      GlStateManager.glShaderSource(shader, Collections.singletonList(source));
      GlStateManager.glCompileShader(shader);
      if (GlStateManager.glGetShaderi(shader, 35713) == 0) {
         String log = GlStateManager.glGetShaderInfoLog(shader, 1024);
         throw new RuntimeException("Failed to compile shader! Caused by: " + log);
      } else {
         return shader;
      }
   }

   public void draw(MinecraftClient client, double time, long frame, float mouseX, float mouseY, int width, int height, float alpha) {
      GL30.glUseProgram(this.program);
      float scale = 1.0F;
      this.canvas.resize((int)((float)width * scale), (int)((float)height * scale));
      this.canvas.write();
      if (this.texture != null) {
         RenderSystem.activeTexture(33984);
         this.manager.bindTexture(this.texture);
         GL30.glUniform1i(this.imageLoc, 0);
      }

      GL30.glUniform1f(this.timeLoc, (float)time);
      GL30.glUniform2f(this.mouseLoc, mouseX, mouseY);
      GL30.glUniform2f(this.resolutionLoc, (float)this.canvas.width(), (float)this.canvas.height());
      GL30.glUniform1i(this.frameLoc, (int)frame);
      GL30.glUniform1i(this.persistentFrameLoc, GlobalState.getFrame());
      GL30.glUniform1f(this.speedLoc, 1.0F);
      RenderSystem.activeTexture(33985);
      this.canvas.read();
      GL30.glUniform1i(this.backbufferLoc, 1);
      this.buffer.bind();
      this.buffer.draw();
      this.canvas.blit(this.buffer, alpha);
   }

   public void close() {
      GlStateManager.glDeleteProgram(this.program);
      this.buffer.close();
      this.canvas.close();
   }
}
