precision highp float;
const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);

varying vec2 v_texture_coord_pos;
uniform sampler2D s_texture_sampler;

varying vec2 leftTextureCoordinate;
varying vec2 rightTextureCoordinate;

varying vec2 topTextureCoordinate;
varying vec2 topLeftTextureCoordinate;
varying vec2 topRightTextureCoordinate;

varying vec2 bottomTextureCoordinate;
varying vec2 bottomLeftTextureCoordinate;
varying vec2 bottomRightTextureCoordinate;

uniform highp float threshold;
uniform highp float quantizationLevels;

void main() {
    vec4 textureColor = texture2D(s_texture_sampler, v_texture_coord_pos);

    float bottomLeftIntensity = texture2D(s_texture_sampler, bottomLeftTextureCoordinate).r;
    float topRightIntensity = texture2D(s_texture_sampler, topRightTextureCoordinate).r;
    float topLeftIntensity = texture2D(s_texture_sampler, topLeftTextureCoordinate).r;
    float bottomRightIntensity = texture2D(s_texture_sampler, bottomRightTextureCoordinate).r;

    float leftIntensity = texture2D(s_texture_sampler, leftTextureCoordinate).r;
    float rightIntensity = texture2D(s_texture_sampler, rightTextureCoordinate).r;
    float bottomIntensity = texture2D(s_texture_sampler, bottomTextureCoordinate).r;
    float topIntensity = texture2D(s_texture_sampler, topTextureCoordinate).r;

    float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;
    float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;
    float mag = length(vec2(h, v));

    vec3 posterizedImageColor = floor((textureColor.rgb * quantizationLevels) + 0.5) / quantizationLevels;
    float thresholdTest = 1.0 - step(threshold, mag);
    gl_FragColor = vec4(posterizedImageColor * thresholdTest, textureColor.a);
}