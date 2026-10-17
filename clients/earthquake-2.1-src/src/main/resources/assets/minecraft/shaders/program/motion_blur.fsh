#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;
uniform float Intensity;
uniform float Samples;

void main() {
    float blurStrength = clamp(Intensity, 0.0, 1.0);
    
    if (blurStrength < 0.001) {
        gl_FragColor = texture2D(DiffuseSampler, texCoord);
        return;
    }
    
    vec4 CurrTexel = texture2D(DiffuseSampler, texCoord);
    vec4 PrevTexel = texture2D(PrevSampler, texCoord);
    
    vec3 blendedColor = mix(CurrTexel.rgb, PrevTexel.rgb, blurStrength);
    
    gl_FragColor = vec4(blendedColor, 1.0);
}
