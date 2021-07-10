package com.lyzirving.flashvideo.edit.core;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import com.lyzirving.flashvideo.edit.MediaEditOp;
import com.lyzirving.flashvideo.edit.core.task.AudioToPcmTask;
import com.lyzirving.flashvideo.edit.core.task.MixPicAndMusicTask;
import com.lyzirving.flashvideo.edit.core.task.PcmToWavTask;
import com.lyzirving.flashvideo.opengl.core.EglCore;
import com.lyzirving.flashvideo.opengl.core.MediaConfig;
import com.lyzirving.flashvideo.util.FileUtil;
import com.lyzirving.flashvideo.util.LogUtil;
import com.lyzirving.flashvideo.util.TimeUtil;

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
    private Surface mInputSurface;
    private MediaCodec mVideoEncoder;
    private MediaMuxer mMuxer;

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

    public void startRecord(MediaEditOp op) {
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
        if (!prepareEncoder(MediaConfig.DEFAULT_VIDEO_WIDTH, MediaConfig.DEFAULT_VIDEO_HEIGHT)) {
            handleQuit();
            return;
        }
        if (!mEglCore.createWindowSurface(mInputSurface)) {
            handleQuit();
            return;
        }
        if (!mEglCore.makeCurrent()) {
            handleQuit();
            return;
        }
        mState = STATE_PREPARE;
        LogUtil.d(TAG, "handlePrepare: success");
    }

    private void handleQuit() {
        Objects.requireNonNull(Looper.myLooper(), "looper is null").quitSafely();
    }

    private void handleRecord(MediaEditOp op) {
        LogUtil.d(TAG, "handleRecord");
        mState = STATE_RECORDING;
        File musicSrc = new File((op.info.path));
        if (!musicSrc.exists()) {
            LogUtil.e(TAG, "handleRecord: " + op.info.path + " does not exist");
            handleQuit();
            return;
        }
        op.pcmTmpDir = FileUtil.MUSIC_CACHE_DIR + "/" + op.info.nameWithoutSuffix + ".pcm";
        AudioToPcmTask audioToPcm = new AudioToPcmTask(op);
        if (!audioToPcm.run()) {
            LogUtil.e(TAG, "handleRecord: failed to decode dst " + op.info.name + " to pcm");
            FileUtil.get().deleteSingleFile(op.pcmTmpDir);
            handleQuit();
            return;
        }
        op.wavTmpDir = FileUtil.MUSIC_CACHE_DIR + "/" + op.info.nameWithoutSuffix + ".wav";
        PcmToWavTask pcmToWav = new PcmToWavTask(op.pcmTmpDir, op.wavTmpDir, op.info);
        if (!pcmToWav.run()) {
            LogUtil.e(TAG, "handleRecord: failed to transfer pcm to wav");
            FileUtil.get().deleteSingleFile(op.pcmTmpDir);
            FileUtil.get().deleteSingleFile(op.wavTmpDir);
            handleQuit();
            return;
        }
        MixPicAndMusicTask mixTask = new MixPicAndMusicTask(op, mMuxer, mVideoEncoder, mEglCore);
        mixTask.run();
        FileUtil.get().deleteSingleFile(op.pcmTmpDir);
        FileUtil.get().deleteSingleFile(op.wavTmpDir);
        handleQuit();
    }

    private boolean prepareEncoder(int outputWidth, int outputHeight) {
        MediaFormat format = MediaFormat.createVideoFormat(MediaConfig.MIME_TYPE_VIDEO_H264, outputWidth, outputHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, MediaConfig.BIT_RATE_2_MILLION);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, MediaConfig.FRAME_RATE_30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, MediaConfig.I_FRAME_INTERVAL_2_SEC);
        LogUtil.d(TAG, "prepareEncoder: output video format = " + format);
        try {
            File outputRoot = new File(FileUtil.MOVIE_CACHE_DIR);
            if (!outputRoot.exists()) {
                outputRoot.mkdirs();
            }
            mMuxer = new MediaMuxer(new File(outputRoot, TimeUtil.getCurrentTimeStr() + ".mp4").getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mVideoEncoder = MediaCodec.createEncoderByType(MediaConfig.MIME_TYPE_VIDEO_H264);
            mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mVideoEncoder.createInputSurface();
            mVideoEncoder.start();
        } catch (Exception e) {
            LogUtil.e(TAG, "prepareEncoder: failed, exception msg = " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        LogUtil.d(TAG, "prepareEncoder: succeed");
        return true;
    }

    private void release() {
        LogUtil.d(TAG, "release");
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        try {
            if (mInputSurface != null) {
                mInputSurface.release();
            }
        } catch (Exception e) {
            LogUtil.i(TAG, "release: input surface exception = " + e.getMessage());
            e.printStackTrace();
        } finally {
            mInputSurface = null;
        }

        try {
            if (mVideoEncoder != null) {
                mVideoEncoder.stop();
                mVideoEncoder.release();
            }
        } catch (Exception e) {
            LogUtil.i(TAG, "release: video encoder exception = " + e.getMessage());
            e.printStackTrace();
        } finally {
            mVideoEncoder = null;
        }

        try {
            if (mMuxer != null) {
                mMuxer.release();
            }
        } catch (Exception e) {
            LogUtil.i(TAG, "release: media muxer exception = " + e.getMessage());
            e.printStackTrace();
        } finally {
            mMuxer = null;
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
                        tmp.handleRecord((MediaEditOp) msg.obj);
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
