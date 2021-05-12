package com.lyzirving.flashvideo.core;

import android.text.TextUtils;

import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class FlashAudio {
    private static final String TAG = "FlashVideo";
    private static final int INVALID_POINTER = -1;

    private long mNativePtr = INVALID_POINTER;
    private double mTotalTime;

    public FlashAudio() {
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

    public void seek(float ratio) {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "seek: pointer is invalid");
        } else {
            nativeSeek(mNativePtr, ratio);
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
            LogUtil.e(TAG, "setSourcePath: path is empty");
        } else if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "setSourcePath: pointer is invalid");
        } else {
            nativeSetPath(mNativePtr, path);
        }
    }

    public void setVideoListener(VideoListenerAdapter listener) {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "setVideoListener: pointer is invalid");
        } else if (listener == null) {
            LogUtil.e(TAG, "setVideoListener: listener is null");
        } else {
            nativeSetListener(mNativePtr, listener);
        }
    }

    public void setVolume(int volume) {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "setVolume: pointer is invalid");
        } else {
            nativeSetVolume(mNativePtr, volume);
        }
    }

    public void setPitch(double pitch) {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "setPitch: pointer is invalid");
        } else {
            nativeSetPitch(mNativePtr, pitch);
        }
    }

    public void setTempo(double tempo) {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.e(TAG, "setTempo: pointer is invalid");
        } else {
            nativeSetTempo(mNativePtr, tempo);
        }
    }

    public void setTotalTime(double time) {
        mTotalTime = time;
    }

    private native static long nativeCreate();
    private native static boolean nativeInit(long ptr);
    private native static void nativeSetPath(long ptr, String path);
    private native static void nativePlay(long ptr);
    private native static void nativePause(long ptr);
    private native static void nativeSetPitch(long ptr, double pitch);
    private native static void nativeSetTempo(long ptr, double tempo);
    private native static void nativeSeek(long ptr, float seekDst);
    private native static void nativeStop(long ptr);
    private native static void nativeSetListener(long ptr, VideoListenerAdapter listener);
    private native static void nativeSetVolume(long ptr, int value);
}
