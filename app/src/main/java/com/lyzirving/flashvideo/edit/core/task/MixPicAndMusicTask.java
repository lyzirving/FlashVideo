package com.lyzirving.flashvideo.edit.core.task;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.lyzirving.flashvideo.edit.MediaEditOp;
import com.lyzirving.flashvideo.edit.core.MediaUtil;
import com.lyzirving.flashvideo.edit.core.MediaWrapper;
import com.lyzirving.flashvideo.opengl.core.EglCore;
import com.lyzirving.flashvideo.opengl.core.MediaConfig;
import com.lyzirving.flashvideo.opengl.filter.BitmapFilter;
import com.lyzirving.flashvideo.util.AssetsManager;
import com.lyzirving.flashvideo.util.ComponentUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;

/**
 * @author lyzirving
 */
public class MixPicAndMusicTask implements MediaTask {
    private static final String TAG = "MixPicAndMusicTask";

    private final Object mLock = new Object();
    private volatile boolean mMuxerStart;
    private int mAudioMuxerTrack = MediaConfig.MEDIA_INVALID, mVideoMuxerTrack = MediaConfig.MEDIA_INVALID;

    private MediaEditOp mOp;
    private MediaMuxer mMediaMuxer;
    private MediaCodec mVideoEncoder;
    private EglCore mEglCore;

    public MixPicAndMusicTask(@NonNull MediaEditOp op, @NonNull MediaMuxer muxer,
                              @NonNull MediaCodec videoEncoder, @NonNull EglCore eglCore) {
        mOp = op;
        mMediaMuxer = muxer;
        mVideoEncoder = videoEncoder;
        mEglCore = eglCore;
    }

    @Override
    public boolean run() {
        AssetsManager.get().executeAsyncTask(new Runnable() {
            @Override
            public void run() {
                drainAudio();
            }
        });
        BitmapFilter filter = new BitmapFilter(ComponentUtil.get().ctx(), mOp.picResId);
        filter.prepareBeforeInit(ComponentUtil.get().ctx(), MediaConfig.DEFAULT_VIDEO_WIDTH, MediaConfig.DEFAULT_VIDEO_HEIGHT);
        filter.init();

        long durationPerFrameUs = 1000 * 1000 / MediaConfig.FRAME_RATE_30;
        long totalTimeUs = (long) ((mOp.rightAnchor - mOp.leftAnchor) * mOp.info.duration * 1000 * 1000);
        long currentTimeUs = 0, lastTimeUsKeyFrame = 0;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        try {
            while (currentTimeUs <= totalTimeUs) {
                drainVideo(false, bufferInfo);
                filter.draw(-1);
                mEglCore.setCurrentTimeStamp(currentTimeUs * 1000);
                mEglCore.swapBuffers();
                currentTimeUs += durationPerFrameUs;
                LogUtil.i(TAG, "run: current time = " + currentTimeUs + ", dst time = " + totalTimeUs);
            }
            LogUtil.i(TAG, "run: finish");
        } catch (Exception e) {
            LogUtil.i(TAG, "run: exception when write video, msg = " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            filter.release();
        }
        return true;
    }

    private void drainVideo(boolean endOfStream, MediaCodec.BufferInfo bufferInfo) {
        if (endOfStream) {
            LogUtil.d(TAG, "drainVideo: signal end of stream on input");
            mVideoEncoder.signalEndOfInputStream();
        }
        int outputBufferIndex;
        ByteBuffer encodedBuffer;
        while (true) {
            outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(bufferInfo, MediaConfig.BUFFER_WAIT_TIME_US);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    LogUtil.d(TAG, "drainVideo: no output available, quit tht spinning");
                    break;
                } else {
                    LogUtil.d(TAG, "drainVideo: no output available, spinning to wait EOS");
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //in this case, output format is changed
                if (mMuxerStart) {
                    throw new RuntimeException("drainVideo: format changed twice");
                }
                MediaFormat newFormat = mVideoEncoder.getOutputFormat();
                LogUtil.d(TAG, "drainVideo: get video format from encoder, " + newFormat);
                mVideoMuxerTrack = mMediaMuxer.addTrack(newFormat);
                int waitCount = 0;
                while(mAudioMuxerTrack == MediaConfig.MEDIA_INVALID) {
                    if (waitCount > 20) {
                        throw new RuntimeException("drainVideo: could not find audio track");
                    }
                    LogUtil.d(TAG, "drainVideo: wait for audio track");
                    try {
                        Thread.sleep(100);
                        waitCount++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                synchronized (mLock) {
                    LogUtil.d(TAG, "drainVideo: start muxer");
                    mMediaMuxer.start();
                    mMuxerStart = true;
                    mLock.notifyAll();
                }
            } else if (outputBufferIndex < 0) {
                LogUtil.d(TAG, "drainVideo: unexpected result from dequeueOutputBuffer " + outputBufferIndex + ", continue spinning");
            } else {
                encodedBuffer = mVideoEncoder.getOutputBuffer(outputBufferIndex);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status, just ignore it.
                    LogUtil.d(TAG, "drainVideo: ignore INFO_OUTPUT_FORMAT_CHANGED state");
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0 && mMuxerStart) {
                    encodedBuffer.position(bufferInfo.offset);
                    encodedBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    mMediaMuxer.writeSampleData(mVideoMuxerTrack, encodedBuffer, bufferInfo);
                }
                mVideoEncoder.releaseOutputBuffer(outputBufferIndex, false);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        LogUtil.d(TAG, "drainVideo: reached end of stream unexpectedly");
                    } else {
                        LogUtil.d(TAG, "drainVideo: end of stream reached");
                    }
                    //go out of while spinning
                    break;
                }
            }
        }
    }

    private void drainAudio() {
        MediaWrapper wrapper = new MediaWrapper(MediaConfig.TYPE_MUSIC);
        wrapper.op = mOp;
        if (!MediaUtil.buildExtractor(wrapper, wrapper.op.wavTmpDir)) {
            LogUtil.i(TAG, "drainAudio: build audio extractor failed");
            wrapper.release();
            return;
        }
        if (!MediaUtil.buildDefaultAACEncoder(wrapper)) {
            LogUtil.i(TAG, "drainAudio: build audio encoder failed");
            wrapper.release();
            return;
        }
        if (!drainAudioInner(wrapper)) {
            LogUtil.i(TAG, "drainAudio: failed");
        }
        wrapper.release();
    }

    private boolean drainAudioInner(MediaWrapper wrapper) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(MediaConfig.DEFAULT_MAX_INPUT_SIZE);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int flags, size;
        int inputBufferIndex, outputBufferIndex;
        ByteBuffer inputBuffer, outputBuffer;
        long sampleTime;
        boolean encodeDone = false;
        try {
            while (!encodeDone) {
                inputBufferIndex = wrapper.encoder.dequeueInputBuffer(10_000);
                if (inputBufferIndex >= 0) {
                    sampleTime = wrapper.extractor.getSampleTime();
                    if (sampleTime < 0) {
                        //end of stream
                        wrapper.encoder.queueInputBuffer(inputBufferIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        flags = wrapper.extractor.getSampleFlags();
                        size = wrapper.extractor.readSampleData(buffer, 0);
                        inputBuffer = wrapper.encoder.getInputBuffer(inputBufferIndex);
                        inputBuffer.clear();
                        inputBuffer.put(buffer);
                        inputBuffer.position(0);
                        wrapper.encoder.queueInputBuffer(inputBufferIndex, 0, size, sampleTime, flags);
                        wrapper.extractor.advance();
                    }
                }
                while (true) {
                    outputBufferIndex = wrapper.encoder.dequeueOutputBuffer(info, 10_000);
                    if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        LogUtil.d(TAG, "drainAudioInner: no output available, quit the output spinning");
                        break;
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (mMuxerStart) {
                            throw new RuntimeException("drainAudioInner: format changed twice");
                        }
                        MediaFormat newFormat = wrapper.encoder.getOutputFormat();
                        LogUtil.d(TAG, "drainAudioInner: get audio format from encoder, " + newFormat);
                        mAudioMuxerTrack = mMediaMuxer.addTrack(newFormat);
                        synchronized (mLock) {
                            while (!mMuxerStart || mVideoMuxerTrack == MediaConfig.MEDIA_INVALID) {
                                LogUtil.i(TAG, "drainAudioInner: wait for media muxer to start");
                                mLock.wait();
                                LogUtil.i(TAG, "drainAudioInner: wake up");
                            }
                        }
                        LogUtil.i(TAG, "drainAudioInner: start write audio data");
                    } else if (outputBufferIndex < 0) {
                        LogUtil.d(TAG, "drainAudioInner: unexpected result from dequeueOutputBuffer " + outputBufferIndex + ", continue spinning");
                    } else {
                        outputBuffer = wrapper.encoder.getOutputBuffer(outputBufferIndex);
                        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // The codec config data was pulled out and fed to the muxer when we got
                            // the INFO_OUTPUT_FORMAT_CHANGED status, just ignore it.
                            LogUtil.d(TAG, "drainAudioInner: ignore INFO_OUTPUT_FORMAT_CHANGED state");
                            info.size = 0;
                        }
                        if (info.size != 0 && mMuxerStart) {
                            assert outputBuffer != null;
                            LogUtil.d(TAG, "drainAudioInner: write sample data, size = " + info.size);
                            outputBuffer.position(info.offset);
                            outputBuffer.limit(info.offset + info.size);
                            mMediaMuxer.writeSampleData(mAudioMuxerTrack, outputBuffer, info);
                        }
                        wrapper.encoder.releaseOutputBuffer(outputBufferIndex, false);
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            LogUtil.d(TAG, "drainAudioInner: end of stream reached");
                            encodeDone = true;
                            break;
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            LogUtil.i(TAG, "drainAudioInner: exception happens = " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
