#version 150

uniform sampler2D g_Texture0; // {"material":"framebuffer","label":"ui_editor_properties_framebuffer","hidden":true}

in vec2 v_TexCoord;
out vec4 fragColor;

uniform float g_Time;
uniform vec3 g_Screen;
uniform float uAlpha; // Arc: 整屏淡入淡出（0 全透明 ~ 1 不透明）

uniform float uSpeed; // {"material":"Rotation Speed","default":1,"range":[0,2]}
uniform vec3 uTopColor; // {"default":"0 0 1","material":"Top Color","type":"color"}
uniform vec3 uBottomColor; // {"default":"1 0 0.7","material":"Bottom Color","type":"color"}
uniform float uIntensity; // {"material":"Intensity","default":1.3,"range":[0.1,3]}
uniform float uGlowAmount; // {"material":"Glow Amount","default":0.002,"range":[0.001,0.02]}
uniform float uPillarWidth; // {"material":"Pillar Width","default":2,"range":[1,10]}
uniform float uPillarHeight; // {"material":"Pillar Height","default":0.5,"range":[0.1,2]}
uniform float uNoiseIntensity; // {"material":"Noise Intensity","default":0.1,"range":[0 , 2]}
uniform float uPillarRotation;  // {"material":"Pillar Rotation","default":333,"range":[0 , 360]}
uniform float uOffsetX; // {"material":"Offset X","default":0.0,"range":[-2,2]}
uniform float uOffsetY; // {"material":"Offset Y","default":0.0,"range":[-2,2]}

const float PI = 3.141592653589793;
const float EPSILON = 0.001;
const float E = 2.71828182845904523536;
const float HALF = 0.5;

vec2 rotate(vec2 v, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec2(v.x * c - v.y * s, v.x * s + v.y * c);
}

float noise(vec2 coord) {
    float G = E;
    vec2 r = (G * sin(G * coord));
    return fract(r.x * r.y * (1.0 + coord.x));
}

vec3 applyWaveDeformation(vec3 pos, float timeOffset) {
    float frequency = 1.0;
    float amplitude = 1.0;
    vec3 deformed = pos;

    for(float i = 0.0; i < 4.0; i++) {
        deformed.xz = rotate(deformed.xz, 0.4);
        float phase = timeOffset * i * 2.0;
        vec3 oscillation = cos(deformed.zxy * frequency - phase);
        deformed += oscillation * amplitude;
        frequency *= 2.0;
        amplitude *= HALF;
    }
    return deformed;
}

float blendMin(float a, float b, float k) {
    float scaledK = k * 4.0;
    float h = max(scaledK - abs(a - b), 0.0);
    return min(a, b) - h * h * 0.25 / scaledK;
}

float blendMax(float a, float b, float k) {
    return -blendMin(-a, -b, k);
}

vec3 blendScreen(vec3 base, vec3 blend) {
    return 1.0 - (1.0 - base) * (1.0 - blend);
}

void main() {
    vec2 fragCoord = v_TexCoord * g_Screen.xy;
    vec2 uv = (fragCoord * 2.0 - g_Screen.xy) / g_Screen.y;
    uv.x -= uOffsetX;
    uv.y -= uOffsetY;

    float rotAngle = uPillarRotation * PI / 180.0;
    uv = rotate(uv, rotAngle);

    vec3 origin = vec3(0.0, 0.0, -10.0);
    vec3 direction = normalize(vec3(uv, 1.0));

    float maxDepth = 50.0;
    float depth = 0.1;

    float rotXAngle = g_Time * uSpeed;

    vec3 color = vec3(0.0, 0.0, 0.0);

    for(float i = 0.0; i < 100.0; i++) {
        vec3 pos = origin + direction * depth;
        pos.xz = rotate(pos.xz, rotXAngle);

        vec3 deformed = pos;
        deformed.y *= uPillarHeight;
        deformed = applyWaveDeformation(deformed + vec3(0.0, g_Time * uSpeed, 0.0), g_Time * uSpeed);

        vec2 cosinePair = cos(deformed.xz);
        float fieldDistance = length(cosinePair) - 0.2;

        float radialBound = length(pos.xz) - uPillarWidth;
        fieldDistance = blendMax(radialBound, fieldDistance, 1.0);
        fieldDistance = abs(fieldDistance) * 0.15 + 0.01;

        // smoothstep 要求 edge0 < edge1（原来写 15 -> -15 是未定义行为），取反等价
        vec3 gradient = mix(uBottomColor, uTopColor, 1.0 - smoothstep(-15.0, 15.0, pos.y));
        color += gradient * (1.0 / fieldDistance);

        if(fieldDistance < EPSILON || depth > maxDepth)
            break;
        depth += fieldDistance;
    }

    float widthNormalization = uPillarWidth / 3.0;
    color = tanh(color * uGlowAmount / widthNormalization);

    // 片元着色器不能用 gl_Position，改用 gl_FragCoord
    float rnd = noise(gl_FragCoord.xy);
    color -= rnd / 15.0 * uNoiseIntensity;

    // 标准 GLSL 用 texture 代替 Wallpaper Engine 的 texSample2D
    vec4 albedo = texture(g_Texture0, v_TexCoord.xy);

    vec3 pillarColor = clamp(color * uIntensity, 0.0, 1.0);

    fragColor = vec4(blendScreen(albedo.rgb, pillarColor), uAlpha);
}