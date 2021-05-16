#ifndef FLASHVIDEO_VIDEODECODER_H
#define FLASHVIDEO_VIDEODECODER_H

#include "Common.h"

#ifdef __cplusplus
extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
#include "libswresample/swresample.h"
}
#endif

#define DEFAULT_FRAME_RATE 25

class VideoDecoder {
public:
    int video_index;
    double duration;
    long frame_count;
    int frame_rate, video_buf_size;
    unsigned char* p_video_buf;

    AVFrame* p_yuv_420_frame;
    AVFrame* p_raw_frame;
    AVFormatContext* p_fmt_ctx;
    AVCodec* p_codec;
    AVCodecContext* p_codec_ctx;
    SwsContext* p_sws_ctx;

    VideoDecoder() {
        video_index = -1;
        p_video_buf = nullptr;
        p_fmt_ctx = nullptr;
        p_codec = nullptr;
        p_codec_ctx = nullptr;
        p_sws_ctx = nullptr;
    }

    ~VideoDecoder(){
        release();
    }

    void decodePacket(AVPacket *p_packet, DataYUV420* p_data);
    bool init(AVFormatContext* p_input_fmt_ctx, int input_video_index);
    void release();
private:
};

#endif
