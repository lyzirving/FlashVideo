#ifndef FLASHVIDEO_AUDIODECODER_H
#define FLASHVIDEO_AUDIODECODER_H

#include "AudioData.h"

#ifdef __cplusplus
extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
#include "libswresample/swresample.h"
}
#endif

class AudioDecoder {
public:
    int audio_index;
    int in_channels, out_channels;
    int in_sample_rate, out_sample_rate;
    uint64_t in_channel_layout, out_channel_layout;
    int out_channel_count;
    AVSampleFormat in_sample_fmt, out_sample_fmt;
    unsigned char* p_audio_buffer;
    AVRational time_base;

    AVFormatContext* p_fmt_ctx;
    AVCodec* p_audio_codec;
    AVCodecContext* p_audio_codec_ctx;
    SwrContext* p_swr_ctx;
    AVFrame* p_tmp_frame;

    AudioDecoder() {
        p_audio_buffer = nullptr;
        p_fmt_ctx = nullptr;
        p_audio_codec = nullptr;
        p_audio_codec_ctx = nullptr;
        p_swr_ctx = nullptr;
        p_tmp_frame = nullptr;
    };

    ~AudioDecoder() {
        release();
    };

    bool init(AVFormatContext* in_ptr_fmt_ctx, int in_audio_index);
    AudioData* decodePacket(AVPacket* in_ptr_packet);
    void release();
private:
};

#endif
