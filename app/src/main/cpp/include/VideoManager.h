#ifndef FLASHVIDEO_VIDEOMANAGER_H
#define FLASHVIDEO_VIDEOMANAGER_H

#include "FFMpegCore.h"
#include "AudioPlayer.h"
#include "VideoDecoder.h"
#include "PacketQueue.h"
#include "MsgQueue.h"
#include "LogUtil.h"

class VideoManager {
public:
    VideoManager() {
        LogUtil::logD("VideoManager", {"constructed"});
        engine = nullptr;
        audio_player = nullptr;
        video_decoder = nullptr;
        path = nullptr;

        video_packet_queue = new PacketQueue;
        audio_packet_queue = new PacketQueue;
        state = STATE_IDLE;
        flag_eof = false;
        main_clock = 0;
        last_main_clock = 0;
        video_clock = 0;
        last_video_pts = 0;
        delay_time = 0;
        audio_is_quitting = false;

        pthread_mutex_init(&quit_mutex, nullptr);
        pthread_cond_init(&quit_cond, nullptr);
    }

    ~VideoManager() {
        LogUtil::logD("VideoManager", {"destroy"});
        if (video_packet_queue != nullptr) {
            video_packet_queue->clear();
            delete video_packet_queue;
            video_packet_queue = nullptr;
        }
        if (audio_packet_queue != nullptr) {
            audio_packet_queue->clear();
            delete audio_packet_queue;
            audio_packet_queue = nullptr;
        }
        if (audio_player != nullptr) {
            delete audio_player;
            audio_player = nullptr;
        }
        if (video_decoder != nullptr) {
            delete video_decoder;
            video_decoder = nullptr;
        }
        if (engine != nullptr) {
            delete engine;
            engine = nullptr;
        }
        audio_is_quitting = false;
        pthread_mutex_destroy(&quit_mutex);
        pthread_cond_destroy(&quit_cond);
    }
    static bool registerSelf(JNIEnv *env);

    void collectPacket(AVPacket* in_packet);
    void init(char* in_path);
    void dealAudioLoop(JNIEnv* env);
    void dealAudioCallback(JNIEnv* env);
    void dealMainLoop(JNIEnv* env);
    void dealVideoLoop(JNIEnv* env);
    double getSleepTime(double time_diff);
    void setState(MediaState new_state);
    bool stateEqual(MediaState in_state);
    bool stateMoreThanAndEqual(MediaState in_state);
    bool stateLessThan(MediaState in_state);
    bool stateLessThanAndEqual(MediaState in_state);
    void updateVideoClock(long in_pts);
private:
    FFMpegCore* engine;
    AudioPlayer* audio_player;
    VideoDecoder* video_decoder;

    PacketQueue* video_packet_queue;
    PacketQueue* audio_packet_queue;

    char* path;
    bool flag_eof;
    double main_clock, last_main_clock, video_clock, delay_time;
    long last_video_pts;
    MediaState state;

    bool audio_is_quitting;
    pthread_mutex_t quit_mutex;
    pthread_cond_t quit_cond;
};

#endif
