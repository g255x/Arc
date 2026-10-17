#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;
uniform vec2 OutSize;

uniform vec2 BlurDir;
uniform vec2 BlurXY;
uniform vec2 BlurCoord;
uniform float Radius;
uniform vec2 DiagonalLine;

float SCurve (float x) {
    x = x * 2.0 - 1.0;
    return -x * abs(x) * 0.5 + x + 0.5;
}

bool isInBlurRegion(vec2 uv) {
    float pixelX = uv.x / oneTexel.x;
    float pixelY = uv.y / oneTexel.y;
    bool inRect = pixelX >= BlurXY.x * 2.0
               && pixelY >= BlurXY.y * 2.0
               && pixelX <= (BlurCoord.x + BlurXY.x) * 2.0
               && pixelY <= (BlurCoord.y + BlurXY.y) * 2.0;

    if (!inRect) return false;
    float screenHeight = 1.0 / oneTexel.y;
    float t = 1.0 - (pixelY / screenHeight);
    float diagonalX = mix(DiagonalLine.x, DiagonalLine.y, t) * 2.0;

    return pixelX <= diagonalX;
}

vec4 BlurH (sampler2D source, vec2 size, vec2 uv, float radius) {

    if (isInBlurRegion(uv))
    {
        vec4 A = vec4(0.0);
        vec4 C = vec4(0.0);

        float divisor = 0.0;
        float weight = 0.0;

        float radiusMultiplier = 1.0 / radius;

        for (float x = -radius; x <= radius; x++)
        {
            A = texture2D(source, uv + vec2(x * size) * BlurDir);

            weight = SCurve(1.0 - (abs(x) * radiusMultiplier));

            C += A * weight;

            divisor += weight;
        }

        return vec4(C.r / divisor, C.g / divisor, C.b / divisor, 1.0);
    }

    return texture2D(source, uv);
}

void main() {
    vec4 blurred = BlurH(DiffuseSampler, oneTexel, texCoord, Radius);
    gl_FragColor = blurred;
}
