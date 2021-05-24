attribute vec3 a_vertex_pos;
attribute vec2 a_texture_coord_pos;
uniform mat4 u_matrix;
varying vec2 v_texture_coord_pos;
void main() {
    gl_Position = u_matrix * vec4(a_vertex_pos,1);
    v_texture_coord_pos = a_texture_coord_pos;
}
