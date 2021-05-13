#include "VideoDecoder.h"
#include "LogUtil.h"

#define TAG "VideoDecoder"

void VideoDecoder::decodePacket(AVPacket *p_packet, DataYUV420* p_data) {
    /**
     * avcodec_send_packet() will send packet to decoder in the decoding order;
     * decoding order is organized by dts, eg: IPBBPBB;
     */
    if (avcodec_send_packet(p_codec_ctx, p_packet) == 0) {
        /**
         * acquire data in decoder, and send it to raw frame in presenting order;
         * the decoder will solve the difference between dts and pts;
         * the output raw frames are organized in an order like IBBPBBP;
         */
        if(avcodec_receive_frame(p_codec_ctx, p_raw_frame) == 0) {
            p_data->width = p_codec_ctx->width;
            p_data->height = p_codec_ctx->height;
            if (p_raw_frame->format == AV_PIX_FMT_YUV420P) {
                p_data->pts = av_frame_get_best_effort_timestamp(p_raw_frame);
                p_data->y_data = p_raw_frame->data[0];
                p_data->u_data = p_raw_frame->data[1];
                p_data->v_data = p_raw_frame->data[2];
            } else {
                //transfer the raw data to yuv_420 data, and fill data in video buffer
                sws_scale(p_sws_ctx,// sws context
                          (const uint8_t *const *)p_raw_frame->data,// src slice
                          p_raw_frame->linesize,// src stride
                          0,// src slice y
                          p_codec_ctx->height,// src slice height
                          p_yuv_420_frame->data,// dst planes
                          p_yuv_420_frame->linesize// dst strides
                );
                p_data->pts = av_frame_get_best_effort_timestamp(p_yuv_420_frame);
                p_data->y_data = p_yuv_420_frame->data[0];
                p_data->u_data = p_yuv_420_frame->data[1];
                p_data->v_data = p_yuv_420_frame->data[2];
            }
        }
    }
}

double VideoDecoder::getFramePresentationTime(double pts) {
    if(pts == AV_NOPTS_VALUE)
        pts = 0;
    return pts * av_q2d(p_codec_ctx->time_base);
}

bool VideoDecoder::init(AVFormatContext *p_input_fmt_ctx, int input_video_index) {
    p_fmt_ctx = p_input_fmt_ctx;
    video_index = input_video_index;
    AVCodecParameters *p_codec_params = p_fmt_ctx->streams[video_index]->codecpar;
    p_codec = avcodec_find_decoder(p_codec_params->codec_id);
    if (p_codec == nullptr) {
        LogUtil::logE(TAG, {"init: failed to find video decoder"});
        goto fail;
    }
    p_codec_ctx = avcodec_alloc_context3(p_codec);
    if (p_codec_ctx == nullptr) {
        LogUtil::logE(TAG, {"init: failed to find video decoder context"});
        goto fail;
    }
    avcodec_parameters_to_context(p_codec_ctx, p_codec_params);
    if (avcodec_open2(p_codec_ctx, p_codec, nullptr) != 0) {
        LogUtil::logE(TAG, {"init: failed to open video source"});
        goto fail;
    }
    duration = p_fmt_ctx->duration * av_q2d(AV_TIME_BASE_Q);
    frame_count = p_fmt_ctx->streams[video_index]->nb_frames;
    frame_rate = frame_count / duration;
    if (frame_rate <= 0)
        frame_rate = DEFAULT_FRAME_RATE;

    video_buf_size = av_image_get_buffer_size(AV_PIX_FMT_YUV420P,
                                              p_codec_ctx->width,
                                              p_codec_ctx->height,
                                              1);
    p_video_buf = (unsigned char *)av_malloc(video_buf_size * sizeof(unsigned char));
    p_yuv_420_frame = av_frame_alloc();
    p_raw_frame = av_frame_alloc();

    //relate the video buffer with placeholder AVFrame
    av_image_fill_arrays(p_yuv_420_frame->data,// dst data
                         p_yuv_420_frame->linesize,// dst line size
                         p_video_buf,// src buffer
                         AV_PIX_FMT_YUV420P,// pixel format
                         p_codec_ctx->width,// width
                         p_codec_ctx->height,// height
                         1// align
    );

    p_sws_ctx = sws_getContext(
            p_codec_ctx->width,// src width
            p_codec_ctx->height,// src height
            p_codec_ctx->pix_fmt,// src format
            p_codec_ctx->width,// dst width
            p_codec_ctx->height,// dst height
            AV_PIX_FMT_YUV420P,// dst format
            SWS_BICUBIC,// flags
            nullptr,// src filter
            nullptr,// dst filter
            nullptr// param
    );

    return true;
    fail:
    release();
    return false;
}

void VideoDecoder::release() {
    LogUtil::logD(TAG, {"release"});
    if (p_yuv_420_frame != nullptr) {
        av_frame_free(&p_yuv_420_frame);
        p_yuv_420_frame = nullptr;
    }
    if (p_raw_frame != nullptr) {
        av_frame_free(&p_raw_frame);
        p_raw_frame = nullptr;
    }
    if (p_sws_ctx != nullptr) {
        sws_freeContext(p_sws_ctx);
        p_sws_ctx = nullptr;
    }
    if (p_codec_ctx != nullptr) {
        avcodec_close(p_codec_ctx);
        p_codec_ctx = nullptr;
    }
    if (p_video_buf != nullptr) {
        free(p_video_buf);
        p_video_buf = nullptr;
    }
    p_codec = nullptr;
    p_fmt_ctx = nullptr;
}

