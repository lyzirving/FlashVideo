attribute vec3 a_vertex_pos;
attribute vec2 a_texture_coord_pos;
varying vec2 v_texture_coord_pos;
void main() {
    gl_Position = vec4(a_vertex_pos,1);
    v_texture_coord_pos = a_texture_coord_pos;
}
