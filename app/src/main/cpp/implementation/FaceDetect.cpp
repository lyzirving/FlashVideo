#include "FaceDetect.h"
#include "LogUtil.h"
#include "JavaCallbackUtil.h"

#include <android/bitmap.h>

#define TAG "FaceDetect"
#define JAVA_CLASS "com/lyzirving/flashvideo/face/FaceDetector"
#define FACE_DETECT_ADAPTER_CLASS "com/lyzirving/flashvideo/face/FaceDetectAdapter"

static JavaVM* sPtrGlobJvm = nullptr;
static std::map<jlong, jobject> sGlobFaceDetectAdapter;

static jlong nConstruct(JNIEnv *env, jclass clazz) {
    if (sPtrGlobJvm == nullptr) {
        env->GetJavaVM(&sPtrGlobJvm);
    }
    return reinterpret_cast<jlong>(new FaceDetect);
}

static jboolean nDetect(JNIEnv *env, jclass clazz, jlong ptr, jobject bitmap) {
    auto* ptrDetect = reinterpret_cast<FaceDetect *>(ptr);
    if (!ptrDetect->bitmap2Matrix(env, bitmap)) {
        LogUtil::logI(TAG, {"nDetect: failed to convert bitmap to matrix"});
        return false;
    }
    return ptrDetect->detect();
}

static void nDestroy(JNIEnv *env, jclass clazz, jlong ptr) {
    auto* p_face_detect = reinterpret_cast<FaceDetect *>(ptr);
    if (p_face_detect != nullptr) {
        delete p_face_detect;
    }
}

static jboolean nInit(JNIEnv *env, jclass clazz, jlong ptr, jstring jpath, jobject adapter) {
    auto* p_face_detect = reinterpret_cast<FaceDetect *>(ptr);
    char* c_path = const_cast<char *>(env->GetStringUTFChars(jpath, 0));
    p_face_detect->setClassifierPath(c_path);
    env->ReleaseStringUTFChars(jpath, c_path);
    sGlobFaceDetectAdapter.insert(std::pair<jlong, jobject >(ptr, env->NewGlobalRef(adapter)));
    return p_face_detect->initClassifier();
}

static JNINativeMethod jni_methods[] = {
        {
                "nativeConstruct",
                "()J",
                (void *) nConstruct
        },
        {
                "nativeInit",
                "(JLjava/lang/String;Lcom/lyzirving/flashvideo/face/FaceDetectAdapter;)Z",
                (void *) nInit
        },
        {
                "nativeDestroy",
                "(J)V",
                (void *) nDestroy
        },
        {
                "nativeDetect",
                "(JLandroid/graphics/Bitmap;)Z",
                (void *) nDetect
        },
};

bool FaceDetect::bitmap2Matrix(JNIEnv* env, jobject bmp) {
    AndroidBitmapInfo bitmapInfo;
    if (AndroidBitmap_getInfo(env, bmp, &bitmapInfo) < 0) {
        LogUtil::logI(TAG, {"bitmap2Matrix: failed to get bitmap info"});
        return false;
    }
    bool supportFmt = bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888 || bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565;
    if (!supportFmt) {
        LogUtil::logI(TAG, {"bitmap2Matrix: invalid bitmap format"});
        return false;
    }
    void* bitmapPixels;
    if (AndroidBitmap_lockPixels(env, bmp, &bitmapPixels) < 0 || bitmapPixels == nullptr) {
        LogUtil::logI(TAG, {"bitmap2Matrix: failed to lock bitmap pixel"});
        return false;
    }
    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        mPtrImgMat = new cv::Mat(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);
    } else {
        mPtrImgMat = new cv::Mat(bitmapInfo.height, bitmapInfo.width, CV_8UC2, bitmapPixels);
        cv::cvtColor(*mPtrImgMat, *mPtrImgMat, cv::COLOR_BGR5652RGB);
    }
    //convert RGB to BGR
    cv::cvtColor(*mPtrImgMat, *mPtrImgMat,cv::COLOR_RGB2BGR);
    AndroidBitmap_unlockPixels(env, bmp);
    LogUtil::logI(TAG, {"bitmap2Matrix: succeed to copy bmp to matrix"});
    return true;
}

void FaceDetect::callAdapterFail(jobject &adapter) {
    if (adapter == nullptr) {
        LogUtil::logI(TAG, {"callAdapterFail: adapter is null"});
        return;
    }
    JNIEnv *env = nullptr;
    bool detached = sPtrGlobJvm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_EDETACHED;
    if (detached) sPtrGlobJvm->AttachCurrentThread(&env, nullptr);

    jclass adapterClass = env->GetObjectClass(adapter);
    jmethodID methodId = env->GetMethodID(adapterClass, "onFaceDetectFail", "()V");
    env->CallVoidMethod(adapter, methodId);

    if (detached) sPtrGlobJvm->DetachCurrentThread();
}

void FaceDetect::callAdapterDetectSuccess(jobject &adapter, std::vector<cv::Rect> &faces) {
    if (adapter == nullptr) {
        LogUtil::logI(TAG, {"callAdapterDetectSuccess: adapter is null"});
        return;
    }
    JNIEnv *env = nullptr;
    bool detached = sPtrGlobJvm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_EDETACHED;
    if (detached) sPtrGlobJvm->AttachCurrentThread(&env, nullptr);

    jclass adapterClass = env->GetObjectClass(adapter);
    if (faces.size() == 0) {
        jmethodID noFaceMethodId = env->GetMethodID(adapterClass, "onNoFaceDetect", "()V");
        env->CallVoidMethod(adapter, noFaceMethodId);
    } else {
        jmethodID faceFoundMethodId = env->GetMethodID(adapterClass, "onFaceRectFound", "([I)V");
        int* result = new int[4 * faces.size()];
        for (int i = 0; i < faces.size(); ++i) {
            result[i * 4 + 0] = faces[i].x;
            result[i * 4 + 1] = faces[i].y;
            result[i * 4 + 2] = faces[i].x + faces[i].width;
            result[i * 4 + 3] = faces[i].y + faces[i].height;
        }
        jintArray array = env->NewIntArray(4 * faces.size());
        env->SetIntArrayRegion(array, 0, 4 * faces.size(), result);
        env->CallVoidMethod(adapter, faceFoundMethodId, array);
        env->ReleaseIntArrayElements(array, result, 0);
    }

    if (detached) sPtrGlobJvm->DetachCurrentThread();
}

bool FaceDetect::detect() {
    jobject adapter = nullptr;
    adapter = JavaCallbackUtil::findListener(&sGlobFaceDetectAdapter, reinterpret_cast<jlong>(this));
    if (mPtrImgMat == nullptr) {
        LogUtil::logI(TAG, {"detect: src mat is null"});
        callAdapterFail(adapter);
        return false;
    }
    cv::Mat gray;
    matrix2Gray(*mPtrImgMat, gray);
    std::vector<cv::Rect> faces;
    mPtrClassifier->detectMultiScale(
            gray, faces,
            1.1f, 4,
            cv::CASCADE_FIND_BIGGEST_OBJECT | cv::CASCADE_DO_ROUGH_SEARCH,
            cv::Size(30, 30));
    callAdapterDetectSuccess(adapter, faces);
    LogUtil::logI(TAG, {"detect: result face = ", std::to_string(faces.size())});
    return true;
}

void FaceDetect::matrix2Gray(cv::Mat &src, cv::Mat &dst) {
    if (src.channels() == 3) {
        LogUtil::logI(TAG, {"matrix2Gray: COLOR_BGR2GRAY"});
        cvtColor(src, dst, cv::COLOR_BGR2GRAY);
    } else if (src.channels() == 4) {
        LogUtil::logI(TAG, {"matrix2Gray: COLOR_BGRA2GRAY"});
        cvtColor(src, dst, cv::COLOR_BGRA2GRAY);
    } else {
        LogUtil::logI(TAG, {"matrix2Gray: no transform"});
        dst = src;
    }
}

bool FaceDetect::initClassifier() {
    if (mClassifierPath == nullptr || std::strlen(mClassifierPath) == 0) {
        LogUtil::logI(TAG, {"initClassifier: classifier path is empty"});
        return false;
    }
    mPtrClassifier = new cv::CascadeClassifier;
    try {
        mPtrClassifier->load(mClassifierPath);
    } catch (cv::Exception e) {
        LogUtil::logE(TAG, {"initClassifier: exception happens = ", e.msg});
        return false;
    }
    LogUtil::logI(TAG, {"initClassifier: success"});
    return true;
}

bool FaceDetect::registerSelf(JNIEnv *env) {
    int count = sizeof(jni_methods) / sizeof(jni_methods[0]);
    jclass javaClass = env->FindClass(JAVA_CLASS);
    if(!javaClass) {
        LogUtil::logE(TAG, {"registerSelf: failed to find class ", JAVA_CLASS});
        goto fail;
    }
    if (env->RegisterNatives(javaClass, jni_methods, count) < 0) {
        LogUtil::logE(TAG, {"registerSelf: failed to register native methods ", JAVA_CLASS});
        goto fail;
    }
    LogUtil::logD(TAG, {"success to register class: ", JAVA_CLASS, ", method count ", std::to_string(count)});
    return true;
    fail:
    return false;
}

void FaceDetect::release() {
    LogUtil::logI(TAG, {"release"});
    if (mClassifierPath != nullptr) {
        free(mClassifierPath);
        mClassifierPath = nullptr;
    }
    if (mPtrClassifier != nullptr) {
        delete mPtrClassifier;
        mPtrClassifier = nullptr;
    }
    if (mPtrImgMat != nullptr) {
        mPtrImgMat->release();
        delete mPtrImgMat;
        mPtrImgMat = nullptr;
    }
}

void FaceDetect::setClassifierPath(char *path) {
    if (path == nullptr || std::strlen(path) == 0) {
        LogUtil::logE(TAG, {"setClassifierPath: input is empty"});
        return;
    }
    mClassifierPath = static_cast<char *>(malloc(std::strlen(path) + 1));
    std:strcpy(mClassifierPath, path);
}