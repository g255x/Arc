#version 150

uniform sampler2D CurrentSampler;
uniform sampler2D PreviousSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 current  = texture(CurrentSampler,  texCoord);
    vec4 previous = texture(PreviousSampler, texCoord);
    fragColor = vec4((current.rgb + previous.rgb) * 0.5, 1.0);
}
