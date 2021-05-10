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
        p_msg_queue = new std::queue<Msg>();
        p_audio_msg_queue = new std::queue<Msg>();
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
    ~AudioController() {
        if (p_ffmpeg_core != nullptr) {
            delete p_ffmpeg_core;
            p_ffmpeg_core = nullptr;
        }
        if (p_audio != nullptr) {
            delete p_audio;
            p_audio = nullptr;
        }
        delete p_audio_packet_queue;
        delete p_audio_msg_queue;
        delete p_msg_queue;

        pthread_mutex_destroy(&main_evt_mutex_lock);
        pthread_cond_destroy(&main_evt_cond_lock);
        pthread_mutex_destroy(&audio_evt_mutex_lock);
        pthread_cond_destroy(&audio_evt_cond_lock);
        pthread_mutex_destroy(&audio_packet_mutex_lock);
        pthread_cond_destroy(&audio_packet_cond_lock);
    }
    static jobject findListener(jlong pointer);
    static jobject removeListener(jlong pointer);
    static bool registerSelf(JNIEnv *env);
    static bool threadAttachJvm(JavaVM* ptrJavaVm,JNIEnv** ppEnv);
    void dealAudioLoop(JNIEnv* env);
    void dealAudioBufferQueueCallback();
    void dealMainEvtLoop(JNIEnv* env);
    void dealPacketCollector();
    bool init();
    void handlePlay();
    void handlePause();
    void handleSeek(float seek_dst);
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
    std::queue<Msg>* p_msg_queue;
    std::queue<Msg>* p_audio_msg_queue;

    pthread_mutex_t main_evt_mutex_lock;
    pthread_cond_t main_evt_cond_lock;

    pthread_mutex_t audio_evt_mutex_lock;
    pthread_cond_t audio_evt_cond_lock;

    pthread_mutex_t audio_packet_mutex_lock;
    pthread_cond_t audio_packet_cond_lock;
};

#endif
