#ifndef FLASHVIDEO_AUDIODATA_H
#define FLASHVIDEO_AUDIODATA_H

#include <malloc.h>

class AudioData {
public:
    int buf_size;
    double now_time;
    unsigned char* data;

    AudioData() {
        buf_size = -1;
        now_time = -1;
        data = nullptr;
    }
    ~AudioData() {
        if (data != nullptr) {
            free(data);
            data = nullptr;
        }
    };
private:
};
#endif
