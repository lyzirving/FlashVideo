attribute vec3 a_vertex_pos;
attribute vec2 a_texture_coord_pos;

uniform highp float texelWidth;
uniform highp float texelHeight;

varying vec2 v_texture_coord_pos;

varying vec2 leftTextureCoordinate;
varying vec2 rightTextureCoordinate;

varying vec2 topTextureCoordinate;
varying vec2 topLeftTextureCoordinate;
varying vec2 topRightTextureCoordinate;

varying vec2 bottomTextureCoordinate;
varying vec2 bottomLeftTextureCoordinate;
varying vec2 bottomRightTextureCoordinate;

void main() {
    gl_Position = vec4(a_vertex_pos,1);

    vec2 widthStep = vec2(texelWidth, 0.0);
    vec2 heightStep = vec2(0.0, texelHeight);
    vec2 widthHeightStep = vec2(texelWidth, texelHeight);
    vec2 widthNegativeHeightStep = vec2(texelWidth, -texelHeight);

    v_texture_coord_pos = a_texture_coord_pos.xy;
    leftTextureCoordinate = a_texture_coord_pos.xy - widthStep;
    rightTextureCoordinate = a_texture_coord_pos.xy + widthStep;

    topTextureCoordinate = a_texture_coord_pos.xy - heightStep;
    topLeftTextureCoordinate = a_texture_coord_pos.xy - widthHeightStep;
    topRightTextureCoordinate = a_texture_coord_pos.xy + widthNegativeHeightStep;

    bottomTextureCoordinate = a_texture_coord_pos.xy + heightStep;
    bottomLeftTextureCoordinate = a_texture_coord_pos.xy - widthNegativeHeightStep;
    bottomRightTextureCoordinate = a_texture_coord_pos.xy + widthHeightStep;
}
