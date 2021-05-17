package com.lyzirving.flashvideo.player;

import android.text.TextUtils;

import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class FlashVideo implements IPlayer {
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

    @Override
    public void prepare() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "init: pointer is invalid");
        } else {
            nativeInit(mNativePtr);
        }
    }

    @Override
    public void play() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "play: pointer is invalid");
        } else {
            nativePlay(mNativePtr);
        }
    }

    @Override
    public void pause() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "pause: pointer is invalid");
        } else {
            nativePause(mNativePtr);
        }
    }

    @Override
    public void stop() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "stop: pointer is invalid");
        } else {
            nativeStop(mNativePtr);
        }
    }

    @Override
    public void setDataSource(String source) {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "setDataSource: pointer is invalid");
        } else if (TextUtils.isEmpty(source)) {
            LogUtil.e(TAG, "setDataSource: path is empty");
        } else {
            nativeSetPath(mNativePtr, source);
        }
    }

    @Override
    public void seek(float ratio) {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "seek: pointer is invalid");
        } else {
            nativeSeek(mNativePtr, ratio);
        }
    }

    public void setListener(VideoListenerAdapter listener) {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "setListener: pointer is invalid");
        } else if (listener == null) {
            LogUtil.e(TAG, "setListener: listener is null");
        } else {
            nativeSetListener(mNativePtr, listener);
        }
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
