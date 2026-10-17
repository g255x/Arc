package dev.kissoo.optimizations;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

public class MotionShaderProgram {

    private int programId = -1;
    private int vao = -1;
    private int vbo = -1;

    private int uProjMat;
    private int uCurrentSampler;
    private int uPreviousSampler;

    public void init() {
        if (programId != -1) return;
        compile();
        createQuadMesh();
    }

    private void compile() {
        int vert = compileShader(GL20.GL_VERTEX_SHADER,  "assets/forever/shaders/motion.vsh");
        int frag = compileShader(GL20.GL_FRAGMENT_SHADER, "assets/forever/shaders/motion.fsh");

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vert);
        GL20.glAttachShader(programId, frag);
        GL20.glBindAttribLocation(programId, 0, "Position");
        GL20.glBindAttribLocation(programId, 1, "UV0");
        GL20.glLinkProgram(programId);

        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Failed to link motion shader: " + GL20.glGetProgramInfoLog(programId, 1024));
        }

        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        uProjMat          = GL20.glGetUniformLocation(programId, "ProjMat");
        uCurrentSampler   = GL20.glGetUniformLocation(programId, "CurrentSampler");
        uPreviousSampler  = GL20.glGetUniformLocation(programId, "PreviousSampler");
    }

    private void createQuadMesh() {
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buf = MemoryUtil.memAllocFloat(5 * 4);
        buf.put(new float[5 * 4]).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
        MemoryUtil.memFree(buf);

        int stride = 5 * Float.BYTES;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void render(int currentTex, int previousTex, int width, int height) {
        int prevProgram      = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao          = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevVbo          = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int prevActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        int prevTex1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        boolean prevBlend     = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean prevDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean prevCullFace  = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        float[] vertices = {
                0,     0,      0,   0, 0,
                width, 0,      0,   1, 0,
                width, height, 0,   1, 1,
                0,     height, 0,   0, 1,
        };
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buf = MemoryUtil.memAllocFloat(vertices.length);
        buf.put(vertices).flip();
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buf);
        MemoryUtil.memFree(buf);

        GL20.glUseProgram(programId);
        GL20.glUniformMatrix4fv(uProjMat, false, ortho(0, width, height, 0, -1, 1));

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTex);
        GL20.glUniform1i(uCurrentSampler, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTex);
        GL20.glUniform1i(uPreviousSampler, 1);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);

        GL30.glBindVertexArray(prevVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevVbo);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex1);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex0);
        GL13.glActiveTexture(prevActiveTexture);

        GL20.glUseProgram(prevProgram);

        if (prevBlend)     GL11.glEnable(GL11.GL_BLEND);     else GL11.glDisable(GL11.GL_BLEND);
        if (prevDepthTest) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (prevCullFace)  GL11.glEnable(GL11.GL_CULL_FACE);  else GL11.glDisable(GL11.GL_CULL_FACE);
    }

    public void destroy() {
        if (programId != -1) { GL20.glDeleteProgram(programId); programId = -1; }
        if (vao != -1) { GL30.glDeleteVertexArrays(vao); vao = -1; }
        if (vbo != -1) { GL15.glDeleteBuffers(vbo); vbo = -1; }
    }

    private int compileShader(int type, String resourcePath) {
        String source = readResource(resourcePath);
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Failed to compile " + resourcePath + ": " + GL20.glGetShaderInfoLog(shader, 1024));
        }
        return shader;
    }

    private String readResource(String path) {
        try (InputStream is = MinecraftClient.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader: " + path, e);
        }
    }

    private static float[] ortho(float l, float r, float b, float t, float n, float f) {
        return new float[]{
                2f / (r - l), 0, 0, 0,
                0, 2f / (t - b), 0, 0,
                0, 0, -2f / (f - n), 0,
                -(r + l) / (r - l), -(t + b) / (t - b), -(f + n) / (f - n), 1
        };
    }
}
