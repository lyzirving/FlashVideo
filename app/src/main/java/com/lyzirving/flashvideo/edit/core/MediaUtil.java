package com.lyzirving.flashvideo.edit.core;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;

import com.lyzirving.flashvideo.edit.MusicEditOp;
import com.lyzirving.flashvideo.opengl.core.MediaConfig;
import com.lyzirving.flashvideo.util.FileUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

/**
 * @author lyzirving
 */
public class MediaUtil {
    private static final String TAG = "MediaUtil";

    public static boolean decodeMusicToPcm(MusicEditOp op) {
        if (op == null || op.info == null || TextUtils.isEmpty(op.info.path)) {
            LogUtil.e(TAG, "decodeMusicToPcm: invalid MusicEditOp");
            return false;
        }
        if (op.info.duration <= 0) {
            LogUtil.e(TAG, "decodeMusicToPcm: invalid duration = " + op.info.duration);
            return false;
        }
        if (op.left < 0 || op.left >= 1) {
            op.left = 0;
        }
        if (op.right > 1 || op.right <= 0) {
            op.right = 1;
        }
        try {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(op.info.path);
            int audioTrack = selectTrack(extractor, MediaConfig.TYPE_MUSIC);
            if (audioTrack < 0) {
                LogUtil.e(TAG, "decodeMusicToPcm: audio track invalid");
                return false;
            }
            extractor.selectTrack(audioTrack);
            long startPos = (long)(op.info.duration * op.left * 1000 * 1000);
            LogUtil.i(TAG, "decodeMusicToPcm: start pos = " + startPos);
            extractor.seekTo(startPos, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            MediaFormat audioFormat = extractor.getTrackFormat(audioTrack);
            LogUtil.i(TAG, "decodeMusicToPcm: audio format = " + audioFormat);
            int maxBufferSize = 100 * 1000;
            if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                LogUtil.i(TAG, "decodeMusicToPcm: get buffer size = " + maxBufferSize);
            }
            op.info.sampleRate = MediaConfig.DEFAULT_SAMPLE_RATE;
            if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                op.info.sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                LogUtil.i(TAG, "decodeMusicToPcm: get sample rate = " + op.info.sampleRate);
            }
            op.info.channelCount = MediaConfig.DEFAULT_CHANNEL_COUNT;
            if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                op.info.channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                LogUtil.i(TAG, "decodeMusicToPcm: get channel count = " + op.info.channelCount);
            }
            op.info.channelMask = AudioFormat.CHANNEL_IN_STEREO;
            if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_MASK)) {
                op.info.channelMask = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_MASK);
                LogUtil.i(TAG, "decodeMusicToPcm: get channel mask = " + op.info.channelMask);
            }
            op.info.encoding = AudioFormat.ENCODING_PCM_16BIT;
            if (audioFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                op.info.encoding = audioFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
                LogUtil.i(TAG, "decodeMusicToPcm: get encoding = " + op.info.encoding);
            }
            op.info.maxBufferSize = AudioRecord.getMinBufferSize(op.info.sampleRate, op.info.channelMask, op.info.encoding);
            LogUtil.i(TAG, "decodeMusicToPcm: " + op.info.toString());
            ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
            MediaCodec mediaCodec = MediaCodec.createDecoderByType(Objects.requireNonNull(
                    audioFormat.getString((MediaFormat.KEY_MIME))));
            //set the decoder
            mediaCodec.configure(audioFormat, null, null, 0);
            mediaCodec.start();

            long startTimeUs = (long)(op.info.duration * op.left * 1000 * 1000);
            long endTimeUs = (long)(op.info.duration * op.right * 1000 * 1000);
            op.pcmTmpDir = FileUtil.MUSIC_CACHE_DIR + "/" +op.info.nameWithoutSuffix + ".pcm";
            boolean  res = decodeMusicToPcmInner( op.pcmTmpDir, mediaCodec, extractor, startTimeUs,
                    endTimeUs, buffer);
            mediaCodec.stop();
            mediaCodec.release();
            extractor.release();
            return res;
        } catch (Exception e) {
            LogUtil.d(TAG, "decodeMusicToPcm: exception = " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean decodePcmToWav(MusicEditOp op) {
        if (TextUtils.isEmpty(op.pcmTmpDir)) {
            LogUtil.i(TAG, "decodePcmToWav: pcm tmp directory is empty");
            return false;
        }
        op.wavTmpDir = FileUtil.MUSIC_CACHE_DIR + "/" + op.info.nameWithoutSuffix + ".wav";
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen;
        long totalDataLen;
        long longSampleRate = op.info.sampleRate;
        int channels = op.info.channelCount;
        long byteRate = 16 * longSampleRate * channels / 8;
        byte[] data = new byte[op.info.maxBufferSize];
        try {
            in = new FileInputStream(op.pcmTmpDir);
            out = new FileOutputStream(op.wavTmpDir);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            writeWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            LogUtil.i(TAG, "decodePcmToWav: succeed");
        }  catch (Exception e) {
            LogUtil.i(TAG, "decodePcmToWav: exception msg = " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                LogUtil.i(TAG, "decodePcmToWav: close exception msg = " + e.getMessage());
                e.printStackTrace();
            }
        }
        return true;
    }

    private static boolean decodeMusicToPcmInner(String outputPath, MediaCodec mediaCodec,
                                                MediaExtractor extractor, long startTimeUs,
                                                long endTimeUs, ByteBuffer buffer) {
        LogUtil.i(TAG, "decodeMusicToPcmInner: " + outputPath + ", left = " + startTimeUs + ", right = " + endTimeUs);
        FileChannel writeChannel = null;
        try {
            File pcmFile = new File(outputPath);
            if (!pcmFile.exists()) {
                pcmFile.createNewFile();
            }
            writeChannel = new FileOutputStream(pcmFile).getChannel();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex;
            float ratio, lastRatio = 0;
            while (true) {
                int decodeInputIndex = mediaCodec.dequeueInputBuffer(1000);
                if (decodeInputIndex >= 0) {
                    long sampleTimeUs = extractor.getSampleTime();
                    if (sampleTimeUs == -1) {
                        LogUtil.d(TAG, "decodeMusicToPcm: no more data");
                        break;
                    } else if (sampleTimeUs < startTimeUs) {
                        extractor.advance();
                        continue;
                    } else if (sampleTimeUs > endTimeUs) {
                        LogUtil.d(TAG, "decodeMusicToPcm: current frame time is larger than end time");
                        break;
                    }
                    ratio = (sampleTimeUs - startTimeUs) * 1f / (endTimeUs - startTimeUs);
                    if (ratio - lastRatio > 0.05) {
                        LogUtil.i(TAG, "decodeMusicToPcmInner: ratio = " + ratio);
                        lastRatio = ratio;
                    }
                    bufferInfo.size = extractor.readSampleData(buffer, 0);
                    bufferInfo.presentationTimeUs = sampleTimeUs;
                    bufferInfo.flags = extractor.getSampleFlags();
                    //get frame content for MediaExtractor
                    byte[] bytesContent = new byte[buffer.remaining()];
                    buffer.get(bytesContent);
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(decodeInputIndex);
                    //put the frame data into MediaCodec
                    inputBuffer.put(bytesContent);
                    mediaCodec.queueInputBuffer(decodeInputIndex, 0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
                    //release last frame data
                    extractor.advance();
                }
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 1000);
                while (outputBufferIndex >= 0) {
                    ByteBuffer decodeOutputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                    writeChannel.write(decodeOutputBuffer);
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 1000);
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "decodeMusicToPcmInner: exception = " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (writeChannel != null) {
                try {
                    writeChannel.close();
                } catch (Exception e) {
                    LogUtil.e(TAG, "decodeMusicToPcmInner: release WriteChannel exception = " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private static int selectTrack(MediaExtractor extractor, @MediaConfig.TYPE int type) {
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

    private static void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        // RIFF/WAVE header
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        //WAVE
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        // 'fmt ' chunk
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        // 4 bytes: size of 'fmt ' chunk
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        // format = 1
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // block align
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0;
        // bits per sample
        header[34] = 16;
        header[35] = 0;
        //data
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
