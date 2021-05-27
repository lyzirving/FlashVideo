package com.lyzirving.flashvideo.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.util.MatrixUtil;

/**
 * filter is used to draw the content of input texture on screen
 * @author lyzirving
 */
public class ShowFilter extends BaseFilter {
    private static final String TAG = "ShowFilter";

    private int mMatrixHandler;
    private float[] mMatrix = new float[16];

    public ShowFilter(Context ctx) {
        super(ctx, R.raw.trans_vertex_shader, R.raw.default_fragment_shader);
        MatrixUtil.get().initMatrix(mMatrix);
    }

    @Override
    public int draw(int textureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, SCREEN_FRAME_BUFFER_ID);
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

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(mTextureSamplerHandler, 1);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, DEFAULT_VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        MatrixUtil.get().initMatrix(mMatrix);

        return mTextureId[0];
    }

    @Override
    protected void initHandler() {
        super.initHandler();
        mMatrixHandler = GLES20.glGetUniformLocation(mProgram, "u_matrix");
    }

    public void flip(boolean flipX, boolean flipY) {
        MatrixUtil.get().flip(mMatrix, flipX, flipY);
    }

}
