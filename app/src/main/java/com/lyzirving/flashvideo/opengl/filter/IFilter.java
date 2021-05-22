package com.lyzirving.flashvideo.opengl.filter;

/**
 * @author lyzirving
 */
public interface IFilter {
    /**
     * method to draw
     * @param inputTextureId texture that may contains contents
     * @return texture id that is affected by the drawing process
     */
    int draw(int inputTextureId);

    /**
     * method must be called before drawn to init the opengl env
     */
    void init();

    /**
     * method to release some resources inside the filter
     */
    void release();

    /**
     * method to input coordinates of the vertex
     * @param vertex coordinates of vertex
     */
    void setVertexCoordinates(float[] vertex);

    /**
     * method to input coordinates of the texture
     * @param textureCoordinates coordinates of texture
     */
    void setTextureCoordinates(float[] textureCoordinates);

    /**
     * set output view's size
     * @param width width
     * @param height height
     */
    void setOutputSize(int width, int height);
}
