package com.lyzirving.flashvideo.imgedit.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.filter.BaseFilter;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.util.LogUtil;

/**
 * filter to adjust contrast, mContrast ranges from [0, 4],
 * and the filter is normal state when value is 1;
 * @author lyzirving
 */
public class ImgContrastFilter extends BaseFilter {
    private static final String TAG = "ImgContrastFilter";
    private static final int MIN_CONTRAST = 0;
    private static final int MAX_CONTRAST = 4;

    private float mContrast;
    private int mContrastHandler;

    public ImgContrastFilter(Context ctx) {
        super(ctx, R.raw.default_vertex_shader, R.raw.contrast_shader);
        mContrast = 1;
    }

    @Override
    protected void initHandler() {
        super.initHandler();
        mContrastHandler = GLES20.glGetUniformLocation(mProgram, "u_contrast");
    }

    @Override
    public void adjustProgress(int progress) {
        if (progress < 0) {
            progress = 0;
        } else if (progress > 100) {
            progress = 100;
        }
        //needs to be （1, 4]
        adjust(1 + progress * 1f / 100 * 3);
    }

    @Override
    public void adjust(float value) {
        if (value < MIN_CONTRAST) {
            mContrast = MIN_CONTRAST;
        } else if (value > MAX_CONTRAST) {
            mContrast = MAX_CONTRAST;
        } else {
            mContrast = value;
        }
        LogUtil.d(TAG, "adjust: original " + value + ", value = " + mContrast);
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

        GLES20.glUniform1f(mContrastHandler, mContrast);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureId);
        GLES20.glUniform1i(mTextureSamplerHandler, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, DEFAULT_VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return inputTextureId;
    }
}
