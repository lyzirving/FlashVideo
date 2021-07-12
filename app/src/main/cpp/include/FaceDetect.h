#ifndef FLASHVIDEO_FACEDETECT_H
#define FLASHVIDEO_FACEDETECT_H

#include <jni.h>
#include <opencv2/opencv.hpp>

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

    bool bitmap2Matrix(JNIEnv* env, jobject bmp);
    bool detect();
    bool initClassifier();
    void matrix2Gray(cv::Mat &src, cv::Mat &dst);
    void release();
    void setClassifierPath(char* path);

private:
    char* mClassifierPath;
    cv::CascadeClassifier* mPtrClassifier;
    cv::Mat* mPtrImgMat;
};

#endif //FLASHVIDEO_FACEDETECT_H
