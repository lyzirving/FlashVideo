package com.lyzirving.flashvideo.imgedit.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.filter.BaseFilter;
import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class ImgToonFilter extends BaseFilter {
    private static final String TAG = "ImgToonFilter";
    private static final int MAX_THRESHOLD = 1;
    private static final int MAX_QUANTIZATION_LEVEL = 100;

    private float mThreshold;
    private int mQuantizationLevels;

    private int mTextureWidthHandler, mTextureHeightHandler;
    private int mThresholdHandler;
    private int mQuantizationLevelsHandler;

    public ImgToonFilter(Context ctx) {
        super(ctx, R.raw.threexthree_texture_sample_vertex_shader, R.raw.toon_fragment_shader);
        mThreshold = 2.5f;
        mQuantizationLevels = 10;
    }

    public void adjustQuantizationLevels(int progress) {
        if (progress <= 0) {
            progress = 1;
        } else if (progress > 100) {
            progress = 100;
        }
        mQuantizationLevels = (int) (progress * 1f/ 100 * MAX_QUANTIZATION_LEVEL);
        LogUtil.i(TAG, "adjustQuantizationLevels: progress = " + progress + ", quantization level = " + mQuantizationLevels);
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

        GLES20.glUniform1f(mTextureWidthHandler, mOutputWidth);
        GLES20.glUniform1f(mTextureHeightHandler, mOutputHeight);
        GLES20.glUniform1f(mThresholdHandler, mThreshold);
        GLES20.glUniform1f(mQuantizationLevelsHandler, mQuantizationLevels);

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
        mTextureWidthHandler = GLES20.glGetUniformLocation(mProgram, "texelWidth");
        mTextureHeightHandler = GLES20.glGetUniformLocation(mProgram, "texelHeight");
        mThresholdHandler = GLES20.glGetUniformLocation(mProgram, "threshold");
        mQuantizationLevelsHandler = GLES20.glGetUniformLocation(mProgram, "quantizationLevels");
    }
}
