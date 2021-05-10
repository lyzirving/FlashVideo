#ifndef FLASHVIDEO_FFMPEGCORE_H
#define FLASHVIDEO_FFMPEGCORE_H

#ifdef __cplusplus
extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
#include "libswresample/swresample.h"
}
#endif

class FFMpegCore {
public:
    int audio_index;
    char* src_path;
    AVFormatContext* p_fmt_ctx;
    float seek_dst;

    FFMpegCore() {
        audio_index = -1;
        src_path = nullptr;
        p_fmt_ctx = nullptr;
    };
    ~FFMpegCore() {
        release();
    }
    bool createEnv();
    void setPath(char* path);
    void release();
private:
};

#endif
