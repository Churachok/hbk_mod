#version 330

uniform sampler2D InSampler;
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 pixel = 1.0 / InSize;
    float center = texture(InSampler, texCoord).a;
    float edge = abs(center - texture(InSampler, texCoord - vec2(pixel.x, 0.0)).a)
        + abs(center - texture(InSampler, texCoord + vec2(pixel.x, 0.0)).a)
        + abs(center - texture(InSampler, texCoord - vec2(0.0, pixel.y)).a)
        + abs(center - texture(InSampler, texCoord + vec2(0.0, pixel.y)).a);
    fragColor = vec4(1.0, 0.04, 0.04, clamp(edge, 0.0, 1.0));
}
