#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform vec2 resolution;

uniform vec3 color;
uniform vec3 color2;
uniform float mixFactor;
uniform float minAlpha;
uniform float radius;
uniform float quality;
uniform float divider;
uniform float maxSample;
uniform float size;
uniform float time;

void main() {
    vec4 base = texture(DiffuseSampler, texCoord);

    float pulse = 0.5 + 0.5 * sin(time * 2.0);
    vec3 tint = mix(color2, color, pulse);

    vec2 uv = texCoord - vec2(0.5);
    float q = clamp(quality, 0.001, 10.0);
    float r = clamp(radius, 0.001, 64.0);
    float sz = max(size, 1.0);
    float dv = max(divider, 1.0);
    float ms = max(maxSample, 1.0);

    float d = length(uv * resolution) / sz;
    float glow = exp(-d * d * r * q) * (ms / (ms + dv));

    vec3 outRgb = mix(base.rgb, tint, clamp(mixFactor, 0.0, 1.0) * (0.5 + 0.5 * glow));
    float outA = max(base.a, clamp(minAlpha, 0.0, 1.0));

    fragColor = vec4(outRgb, outA);
}
