package com.lyzirving.flashvideo.core;

/**
 * @author lyzirving
 */
public interface IVideoListener {
    void onFrame(int width, int height, byte[] yData, byte[] uData, byte[] vData);
    /**
     * called when media source is prepared
     * @param duration the total time of playback
     */
    void onPrepare(double duration);

    /**
     * called when media is stopped and source is about to be released
     */
    void onStop();

    /**
     * called when media is played for each second
     * @param currentTime current played time
     */
    void onTickTime(double currentTime);
}
