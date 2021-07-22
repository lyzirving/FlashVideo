varying highp vec2 v_texture_coord_pos;
uniform sampler2D s_texture_sampler;

uniform lowp float saturation;
const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);
void main() {
    lowp vec4 textureColor = texture2D(s_texture_sampler, v_texture_coord_pos);
    lowp float luminance = dot(textureColor.rgb, luminanceWeighting);
    lowp vec3 greyScaleColor = vec3(luminance);
    gl_FragColor = vec4(mix(greyScaleColor, textureColor.rgb, saturation), textureColor.w);
}