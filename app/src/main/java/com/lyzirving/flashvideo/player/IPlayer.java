package com.lyzirving.flashvideo.player;

/**
 * @author lyzirving
 */
public interface IPlayer {
    /**
     * method is called to make the player to be prepared
     */
    void prepare();

    /**
     * called to play media
     */
    void play();

    /**
     * called to pause media
     */
    void pause();

    /**
     * called to stop media
     */
    void stop();

    /**
     * called to set data source
     * @param source path of media data
     */
    void setDataSource(String source);

    /**
     * called to seek the media to specific dst
     * @param ratio value is ranged by [0, 1];
     */
    void seek(float ratio);
}
