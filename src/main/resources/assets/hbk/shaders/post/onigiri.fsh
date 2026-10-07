#version 330

uniform sampler2D SceneSampler;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    fragColor = vec4(min(scene.rgb * 1.12, vec3(1.0)), scene.a);
}
