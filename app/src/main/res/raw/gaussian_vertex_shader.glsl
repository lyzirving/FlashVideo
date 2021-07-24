const int GAUSSIAN_SAMPLES = 9;

attribute vec3 a_vertex_pos;
attribute vec2 a_texture_coord_pos;

uniform float u_texture_width_offset;
uniform float u_texture_height_offset;
uniform mat4 u_matrix;

varying vec2 v_texture_coord_pos;
varying vec2 v_blur_coordinates[GAUSSIAN_SAMPLES];

void main() {
    gl_Position = u_matrix * vec4(a_vertex_pos,1);
    v_texture_coord_pos = a_texture_coord_pos;

    //Calculate the positions for the blur
    int multiplier = 0;
    vec2 blur_step;
    vec2 single_step_offset = vec2(u_texture_height_offset, u_texture_width_offset);
    for (int i = 0; i < GAUSSIAN_SAMPLES; i++) {
        multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));
        // Blur in x (horizontal)
        blur_step = float(multiplier) * single_step_offset;
        v_blur_coordinates[i] = a_texture_coord_pos + blur_step;
    }
}
