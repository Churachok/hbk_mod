#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 faceUv;
flat in vec4 spriteBounds;
flat in float blockSeed;
in float cameraDistance;

out vec4 fragColor;

float noise(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

vec3 originalTexture(vec2 uv) {
    // Keep displaced fragments inside their own sprite, including with resource packs.
    vec2 margin = 0.65 / vec2(textureSize(Sampler0, 0));
    vec2 lo = spriteBounds.xy + margin;
    vec2 hi = max(lo, spriteBounds.zw - margin);
    return texture(Sampler0, mix(lo, hi, fract(uv))).rgb;
}

void main() {
    float fade = 1.0 - smoothstep(24.0, 32.0, cameraDistance);
    if (fade <= 0.0) discard;

    float seed = mod(blockSeed, 4093.0);
    float time = ColorModulator.a;
    // Each block has its own clock: corruption comes in short asynchronous bursts.
    float clock = time * 9.0 + seed * 0.37;
    float frame = floor(clock);
    float burst = step(0.58, noise(vec2(seed, floor(clock / 5.0))));
    float row = floor(faceUv.y * 16.0);
    float torn = step(0.72, noise(vec2(row + seed, frame))) * (0.25 + 0.75 * burst);
    float shift = (noise(vec2(frame, row + seed)) - 0.5) * 0.7 * torn;
    vec2 brokenUv = faceUv + vec2(shift, 0.0);

    // Red/cyan separation and displaced slices still show the block's actual texture.
    vec3 image = originalTexture(brokenUv);
    image.r = originalTexture(brokenUv + vec2(0.045 * torn, 0.0)).r;
    image.b = originalTexture(brokenUv - vec2(0.045 * torn, 0.0)).b;

    float staticPixel = noise(floor(faceUv * vec2(32.0, 24.0)) + vec2(seed, frame));
    float digital = step(0.84, staticPixel) * burst;
    float dropout = step(0.93, noise(vec2(row, frame + seed))) * burst;
    float scan = 1.0 - step(0.12, fract(faceUv.y * 24.0 - time * 2.0 + seed));
    float hue = noise(vec2(seed, floor(frame / 3.0)));
    vec3 interference = hue < 0.33 ? vec3(0.15, 0.95, 1.0)
        : hue < 0.66 ? vec3(0.95, 0.18, 0.85) : vec3(0.6, 0.3, 1.0);
    vec3 color = mix(image, interference, digital * 0.8 + dropout * 0.55);
    color += interference * scan * (0.15 + 0.3 * burst);
    color *= 1.0 - dropout * 0.4;

    float alpha = (0.035 + scan * 0.18 + torn * 0.65 + digital * 0.35 + dropout * 0.45) * fade;
    fragColor = vec4(clamp(color, 0.0, 1.0), min(alpha, 0.88));
}
