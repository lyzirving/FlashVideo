package com.lyzirving.flashvideo.imgedit.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.opengl.filter.BaseFilterGroup;
import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class ImgGaussianFilter extends BaseFilterGroup {
    private static final String TAG = "ImgGaussianFilter";

    private float mBlurSize;
    private ImgHorizontalGaussianFilter mHorizontalFilter;
    private ImgVerticalGaussianFilter mVerticalFilter;

    public ImgGaussianFilter(Context ctx) {
        super(ctx);
        mBlurSize = 0;
        mHorizontalFilter = new ImgHorizontalGaussianFilter(ctx);
        mVerticalFilter = new ImgVerticalGaussianFilter(ctx);
        addFilterThenInit(mHorizontalFilter);
        addFilterThenInit(mVerticalFilter);
    }

    @Override
    public void adjust(float value) {
        super.adjust(value);
        LogUtil.i(TAG, "adjust: blur size = " + value);
        mBlurSize = value;
    }

    @Override
    public void adjustProgress(int progress) {
        super.adjustProgress(progress);
        if (progress < 0) {
            progress = 0;
        } else if (progress > 100) {
            progress = 100;
        }
        LogUtil.i(TAG, "adjustProgress: " + progress);
        adjust(progress * 1f / 100 * mOutputWidth);
    }

    public int draw(int inputTextureId, int outputFrameBuffer) {
        mHorizontalFilter.setBlurSize(mBlurSize);
        mVerticalFilter.setBlurSize(mBlurSize);
        mVerticalFilter.flip(false, true);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGroupFrameBuffers[0]);
        mHorizontalFilter.draw(inputTextureId);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, outputFrameBuffer);
        mVerticalFilter.draw(mGroupTextures[0]);

        return outputFrameBuffer;
    }
}
