package com.lyzirving.flashvideo.core;

/**
 * @author lyzirving
 */
public class VideoListenerAdapter implements IVideoListener {
    @Override
    public void onFrame(int width, int height, byte[] yData, byte[] uData, byte[] vData) {
        //put your own implementation here
    }

    @Override
    public void onPrepare(double duration) {
        //put your own implementation here
    }

    @Override
    public void onStop() {
        //put your own implementation here
    }

    @Override
    public void onTickTime(double currentTime) {
        //put your own implementation here
    }
}
