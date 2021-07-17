#include "ImgUtil.h"
#include "LogUtil.h"

#define TAG "ImgUtil"

bool ImgUtil::bitmap2Mat(JNIEnv *env, jobject &bitmap, cv::Mat &mat) {
    AndroidBitmapInfo bitmapInfo;
    if (AndroidBitmap_getInfo(env, bitmap, &bitmapInfo) < 0) {
        LogUtil::logI(TAG, {"bitmap2Mat: failed to get bitmap info"});
        return false;
    }
    bool supportFmt = bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                      bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565;
    if (!supportFmt) {
        LogUtil::logI(TAG, {"bitmap2Mat: invalid bitmap format"});
        return false;
    }
    void *bitmapPixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels) < 0 || bitmapPixels == nullptr) {
        LogUtil::logI(TAG, {"bitmap2Mat: failed to lock bitmap pixel"});
        return false;
    }
    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LogUtil::logI(TAG, {"bitmap2Mat: src type RGBA_8888"});
        //the dst channels is sorted as RGBA
        mat = cv::Mat(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);
    } else {
        LogUtil::logI(TAG, {"bitmap2Mat: src type RGB_565"});
        mat = cv::Mat(bitmapInfo.height, bitmapInfo.width, CV_8UC2, bitmapPixels);
        cv::cvtColor(mat, mat, cv::COLOR_BGR5652RGB);
    }
    AndroidBitmap_unlockPixels(env, bitmap);
    LogUtil::logI(TAG, {"bitmap2Mat: succeed to copy bmp to matrix"});
    return true;
}

bool ImgUtil::mat2Bitmap(JNIEnv *env, cv::Mat &mat, jobject &bitmap) {
    void *bitmapPixels;
    AndroidBitmapInfo bitmapInfo;
    if (AndroidBitmap_getInfo(env, bitmap, &bitmapInfo) < 0) {
        LogUtil::logI(TAG, {"mat2Bitmap: failed to get bitmap info"});
        return false;
    }
    bool supportFmt = bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                      bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565;
    if (!supportFmt) {
        LogUtil::logI(TAG, {"mat2Bitmap: invalid bitmap format"});
        return false;
    }
    if (mat.dims != 2 || bitmapInfo.height != mat.rows || bitmapInfo.width != mat.cols) {
        LogUtil::logI(TAG, {"mat2Bitmap: size does not match"});
        return false;
    }
    if (!(mat.type() == CV_8UC1 || mat.type() == CV_8UC3 || mat.type() == CV_8UC4)) {
        LogUtil::logI(TAG, {"mat2Bitmap: invalid mat type"});
        return false;
    }
    if (AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels) < 0 || bitmapPixels == nullptr) {
        LogUtil::logI(TAG, {"mat2Bitmap: failed to lock bitmap"});
        return false;
    }
    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);
        switch (mat.type()) {
            case CV_8UC1: {
                cv::cvtColor(mat, tmp, cv::COLOR_GRAY2RGBA);
                break;
            }
            case CV_8UC3: {
                cv::cvtColor(mat, tmp, cv::COLOR_RGB2RGBA);
                break;
            }
            case CV_8UC4: {
                mat.copyTo(tmp);
                break;
            }
            default: {
                LogUtil::logI(TAG, {"mat2Bitmap: no match type for transform 8888"});
                AndroidBitmap_unlockPixels(env, bitmap);
                return false;
            }
        }
    } else {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC2, bitmapPixels);
        switch (mat.type()) {
            case CV_8UC1: {
                cv::cvtColor(mat, tmp, cv::COLOR_GRAY2BGR565);
                break;
            }
            case CV_8UC3: {
                cv::cvtColor(mat, tmp, cv::COLOR_RGB2BGR565);
                break;
            }
            case CV_8UC4: {
                cv::cvtColor(mat, tmp, cv::COLOR_RGBA2BGR565);
                break;
            }
            default: {
                LogUtil::logI(TAG, {"mat2Bitmap: no match type for transform 565"});
                AndroidBitmap_unlockPixels(env, bitmap);
                return false;
            }
        }
    }
    LogUtil::logI(TAG, {"mat2Bitmap: success"});
    AndroidBitmap_unlockPixels(env, bitmap);
    return true;
}

void ImgUtil::mat2Gray(cv::Mat &src, cv::Mat &dst) {
    if (src.channels() == 3) {
        LogUtil::logI(TAG, {"mat2Gray: COLOR_RGBA2GRAY"});
        cvtColor(src, dst, cv::COLOR_RGBA2GRAY);
    } else if (src.channels() == 4) {
        LogUtil::logI(TAG, {"mat2Gray: COLOR_RGBA2GRAY"});
        cvtColor(src, dst, cv::COLOR_RGBA2GRAY);
    } else {
        LogUtil::logI(TAG, {"mat2Gray: no transform"});
        dst = src;
    }
}


