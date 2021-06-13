package com.lyzirving.flashvideo.player;

import android.text.TextUtils;

import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class FlashVideoPlayer {
    private static final String TAG = "FlashVideoPlayer";
    private static final int INVALID_POINTER = -1;

    private long mNativePtr = INVALID_POINTER;

    public FlashVideoPlayer() {
        mNativePtr = nativeConstruct();
    }

    public void init(String path, VideoListenerAdapter listener) {
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException("path is empty");
        } else if (listener == null) {
            throw new RuntimeException("listener is null");
        }
        nativeInit(mNativePtr, path, listener);
    }

    public void play() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.d(TAG, "play: invalid pointer");
            return;
        }
        nativePlay(mNativePtr);
    }

    public void pause() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.d(TAG, "pause: invalid pointer");
            return;
        }
        nativePause(mNativePtr);
    }

    public void stop() {
        if (mNativePtr == INVALID_POINTER) {
            LogUtil.d(TAG, "stop: invalid pointer");
            return;
        }
        nativeStop(mNativePtr);
    }

    private static native long nativeConstruct();
    private static native void nativeInit(long ptr, String path, VideoListenerAdapter listener);
    private static native void nativePlay(long ptr);
    private static native void nativePause(long ptr);
    private static native void nativeStop(long ptr);
}
