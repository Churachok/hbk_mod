#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 oneTexel = 1.0 / InSize;

    vec4 center = texture(InSampler, texCoord);
    vec4 left = texture(InSampler, texCoord - vec2(oneTexel.x, 0.0));
    vec4 right = texture(InSampler, texCoord + vec2(oneTexel.x, 0.0));
    vec4 up = texture(InSampler, texCoord - vec2(0.0, oneTexel.y));
    vec4 down = texture(InSampler, texCoord + vec2(0.0, oneTexel.y));

    float edge = clamp(
        abs(center.a - left.a)
        + abs(center.a - right.a)
        + abs(center.a - up.a)
        + abs(center.a - down.a),
        0.0,
        1.0
    );
    if (edge <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    // Read the entity's encoded vertical bounds from whichever mask samples
    // touch this edge. This keeps the flag aligned to each individual mob.
    vec4 encodedMask = center * center.a
        + left * left.a
        + right * right.a
        + up * up.a
        + down * down.a;
    float maskWeight = center.a + left.a + right.a + up.a + down.a;
    encodedMask /= max(maskWeight, 0.001);

    float bottom = encodedMask.r;
    float top = encodedMask.g;
    float localY = clamp((texCoord.y - bottom) / max(top - bottom, 1.0 / 255.0), 0.0, 1.0);

    const vec3 BLUE = vec3(0.0, 181.0 / 255.0, 226.0 / 255.0);
    const vec3 RED = vec3(239.0 / 255.0, 51.0 / 255.0, 64.0 / 255.0);
    const vec3 GREEN = vec3(80.0 / 255.0, 158.0 / 255.0, 47.0 / 255.0);

    vec3 flagColor = localY >= 2.0 / 3.0 ? BLUE : localY >= 1.0 / 3.0 ? RED : GREEN;
    fragColor = vec4(flagColor, edge);
}
