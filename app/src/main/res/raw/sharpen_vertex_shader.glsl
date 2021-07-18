attribute vec3 a_vertex_pos;
attribute vec2 a_texture_coord_pos;

uniform float imageWidthFactor;
uniform float imageHeightFactor;
uniform float sharpness;

varying vec2 varyingTextureCoordPos;
varying vec2 varyingLeftTextureCoordPos;
varying vec2 varyingRightTextureCoordPos;
varying vec2 varyingTopTextureCoordPos;
varying vec2 varyingBottomTextureCoordPos;

varying float varyingCenterMultiplier;
varying float varyingEdgeMultiplier;

void main() {
    gl_Position = vec4(a_vertex_pos, 1);

    mediump vec2 widthStep = vec2(imageWidthFactor, 0.0);
    mediump vec2 heightStep = vec2(0.0, imageHeightFactor);

    varyingTextureCoordPos = a_texture_coord_pos.xy;
    varyingLeftTextureCoordPos = a_texture_coord_pos.xy - widthStep;
    varyingRightTextureCoordPos = a_texture_coord_pos.xy + widthStep;
    varyingTopTextureCoordPos = a_texture_coord_pos.xy + heightStep;
    varyingBottomTextureCoordPos = a_texture_coord_pos.xy - heightStep;

    varyingCenterMultiplier = 1.0 + 4.0 * sharpness;
    varyingEdgeMultiplier = sharpness;
}
