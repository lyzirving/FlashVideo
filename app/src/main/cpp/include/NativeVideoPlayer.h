#ifndef FLASHVIDEO_NATIVEVIDEOPLAYER_H
#define FLASHVIDEO_NATIVEVIDEOPLAYER_H

#include <jni.h>
#include <pthread.h>
#include <queue>

#include "FFMpegCore.h"
#include "AudioPlayer.h"

class NativeVideoPlayer {
public:
    enum MediaMsg{
        MSG_PLAY = 1,
        MSG_PAUSE = 2,
        MSG_STOP = 3,
        MSG_SEEK = 4,
        MSG_QUIT = 5,
    };
    struct Msg {
        MediaMsg what;
    };
    enum MediaState{
        STATE_IDLE = 1,
        STATE_INITIALIZED = 2,
        STATE_PLAY = 3,
        STATE_PAUSE = 4,
        STATE_STOP = 5,
        STATE_ERROR = 6,
    };

    NativeVideoPlayer() {
        p_ffmpeg_core = nullptr;
        p_audio = nullptr;
        p_audio_packet_queue = new std::queue<AVPacket>();
        p_msg_queue = new std::queue<Msg>();
        p_audio_msg_queue = new std::queue<Msg>();
        media_state = STATE_IDLE;
        main_clock = 0;

        pthread_mutex_init(&main_evt_mutex_lock, nullptr);
        pthread_cond_init(&main_evt_cond_lock, nullptr);
        pthread_mutex_init(&audio_evt_mutex_lock, nullptr);
        pthread_cond_init(&audio_evt_cond_lock, nullptr);
        pthread_mutex_init(&audio_packet_mutex_lock, nullptr);
        pthread_cond_init(&audio_packet_cond_lock, nullptr);
    }
    ~NativeVideoPlayer() {
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
    static bool registerSelf(JNIEnv *env);
    static bool threadAttachJvm(JavaVM* ptrJavaVm,JNIEnv** ppEnv);
    void dealAudioLoop();
    void dealAudioBufferQueueCallback();
    void dealMainEvtLoop();
    void dealPacketCollector();
    bool init();
    void handlePlay();
    void setPath(char* path);
private:
    double main_clock;
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
