package com.lyzirving.flashvideo.edit.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.lyzirving.flashvideo.edit.MusicEditOp;
import com.lyzirving.flashvideo.opengl.core.EglCore;
import com.lyzirving.flashvideo.opengl.core.MediaConfig;
import com.lyzirving.flashvideo.opengl.filter.ShowFilter;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.opengl.util.VertexUtil;
import com.lyzirving.flashvideo.util.ComponentUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.ref.SoftReference;
import java.util.Objects;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * steps to combine the picture and music:
 *  1 decode the music into pcm by selected range, store the pcm in tmp file;
 *  2 MediaMuxer creates a video track by MediaFormat of video;
 *  3 MediaMuxer creates a audio track by MediaFormat of mp3;
 *  4 encode the slice of pcm, and fill the audio track by encoded music data;
 *  5 draw the image onto texture, so the video encoder is fed by the data, and encode these data;
 *  6 use the encoded video data to feed the video track;
 *
 * @author lyzirving
 */
public class MediaEditor extends Thread {
    private static final String TAG = "MediaEditor";
    private static final int MSG_PREPARE = 1;
    private static final int MSG_START_RECORD = 2;
    private static final int MSG_QUIT = 3;

    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARE = 1;
    public static final int STATE_RECORDING = 2;
    public static final int STATE_QUIT = 3;

    @IntDef({STATE_IDLE, STATE_PREPARE, STATE_RECORDING, STATE_QUIT})
    @Retention(CLASS)
    @Target({PARAMETER,METHOD,LOCAL_VARIABLE,FIELD})
    public @interface EditorState {}

    private final Object mLock = new Object();
    private MediaEditorHandler mHandler;
    private EglCore mEglCore;
    private MediaEditEncoder mEncoder;
    private ShowFilter mFilter;

    @EditorState
    private int mState;

    public MediaEditor() {
        super();
        mState = STATE_IDLE;
    }

    @Override
    public void run() {
        super.run();
        editLoop();
    }

    public void prepare() {
        if (mState != STATE_IDLE) {
            LogUtil.d(TAG, "prepare: invalid state: " + getStateStr(mState));
            return;
        }
        start();
        if (mHandler == null) {
            synchronized (mLock) {
                try {
                    mLock.wait();
                    mHandler.obtainMessage(MSG_PREPARE).sendToTarget();
                } catch (Exception e) {
                    LogUtil.e(TAG, "prepare: exception happens when waiting, msg = " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void quit() {
        if (mState < STATE_PREPARE) {
            LogUtil.d(TAG, "quit: invalid state: " + getStateStr(mState));
            return;
        }
        Objects.requireNonNull(mHandler, "handle is null").sendEmptyMessage(MSG_QUIT);
    }

    public void startRecord(MusicEditOp op) {
        if (mState != STATE_PREPARE) {
            LogUtil.d(TAG, "startRecord: invalid state: " + getStateStr(mState));
            return;
        }
        Objects.requireNonNull(mHandler, "handle is null").obtainMessage(MSG_START_RECORD, op).sendToTarget();
    }

    private void editLoop() {
        LogUtil.d(TAG, "editLoop: enter");
        Looper.prepare();
        mHandler = new MediaEditorHandler(Looper.myLooper(), this);
        synchronized (mLock) {
            mLock.notifyAll();
        }
        Looper.loop();
        LogUtil.d(TAG, "editLoop: quit");
        release();
    }

    private String getStateStr(@EditorState int state) {
        switch (state) {
            case STATE_IDLE: {
                return "idle";
            }
            case STATE_PREPARE: {
                return "prepare";
            }
            case STATE_RECORDING: {
                return "recording";
            }
            case STATE_QUIT: {
                return "quit";
            }
            default: {
                return "default";
            }
        }
    }

    private void handlePrepare() {
        mEglCore = new EglCore();
        if (!mEglCore.prepare(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3)) {
            handleQuit();
            return;
        }
        mEncoder = new MediaEditEncoder();
        if (!mEncoder.prepare(MediaConfig.DEFAULT_VIDEO_WIDTH, MediaConfig.DEFAULT_VIDEO_HEIGHT)) {
            handleQuit();
            return;
        }
        if (!mEglCore.createWindowSurface(mEncoder.getInputSurface())) {
            handleQuit();
            return;
        }
        if (!mEglCore.makeCurrent()) {
            handleQuit();
            return;
        }
        mFilter = new ShowFilter(ComponentUtil.get().ctx());
        mFilter.setOutputSize(MediaConfig.DEFAULT_VIDEO_WIDTH, MediaConfig.DEFAULT_VIDEO_HEIGHT);
        mFilter.setVertexCoordinates(VertexUtil.get().getDefaultVertex());
        mFilter.setTextureCoordinates(TextureUtil.get().getDefaultTextureCoordinates());
        mFilter.init();
        mState = STATE_PREPARE;
        LogUtil.d(TAG, "handlePrepare: success");
    }

    private void handleQuit() {
        Objects.requireNonNull(Looper.myLooper(), "looper is null").quitSafely();
    }

    private void handleRecord(MusicEditOp op) {
        LogUtil.d(TAG, "handleRecord");
        mState = STATE_RECORDING;
        File musicSrc = new File((op.info.path));
        if (!musicSrc.exists()) {
            LogUtil.e(TAG, "handleRecord: " + op.info.path + " does not exist");
            handleQuit();
            return;
        }
        if (!MediaUtil.decodeMusicToPcm(op)) {
            LogUtil.e(TAG, "handleRecord: failed to decode music to pcm");
            handleQuit();
            return;
        }
        if (!MediaUtil.decodePcmToWav(op)) {
            LogUtil.e(TAG, "handleRecord: failed to decode pcm to wav");
            handleQuit();
            return;
        }
    }

    private void release() {
        LogUtil.d(TAG, "release");
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mFilter != null) {
            mFilter.release();
            mFilter = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        if (mEncoder != null) {
            mEncoder.release();
            mEncoder = null;
        }
        mState = STATE_QUIT;
    }

    private static class MediaEditorHandler extends Handler {
        private SoftReference<MediaEditor> mRef;

        MediaEditorHandler(Looper looper, MediaEditor editor) {
            super(looper);
            mRef = new SoftReference<>(editor);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            MediaEditor tmp;
            if ((tmp = mRef.get()) == null) {
                LogUtil.e(TAG, "handleMessage: editor is null");
            } else {
                switch (msg.what) {
                    case MSG_PREPARE: {
                        tmp.handlePrepare();
                        break;
                    }
                    case MSG_START_RECORD: {
                        tmp.handleRecord((MusicEditOp) msg.obj);
                        break;
                    }
                    case MSG_QUIT: {
                        tmp.handleQuit();
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        }
    }
}
