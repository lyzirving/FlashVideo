package com.lyzirving.flashvideo.player;

/**
 * @author lyzirving
 */
public interface IVideoListener {
    void onFrame(int width, int height, byte[] yData, byte[] uData, byte[] vData);

    /**
     * called when media source is prepared
     * @param duration the total time of playback
     * @param width width of video frame
     * @param height height of video frame
     */
    void onPrepare(double duration, int width, int height);

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
