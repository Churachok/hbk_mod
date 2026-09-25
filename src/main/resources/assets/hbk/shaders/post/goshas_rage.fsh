#version 330

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

float sampleLuma(vec2 offset, vec2 pixel) {
    return luminance(texture(SceneSampler, texCoord + offset * pixel).rgb);
}

float sampleDepth(vec2 offset, vec2 pixel) {
    return texture(DepthSampler, texCoord + offset * pixel).r;
}

void main() {
    vec2 pixel = 1.0 / InSize;

    float l00 = sampleLuma(vec2(-1.0, -1.0), pixel);
    float l10 = sampleLuma(vec2( 0.0, -1.0), pixel);
    float l20 = sampleLuma(vec2( 1.0, -1.0), pixel);
    float l01 = sampleLuma(vec2(-1.0,  0.0), pixel);
    float l21 = sampleLuma(vec2( 1.0,  0.0), pixel);
    float l02 = sampleLuma(vec2(-1.0,  1.0), pixel);
    float l12 = sampleLuma(vec2( 0.0,  1.0), pixel);
    float l22 = sampleLuma(vec2( 1.0,  1.0), pixel);
    float colorX = -l00 - 2.0 * l01 - l02 + l20 + 2.0 * l21 + l22;
    float colorY = -l00 - 2.0 * l10 - l20 + l02 + 2.0 * l12 + l22;
    float colorEdge = smoothstep(0.18, 0.55, length(vec2(colorX, colorY)));

    float d00 = sampleDepth(vec2(-1.0, -1.0), pixel);
    float d10 = sampleDepth(vec2( 0.0, -1.0), pixel);
    float d20 = sampleDepth(vec2( 1.0, -1.0), pixel);
    float d01 = sampleDepth(vec2(-1.0,  0.0), pixel);
    float d21 = sampleDepth(vec2( 1.0,  0.0), pixel);
    float d02 = sampleDepth(vec2(-1.0,  1.0), pixel);
    float d12 = sampleDepth(vec2( 0.0,  1.0), pixel);
    float d22 = sampleDepth(vec2( 1.0,  1.0), pixel);
    float depthX = -d00 - 2.0 * d01 - d02 + d20 + 2.0 * d21 + d22;
    float depthY = -d00 - 2.0 * d10 - d20 + d02 + 2.0 * d12 + d22;
    float depthEdge = smoothstep(0.00015, 0.0025, length(vec2(depthX, depthY)));

    float edge = max(colorEdge, depthEdge);
    vec3 result = vec3(edge);

    fragColor = vec4(result, 1.0);
}
