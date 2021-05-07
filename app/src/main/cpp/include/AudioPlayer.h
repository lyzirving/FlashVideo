#ifndef FLASHVIDEO_AUDIOPLAYER_H
#define FLASHVIDEO_AUDIOPLAYER_H


#include "AudioDecoder.h"
#include "AudioEngine.h"

class AudioPlayer {
public:
    AudioDecoder* p_audio_decoder;
    AudioEngine* p_audio_engine;

    AudioPlayer() {
        p_audio_decoder = nullptr;
        p_audio_engine = nullptr;
    };

    ~AudioPlayer() {
        release();
    };

    bool init(AVFormatContext* in_ptr_fmt_ctx, int in_audio_index);
    bool createBufQueuePlayer(slAndroidSimpleBufferQueueCallback ref, void* in_call_obj);
    AudioData* decodePacket(AVPacket* in_ptr_packet);
    void enqueueAudio(AudioData* data);
    void release();
    void setPlayState();
    void setPauseState();
    void setStopState();
private:
};

#endif
