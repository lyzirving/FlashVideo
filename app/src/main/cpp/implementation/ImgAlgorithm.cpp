#include "ImgAlgorithm.h"
#include "ImgUtil.h"
#include "JavaCallbackUtil.h"
#include "LogUtil.h"
#include "TimeUtil.h"

#define TAG "ImgAlgorithm"
#define JAVA_CLASS "com/lyzirving/flashvideo/imgedit/algorithm/ImgAlgorithm"

static std::map<jlong, jobject> sGlobalListeners;

void *nMainLoop(void *args) {
    auto* ptr = static_cast<ImgAlgorithm *>(args);
    JNIEnv *env = nullptr;
    if (!JavaCallbackUtil::threadAttachJvm(common::sPtrGlobalVm, &env)) {
        LogUtil::logE(TAG, {"mainLoop: failed to attach thread to jvm"});
        return nullptr;
    }
    ptr->loop(env);
    common::sPtrGlobalVm->DetachCurrentThread();
    delete ptr;
    return nullptr;
}

static jlong nConstruct(JNIEnv *env, jclass clazz, jobject listener) {
    auto* ptr = new ImgAlgorithm;
    if (common::sPtrGlobalVm == nullptr) {
        env->GetJavaVM(&common::sPtrGlobalVm);
    }
    sGlobalListeners.insert(std::pair<jlong, jobject>(reinterpret_cast<const long>(ptr),
            env->NewGlobalRef(listener)));
    pthread_t thread;
    pthread_create(&thread, nullptr, nMainLoop, ptr);
    pthread_setname_np(thread, TAG);
    return reinterpret_cast<jlong>(ptr);
}

static void nFastNlMean(JNIEnv *env, jclass clazz, jlong pointer, jobject inputBmp, jfloat h) {
    auto* ptr = reinterpret_cast<ImgAlgorithm *>(pointer);
    common::Msg msg{.what = ImgAlgorithm::MSG_FAST_NL_MEAN, .object = (env->NewGlobalRef(inputBmp)), .valFloat = h};
    ptr->enqueue(msg);
}

static void nHistEqual(JNIEnv *env, jclass clazz, jlong pointer, jobject inputBmp) {
    auto* ptr = reinterpret_cast<ImgAlgorithm *>(pointer);
    common::Msg msg{.what = ImgAlgorithm::MSG_HIST_EQUAL, .object = (env->NewGlobalRef(inputBmp))};
    ptr->enqueue(msg);
}

static void nRelease(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* ptr = reinterpret_cast<ImgAlgorithm *>(pointer);
    common::Msg msg{.what = ImgAlgorithm::MSG_QUIT};
    ptr->enqueue(msg);
}

static JNINativeMethod sJniMethods[] = {
        {
                "nativeConstruct",
                "(Lcom/lyzirving/flashvideo/imgedit/algorithm/ImgAlgorithmListener;)J",
                (void *) nConstruct
        },
        {
                "nativeHistEqual",
                "(JLandroid/graphics/Bitmap;)V",
                (void *) nHistEqual
        },
        {
                "nativeRelease",
                "(J)V",
                (void *) nRelease
        },
        {
                "nativeNlMean",
                "(JLandroid/graphics/Bitmap;F)V",
                (void *) nFastNlMean
        },
};

bool ImgAlgorithm::registerSelf(JNIEnv *env) {
    int count = sizeof(sJniMethods) / sizeof(sJniMethods[0]);
    jclass javaClass = env->FindClass(JAVA_CLASS);
    if(!javaClass) {
        LogUtil::logE(TAG, {"registerSelf: failed to find class ", JAVA_CLASS});
        goto fail;
    }
    if (env->RegisterNatives(javaClass, sJniMethods, count) < 0) {
        LogUtil::logE(TAG, {"registerSelf: failed to register native methods ", JAVA_CLASS});
        goto fail;
    }
    LogUtil::logD(TAG, {"success to register class: ", JAVA_CLASS, ", method count ", std::to_string(count)});
    return true;
    fail:
    return false;
}

void ImgAlgorithm::enqueue(common::Msg& msg) {
    pthread_mutex_lock(&mMutexLock);
    mPtrMsgQueue->push(msg);
    pthread_cond_signal(&mCondLock);
    pthread_mutex_unlock(&mMutexLock);
}

common::Msg* ImgAlgorithm::dequeue() {
    pthread_mutex_lock(&mMutexLock);
    if (mPtrMsgQueue->empty()) {
        LogUtil::logI(TAG, {"dequeue: wait"});
        pthread_cond_wait(&mCondLock, &mMutexLock);
    }
    common::Msg* msg = &mPtrMsgQueue->front();
    mPtrMsgQueue->pop();
    pthread_mutex_unlock(&mMutexLock);
    return msg;
}

bool ImgAlgorithm::fastNlMeanDeNoise(cv::Mat &src, cv::Mat &dst, float h) {
    if (src.empty() || !src.data) {
        LogUtil::logI(TAG, {"fastNlMeanDeNoise: src is invalid"});
        return false;
    }
    long long startTime = TimeUtil::getCurrentTimeMs();
    cv::fastNlMeansDenoisingColored(src, dst, h);
    long long endTime = TimeUtil::getCurrentTimeMs();
    LogUtil::logI(TAG, {"fastNlMeanDeNoise: duration: ", std::to_string(endTime - startTime), " ms"});
    return true;
}

bool ImgAlgorithm::histEqual(cv::Mat &src, cv::Mat &dst) {
    if (src.empty() || !src.data) {
        LogUtil::logI(TAG, {"histEqual: src is invalid"});
        return false;
    }
    std::vector<cv::Mat> grbMat;
    cv::split(src, grbMat);

    cv::equalizeHist(grbMat[0], grbMat[0]);
    cv::equalizeHist(grbMat[1], grbMat[1]);
    cv::equalizeHist(grbMat[2], grbMat[2]);

    cv::merge(grbMat, dst);
    return true;
}

void ImgAlgorithm::handleFastNlMean(JNIEnv *env, jobject inputBmp, float h) {
    cv::Mat src;
    long long startTime = TimeUtil::getCurrentTimeMs();
    jobject listener = JavaCallbackUtil::findListener(&sGlobalListeners, reinterpret_cast<jlong>(this));
    jclass listenerClass = env->GetObjectClass(listener);
    jmethodID methodOnFail = env->GetMethodID(listenerClass, "onFail", "()V");
    if (!ImgUtil::bitmap2Mat(env, inputBmp, src)) {
        env->CallVoidMethod(listener, methodOnFail);
        return;
    }
    cv::Mat dst;
    if (!fastNlMeanDeNoise(src, dst, h)) {
        env->CallVoidMethod(listener, methodOnFail);
        return;
    }
    if (!ImgUtil::mat2Bitmap(env, dst, inputBmp)) {
        env->CallVoidMethod(listener, methodOnFail);
        return;
    }
    long long endTime = TimeUtil::getCurrentTimeMs();
    LogUtil::logI(TAG, {"handleFastNlMean: duration = ", std::to_string(endTime - startTime)});
    jmethodID methodOnSuccess = env->GetMethodID(listenerClass, "onGetImage", "(Landroid/graphics/Bitmap;)V");
    env->CallVoidMethod(listener, methodOnSuccess, inputBmp);
}

void ImgAlgorithm::handleHistEqual(JNIEnv* env, jobject inputBmp) {
    cv::Mat src;
    jobject listener = JavaCallbackUtil::findListener(&sGlobalListeners, reinterpret_cast<jlong>(this));
    jclass listenerClass = env->GetObjectClass(listener);
    jmethodID methodOnFail = env->GetMethodID(listenerClass, "onFail", "()V");
    if (!ImgUtil::bitmap2Mat(env, inputBmp, src)) {
        env->CallVoidMethod(listener, methodOnFail);
        return;
    }
    cv::Mat dst;
    if (!histEqual(src, dst)) {
        env->CallVoidMethod(listener, methodOnFail);
        return;
    }
    if (!ImgUtil::mat2Bitmap(env, dst, inputBmp)) {
        env->CallVoidMethod(listener, methodOnFail);
        return;
    }
    jmethodID methodOnSuccess = env->GetMethodID(listenerClass, "onGetImage", "(Landroid/graphics/Bitmap;)V");
    env->CallVoidMethod(listener, methodOnSuccess, inputBmp);
}

void ImgAlgorithm::loop(JNIEnv *env) {
    common::Msg* msg = nullptr;
    for (;;) {
        msg = dequeue();
        switch (msg->what) {
            case MSG_HIST_EQUAL: {
                LogUtil::logI(TAG, {"loop: handle msg hist equal"});
                handleHistEqual(env, static_cast<jobject>(msg->object));
                break;
            }
            case MSG_FAST_NL_MEAN: {
                LogUtil::logI(TAG, {"loop: handle msg fast nl mean"});
                handleFastNlMean(env, static_cast<jobject>(msg->object), msg->valFloat);
                break;
            }
            case MSG_QUIT: {
                LogUtil::logI(TAG, {"loop: handle msg quit"});
                goto quit;
            }
            default: {
                break;
            }
        }
    }
    quit:
    LogUtil::logI(TAG, {"loop: quit"});
    JavaCallbackUtil::removeListener(&sGlobalListeners,  reinterpret_cast<jlong>(this));
}

