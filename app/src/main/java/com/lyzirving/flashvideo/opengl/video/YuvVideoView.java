package com.lyzirving.flashvideo.opengl.video;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.lyzirving.flashvideo.opengl.filter.YuvFilter;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.opengl.util.VertexUtil;
import com.lyzirving.flashvideo.player.FlashVideoPlayer;
import com.lyzirving.flashvideo.player.VideoListenerAdapter;
import com.lyzirving.flashvideo.util.LogUtil;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author lyzirving
 */
public class YuvVideoView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final String TAG = "YuvVideoView";

    private int mViewWidth, mViewHeight, mVideoWidth, mVideoHeight;
    private final Queue<Runnable> mRunPreDraw;

    private boolean mIsRendering, mClearBufferBit;
    private ByteBuffer mChannelY, mChannelU, mChannelV;
    private YuvFilter mYuvVideoBgFilter;

    private VideoListenerAdapter mVideoListener;
    private VideoViewListener mVideoViewListener;
    private FlashVideoPlayer mVideoPlayer;

    public YuvVideoView(Context context) {
        this(context, null);
    }

    public YuvVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRunPreDraw = new LinkedList<>();
        initEnv();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtil.d(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0,0,0, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        LogUtil.d(TAG, "onSurfaceCreated: width = " + width + ", height = " + height);
        boolean viewChanged = ((mViewWidth != width) || (mViewHeight != height));
        if (viewChanged) {
            LogUtil.d(TAG, "onSurfaceChanged: view is changed");
            mViewWidth = width;
            mViewHeight = height;
            GLES20.glViewport(0, 0, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mIsRendering = true;
        runPreDraw();
        if (mYuvVideoBgFilter == null || mClearBufferBit) {
            LogUtil.d(TAG, "onDrawFrame: clear color bit");
            mClearBufferBit = false;
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            if (mYuvVideoBgFilter != null) {
                mYuvVideoBgFilter.release();
                mYuvVideoBgFilter = null;
            }
        }
        drawOnScreen();
        mIsRendering = false;
    }

    public void play() {
        if (mVideoPlayer == null) {
            LogUtil.e(TAG, "play: video player is null");
            return;
        }
        mVideoPlayer.play();
    }

    public void pause() {
        if (mVideoPlayer == null) {
            LogUtil.e(TAG, "pause: video player is null");
            return;
        }
        mVideoPlayer.pause();
    }

    public void prepare(String path, VideoViewListener listener) {
        if (mVideoPlayer == null) {
            mVideoPlayer = new FlashVideoPlayer();
        }
        mVideoViewListener = listener;
        mVideoPlayer.init(path, getVideoListener());
    }

    public void stop() {
        if (mVideoPlayer == null) {
            LogUtil.e(TAG, "stop: video player is null");
            return;
        }
        mVideoPlayer.stop();
    }

    private void addPreDrawTask(final Runnable runnable) {
        synchronized (mRunPreDraw) {
            mRunPreDraw.add(runnable);
        }
    }

    private void drawOnScreen() {
        if (mVideoPlayer != null && mYuvVideoBgFilter != null && mChannelY != null && mChannelU != null && mChannelV != null) {
            mYuvVideoBgFilter.draw(mVideoWidth, mVideoHeight, mChannelY, mChannelU, mChannelV);
            mChannelY.clear();
            mChannelU.clear();
            mChannelV.clear();
            mChannelY = null;
            mChannelU = null;
            mChannelV = null;
        }
    }

    private VideoListenerAdapter getVideoListener() {
        if (mVideoListener == null) {
            mVideoListener = new VideoListenerAdapter(){
                @Override
                public void onFrame(int width, int height, byte[] yData, byte[] uData, byte[] vData) {
                    super.onFrame(width, height, yData, uData, vData);
                    if (!mIsRendering) {
                        mChannelY = ByteBuffer.wrap(yData);
                        mChannelU = ByteBuffer.wrap(uData);
                        mChannelV = ByteBuffer.wrap(vData);
                        requestRender();
                    } else {
                        LogUtil.d(TAG, "onFrame: video is rendering, skip");
                    }
                }

                @Override
                public void onPrepare(double duration, int width, int height) {
                    super.onPrepare(duration, width, height);
                    LogUtil.d(TAG, "onPrepare: duration = " + duration + ", width = " + width + ", height = " + height);
                    mVideoWidth = width;
                    mVideoHeight = height;
                    mYuvVideoBgFilter = new YuvFilter(getContext());
                    mYuvVideoBgFilter.setVertexCoordinates(VertexUtil.get().createVertexCoordinates(width, height));
                    mYuvVideoBgFilter.setTextureCoordinates(TextureUtil.get().getDefaultTextureCoordinates());
                    addPreDrawTask(new Runnable() {
                        @Override
                        public void run() {
                            mYuvVideoBgFilter.init();
                        }
                    });
                    if (mVideoViewListener != null) {
                        mVideoViewListener.onPrepare(duration);
                    }
                }

                @Override
                public void onStop() {
                    super.onStop();
                    LogUtil.d(TAG, "onStop");
                    mVideoPlayer = null;
                    mClearBufferBit = true;
                    if (mVideoViewListener != null) {
                        mVideoViewListener.onVideoStop();
                    }
                    requestRender();
                }

                @Override
                public void onTickTime(double currentTime) {
                    super.onTickTime(currentTime);
                    LogUtil.d(TAG, "onTickTime: " + currentTime);
                    if (mVideoViewListener != null) {
                        mVideoViewListener.onVideoTickTime(currentTime);
                    }
                }
            };
        }
        return mVideoListener;
    }

    private void initEnv() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,16,0);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mVideoPlayer = new FlashVideoPlayer();
    }

    private void runPreDraw() {
        Runnable task;
        synchronized (mRunPreDraw) {
            while (!mRunPreDraw.isEmpty()) {
                if ((task = mRunPreDraw.poll()) != null) {
                    task.run();
                }
            }
        }
    }
}
