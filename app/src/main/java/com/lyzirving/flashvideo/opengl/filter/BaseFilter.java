package com.lyzirving.flashvideo.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.util.FilterUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import androidx.annotation.IdRes;

/**
 * @author lyzirving
 */
public class BaseFilter implements IFilter {

    protected int mProgram;
    protected String mVertexShader, mFragmentShader;
    protected int mVertexPosHandler, mTextureCoordinateHandler, mTextureSamplerHandler;
    protected int[] mTextureId;
    protected FloatBuffer mVertexBuffer, mTexCoordinatesBuffer;
    protected boolean mIsInit;

    protected float[] mVertexCoordinates, mTextureCoordinates;

    public BaseFilter(Context ctx) {
        mVertexShader = FilterUtil.get().readRawText(ctx, R.raw.default_vertex_shader);
        mFragmentShader = FilterUtil.get().readRawText(ctx, R.raw.default_fragment_shader);
    }

    public BaseFilter(Context ctx,@IdRes int vertexShaderRawId,@IdRes int fragShaderRawId) {
        mVertexShader = FilterUtil.get().readRawText(ctx, vertexShaderRawId);
        mFragmentShader = FilterUtil.get().readRawText(ctx, fragShaderRawId);
    }

    @Override
    final public void init() {
        if (!mIsInit) {
            mProgram = FilterUtil.get().createProgram(mVertexShader, mFragmentShader);
            onInitShader();
            initFloatBuffer();
            initTexture();
            mIsInit = true;
        }
    }

    @Override
    public void draw(int textureId) {

    }

    @Override
    public void release() {
        mVertexBuffer.clear();
        mTexCoordinatesBuffer.clear();
    }

    @Override
    final public void setVertexCoordinates(float[] vertex) {
        mVertexCoordinates = vertex;
    }

    @Override
    final public void setTextureCoordinates(float[] textureCoordinates) {
        mTextureCoordinates = textureCoordinates;
    }

    protected void initFloatBuffer() {
        if (mVertexCoordinates == null) {
            throw new RuntimeException("vertex coordinates are null");
        }
        ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(mVertexCoordinates.length * 4);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        mVertexBuffer = vertexByteBuffer.asFloatBuffer();
        mVertexBuffer.put(mVertexCoordinates);
        mVertexBuffer.position(0);

        if (mTextureCoordinates == null) {
            throw new RuntimeException("texture coordinates are null");
        }
        ByteBuffer textureCoordinateByteBuffer = ByteBuffer.allocateDirect(mTextureCoordinates.length * 4);
        textureCoordinateByteBuffer.order(ByteOrder.nativeOrder());
        mTexCoordinatesBuffer = textureCoordinateByteBuffer.asFloatBuffer();
        mTexCoordinatesBuffer.put(mTextureCoordinates);
        mTexCoordinatesBuffer.position(0);
    }

    protected void initTexture() {
        mTextureId = new int[1];
        GLES20.glGenTextures(1, mTextureId, 0);
        for (int i = 0; i < 1; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[i]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    protected void onInitShader() {
        mVertexPosHandler = GLES20.glGetAttribLocation(mProgram, "a_vertex_pos");
        mTextureCoordinateHandler = GLES20.glGetAttribLocation(mProgram, "a_texture_coord_pos");
        mTextureSamplerHandler = GLES20.glGetUniformLocation(mProgram, "s_texture_sampler");
    }
}
