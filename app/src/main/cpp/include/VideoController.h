#ifndef FLASHVIDEO_VIDEOCONTROLLER_H
#define FLASHVIDEO_VIDEOCONTROLLER_H

#include <pthread.h>
#include <queue>
#include <unistd.h>

#include "Common.h"
#include "JavaCallbackUtil.h"
#include "FFMpegCore.h"
#include "AudioPlayer.h"
#include "VideoDecoder.h"

#define TEN_SEC 10
#define FIVE_HUNDRED_MILL_SEC 0.5
#define FORTY_MILL_SEC 0.04
#define THREE_MILL_SEC 0.003
#define ONE_MIC0_SEC 1000000

class VideoController {

public:
    VideoController() {
        p_ffmpeg_core = nullptr;
        p_audio = nullptr;
        p_video_decoder = nullptr;
        media_state = STATE_IDLE;

        video_clock = 0;
        main_clock = 0;
        last_main_clock = 0;
        last_video_pts = 0;

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
        pthread_mutex_init(&audio_buffer_queue_mutex_lock, nullptr);
        pthread_mutex_init(&video_evt_mutex_lock, nullptr);
        pthread_cond_init(&video_evt_cond_lock, nullptr);
        pthread_mutex_init(&video_packet_mutex_lock, nullptr);
        pthread_cond_init(&video_packet_cond_lock, nullptr);
    }

    ~VideoController() {}

    static bool registerSelf(JNIEnv *env);
    void audioEventEnqueue(MediaMsg in_msg_type);
    Msg audioEventDequeue();
    void audioEventQueueDelete();
    void mainEventEnqueue(MediaMsg in_msg_type);
    Msg mainEventDequeue();
    void mainEventQueueDelete();
    void videoEventEnqueue(MediaMsg in_msg_type);
    Msg videoEventDequeue();
    void videoEventQueueDelete();
    void audioPacketClear();
    AVPacket audioPacketDequeue();
    void audioPacketDelete();
    void audioPacketEnqueue(AVPacket* packet);
    void videoPacketClear();
    AVPacket videoPacketDequeue();
    void videoPacketDelete();
    void videoPacketEnqueue(AVPacket* packet);
    void dealAudioBufferQueueCallback();
    void dealAudioEvtLoop(JNIEnv* env);
    void dealVideoEvtLoop(JNIEnv* env);
    void dealMainEvtLoop(JNIEnv* env);
    void dealPacketCollector();
    double getSleepTime(double time_diff);
    void handlePlay();
    void handlePause();
    void handleStop();
    bool init();
    bool playAudio();
    void setPath(char* path);
    void updateVideoClock(long in_pts);

private:
    double main_clock, last_main_clock, video_clock;
    double delay_time;
    long last_video_pts;
    MediaState media_state;
    std::queue<Msg>* p_main_evt_queue;
    std::queue<Msg>* p_audio_evt_queue;
    std::queue<Msg>* p_video_evt_queue;

    std::queue<AVPacket>* p_audio_packet_queue;
    std::queue<AVPacket>* p_video_packet_queue;

    FFMpegCore* p_ffmpeg_core;
    AudioPlayer* p_audio;
    VideoDecoder* p_video_decoder;

    pthread_mutex_t main_evt_mutex_lock;
    pthread_cond_t main_evt_cond_lock;

    pthread_mutex_t audio_evt_mutex_lock;
    pthread_cond_t audio_evt_cond_lock;
    pthread_mutex_t audio_packet_mutex_lock;
    pthread_cond_t audio_packet_cond_lock;

    pthread_mutex_t audio_buffer_queue_mutex_lock;

    pthread_mutex_t video_evt_mutex_lock;
    pthread_cond_t video_evt_cond_lock;
    pthread_mutex_t video_packet_mutex_lock;
    pthread_cond_t video_packet_cond_lock;
};

#endif
