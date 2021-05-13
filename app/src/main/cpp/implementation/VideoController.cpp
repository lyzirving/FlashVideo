#include "VideoController.h"
#include "LogUtil.h"
#include <map>

#define TAG "VideoController"
#define CLASS "com/lyzirving/flashvideo/core/FlashVideo"

static JavaVM* p_global_jvm = nullptr;
static std::map<jlong, jobject> global_listeners;
static JNIEnv* audio_thread_env = nullptr;

void *audioEvtLoop(void *args) {
    auto* p_control = static_cast<VideoController *>(args);
    JNIEnv *env = nullptr;
    if (!JavaCallbackUtil::threadAttachJvm(p_global_jvm, &env)) {
        LogUtil::logE(TAG, {"audioEvtLoop: failed to attach thread to jvm"});
        return nullptr;
    }
    p_control->dealAudioEvtLoop(env);
    p_global_jvm->DetachCurrentThread();
    return nullptr;
}

void *videoEvtLoop(void *args) {
    auto* p_control = static_cast<VideoController *>(args);
    JNIEnv *env = nullptr;
    if (!JavaCallbackUtil::threadAttachJvm(p_global_jvm, &env)) {
        LogUtil::logE(TAG, {"videoEvtLoop: failed to attach thread to jvm"});
        return nullptr;
    }
    p_control->dealVideoEvtLoop(env);
    p_global_jvm->DetachCurrentThread();
    return nullptr;
}

void openslCallback(SLAndroidSimpleBufferQueueItf buf_queue_itf, void *args) {
    if (audio_thread_env == nullptr && !JavaCallbackUtil::threadAttachJvm(p_global_jvm, &audio_thread_env)) {
        LogUtil::logE(TAG, {"openslCallback: env attach audio callback thread failed"});
        audio_thread_env = nullptr;
    }
    auto* p_control = (VideoController*)args;
    if (p_control != nullptr)
        p_control->dealAudioBufferQueueCallback();
    if (audio_thread_env != nullptr) {
        p_global_jvm->DetachCurrentThread();
        audio_thread_env = nullptr;
    }
}

void *mainEvtLooper(void *args) {
    auto* p_control = static_cast<VideoController *>(args);
    JNIEnv *env = nullptr;
    if (!JavaCallbackUtil::threadAttachJvm(p_global_jvm, &env)) {
        LogUtil::logE(TAG, {"eventLooper: failed to attach thread to jvm"});
        return nullptr;
    }
    p_control->dealMainEvtLoop(env);
    p_global_jvm->DetachCurrentThread();
    return nullptr;
}

static jlong nCreate(JNIEnv *env, jclass clazz) {
    if (p_global_jvm == nullptr) {
        env->GetJavaVM(&p_global_jvm);
    }
    return reinterpret_cast<jlong>(new VideoController);
}

static jboolean nInit(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* p_control = reinterpret_cast<VideoController *>(pointer);
    return p_control->init();
}

static void nSetPath(JNIEnv *env, jclass clazz, jlong pointer, jstring path) {
    auto* p_control = reinterpret_cast<VideoController *>(pointer);
    char* tmp = const_cast<char *>(env->GetStringUTFChars(path, nullptr));
    p_control->setPath(tmp);
    env->ReleaseStringUTFChars(path, tmp);
}

static void nPlay(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* p_control = reinterpret_cast<VideoController *>(pointer);
    p_control->handlePlay();
}

static void nPause(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* p_control = reinterpret_cast<VideoController *>(pointer);
    p_control->handlePause();
}

static void nStop(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* p_control = reinterpret_cast<VideoController *>(pointer);
    p_control->handleStop();
}

static void nSeek(JNIEnv *env, jclass clazz, jlong pointer, jfloat seekDst) {
    auto* p_control = reinterpret_cast<VideoController *>(pointer);
    //p_control->handleSeek(seekDst);
}

static void nSetListener(JNIEnv *env, jclass clazz, jlong pointer, jobject listener) {
    if (listener == nullptr) {
        LogUtil::logE(TAG, {"nSetListener: listener is null"});
    } else {
        global_listeners.insert(std::pair<jlong, jobject >(pointer, env->NewGlobalRef(listener)));
    }
}

void VideoController::dealAudioEvtLoop(JNIEnv *env) {
    AVPacket audio_packet;
    AudioData* tmp_audio_data;
    pthread_mutex_lock(&audio_evt_mutex_lock);
    while(true) {
        if (p_audio_evt_queue->empty()) {
            LogUtil::logD(TAG, {"dealAudioEvtLoop: wait for msg"});
            pthread_cond_wait(&audio_evt_cond_lock, &audio_evt_mutex_lock);
        }
        Msg msg = p_audio_evt_queue->front();
        switch (msg.what) {
            case MSG_PLAY: {
                LogUtil::logD(TAG, {"dealAudioEvtLoop: handle msg play"});
                p_audio->setPlayState();
                bool finish = false;
                while(!finish && media_state == STATE_PLAY) {
                    pthread_mutex_lock(&audio_packet_mutex_lock);
                    if (p_audio_packet_queue == nullptr) {
                        LogUtil::logD(TAG, {"dealAudioEvtLoop: handle play, queue is null, quit"});
                        pthread_mutex_unlock(&audio_packet_mutex_lock);
                        goto quit;
                    }
                    if (p_audio_packet_queue->empty()) {
                        pthread_cond_wait(&audio_packet_cond_lock, &audio_packet_mutex_lock);
                    }
                    if (p_audio_packet_queue == nullptr) {
                        LogUtil::logD(TAG, {"dealAudioEvtLoop: handle play awake, queue is null, quit"});
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
                LogUtil::logD(TAG, {"dealAudioEvtLoop: handle msg stop"});
                p_audio->setStopState();
                pthread_mutex_lock(&audio_packet_mutex_lock);
                if (p_audio_packet_queue != nullptr)
                    delete p_audio_packet_queue;
                p_audio_packet_queue = nullptr;
                pthread_cond_signal(&audio_packet_cond_lock);
                pthread_mutex_unlock(&audio_packet_mutex_lock);
                goto quit;
            }
        }
        p_audio->setPauseState();
        p_audio_evt_queue->pop();
    }
    quit:
    LogUtil::logD(TAG, {"dealAudioEvtLoop: quit"});
    p_audio_evt_queue->pop();
    pthread_mutex_unlock(&audio_evt_mutex_lock);

    if (p_audio_evt_queue != nullptr)
        delete p_audio_evt_queue;
    p_audio_evt_queue = nullptr;

    if (p_audio != nullptr)
        delete p_audio;
    p_audio = nullptr;

    pthread_mutex_destroy(&audio_evt_mutex_lock);
    pthread_cond_destroy(&audio_evt_cond_lock);
    pthread_mutex_destroy(&audio_packet_mutex_lock);
    pthread_cond_destroy(&audio_packet_cond_lock);
}

void VideoController::dealAudioBufferQueueCallback() {
    bool finish = false;
    while(!finish && media_state == STATE_PLAY) {
        //make it safe in concurrent env
        pthread_mutex_lock(&audio_packet_mutex_lock);
        if (p_audio_packet_queue == nullptr) {
            LogUtil::logD(TAG, {"dealAudioBufferQueueCallback: queue is null, quit"});
            pthread_mutex_unlock(&audio_packet_mutex_lock);
            break;
        }
        if (p_audio_packet_queue->empty()) {
            pthread_cond_wait(&audio_packet_cond_lock, &audio_packet_mutex_lock);
        }
        if (media_state != STATE_PLAY || p_audio_packet_queue == nullptr || p_audio == nullptr) {
            LogUtil::logD(TAG, {"dealAudioBufferQueueCallback: invalid state, quit"});
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

void VideoController::dealMainEvtLoop(JNIEnv *env) {
    pthread_t audio_thread;
    pthread_t video_thread;
    jobject listener = nullptr;
    if (!p_ffmpeg_core->createEnv(FFMpegCore::MODE_VIDEO)) {
        goto quit;
    }
    p_audio = new AudioPlayer;
    if (!p_audio->init(p_ffmpeg_core->p_fmt_ctx, p_ffmpeg_core->audio_index)) {
        goto quit;
    }
    if (!p_audio->createBufQueuePlayer(openslCallback, this)) {
        goto quit;
    }
    p_audio->initSoundTouch();

    p_video_decoder = new VideoDecoder;
    if (!p_video_decoder->init(p_ffmpeg_core->p_fmt_ctx, p_ffmpeg_core->video_index)) {
        goto quit;
    }
    media_state = STATE_INITIALIZED;
    listener = JavaCallbackUtil::findListener(&global_listeners, reinterpret_cast<jlong>(this));
    if (listener != nullptr)
        JavaCallbackUtil::callMediaPrepare(env, listener, p_video_decoder->duration);

    pthread_create(&audio_thread, nullptr, audioEvtLoop, this);
    pthread_setname_np(audio_thread, "audio-event-thread");

    pthread_create(&video_thread, nullptr, videoEvtLoop, this);
    pthread_setname_np(video_thread, "video-event-thread");

    pthread_mutex_lock(&main_evt_mutex_lock);
    while(true) {
        if (p_main_evt_queue->empty()) {
            LogUtil::logD(TAG, {"dealMainEvtLoop: wait for msg"});
            pthread_cond_wait(&main_evt_cond_lock, &main_evt_mutex_lock);
        }
        Msg msg = p_main_evt_queue->front();
        switch(msg.what) {
            case MSG_PLAY: {
                LogUtil::logD(TAG, {"dealMainEvtLoop: handle msg play"});
                Msg msg_play{.what = MSG_PLAY};
                pthread_mutex_lock(&audio_evt_mutex_lock);
                p_audio_evt_queue->push(msg_play);
                pthread_cond_signal(&audio_evt_cond_lock);
                pthread_mutex_unlock(&audio_evt_mutex_lock);

                pthread_mutex_lock(&video_evt_mutex_lock);
                p_video_evt_queue->push(msg_play);
                pthread_cond_signal(&video_evt_cond_lock);
                pthread_mutex_unlock(&video_evt_mutex_lock);

                //this method will block current thread
                dealPacketCollector();
                break;
            }
            case MSG_STOP: {
                LogUtil::logD(TAG, {"dealMainEvtLoop: handle msg stop"});
                pthread_mutex_lock(&audio_packet_mutex_lock);
                if (p_audio_packet_queue != nullptr)
                    delete p_audio_packet_queue;
                p_audio_packet_queue = nullptr;
                pthread_cond_signal(&audio_packet_cond_lock);
                pthread_mutex_unlock(&audio_packet_mutex_lock);

                pthread_mutex_lock(&video_packet_mutex_lock);
                if (p_video_packet_queue != nullptr)
                    delete p_video_packet_queue;
                p_video_packet_queue = nullptr;
                pthread_cond_signal(&video_packet_cond_lock);
                pthread_mutex_unlock(&video_packet_mutex_lock);

                Msg msg_stop{.what = MSG_STOP};
                pthread_mutex_lock(&audio_evt_mutex_lock);
                p_audio_evt_queue->push(msg_stop);
                pthread_cond_signal(&audio_evt_cond_lock);
                pthread_mutex_unlock(&audio_evt_mutex_lock);

                pthread_mutex_lock(&video_evt_mutex_lock);
                p_video_evt_queue->push(msg_stop);
                pthread_cond_signal(&video_evt_cond_lock);
                pthread_mutex_unlock(&video_evt_mutex_lock);
                goto quit;
            }
        }
        p_main_evt_queue->pop();
    }
    quit:
    p_main_evt_queue->pop();
    pthread_mutex_unlock(&main_evt_mutex_lock);
    LogUtil::logD(TAG, {"dealMainEvtLoop: quit"});

    if (listener != nullptr)
        JavaCallbackUtil::callMediaStop(env, listener);
    if ((listener = JavaCallbackUtil::removeListener(&global_listeners, reinterpret_cast<jlong>(this))) != nullptr) {
        env->DeleteGlobalRef(listener);
    }

    if (p_main_evt_queue != nullptr)
        delete p_main_evt_queue;
    p_main_evt_queue = nullptr;

    pthread_mutex_destroy(&main_evt_mutex_lock);
    pthread_cond_destroy(&main_evt_cond_lock);
}

void VideoController::dealPacketCollector() {
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
            if (p_audio_packet_queue != nullptr)
                p_audio_packet_queue->push(*p_packet);
            pthread_cond_signal(&audio_packet_cond_lock);
            pthread_mutex_unlock(&audio_packet_mutex_lock);
        } else if (p_packet->stream_index == p_ffmpeg_core->video_index) {
            pthread_mutex_lock(&video_packet_mutex_lock);
            if (p_video_packet_queue != nullptr)
                p_video_packet_queue->push(*p_packet);
            pthread_cond_signal(&video_packet_cond_lock);
            pthread_mutex_unlock(&video_packet_mutex_lock);
        }
    }
}

void VideoController::dealVideoEvtLoop(JNIEnv *env) {
    AVPacket video_packet;
    DataYUV420* p_yuv;
    jobject listener;
    double video_sleep_time;
    pthread_mutex_lock(&video_evt_mutex_lock);
    while(true) {
        if (p_video_evt_queue->empty()) {
            LogUtil::logD(TAG, {"dealVideoEvtLoop: wait for msg"});
            pthread_cond_wait(&video_evt_cond_lock, &video_evt_mutex_lock);
        }
        Msg msg = p_video_evt_queue->front();
        switch (msg.what) {
            case MSG_PLAY: {
                LogUtil::logD(TAG, {"dealVideoEvtLoop: handle play"});
                while(media_state == STATE_PLAY) {
                    pthread_mutex_lock(&video_packet_mutex_lock);
                    if (p_video_packet_queue == nullptr) {
                        LogUtil::logD(TAG, {"dealVideoEvtLoop: handle play, queue is null, quit"});
                        pthread_mutex_unlock(&video_packet_mutex_lock);
                        goto quit;
                    }
                    if (p_video_packet_queue->empty()) {
                        pthread_cond_wait(&video_packet_cond_lock, &video_packet_mutex_lock);
                    }
                    if (p_video_packet_queue == nullptr) {
                        LogUtil::logD(TAG, {"dealVideoEvtLoop: handle play awake, queue is null, quit"});
                        pthread_mutex_unlock(&video_packet_mutex_lock);
                        goto quit;
                    }
                    video_packet = p_video_packet_queue->front();
                    p_yuv = new DataYUV420;
                    p_yuv->width = -1;
                    p_video_decoder->decodePacket(&video_packet, p_yuv);
                    if (p_yuv->width > 0) {
                        video_clock += p_video_decoder->getFramePresentationTime(p_yuv->pts);
                        video_sleep_time = getSleepTime(main_clock - video_clock);
                        if (video_sleep_time != 0)
                            av_usleep(video_sleep_time * ONE_MIC0_SEC);
                        listener = JavaCallbackUtil::findListener(&global_listeners,reinterpret_cast<jlong>(this));
                        JavaCallbackUtil::callVideoFrame(env, listener, p_yuv->width, p_yuv->height,
                                p_yuv->y_data, p_yuv->u_data, p_yuv->v_data);
                    }
                    p_video_packet_queue->pop();
                    av_packet_free(reinterpret_cast<AVPacket **>(&video_packet));
                    av_frame_unref(p_video_decoder->p_raw_frame);
                    pthread_mutex_unlock(&video_packet_mutex_lock);
                }
                break;
            }
            case MSG_STOP: {
                LogUtil::logD(TAG, {"dealVideoEvtLoop: handle stop"});
                goto quit;
            }
        }
        p_video_evt_queue->pop();
    }
    quit:
    p_video_evt_queue->pop();
    pthread_mutex_unlock(&video_evt_mutex_lock);
    LogUtil::logD(TAG, {"dealVideoEvtLoop: video loop quit"});

    if (p_video_evt_queue != nullptr)
        delete p_video_evt_queue;
    p_video_evt_queue = nullptr;

    if (p_video_decoder != nullptr) {
        delete p_video_decoder;
        p_video_decoder = nullptr;
    }

    if (p_ffmpeg_core != nullptr)
        delete p_ffmpeg_core;
    p_ffmpeg_core = nullptr;

    pthread_mutex_destroy(&video_evt_mutex_lock);
    pthread_cond_destroy(&video_evt_cond_lock);
    pthread_mutex_destroy(&video_packet_mutex_lock);
    pthread_cond_destroy(&video_packet_cond_lock);
}

void VideoController::handlePlay() {
    if (media_state == STATE_PLAY) {
        LogUtil::logD(TAG, {"handlePlay: already in play state"});
    }else if (media_state == STATE_INITIALIZED || media_state == STATE_PAUSE) {
        media_state = STATE_PLAY;
        Msg msg{.what = MSG_PLAY};
        pthread_mutex_lock(&main_evt_mutex_lock);
        p_main_evt_queue->push(msg);
        pthread_cond_signal(&main_evt_cond_lock);
        pthread_mutex_unlock(&main_evt_mutex_lock);
    } else {
        LogUtil::logD(TAG, {"handlePlay: invalid state ", (const char *) media_state});
    }
}

void VideoController::handlePause() {
    if (media_state == STATE_PAUSE) {
        LogUtil::logD(TAG, {"handlePause: already in pause state"});
    }else if (media_state == STATE_PLAY) {
        media_state = STATE_PAUSE;
    } else {
        LogUtil::logD(TAG, {"handlePause: invalid state ", (const char *) media_state});
    }
}

void VideoController::handleStop() {
    if (media_state >= STATE_INITIALIZED && media_state < STATE_STOP) {
        media_state = STATE_STOP;
        Msg msg{.what = MSG_STOP};
        pthread_mutex_lock(&main_evt_mutex_lock);
        p_main_evt_queue->push(msg);
        pthread_cond_signal(&main_evt_cond_lock);
        pthread_mutex_unlock(&main_evt_mutex_lock);
    } else {
        LogUtil::logD(TAG, {"handleStop: invalid state ", (const char *) media_state});
    }
}

double VideoController::getSleepTime(double time_diff) {
    //returned value should be within 3ms
    //music goes ahead
    if (time_diff > THREE_MILL_SEC) {
        delay_time = delay_time * 2 / 3;
        if (delay_time < FORTY_MILL_SEC / 2) {
            delay_time = FORTY_MILL_SEC * 2 / 3;
        } else if (delay_time > FORTY_MILL_SEC * 2) {
            delay_time = FORTY_MILL_SEC * 2;
        }
    } else if (time_diff < -THREE_MILL_SEC) {
        //video goes ahead
        delay_time = delay_time * 3 / 2;
        if (delay_time < FORTY_MILL_SEC / 2) {
            delay_time = FORTY_MILL_SEC * 2 / 3;
        } else if (delay_time > FORTY_MILL_SEC * 2) {
            delay_time = FORTY_MILL_SEC * 2;
        }
    }
    //music goes ahead a little bit fast
    if (time_diff >= FIVE_HUNDRED_MILL_SEC) {
        delay_time = 0;
    } else if (time_diff <= -FIVE_HUNDRED_MILL_SEC) {
        //video goes a little bit fast
        delay_time = FORTY_MILL_SEC * 2;
    }
    //music goes too fast
    if (time_diff >= TEN_SEC) {
        pthread_mutex_lock(&video_packet_mutex_lock);
        if (p_video_packet_queue != nullptr)
            delete p_video_packet_queue;
        p_video_packet_queue = new std::queue<AVPacket>();
        pthread_cond_signal(&video_packet_cond_lock);
        pthread_mutex_unlock(&video_packet_mutex_lock);
        delay_time = FORTY_MILL_SEC;
    } else if (time_diff <= -TEN_SEC) {
        //video goes too fast
        pthread_mutex_lock(&audio_packet_mutex_lock);
        if (p_audio_packet_queue != nullptr)
            delete p_audio_packet_queue;
        p_audio_packet_queue = new std::queue<AVPacket>();
        pthread_cond_signal(&audio_packet_cond_lock);
        pthread_mutex_unlock(&audio_packet_mutex_lock);
        delay_time = FORTY_MILL_SEC;
    }
    return delay_time;
}

bool VideoController::init() {
    if (p_ffmpeg_core == nullptr) {
        LogUtil::logE(TAG, {"init: ffmpeg core is null"});
        return false;
    }
    pthread_t main_evt_thread;
    pthread_create(&main_evt_thread, nullptr, mainEvtLooper, this);
    pthread_setname_np(main_evt_thread, "main-event-thread");
    return true;
}

void VideoController::setPath(char *path) {
    if (path != nullptr && path[0] != '\0') {
        if (p_ffmpeg_core == nullptr) {
            p_ffmpeg_core = new FFMpegCore;
        }
        p_ffmpeg_core->setPath(path);
    }
}

static JNINativeMethod jni_methods[] = {
        {
                "nativeCreate",
                "()J",
                (void *) nCreate
        },
        {
                "nativeInit",
                "(J)Z",
                (void *) nInit
        },
        {
                "nativeSetPath",
                "(JLjava/lang/String;)V",
                (void *) nSetPath
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
                "nativeSeek",
                "(JF)V",
                (void *) nSeek
        },
        {
                "nativeSetListener",
                "(JLcom/lyzirving/flashvideo/core/VideoListenerAdapter;)V",
                (void *) nSetListener
        },
};

bool VideoController::registerSelf(JNIEnv *env) {
    int count = sizeof(jni_methods) / sizeof(jni_methods[0]);
    jclass javaClass = env->FindClass(CLASS);
    if(!javaClass) {
        LogUtil::logE(TAG, {"registerSelf: failed to find class ", CLASS});
        goto fail;
    }
    if (env->RegisterNatives(javaClass, jni_methods, count) < 0) {
        LogUtil::logE(TAG, {"registerSelf: failed to register native methods ", CLASS});
        goto fail;
    }
    return true;
    fail:
    return false;
}



