#ifndef FLASHVIDEO_AUDIOCONTROLLER_H
#define FLASHVIDEO_AUDIOCONTROLLER_H

#include <pthread.h>
#include <queue>

#include "Common.h"
#include "JavaCallbackUtil.h"
#include "FFMpegCore.h"
#include "AudioPlayer.h"

class AudioController {
public:
    AudioController() {
        p_ffmpeg_core = nullptr;
        p_audio = nullptr;

        p_audio_packet_queue = new std::queue<AVPacket>();
        p_msg_queue = new std::queue<common::Msg>();
        p_audio_msg_queue = new std::queue<common::Msg>();

        media_state = STATE_IDLE;
        main_clock = 0;
        last_main_clock = 0;

        pthread_mutex_init(&main_evt_mutex_lock, nullptr);
        pthread_cond_init(&main_evt_cond_lock, nullptr);
        pthread_mutex_init(&audio_evt_mutex_lock, nullptr);
        pthread_cond_init(&audio_evt_cond_lock, nullptr);
        pthread_mutex_init(&audio_packet_mutex_lock, nullptr);
        pthread_cond_init(&audio_packet_cond_lock, nullptr);
    }
    ~AudioController() {}

    static bool registerSelf(JNIEnv *env);
    void dealAudioLoop(JNIEnv* env);
    void dealAudioBufferQueueCallbackWithSoundTouch();
    void dealMainEvtLoop(JNIEnv* env);
    void dealPacketCollector();
    bool init();
    void handlePlay();
    void handlePause();
    void handleSeek(float seek_dst);
    void handleSetPitch(double pitch);
    void handleSetTempo(double pitch);
    void handleStop();
    void handleSetVolume(int volume);
    void setPath(char* path);
    void seekToDst(float dst_ratio);
private:
    double main_clock, last_main_clock;
    FFMpegCore* p_ffmpeg_core;
    AudioPlayer* p_audio;
    MediaState media_state;
    std::queue<AVPacket>* p_audio_packet_queue;
    std::queue<common::Msg>* p_msg_queue;
    std::queue<common::Msg>* p_audio_msg_queue;

    pthread_mutex_t main_evt_mutex_lock;
    pthread_cond_t main_evt_cond_lock;

    pthread_mutex_t audio_evt_mutex_lock;
    pthread_cond_t audio_evt_cond_lock;

    pthread_mutex_t audio_packet_mutex_lock;
    pthread_cond_t audio_packet_cond_lock;
};

#endif
