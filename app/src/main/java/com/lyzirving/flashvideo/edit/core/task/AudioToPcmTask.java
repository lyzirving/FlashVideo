package com.lyzirving.flashvideo.edit.core.task;

import android.media.MediaExtractor;
import android.text.TextUtils;

import com.lyzirving.flashvideo.edit.MediaEditOp;
import com.lyzirving.flashvideo.edit.core.MediaUtil;
import com.lyzirving.flashvideo.edit.core.MediaWrapper;
import com.lyzirving.flashvideo.opengl.core.MediaConfig;
import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class AudioToPcmTask implements MediaTask {
    private static final String TAG = "AudioToPcmTask";

    private MediaEditOp mOp;

    public AudioToPcmTask(MediaEditOp op) {
        mOp = op;
    }

    @Override
    public boolean run() {
        if (!checkEditOp(mOp)) {
            LogUtil.i(TAG, "run: failed, invalid edit operation");
            return false;
        }
        MediaWrapper wrapper = new MediaWrapper(MediaConfig.TYPE_MUSIC);
        wrapper.op = mOp;
        if (!MediaUtil.buildExtractor(wrapper, wrapper.op.info.path)) {
            LogUtil.i(TAG, "run: failed to build extractor");
            wrapper.release();
            return false;
        }
        if (wrapper.op.leftAnchor > 0) {
            long startPos = (long)(wrapper.op.info.duration * wrapper.op.leftAnchor * 1000 * 1000);
            wrapper.extractor.seekTo(startPos, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            LogUtil.i(TAG, "run: seek to left anchor = " + startPos * 1f / (1000 * 1000) + "s");
        }
        if (!MediaUtil.buildDecoder(wrapper)) {
            LogUtil.i(TAG, "run: failed to build decoder");
            wrapper.release();
            return false;
        }
        if (!MediaUtil.decodeToDst(wrapper.op.pcmTmpDir, wrapper)) {
            LogUtil.i(TAG, "run: failed to decode " + wrapper.op.info.nameWithoutSuffix + " to pcm");
            wrapper.release();
            return false;
        }
        wrapper.release();
        return true;
    }

    private boolean checkEditOp(MediaEditOp op) {
        if (op == null || op.info == null || TextUtils.isEmpty(op.info.path)) {
            LogUtil.e(TAG, "checkEditOp: invalid MediaEditOp");
            return false;
        }
        if (op.info.duration <= 0) {
            LogUtil.e(TAG, "checkEditOp: invalid duration = " + op.info.duration);
            return false;
        }
        if (op.leftAnchor < 0 || op.leftAnchor >= 1) {
            op.leftAnchor = 0;
        }
        if (op.rightAnchor > 1 || op.rightAnchor <= 0) {
            op.rightAnchor = 1;
        }
        return true;
    }
}
