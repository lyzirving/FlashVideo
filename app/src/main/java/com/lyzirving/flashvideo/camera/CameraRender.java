package com.lyzirving.flashvideo.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraMetadata;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Size;

import com.lyzirving.flashvideo.opengl.filter.CaptureFilter;
import com.lyzirving.flashvideo.opengl.filter.OesFilter;
import com.lyzirving.flashvideo.opengl.filter.ShowFilter;
import com.lyzirving.flashvideo.opengl.util.MatrixUtil;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.opengl.util.VertexUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Objects;
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
    private OesFilter mOesFilter;
    private ShowFilter mShowFilter;
    private CaptureFilter mCaptureFilter;
    private float[] mOesFilterM;
    private int mViewWidth, mViewHeight;

    private boolean mTakePhoto;

    public CameraRender(Context context) {
        mContext = context;
        mRunPreDraw = new LinkedList<>();
    }

    public void destroy() {
        if (mOesTextureId != TextureUtil.ID_NO_TEXTURE) {
            TextureUtil.get().deleteTexture(mOesTextureId);
            mOesTextureId = TextureUtil.ID_NO_TEXTURE;
        }
        if (mOesFilter != null) {
            mOesFilter.release();
            mOesFilter = null;
        }
        if (mShowFilter != null) {
            mShowFilter.release();
            mShowFilter = null;
        }
        if (mCaptureFilter != null) {
            mCaptureFilter.release();
            mCaptureFilter = null;
        }
        mOesTexture = null;
    }
    
    public SurfaceTexture getOesTexture() {
        return mOesTexture;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {}

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mViewWidth = width;
        mViewHeight = height;
        prepareOesFilterMatrix(CameraHelper.get().getPreviewSize());
        mOesFilter = new OesFilter(mContext);
        mOesFilter.setOutputSize(width, height);
        mOesFilter.setVertexCoordinates(VertexUtil.get().getDefaultVertex());
        mOesFilter.setTextureCoordinates(TextureUtil.get().getDefaultTextureCoordinates());
        mOesFilter.setMatrix(mOesFilterM);
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                mOesFilter.init();
            }
        });

        mShowFilter = new ShowFilter(mContext);
        mShowFilter.setOutputSize(width, height);
        mShowFilter.setVertexCoordinates(VertexUtil.get().getDefaultVertex());
        mShowFilter.setTextureCoordinates(TextureUtil.get().getDefaultTextureCoordinates());
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                mShowFilter.init();
            }
        });
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mOesTexture != null) {
            //update and consume the image data
            //or the next frame won't come
            mOesTexture.updateTexImage();
        }
        runPreDraw();

        int previewFrame = mOesFilter.draw(mOesTextureId);
        mShowFilter.draw(previewFrame);
        takePhotoIfNeed(previewFrame);
    }

    public void preOnSurfaceChanged() {
        mOesTextureId = TextureUtil.get().generateOesTexture();
        mOesTexture = new SurfaceTexture(mOesTextureId);
    }

    /**
     * camera activity must be portrait, so viewWidth must smaller than viewHeight
     * @param previewSize camera preview size
     */
    public void prepareOesFilterMatrix(Size previewSize) {
        mOesFilterM = new float[16];
        MatrixUtil.get().initMatrix(mOesFilterM);

        if (CameraHelper.get().getFrontType() == CameraMetadata.LENS_FACING_FRONT) {
            MatrixUtil.get().rotate(mOesFilterM, 270);
        } else {
            MatrixUtil.get().flip(mOesFilterM, true, false);
            MatrixUtil.get().rotate(mOesFilterM, 90);
        }

        int previewWidth = previewSize.getHeight();
        int previewHeight = previewSize.getWidth();
        float previewRatio = previewWidth * 1f / previewHeight;
        float viewRatio = mViewWidth * 1f / mViewHeight;
        if (previewRatio > viewRatio) {
            MatrixUtil.get().scale(mOesFilterM, viewRatio / previewRatio, 1);
        }  else if (previewRatio < viewRatio) {
            MatrixUtil.get().scale(mOesFilterM, 1, previewRatio / viewRatio);
        }
    }

    public void takePhoto() {
        if (mCaptureFilter == null) {
            mCaptureFilter = new CaptureFilter(mContext);
            mCaptureFilter.setOutputSize(mViewWidth, mViewHeight);
            mCaptureFilter.setVertexCoordinates(VertexUtil.get().getDefaultVertex());
            mCaptureFilter.setTextureCoordinates(TextureUtil.get().getDefaultTextureCoordinates());
            mCaptureFilter.setFlip(false, true);
            mCaptureFilter.setOutputRootDir(Objects.requireNonNull(mContext.getExternalFilesDir(null)).getAbsolutePath() + "/image");
            addPreDrawTask(new Runnable() {
                @Override
                public void run() {
                    mCaptureFilter.init();
                }
            });
        }
        mTakePhoto = true;
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

    private void takePhotoIfNeed(int textureId) {
        if (mTakePhoto && mCaptureFilter.isInit()) {
            LogUtil.d(TAG, "takePhotoIfNeed");
            mTakePhoto = false;
            mCaptureFilter.draw(textureId);
            mCaptureFilter.saveCapture();
        }
    }
}
