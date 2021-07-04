package com.lyzirving.flashvideo.opengl.core;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import androidx.annotation.IntDef;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * @author lyzirving
 */
public class MediaConfig {
    public static final String MUSIC_PREFIX = "audio/";
    public static final String VIDEO_PREFIX = "video/";
    public static final String MIME_TYPE_VIDEO_H264 = "video/avc";

    public static final int DEFAULT_VIDEO_WIDTH = 1560;
    public static final int DEFAULT_VIDEO_HEIGHT = 720;

    public static final int FRAME_RATE_30 = 30;

    public static final int I_FRAME_INTERVAL_2_SEC = 2;

    public static final int BIT_RATE_64_MILLION = 64000000;
    public static final int BIT_RATE_2_MILLION = 2000000;

    public static final int DEFAULT_SAMPLE_RATE = 44100;
    public static final int DEFAULT_CHANNEL_COUNT = 2;

    public static final int TYPE_INVALID = 0;
    public static final int TYPE_MUSIC = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_SUBTITLE = 3;

    @IntDef({TYPE_INVALID, TYPE_MUSIC, TYPE_VIDEO, TYPE_SUBTITLE})
    @Retention(CLASS)
    @Target({PARAMETER,METHOD,LOCAL_VARIABLE,FIELD})
    public @interface TYPE {}
}
