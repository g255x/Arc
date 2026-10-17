#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform vec2 resolution;
uniform float time;
uniform float alpha;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float hash3(vec3 p) {
    return fract(sin(dot(p, vec3(12.9898, 78.233, 45.5432))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float f = 0.0;
    f += 0.5000 * noise(p); p = p * 2.02 + vec2(0.1);
    f += 0.2500 * noise(p); p = p * 2.03 + vec2(0.2);
    f += 0.1250 * noise(p); p = p * 2.01;
    f += 0.0625 * noise(p);
    return f / 0.9375;
}

float noise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = hash3(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash3(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash3(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(i + vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);

    float nxy0 = mix(nx00, nx10, f.y);
    float nxy1 = mix(nx01, nx11, f.y);

    return mix(nxy0, nxy1, f.z);
}

float fbm3D(vec3 p) {
    float f = 0.0;
    f += 0.5000 * noise3D(p); p = p * 2.02;
    f += 0.2500 * noise3D(p); p = p * 2.03;
    f += 0.1250 * noise3D(p); p = p * 2.01;
    f += 0.0625 * noise3D(p);
    return f / 0.9375;
}

vec3 auroraLayer(vec2 uv, float t, float layer) {
    float speed = 0.03 + layer * 0.015;
    float baseY = -0.15 + layer * 0.12;
    float scale = 1.5 + layer * 0.8;

    float y = uv.y - baseY;
    y += sin(uv.x * scale + t * speed) * 0.06;
    y += cos(uv.x * 1.7 + t * (speed + 0.02)) * 0.04;
    y += fbm(vec2(uv.x * 1.8 + t * 0.04, t * 0.08 + layer)) * 0.10;

    float wave = smoothstep(-0.12, 0.0, y) * smoothstep(0.20, 0.04, y);
    wave *= 0.4 + 0.6 * sin(uv.x * 6.0 + t * 0.4 + layer * 2.5);
    wave *= 0.6 + 0.4 * cos(uv.x * 3.5 + t * 0.25 + layer * 1.8);

    float hueShift = sin(uv.x * 2.5 + t * 0.15 + layer * 2.0) * 0.5 + 0.5;
    vec3 colA = vec3(0.15, 0.85, 0.5);
    vec3 colB = vec3(0.20, 0.50, 0.95);
    vec3 colC = vec3(0.75, 0.20, 0.65);
    vec3 colD = vec3(0.10, 0.70, 0.80);

    vec3 color1 = mix(colA, colB, hueShift);
    vec3 color2 = mix(colC, colD, layer * 0.3 + sin(t * 0.3 + layer) * 0.5 + 0.5);
    vec3 auroraCol = mix(color1, color2, layer * 0.35);

    float brightness = 0.85 - layer * 0.2;
    return wave * auroraCol * brightness;
}

vec3 stars(vec2 uv, float t) {
    float starCellSize = 6.0;

    vec2 pixel = uv * resolution.xy;
    vec2 cell = floor(pixel / starCellSize);
    vec2 local = fract(pixel / starCellSize);

    float seed = hash(cell);
    float starExists = step(0.996, seed);

    vec2 starPos = vec2(
        hash(cell + vec2(17.0, 43.0)),
        hash(cell + vec2(91.0, 7.0))
    );

    float dist = length(local - starPos);

    float bigStar = step(0.9997, seed);
    float radius = mix(0.28, 0.55, bigStar);

    float starShape = 1.0 - smoothstep(0.0, radius, dist);
    starShape *= starShape;

    // 随机出现和消失
    float phase = hash(cell + vec2(3.0, 11.0));
    float blinkSpeed = 0.6 + hash(cell + vec2(7.0, 13.0)) * 1.2;
    float blink = sin(t * blinkSpeed + phase * 6.2831853) * 0.5 + 0.5;
    float alive = smoothstep(0.25, 0.6, blink);

    // 轻微闪烁
    float flicker = 0.85 + 0.15 * sin(t * 2.0 + phase * 20.0);

    float brightness = starExists * starShape * alive * flicker * mix(0.5, 1.0, bigStar);
    return vec3(1.0, 0.95, 0.8) * brightness;
}

float vignette(vec2 uv) {
    uv = uv * 2.0 - 1.0;
    float d = length(uv * vec2(resolution.x / resolution.y, 1.0)) * 0.65;
    return clamp(1.0 - d * d * d * 1.2, 0.0, 1.0);
}

void main() {
    vec2 uv = texCoord;
    vec2 centeredUV = uv - 0.5;
    centeredUV.x *= resolution.x / resolution.y;

    // 背景/极光速度加快：原来 time * 0.6
    float bgT = time * 1.2;

    vec3 bg = mix(
        vec3(0.0, 0.0, 0.04),
        vec3(0.03, 0.01, 0.08),
        uv.y
    );

    float horizonGradient = smoothstep(0.35, 0.65, uv.y);
    bg = mix(
        mix(vec3(0.01, 0.01, 0.03), vec3(0.0, 0.01, 0.05), horizonGradient),
        bg,
        0.5
    );

    // 星星使用原始 time，生灭速度独立
    bg += stars(uv, time);

    vec3 aurora = vec3(0.0);
    for (float i = 0.0; i < 4.0; i++) {
        aurora += auroraLayer(centeredUV, bgT, i);
    }

    aurora *= 1.0 + fbm3D(vec3(centeredUV * 3.0, bgT * 0.1)) * 0.15;
    bg += aurora;

    bg *= vignette(uv);

    fragColor = vec4(bg * alpha, 1.0);
}