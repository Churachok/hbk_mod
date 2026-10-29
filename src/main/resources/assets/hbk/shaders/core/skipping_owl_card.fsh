#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    // R carries clarity independently of the flash envelope in A. The same
    // original image is convolved on the GPU; no blurred replacement art exists.
    float spread = (1.0 - vertexColor.r) * 0.03;
    vec4 color;
    if (spread < 0.0001) {
        color = texture(Sampler0, texCoord0);
    } else {
        color = vec4(0.0);
        float total = 0.0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                vec2 offset = vec2(x, y);
                float weight = exp(-dot(offset, offset) * 0.5);
                color += texture(Sampler0, clamp(texCoord0 + offset * spread, vec2(0.0), vec2(1.0))) * weight;
                total += weight;
            }
        }
        color /= total;
    }
    fragColor = vec4(color.rgb * ColorModulator.rgb, color.a * vertexColor.a * ColorModulator.a);
}
