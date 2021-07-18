precision highp float;

varying highp vec2 varyingTextureCoordPos;
varying highp vec2 varyingLeftTextureCoordPos;
varying highp vec2 varyingRightTextureCoordPos;
varying highp vec2 varyingTopTextureCoordPos;
varying highp vec2 varyingBottomTextureCoordPos;

varying highp float varyingCenterMultiplier;
varying highp float varyingEdgeMultiplier;

uniform sampler2D s_texture_sampler;

void main() {
    mediump vec3 textureColor = texture2D(s_texture_sampler, varyingTextureCoordPos).rgb;
    mediump vec3 leftTextureColor = texture2D(s_texture_sampler, varyingLeftTextureCoordPos).rgb;
    mediump vec3 rightTextureColor = texture2D(s_texture_sampler, varyingRightTextureCoordPos).rgb;
    mediump vec3 topTextureColor = texture2D(s_texture_sampler, varyingTopTextureCoordPos).rgb;
    mediump vec3 bottomTextureColor = texture2D(s_texture_sampler, varyingBottomTextureCoordPos).rgb;

    gl_FragColor = vec4((textureColor * varyingCenterMultiplier - (leftTextureColor * varyingEdgeMultiplier + rightTextureColor * varyingEdgeMultiplier + topTextureColor * varyingEdgeMultiplier + bottomTextureColor * varyingEdgeMultiplier)), texture2D(s_texture_sampler, varyingBottomTextureCoordPos).w);
}