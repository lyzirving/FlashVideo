package com.lyzirving.flashvideo.opengl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.lyzirving.flashvideo.player.IPlayer;
import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class GLVideoView extends GLSurfaceView implements IPlayer {
    private static final String TAG = "GLVideoView";

    private VideoRender mRender;

    public GLVideoView(Context context) {
        this(context, null);
    }

    public GLVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initEnv();
    }

    @Override
    public void prepare() {
        if (mRender == null) {
            LogUtil.e(TAG, "prepare: render is null");
        } else {
            mRender.prepare();
        }
    }

    @Override
    public void play() {
        if (mRender == null) {
            LogUtil.e(TAG, "play: render is null");
        } else {
            mRender.play();
        }
    }

    @Override
    public void pause() {
        if (mRender == null) {
            LogUtil.e(TAG, "pause: render is null");
        } else {
            mRender.pause();
        }
    }

    @Override
    public void stop() {
        if (mRender == null) {
            LogUtil.e(TAG, "stop: render is null");
        } else {
            mRender.stop();
        }
    }

    @Override
    public void setDataSource(String source) {
        if (mRender == null) {
            LogUtil.e(TAG, "setDataSource: render is null");
        } else {
            mRender.setDataSource(source);
        }
    }

    @Override
    public void seek(float ratio) {
        mRender.seek(ratio);
    }

    public void setVideoViewListener(GLVideoViewListener listener) {
        mRender.setVideoViewListener(listener);
    }

    private void initEnv() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,16,0);
        mRender = new VideoRender(this);
        setRenderer(mRender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    public interface GLVideoViewListener {
        void onPrepare(double duration);
        void onVideoPlay();
        void onVideoPause();
        void onVideoTickTime(double currentTime);
        void onVideoStop();
    }
}
