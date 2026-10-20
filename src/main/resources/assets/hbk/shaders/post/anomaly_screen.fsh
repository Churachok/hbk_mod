#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D SceneSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform AnomalyScreenConfig {
    vec4 Effects;
};

in vec2 texCoord;
out vec4 fragColor;

float hash(float value) {
    return fract(sin(value * 127.1 + 311.7) * 43758.5453);
}

float scratch(vec2 p, vec2 start, vec2 end, float width, float pixel) {
    vec2 segment = end - start;
    float along = clamp(dot(p - start, segment) / dot(segment, segment), 0.0, 1.0);
    float distance = length(p - start - along * segment);
    float taper = 0.3 + 0.7 * sin(along * 3.14159265);
    return 1.0 - smoothstep(width * taper, width * taper + pixel, distance);
}

vec2 inside(vec2 uv) {
    return clamp(uv, vec2(0.001), vec2(0.999));
}

void main() {
    float time = GameTime * 1200.0;
    vec2 uv = texCoord;
    // A negative barrel lens accompanies the native wider FOV; the center and crosshair stay fixed.
    vec2 lensCenter = uv - 0.5;
    uv = 0.5 + lensCenter * (1.0 - 0.7 * dot(lensCenter, lensCenter) * clamp(Effects.w, 0.0, 1.0));
    vec3 color;
    float distortion = clamp(Effects.y, 0.0, 1.0);
    float scratches = clamp(Effects.x, 0.0, 1.0);
    if (distortion > 0.0) {
        vec2 centered = uv - 0.5;
        // A strongly bent lens, broken horizontal bands and separated color channels.
        uv = 0.5 + centered * (1.0 - 1.15 * dot(centered, centered) * distortion);
        float row = floor(uv.y * 22.0);
        float burst = step(0.65, hash(row + floor(time * 7.0) * 53.0));
        uv.x += sin(uv.y * 29.0 + time * 3.7) * 0.025 * distortion;
        uv.x += burst * sin(row * 4.7 + time * 5.0) * 0.105 * distortion;
        uv.y += sin(uv.x * 14.0 - time * 2.3) * 0.018 * distortion;
        vec2 split = vec2(0.009 + 0.005 * sin(time * 4.1), 0.002) * distortion;
        color.r = texture(SceneSampler, inside(uv + split)).r;
        color.g = texture(SceneSampler, inside(uv)).g;
        color.b = texture(SceneSampler, inside(uv - split)).b;
        float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        color = mix(vec3(luma), color, mix(1.0, 1.55, distortion));
        color = clamp((color - 0.5) * mix(1.0, 4.8, distortion) + 0.5, 0.0, 1.0);
    } else {
        color = texture(SceneSampler, uv).rgb;
    }

    if (scratches > 0.0) {
        float pixel = 1.0 / OutSize.y;
        vec2 aspect = vec2(OutSize.x / OutSize.y, 1.0);
        vec2 p = texCoord * aspect;
        float dark = 0.0;
        float gleam = 0.0;
        // Long, tapered gouges with hairline forks; fixed in screen space like scratched glass.
        for (int i = 0; i < 14; i++) {
            float seed = float(i) + 7.0;
            vec2 start = vec2(0.12 + 0.76 * hash(seed), 0.12 + 0.76 * hash(seed + 23.0)) * aspect;
            float angle = -1.15 + hash(seed + 71.0) * 0.65;
            vec2 direction = vec2(cos(angle), sin(angle));
            vec2 end = start + direction * (0.13 + 0.30 * hash(seed + 49.0));
            float width = pixel * (0.55 + hash(seed + 13.0));
            dark = max(dark, scratch(p, start, end, width, pixel));
            gleam = max(gleam, scratch(p + vec2(pixel * 1.8, -pixel), start, end, width, pixel));
            vec2 branch = mix(start, end, 0.45);
            vec2 tip = branch + vec2(direction.y, -direction.x) * 0.035 + direction * 0.055;
            dark = max(dark, scratch(p, branch, tip, pixel * 0.4, pixel));
        }
        color = mix(color, vec3(0.83, 0.77, 0.94), gleam * 0.7 * scratches);
        color = mix(color, vec3(0.055, 0.025, 0.075), dark * 0.85 * scratches);
    }
    // Desaturate last so scratches and color-channel glitches also become monochrome.
    float grey = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(color, vec3(grey), clamp(Effects.z, 0.0, 1.0));
    fragColor = vec4(color, 1.0);
}
