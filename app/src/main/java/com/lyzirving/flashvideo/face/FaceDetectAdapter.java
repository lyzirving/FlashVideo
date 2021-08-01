package com.lyzirving.flashvideo.face;

/**
 * @author lyzirving
 */
public class FaceDetectAdapter implements IFaceDetect {
    @Override
    public void onFaceDetectFail() {}

    @Override
    public void onFaceRectFound(int[] faceRectArray) {}

    @Override
    public void onLandmarkFound(int[] landmarks) {}

    @Override
    public void onNoFaceDetect() {}

    @Override
    public void noLandmarkDetect() {}
}
