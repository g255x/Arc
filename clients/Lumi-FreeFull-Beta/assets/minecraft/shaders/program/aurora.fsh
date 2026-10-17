#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform vec2 resolution;
uniform float time;
uniform float alpha;

const vec3 Color1 = vec3(0.01, 0.02, 0.12);
const vec3 Color2 = vec3(0.02, 0.03, 0.08);
const vec3 Color3 = vec3(0.01, 0.01, 0.05);
const float Intensity = 1.0;
const float Exponent1 = 1.0;
const float Exponent2 = 1.0;

const vec3 CloudColor = vec3(0.15, 0.15, 0.3);
const float CloudIntensity = 0.4;
const float CloudSpeed = 0.3;

const vec3 AuroraColor = vec3(0.1, 0.8, 0.4);
const float AuroraSpeed = 0.5;
const float SurAuroraColFactor = 0.5;

float CloudHash(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float CloudNoise(vec2 st) {
    st += vec2(0.0, time * CloudSpeed);
    vec2 i = floor(st);
    vec2 f = fract(st);
    float a = CloudHash(i);
    float b = CloudHash(i + vec2(1.0, 0.0));
    float c = CloudHash(i + vec2(0.0, 1.0));
    float d = CloudHash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float Cloudfbm(vec2 st) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 3; i++) {
        value += amplitude * CloudNoise(st);
        st *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

float AuroraHash(float n) {
    return fract(sin(n) * 758.5453);
}

float AuroraNoise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    float n = p.x + p.y * 57.0 + p.z * 800.0;
    return mix(mix(mix(AuroraHash(n + 0.0), AuroraHash(n + 1.0), f.x),
                   mix(AuroraHash(n + 57.0), AuroraHash(n + 58.0), f.x), f.y),
               mix(mix(AuroraHash(n + 800.0), AuroraHash(n + 801.0), f.x),
                   mix(AuroraHash(n + 857.0), AuroraHash(n + 858.0), f.x), f.y), f.z);
}

float Aurorafbm(vec3 p) {
    float f = 0.50000 * AuroraNoise(p);
    p *= 2.02;
    f += 0.25000 * AuroraNoise(p);
    p *= 2.03;
    f += 0.12500 * AuroraNoise(p);
    p *= 2.01;
    f += 0.06250 * AuroraNoise(p);
    return f * 1.066667;
}

float GetAurora(vec3 p) {
    p += Aurorafbm(vec3(p.x, p.y, 0.0) * 0.5) * 2.25;
    float a = smoothstep(0.0, 0.9, Aurorafbm(p * 2.0) * 2.2 - 1.1);
    return a < 0.0 ? 0.0 : a;
}

mat2 RotateMatrix(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat2(c, s, -s, c);
}

float tri(float x) {
    return clamp(abs(fract(x) - 0.5), 0.01, 0.49);
}

vec2 tri2(vec2 p) {
    return vec2(tri(p.x) + tri(p.y), tri(p.y + tri(p.x)));
}

float SurAuroraNoise(vec2 pos) {
    float intensity = 1.8;
    float size = 2.5;
    float rz = 0.0;
    pos = RotateMatrix(pos.x * 0.06) * pos;
    vec2 bp = pos;
    for (int i = 0; i < 3; i++) {
        vec2 dg = tri2(bp * 1.85) * 0.75;
        dg = RotateMatrix(time * AuroraSpeed) * dg;
        pos -= dg / size;
        bp *= 1.3;
        size *= 0.45;
        intensity *= 0.42;
        pos *= 1.21 + (rz - 1.0) * 0.02;
        rz += tri(pos.x + tri(pos.y)) * intensity;
        pos = -mat2(0.95534, 0.29552, -0.29552, 0.95534) * pos;
    }
    return clamp(1.0 / pow(rz * 29.0, 1.3), 0.0, 0.55);
}

float SurHash(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

vec4 SurAurora(vec3 pos, vec3 ro) {
    vec4 col = vec4(0.0);
    vec4 avgCol = vec4(0.0);

    for (int i = 0; i < 12; i++) {
        float of = 0.006 * SurHash(pos.xy) * smoothstep(0.0, 15.0, float(i));
        float pt = ((0.8 + pow(float(i), 1.4) * 0.002) - ro.y) / (pos.y * 2.0 + 0.8);
        pt -= of;
        vec3 bpos = ro + pt * pos;
        vec2 p = bpos.zx;

        float noise = SurAuroraNoise(p);
        vec4 col2 = vec4(0.0, 0.0, 0.0, noise);
        col2.rgb = (sin(1.0 - vec3(2.15, -0.5, 1.2) + float(i) * SurAuroraColFactor * 0.1) * 0.8 + 0.5) * noise;
        avgCol = mix(avgCol, col2, 0.5);
        col += avgCol * exp2(-float(i) * 0.065 - 2.5) * smoothstep(0.0, 5.0, float(i));
    }

    col *= clamp(pos.y * 15.0 + 0.4, 0.0, 1.0);
    return col * 1.8;
}

void main() {
    vec2 uv = vec2(texCoord.x, 1.0 - texCoord.y);
    vec2 p = uv * 2.0 - 1.0;
    p.x *= resolution.x / resolution.y;

    vec3 rawDir = vec3(p.x, p.y, 1.0);
    vec3 dir = normalize(rawDir);
    float py = dir.y;
    int reflection = py < 0.0 ? -1 : 1;

    float p1 = 1.0 - pow(min(1.0, 1.0 - py), Exponent1);
    float p3 = 1.0 - pow(min(1.0, 1.0 + py), Exponent2);
    float p2 = 1.0 - p1 - p3;

    float cloud = Cloudfbm(vec2(rawDir.x, rawDir.z) * 8.0);
    vec4 cloudCol = vec4(cloud * CloudColor, cloud * 0.8) * CloudIntensity;

    vec4 surAuroraCol = smoothstep(0.0, 1.5, SurAurora(
        vec3(rawDir.x, abs(rawDir.y), rawDir.z),
        vec3(0.0, 0.0, -6.7)
    )) + float(reflection - 1) * -0.1;

    vec3 aurora = AuroraColor.rgb * GetAurora(vec3(rawDir.xy * vec2(1.1, 0.85), time * AuroraSpeed * 0.24)) * 1.0 +
                  AuroraColor.rgb * GetAurora(vec3(rawDir.xy * vec2(0.85, 0.55), time * AuroraSpeed * 0.25)) * 0.7;
    vec4 auroraCol = vec4(aurora, 0.1354);

    vec4 skyCol = (vec4(Color1, 1.0) * p1 + vec4(Color2, 1.0) * p2 + vec4(Color3, 1.0) * p3) * Intensity;
    skyCol = skyCol * (1.0 - cloudCol.a) + cloudCol * cloudCol.a;
    skyCol = skyCol * (1.0 - surAuroraCol.a) + surAuroraCol * surAuroraCol.a;
    skyCol = skyCol * (1.0 - auroraCol.a) + auroraCol * auroraCol.a;

    if (reflection == -1) {
        float c = dot(vec3(0.0, 1.0, 0.0), dir);
        vec3 refPos = dir * (1.23 / c);
        float re = Cloudfbm(refPos.xz * 0.5) * 0.5 + 0.5;
        skyCol.rgb *= (mix(0.25, 0.85, re) - 0.05);
        float gray = dot(skyCol.rgb, vec3(0.299, 0.587, 0.114));
        skyCol.rgb = mix(skyCol.rgb, vec3(gray), 0.35);
        skyCol.rgb += re * 0.06;
        skyCol += vec4(Color2, 1.0) * p2 * 0.15;
    }

    fragColor = vec4(skyCol.rgb * alpha, 1.0);
}