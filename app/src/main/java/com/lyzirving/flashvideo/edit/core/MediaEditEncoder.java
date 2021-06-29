package com.lyzirving.flashvideo.edit.core;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.view.Surface;

import com.lyzirving.flashvideo.opengl.core.MediaConfig;
import com.lyzirving.flashvideo.util.FileUtil;
import com.lyzirving.flashvideo.util.LogUtil;
import com.lyzirving.flashvideo.util.TimeUtil;

import java.io.File;

/**
 * @author lyzirving
 */
public class MediaEditEncoder {
    private static final String TAG = "MediaEditEncoder";

    private MediaCodec.BufferInfo mBufferInfo;
    private Surface mInputSurface;
    private MediaCodec mVideoEncoder;
    private MediaMuxer mMuxer;

    public Surface getInputSurface() {
        return mInputSurface;
    }

    public boolean prepare(int width, int height) {
        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MediaConfig.MIME_TYPE_VIDEO_H264, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, MediaConfig.BIT_RATE_2_MILLION);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, MediaConfig.FRAME_RATE_30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, MediaConfig.I_FRAME_INTERVAL_2_SEC);
        LogUtil.d(TAG, "prepare: format = " + format);
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
            LogUtil.e(TAG, "prepare: exception happens, msg = " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void release() {
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mMuxer != null) {
            try {
                /**
                 * the MediaMuxer will throw an Exception if you start a Muxer, but don't
                 * feed any data to it
                 */
                mMuxer.stop();
                mMuxer.release();
            } catch (Exception e) {
                LogUtil.d(TAG, "release: " + e.getMessage());
                e.printStackTrace();
            } finally {
                mMuxer = null;
            }
        }
    }
}
