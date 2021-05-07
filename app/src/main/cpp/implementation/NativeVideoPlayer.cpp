#include "NativeVideoPlayer.h"
#include "LogUtil.h"

#define TAG "NativeVideoPlayer"
#define CLASS "com/lyzirving/flashvideo/core/FlashVideo"

static JavaVM* p_global_jvm = nullptr;

static jlong nCreate(JNIEnv *env, jclass clazz) {
    if (p_global_jvm == nullptr) {
        env->GetJavaVM(&p_global_jvm);
    }
    return reinterpret_cast<jlong>(new NativeVideoPlayer);
}

static jboolean nInit(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* p_player = reinterpret_cast<NativeVideoPlayer *>(pointer);
    return p_player->init();
}

static void nSetPath(JNIEnv *env, jclass clazz, jlong pointer, jstring path) {
    auto* p_player = reinterpret_cast<NativeVideoPlayer *>(pointer);
    char* tmp = const_cast<char *>(env->GetStringUTFChars(path, nullptr));
    p_player->setPath(tmp);
    env->ReleaseStringUTFChars(path, tmp);
}

static void nPlay(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* p_player = reinterpret_cast<NativeVideoPlayer *>(pointer);
    p_player->handlePlay();
}

static JNINativeMethod jniMethods[] = {
        {
            "nativeCreate",
            "()J",
            (void *) nCreate
        },
        {
                "nativeSetPath",
                "(JLjava/lang/String;)V",
                (void *) nSetPath
        },
        {
            "nativeInit",
            "(J)Z",
                (void *) nInit
        },
        {
            "nativePlay",
            "(J)V",
                (void *) nPlay
            },
};

void audioBufQueueCallback(SLAndroidSimpleBufferQueueItf buf_queue_itf, void *args) {
    auto* p_video_player = (NativeVideoPlayer*)args;
    p_video_player->dealAudioBufferQueueCallback();
}

void *audioLooper(void *args) {
    auto* p_player = static_cast<NativeVideoPlayer *>(args);
    p_player->dealAudioLoop();
}

void NativeVideoPlayer::dealAudioLoop() {
    AVPacket audio_packet;
    AudioData* tmp_audio_data;
    pthread_mutex_lock(&audio_evt_mutex_lock);
    while(true) {
        if (p_audio_msg_queue->empty()) {
            LogUtil::logD(TAG, {"dealAudioLoop: wait for msg"});
            pthread_cond_wait(&audio_evt_cond_lock, &audio_evt_mutex_lock);
        }
        Msg msg = p_audio_msg_queue->front();
        switch (msg.what) {
            case MSG_PLAY: {
                LogUtil::logD(TAG, {"dealAudioLoop: handle msg play"});
                p_audio->setPlayState();
                pthread_mutex_lock(&audio_packet_mutex_lock);
                if (p_audio_packet_queue->empty()) {
                    pthread_cond_wait(&audio_packet_cond_lock, &audio_packet_mutex_lock);
                }
                if (media_state == STATE_PLAY) {
                    audio_packet = p_audio_packet_queue->front();
                    tmp_audio_data = p_audio->decodePacket(&audio_packet);
                    if (tmp_audio_data->buf_size > 0) {
                        if(tmp_audio_data->now_time < main_clock) tmp_audio_data->now_time = main_clock;
                        main_clock = tmp_audio_data->now_time;
                        p_audio->enqueueAudio(tmp_audio_data);
                    }
                    av_packet_unref(&audio_packet);
                    p_audio_packet_queue->pop();
                }
                pthread_mutex_unlock(&audio_packet_mutex_lock);
                break;
            }
            case MSG_PAUSE:
            case MSG_STOP:
            case MSG_SEEK: {
                break;
            }
            case MSG_QUIT: {
                p_audio_msg_queue->pop();
                goto quit;
            }
        }
        p_audio_msg_queue->pop();
    }
    quit:
    LogUtil::logD(TAG, {"dealAudioLoop: quit"});
    pthread_mutex_unlock(&audio_evt_mutex_lock);
    p_audio->release();
}

void NativeVideoPlayer::dealAudioBufferQueueCallback() {
    if(media_state == STATE_PLAY) {
        pthread_mutex_lock(&audio_packet_mutex_lock);
        if (p_audio_packet_queue->empty()) {
            pthread_cond_wait(&audio_packet_cond_lock, &audio_packet_mutex_lock);
        }
        AVPacket audio_packet = p_audio_packet_queue->front();
        AudioData* audio_data = p_audio->decodePacket(&audio_packet);
        if (audio_data->buf_size > 0) {
            main_clock += audio_data->buf_size / ((double)(p_audio->p_audio_decoder->out_sample_rate * 2 * 2));
            p_audio->enqueueAudio(audio_data);
        }
        av_packet_unref(&audio_packet);
        p_audio_packet_queue->pop();
        pthread_mutex_unlock(&audio_packet_mutex_lock);
    }
}

void NativeVideoPlayer::dealMainEvtLoop() {
    pthread_t audio_thread;
    if (!p_ffmpeg_core->createEnv()) {
        goto quit;
    }
    p_audio = new AudioPlayer;
    if (!p_audio->init(p_ffmpeg_core->p_fmt_ctx, p_ffmpeg_core->audio_index)) {
        goto quit;
    }
    if (!p_audio->createBufQueuePlayer(audioBufQueueCallback, this)) {
        goto quit;
    }
    media_state = STATE_INITIALIZED;
    pthread_create(&audio_thread, nullptr, audioLooper, this);
    pthread_mutex_lock(&main_evt_mutex_lock);
    while(true) {
        if (p_msg_queue->empty()) {
            LogUtil::logD(TAG, {"dealMainEvtLoop: wait for msg"});
            pthread_cond_wait(&main_evt_cond_lock, &main_evt_mutex_lock);
        }
        Msg msg = p_msg_queue->front();
        switch(msg.what) {
            case MSG_PLAY: {
                LogUtil::logD(TAG, {"dealMainEvtLoop: handle msg play"});
                dealPacketCollector();
                break;
            }
            case MSG_QUIT: {
                LogUtil::logD(TAG, {"dealMainEvtLoop: handle msg quit"});
                goto quit;
            }
            default: {
                LogUtil::logD(TAG, {"dealMainEvtLoop: handle msg default"});
                break;
            }
        }
        p_msg_queue->pop();
    }
    quit:
    pthread_mutex_unlock(&main_evt_mutex_lock);
    LogUtil::logD(TAG, {"dealMainEvtLoop: quit"});
    if (p_ffmpeg_core != nullptr) {
        delete p_ffmpeg_core;
        p_ffmpeg_core = nullptr;
    }
    if (p_audio != nullptr) {
        delete p_audio;
        p_audio = nullptr;
    }
}

void NativeVideoPlayer::dealPacketCollector() {
    AVPacket* p_packet = nullptr;
    int res;
    while (media_state == STATE_PLAY) {
        if (p_packet == nullptr) p_packet = (AVPacket *)av_malloc(sizeof(AVPacket));
        res = av_read_frame(p_ffmpeg_core->p_fmt_ctx, p_packet);
        if (AVERROR_EOF == res) {
            LogUtil::logD(TAG, {"dealPacketCollector: end of stream"});
            break;
        } else if (JNI_OK != res) {
            av_packet_unref(p_packet);
        } else if (p_packet->stream_index == p_ffmpeg_core->audio_index) {
            pthread_mutex_lock(&audio_packet_mutex_lock);
            p_audio_packet_queue->push(*p_packet);
            pthread_cond_signal(&audio_packet_cond_lock);
            pthread_mutex_unlock(&audio_packet_mutex_lock);
        }
    }
}

void *eventLooper(void *args) {
    auto* p_player = static_cast<NativeVideoPlayer *>(args);
    /*JNIEnv *env = nullptr;
    if (!NativeVideoPlayer::threadAttachJvm(p_global_jvm, &env)) {
        LogUtil::logE(TAG, {"eventLooper: failed to attach thread to jvm"});
        return nullptr;
    }*/
    p_player->dealMainEvtLoop();
    //p_global_jvm->DetachCurrentThread();
    return nullptr;
}

void NativeVideoPlayer::handlePlay() {
    if (media_state == STATE_PLAY) {
        LogUtil::logD(TAG, {"handlePlay: already in play state"});
    }else if (media_state == STATE_INITIALIZED || media_state == STATE_PAUSE) {
        media_state = STATE_PLAY;
        Msg msg{.what = MSG_PLAY};
        pthread_mutex_lock(&main_evt_mutex_lock);
        p_msg_queue->push(msg);
        pthread_cond_signal(&main_evt_cond_lock);
        pthread_mutex_unlock(&main_evt_mutex_lock);

        pthread_mutex_lock(&audio_evt_mutex_lock);
        p_audio_msg_queue->push(msg);
        pthread_cond_signal(&audio_evt_cond_lock);
        pthread_mutex_unlock(&audio_evt_mutex_lock);
    } else {
        LogUtil::logD(TAG, {"handlePlay: invalid state ", (const char *) media_state});
    }
}

/**
 * create ffmpeg env
 * create AVPacket-thread, and start to generate AVPacket
 * create event-control thread
 * @return
 */
bool NativeVideoPlayer::init() {
    if (p_ffmpeg_core == nullptr) {
        LogUtil::logE(TAG, {"init: ffmpeg core is null"});
        return false;
    }
    pthread_t event_thread;
    pthread_create(&event_thread, nullptr, eventLooper, this);
    return true;
}

void NativeVideoPlayer::setPath(char *path) {
    if (path != nullptr && path[0] != '\0') {
        if (p_ffmpeg_core == nullptr) {
            p_ffmpeg_core = new FFMpegCore;
        }
        p_ffmpeg_core->setPath(path);
    }
}

bool NativeVideoPlayer::registerSelf(JNIEnv *env) {
    int count = sizeof(jniMethods) / sizeof(jniMethods[0]);
    jclass javaClass = env->FindClass(CLASS);
    if(!javaClass) {
        LogUtil::logE(TAG, {"registerSelf: failed to find class ", CLASS});
        goto fail;
    }
    if (env->RegisterNatives(javaClass, jniMethods, count) < 0) {
        LogUtil::logE(TAG, {"registerSelf: failed to register native methods ", CLASS});
        goto fail;
    }
    return true;
    fail:
    return false;
}

bool NativeVideoPlayer::threadAttachJvm(JavaVM *p_jvm, JNIEnv **pp_env) {
    if (p_jvm->GetEnv((void **) pp_env, JNI_VERSION_1_6) == JNI_EDETACHED) {
        if (p_jvm->AttachCurrentThread(pp_env, nullptr) != 0) {
            return false;
        }
    }
    return true;
}



