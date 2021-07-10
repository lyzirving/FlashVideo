package com.lyzirving.flashvideo.util;

import android.os.Environment;

import java.io.File;

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

    public boolean deleteSingleFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                LogUtil.i(TAG, "deleteSingleFile: succeed to delete " + fileName);
                return true;
            } else {
                LogUtil.i(TAG, "deleteSingleFile: failed to delete " + fileName);
                return false;
            }
        } else {
            LogUtil.i(TAG, "deleteSingleFile: failed to delete " + fileName + " because the file does not exist");
            return false;
        }
    }
}
