#ifndef FLASHVIDEO_AUDIOPLAYER_H
#define FLASHVIDEO_AUDIOPLAYER_H

#include "AudioDecoder.h"
#include "AudioEngine.h"
#include "SoundTouch.h"

class AudioPlayer {
public:
    AudioDecoder* p_audio_decoder;
    AudioEngine* p_audio_engine;
    soundtouch::SoundTouch* p_sound_touch;
    short* p_st_buf;

    AudioPlayer() {
        p_audio_decoder = nullptr;
        p_audio_engine = nullptr;
        p_sound_touch = nullptr;
        p_st_buf = nullptr;
    };

    ~AudioPlayer() {
        release();
    };

    bool createBufQueuePlayer(slAndroidSimpleBufferQueueCallback ref, void* in_call_obj);
    void clearBufferQueue();
    AudioData* decodePacket(AVPacket* in_ptr_packet);
    bool enqueueAudio(AudioData* data);
    bool enqueueAudioWithSoundTouch(AudioData* data);
    bool init(AVFormatContext* in_ptr_fmt_ctx, int in_audio_index);
    void initSoundTouch();
    void release();
    void setPlayState();
    void setPauseState();
    void setStopState();
    void setVolume(int val);
    //adjust tone
    void setPitch(double pitch);
    //adjust speed
    void setTempo(double speed);
private:
};

#endif
