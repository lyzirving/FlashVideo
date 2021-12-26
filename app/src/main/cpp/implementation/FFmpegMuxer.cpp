//
// Created by lyzirving on 2021/12/23.
//
#include "FFmpegMuxer.h"
#include "LogUtil.h"

#include <string>
#include <ctime>

#define TAG "FFmpegMuxer"
#define JAVA_CLASS "com/lyzirving/flashvideo/record/FFmpegMuxer"

static JavaVM *gJvm{nullptr};

FFmpegMuxer::FFmpegMuxer() = default;

FFmpegMuxer::~FFmpegMuxer() {
    LogUtil::logI(TAG, {"deconstruct"});
    if (mOutputFormatCtx != nullptr) {
        if (mOutputFormatCtx->pb != nullptr) {
            avio_close(mOutputFormatCtx->pb);
            mOutputFormatCtx->pb = nullptr;
        }
        if (mOutputFormatCtx->metadata != nullptr) {
            av_dict_free(&mOutputFormatCtx->metadata);
            mOutputFormatCtx->metadata = nullptr;
        }
        av_free(mOutputFormatCtx);
        mOutputFormatCtx = nullptr;
    }
}

static jlong nativeCreate(JNIEnv *env, jclass clazz) {
    if (gJvm == nullptr) { env->GetJavaVM(&gJvm); }
    return reinterpret_cast<jlong>(new FFmpegMuxer);
}

static jboolean nativePrepare(JNIEnv *env, jclass clazz, jlong ptr) {
    auto *nativePtr = reinterpret_cast<FFmpegMuxer *>(ptr);
    return nativePtr->prepare();
}

static void nativeEnqueueData(JNIEnv *env, jclass clazz, jlong ptr, jobject data,
        jint offset, jint size, jlong pts, jboolean keyFrame) {
    auto *nativePtr = reinterpret_cast<FFmpegMuxer *>(ptr);
    auto *buf = static_cast<uint8_t *>(env->GetDirectBufferAddress(data));
    nativePtr->enqueueBuffer(buf, offset, size, pts, keyFrame);
}

static void nativeStop(JNIEnv *env, jclass clazz, jlong ptr) {
    auto *nativePtr = reinterpret_cast<FFmpegMuxer *>(ptr);
    nativePtr->stop();
}

static void nativeRelease(JNIEnv *env, jclass clazz, jlong ptr) {
    auto *nativePtr = reinterpret_cast<FFmpegMuxer *>(ptr);
    delete(nativePtr);
}

static JNINativeMethod jni_methods[] = {
        {
                "nCreate",  "()J",
                (void *) nativeCreate
        },
        {
                "nPrepare", "(J)Z",
                (void *) nativePrepare
        },
        {
                "nEnqueueData", "(JLjava/nio/ByteBuffer;IIJZ)V",
                (void *) nativeEnqueueData
        },
        {
                "nStop", "(J)V",
                (void *) nativeStop
        },
        {
                "nRelease", "(J)V",
                (void *) nativeRelease
        }
};

void FFmpegMuxer::enqueueBuffer(uint8_t *data, int offset, int size, long pts, bool keyFrame) {
    if (mPacket == nullptr) { mPacket = av_packet_alloc(); }
    av_init_packet(mPacket);
    mPacket->stream_index = mOutputStreamInd;
    mPacket->size = size;
    mPacket->data = data + offset;
    if (mRecStartPts == 0) {
        mRecStartPts = pts;
        mPacket->pts = 0;
        mPacket->dts = 0;
    } else {
        int64_t dstPts = pts - mRecStartPts;
        dstPts = av_rescale_q(dstPts, AV_TIME_BASE_Q,
                              mOutputFormatCtx->streams[mOutputStreamInd]->time_base);
        mPacket->pts = dstPts;
        mPacket->dts = dstPts;
    }

    if (keyFrame) {
        LogUtil::logI(TAG, {"enqueueBuffer: packet contains key frame"});
        mPacket->flags = AV_PKT_FLAG_KEY;
    }

    int status = av_interleaved_write_frame(mOutputFormatCtx, mPacket);
    if (status < 0) {
        LogUtil::logE(TAG, {"enqueueBuffer: failed, reason = ", std::to_string(status)});
    }
    av_packet_unref(mPacket);
}

bool FFmpegMuxer::prepare() {
    std::string path("/storage/emulated/0/Android/data/com.lyzirving.flashvideo/files/video/");
    time_t now = time(nullptr);
    tm *localTimeNow = localtime(&now);
    path += std::to_string(1900 + localTimeNow->tm_year) + "_" +
            std::to_string(localTimeNow->tm_mon) + "_" +
            std::to_string(localTimeNow->tm_mday) + "_" +
            std::to_string(localTimeNow->tm_hour) + "_" +
            std::to_string(localTimeNow->tm_min) + "_" +
            std::to_string(localTimeNow->tm_sec) + ".ts";

    AVOutputFormat *outputFormat{nullptr};
    AVStream *stream{nullptr};
    AVDictionary *opts{nullptr};
    int status{0};

    mOutputFormatCtx = avformat_alloc_context();
    if (!mOutputFormatCtx) {
        LogUtil::logE(TAG, {"prepare: failed to alloc context"});
        goto fail;
    }
    outputFormat = av_guess_format(nullptr, "test.ts", nullptr);
    if (outputFormat == nullptr) {
        LogUtil::logE(TAG, {"prepare: failed to guess output format"});
        goto fail;
    }
    mOutputFormatCtx->oformat = outputFormat;
    LogUtil::logI(TAG, {"prepare: output format, name = ", outputFormat->name,
                        ", mime = ", outputFormat->mime_type,
                        ", extension = ", outputFormat->extensions});
    stream = avformat_new_stream(mOutputFormatCtx, nullptr);
    if (stream == nullptr) {
        LogUtil::logE(TAG, {"prepare: failed to create new stream"});
        goto fail;
    }
    stream->codecpar->codec_id = AV_CODEC_ID_H264;
    stream->codecpar->format = AV_PIX_FMT_RGBA;
    stream->codecpar->codec_type = AVMEDIA_TYPE_VIDEO;
    stream->codecpar->codec_tag = av_codec_get_tag(mOutputFormatCtx->oformat->codec_tag, AV_CODEC_ID_H264);
    stream->codecpar->width = 1080;
    stream->codecpar->height = 1200;
    stream->codecpar->bit_rate = 4 * 1024 * 1024;
    stream->time_base.num = 1;
    stream->time_base.den = 25;
    mOutputStreamInd = stream->index;
    LogUtil::logI(TAG, {"prepare: new stream index = ", std::to_string(mOutputStreamInd)});
    if (mOutputFormatCtx->oformat->flags & AVFMT_GLOBALHEADER) {
        LogUtil::logI(TAG, {"%prepare: output format needs global header"});
        mOutputFormatCtx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
    }

    status = avio_open2(&mOutputFormatCtx->pb, path.c_str(), AVIO_FLAG_WRITE, nullptr, nullptr);
    if (status < 0) {
        LogUtil::logE(TAG, {"prepare: failed to open output file, reason = ", std::to_string(status)});
        goto fail;
    }

    av_dict_set(&opts, "movflags", "faststart", 0);
    av_dict_set_int(&opts, "video_track_timescale", 25, 0);
    status = avformat_write_header(mOutputFormatCtx, &opts);
    av_dict_free(&opts);
    if (status < 0) {
        LogUtil::logE(TAG, {"prepare: failed to write header, reason = ", std::to_string(status)});
        goto fail;
    }
    LogUtil::logI(TAG, {"prepare: success"});
    return true;
    fail:
    return false;
}

bool FFmpegMuxer::registerSelf(JNIEnv *env) {
    int count = sizeof(jni_methods) / sizeof(jni_methods[0]);
    jclass javaClass = env->FindClass(JAVA_CLASS);
    if (!javaClass) {
        LogUtil::logE(TAG, {"registerSelf: failed to find class ", JAVA_CLASS});
        goto fail;
    }
    if (env->RegisterNatives(javaClass, jni_methods, count) < 0) {
        LogUtil::logE(TAG, {"registerSelf: failed to register native methods ", JAVA_CLASS});
        goto fail;
    }
    LogUtil::logD(TAG, {"success to register class: ", JAVA_CLASS, ", method count ",
                        std::to_string(count)});
    return true;
    fail:
    return false;
}

void FFmpegMuxer::stop() {
    mRecStartPts = 0;
    if (mOutputFormatCtx != nullptr) {
        int status = av_write_trailer(mOutputFormatCtx);
        if (status != 0) {
            LogUtil::logE(TAG, {"stop: error = ", std::to_string(status)});
        }
    } else {
        LogUtil::logE(TAG, {"stop: avFormatContext is nullptr"});
    }
}



