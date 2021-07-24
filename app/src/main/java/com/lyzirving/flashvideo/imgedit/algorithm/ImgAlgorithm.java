package com.lyzirving.flashvideo.imgedit.algorithm;

import android.graphics.Bitmap;

/**
 * @author lyzirving
 */
public class ImgAlgorithm {
    private static final String TAG = "ImgAlgorithm";
    private static final int INVALID_PTR = -1;
    private long mPtr = INVALID_PTR;

    public ImgAlgorithm(ImgAlgorithmListener listener) {
        if (listener == null) {
            throw new RuntimeException(TAG + ": empty listener");
        }
        mPtr = nativeConstruct(listener);
    }

    public void fastNlMean(Bitmap bmp, float h) {
        nativeNlMean(mPtr, bmp, h);
    }

    public void histEqual(Bitmap bmp) {
        nativeHistEqual(mPtr, bmp);
    }

    public void release() {
        nativeRelease(mPtr);
    }

    private static native long nativeConstruct(ImgAlgorithmListener listener);
    private static native void nativeNlMean(long ptr, Bitmap input, float h);
    private static native void nativeHistEqual(long ptr, Bitmap input);
    private static native void nativeRelease(long ptr);
}
