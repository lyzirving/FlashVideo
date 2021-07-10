package com.lyzirving.flashvideo.edit.core.task;

import android.text.TextUtils;

import com.lyzirving.flashvideo.edit.MediaInfo;
import com.lyzirving.flashvideo.util.LogUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author lyzirving
 */
public class PcmToWavTask implements MediaTask {
    private static final String TAG = "PcmToWavTask";

    private String mInputPath, mOutputPath;
    private MediaInfo mInfo;

    public PcmToWavTask(String inputPath, String outputPath, MediaInfo info) {
        mInputPath = inputPath;
        mOutputPath = outputPath;
        mInfo = info;
    }

    @Override
    public boolean run() {
        if (TextUtils.isEmpty(mInputPath) || TextUtils.isEmpty(mOutputPath)) {
            LogUtil.i(TAG, "run: path is invalid");
            return false;
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(mInputPath);
            out = new FileOutputStream(mOutputPath);
            long srcLen = in.getChannel().size();
            long outputLen = srcLen + 36;
            byte[] data = new byte[mInfo.minBufferSize];
            writeWaveFileHeader(out, srcLen, outputLen, mInfo.sampleRate, mInfo.channelCount,
                    16 * mInfo.sampleRate * mInfo.channelCount / 8);
            while (in.read(data) != -1) {
                out.write(data);
            }
            LogUtil.i(TAG, "run: success");
        } catch (Exception e) {
            LogUtil.i(TAG, "run: failed, exception = " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
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
