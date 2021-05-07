#include "AudioPlayer.h"

#include "LogUtil.h"

#define TAG "AudioPlayer"

bool AudioPlayer::createBufQueuePlayer(slAndroidSimpleBufferQueueCallback ref, void *in_call_obj) {
    p_audio_engine->setBufferQueueCallback(ref, in_call_obj);
    return p_audio_engine->createBufQueuePlayer();
}

bool AudioPlayer::init(AVFormatContext *in_ptr_fmt_ctx, int in_audio_index) {
    p_audio_decoder = new AudioDecoder;
    if (!p_audio_decoder->init(in_ptr_fmt_ctx, in_audio_index)) {
        goto fail;
    }
    p_audio_engine = new AudioEngine;
    if (!p_audio_engine->init(p_audio_decoder->out_sample_rate, p_audio_decoder->out_sample_fmt,
            p_audio_decoder->in_channels)) {
        goto fail;
    }
    return true;
    fail:
    release();
    return false;
}

AudioData* AudioPlayer::decodePacket(AVPacket *in_ptr_packet) {
    return p_audio_decoder->decodePacket(in_ptr_packet);
}

void AudioPlayer::enqueueAudio(AudioData *data) {
    p_audio_engine->enqueueAudio(data->buf_size, data->data);
}

void AudioPlayer::release() {
    if (p_audio_decoder != nullptr) {
        delete p_audio_decoder;
        p_audio_decoder = nullptr;
    }
    if (p_audio_engine != nullptr) {
        delete p_audio_engine;
        p_audio_engine = nullptr;
    }
}

void AudioPlayer::setPlayState() {
    p_audio_engine->setPlayState();
}

