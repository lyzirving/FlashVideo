precision mediump float;
varying vec2 v_texture_coord_pos;
uniform sampler2D sampler_y;
uniform sampler2D sampler_u;
uniform sampler2D sampler_v;
void main() {
    float y, u, v;
    vec3 rgb;
    y = texture2D(sampler_y, v_texture_coord_pos).r;
    u = texture2D(sampler_u, v_texture_coord_pos).r - 0.5;
    v = texture2D(sampler_v, v_texture_coord_pos).r - 0.5;
    //yuv ---> rgb
    rgb.r = y + 1.403 * v;
    rgb.g = y - 0.344 * u - 0.714 * v;
    rgb.b = y + 1.770 * u;
    gl_FragColor = vec4(rgb, 1);
}
