package com.lyzirving.flashvideo.opengl.filter;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;


/**
 * this filter is used to record the preview frame from camera
 * @author lyzirving
 */
public class OesFilter extends BaseFilter {
    private static final String TAG = "OesFilter";

    private int mMatrixHandler;
    private float[] mMatrix = new float[16];

    public OesFilter(Context ctx) {
        super(ctx, R.raw.oes_vertex_shader, R.raw.oes_fragment_shader);
    }

    @Override
    public int draw(int oesTextureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId[0]);
        GLES20.glUseProgram(mProgram);
        runPreDraw();

        GLES20.glVertexAttribPointer(mVertexPosHandler, 3, GLES20.GL_FLOAT, false,
                3 * 4, mVertexBuffer);
        //transfer texture coordinate data
        GLES20.glVertexAttribPointer(mTextureCoordinateHandler, 2, GLES20.GL_FLOAT, false,
                2 * 4, mTexCoordinatesBuffer);
        GLES20.glEnableVertexAttribArray(mVertexPosHandler);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandler);

        GLES20.glUniformMatrix4fv(mMatrixHandler, 1, false, mMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId);
        GLES20.glUniform1i(mTextureSamplerHandler, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, DEFAULT_VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, SCREEN_FRAME_BUFFER_ID);

        return mTextureId[0];
    }

    @Override
    protected void initHandler() {
        super.initHandler();
        mMatrixHandler = GLES20.glGetUniformLocation(mProgram, "u_matrix");
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
    public void release() {
        super.release();
        if (mFrameBufferId != null && mFrameBufferId[0] != TextureUtil.ID_NO_FRAME_BUFFER) {
            GLES20.glDeleteFramebuffers(mFrameBufferId.length, mFrameBufferId, 0);
            mFrameBufferId = null;
        }
    }

    public void setMatrix(float[] matrix) {
        System.arraycopy(matrix, 0, mMatrix, 0, 16);
    }
}
