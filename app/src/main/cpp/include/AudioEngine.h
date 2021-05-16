#ifndef FLASHVIDEO_AUDIOENGINE_H
#define FLASHVIDEO_AUDIOENGINE_H

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

class AudioEngine {
public:
    SLuint32 sample_rate;
    SLuint32 sample_fmt;
    SLuint32 channel_count;
    SLObjectItf p_engine_obj;
    SLEngineItf p_engine_itf;
    SLObjectItf p_output_mix_obj;
    SLEnvironmentalReverbItf p_output_mix_env_reverb_itf;
    SLObjectItf p_audio_player_obj;
    SLPlayItf p_audio_player_itf;
    SLAndroidSimpleBufferQueueItf p_buf_queue_itf;
    SLVolumeItf p_volume_itf;

    slAndroidSimpleBufferQueueCallback p_buf_queue_callback;
    void* p_callback_obj;

    AudioEngine() {
        p_engine_obj = nullptr;
        p_engine_itf = nullptr;
        p_output_mix_obj = nullptr;
        p_output_mix_env_reverb_itf = nullptr;
        p_audio_player_obj = nullptr;
        p_audio_player_itf = nullptr;
        p_buf_queue_itf = nullptr;
        p_volume_itf = nullptr;
    };

    ~AudioEngine() {
        release();
    };

    bool init(SLuint32 in_sample_rate, SLuint32 in_sample_fmt, SLuint32 in_channel_count);
    bool createBufQueuePlayer();
    bool enqueueAudio(int length, unsigned char* data);
    void setBufferQueueCallback(slAndroidSimpleBufferQueueCallback ref, void* in_call_obj);
    void setPlayState();
    void setPauseState();
    void setStopState();
    void setVolume(int val);
    void release();
private:
    static SLuint32 getBitsPerSample(SLuint32 in_sample_fmt);
    static SLuint32 getChannelMask(SLuint32 channels);
    static SLuint32 getSamplesPerSec(SLuint32 in_sample_rate);
};

#endif
