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
    private static final int MAX_BLUR_SIZE = 3;

    private float mHorBlurSize, mVerBlurSize;
    private ImgHorizontalGaussianFilter mHorizontalFilter;
    private ImgVerticalGaussianFilter mVerticalFilter;

    public ImgGaussianFilter(Context ctx) {
        super(ctx);
        mHorBlurSize = 0;
        mVerBlurSize = 0;
        mHorizontalFilter = new ImgHorizontalGaussianFilter(ctx);
        mVerticalFilter = new ImgVerticalGaussianFilter(ctx);
        addFilterThenInit(mHorizontalFilter);
        addFilterThenInit(mVerticalFilter);
    }

    public void adjustHorBlur(int progress) {
        if (progress < 0) {
            progress = 0;
        } else if (progress > 100) {
            progress = 100;
        }
        mHorBlurSize = progress * 1f / 100 * MAX_BLUR_SIZE;
        LogUtil.i(TAG, "adjustHorBlur: progress = " + progress + ", hor blur size = " + mHorBlurSize);
    }

    public void adjustVerBlur(int progress) {
        if (progress < 0) {
            progress = 0;
        } else if (progress > 100) {
            progress = 100;
        }
        mVerBlurSize = progress * 1f / 100 * MAX_BLUR_SIZE;
        LogUtil.i(TAG, "adjustVerBlur: progress = " + progress + ", ver blur size = " + mHorBlurSize);
    }

    public int draw(int inputTextureId, int outputFrameBuffer) {
        mHorizontalFilter.setBlurSize(mHorBlurSize);
        mVerticalFilter.setBlurSize(mVerBlurSize);
        mVerticalFilter.flip(false, true);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGroupFrameBuffers[0]);
        mHorizontalFilter.draw(inputTextureId);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, outputFrameBuffer);
        mVerticalFilter.draw(mGroupTextures[0]);

        return outputFrameBuffer;
    }
}
