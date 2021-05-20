package com.lyzirving.flashvideo.camera;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraMetadata;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

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

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraRender.onSurfaceCreated(gl, config);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCameraRender.onSurfaceChanged(gl, width, height);
        CameraHelper.get().prepare(getContext(), width, height, CameraMetadata.LENS_FACING_FRONT);
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

    private void initEnv() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,16,0);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }
}
