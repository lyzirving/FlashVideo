package com.lyzirving.flashvideo.imgedit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.lyzirving.flashvideo.imgedit.filter.ImgBitmapFilter;
import com.lyzirving.flashvideo.imgedit.filter.ImgGaussianFilter;
import com.lyzirving.flashvideo.imgedit.filter.ImgScreenFilter;
import com.lyzirving.flashvideo.opengl.filter.BaseFilter;
import com.lyzirving.flashvideo.opengl.filter.BaseFilterGroup;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.util.ComponentUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author lyzirving
 */
public class ImgEditView extends GLSurfaceView implements GLSurfaceView.Renderer, IEdit {
    private static final String TAG = "ImgEditView";

    private final Object mLock = new Object();
    private LinkedList<Runnable> mPreDrawTask;
    private int mViewWidth, mViewHeight;

    private int[] mOffScreenFrameBuffer = new int[]{TextureUtil.ID_NO_FRAME_BUFFER};
    private int[] mOffScreenTexture = new int[]{TextureUtil.ID_NO_TEXTURE};
    private BaseFilter mBgFilter;
    private ImgScreenFilter mScreenFilter;
    private BaseFilterGroup mFilterGroup;
    private ImgEditViewListener mListener;

    public ImgEditView(Context context) {
        this(context, null);
    }

    public ImgEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initEnv();
        initData();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtil.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(1,1,1, 1f);
        if (mListener != null) { mListener.onViewCreate(); }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        LogUtil.i(TAG, "onSurfaceChanged: current size = (" + width + "," + height + "), previous size = (" + mViewWidth + "," + mViewHeight + ")");
        boolean sizeChange = mViewWidth != width || mViewHeight != height;
        if (sizeChange) {
            mViewWidth = width;
            mViewHeight = height;
            GLES20.glViewport(0, 0, width, height);
            prepareFilterWhenViewSizeChange(width, height);
            if (mListener != null) { mListener.onViewChange(width, height); }
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        LogUtil.i(TAG, "onDrawFrame");
        runPreDraw();
        mFilterGroup.runPreDraw();
        if (mBgFilter != null) {
            //draw off screen
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mOffScreenFrameBuffer[0]);
            GLES20.glClearColor(1, 1, 1, 1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            int lastTexture = mBgFilter.draw(mOffScreenTexture[0]);
            lastTexture = mFilterGroup.draw(lastTexture);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            mScreenFilter.flip(false, mFilterGroup.getFilterCount() % 2 == 0);
            mScreenFilter.draw(lastTexture);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LogUtil.i(TAG, "onDetachedFromWindow");
        mListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void setImageBitmap(final Bitmap bmp, boolean forceRender) {
        LogUtil.i(TAG, "setImageBitmap: " + bmp);
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                LogUtil.i(TAG, "setImageBitmap: pre draw task");
                if (mBgFilter != null) {
                    mBgFilter.release();
                }
                mBgFilter = new ImgBitmapFilter(ComponentUtil.get().ctx(), bmp);
                mBgFilter.setOutputSize(mViewWidth, mViewHeight);
                mBgFilter.init();
            }
        });
        if (forceRender) {
            requestRender();
        }
    }

    @Override
    public void setImageResource(final int srcId, boolean forceRender) {
        LogUtil.i(TAG, "setImageResource: " + srcId);
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                LogUtil.i(TAG, "setImageResource: pre draw task");
                if (mBgFilter != null) {
                    mBgFilter.release();
                }
                mBgFilter = new ImgBitmapFilter(ComponentUtil.get().ctx(), srcId);
                mBgFilter.setOutputSize(mViewWidth, mViewHeight);
                mBgFilter.init();
            }
        });
        if (forceRender) {
            requestRender();
        }
    }

    public void adjust(String tag, int value) {
        if (mFilterGroup == null) {
            LogUtil.i(TAG, "adjust: filter group is null");
            return;
        }
        BaseFilter filter = mFilterGroup.getFilter(tag);
        if (filter == null) {
            LogUtil.i(TAG, "adjust: can not get filter " + tag + " from filter group");
            return;
        }
        filter.adjustProgress(value);
        requestRender();
    }

    public void adjustHorDenoise(int value) {
        if (mFilterGroup == null) {
            LogUtil.i(TAG, "adjustHorDenoise: filter group is null");
            return;
        }
        BaseFilter filter = mFilterGroup.getFilter(ImgGaussianFilter.class.getSimpleName());
        if (!(filter instanceof ImgGaussianFilter)) {
            LogUtil.i(TAG, "adjustHorDenoise: can not get gaussian filter");
            return;
        }
        ((ImgGaussianFilter) filter).adjustHorBlur(value);
        requestRender();
    }

    public void adjustVerDenoise(int value) {
        if (mFilterGroup == null) {
            LogUtil.i(TAG, "adjustVerDenoise: filter group is null");
            return;
        }
        BaseFilter filter = mFilterGroup.getFilter(ImgGaussianFilter.class.getSimpleName());
        if (!(filter instanceof ImgGaussianFilter)) {
            LogUtil.i(TAG, "adjustVerDenoise: can not get gaussian filter");
            return;
        }
        ((ImgGaussianFilter) filter).adjustVerBlur(value);
        requestRender();
    }

    public void addFilter(BaseFilter filter, boolean forceRender) {
        if (mFilterGroup == null) {
            LogUtil.i(TAG, "addFilter: filter group is null");
            return;
        }
        if (mFilterGroup.addFilter(filter, forceRender) && forceRender) {
            requestRender();
        }
    }

    public void clear() {
        if (mFilterGroup == null) {
            LogUtil.i(TAG, "clear: filter group is null");
            return;
        }
        addPreDrawTask(new Runnable() {
            @Override
            public void run() { mFilterGroup.release(); }
        });
        requestRender();
    }

    public void setListener(ImgEditViewListener listener) {
        mListener = listener;
    }

    private void addPreDrawTask(final Runnable runnable) {
        synchronized (mLock) {
            mPreDrawTask.addLast(runnable);
        }
    }

    private void addPreDrawTaskHead(final Runnable runnable) {
        synchronized (mLock) {
            mPreDrawTask.addFirst(runnable);
        }
    }

    private void initEnv() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,16,0);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    private void initData() {
        mPreDrawTask = new LinkedList<>();
    }

    private void prepareFilterWhenViewSizeChange(int width, int height) {
        if (mFilterGroup != null) {
            addPreDrawTaskHead(new Runnable() {
                @Override
                public void run() { mFilterGroup.release(); }
            });
        }
        mFilterGroup = new BaseFilterGroup(getContext());
        mFilterGroup.setOutputSize(width, height);
        if (mOffScreenFrameBuffer[0] != TextureUtil.ID_NO_FRAME_BUFFER) {
            TextureUtil.get().deleteFrameBufferAndTexture(mOffScreenFrameBuffer, mOffScreenTexture);
        }
        TextureUtil.get().generateFrameBufferAndTexture(1, mOffScreenFrameBuffer, mOffScreenTexture, width, height);
        if (mScreenFilter != null) {
            mScreenFilter.release();
        }
        mScreenFilter = new ImgScreenFilter(getContext());
        mScreenFilter.setOutputSize(width, height);
        mScreenFilter.init();
    }

    private void runPreDraw() {
        Runnable task;
        synchronized (mLock) {
            while (!mPreDrawTask.isEmpty()) {
                if ((task = mPreDrawTask.poll()) != null) {
                    task.run();
                }
            }
        }
    }

    public interface ImgEditViewListener {
        void onViewCreate();
        void onViewChange(int width, int height);
    }
}
