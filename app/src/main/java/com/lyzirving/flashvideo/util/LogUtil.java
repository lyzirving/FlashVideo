package com.lyzirving.flashvideo.util;

import android.util.Log;

/**
 * @author lyzirving
 */
public class LogUtil {
    private static final String TAG = "FlashVideo";

    public static void d(String tag, String msg) {
        Log.d(buildTag(tag), msg);
    }

    public static void i(String tag, String msg) {
        Log.i(buildTag(tag), msg);
    }

    public static void e(String tag, String msg) {
        Log.e(buildTag(tag), msg);
    }

    private static String buildTag(String tag) {
        return new StringBuilder().append(TAG).append("_").append(tag).toString();
    }
}
