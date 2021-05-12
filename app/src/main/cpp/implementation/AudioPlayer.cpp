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
            p_audio_decoder->in_channel_count)) {
        goto fail;
    }
    return true;
    fail:
    release();
    return false;
}

void AudioPlayer::initSoundTouch() {
    if(p_sound_touch == nullptr) {
        p_sound_touch = new soundtouch::SoundTouch;
        p_sound_touch->setSampleRate(p_audio_decoder->out_sample_rate);
        p_sound_touch->setChannels(p_audio_decoder->out_channel_count);
        p_st_buf = static_cast<short *>(malloc(p_audio_decoder->out_sample_rate * 2 * p_audio_decoder->out_channel_count));
        p_sound_touch->setPitch(1);
        p_sound_touch->setTempo(1);
    }
}

AudioData* AudioPlayer::decodePacket(AVPacket *in_ptr_packet) {
    return p_audio_decoder->decodePacket(in_ptr_packet);
}

bool AudioPlayer::enqueueAudio(AudioData *data) {
    for (int i = 0; i < data->buf_size / 2 + 1; ++i) {
        p_st_buf[i] = (data->data[2 * i] | ((data->data[2 * i + 1]) << 8));
    }
    p_sound_touch->putSamples(p_st_buf, data->buf_size / 4);
    int sample_num = p_sound_touch->receiveSamples(p_st_buf, p_audio_decoder->out_sample_rate * 4);
    if (sample_num > 0) {
        p_audio_engine->enqueueAudio(sample_num * 4, reinterpret_cast<unsigned char *>(p_st_buf));
        return true;
    } else {
        return false;
    }
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
    if(p_sound_touch != nullptr) {
        delete p_sound_touch;
        p_sound_touch = nullptr;
    }
    if (p_st_buf != nullptr) {
        free(p_st_buf);
        p_st_buf = nullptr;
    }
}

void AudioPlayer::setPlayState() {
    p_audio_engine->setPlayState();
}

void AudioPlayer::setPauseState() {
    p_audio_engine->setPauseState();
}

void AudioPlayer::setStopState(){
    p_audio_engine->setStopState();
}

void AudioPlayer::setVolume(int val) {
    p_audio_engine->setVolume(val);
}

void AudioPlayer::setPitch(double pitch) {
    if (p_sound_touch != nullptr)
        p_sound_touch->setPitch(pitch);
}

void AudioPlayer::setTempo(double speed) {
    if (p_sound_touch != nullptr)
        p_sound_touch->setTempo(speed);
}

