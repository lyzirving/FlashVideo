#include <jni.h>

#include "JavaCallbackUtil.h"
#include "AudioController.h"
#include "VideoManager.h"
#include "LogUtil.h"

#define TAG "native-lib"

JNIEXPORT int JNICALL JNI_OnLoad(JavaVM *vm,void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env,JNI_VERSION_1_6) != JNI_OK) {
        LogUtil::logD(TAG, {"JNI_OnLoad: failed to get env"});
        return JNI_ERR;
    }
    if (!AudioController::registerSelf(env)) {
        return JNI_ERR;
    }
    if (!VideoManager::registerSelf(env)) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}
