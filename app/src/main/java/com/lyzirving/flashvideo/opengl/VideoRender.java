package com.lyzirving.flashvideo.opengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.lyzirving.flashvideo.opengl.filter.YuvVideoFilter;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.opengl.util.VertexUtil;
import com.lyzirving.flashvideo.player.FlashVideo;
import com.lyzirving.flashvideo.player.IPlayer;
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
public class VideoRender implements GLSurfaceView.Renderer, IPlayer {
    private static final String TAG = "VideoRender";

    private final Queue<Runnable> mRunPreDraw;
    private int mViewWidth, mViewHeight;
    private int mVideoWidth, mVideoHeight;
    private ByteBuffer mChannelY, mChannelU, mChannelV;
    private boolean mIsRendering;

    private GLSurfaceView mView;
    private FlashVideo mVideo;
    private YuvVideoFilter mVideoBgFilter;
    private VideoListenerAdapter mVideoListener;
    private GLVideoView.GLVideoViewListener mVideoViewListener;

    public VideoRender(GLSurfaceView view) {
        mView = view;
        mVideo = new FlashVideo();
        mVideo.setListener(getVideoListener());
        mVideoBgFilter = new YuvVideoFilter(view.getContext());
        mRunPreDraw = new LinkedList<>();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtil.d(TAG, "onSurfaceCreated");
        GLES20.glClearColor(1f,1f,1f, 1f);
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
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        runPreDraw();
        renderVideoBgOnScreen();
    }

    @Override
    public void prepare() {
        if (mVideo == null) {
            mVideo = new FlashVideo();
            mVideo.setListener(getVideoListener());
        }
        mVideo.prepare();
    }

    @Override
    public void play() {
        if (mVideo == null) {
            LogUtil.e(TAG, "play: video is null");
        } else {
            mVideo.play();
        }
    }

    @Override
    public void pause() {
        if (mVideo == null) {
            LogUtil.e(TAG, "pause: video is null");
        } else {
            mVideo.pause();
        }
    }

    @Override
    public void stop() {
        if (mVideo == null) {
            LogUtil.e(TAG, "stop: video is null");
        } else {
            mVideo.stop();
        }
    }

    @Override
    public void setDataSource(String source) {
        if (mVideo == null) {
            mVideo = new FlashVideo();
            mVideo.setListener(getVideoListener());
        }
        mVideo.setDataSource(source);
    }

    @Override
    public void seek(float ratio) {
        if (mVideo == null) {
            LogUtil.e(TAG, "seek: video is null");
        } else {
            mVideo.seek(ratio);
        }
    }

    public void setVideoViewListener(GLVideoView.GLVideoViewListener listener) {
        mVideoViewListener = listener;
    }

    private void addPreDrawTask(final Runnable runnable) {
        synchronized (mRunPreDraw) {
            mRunPreDraw.add(runnable);
        }
    }

    private void createVideoFilter(int videoWidth, int videoHeight) {
        mVideoBgFilter.setVertexCoordinates(VertexUtil.get().createVertexCoordinates(videoWidth, videoHeight));
        mVideoBgFilter.setTextureCoordinates(TextureUtil.get().getDefaultTextureCoordinates());
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                mVideoBgFilter.init();
            }
        });
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
                        mView.requestRender();
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
                    createVideoFilter(width, height);
                    if (mVideoViewListener != null) {
                        mVideoViewListener.onPrepare(duration);
                    }
                }

                @Override
                public void onStop() {
                    super.onStop();
                    LogUtil.d(TAG, "onStop");
                    mVideo = null;
                    if (mVideoViewListener != null) {
                        mVideoViewListener.onVideoStop();
                    }
                }

                @Override
                public void onTickTime(double currentTime) {
                    super.onTickTime(currentTime);
                    if (mVideoViewListener != null) {
                        mVideoViewListener.onVideoTickTime(currentTime);
                    }
                }
            };
        }
        return mVideoListener;
    }

    private void renderVideoBgOnScreen() {
        if (mVideoBgFilter != null && mChannelY != null && mChannelU != null && mChannelV != null) {
            mIsRendering = true;
            mVideoBgFilter.draw(mVideoWidth, mVideoHeight, mChannelY, mChannelU, mChannelV);
            mChannelY.clear();
            mChannelU.clear();
            mChannelV.clear();
            mChannelY = null;
            mChannelU = null;
            mChannelV = null;
            mIsRendering = false;
        }
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
