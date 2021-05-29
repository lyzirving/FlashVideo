precision mediump float;
varying highp vec2 v_texture_coord_pos;
uniform sampler2D s_texture_sampler;
uniform float u_saturation;
const vec3 luminance_weight = vec3(0.2125, 0.7154, 0.0721);

void main(){
    vec4 texture_color = texture2D(s_texture_sampler, v_texture_coord_pos);
    float luminance = dot(texture_color.rgb, luminance_weight);
    vec3 grey_scale_color = vec3(luminance);
    gl_FragColor = vec4(mix(grey_scale_color, texture_color.rgb, u_saturation), texture_color.w);
}