package com.lyzirving.flashvideo.record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.view.Surface;

import com.lyzirving.flashvideo.util.LogUtil;
import com.lyzirving.flashvideo.util.TimeUtil;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * @author lyzirving
 */
public class RecordEncoder {
    private static final String TAG = "RecordEncoder";
    private static final int DECODE_TIME_OUT = 10000;
    /**
     * mime type for h264 data stream
     */
    private static String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 30;
    private static final int I_FRAME_INTERVAL = 5;
    private static final int BIT_RATE = 64000000;

    private int mTrackIndex;
    private MediaCodec.BufferInfo mBufferInfo;
    private Surface mInputSurface;
    private MediaMuxer mMuxer;
    private MediaCodec mEncoder;
    private boolean mMuxerStart;

    public Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * If endOfStream is not set, this method returns when there is no more data to drain.
     * If eos is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * This method does not record audio.
     * @param endOfStream indicate whether we finish the stream by program
     */
    public void drain(boolean endOfStream) {
        if (endOfStream) {
            LogUtil.d(TAG, "drain: signal end of stream on input");
            mEncoder.signalEndOfInputStream();
        }
        int status;
        ByteBuffer encodedBuffer;
        while (true) {
            status = mEncoder.dequeueOutputBuffer(mBufferInfo, DECODE_TIME_OUT);
            //the call is timed out
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    LogUtil.d(TAG, "drain: no output available, quit tht spinning");
                    break;
                } else {
                    LogUtil.d(TAG, "drain: no output available, spinning to wait EOS");
                }
            } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //in this case, output format is changed
                if (mMuxerStart) {
                    throw new RuntimeException("drain: format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                LogUtil.d(TAG, "drain: encoder has changed to new format, " + newFormat);
                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStart = true;
            } else if (status < 0) {
                LogUtil.d(TAG, "drain: unexpected result from dequeueOutputBuffer " + status + ", continue spinning");
            } else {
                encodedBuffer = mEncoder.getOutputBuffer(status);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status, just ignore it.
                    LogUtil.d(TAG, "drain: ignore INFO_OUTPUT_FORMAT_CHANGED state");
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    if (!mMuxerStart) {
                        throw new RuntimeException("drain: muxer has not started");
                    }
                    assert encodedBuffer != null;
                    encodedBuffer.position(mBufferInfo.offset);
                    encodedBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                    mMuxer.writeSampleData(mTrackIndex, encodedBuffer, mBufferInfo);
                }
                mEncoder.releaseOutputBuffer(status, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        LogUtil.d(TAG, "drain: reached end of stream unexpectedly");
                    } else {
                        LogUtil.d(TAG, "end of stream reached");
                    }
                    //go out of while spinning
                    break;
                }
            }
        }
    }

    public boolean prepare(int width, int height, String outputRootDir) {
        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        LogUtil.d(TAG, "prepare: format = " + format);
        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mEncoder.createInputSurface();
            mEncoder.start();
            File outputRoot = new File(outputRootDir);
            if (!outputRoot.exists()) {
                outputRoot.mkdirs();
            }
            mMuxer = new MediaMuxer(new File(outputRootDir, TimeUtil.getCurrentTimeStr() + ".mp4").getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mTrackIndex = -1;
            mMuxerStart = false;
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
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mMuxer != null) {
            // TODO: stop() throws an exception if you haven't fed it any data.  Keep track
            //       of frames submitted, and don't call stop() if we haven't written anything.
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }
}
