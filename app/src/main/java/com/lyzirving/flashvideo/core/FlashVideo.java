package com.lyzirving.flashvideo.core;

import android.text.TextUtils;

import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class FlashVideo {
    static {
        System.loadLibrary("native-lib");
    }
    private static final String TAG = "FlashVideo";
    private static final int INVALID_POINTER = -1;

    private long mNativePtr = INVALID_POINTER;

    public FlashVideo() {
        mNativePtr = nativeCreate();
    }

    public boolean init() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "init: pointer is invalid");
            return false;
        }
        return nativeInit(mNativePtr);
    }

    public void play() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "play: pointer is invalid");
        } else {
            nativePlay(mNativePtr);
        }
    }

    public void pause() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "pause: pointer is invalid");
        } else {
            nativePause(mNativePtr);
        }
    }

    public void stop() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "stop: pointer is invalid");
        } else {
            nativeStop(mNativePtr);
        }
    }

    public void setSourcePath(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        if (mNativePtr == INVALID_POINTER) {
            return;
        }
        nativeSetPath(mNativePtr, path);
    }

    private native static long nativeCreate();
    private native static boolean nativeInit(long ptr);
    private native static void nativeSetPath(long ptr, String path);
    private native static void nativePlay(long ptr);
    private native static void nativePause(long ptr);
    private native static void nativeStop(long ptr);
}
