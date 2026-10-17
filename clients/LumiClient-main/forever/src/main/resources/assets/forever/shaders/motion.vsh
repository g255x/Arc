#version 150

in vec3 Position;
in vec2 UV0;

out vec2 texCoord;

uniform mat4 ProjMat;

void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);
    texCoord = vec2(UV0.x, 1.0 - UV0.y);
}
