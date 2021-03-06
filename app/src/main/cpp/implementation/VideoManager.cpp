#include "VideoManager.h"
#include "JavaCallbackUtil.h"

#include <string>
#include <unistd.h>
#include <sys/syscall.h>

#define TAG "Native_VideoManager"
#define MAX_PACKET_COUNT (1024 * 100)
#define FIVE_SEC 5
#define FIVE_MIN (5 * 60)
#define THREE_HUNDRED_MILL_SEC 0.3
#define FORTY_MILL_SEC 0.04
#define THREE_MILL_SEC 0.003
#define ONE_MIC0_SEC 1000000
#define JAVA_CLASS "com/lyzirving/flashvideo/player/FlashVideoPlayer"

static JavaVM* glob_jvm = nullptr;
static std::map<jlong, jobject> glob_listeners;

static jlong nConstruct(JNIEnv *env, jclass clazz) {
    if (glob_jvm == nullptr) {
        env->GetJavaVM(&glob_jvm);
    }
    return reinterpret_cast<jlong>(new VideoManager);
}

static void nInit(JNIEnv *env, jclass clazz, jlong pointer, jstring path, jobject listener) {
    auto* manager = reinterpret_cast<VideoManager *>(pointer);
    char* c_path = const_cast<char *>(env->GetStringUTFChars(path, 0));
    glob_listeners.insert(std::pair<jlong, jobject >(pointer, env->NewGlobalRef(listener)));
    manager->init(c_path);
    env->ReleaseStringUTFChars(path, c_path);
}

static void nPlay(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* manager = reinterpret_cast<VideoManager *>(pointer);
    manager->setState(STATE_PLAY);
}

static void nPause(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* manager = reinterpret_cast<VideoManager *>(pointer);
    manager->setState(STATE_PAUSE);
}

static void nStop(JNIEnv *env, jclass clazz, jlong pointer) {
    auto* manager = reinterpret_cast<VideoManager *>(pointer);
    manager->setState(STATE_STOP);
}

static void nSetProgress(JNIEnv *env, jclass clazz, jlong pointer, jfloat ratio) {
    auto* manager = reinterpret_cast<VideoManager *>(pointer);
    manager->setSeekRatio(ratio);
    manager->setState(STATE_SEEK);
}

static JNINativeMethod jni_methods[] = {
        {
                "nativeConstruct",
                "()J",
                (void *) nConstruct
        },
        {
                "nativeInit",
                "(JLjava/lang/String;Lcom/lyzirving/flashvideo/player/VideoListenerAdapter;)V",
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
               "nativeSetProgress",
               "(JF)V",
                (void *) nSetProgress
        }
};

void audioCallback(SLAndroidSimpleBufferQueueItf buf_queue_itf, void *args) {
    JNIEnv* env = nullptr;
    if (!JavaCallbackUtil::threadAttachJvm(glob_jvm, &env)) {
        LogUtil::logE(TAG, {"audioCallback: env failed to attach audio callback thread"});
        env = nullptr;
    }
    auto* manager = (VideoManager*)args;
    manager->dealAudioCallback(env);
    if (env != nullptr) {
        glob_jvm->DetachCurrentThread();
        env = nullptr;
    }
}

void *audioLoop(void *args) {
    auto* manager = static_cast<VideoManager *>(args);
    JNIEnv *env = nullptr;
    if (!JavaCallbackUtil::threadAttachJvm(glob_jvm, &env)) {
        LogUtil::logE(TAG, {"audioLoop: failed to attach thread to jvm"});
        return nullptr;
    }
    manager->dealAudioLoop(env);
    glob_jvm->DetachCurrentThread();
    return nullptr;
}

void *mainLoop(void *args) {
    auto* manager = static_cast<VideoManager *>(args);
    JNIEnv *env = nullptr;
    if (!JavaCallbackUtil::threadAttachJvm(glob_jvm, &env)) {
        LogUtil::logE(TAG, {"mainEvtLooper: failed to attach thread to jvm"});
        return nullptr;
    }
    manager->dealMainLoop(env);
    glob_jvm->DetachCurrentThread();
    return nullptr;
}

void *videoLoop(void *args) {
    auto* manager = static_cast<VideoManager *>(args);
    JNIEnv *env = nullptr;
    if (!JavaCallbackUtil::threadAttachJvm(glob_jvm, &env)) {
        LogUtil::logE(TAG, {"videoLoop: failed to attach thread to jvm"});
        return nullptr;
    }
    manager->dealVideoLoop(env);
    glob_jvm->DetachCurrentThread();
    return nullptr;
}

void VideoManager::releaseBlockForEnoughPacket() {
    if (block_enough_packet && !hasEnoughPacket()) {
        pthread_mutex_lock(&wait_mutex);
        block_enough_packet = false;
        pthread_cond_signal(&wait_cond);
        pthread_mutex_unlock(&wait_mutex);
    }
}

void VideoManager::collectPacket(AVPacket* in_packet) {
    int res = av_read_frame(engine->p_fmt_ctx, in_packet);
    if (AVERROR_EOF == res) {
        LogUtil::logD(TAG, {"collectPacket: end of stream"});
        flag_eof = true;
    } else if (JNI_OK != res) {
        LogUtil::logD(TAG, {"collectPacket: not ok, res = ", std::to_string(res), ", current seek = ", std::to_string(seek_ratio)});
        av_packet_unref(in_packet);
    } else if (in_packet->stream_index == engine->audio_index) {
        audio_packet_queue->enqueue(*in_packet);
    } else if (in_packet->stream_index == engine->video_index) {
        video_packet_queue->enqueue(*in_packet);
    }
}

void VideoManager::dealAudioLoop(JNIEnv *env) {
    bool has_send_data = false;
    AudioData* data = nullptr;
    AVPacket* audio_packet = nullptr;
    jobject listener = nullptr;
    LogUtil::logD(TAG, {"dealAudioLoop: tid = ", std::to_string((int)syscall(SYS_gettid))});
    for(;;) {
        if (stateEqual(STATE_STOP)) {
            break;
        } else if (stateEqual(STATE_PAUSE)) {
            has_send_data = false;
        } else if (stateEqual(STATE_SEEK)) {
            if (!audio_held_by_seek) {
                LogUtil::logD(TAG, {"dealAudioLoop: process seek"});
                has_send_data = false;
                pthread_mutex_lock(&wait_mutex);
                block_enough_packet = false;
                pthread_cond_signal(&wait_cond);
                pthread_mutex_unlock(&wait_mutex);
                audio_held_by_seek = true;
            }
        } else if (stateEqual(STATE_PLAY) && !has_send_data) {
            audio_held_by_seek = false;
            audio_packet = audio_packet_queue->dequeue();
            releaseBlockForEnoughPacket();
            if (audio_packet != nullptr) {
                data = audio_player->decodePacket(audio_packet);
                if (data->buf_size > 0) {
                    if(data->now_time < main_clock) data->now_time = main_clock;
                    main_clock = data->now_time;
                    if (main_clock - last_main_clock > 1) {
                        last_main_clock = main_clock;
                        if (listener == nullptr)
                            listener = JavaCallbackUtil::findListener(&glob_listeners, reinterpret_cast<jlong>(this));
                        if (listener != nullptr)
                            JavaCallbackUtil::callMediaTickTime(env, listener, main_clock);
                    }
                    has_send_data = audio_player->enqueueAudio(data);
                }
                av_packet_unref(audio_packet);
            }
        }
    }
    pthread_mutex_lock(&wait_mutex);
    LogUtil::logD(TAG, {"dealAudioLoop: audio is quitting"});
    if (audio_player != nullptr) {
        delete audio_player;
        audio_player = nullptr;
    }
    audio_is_quitting = false;
    pthread_cond_signal(&wait_cond);
    pthread_mutex_unlock(&wait_mutex);
    LogUtil::logD(TAG, {"dealAudioLoop: audio finishes quitting"});
}

void VideoManager::dealAudioCallback(JNIEnv* env) {
    AudioData* data = nullptr;
    bool loopAgain = true;
    while (stateEqual(STATE_PLAY) && loopAgain) {
        AVPacket* audio_packet = audio_packet_queue->dequeue();
        releaseBlockForEnoughPacket();
        if (audio_packet != nullptr) {
            data = audio_player->decodePacket(audio_packet);
            if (data->buf_size > 0) {
                main_clock += data->buf_size / ((double)(audio_player->p_audio_decoder->out_sample_rate * 2 * 2));
                //give java callback when the audio lasts 1 second
                if (main_clock - last_main_clock > 1) {
                    last_main_clock = main_clock;
                    jobject listener;
                    if ((listener = JavaCallbackUtil::findListener(&glob_listeners, reinterpret_cast<jlong>(this))) != nullptr)
                        JavaCallbackUtil::callMediaTickTime(env, listener, main_clock);
                }
                loopAgain = !audio_player->enqueueAudio(data);
            }
            av_packet_unref(audio_packet);
        }
    }
}

void VideoManager::dealMainLoop(JNIEnv *env) {
    jobject listener = nullptr;
    pthread_t audio_thread, video_thread;
    auto* in_packet = (AVPacket *)av_malloc(sizeof(AVPacket));
    engine = new FFMpegCore;
    engine->setPath(path);
    if (!engine->createEnv(FFMpegCore::MODE_VIDEO)) {
        goto quit;
    }
    audio_player = new AudioPlayer;
    if (!audio_player->init(engine->p_fmt_ctx, engine->audio_index)) {
        goto quit;
    }
    if (!audio_player->createBufQueuePlayer(audioCallback, this)) {
        goto quit;
    }
    video_decoder = new VideoDecoder;
    if (!video_decoder->init(engine->p_fmt_ctx, engine->video_index)) {
        goto quit;
    }
    state = STATE_INITIALIZED;
    listener = JavaCallbackUtil::findListener(&glob_listeners, reinterpret_cast<jlong>(this));
    if (listener != nullptr)
        JavaCallbackUtil::callMediaPrepare(env, listener, video_decoder->duration,
                video_decoder->p_codec_ctx->width, video_decoder->p_codec_ctx->height);

    pthread_create(&audio_thread, nullptr, audioLoop, this);
    pthread_create(&video_thread, nullptr, videoLoop, this);
    LogUtil::logD(TAG, {"dealMainLoop: tid = ", std::to_string((int)syscall(SYS_gettid))});
    for (;;) {
        if (stateEqual(STATE_SEEK)) {
            if (audio_held_by_seek && video_held_by_seek) {
                LogUtil::logD(TAG, {"dealMainLoop: process seek"});
                flag_eof = false;
                audio_player->setPauseState();
                audio_packet_queue->clear();
                video_packet_queue->clear();
                last_main_clock = video_clock = main_clock = 0;
                last_video_pts = 0;
                engine->seekTo(seek_ratio);
                avcodec_flush_buffers(audio_player->p_audio_decoder->p_audio_codec_ctx);
                audio_player->clearBufferQueue();
                avcodec_flush_buffers(video_decoder->p_codec_ctx);
                setState(STATE_PLAY);
                continue;
            }
        } else if (stateEqual(STATE_STOP)) {
            goto quit;
        }
        //check this in every loop
        if (!stateEqual(STATE_SEEK) && stateMoreThanAndEqual(STATE_INITIALIZED)
                && stateLessThan(STATE_STOP) && !flag_eof) {
            collectPacket(in_packet);
            if (hasEnoughPacket()) {
                pthread_mutex_lock(&wait_mutex);
                block_enough_packet = true;
                LogUtil::logD(TAG, {"dealMainLoop: wait"});
                pthread_cond_wait(&wait_cond, &wait_mutex);
                LogUtil::logD(TAG, {"dealMainLoop: resume"});
                pthread_mutex_unlock(&wait_mutex);
            }
        }
    }
    quit:
    LogUtil::logD(TAG, {"dealMainLoop: quit"});
}

void VideoManager::dealVideoLoop(JNIEnv *env) {
    AVPacket* packet = nullptr;
    double video_sleep_time = 0;
    jobject listener = nullptr;
    auto* yuv420 = new DataYUV420;
    LogUtil::logD(TAG, {"dealVideoLoop: tid = ", std::to_string((int)syscall(SYS_gettid))});
    for(;;) {
        if (stateEqual(STATE_STOP)) {
            if (audio_is_quitting) {
                pthread_mutex_lock(&wait_mutex);
                //double check
                if (audio_is_quitting) {
                    LogUtil::logD(TAG, {"dealVideoLoop: wait audio to quit"});
                    pthread_cond_wait(&wait_cond, &wait_mutex);
                }
                pthread_mutex_unlock(&wait_mutex);
            }
            break;
        } else if(stateEqual(STATE_SEEK)) {
            if (!video_held_by_seek) {
                LogUtil::logD(TAG, {"dealVideoLoop: process seek"});
                pthread_mutex_lock(&wait_mutex);
                block_enough_packet = false;
                pthread_cond_signal(&wait_cond);
                pthread_mutex_unlock(&wait_mutex);
                video_held_by_seek = true;
            }
        } else if (stateEqual(STATE_PLAY)) {
            video_held_by_seek = false;
            packet = video_packet_queue->dequeue();
            releaseBlockForEnoughPacket();
            if (packet != nullptr) {
                yuv420->width = -1;
                video_decoder->decodePacket(packet, yuv420);
                if (yuv420->width > 0) {
                    updateVideoClock(yuv420->pts);
                    video_sleep_time = getSleepTime(main_clock - video_clock);
                    usleep(video_sleep_time * ONE_MIC0_SEC);
                    if ((listener = JavaCallbackUtil::findListener(
                            &glob_listeners,reinterpret_cast<jlong>(this))) != nullptr) {
                        JavaCallbackUtil::callVideoFrame(
                                env, listener, yuv420->width, yuv420->height,
                                yuv420->y_data, yuv420->u_data, yuv420->v_data);
                    }
                }
                av_frame_unref(video_decoder->p_raw_frame);
                av_frame_unref(video_decoder->p_yuv_420_frame);
                av_packet_unref(packet);
            }
        }
    }
    LogUtil::logD(TAG, {"dealVideoLoop: quit"});
    if (listener == nullptr)
        listener = JavaCallbackUtil::findListener(&glob_listeners, reinterpret_cast<jlong>(this));
    if (listener != nullptr)
        JavaCallbackUtil::callMediaStop(env, listener);
    if ((listener = JavaCallbackUtil::removeListener(&glob_listeners, reinterpret_cast<jlong>(this))) != nullptr)
        env->DeleteGlobalRef(listener);
    if (video_decoder != nullptr) {
        delete video_decoder;
        video_decoder = nullptr;
    }
    if (engine != nullptr) {
        delete engine;
        engine = nullptr;
    }
    delete this;
}

double VideoManager::getSleepTime(double time_diff) {
    //returned value should be within 3ms
    //music goes ahead
    LogUtil::logD(TAG, {"getSleepTime: diff ", std::to_string(time_diff)});
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
    if (time_diff >= THREE_HUNDRED_MILL_SEC) {
        delay_time = 0;
    } else if (time_diff <= -THREE_HUNDRED_MILL_SEC) {
        //video goes a little bit fast
        delay_time = FORTY_MILL_SEC * 2;
    }
    //music goes too fast
    if (time_diff >= FIVE_SEC) {
        video_packet_queue->clear();
        pthread_mutex_lock(&wait_mutex);
        block_enough_packet = false;
        pthread_cond_signal(&wait_cond);
        pthread_mutex_unlock(&wait_mutex);
        delay_time = FORTY_MILL_SEC;
    } else if (time_diff <= -FIVE_SEC) {
        //video goes too fast
        audio_packet_queue->clear();
        pthread_mutex_lock(&wait_mutex);
        block_enough_packet = false;
        pthread_cond_signal(&wait_cond);
        pthread_mutex_unlock(&wait_mutex);
        delay_time = FORTY_MILL_SEC;
    }
    LogUtil::logD(TAG, {"getSleepTime: diff sleep time ", std::to_string(delay_time)});
    return delay_time;
}

bool VideoManager::hasEnoughPacket() {
    bool reach_max_size = (video_packet_queue->size() + audio_packet_queue->size()) > MAX_PACKET_COUNT;
    AVPacket* back_packet = nullptr;
    if ((back_packet = audio_packet_queue->back()) == nullptr) {
        return reach_max_size;
    }
    double decode_last_time = back_packet->pts * av_q2d(audio_player->p_audio_decoder->time_base);
    bool reach_max_time = decode_last_time - main_clock > FIVE_MIN;
    return reach_max_size || reach_max_time;
}

void VideoManager::init(char* in_path) {
    if(in_path == nullptr || strlen(in_path) == 0) {
        LogUtil::logE(TAG, {"init: input path is empty"});
        return;
    }
    path = static_cast<char *>(malloc(strlen(in_path) + 1));
    strncpy(path, in_path, strlen(in_path) + 1);

    pthread_t thread;
    pthread_create(&thread, nullptr, mainLoop, this);
    pthread_setname_np(thread, "main-loop");
}

bool VideoManager::registerSelf(JNIEnv *env) {
    int count = sizeof(jni_methods) / sizeof(jni_methods[0]);
    jclass javaClass = env->FindClass(JAVA_CLASS);
    if(!javaClass) {
        LogUtil::logE(TAG, {"registerSelf: failed to find class ", JAVA_CLASS});
        goto fail;
    }
    if (env->RegisterNatives(javaClass, jni_methods, count) < 0) {
        LogUtil::logE(TAG, {"registerSelf: failed to register native methods ", JAVA_CLASS});
        goto fail;
    }
    LogUtil::logD(TAG, {"success to register class: ", JAVA_CLASS, ", method count ", std::to_string(count)});
    return true;
    fail:
    return false;
}

void VideoManager::setSeekRatio(float ratio) {
    if (ratio < 0)
        ratio = 0;
    else if (ratio > 1)
        ratio = 1;
    seek_ratio = ratio;
}

void VideoManager::setState(MediaState new_state) {
    if (new_state == STATE_PLAY) {
        audio_player->setPlayState();
    } else if (new_state == STATE_STOP) {
        if (block_enough_packet) {
            pthread_mutex_lock(&wait_mutex);
            block_enough_packet = false;
            pthread_cond_signal(&wait_cond);
            pthread_mutex_unlock(&wait_mutex);
        }
        audio_is_quitting = true;
        audio_player->setStopState();
    }
    //player's state must be changed before the state field
    state = new_state;
}

bool VideoManager::stateEqual(MediaState in_state) {
    return state == in_state;
}

bool VideoManager::stateMoreThanAndEqual(MediaState in_state) {
    return state >= in_state;
}

bool VideoManager::stateLessThan(MediaState in_state) {
    return state < in_state;
}

bool VideoManager::stateLessThanAndEqual(MediaState in_state) {
    return state <= in_state;
}

void VideoManager::updateVideoClock(long in_pts) {
    //first frame
    if (last_video_pts == 0) {
        last_video_pts = in_pts;
        video_clock = main_clock;
    } else if (in_pts != last_video_pts) {
        video_clock = in_pts * av_q2d(engine->p_fmt_ctx->streams[engine->video_index]->time_base);
        last_video_pts = in_pts;
    }
}


