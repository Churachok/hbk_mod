#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 faceUv;
flat out float blockSeed;
flat out float phase;
flat out float echo;
flat out float opacity;
out float cameraDistance;

void main() {
    blockSeed = round(Color.r * 255.0);
    phase = round(Color.g * 2.0);
    echo = round(Color.b * 2.0);
    opacity = Color.a;
    float time = ColorModulator.a;
    vec3 position = Position;
    if (echo > 0.0) {
        float clock = floor(time * 7.0 + blockSeed * 0.13);
        float angle = blockSeed * 2.41 + echo * 2.5;
        position += vec3(cos(angle), sin(angle * 1.7) * 0.5, sin(angle))
            * (0.45 + echo * 0.26 + 0.09 * sin(clock * 1.8));
        position.y += sin(time * 1.5 + blockSeed) * 0.06;
    }
    vec4 viewPosition = ModelViewMat * vec4(position, 1.0);
    gl_Position = ProjMat * viewPosition;
    faceUv = UV0;
    cameraDistance = length(viewPosition.xyz);
}
