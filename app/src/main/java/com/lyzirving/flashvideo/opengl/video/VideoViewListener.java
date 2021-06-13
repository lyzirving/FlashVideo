package com.lyzirving.flashvideo.opengl.video;

/**
 * @author lyzirving
 */
public interface VideoViewListener {
    /**
     * called when the video player is prepared
     * @param duration total time duration of video source
     */
    void onPrepare(double duration);

    /**
     * called when video begins to play
     */
    void onVideoPlay();

    /**
     * called when video is paused
     */
    void onVideoPause();

    /**
     * called when video is played for one second
     * @param currentTime current video played time
     */
    void onVideoTickTime(double currentTime);

    /**
     * called when video is stopped
     */
    void onVideoStop();
}
