package com.lyzirving.flashvideo.edit.core;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;

import com.lyzirving.flashvideo.edit.MediaEditOp;
import com.lyzirving.flashvideo.edit.core.MediaWrapper;
import com.lyzirving.flashvideo.opengl.core.MediaConfig;
import com.lyzirving.flashvideo.util.LogUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

/**
 * @author lyzirving
 */
public class MediaUtil {
    private static final String TAG = "MediaUtil";

    public static boolean buildExtractor(MediaWrapper wrapper, String inputPath) {
        wrapper.extractor = new MediaExtractor();
        try {
            wrapper.extractor.setDataSource(inputPath);
            wrapper.mediaTrack = selectTrack(wrapper.extractor, wrapper.mediaType);
            if (wrapper.mediaTrack < 0) {
                LogUtil.e(TAG, "buildExtractor: failed, select invalid track, input media type = " + wrapper.mediaType);
                return false;
            }
            wrapper.extractor.selectTrack(wrapper.mediaTrack);
        } catch (Exception e) {
            LogUtil.e(TAG, "buildExtractor: failed, input media type = " + wrapper.mediaType + ", exception = " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean buildDecoder(MediaWrapper wrapper) {
        try {
            MediaFormat mediaFormat = wrapper.extractor.getTrackFormat(wrapper.mediaTrack);
            LogUtil.i(TAG, "buildDecoder: media format = " + mediaFormat);

            wrapper.op.info.maxBufferSize = 100 * 1000;
            if (mediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                wrapper.op.info.maxBufferSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                LogUtil.i(TAG, "buildDecoder: max buffer size = " + wrapper.op.info.maxBufferSize);
            }
            wrapper.op.info.sampleRate = MediaConfig.DEFAULT_SAMPLE_RATE;
            if (mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                wrapper.op.info.sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                LogUtil.i(TAG, "buildDecoder: sample rate = " + wrapper.op.info.sampleRate);
            }
            wrapper.op.info.channelCount = MediaConfig.DEFAULT_CHANNEL_COUNT;
            if (mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                wrapper.op.info.channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                LogUtil.i(TAG, "buildDecoder: channel count = " + wrapper.op.info.channelCount);
            }
            wrapper.op.info.channelMask = AudioFormat.CHANNEL_IN_STEREO;
            if (mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_MASK)) {
                wrapper.op.info.channelMask = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_MASK);
                LogUtil.i(TAG, "buildDecoder: channel mask = " + wrapper.op.info.channelMask);
            }
            wrapper.op.info.encoding = AudioFormat.ENCODING_PCM_16BIT;
            if (mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                wrapper.op.info.encoding = mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
                LogUtil.i(TAG, "buildDecoder: encoding = " + wrapper.op.info.encoding);
            }
            wrapper.op.info.minBufferSize = AudioRecord.getMinBufferSize(wrapper.op.info.sampleRate,
                    wrapper.op.info.channelMask, wrapper.op.info.encoding);
            LogUtil.i(TAG, "buildDecoder: " + wrapper.op.info.toString());
            wrapper.byteBuffer = ByteBuffer.allocateDirect(wrapper.op.info.maxBufferSize);
            wrapper.decoder = MediaCodec.createDecoderByType(Objects.requireNonNull(
                    mediaFormat.getString((MediaFormat.KEY_MIME))));
            //set the decoder
            wrapper.decoder.configure(mediaFormat, null, null, 0);
            wrapper.decoder.start();
        } catch (Exception e) {
            LogUtil.i(TAG, "buildDecoder: failed, exception = " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean buildDefaultAACEncoder(MediaWrapper wrapper) {
        try {
            MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                    MediaConfig.DEFAULT_SAMPLE_RATE, MediaConfig.DEFAULT_CHANNEL_COUNT);
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, MediaConfig.BIT_RATE_64_KBPS);
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MediaConfig.DEFAULT_MAX_INPUT_SIZE);
            LogUtil.i(TAG, "buildDefaultAACEncoder: encode format = " + encodeFormat);

            wrapper.encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            wrapper.encoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            wrapper.encoder.start();
        } catch (Exception e){
            LogUtil.i(TAG, "buildDefaultAACEncoder: exception = " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean decodeToDst(String outputPath, MediaWrapper wrapper) {
        FileChannel writeChannel = null;
        ByteBuffer inputBuffer = null, outputBuffer = null;
        try {
            MediaEditOp op = wrapper.op;
            long startTimeUs = (long)(op.info.duration * op.leftAnchor * 1000 * 1000);
            long endTimeUs = (long)(op.info.duration * op.rightAnchor * 1000 * 1000);

            File outputFile = new File(outputPath);
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            writeChannel = new FileOutputStream(outputFile).getChannel();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int inputIndex, outputBufferIndex;
            long sampleTimeUs;
            byte[] bytesContent;

            while (true) {
                inputIndex = wrapper.decoder.dequeueInputBuffer(1_000);
                if (inputIndex >= 0) {
                    sampleTimeUs = wrapper.extractor.getSampleTime();
                    if (sampleTimeUs == -1) {
                        LogUtil.d(TAG, "decodeToDst: no more data");
                        break;
                    } else if (sampleTimeUs < startTimeUs) {
                        wrapper.extractor.advance();
                        continue;
                    } else if (sampleTimeUs > endTimeUs) {
                        LogUtil.d(TAG, "decodeToDst: current frame time is larger than required end time");
                        break;
                    }
                    bufferInfo.size = wrapper.extractor.readSampleData(wrapper.byteBuffer, 0);
                    bufferInfo.presentationTimeUs = sampleTimeUs;
                    bufferInfo.flags = wrapper.extractor.getSampleFlags();
                    if (wrapper.byteBuffer.remaining() <= 0) {
                        LogUtil.i(TAG, "decodeToDst: unexpected error content, continue");
                        continue;
                    }
                    //get frame content for MediaExtractor
                    bytesContent = new byte[wrapper.byteBuffer.remaining()];
                    wrapper.byteBuffer.get(bytesContent);
                    inputBuffer = wrapper.decoder.getInputBuffer(inputIndex);
                    //put the frame data into MediaCodec
                    inputBuffer.put(bytesContent);
                    wrapper.decoder.queueInputBuffer(inputIndex, 0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
                    //release last frame data
                    wrapper.extractor.advance();
                }
                outputBufferIndex = wrapper.decoder.dequeueOutputBuffer(bufferInfo, 1_000);
                while (outputBufferIndex >= 0) {
                    outputBuffer = wrapper.decoder.getOutputBuffer(outputBufferIndex);
                    writeChannel.write(outputBuffer);
                    wrapper.decoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = wrapper.decoder.dequeueOutputBuffer(bufferInfo, 1_000);
                }
            }
        } catch (Exception e) {
            LogUtil.i(TAG, "decodeToDst: exception = " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (writeChannel != null) {
                try {
                    writeChannel.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (inputBuffer != null) {
                inputBuffer.clear();
            }
            if (outputBuffer != null) {
                outputBuffer.clear();
            }
        }
        LogUtil.i(TAG, "decodeToDst: succeed");
        return true;
    }

    public static int selectTrack(MediaExtractor extractor, @MediaConfig.TYPE int type) {
        int numTracks = extractor.getTrackCount();
        int result = -1;
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (TextUtils.isEmpty(mime)) {
                continue;
            }
            if (type == MediaConfig.TYPE_MUSIC && mime.startsWith(MediaConfig.MUSIC_PREFIX)) {
                result = i;
                break;
            } else if (type == MediaConfig.TYPE_VIDEO && mime.startsWith(MediaConfig.VIDEO_PREFIX)) {
                result = i;
                break;
            }
        }
        return result;
    }
}
