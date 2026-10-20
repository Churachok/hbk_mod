#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;

out vec2 faceUv;
flat out vec4 spriteBounds;
flat out float blockSeed;
out float cameraDistance;

void main() {
    vec4 viewPosition = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPosition;
    faceUv = UV0;
    // Overlay/lightmap attributes carry the original block sprite's atlas rectangle.
    spriteBounds = vec4(vec2(UV1), vec2(UV2)) / 32767.0;
    blockSeed = dot(Color.rgb, vec3(255.0, 64005.0, 160905.0));
    cameraDistance = length(viewPosition.xyz);
}
