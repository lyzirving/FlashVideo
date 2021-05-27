package com.lyzirving.flashvideo.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;

/**
 * filter to adjust contrast, mContrast ranges from [0, 4], and the filter is normal state when value is 1;
 * @author lyzirving
 */
public class ContrastFilter extends BaseFilter {
    private static final int MIN_CONTRAST = 0;
    private static final int MAX_CONTRAST = 4;

    private float mContrast;
    private int mContrastHandler;

    public ContrastFilter(Context ctx) {
        super(ctx, R.raw.default_vertex_shader, R.raw.contrast_shader);
        mContrast = 1;
    }

    @Override
    protected void initHandler() {
        super.initHandler();
        mContrastHandler = GLES20.glGetUniformLocation(mProgram, "u_contrast");
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

    public void adjust(float value) {
        if (value < MIN_CONTRAST) {
            mContrast = MIN_CONTRAST;
        } else if (value > MAX_CONTRAST) {
            mContrast = MAX_CONTRAST;
        } else {
            mContrast = value;
        }
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

        GLES20.glUniform1f(mContrastHandler, mContrast);

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
}
