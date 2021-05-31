package com.lyzirving.flashvideo.camera;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.lyzirving.flashvideo.util.LogUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author lyzirving
 */
public class GLCameraView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final String TAG = "GLCameraView";

    private CameraRender mCameraRender;

    public GLCameraView(Context context) {
        this(context, null);
    }

    public GLCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initEnv();
        mCameraRender = new CameraRender(context);
    }

    public void adjust(@CameraRender.FilterType int type, int value) {
        mCameraRender.adjust(type, value);
    }

    public void addFilter(@CameraRender.FilterType int type) {
        mCameraRender.addFilter(type);
    }

    public void closeCamera() {
        CameraHelper.get().closeCamera();
    }

    public void dequeueFilter(@CameraRender.FilterType int type) {
        mCameraRender.dequeueFilter(type);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtil.d(TAG, "onSurfaceCreated");
        mCameraRender.onSurfaceCreated(gl, config);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        LogUtil.d(TAG, "onSurfaceChanged: width = " + width + ", height = " + height);
        mCameraRender.preOnSurfaceChanged();
        CameraHelper.get().prepare(getContext(), width, height, CameraHelper.get().getFrontType());
        mCameraRender.onSurfaceChanged(gl, width, height);
        CameraHelper.get().setOesTexture(mCameraRender.getOesTexture());
        mCameraRender.getOesTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });
        CameraHelper.get().open(getContext());
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mCameraRender.onDrawFrame(gl);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        LogUtil.d(TAG, "surfaceDestroyed");
        mCameraRender.destroy();
        super.surfaceDestroyed(holder);
    }

    public void switchRecord(boolean on) {
        mCameraRender.switchRecord(on);
    }

    public void switchFace(int type) {
        if (CameraHelper.get().checkFaceType(type)) {
            onPause();
            CameraHelper.get().setFrontType(type);
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    onResume();
                }
            }, 50);
        }
    }

    public void takePhoto() {
        mCameraRender.takePhoto();
    }

    private void initEnv() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,16,0);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }
}
