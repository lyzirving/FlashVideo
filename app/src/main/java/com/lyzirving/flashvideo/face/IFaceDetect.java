package com.lyzirving.flashvideo.face;

/**
 * @author lyzirving
 */
public interface IFaceDetect {
    /**
     * called when failing to detect face
     */
    void onFaceDetectFail();

    /**
     * called when finding face rects
     * @param faceRectArray arrays that contain the msg of face rects;
     *                  face rect is stored as left/top/right/bottom in arrays;
     *                  the number of face rect is faceRects.length() / 4
     */
    void onFaceRectFound(int[] faceRectArray);

    /**
     * landmark's length is num(face) * 68 * 2;
     * @param landmarks landmarks of faces
     */
    void onLandmarkFound(int[] landmarks);

    /**
     * called when no faces is detected
     */
    void onNoFaceDetect();

    /**
     * called when no landmark is detected
     */
    void noLandmarkDetect();
}
