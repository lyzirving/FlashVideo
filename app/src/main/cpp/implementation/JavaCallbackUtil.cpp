#include "JavaCallbackUtil.h"

void JavaCallbackUtil::callMediaPrepare(JNIEnv* env, jobject listener, double duration) {
    jclass listener_class = env->GetObjectClass(listener);
    jmethodID method_id = env->GetMethodID(listener_class, "onPrepare", "(D)V");
    env->CallVoidMethod(listener, method_id, duration);
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
