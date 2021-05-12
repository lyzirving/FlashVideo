#ifndef FLASHVIDEO_VIDEOCONTROLLER_H
#define FLASHVIDEO_VIDEOCONTROLLER_H

#include <pthread.h>
#include <queue>

#include "Common.h"
#include "JavaCallbackUtil.h"
#include "FFMpegCore.h"
#include "AudioPlayer.h"

class VideoController {
public:
    VideoController() {
        p_ffmpeg = nullptr;
        p_audio = nullptr;

        media_state = STATE_IDLE;

        p_video_packet_queue = new std::queue<AVPacket>();
        p_audio_packet_queue = new std::queue<AVPacket>();
        p_main_evt_queue = new std::queue<Msg>();
        p_audio_evt_queue = new std::queue<Msg>();
        p_video_evt_queue = new std::queue<Msg>();

        pthread_mutex_init(&main_evt_mutex_lock, nullptr);
        pthread_cond_init(&main_evt_cond_lock, nullptr);
        pthread_mutex_init(&audio_evt_mutex_lock, nullptr);
        pthread_cond_init(&audio_evt_cond_lock, nullptr);
        pthread_mutex_init(&audio_packet_mutex_lock, nullptr);
        pthread_cond_init(&audio_packet_cond_lock, nullptr);
        pthread_mutex_init(&video_evt_mutex_lock, nullptr);
        pthread_cond_init(&video_evt_cond_lock, nullptr);
        pthread_mutex_init(&video_packet_mutex_lock, nullptr);
        pthread_cond_init(&video_packet_cond_lock, nullptr);
    }

    ~VideoController() {
        pthread_mutex_lock(&audio_packet_mutex_lock);
        if (p_audio_packet_queue != nullptr)
            delete p_audio_packet_queue;
        p_audio_packet_queue = nullptr;
        pthread_mutex_unlock(&audio_packet_mutex_lock);

        pthread_mutex_lock(&video_packet_mutex_lock);
        if (p_video_packet_queue != nullptr)
            delete p_video_packet_queue;
        p_video_packet_queue = nullptr;
        pthread_mutex_unlock(&video_packet_mutex_lock);

        delete p_main_evt_queue;
        delete p_audio_evt_queue;
        delete p_video_evt_queue;

        pthread_mutex_destroy(&main_evt_mutex_lock);
        pthread_cond_destroy(&main_evt_cond_lock);
        pthread_mutex_destroy(&audio_evt_mutex_lock);
        pthread_cond_destroy(&audio_evt_cond_lock);
        pthread_mutex_destroy(&audio_packet_mutex_lock);
        pthread_cond_destroy(&audio_packet_cond_lock);
        pthread_mutex_destroy(&video_evt_mutex_lock);
        pthread_cond_destroy(&video_evt_cond_lock);
        pthread_mutex_destroy(&video_packet_mutex_lock);
        pthread_cond_destroy(&video_packet_cond_lock);

        if (p_ffmpeg != nullptr) {
            delete p_ffmpeg;
            p_ffmpeg = nullptr;
        }
        if (p_audio != nullptr) {
            delete p_audio;
            p_audio = nullptr;
        }
    }

private:
    double main_clock, last_main_clock;
    MediaState media_state;
    std::queue<Msg>* p_main_evt_queue;
    std::queue<Msg>* p_audio_evt_queue;
    std::queue<Msg>* p_video_evt_queue;

    std::queue<AVPacket>* p_audio_packet_queue;
    std::queue<AVPacket>* p_video_packet_queue;

    FFMpegCore* p_ffmpeg;
    AudioPlayer* p_audio;

    pthread_mutex_t main_evt_mutex_lock;
    pthread_cond_t main_evt_cond_lock;

    pthread_mutex_t audio_evt_mutex_lock;
    pthread_cond_t audio_evt_cond_lock;
    pthread_mutex_t audio_packet_mutex_lock;
    pthread_cond_t audio_packet_cond_lock;

    pthread_mutex_t video_evt_mutex_lock;
    pthread_cond_t video_evt_cond_lock;
    pthread_mutex_t video_packet_mutex_lock;
    pthread_cond_t video_packet_cond_lock;
};

#endif
