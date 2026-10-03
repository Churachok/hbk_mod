#version 330

uniform sampler2D SceneSampler;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    float brightness = dot(scene.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 throughRedGlass = vec3(
        min(1.0, scene.r * 0.50 + brightness * 0.50 + 0.12),
        scene.g * 0.24,
        scene.b * 0.24
    );
    fragColor = vec4(throughRedGlass, scene.a);
}
