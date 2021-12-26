package com.lyzirving.flashvideo.record;

import java.nio.ByteBuffer;

public class FFmpegMuxer {
    private static final String TAG = "FFmpegMuxer";

    private long mPtr;

    public FFmpegMuxer() { mPtr = nCreate(); }

    public void enqueueData(ByteBuffer data, int offset, int size, long pts, boolean keyFrame) {
        nEnqueueData(mPtr, data, offset, size,pts, keyFrame);
    }
    public boolean prepare() { return nPrepare(mPtr); }

    public void stop() { nStop(mPtr); }

    public void release() {
        nRelease(mPtr);
        mPtr = -1;
    }

    private static native long nCreate();
    private static native void nEnqueueData(long ptr, ByteBuffer data, int offset, int size, long pts, boolean keyFrame);
    private static native boolean nPrepare(long ptr);
    private static native void nStop(long ptr);
    private static native void nRelease(long ptr);
}
