#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
in vec2 faceUv;
flat in float blockSeed;
flat in float phase;
flat in float echo;
flat in float opacity;
in float cameraDistance;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    float fade = 1.0 - smoothstep(20.0, 24.0, cameraDistance);
    if (fade <= 0.0) discard;
    float time = ColorModulator.a;
    float frame = floor(time * 11.0 + blockSeed);
    vec2 uv = faceUv;
    float torn = 0.0;
    if (phase > 1.5) {
        float row = floor(uv.y * 16.0);
        torn = step(0.67, hash(vec2(row + blockSeed, frame)));
        uv.x += (hash(vec2(row, frame)) - 0.5) * torn * 0.35;
    }
    vec3 stone = texture(Sampler0, clamp(uv, 0.001, 0.999)).rgb;
    float rift = smoothstep(0.19, 0.42, stone.b - stone.r);
    vec3 cyan = vec3(0.19, 0.82, 1.0);
    float pulse = 0.72 + 0.28 * sin(time * 2.1 + blockSeed);
    float alpha;
    vec3 color;
    if (echo > 0.0) {
        color = mix(stone, cyan, 0.3 + torn * 0.4);
        alpha = opacity * (0.65 + rift * 0.35) * pulse;
    } else if (phase > 1.5) {
        color = mix(stone, cyan, torn * 0.75);
        alpha = opacity * (torn * 0.75 + rift * 0.3);
    } else {
        color = cyan;
        alpha = rift * opacity * pulse * (phase > 0.5 ? 0.12 : 0.3);
    }
    fragColor = vec4(color, clamp(alpha * fade, 0.0, 0.85));
}
