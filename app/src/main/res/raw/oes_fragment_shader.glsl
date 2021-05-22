#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 v_texture_coord_pos;
uniform samplerExternalOES s_texture_sampler;
void main() {
    gl_FragColor = texture2D(s_texture_sampler, v_texture_coord_pos);
}