package com.lyzirving.flashvideo.imgedit.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.filter.BaseFilter;
import com.lyzirving.flashvideo.util.LogUtil;

/**
 * Sharpens the picture;
 * sharpness: from -4.0 to 4.0, with 0.0 as the normal level
 * @author lyzirving
 */
public class ImgSharpenFilter extends BaseFilter {
    private static final String TAG = "ImgSharpenFilter";

    private float mSharpen, mImgWidthFactor, mImgHeightFactor;
    private int mSharpenHandler, mImgWidthHandler, mImgHeightHandler;

    public ImgSharpenFilter(Context ctx) {
        super(ctx, R.raw.sharpen_vertex_shader, R.raw.sharpen_fragment_shader);
        mSharpen = 0;
    }

    @Override
    public void adjust(float value) {
        super.adjust(value);
        if (value < -4) {
            mSharpen = -4;
        } else if (value > 4) {
            mSharpen = 4;
        } else {
            mSharpen = value;
        }
        LogUtil.i(TAG, "adjust: input = " + value + ", result = " + mSharpen);
    }

    @Override
    public void adjustProgress(int progress) {
        super.adjustProgress(progress);
        LogUtil.i(TAG, "adjustProgress: " + progress);
        if (progress < MIN_PROGRESS) {
            progress = MIN_PROGRESS;
        } else if (progress > MAX_PROGRESS) {
            progress = MAX_PROGRESS;
        }
        if (progress <= 50) {
            adjust((50 - progress) * 1f / 50 * -4);
        } else {
            adjust((progress - 50) * 1f / 50 * 4);
        }
    }

    @Override
    public int draw(int inputTextureId) {
        GLES20.glUseProgram(mProgram);
        runPreDraw();

        GLES20.glVertexAttribPointer(mVertexPosHandler, 3, GLES20.GL_FLOAT, false,
                3 * 4, mVertexBuffer);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandler, 2, GLES20.GL_FLOAT, false,
                2 * 4, mTexCoordinatesBuffer);
        GLES20.glEnableVertexAttribArray(mVertexPosHandler);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandler);

        GLES20.glUniform1f(mSharpenHandler, mSharpen);
        GLES20.glUniform1f(mImgWidthHandler, mImgWidthFactor);
        GLES20.glUniform1f(mImgHeightHandler, mImgHeightFactor);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureId);
        GLES20.glUniform1i(mTextureSamplerHandler, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, DEFAULT_VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return inputTextureId;
    }

    @Override
    protected void initHandler() {
        super.initHandler();
        mSharpenHandler = GLES20.glGetUniformLocation(mProgram, "sharpness");
        mImgWidthHandler = GLES20.glGetUniformLocation(mProgram, "imageWidthFactor");
        mImgHeightHandler = GLES20.glGetUniformLocation(mProgram, "imageHeightFactor");
    }

    @Override
    public void setOutputSize(int width, int height) {
        super.setOutputSize(width, height);
        mImgWidthFactor = 1f / width;
        mImgHeightFactor = 1f / height;
    }
}
