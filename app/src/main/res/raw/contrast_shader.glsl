precision mediump float;
varying vec2 v_texture_coord_pos;
uniform sampler2D s_texture_sampler;
uniform float u_contrast;
void main(){
    vec4 textureColor = texture2D(s_texture_sampler, v_texture_coord_pos);
    gl_FragColor = vec4(((textureColor.rgb - vec3(0.5)) * u_contrast + vec3(0.5)), textureColor.w);
}