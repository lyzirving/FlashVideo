#include "AudioDecoder.h"

#include "LogUtil.h"

#define TAG "AudioDecoder"

bool AudioDecoder::init(AVFormatContext *in_ptr_fmt_ctx, int in_audio_index) {
    p_fmt_ctx = in_ptr_fmt_ctx;
    audio_index = in_audio_index;
    AVCodecParameters *tmp_p_codec_params = p_fmt_ctx->streams[audio_index]->codecpar;
    time_base = p_fmt_ctx->streams[audio_index]->time_base;
    p_audio_codec = avcodec_find_decoder(tmp_p_codec_params->codec_id);
    if (nullptr == p_audio_codec) {
        LogUtil::logE(TAG, {"init: could not find audio codec"});
        goto fail;
    }
    p_audio_codec_ctx = avcodec_alloc_context3(p_audio_codec);
    if (nullptr == p_audio_codec_ctx) {
        LogUtil::logE(TAG, {"init: failed to find audio codec context"});
        goto fail;
    }
    avcodec_parameters_to_context(p_audio_codec_ctx, tmp_p_codec_params);
    if (avcodec_open2(p_audio_codec_ctx, p_audio_codec, nullptr) != 0) {
        LogUtil::logE(TAG, {"init: failed to open audio codec"});
        goto fail;
    }
    p_swr_ctx = swr_alloc();
    in_sample_fmt = p_audio_codec_ctx->sample_fmt;
    out_sample_fmt = AV_SAMPLE_FMT_S16;
    in_channels = p_audio_codec_ctx->channels;
    in_sample_rate = p_audio_codec_ctx->sample_rate;
    out_sample_rate = 44100;
    in_channel_layout = p_audio_codec_ctx->channel_layout;
    out_channel_layout = AV_CH_LAYOUT_STEREO;
    out_channel_count = av_get_channel_layout_nb_channels(out_channel_layout);
    swr_alloc_set_opts(p_swr_ctx, out_channel_layout, out_sample_fmt, out_sample_rate,
                       in_channel_layout, in_sample_fmt, in_sample_rate, 0, nullptr);
    swr_init(p_swr_ctx);
    p_audio_buffer = (unsigned char*) av_malloc(out_channel_count * out_sample_rate);
    media_time = p_fmt_ctx->duration * av_q2d(AV_TIME_BASE_Q);
    return true;
    fail:
    release();
    return false;
}

AudioData* AudioDecoder::decodePacket(AVPacket *in_ptr_packet) {
    auto* p_result = new AudioData;
    if (avcodec_send_packet(p_audio_codec_ctx, in_ptr_packet) == 0) {
        if (p_tmp_frame == nullptr) p_tmp_frame = av_frame_alloc();
        if (avcodec_receive_frame(p_audio_codec_ctx, p_tmp_frame) == 0) {
            //fill buffer from decoded frame
            swr_convert(p_swr_ctx, &p_audio_buffer,
                        out_sample_rate * out_channel_count,
                        (const uint8_t **) p_tmp_frame->data, p_tmp_frame->nb_samples);
            p_result->buf_size = av_samples_get_buffer_size(nullptr, out_channel_count,
                    p_tmp_frame->nb_samples, AV_SAMPLE_FMT_S16, 1);
            p_result->now_time= p_tmp_frame->pts * av_q2d(time_base);
            p_result->data = static_cast<unsigned char *>(malloc(p_result->buf_size));
            memcpy(p_result->data, p_audio_buffer, p_result->buf_size);
            av_frame_unref(p_tmp_frame);
        }
    }
    return p_result;
}

void AudioDecoder::release() {
    if (p_audio_buffer != nullptr) {
        av_free(p_audio_buffer);
        p_audio_buffer = nullptr;
    }
    if (p_swr_ctx != nullptr) {
        swr_free(&p_swr_ctx);
        p_swr_ctx = nullptr;
    }
    if (p_audio_codec_ctx != nullptr) {
        avcodec_close(p_audio_codec_ctx);
        p_audio_codec_ctx = nullptr;
    }
    if (p_tmp_frame != nullptr) {
        av_frame_free(&p_tmp_frame);
        p_tmp_frame = nullptr;
    }
    p_audio_codec = nullptr;
    p_fmt_ctx = nullptr;
}

