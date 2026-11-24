#version 150

// Arc 通用整屏四边形顶点着色器。
// 顶点坐标直接就是裁剪空间的 (-1,-1) ~ (1,1)，不需要任何矩阵；
// 同时输出两套纹理坐标名（texCoord / v_TexCoord）以兼容两种来源的片元着色器：
//   - 移植自 Hachimi 的 menu.fsh 用 texCoord
//   - 移植自 Wallpaper Engine 的 loading.fsh 用 v_TexCoord

in vec2 Position;

out vec2 texCoord;
out vec2 v_TexCoord;
out vec2 oneTexel;

void main() {
    vec2 uv = Position * 0.5 + 0.5;
    texCoord = uv;
    v_TexCoord = uv;
    oneTexel = vec2(1.0 / 1920.0, 1.0 / 1080.0);
    gl_Position = vec4(Position, 0.0, 1.0);
}
