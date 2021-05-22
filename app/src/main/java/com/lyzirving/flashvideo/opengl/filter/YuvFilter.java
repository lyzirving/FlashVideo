package com.lyzirving.flashvideo.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;

import java.nio.ByteBuffer;

/**
 * @author lyzirving
 */
public class YuvFilter extends BaseFilter {
    private static final int TEXTURE_SIZE = 3;
    private static final int Y_INDEX = 0;
    private static final int U_INDEX = 1;
    private static final int V_INDEX = 2;

    private int mSamplerY, mSamplerU, mSamplerV;

    public YuvFilter(Context ctx) {
        super(ctx, R.raw.default_vertex_shader, R.raw.yuv2rgb_fragment_shader);
    }

    public void draw(int width, int height, ByteBuffer yBuffer, ByteBuffer uBuffer, ByteBuffer vBuffer) {
        GLES20.glUseProgram(mProgram);
        mVertexBuffer.position(0);
        mTexCoordinatesBuffer.position(0);
        //transfer vertex coordinate data
        GLES20.glVertexAttribPointer(mVertexPosHandler, 3, GLES20.GL_FLOAT, false,
                3*4, mVertexBuffer);
        //transfer texture coordinate data
        GLES20.glVertexAttribPointer(mTextureCoordinateHandler, 2, GLES20.GL_FLOAT, false,
                2*4, mTexCoordinatesBuffer);
        GLES20.glEnableVertexAttribArray(mVertexPosHandler);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandler);

        //draw on default framebuffer(screen)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[Y_INDEX]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width, height,
                0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[U_INDEX]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width / 2, height / 2,
                0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, uBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[V_INDEX]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width / 2, height / 2,
                0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, vBuffer);

        GLES20.glUniform1i(mSamplerY, Y_INDEX);
        GLES20.glUniform1i(mSamplerU, U_INDEX);
        GLES20.glUniform1i(mSamplerV, V_INDEX);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mVertexCoordinates.length / 3);

        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    @Override
    protected void initHandler() {
        mVertexPosHandler = GLES20.glGetAttribLocation(mProgram, "a_vertex_pos");
        mTextureCoordinateHandler = GLES20.glGetAttribLocation(mProgram, "a_texture_coord_pos");
        mSamplerY = GLES20.glGetUniformLocation(mProgram, "sampler_y");
        mSamplerU = GLES20.glGetUniformLocation(mProgram, "sampler_u");
        mSamplerV = GLES20.glGetUniformLocation(mProgram, "sampler_v");
    }

    @Override
    protected void initTexture() {
        mTextureId = new int[TEXTURE_SIZE];
        GLES20.glGenTextures(TEXTURE_SIZE, mTextureId, 0);
        for (int i = 0; i < TEXTURE_SIZE; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[i]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}
