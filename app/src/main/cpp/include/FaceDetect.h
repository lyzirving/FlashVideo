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
        mLandmarkModelPath = nullptr;
        mPtrClassifier = nullptr;
        mPtrImgMat = nullptr;
    }
    ~FaceDetect() {
        release();
    }

    bool detect();
    bool initClassifier();
    bool initFrontFaceDetector();
    void release();
    void setClassifierPath(char* path);
    void setLandmarkModelPath(char* path);
    void setImgMat(cv::Mat& mat);
private:
    char* mClassifierPath;
    char* mLandmarkModelPath;
    cv::CascadeClassifier* mPtrClassifier;
    cv::Mat* mPtrImgMat;
    dlib::frontal_face_detector mFrontFaceDetector;
    dlib::shape_predictor mPoseModel;
};

#endif //FLASHVIDEO_FACEDETECT_H
