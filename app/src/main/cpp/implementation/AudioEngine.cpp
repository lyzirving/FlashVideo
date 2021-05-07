#include "AudioEngine.h"

#include "LogUtil.h"

#define TAG "AudioEngine"

SLuint32 AudioEngine::getBitsPerSample(SLuint32 sampleFormat) {
    if (sampleFormat == 8) {
        return SL_PCMSAMPLEFORMAT_FIXED_8;
    } else {
        return SL_PCMSAMPLEFORMAT_FIXED_16;
    }
}

SLuint32 AudioEngine::getChannelMask(SLuint32 channels) {
    if (channels == 2) {
        return SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    }
    return SL_SPEAKER_FRONT_CENTER;
}

SLuint32 AudioEngine::getSamplesPerSec(SLuint32 sampleRate) {
    if (sampleRate == 8000) {
        return SL_SAMPLINGRATE_8;
    } else if (sampleRate == 12000) {
        return SL_SAMPLINGRATE_12;
    } else if (sampleRate == 16000) {
        return SL_SAMPLINGRATE_16;
    } else {
        return SL_SAMPLINGRATE_44_1;
    }
}

bool AudioEngine::createBufQueuePlayer() {
    // configure audio source
    SLDataLocator_AndroidSimpleBufferQueue buffer_queue_allocator = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM pcm_fmt = {SL_DATAFORMAT_PCM,
                                channel_count,
                                sample_rate,
                                sample_fmt,
                                sample_fmt,
                                getChannelMask(channel_count),
                                SL_BYTEORDER_LITTLEENDIAN};
    SLDataSource audio_src = {&buffer_queue_allocator, &pcm_fmt};
    // configure audio sink
    SLDataLocator_OutputMix out_put_mix_locator = {SL_DATALOCATOR_OUTPUTMIX, p_output_mix_obj};
    SLDataSink audio_output = {&out_put_mix_locator, NULL};

    const SLInterfaceID audio_ids[3] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME, SL_IID_EFFECTSEND};
    const SLboolean req_audio[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE,
            /*SL_BOOLEAN_TRUE,*/ };
    if ((*p_engine_itf)->CreateAudioPlayer(p_engine_itf, &p_audio_player_obj, &audio_src, &audio_output,
                                           3, audio_ids, req_audio) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"createBufQueuePlayer: failed to create player object"});
        goto fail;
    }
    if ((*p_audio_player_obj)->Realize(p_audio_player_obj, SL_BOOLEAN_FALSE) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"createBufQueuePlayer: failed to realize player object"});
        goto fail;
    }
    //get the player interface
    if ((*p_audio_player_obj)->GetInterface(p_audio_player_obj, SL_IID_PLAY, &p_audio_player_itf) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"createBufQueuePlayer: failed to get player interface"});
        goto fail;
    }
    //get the buffer queue interface
    if ((*p_audio_player_obj)->GetInterface(p_audio_player_obj, SL_IID_BUFFERQUEUE, &p_buf_queue_itf) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"createBufQueuePlayer: failed to get buffer queue interface"});
        goto fail;
    }
    //get the volume interface
    if ((*p_audio_player_obj)->GetInterface(p_audio_player_obj, SL_IID_VOLUME, &p_volume_itf) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"createBufQueuePlayer: failed to get volume interface"});
        goto fail;
    }
    if ((*p_buf_queue_itf)->RegisterCallback(p_buf_queue_itf, p_buf_queue_callback, p_callback_obj) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"createBufQueuePlayer: failed to register buffer queue callback"});
        goto fail;
    }
    return true;
    fail:
    release();
    return false;
}

void AudioEngine::enqueueAudio(int length, unsigned char* data) {
    (*p_buf_queue_itf)->Enqueue(p_buf_queue_itf, data, length);
}

bool AudioEngine::init(SLuint32 in_sample_rate, SLuint32 in_sample_fmt, SLuint32 in_channel_count) {
    //effect of environmental reverb
    SLInterfaceID effect_env_reverb[1] = {SL_IID_ENVIRONMENTALREVERB};
    SLboolean control_effect_env_reverb[1] = {SL_BOOLEAN_FALSE};
    SLEnvironmentalReverbSettings reverb_setting = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;

    if (slCreateEngine(&p_engine_obj, 0, nullptr,
            0,nullptr, nullptr) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"init: failed to create engine object"});
        goto fail;
    }
    if ((*p_engine_obj)->Realize(p_engine_obj, SL_BOOLEAN_FALSE) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"init: failed to realize engine object"});
        goto fail;
    }
    // get the engine interface, which is needed in order to create other objects
    if ((*p_engine_obj)->GetInterface(p_engine_obj, SL_IID_ENGINE, &p_engine_itf) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"init: failed to get engine interface"});
        goto fail;
    }
    //create the mixer
    if ((*p_engine_itf)->CreateOutputMix(p_engine_itf, &p_output_mix_obj, 1, effect_env_reverb,
            control_effect_env_reverb) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"init: failed to get output mix interface"});
        goto fail;
    }
    if ((*p_output_mix_obj)->Realize(p_output_mix_obj, SL_BOOLEAN_FALSE) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"init: failed to realize output mix interface"});
        goto fail;
    }
    // get the environmental reverb interface
    // this could fail if the environmental reverb effect is not available,
    // either because the feature is not present, excessive CPU load, or
    // the required MODIFY_AUDIO_SETTINGS permission was not requested and granted
    if ((*p_output_mix_obj)->GetInterface(p_output_mix_obj, SL_IID_ENVIRONMENTALREVERB,
            &p_output_mix_env_reverb_itf) == SL_RESULT_SUCCESS) {
        (*p_output_mix_env_reverb_itf)->SetEnvironmentalReverbProperties(p_output_mix_env_reverb_itf, &reverb_setting);
    }
    sample_rate = getSamplesPerSec(in_sample_rate);
    sample_fmt = getBitsPerSample(in_sample_fmt);
    channel_count = in_channel_count;
    return true;
    fail:
    release();
    return false;
}

void AudioEngine::setBufferQueueCallback(slAndroidSimpleBufferQueueCallback ref, void* in_call_obj) {
    p_buf_queue_callback = ref;
    p_callback_obj = in_call_obj;
}

void AudioEngine::setPlayState() {
    if ((*p_audio_player_itf)->SetPlayState(p_audio_player_itf, SL_PLAYSTATE_PLAYING) != SL_RESULT_SUCCESS) {
        LogUtil::logE(TAG, {"setPlayState: failed"});
    }
}

void AudioEngine::release() {
    if (p_audio_player_obj != nullptr) {
        (*p_audio_player_obj)->Destroy(p_audio_player_obj);
        p_audio_player_obj = nullptr;
    }
    if (p_output_mix_obj != nullptr) {
        (*p_output_mix_obj)->Destroy(p_output_mix_obj);
        p_output_mix_obj = nullptr;
    }
    // destroy engine object, and invalidate all associated interfaces
    if (p_engine_obj != nullptr) {
        (*p_engine_obj)->Destroy(p_engine_obj);
        p_engine_obj = nullptr;
    }
    p_output_mix_env_reverb_itf = nullptr;
    p_engine_itf = nullptr;
    p_audio_player_itf = nullptr;
    p_buf_queue_itf = nullptr;
    p_volume_itf = nullptr;
}

