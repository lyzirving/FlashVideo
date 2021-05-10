#ifndef FLASHVIDEO_JAVACALLBACKUTIL_H
#define FLASHVIDEO_JAVACALLBACKUTIL_H

#include <jni.h>

class JavaCallbackUtil {
public:
    static void callMediaPrepare(JNIEnv* env, jobject listener, double duration);
    static void callMediaStop(JNIEnv* env, jobject listener);
    static void callMediaTickTime(JNIEnv* env, jobject listener, double current_time);
private:
};

#endif //FLASHVIDEO_JAVACALLBACKUTIL_H
