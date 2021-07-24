package com.lyzirving.flashvideo.imgedit.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.filter.BaseFilter;
import com.lyzirving.flashvideo.opengl.util.MatrixUtil;

/**
 * @author lyzirving
 */
public class ImgVerticalGaussianFilter extends BaseFilter {
    private static final String TAG = "ImgVerticalGaussianFilter";

    private int mTextureWidthOffsetHandler, mTextureHeightOffsetHandler;
    private int mMatrixHandler;
    private float[] mMatrix = new float[16];

    public ImgVerticalGaussianFilter(Context ctx) {
        super(ctx, R.raw.gaussian_vertex_shader, R.raw.gaussian_fragment_shader);
        MatrixUtil.get().initMatrix(mMatrix);
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

        GLES20.glUniformMatrix4fv(mMatrixHandler, 1, false, mMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureId);
        GLES20.glUniform1i(mTextureSamplerHandler, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, DEFAULT_VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        MatrixUtil.get().initMatrix(mMatrix);

        return inputTextureId;
    }

    public void flip(boolean flipX, boolean flipY) {
        MatrixUtil.get().flip(mMatrix, flipX, flipY);
    }

    public void setBlurSize(float blurSize) {
        setFloatValue(mTextureWidthOffsetHandler, 0,
                mTextureHeightOffsetHandler, blurSize / mOutputHeight);
    }

    @Override
    protected void initHandler() {
        super.initHandler();
        mTextureWidthOffsetHandler = GLES20.glGetUniformLocation(mProgram, "u_texture_width_offset");
        mTextureHeightOffsetHandler = GLES20.glGetUniformLocation(mProgram, "u_texture_height_offset");
        mMatrixHandler = GLES20.glGetUniformLocation(mProgram, "u_matrix");
    }
}
