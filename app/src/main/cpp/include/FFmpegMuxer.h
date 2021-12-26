//
// Created by lyzirving on 2021/12/23.
//
#ifndef FLASHVIDEO_FFMPEGMUXER_H
#define FLASHVIDEO_FFMPEGMUXER_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavcodec/avcodec.h>

#ifdef __cplusplus
}
#endif

class FFmpegMuxer {
private:
    AVFormatContext* mOutputFormatCtx{nullptr};
    AVPacket* mPacket{nullptr};
    int mOutputStreamInd{-1};
    int64_t mRecStartPts{0};

public:
    static bool registerSelf(JNIEnv *env);

    FFmpegMuxer();

    ~FFmpegMuxer();

    void enqueueBuffer(uint8_t* data, int offset, int size, long pts, bool keyFrame);
    bool prepare();
    void stop();

};

#endif //FLASHVIDEO_FFMPEGMUXER_H
