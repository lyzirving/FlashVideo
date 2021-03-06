package com.lyzirving.flashvideo.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.util.FilterUtil;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.opengl.util.VertexUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import androidx.annotation.RawRes;

/**
 * @author lyzirving
 */
public class BaseFilter implements IFilter {
    protected static final int INVALID_ID = -1;
    protected static final int DEFAULT_VERTEX_COUNT = 6;
    protected static final int SCREEN_FRAME_BUFFER_ID = 0;
    protected static final int MIN_PROGRESS = 0;
    protected static final int MAX_PROGRESS = 100;

    protected int mProgram;
    protected String mVertexShader, mFragmentShader;
    protected int mOutputWidth, mOutputHeight, mTextureWidth, mTextureHeight;
    protected int mVertexPosHandler, mTextureCoordinateHandler, mTextureSamplerHandler;
    protected int[] mTextureId, mFrameBufferId;
    protected FloatBuffer mVertexBuffer, mTexCoordinatesBuffer;
    protected boolean mIsInit;

    protected float[] mVertexCoordinates = new float[18];
    protected float[] mTextureCoordinates = new float[12];
    private final LinkedList<Runnable> mRunPreDraw;

    public BaseFilter(Context ctx) {
        mVertexShader = FilterUtil.get().readRawText(ctx, R.raw.default_vertex_shader);
        mFragmentShader = FilterUtil.get().readRawText(ctx, R.raw.default_fragment_shader);
        mRunPreDraw = new LinkedList<>();
    }

    public BaseFilter(Context ctx, @RawRes int vertexShaderRawId, @RawRes int fragShaderRawId) {
        mVertexShader = FilterUtil.get().readRawText(ctx, vertexShaderRawId);
        mFragmentShader = FilterUtil.get().readRawText(ctx, fragShaderRawId);
        mRunPreDraw = new LinkedList<>();
        mVertexCoordinates = VertexUtil.get().getDefaultVertex();
        mTextureCoordinates = TextureUtil.get().getDefaultTextureCoordinates();
    }

    public void adjustProgress(int progress) {
        //implementation in subclass
    }

    public void adjust(float value) {
        //implementation in subclass
    }

    protected void addPreDrawTask(Runnable task) {
        synchronized (mRunPreDraw) {
            mRunPreDraw.addLast(task);
        }
    }

    protected void addPreDrawTaskHead(Runnable task) {
        synchronized (mRunPreDraw) {
            mRunPreDraw.addFirst(task);
        }
    }

    public boolean isInit() {
        return mIsInit;
    }

    @Override
    final public void init() {
        if (!mIsInit) {
            preInit();
            mTextureId = new int[]{TextureUtil.ID_NO_TEXTURE};
            mFrameBufferId = new int[]{TextureUtil.ID_NO_FRAME_BUFFER};
            mProgram = FilterUtil.get().createProgram(mVertexShader, mFragmentShader);
            initHandler();
            initFloatBuffer();
            initTexture();
            initFrameBuffer();
            onInit();
            mIsInit = true;
        }
    }

    @Override
    public int draw(int textureId) {
        return TextureUtil.ID_NO_TEXTURE;
    }

    protected void onInit() {}

    protected void preInit() {}

    @Override
    public void release() {
        if (mVertexBuffer != null) {
            mVertexBuffer.clear();
        }
        if (mTexCoordinatesBuffer != null) {
            mTexCoordinatesBuffer.clear();
        }
        if (mTextureId != null && mTextureId[0] != TextureUtil.ID_NO_TEXTURE) {
            GLES20.glDeleteTextures(mTextureId.length, mTextureId, 0);
            mTextureId = null;
        }
    }

    public void runPreDraw() {
        synchronized (mRunPreDraw) {
            while (!mRunPreDraw.isEmpty()) {
                mRunPreDraw.poll().run();
            }
        }
    }

    @Override
    final public void setVertexCoordinates(float[] vertex) {
        System.arraycopy(vertex, 0, mVertexCoordinates, 0, vertex.length);
    }

    @Override
    final public void setTextureCoordinates(float[] textureCoordinates) {
        System.arraycopy(textureCoordinates, 0, mTextureCoordinates, 0, textureCoordinates.length);
    }

    @Override
    public void setOutputSize(int width, int height) {
        mOutputWidth = width;
        mOutputHeight = height;
        mTextureWidth = width;
        mTextureHeight = height;
    }

    protected void setFloatValue(final int handler, final float val) {
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(handler, val);
            }
        });
    }

    protected void setFloatValue(final int handler1, final float val1, final int handler2, final float val2) {
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(handler1, val1);
                GLES20.glUniform1f(handler2, val2);
            }
        });
    }

    protected void initFloatBuffer() {
        if (mVertexCoordinates == null) {
            mVertexCoordinates = VertexUtil.get().getDefaultVertex();
        }
        ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(mVertexCoordinates.length * 4);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        mVertexBuffer = vertexByteBuffer.asFloatBuffer();
        mVertexBuffer.put(mVertexCoordinates);
        mVertexBuffer.position(0);

        if (mTextureCoordinates == null) {
            mTextureCoordinates = TextureUtil.get().getDefaultTextureCoordinates();
        }
        ByteBuffer textureCoordinateByteBuffer = ByteBuffer.allocateDirect(mTextureCoordinates.length * 4);
        textureCoordinateByteBuffer.order(ByteOrder.nativeOrder());
        mTexCoordinatesBuffer = textureCoordinateByteBuffer.asFloatBuffer();
        mTexCoordinatesBuffer.put(mTextureCoordinates);
        mTexCoordinatesBuffer.position(0);
    }

    protected void initFrameBuffer() {}

    protected void initHandler() {
        mVertexPosHandler = GLES20.glGetAttribLocation(mProgram, "a_vertex_pos");
        mTextureCoordinateHandler = GLES20.glGetAttribLocation(mProgram, "a_texture_coord_pos");
        mTextureSamplerHandler = GLES20.glGetUniformLocation(mProgram, "s_texture_sampler");
    }

    protected void initTexture() {
        if (mTextureWidth == 0 || mTextureHeight == 0) {
            throw new RuntimeException("initTexture: output size is invalid");
        }
        GLES20.glGenTextures(1, mTextureId, 0);
        for (int i = 0; i < 1; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mTextureWidth,
                    mTextureHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}
