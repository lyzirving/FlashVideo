#ifndef FLASHVIDEO_FACEDETECT_H
#define FLASHVIDEO_FACEDETECT_H

#include "ImgUtil.h"

class FaceDetect {
public:
    static bool registerSelf(JNIEnv *env);
    static void callAdapterFail(jobject &adapter);
    static void callAdapterDetectSuccess(jobject &adapter, std::vector<cv::Rect> &faces);
    FaceDetect() {
        mClassifierPath = nullptr;
        mPtrClassifier = nullptr;
        mPtrImgMat = nullptr;
    }
    ~FaceDetect() {
        release();
    }

    bool detect();
    bool initClassifier();
    void release();
    void setClassifierPath(char* path);
    void setImgMat(cv::Mat& mat);
private:
    char* mClassifierPath;
    cv::CascadeClassifier* mPtrClassifier;
    cv::Mat* mPtrImgMat;
};

#endif //FLASHVIDEO_FACEDETECT_H
