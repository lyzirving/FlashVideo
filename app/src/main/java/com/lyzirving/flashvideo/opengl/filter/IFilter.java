package com.lyzirving.flashvideo.opengl.filter;

/**
 * @author lyzirving
 */
public interface IFilter {
    /**
     * method to draw the specific texture via textureId
     * @param textureId id of texture
     */
    void draw(int textureId);

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
}
