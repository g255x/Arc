#version 120

uniform sampler2D DiffuseSampler;
uniform float Radius;
uniform vec2 OutSize;

varying vec2 texCoord;

void main() {
    vec4 centerColor = texture2D(DiffuseSampler, texCoord);
    
    if (Radius <= 0.5) {
        gl_FragColor = centerColor;
        return;
    }
    
    vec2 texelSize = 1.0 / OutSize;
    vec4 color = vec4(0.0);
    float total = 0.0;
    float blurRadius = Radius * 0.05;
    
    for (float x = -3.0; x <= 3.0; x += 1.0) {
        for (float y = -3.0; y <= 3.0; y += 1.0) {
            vec2 offset = vec2(x, y) * texelSize * blurRadius;
            float dist = length(vec2(x, y));
            float weight = exp(-dist * dist / 6.0);
            vec2 sampleCoord = clamp(texCoord + offset, vec2(0.0), vec2(1.0));
            color += texture2D(DiffuseSampler, sampleCoord) * weight;
            total += weight;
        }
    }
    
    gl_FragColor = color / total;
}
