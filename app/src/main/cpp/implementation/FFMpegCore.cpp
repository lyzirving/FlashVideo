#include "FFMpegCore.h"
#include "LogUtil.h"

#define TAG "FFMpegCore"

bool FFMpegCore::createEnv(int mode) {
    if (src_path == nullptr || strlen(src_path) == 0) {
        LogUtil::logE(TAG, {"createEnv: src path is invalid"});
        goto fail;
    }

    av_register_all();
    avcodec_register_all();

    p_fmt_ctx = avformat_alloc_context();
    if (avformat_open_input(&p_fmt_ctx, src_path, nullptr, nullptr) != 0) {
        LogUtil::logE(TAG, {"createEnv: open media failed"});
        goto fail;
    }
    if (avformat_find_stream_info(p_fmt_ctx, nullptr) < 0) {
        LogUtil::logE(TAG, {"createEnv: could not find stream info"});
        goto fail;
    }
    for (int i = 0; i < p_fmt_ctx->nb_streams; i++) {
        if (p_fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_index = i;
        } else if (p_fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_index = i;
        }
    }
    if (-1 == audio_index) {
        LogUtil::logD(TAG, {"failed to find audio track"});
        goto fail;
    }
    if (mode == MODE_VIDEO && -1 == video_index) {
        LogUtil::logD(TAG, {"failed to find video track"});
        goto fail;
    }
    return true;
    fail:
    release();
    return false;
}

void FFMpegCore::setPath(char *path) {
    if (src_path != nullptr) {
        free(src_path);
        src_path = nullptr;
    }
    src_path = static_cast<char *>(malloc(strlen(path) + 1));
    strncpy(src_path, path, strlen(path) + 1);
    LogUtil::logD(TAG, {"setPath: ", src_path});
}

void FFMpegCore::seekTo(float dst) {
    if (dst < 0)
        dst = 0;
    else if (dst > 1)
        dst = 1;
    avformat_seek_file(p_fmt_ctx, -1, INT64_MIN,
                       p_fmt_ctx->duration * dst, INT64_MAX, 0);
}

void FFMpegCore::release() {
    LogUtil::logD(TAG, {"release"});
    if (p_fmt_ctx != nullptr) {
        avformat_close_input(&p_fmt_ctx);
        p_fmt_ctx = nullptr;
    }
    if (src_path != nullptr) {
        free(src_path);
        src_path = nullptr;
    }
}