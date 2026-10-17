package dev.kissoo.mod.gui.background;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class ShaderBackground {

    private static ShaderBackground INSTANCE;

    public static ShaderBackground getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShaderBackground();
        }
        return INSTANCE;
    }

    private int programId = -1;
    private int vao = -1;
    private int vbo = -1;
    private int uTime = -1;
    private int uResolution = -1;

    private boolean initialized = false;
    private long startTime;

    private ShaderBackground() {}
    public void init() {
        if (initialized) return;

        try {
            int vertexShader = compile(GL20.GL_VERTEX_SHADER, VERTEX_SRC);
            int fragmentShader = compile(GL20.GL_FRAGMENT_SHADER, FRAG_SRC);

            programId = GL20.glCreateProgram();
            GL20.glAttachShader(programId, vertexShader);
            GL20.glAttachShader(programId, fragmentShader);
            GL20.glBindAttribLocation(programId, 0, "Position");
            GL20.glLinkProgram(programId);

            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
                throw new RuntimeException("Shader link error: " +
                        GL20.glGetProgramInfoLog(programId));
            }

            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);

            uTime = GL20.glGetUniformLocation(programId, "time");
            uResolution = GL20.glGetUniformLocation(programId, "resolution");
            vao = GL30.glGenVertexArrays();
            vbo = GL30.glGenBuffers();

            GL30.glBindVertexArray(vao);
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo);

            float[] vertices = {
                    0f, 0f, 0f,
                    1f, 0f, 0f,
                    1f, 1f, 0f,
                    0f, 0f, 0f,
                    1f, 1f, 0f,
                    0f, 1f, 0f
            };

            GL30.glBufferData(GL30.GL_ARRAY_BUFFER, vertices, GL30.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 12, 0);

            GL30.glBindVertexArray(0);

            GL20.glUseProgram(programId);
            uploadOrtho();
            GL20.glUseProgram(0);

            startTime = System.currentTimeMillis();
            initialized = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void render() {
        if (!initialized) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();

        float time = (System.currentTimeMillis() - startTime) / 1000f;

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();

        GL20.glUseProgram(programId);

        if (uTime >= 0)
            GL20.glUniform1f(uTime, time);

        if (uResolution >= 0)
            GL20.glUniform2f(uResolution, width, height);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(0);

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Shader compile error: " +
                    GL20.glGetShaderInfoLog(shader));
        }

        return shader;
    }

    private void uploadOrtho() {
        Matrix4f proj = new Matrix4f().ortho(0,1,0,1,-1,1);
        Matrix4f model = new Matrix4f().identity();

        float[] p = new float[16];
        float[] m = new float[16];

        proj.get(p);
        model.get(m);

        int projLoc = GL20.glGetUniformLocation(programId, "ProjMat");
        int modelLoc = GL20.glGetUniformLocation(programId, "ModelViewMat");

        if (projLoc >= 0)
            GL20.glUniformMatrix4fv(projLoc, false, p);

        if (modelLoc >= 0)
            GL20.glUniformMatrix4fv(modelLoc, false, m);
    }

    private static final String VERTEX_SRC = """
            #version 150 core
            in vec3 Position;
            uniform mat4 ProjMat;
            uniform mat4 ModelViewMat;
            out vec2 vUv;
            void main() {
                gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                vUv = Position.xy;
            }
            """;

    private static final String FRAG_SRC = """
         #version 150 core
            uniform float time;
            uniform vec2  resolution;
            in  vec2 vUv;
            out vec4 fragColor;
            float hash21(vec2 p){vec3 p3=fract(vec3(p.xyx)*.1031);p3+=dot(p3,p3.yzx+33.33);return fract((p3.x+p3.y)*p3.z);}
            float vnoise(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(hash21(i),hash21(i+vec2(1,0)),f.x),mix(hash21(i+vec2(0,1)),hash21(i+vec2(1,1)),f.x),f.y);}
            float fbm(vec2 p){float v=0.,a=.5;mat2 rot=mat2(cos(.5),sin(.5),-sin(.5),cos(.5));for(int i=0;i<6;i++){v+=a*vnoise(p);p=rot*p*2.1+vec2(1.7,9.2);a*=.5;}return v;}
            float warpFbm(vec2 p,float t){vec2 q=vec2(fbm(p+t*.04),fbm(p+vec2(5.2,1.3)+t*.03));vec2 r=vec2(fbm(p+4.*q+vec2(1.7,9.2)+t*.02),fbm(p+4.*q+vec2(8.3,2.8)+t*.02));return fbm(p+4.*r+t*.01);}
            void main(){
                vec2 uv=vUv,p=(vUv-.5)*vec2(resolution.x/resolution.y,1.)*2.2;
                float t=time,f=warpFbm(p*1.1,t);
                vec3 c0=vec3(.02,.04,.12),c1=vec3(.05,.02,.18),c2=vec3(.1,.28,.55),c3=vec3(.55,.12,.6),c4=vec3(.8,.9,1.);
                vec3 col;
                if(f<.2)col=mix(c0,c1,f/.2);else if(f<.5)col=mix(c1,c2,(f-.2)/.3);else if(f<.7)col=mix(c2,c3,(f-.5)/.2);else col=mix(c3,c4,(f-.7)/.3);
                col+=c4*pow(max(f-.8,0.),2.)*3.;
                vec2 g1=vec2(sin(t*.22)*.4,cos(t*.17)*.3),g2=vec2(cos(t*.19)*.5,sin(t*.25)*.25);
                col+=vec3(.08,.35,.9)*(.18/(dot(p-g1,p-g1)+.12));
                col+=vec3(.6,.1,.85)*(.12/(dot(p-g2,p-g2)+.1));
                col+=sin(uv.y*resolution.y*1.8+t*5.)*.008;
                col*=clamp(1.-dot(p*.55,p*.55),0.,1.)*.45+.55;
                col*=.82;
                fragColor=vec4(clamp(col,0.,1.),1.);
            }
         """;
}