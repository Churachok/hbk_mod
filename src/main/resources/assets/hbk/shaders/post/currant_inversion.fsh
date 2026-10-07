#version 330

uniform sampler2D SceneSampler;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = texture(SceneSampler, vec2(1.0 - texCoord.x, 1.0 - texCoord.y));
}
