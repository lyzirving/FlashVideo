package com.lyzirving.flashvideo.util;

import android.os.Environment;

/**
 * @author lyzirving
 */
public class FileUtil {
    private static final String TAG = "FileUtil";
    public static final String MOVIE_CACHE_DIR = ComponentUtil.get().ctx().getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath();
    public static final String MUSIC_CACHE_DIR = ComponentUtil.get().ctx().getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath();

    private static volatile FileUtil sInstance;

    private FileUtil() {}

    public static FileUtil get() {
        if (sInstance == null) {
            synchronized (FileUtil.class) {
                if (sInstance == null) {
                    sInstance = new FileUtil();
                }
            }
        }
        return sInstance;
    }
}
