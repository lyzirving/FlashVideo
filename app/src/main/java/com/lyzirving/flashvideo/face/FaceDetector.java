package com.lyzirving.flashvideo.face;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.lyzirving.flashvideo.util.AssetsManager;
import com.lyzirving.flashvideo.util.FileUtil;
import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class FaceDetector {
    private static final String TAG = "FaceDetector";
    private static final int INVALID_PTR = -1;
    private static final String DEFAULT_CLASSIFIER_PATH = FileUtil.DOC_CACHE_DIR + "/" + "haarcascade_frontalface_default.xml";
    private long mPtr = INVALID_PTR;

    public FaceDetector() {
        mPtr = nativeConstruct();
    }

    public boolean detect(Bitmap bmp) {
        if (mPtr == INVALID_PTR) {
            LogUtil.i(TAG, "detect: invalid ptr");
            return false;
        }
        if (bmp == null) {
            LogUtil.i(TAG, "detect: input bmp is null");
            return false;
        }
        return nativeDetect(mPtr, bmp);
    }

    public void destroy() {
        if (mPtr == INVALID_PTR) {
            LogUtil.i(TAG, "destroy: invalid ptr");
            return;
        }
        nativeDestroy(mPtr);
    }

    public boolean init(String classifierPath, FaceDetectAdapter adapter) {
        if (mPtr == INVALID_PTR) {
            LogUtil.i(TAG, "init: invalid ptr");
            return false;
        }
        if (TextUtils.isEmpty(classifierPath)) {
            LogUtil.i(TAG, "init: classifier path is null, use default");
            classifierPath = DEFAULT_CLASSIFIER_PATH;
        }
        return nativeInit(mPtr, classifierPath, adapter);
    }

    private static native long nativeConstruct();
    private static native void nativeDestroy(long ptr);
    private static native boolean nativeDetect(long ptr, Bitmap bmp);
    private static native boolean nativeInit(long ptr, String classifierPath, FaceDetectAdapter adapter);
}
