package com.lyzirving.flashvideo.edit.core;

import android.media.MediaCodec;
import android.media.MediaExtractor;

import com.lyzirving.flashvideo.edit.MediaEditOp;
import com.lyzirving.flashvideo.opengl.core.MediaConfig;
import com.lyzirving.flashvideo.util.LogUtil;

import java.nio.ByteBuffer;

/**
 * @author lyzirving
 */
public class MediaWrapper {
    private static final String TAG = "MediaWrapper";

    @MediaConfig.TYPE
    public int mediaType;
    public int mediaTrack;

    public ByteBuffer byteBuffer;
    public MediaEditOp op;
    public MediaExtractor extractor;
    public MediaCodec decoder;
    public MediaCodec encoder;

    public MediaWrapper(@MediaConfig.TYPE int type) {
        mediaType = type;
    }

    public void release() {
        if (extractor != null) {
            try {
                extractor.release();
            } catch (Exception e) {
                LogUtil.i(TAG, "release: extractor exception = " + e.getMessage());
                e.printStackTrace();
            } finally {
                extractor = null;
            }
        }
        if (decoder != null) {
            try {
                decoder.stop();
                decoder.release();
            } catch (Exception e) {
                LogUtil.i(TAG, "release: decoder exception = " + e.getMessage());
                e.printStackTrace();
            } finally {
                decoder = null;
            }
        }
        if (encoder != null) {
            try {
                encoder.stop();
                encoder.release();
            } catch (Exception e) {
                LogUtil.i(TAG, "release: encoder exception = " + e.getMessage());
                e.printStackTrace();
            } finally {
                encoder = null;
            }
        }
        if (byteBuffer != null) {
            byteBuffer.clear();
            byteBuffer = null;
        }
    }
}
