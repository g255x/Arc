#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
uniform vec2 resolution;

out vec4 fragColor;

uniform vec3 color;
uniform float mixFactor;

uniform float minAlpha;
uniform float quality;
uniform float radius;
uniform float divider;
uniform float maxSample;

float glowShader() {
    vec2 texelSize = vec2(1.0 / resolution.x * (radius * quality), 1.0 / resolution.y * (radius * quality));
    float alpha = 0.0;

    for (float x = -radius; x < radius; x++) {
        for (float y = -radius; y < radius; y++) {
            vec4 currentColor = texture(DiffuseSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));

            if (currentColor.a != 0)
                alpha += divider > 0 ? max(0.0, (maxSample - distance(vec2(x, y), vec2(0.0))) / divider) : 1.0;
        }
    }

    // 原来在循环里 alpha *= minAlpha，采样次数一多就会被指数衰减成 0，这里只在最后压一次
    return alpha * minAlpha;
}

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);
    float alpha = 0;

    if (centerCol.a != 0) {
        fragColor = vec4(color, mixFactor);
    } else {
        alpha = glowShader();
        fragColor = vec4(color, alpha);
    }
}
