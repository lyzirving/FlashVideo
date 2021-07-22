package com.lyzirving.flashvideo.imgedit.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.filter.BaseFilter;
import com.lyzirving.flashvideo.util.LogUtil;

/**
 * saturation: The degree of saturation or desaturation to apply to the image (0.0 - 2.0, with 1.0 as the default)
 * @author lyzirving
 */
public class ImgSaturationFilter extends BaseFilter {
    private static final String TAG = "ImgSaturationFilter";

    private float mSaturation;
    private int mSaturationHandler;

    public ImgSaturationFilter(Context ctx) {
        super(ctx, R.raw.default_vertex_shader, R.raw.saturation_fragment_shader);
        mSaturation = 1;
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
        if (progress < 50) {
            adjust(1 - (50 - progress) * 1f / 50);
        } else {
            adjust(1 + (progress - 50) * 1f / 50);
        }
    }

    @Override
    public void adjust(float value) {
        super.adjust(value);
        LogUtil.i(TAG, "adjust: " + value);
        mSaturation = value;
    }

    @Override
    protected void initHandler() {
        super.initHandler();
        mSaturationHandler = GLES20.glGetUniformLocation(mProgram, "saturation");
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

        GLES20.glUniform1f(mSaturationHandler, mSaturation);

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
