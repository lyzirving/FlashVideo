package com.lyzirving.flashvideo.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.util.LogUtil;

/**
 * A more generalized 9x9 Gaussian blur filter
 * blurSize ranging from 0.0 on up, with a default of 1.0
 * @author lyzirving
 */
public class GaussianFilter extends BaseFilter {
    private static final String TAG = "GaussianFilter";

    private float mBluerSize;
    private int mTextureWidthOffsetHandler, mTextureHeightOffsetHandler;

    public GaussianFilter(Context ctx) {
        super(ctx, R.raw.gaussian_vertex_shader, R.raw.gaussian_fragment_shader);
        mBluerSize = 1;
    }

    @Override
    protected void initHandler() {
        super.initHandler();
        mTextureWidthOffsetHandler = GLES20.glGetUniformLocation(mProgram, "u_texture_width_offset");
        mTextureHeightOffsetHandler = GLES20.glGetUniformLocation(mProgram, "u_texture_height_offset");
    }

    @Override
    protected void initFrameBuffer() {
        super.initFrameBuffer();
        if (mTextureId[0] == TextureUtil.ID_NO_TEXTURE) {
            throw new RuntimeException("initFrameBuffer: texture is invalid");
        }
        GLES20.glGenFramebuffers(1, mFrameBufferId, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId[0]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mTextureId[0], 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, SCREEN_FRAME_BUFFER_ID);
    }

    @Override
    public int draw(int inputTextureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId[0]);
        GLES20.glUseProgram(mProgram);
        runPreDraw();

        GLES20.glVertexAttribPointer(mVertexPosHandler, 3, GLES20.GL_FLOAT, false,
                3 * 4, mVertexBuffer);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandler, 2, GLES20.GL_FLOAT, false,
                2 * 4, mTexCoordinatesBuffer);
        GLES20.glEnableVertexAttribArray(mVertexPosHandler);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandler);

        GLES20.glUniform1f(mTextureWidthOffsetHandler, mBluerSize / mOutputWidth);
        GLES20.glUniform1f(mTextureHeightOffsetHandler, mBluerSize / mOutputHeight);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureId);
        GLES20.glUniform1i(mTextureSamplerHandler, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, DEFAULT_VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, SCREEN_FRAME_BUFFER_ID);

        return mTextureId[0];
    }

    @Override
    public void adjust(float size) {
        if (size < 0) {
            size = 0;
        }
        mBluerSize = size;
        LogUtil.d(TAG, "setBlurSize: original = " + size + ", result = " + mBluerSize);
    }

}
