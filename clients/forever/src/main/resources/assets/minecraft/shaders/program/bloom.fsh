#version 150

// Faithful port of SKIDSPACE bloom.frag.
//   u_Texture  -> DiffuseSampler
//   v_TexCoord -> texCoord
//   v_OneTexel -> texel (computed from the `resolution` uniform, because the
//                 vertex-supplied oneTexel is unreliable in this pipeline;
//                 every working shader here samples via `resolution` too)
// Overlay-texture / image / dots-decorator paths are omitted (they need an
// extra sampler + asset that this pipeline does not provide); everything else
// is the original algorithm unchanged.

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform vec2 resolution;

uniform int u_Width;
uniform bool u_FastLines;
uniform int u_ShapeMode;
uniform float u_GlowMultiplier;
uniform int u_GlowQuality;
uniform vec4 u_FillColor;
uniform vec4 u_OutlineColor;

vec2 texel = 1.0 / resolution;

float blur(vec4 center, bool outline) {
    if (u_Width == 0) return 0.0;

    int w = u_GlowQuality * u_Width;
    float blurred = 0.0;

    blurred += sign(texture(DiffuseSampler, texCoord + texel * vec2(w, 0)).a);
    blurred += sign(texture(DiffuseSampler, texCoord + texel * vec2(-w, 0)).a);
    blurred += sign(texture(DiffuseSampler, texCoord + texel * vec2(0, w)).a);
    blurred += sign(texture(DiffuseSampler, texCoord + texel * vec2(0, -w)).a);

    blurred += sign(texture(DiffuseSampler, texCoord + texel * vec2(w, w)).a);
    blurred += sign(texture(DiffuseSampler, texCoord + texel * vec2(w, -w)).a);
    blurred += sign(texture(DiffuseSampler, texCoord + texel * vec2(-w, w)).a);
    blurred += sign(texture(DiffuseSampler, texCoord + texel * vec2(-w, -w)).a);

    if (u_FastLines && u_Width > 2 && blurred == 0.0) {
        return 0.0;
    }

    for (int x = -w; x <= w; x += u_GlowQuality) {
        for (int y = -w; y <= w; y += u_GlowQuality) {
            if (x == 0 && y == 0) {
                continue;
            }
            if (sign(x) == w && sign(y) == w
                || sign(x) == w && y == 0
                || sign(y) == 0 && x == 0) {
                continue;
            }

            blurred += sign(texture(DiffuseSampler, texCoord + texel * vec2(x, y)).a);
        }
    }

    return clamp(blurred / (((u_Width * u_Width) + u_Width) * 4), 0.0, 1.0) * u_GlowMultiplier;
}

void main() {
    vec4 center = texture(DiffuseSampler, texCoord);

    if (center.a != 0.0) {
        if (u_ShapeMode == 0) discard;
        center = u_FillColor;
        if (u_Width != 0) {
            center = mix(center, u_OutlineColor, u_GlowMultiplier - blur(center, false));
        }
    } else {
        if (u_ShapeMode == 1 || u_Width == 0) discard;

        float blurFactor = blur(center, true);

        if (blurFactor == 0.0) discard;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0)
                    continue;

                if (texture(DiffuseSampler, texCoord + texel * vec2(x, y)).a > 0.0) {
                    center = u_OutlineColor;
                    center.a = 1.0;
                }
            }
        }

        if (center.a == 0.0) {
            center = u_OutlineColor;
            center.a = blurFactor;
        }
    }

    fragColor = center;
}
