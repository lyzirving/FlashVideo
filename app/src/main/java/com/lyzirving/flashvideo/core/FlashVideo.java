package com.lyzirving.flashvideo.core;

import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class FlashVideo {
    private static final String TAG = "FlashVideo";
    private static final int INVALID_POINTER = -1;

    private long mNativePtr = INVALID_POINTER;
    private double mTotalTime;

    public FlashVideo() {
        mNativePtr = nativeCreate();
    }

    public double getTotalTime() {
        return mTotalTime;
    }

    public boolean init() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "init: pointer is invalid");
            return false;
        }
        return nativeInit(mNativePtr);
    }

    private native static long nativeCreate();
    private native static boolean nativeInit(long ptr);
    private native static void nativeSetPath(long ptr, String path);
    private native static void nativePlay(long ptr);
    private native static void nativePause(long ptr);
    private native static void nativeStop(long ptr);
    private native static void nativeSeek(long ptr, float seekDst);
    private native static void nativeSetListener(long ptr, VideoListenerAdapter listener);
}
