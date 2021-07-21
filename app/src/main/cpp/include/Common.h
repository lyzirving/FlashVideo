#ifndef FLASHVIDEO_COMMON_H
#define FLASHVIDEO_COMMON_H

#include <jni.h>

namespace common {
    static JavaVM *sPtrGlobalVm = nullptr;
    struct Msg {
        int what;
        void* object;
    };
}

enum MediaMsg {
    MSG_IDLE = 0,
    MSG_PLAY = 1,
    MSG_PAUSE = 2,
    MSG_STOP = 3,
    MSG_SEEK = 4,
    MSG_QUIT = 5,
};

enum MediaState{
    STATE_IDLE = 1,
    STATE_INITIALIZED = 2,
    STATE_PLAY = 3,
    STATE_PAUSE = 4,
    STATE_SEEK = 5,
    STATE_STOP = 6,
    STATE_ERROR = 7,
    STATE_ABORT = 8,
};

struct DataYUV420 {
    long pts;
    unsigned char* y_data;
    unsigned char* u_data;
    unsigned char* v_data;
    unsigned char* line_size;
    int width;
    int height;
};

#endif //FLASHVIDEO_COMMON_H
