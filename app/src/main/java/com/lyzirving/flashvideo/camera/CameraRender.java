package com.lyzirving.flashvideo.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;

import com.lyzirving.flashvideo.opengl.filter.CameraPreviewFilter;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.opengl.util.VertexUtil;

import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

// TODO: 2021/5/21 camera filter needs to create a framebuffer and texture, when they are
// TODO: 2021/5/21 filled with pixel data, they should be drawn onto the screen 

/**
 * @author lyzirving
 */
public class CameraRender implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraRender";

    private Context mContext;
    private final Queue<Runnable> mRunPreDraw;

    private int mOesTextureId;
    private SurfaceTexture mOesTexture;
    private CameraPreviewFilter mPreviewFilter;

    public CameraRender(Context context) {
        mContext = context;
        mRunPreDraw = new LinkedList<>();
    }
    
    public SurfaceTexture getOesTexture() {
        return mOesTexture;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mOesTextureId = TextureUtil.get().generateOesTexture();
        mOesTexture = new SurfaceTexture(mOesTextureId);
        mPreviewFilter = new CameraPreviewFilter(mContext);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mPreviewFilter.setVertexCoordinates(VertexUtil.get().getDefaultVertexCoordinates());
        mPreviewFilter.setTextureCoordinates(TextureUtil.get().getDefaultTextureCoordinates());
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                mPreviewFilter.init();
            }
        });
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mOesTexture != null) {
            //update and consume the image data
            //or the next frame won't come
            mOesTexture.updateTexImage();
        }
        runPreDraw();
    }

    private void addPreDrawTask(final Runnable runnable) {
        synchronized (mRunPreDraw) {
            mRunPreDraw.add(runnable);
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
