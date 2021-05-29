package com.lyzirving.flashvideo.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraMetadata;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Size;

import com.lyzirving.flashvideo.opengl.filter.CaptureFilter;
import com.lyzirving.flashvideo.opengl.filter.ContrastFilter;
import com.lyzirving.flashvideo.opengl.filter.FilterGroup;
import com.lyzirving.flashvideo.opengl.filter.GaussianFilter;
import com.lyzirving.flashvideo.opengl.filter.OesFilter;
import com.lyzirving.flashvideo.opengl.filter.SaturationFilter;
import com.lyzirving.flashvideo.opengl.filter.ShowFilter;
import com.lyzirving.flashvideo.opengl.util.MatrixUtil;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.opengl.util.VertexUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.annotation.IntDef;

// TODO: 2021/5/21 camera filter needs to create a framebuffer and texture, when they are
// TODO: 2021/5/21 filled with pixel data, they should be drawn onto the screen 

/**
 * @author lyzirving
 */
public class CameraRender implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraRender";
    public static final int FILTER_CONTRAST = 0x00;
    public static final int FILTER_SATURATION = 0x01;
    public static final int FILTER_BLUR = 0x02;

    @IntDef({FILTER_CONTRAST, FILTER_SATURATION, FILTER_BLUR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FilterType{}

    private Context mContext;
    private final Queue<Runnable> mRunPreDraw;

    private int mOesTextureId;
    private SurfaceTexture mOesTexture;
    private OesFilter mOesFilter;
    private ShowFilter mShowFilter;
    private CaptureFilter mCaptureFilter;
    private float[] mOesFilterM;
    private int mViewWidth, mViewHeight;

    private FilterGroup mFilterGroup;

    private boolean mTakePhoto;

    public CameraRender(Context context) {
        mContext = context;
        mRunPreDraw = new LinkedList<>();
    }

    public void adjust(@FilterType int type, int value) {
        switch (type) {
            case FILTER_CONTRAST: {
                mFilterGroup.adjust(ContrastFilter.class, processContract(value));
                break;
            }
            case FILTER_SATURATION: {
                mFilterGroup.adjust(SaturationFilter.class, processSaturation(value));
                break;
            }
            case FILTER_BLUR: {
                mFilterGroup.adjust(GaussianFilter.class, processGaussian(value));
                break;
            }
            default: {
                break;
            }
        }
    }

    public void addFilter(@FilterType int type) {
        switch (type) {
            case FILTER_CONTRAST: {
                ContrastFilter filter = new ContrastFilter(mContext);
                filter.setOutputSize(mViewWidth, mViewHeight);
                mFilterGroup.add(filter);
                addPreDrawTask(new Runnable() {
                    @Override
                    public void run() {
                        mFilterGroup.init();
                    }
                });
                break;
            }
            case FILTER_SATURATION: {
                SaturationFilter filter = new SaturationFilter(mContext);
                filter.setOutputSize(mViewWidth, mViewHeight);
                mFilterGroup.add(filter);
                addPreDrawTask(new Runnable() {
                    @Override
                    public void run() {
                        mFilterGroup.init();
                    }
                });
                break;
            }
            case FILTER_BLUR: {
                GaussianFilter filter = new GaussianFilter(mContext);
                filter.setOutputSize(mViewWidth, mViewHeight);
                mFilterGroup.add(filter);
                addPreDrawTask(new Runnable() {
                    @Override
                    public void run() {
                        mFilterGroup.init();
                    }
                });
                break;
            }
            default: {
                break;
            }
        }
    }

    public void dequeueFilter(@FilterType int type) {
        switch (type) {
            case FILTER_CONTRAST: {
                mFilterGroup.dequeue(ContrastFilter.class);
                break;
            }
            case FILTER_SATURATION: {
                mFilterGroup.dequeue(SaturationFilter.class);
                break;
            }
            case FILTER_BLUR: {
                mFilterGroup.dequeue(GaussianFilter.class);
                break;
            }
            default: {
                break;
            }
        }
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
        if (mFilterGroup != null) {
            mFilterGroup.release();
            mFilterGroup = null;
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
        releaseBeforeResume();
        mViewWidth = width;
        mViewHeight = height;
        prepareOesFilterMatrix(CameraHelper.get().getPreviewSize());
        mOesFilter = new OesFilter(mContext);
        mOesFilter.setOutputSize(width, height);
        mOesFilter.setMatrix(mOesFilterM);
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                mOesFilter.init();
            }
        });

        mShowFilter = new ShowFilter(mContext);
        mShowFilter.setOutputSize(width, height);
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                mShowFilter.init();
            }
        });

        GLES20.glViewport(0, 0, width, height);
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
        previewFrame = mFilterGroup.draw(previewFrame);
        mShowFilter.flip(false, (mFilterGroup.size() % 2 != 0));
        mShowFilter.draw(previewFrame);
        takePhotoIfNeed(previewFrame, false, (mFilterGroup.size() % 2 == 0));
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

    private void takePhotoIfNeed(int textureId, boolean flipX, boolean flipY) {
        if (mTakePhoto && mCaptureFilter.isInit()) {
            LogUtil.d(TAG, "takePhotoIfNeed");
            mTakePhoto = false;
            mCaptureFilter.setFlip(flipX, flipY);
            mCaptureFilter.draw(textureId);
            mCaptureFilter.saveCapture(mViewWidth, mViewHeight);
        }
    }

    private float processContract(int value) {
        if (value >= 25) {
            return (value - 25) / 75f * (4 - 1) + 1;
        } else {
            return value / 25f;
        }
    }

    private float processSaturation(int value) {
        if (value >= 50) {
            return (value - 50) / 50f + 1;
        } else {
            return 1 - (50 - value) / 50f;
        }
    }

    private float processGaussian(int value) {
        if (value >= 20) {
            return (value - 20) / 20f + 1;
        } else {
            return 1 - (20 - value) / 20f;
        }
    }

    private void releaseBeforeResume() {
        if (mOesFilter != null) {
            mOesFilter.release();
            mOesFilter = null;
        }
        if (mShowFilter != null) {
            mShowFilter.release();
            mShowFilter = null;
        }
        if (mFilterGroup != null) {
            mFilterGroup.release();
        }
        mFilterGroup = new FilterGroup();
    }
}
