#include "JavaCallbackUtil.h"
#include "LogUtil.h"

#define TAG "JavaCallbackUtil"

void JavaCallbackUtil::callMediaPrepare(JNIEnv* env, jobject listener, double duration, int width, int height) {
    jclass listener_class = env->GetObjectClass(listener);
    jmethodID method_id = env->GetMethodID(listener_class, "onPrepare", "(DII)V");
    env->CallVoidMethod(listener, method_id, duration, width, height);
}

void JavaCallbackUtil::callMediaStop(JNIEnv *env, jobject listener) {
    jclass listener_class = env->GetObjectClass(listener);
    jmethodID method_id = env->GetMethodID(listener_class, "onStop", "()V");
    env->CallVoidMethod(listener, method_id);
}

void JavaCallbackUtil::callMediaTickTime(JNIEnv *env, jobject listener, double current_time) {
    jclass listener_class = env->GetObjectClass(listener);
    jmethodID method_id = env->GetMethodID(listener_class, "onTickTime", "(D)V");
    env->CallVoidMethod(listener, method_id, current_time);
}

void JavaCallbackUtil::callVideoFrame(JNIEnv *env, jobject listener, int width, int height,
        unsigned char *y_data, unsigned char *u_data, unsigned char *v_data) {
    jbyteArray y = env->NewByteArray(width * height);
    env->SetByteArrayRegion(y, 0, width * height, reinterpret_cast<const jbyte *>(y_data));
    jbyteArray u = env->NewByteArray(width * height / 4);
    env->SetByteArrayRegion(u, 0, width * height / 4, reinterpret_cast<const jbyte *>(u_data));
    jbyteArray v = env->NewByteArray(width * height / 4);
    env->SetByteArrayRegion(v, 0, width * height / 4, reinterpret_cast<const jbyte *>(v_data));

    jclass listener_class = env->GetObjectClass(listener);
    jmethodID method_id = env->GetMethodID(listener_class, "onFrame", "(II[B[B[B)V");
    env->CallVoidMethod(listener, method_id, width, height, y, u, v);

    env->DeleteLocalRef(y);
    env->DeleteLocalRef(u);
    env->DeleteLocalRef(v);
}

jobject JavaCallbackUtil::findListener(std::map<jlong, jobject> *p_map, jlong key) {
    auto iterator = p_map->find(key);
    if (iterator == p_map->end()) {
        LogUtil::logE(TAG, {"findListener: failed to find listener"});
        return nullptr;
    }
    return iterator->second;
}

jobject JavaCallbackUtil::removeListener(std::map<jlong, jobject> *p_map, jlong key) {
    jobject listener = findListener(p_map, key);
    if (listener != nullptr) {
        p_map->erase(key);
    }
    return listener;
}

bool JavaCallbackUtil::threadAttachJvm(JavaVM *p_jvm, JNIEnv **pp_env) {
    if (p_jvm->GetEnv((void **) pp_env, JNI_VERSION_1_6) == JNI_EDETACHED) {
        if (p_jvm->AttachCurrentThread(pp_env, nullptr) != 0) {
            return false;
        }
    }
    return true;
}
