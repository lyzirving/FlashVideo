package com.lyzirving.flashvideo.record;

import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.lyzirving.flashvideo.opengl.EglCore;
import com.lyzirving.flashvideo.opengl.filter.ShowFilter;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.opengl.util.VertexUtil;
import com.lyzirving.flashvideo.util.ComponentUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Objects;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

/**
 * @author lyzirving
 */
public class RecordThread extends Thread {
    private static final String TAG = "RecordThread";
    public static final int MSG_PREPARE = 10;
    public static final int MSG_RECORDING = 11;
    public static final int MSG_STOP = 12;
    public static final int MSG_QUIT = 13;

    public static final int RECORD_STATE_IDLE = -1;
    public static final int RECORD_STATE_OFF = 0;
    public static final int RECORD_STATE_RUNNING = 1;
    public static final int RECORD_STATE_PREPARED = 2;
    public static final int RECORD_STATE_RECORDING = 3;
    public static final int RECORD_STATE_STOP_RECORDING = 4;

    @IntDef({RECORD_STATE_IDLE, RECORD_STATE_OFF, RECORD_STATE_RUNNING, RECORD_STATE_PREPARED,
            RECORD_STATE_RECORDING, RECORD_STATE_STOP_RECORDING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecordState{}

    private @RecordState volatile int mRecordState = RECORD_STATE_IDLE;
    private int mViewWidth, mViewHeight;
    private int mFrameTextureId;
    private EGLContext mSharedEglCtx;
    private RecordHandler mRecordHandler;
    private ShowFilter mFilter;

    private RecordEncoder mEncoder;
    private EglCore mEglCore;

    public int getRecordState() {
        return mRecordState;
    }

    public void prepareEnv(EGLContext sharedCtx, int viewWidth, int viewHeight) {
        mSharedEglCtx = sharedCtx;
        mViewWidth = viewWidth;
        mViewHeight = viewHeight;
    }

    @Override
    public void run() {
        super.run();
        if (mRecordState == RECORD_STATE_OFF) {
            mRecordState = RECORD_STATE_RUNNING;
            Looper.prepare();
            mRecordHandler = new RecordHandler(this);
            mRecordState = RECORD_STATE_PREPARED;
            LogUtil.d(TAG, "Record thread: start");
            Looper.loop();

            LogUtil.d(TAG, "Record thread: quit");
            mRecordState = RECORD_STATE_IDLE;
            mRecordHandler.removeCallbacksAndMessages(null);
            mRecordHandler = null;
            release();
        }
    }

    public void sendMsg(int what) {
        mRecordHandler.obtainMessage(what).sendToTarget();
    }

    public void sendMsg(int what, int arg1, int arg2, Object data) {
        mRecordHandler.obtainMessage(what, arg1, arg2, data).sendToTarget();
    }

    public void setState(@RecordState int state) {
        LogUtil.d(TAG, "setState: " + state);
        mRecordState = state;
    }

    public boolean stateEqual(@RecordState int state) {
        return mRecordState == state;
    }

    public boolean stateMoreThan(@RecordState int state) {
        return mRecordState > state;
    }

    public boolean stateBiggerAndEqual(@RecordState int state) {
        return mRecordState >= state;
    }

    private void handlePrepare() {
        LogUtil.d(TAG, "handlePrepare");
        //avoid repeated handle-prepare call
        if (mEncoder == null) {
            mEncoder = new RecordEncoder();
            boolean success = mEncoder.prepare(mViewWidth, mViewHeight, Objects.requireNonNull(ComponentUtil.get().getAppCtx().getExternalFilesDir(null)).getAbsolutePath() + "/video");
            if (!success) {
                LogUtil.e(TAG, "handlePrepare: failed to init encoder");
                handleQuit();
                return;
            }
            mEglCore = new EglCore();
            if (!mEglCore.prepare(mSharedEglCtx, EglCore.FLAG_RECORDABLE)) {
                LogUtil.e(TAG, "handlePrepare: failed to init egl core");
                handleQuit();
                return;
            }
            if (!mEglCore.createWindowSurface(mEncoder.getInputSurface())) {
                LogUtil.e(TAG, "handlePrepare: failed to create window surface");
                handleQuit();
                return;
            }
            mEglCore.makeCurrent();
            mFilter = new ShowFilter(ComponentUtil.get().getAppCtx());
            mFilter.setVertexCoordinates(VertexUtil.get().getDefaultVertex());
            mFilter.setTextureCoordinates(TextureUtil.get().getDefaultTextureCoordinates());
            mFilter.setOutputSize(mViewWidth, mViewHeight);
            mFilter.init();
            mRecordState = RECORD_STATE_RECORDING;
        }
    }

    private void handleQuit() {
        LogUtil.d(TAG, "handleQuit");
        mRecordState = RECORD_STATE_IDLE;
        Objects.requireNonNull(Looper.myLooper()).quitSafely();
    }

    private void handleRecording(int arg1, int arg2, Object data) {
        mFrameTextureId = arg1;
        long curTimeStamp = (Long) data;
        mEncoder.drain(false);
        //draw frame data
        mFilter.flip(false, arg2 > 0);
        mFilter.draw(mFrameTextureId);

        mEglCore.setCurrentTimeStamp(curTimeStamp);
        mEglCore.swapBuffers();
    }

    private void handleStop() {
        LogUtil.d(TAG, "handleStop");
        mEncoder.drain(true);
        handleQuit();
    }

    private void release() {
        if (mEncoder != null) {
            mEncoder.release();
            mEncoder = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        if (mFilter != null) {
            mFilter.release();
        }
    }

    private static class RecordHandler extends Handler {

        private WeakReference<RecordThread> mRef;

        RecordHandler(RecordThread thread) {
            super();
            mRef = new WeakReference<>(thread);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            RecordThread recordThread;
            if (mRef != null && (recordThread = mRef.get()) != null) {
                switch (msg.what) {
                    case MSG_PREPARE: {
                        recordThread.handlePrepare();
                        break;
                    }
                    case MSG_RECORDING: {
                        recordThread.handleRecording(msg.arg1, msg.arg2, msg.obj);
                        break;
                    }
                    case MSG_STOP: {
                        recordThread.handleStop();
                        break;
                    }
                    case MSG_QUIT: {
                        recordThread.handleQuit();
                        break;
                    }
                    default: {
                        break;
                    }
                }
            } else {
                LogUtil.e(TAG, "handleMessage: invalid ref");
            }
        }
    }
}
