#ifndef FLASHVIDEO_JAVACALLBACKUTIL_H
#define FLASHVIDEO_JAVACALLBACKUTIL_H

#include <jni.h>
#include <map>

class JavaCallbackUtil {
public:
    static void callMediaPrepare(JNIEnv* env, jobject listener, double duration, int width, int height);
    static void callMediaStop(JNIEnv* env, jobject listener);
    static void callMediaTickTime(JNIEnv* env, jobject listener, double current_time);
    static void callVideoFrame(JNIEnv* env, jobject listener, int width, int height, unsigned char* y_data, unsigned char* u_data, unsigned char* v_data);
    static jobject findListener(std::map<jlong, jobject>* p_map, jlong key);
    static jobject removeListener(std::map<jlong, jobject>* p_map, jlong key);
    static bool threadAttachJvm(JavaVM *p_jvm, JNIEnv **pp_env);
private:
};

#endif //FLASHVIDEO_JAVACALLBACKUTIL_H
