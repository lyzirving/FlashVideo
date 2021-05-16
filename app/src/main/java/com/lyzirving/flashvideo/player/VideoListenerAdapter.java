package com.lyzirving.flashvideo.player;

/**
 * @author lyzirving
 */
public class VideoListenerAdapter implements IVideoListener {
    @Override
    public void onFrame(int width, int height, byte[] yData, byte[] uData, byte[] vData) {
        //put your own implementation here
    }

    @Override
    public void onPrepare(double duration, int width, int height) {
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
