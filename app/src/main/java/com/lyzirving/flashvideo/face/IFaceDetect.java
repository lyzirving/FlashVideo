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
     * called when no faces is detected
     */
    void onNoFaceDetect();
}
