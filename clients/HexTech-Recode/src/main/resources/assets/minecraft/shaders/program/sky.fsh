#version 150

uniform float time;
uniform vec2 resolution;
uniform sampler2D DiffuseSampler;
in vec2 texCoord;
uniform vec4 color;

uniform float quality;
uniform float radius;
uniform float divider;
uniform float maxSample;
uniform float mixFactor;
uniform float minAlpha;
uniform int skyMode;

out vec4 fragColor;

float glowShader() {
    vec2 texelSize = vec2(1.0 / resolution.x * (radius * quality), 1.0 / resolution.y * (radius * quality));
    float alpha = 0.0;
    for (float x = -radius; x < radius; x++) {
        for (float y = -radius; y < radius; y++) {
            vec4 currentColor = texture(DiffuseSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));
            if (currentColor.a != 0.0)
                alpha += divider > 0.0 ? max(0.0, (maxSample - distance(vec2(x, y), vec2(0.0))) / divider) : 1.0;
            alpha *= minAlpha;
        }
    }
    return alpha;
}

void main(){
    vec4 centerCol = texture(DiffuseSampler, texCoord);
    vec2 uv = (gl_FragCoord.xy * 2.0 - resolution.xy) / min(resolution.x, resolution.y);
    vec3 finalColor = vec3(0.0);
    
    if (skyMode == 0) {
        // Stars
        float t = time * 0.5;
        uv *= mat2(cos(t), -sin(t), sin(t), cos(t));
        float c = 0.0;
        for(float i = 1.0; i < 4.0; i++) {
            vec2 q = fract(uv * 3.0 * i - vec2(time * 2.0 * i, time * 1.0 * i)) - 0.5;
            float d = length(q);
            c += (0.01 / d) * (1.0 / i);
        }
        finalColor = vec3(c);
    } else {
        // Galaxy
        vec2 p = uv * 2.0;
        float f = 0.0;
        p += time * 1.0;
        f += 0.5000 * (sin(p.x) * cos(p.y)); p *= 2.01;
        f += 0.2500 * (sin(p.x) * cos(p.y)); p *= 2.02;
        f += 0.1250 * (sin(p.x) * cos(p.y)); p *= 2.03;
        f += 0.0625 * (sin(p.x) * cos(p.y));
        finalColor = vec3(f + 0.5);
    }
    
    finalColor *= color.rgb;
    
    float alpha = 0.0;
    if (centerCol.a != 0.0) {
        alpha = mixFactor;
    } else {
        alpha = glowShader();
    }
    
    fragColor = vec4(finalColor, alpha);
}
