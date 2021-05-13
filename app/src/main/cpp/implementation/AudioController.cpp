#include "AudioController.h"
#include "LogUtil.h"

#include <map>

#define TAG "AudioController"
#define CLASS "com/lyzirving/flashvideo/core/FlashAudio"

static JavaVM* p_global_jvm = nullptr;
static std::map<jlong, jobject> global_listeners;
static JNIEnv* audio_thread_env = nullptr;

static jlong nCreate(JNIEnv *env, jclass clazz) {
    if (p_global_jvm == nullptr) {
        env->GetJavaVM(&p_global_jvm);
    }
    return reinterpret_cast<jlong>(new AudioController);
}

static jboolean nInit(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* p_control = reinterpret_cast<AudioController *>(pointer);
    return p_control->init();
}

static void nSetPath(JNIEnv *env, jclass clazz, jlong pointer, jstring path) {
    auto* p_control = reinterpret_cast<AudioController *>(pointer);
    char* tmp = const_cast<char *>(env->GetStringUTFChars(path, nullptr));
    p_control->setPath(tmp);
    env->ReleaseStringUTFChars(path, tmp);
}

static void nPlay(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* p_control = reinterpret_cast<AudioController *>(pointer);
    p_control->handlePlay();
}

static void nPause(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* p_control = reinterpret_cast<AudioController *>(pointer);
    p_control->handlePause();
}

static void nStop(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* p_control = reinterpret_cast<AudioController *>(pointer);
    p_control->handleStop();
}

static void nSetListener(JNIEnv *env, jclass clazz, jlong pointer, jobject listener) {
    if (listener == nullptr) {
        LogUtil::logE(TAG, {"nSetListener: listener is null"});
    } else {
        global_listeners.insert(std::pair<jlong, jobject >(pointer, env->NewGlobalRef(listener)));
    }
}

static void nSeek(JNIEnv *env, jclass clazz, jlong pointer, jfloat seekDst) {
    auto* p_control = reinterpret_cast<AudioController *>(pointer);
    p_control->handleSeek(seekDst);
}

static void nSetVolume(JNIEnv *env, jclass clazz, jlong pointer, jint volume) {
    auto* p_control = reinterpret_cast<AudioController *>(pointer);
    p_control->handleSetVolume(volume);
}

static void nSetPitch(JNIEnv *env, jclass clazz, jlong pointer, jdouble pitch) {
    auto* p_control = reinterpret_cast<AudioController *>(pointer);
    p_control->handleSetPitch(pitch);
}

static void nSetTempo(JNIEnv *env, jclass clazz, jlong pointer, jdouble pitch) {
    auto* p_control = reinterpret_cast<AudioController *>(pointer);
    p_control->handleSetTempo(pitch);
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
        {
                "nativePause",
                "(J)V",
                (void *) nPause
        },
        {
                "nativeStop",
                "(J)V",
                (void *) nStop
        },
        {
                "nativeSetListener",
                "(JLcom/lyzirving/flashvideo/core/VideoListenerAdapter;)V",
                (void *) nSetListener
        },
        {
                "nativeSeek",
                "(JF)V",
                (void *) nSeek
        },
        {
                "nativeSetVolume",
                "(JI)V",
                (void *) nSetVolume
        },
        {
                "nativeSetPitch",
                "(JD)V",
                (void *) nSetPitch
        },
        {
                "nativeSetTempo",
                "(JD)V",
                (void *) nSetTempo
        },
};

void audioBufQueueCallback(SLAndroidSimpleBufferQueueItf buf_queue_itf, void *args) {
    if (audio_thread_env == nullptr && !JavaCallbackUtil::threadAttachJvm(p_global_jvm, &audio_thread_env)) {
        LogUtil::logE(TAG, {"audioBufQueueCallback: env attach audio callback thread failed"});
        audio_thread_env = nullptr;
    }
    auto* p_control = (AudioController*)args;
    if (p_control != nullptr)
        p_control->dealAudioBufferQueueCallback();
    if (audio_thread_env != nullptr) {
        p_global_jvm->DetachCurrentThread();
        audio_thread_env = nullptr;
    }
}

void *audioLooper(void *args) {
    auto* p_control = static_cast<AudioController *>(args);
    JNIEnv *env = nullptr;
    if (!JavaCallbackUtil::threadAttachJvm(p_global_jvm, &env)) {
        LogUtil::logE(TAG, {"audioLooper: failed to attach thread to jvm"});
        return nullptr;
    }
    p_control->dealAudioLoop(env);
    p_global_jvm->DetachCurrentThread();
    return nullptr;
}

void AudioController::dealAudioLoop(JNIEnv* env) {
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
                LogUtil::logD(TAG, {"dealAudioLoop: handle play"});
                p_audio->setPlayState();
                bool finish = false;
                while(!finish && media_state == STATE_PLAY) {
                    pthread_mutex_lock(&audio_packet_mutex_lock);
                    if (p_audio_packet_queue == nullptr) {
                        LogUtil::logD(TAG, {"dealAudioLoop: play, audio packet is null"});
                        pthread_mutex_unlock(&audio_packet_mutex_lock);
                        goto quit;
                    }
                    if (p_audio_packet_queue->empty()) {
                        pthread_cond_wait(&audio_packet_cond_lock, &audio_packet_mutex_lock);
                    }
                    if (p_audio_packet_queue == nullptr) {
                        LogUtil::logD(TAG, {"dealAudioLoop: play awake, audio packet is null"});
                        pthread_mutex_unlock(&audio_packet_mutex_lock);
                        goto quit;
                    }
                    audio_packet = p_audio_packet_queue->front();
                    tmp_audio_data = p_audio->decodePacket(&audio_packet);
                    if (tmp_audio_data->buf_size > 0) {
                        if(tmp_audio_data->now_time < main_clock) tmp_audio_data->now_time = main_clock;
                        main_clock = tmp_audio_data->now_time;
                        last_main_clock = main_clock;
                        finish = p_audio->enqueueAudio(tmp_audio_data);
                    }
                    av_packet_unref(&audio_packet);
                    p_audio_packet_queue->pop();
                    pthread_mutex_unlock(&audio_packet_mutex_lock);
                }
                break;
            }
            case MSG_STOP: {
                LogUtil::logD(TAG, {"dealAudioLoop: handle stop"});
                p_audio->setStopState();
                goto quit;
            }
        }
        p_audio_msg_queue->pop();
    }
    quit:
    p_audio_msg_queue->pop();
    LogUtil::logD(TAG, {"dealAudioLoop: quit"});
    pthread_mutex_unlock(&audio_evt_mutex_lock);
    if (p_audio != nullptr) {
        delete p_audio;
        p_audio = nullptr;
    }
    if (p_ffmpeg_core != nullptr) {
        delete p_ffmpeg_core;
        p_ffmpeg_core = nullptr;
    }
    if (p_audio_msg_queue != nullptr)
        delete p_audio_msg_queue;
    p_audio_msg_queue = nullptr;

    pthread_mutex_destroy(&audio_evt_mutex_lock);
    pthread_cond_destroy(&audio_evt_cond_lock);
    pthread_mutex_destroy(&audio_packet_mutex_lock);
    pthread_cond_destroy(&audio_packet_cond_lock);
}

void AudioController::dealAudioBufferQueueCallback() {
    bool finish = false;
    while(!finish && media_state == STATE_PLAY) {
        //make it safe in concurrent env
        pthread_mutex_lock(&audio_packet_mutex_lock);
        if (p_audio_packet_queue == nullptr) {
            LogUtil::logD(TAG, {"dealAudioBufferQueueCallback: audio packet is null"});
            pthread_mutex_unlock(&audio_packet_mutex_lock);
            break;
        }
        if (p_audio_packet_queue->empty()) {
            pthread_cond_wait(&audio_packet_cond_lock, &audio_packet_mutex_lock);
        }
        //make it safe in concurrent env
        if (media_state != STATE_PLAY || p_audio_packet_queue == nullptr || p_audio == nullptr) {
            LogUtil::logD(TAG, {"dealAudioBufferQueueCallback: invalid state"});
            pthread_mutex_unlock(&audio_packet_mutex_lock);
            break;
        }
        AVPacket audio_packet = p_audio_packet_queue->front();
        AudioData* audio_data = p_audio->decodePacket(&audio_packet);
        if (audio_data->buf_size > 0) {
            main_clock += audio_data->buf_size / ((double)(p_audio->p_audio_decoder->out_sample_rate * 2 * 2));
            //invoke the callback every second
            if (main_clock - last_main_clock > 1) {
                last_main_clock = main_clock;
                jobject listener;
                if ((listener = JavaCallbackUtil::findListener(&global_listeners, reinterpret_cast<jlong>(this))) != nullptr
                    && audio_thread_env != nullptr)
                    JavaCallbackUtil::callMediaTickTime(audio_thread_env, listener, main_clock);
            }
            finish = p_audio->enqueueAudio(audio_data);
        }
        av_packet_unref(&audio_packet);
        p_audio_packet_queue->pop();
        pthread_mutex_unlock(&audio_packet_mutex_lock);
    }
}

void AudioController::dealMainEvtLoop(JNIEnv* env) {
    pthread_t audio_thread;
    jobject listener = nullptr;
    if (!p_ffmpeg_core->createEnv(FFMpegCore::MODE_AUDIO)) {
        goto quit;
    }
    p_audio = new AudioPlayer;
    if (!p_audio->init(p_ffmpeg_core->p_fmt_ctx, p_ffmpeg_core->audio_index)) {
        goto quit;
    }
    if (!p_audio->createBufQueuePlayer(audioBufQueueCallback, this)) {
        goto quit;
    }
    p_audio->initSoundTouch();
    media_state = STATE_INITIALIZED;
    listener = JavaCallbackUtil::findListener(&global_listeners, reinterpret_cast<jlong>(this));
    if (listener != nullptr) JavaCallbackUtil::callMediaPrepare(env, listener, p_audio->p_audio_decoder->media_time);
    pthread_create(&audio_thread, nullptr, audioLooper, this);
    pthread_setname_np(audio_thread, "audio-event-thread");
    pthread_mutex_lock(&main_evt_mutex_lock);
    while(true) {
        if (p_msg_queue->empty()) {
            LogUtil::logD(TAG, {"dealMainEvtLoop: wait for msg"});
            pthread_cond_wait(&main_evt_cond_lock, &main_evt_mutex_lock);
        }
        Msg msg = p_msg_queue->front();
        switch(msg.what) {
            case MSG_PLAY: {
                LogUtil::logD(TAG, {"dealMainEvtLoop: handle play"});
                dealPacketCollector();
                break;
            }
            case MSG_STOP: {
                LogUtil::logD(TAG, {"dealMainEvtLoop: handle stop"});
                pthread_mutex_lock(&audio_packet_mutex_lock);
                if (p_audio_packet_queue != nullptr)
                    delete p_audio_packet_queue;
                p_audio_packet_queue = nullptr;
                pthread_cond_signal(&audio_packet_cond_lock);
                pthread_mutex_unlock(&audio_packet_mutex_lock);

                Msg msg_stop{.what = MSG_STOP};
                pthread_mutex_lock(&audio_evt_mutex_lock);
                p_audio_msg_queue->push(msg_stop);
                pthread_cond_signal(&audio_evt_cond_lock);
                pthread_mutex_unlock(&audio_evt_mutex_lock);
                goto quit;
            }
            case MSG_SEEK: {
                LogUtil::logD(TAG, {"dealMainEvtLoop: handle seek"});
                seekToDst(p_ffmpeg_core->seek_dst);
                media_state = STATE_PLAY;
                Msg msg_play{.what = MSG_PLAY};
                p_msg_queue->push(msg_play);
                pthread_mutex_lock(&audio_evt_mutex_lock);
                p_audio_msg_queue->push(msg_play);
                pthread_cond_signal(&audio_evt_cond_lock);
                pthread_mutex_unlock(&audio_evt_mutex_lock);
                break;
            }
        }
        p_msg_queue->pop();
    }
    quit:
    p_msg_queue->pop();
    pthread_mutex_unlock(&main_evt_mutex_lock);
    LogUtil::logD(TAG, {"dealMainEvtLoop: quit"});
    if (listener != nullptr) JavaCallbackUtil::callMediaStop(env, listener);
    if ((listener = JavaCallbackUtil::removeListener(&global_listeners, reinterpret_cast<jlong>(this))) != nullptr) {
        env->DeleteGlobalRef(listener);
    }
    if (p_msg_queue != nullptr)
        delete p_msg_queue;
    p_msg_queue = nullptr;

    pthread_mutex_destroy(&main_evt_mutex_lock);
    pthread_cond_destroy(&main_evt_cond_lock);
}

void AudioController::dealPacketCollector() {
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
    auto* p_control = static_cast<AudioController *>(args);
    JNIEnv *env = nullptr;
    if (!JavaCallbackUtil::threadAttachJvm(p_global_jvm, &env)) {
        LogUtil::logE(TAG, {"eventLooper: failed to attach thread to jvm"});
        return nullptr;
    }
    p_control->dealMainEvtLoop(env);
    p_global_jvm->DetachCurrentThread();
    return nullptr;
}

void AudioController::handlePlay() {
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

void AudioController::handleSeek(float seek_dst) {
    if (media_state < STATE_INITIALIZED || media_state > STATE_STOP) {
        LogUtil::logE(TAG, {"handleSeek: invalid state, " , std::to_string(media_state)});
        return;
    }
    media_state = STATE_SEEK;
    p_ffmpeg_core->seek_dst = seek_dst;
    pthread_mutex_lock(&main_evt_mutex_lock);
    Msg seek_msg{.what = MSG_SEEK};
    p_msg_queue->push(seek_msg);
    pthread_cond_signal(&main_evt_cond_lock);
    pthread_mutex_unlock(&main_evt_mutex_lock);
}

void AudioController::handlePause() {
    if (media_state == STATE_PAUSE) {
        LogUtil::logD(TAG, {"handlePause: already in pause state"});
    }else if (media_state == STATE_PLAY) {
        media_state = STATE_PAUSE;
    } else {
        LogUtil::logD(TAG, {"handlePause: invalid state ", (const char *) media_state});
    }
}

void AudioController::handleStop() {
    if (media_state >= STATE_INITIALIZED && media_state < STATE_STOP) {
        media_state = STATE_STOP;
        Msg msg{.what = MSG_STOP};
        pthread_mutex_lock(&main_evt_mutex_lock);
        p_msg_queue->push(msg);
        pthread_cond_signal(&main_evt_cond_lock);
        pthread_mutex_unlock(&main_evt_mutex_lock);
    } else {
        LogUtil::logD(TAG, {"handleStop: invalid state ", (const char *) media_state});
    }
}

void AudioController::handleSetVolume(int volume) {
    p_audio->setVolume(volume);
}

void AudioController::handleSetPitch(double pitch) {
    if (p_audio != nullptr) {
        //be sure to use lock, because in audio thread, there might be concurrent problem
        pthread_mutex_lock(&audio_packet_mutex_lock);
        p_audio->setPitch(pitch);
        pthread_mutex_unlock(&audio_packet_mutex_lock);
    }
}

void AudioController::handleSetTempo(double tempo) {
    if (p_audio != nullptr) {
        //be sure to use lock, because in audio thread, there might be concurrent problem
        pthread_mutex_lock(&audio_packet_mutex_lock);
        p_audio->setTempo(tempo);
        pthread_mutex_unlock(&audio_packet_mutex_lock);
    }
}

/**
 * create ffmpeg env
 * create AVPacket-thread, and start to generate AVPacket
 * create event-control thread
 * @return
 */
bool AudioController::init() {
    if (p_ffmpeg_core == nullptr) {
        LogUtil::logE(TAG, {"init: ffmpeg core is null"});
        return false;
    }
    pthread_t event_thread;
    pthread_create(&event_thread, nullptr, eventLooper, this);
    pthread_setname_np(event_thread, "main-event-thread");
    return true;
}

void AudioController::setPath(char *path) {
    if (path != nullptr && path[0] != '\0') {
        if (p_ffmpeg_core == nullptr) {
            p_ffmpeg_core = new FFMpegCore;
        }
        p_ffmpeg_core->setPath(path);
    }
}

void AudioController::seekToDst(float dst_ratio) {
    float target;
    if(dst_ratio < 0)
        target = 0;
    else if(dst_ratio > 1)
        target = 1;
    else
        target = dst_ratio;
    LogUtil::logD(TAG, {"seekToDst: original = ", std::to_string(dst_ratio), ", target = ", std::to_string(target)});
    pthread_mutex_lock(&audio_packet_mutex_lock);
    //clear the packet queue
    if (p_audio_packet_queue != nullptr)
        delete p_audio_packet_queue;
    p_audio_packet_queue = new std::queue<AVPacket>();
    pthread_mutex_unlock(&audio_packet_mutex_lock);
    avformat_seek_file(p_ffmpeg_core->p_fmt_ctx, -1, INT64_MIN,
                       p_ffmpeg_core->p_fmt_ctx->duration * target, INT64_MAX, 0);
    last_main_clock = main_clock = 0;
    p_ffmpeg_core->seek_dst = -1;
    if (p_audio != nullptr)
        avcodec_flush_buffers(p_audio->p_audio_decoder->p_audio_codec_ctx);
}

bool AudioController::registerSelf(JNIEnv *env) {
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



