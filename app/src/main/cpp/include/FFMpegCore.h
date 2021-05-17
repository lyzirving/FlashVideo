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
    const static int MODE_AUDIO = 0x01;
    const static int MODE_VIDEO = 0x02;

    int audio_index, video_index;
    char* src_path;
    float seek_dst;

    AVFormatContext* p_fmt_ctx;

    FFMpegCore() {
        audio_index = -1;
        video_index = -1;
        src_path = nullptr;
        p_fmt_ctx = nullptr;
    };
    ~FFMpegCore() {
        release();
    }
    bool createEnv(int mode);
    void setPath(char* path);
    void seekTo(float dst);
    void release();
private:
};

#endif
