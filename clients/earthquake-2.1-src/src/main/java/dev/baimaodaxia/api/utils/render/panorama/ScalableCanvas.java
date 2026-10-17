package dev.baimaodaxia.api.utils.render.panorama;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.Closeable;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;

public class ScalableCanvas implements Closeable {
   private final Matrix4f identity = new Matrix4f();
   private final Framebuffer input;
   private final Framebuffer output = MinecraftClient.getInstance().getFramebuffer();

   public ScalableCanvas() {
      this.input = new SimpleFramebuffer(this.output.textureWidth, this.output.textureHeight, false, false);
   }

   public void resize(int width, int height) {
      if (this.width() != width && this.height() != height && width > 0 && height > 0) {
         this.input.resize(width, height, MinecraftClient.IS_SYSTEM_MAC);
      }

   }

   public int width() {
      return this.input.textureWidth;
   }

   public int height() {
      return this.input.textureHeight;
   }

   public void write() {
      this.input.beginWrite(true);
   }

   public void read() {
      this.input.beginRead();
   }

   public void blit(VertexBuffer buffer, float alpha) {
      this.output.beginWrite(true);
      RenderSystem.setShaderTexture(0, this.input.getColorAttachment());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      RenderSystem.enableBlend();
      buffer.draw(this.identity, this.identity, GameRenderer.getPositionTexColorProgram());
   }

   public void close() {
      this.input.delete();
   }
}
