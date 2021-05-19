package com.lyzirving.flashvideo.camera;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;

import com.lyzirving.flashvideo.opengl.util.TextureUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author lyzirving
 */
public class CameraRender implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraRender";

    private int mOesTextureId;
    private SurfaceTexture mOesTexture;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mOesTextureId = TextureUtil.get().generateOesTexture();
        mOesTexture = new SurfaceTexture(mOesTextureId);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //acquire new image data
        if (mOesTexture != null) {
            mOesTexture.updateTexImage();
        }
    }

    public SurfaceTexture getOesTexture() {
        return mOesTexture;
    }
}
