#ifndef FLASHVIDEO_IMGALGORITHM_H
#define FLASHVIDEO_IMGALGORITHM_H

#include "ImgUtil.h"
#include <queue>
#include <pthread.h>

class ImgAlgorithm {
public:
    static bool registerSelf(JNIEnv *env);
    const static int MSG_HIST_EQUAL = 1;
    const static int MSG_QUIT = 2;

    ImgAlgorithm() {
        mPtrMsgQueue = new std::queue<common::Msg>;
        pthread_mutex_init(&mMutexLock, nullptr);
        pthread_cond_init(&mCondLock, nullptr);
    }

    ~ImgAlgorithm() {
        if (mPtrMsgQueue != nullptr) {
            delete mPtrMsgQueue;
            mPtrMsgQueue = nullptr;
        }
        pthread_mutex_destroy(&mMutexLock);
        pthread_cond_destroy(&mCondLock);
    }

    void enqueue(common::Msg& msg);
    common::Msg* dequeue();
    bool histEqual(cv::Mat &src, cv::Mat &dst);
    void loop(JNIEnv* env);
private:
    std::queue<common::Msg>* mPtrMsgQueue;
    pthread_mutex_t mMutexLock;
    pthread_cond_t mCondLock;

    void handleHistEqual(JNIEnv* env, jobject inputBmp);
};

#endif //FLASHVIDEO_IMGALGORITHM_H
