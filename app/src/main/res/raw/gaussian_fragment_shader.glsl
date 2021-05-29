precision mediump float;
const int GAUSSIAN_SAMPLES = 9;

uniform sampler2D s_texture_sampler;
varying vec2 v_texture_coord_pos;
varying vec2 v_blur_coordinates[GAUSSIAN_SAMPLES];

void main() {
    vec3 sum = vec3(0.0);
    vec4 frag_color = texture2D(s_texture_sampler, v_texture_coord_pos);

    sum += texture2D(s_texture_sampler, v_blur_coordinates[0]).rgb * 0.05;
    sum += texture2D(s_texture_sampler, v_blur_coordinates[1]).rgb * 0.09;
    sum += texture2D(s_texture_sampler, v_blur_coordinates[2]).rgb * 0.12;
    sum += texture2D(s_texture_sampler, v_blur_coordinates[3]).rgb * 0.15;
    sum += texture2D(s_texture_sampler, v_blur_coordinates[4]).rgb * 0.18;
    sum += texture2D(s_texture_sampler, v_blur_coordinates[5]).rgb * 0.15;
    sum += texture2D(s_texture_sampler, v_blur_coordinates[6]).rgb * 0.12;
    sum += texture2D(s_texture_sampler, v_blur_coordinates[7]).rgb * 0.09;
    sum += texture2D(s_texture_sampler, v_blur_coordinates[8]).rgb * 0.05;

    gl_FragColor = vec4(sum, frag_color.a);
}