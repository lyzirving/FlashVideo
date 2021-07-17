#include "FaceDetect.h"
#include "LogUtil.h"
#include "JavaCallbackUtil.h"

#define TAG "FaceDetect"
#define JAVA_CLASS "com/lyzirving/flashvideo/face/FaceDetector"

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
    cv::Mat mat;
    if (!ImgUtil::bitmap2Mat(env, bitmap, mat)) {
        LogUtil::logI(TAG, {"nDetect: failed to convert bitmap to matrix"});
        return false;
    }
    ptrDetect->setImgMat(mat);
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
    ImgUtil::mat2Gray(*mPtrImgMat, gray);
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

void FaceDetect::setImgMat(cv::Mat &mat) {
    mPtrImgMat = new cv::Mat(mat);
}