#version 150

#ifdef GL_ES
precision mediump float;
#endif
#define NUM_OCTAVES 16

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
uniform vec2 resolution;

out vec4 fragColor;

uniform vec3 color;
uniform vec3 color2;
uniform float mixFactor;
uniform float minAlpha;
uniform float quality;
uniform float radius;
uniform float divider;
uniform float maxSample;
uniform float time;
uniform float size;

float glowShader() {
    float pulseSize = radius + sin(time * 0.01) * size * 0.5;
    vec2 texelSize = vec2(1.0 / resolution.x * (pulseSize * quality), 1.0 / resolution.y * (pulseSize * quality));
    float alpha = 0;

    for (float x = -pulseSize; x < pulseSize; x++) {
        for (float y = -pulseSize; y < pulseSize; y++) {
            vec4 currentColor = texture(DiffuseSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));

            if (currentColor.a != 0)
            alpha += divider > 0 ? max(0.0, (maxSample - distance(vec2(x, y), vec2(0))) / divider) : 1;
            alpha *= minAlpha;
        }
    }

    return alpha;
}

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);
    float alpha = 0;
    float t = sin(time * 0.05) * 0.5 + 0.5;
    vec3 pulseColor = mix(color, color2, t);

    if (centerCol.a != 0) {
        fragColor = vec4(pulseColor, mixFactor);
    } else {
        alpha = glowShader();
        fragColor = vec4(pulseColor, alpha);
    }
}