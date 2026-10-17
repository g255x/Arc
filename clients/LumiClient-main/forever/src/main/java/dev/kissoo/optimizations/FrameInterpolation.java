package dev.kissoo.optimizations;

import dev.kissoo.mod.modules.impl.render.FrameInterp;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

public class FrameInterpolation {

    private static int lastFrameTex = -1;
    private static int lastFrameFbo = -1;
    private static int lastWidth, lastHeight;
    private static final MotionShaderProgram shader = new MotionShaderProgram();
    private static final int[] VIEWPORT_BUFFER = new int[4];
    private static boolean initialized = false;

    public static void interp() {
        if (FrameInterp.INSTANCE == null || !FrameInterp.INSTANCE.isOn()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        Screen screen = client.currentScreen;
        if (screen instanceof ProgressScreen || screen instanceof DownloadingTerrainScreen || screen instanceof ConnectScreen) {
            return;
        }

        Framebuffer fb = client.getFramebuffer();
        Window window = client.getWindow();
        if (fb == null || window == null) return;

        int w = fb.textureWidth;
        int h = fb.textureHeight;

        if (!initialized) {
            shader.init();
            initialized = true;
        }

        if (lastFrameTex == -1 || w != lastWidth || h != lastHeight) {
            init(w, h);
            capture(fb, w, h);
            return;
        }

        long handle = window.getHandle();

        int prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT_BUFFER);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, w, h);
        shader.render(fb.getColorAttachment(), lastFrameTex, w, h);

        GLFW.glfwSwapBuffers(handle);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fb.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

        capture(fb, w, h);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
        GL11.glViewport(VIEWPORT_BUFFER[0], VIEWPORT_BUFFER[1], VIEWPORT_BUFFER[2], VIEWPORT_BUFFER[3]);
    }

    private static void capture(Framebuffer fb, int w, int h) {
        int prevReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fb.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, lastFrameFbo);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);

        lastWidth = w;
        lastHeight = h;
    }

    private static void init(int w, int h) {
        if (lastFrameTex != -1) GL11.glDeleteTextures(lastFrameTex);
        if (lastFrameFbo != -1) GL30.glDeleteFramebuffers(lastFrameFbo);

        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        lastFrameTex = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lastFrameTex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        lastFrameFbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, lastFrameFbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, lastFrameTex, 0);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("FrameInterpolation FBO incomplete: " + status);
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);

        lastWidth = w;
        lastHeight = h;
    }

    public static void cleanup() {
        if (lastFrameTex != -1) { GL11.glDeleteTextures(lastFrameTex); lastFrameTex = -1; }
        if (lastFrameFbo != -1) { GL30.glDeleteFramebuffers(lastFrameFbo); lastFrameFbo = -1; }
        shader.destroy();
        initialized = false;
    }
}
