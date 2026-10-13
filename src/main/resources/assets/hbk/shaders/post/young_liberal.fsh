#version 330

uniform sampler2D SceneSampler;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    float luminance = dot(scene.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 saturated = mix(vec3(luminance), scene.rgb, 1.20);
    vec3 contrasted = (saturated - 0.5) * 1.35 + 0.5;
    fragColor = vec4(clamp(contrasted, 0.0, 1.0), scene.a);
}
