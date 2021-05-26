package com.lyzirving.flashvideo.opengl.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.text.TextUtils;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.util.MatrixUtil;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.util.LogUtil;
import com.lyzirving.flashvideo.util.TimeUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author lyzirving
 */
public class CaptureFilter extends BaseFilter {
    private static final String TAG = "CaptureFilter";

    private int mMatrixHandler;
    private float[] mMatrix = new float[16];
    private ByteBuffer mByteBuffer;
    private String mOutputRootDir;

    public CaptureFilter(Context ctx) {
        super(ctx, R.raw.trans_vertex_shader, R.raw.default_fragment_shader);
        MatrixUtil.get().initMatrix(mMatrix);
    }

    @Override
    public int draw(int textureId) {
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
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
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
        if (mByteBuffer != null) {
            mByteBuffer.clear();
            mByteBuffer = null;
        }
    }

    public void saveCapture(int viewWidth, int viewHeight) {
        if (TextUtils.isEmpty(mOutputRootDir)) {
            LogUtil.e(TAG, "saveCapture: output root directory is empty");
            return;
        }
        if (mByteBuffer == null) {
            mByteBuffer = ByteBuffer.allocate(viewWidth * viewHeight * 4);
            mByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId[0]);
        mByteBuffer.position(0);
        GLES20.glReadPixels(0, 0, viewWidth, viewHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mByteBuffer);
        mByteBuffer.rewind();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, SCREEN_FRAME_BUFFER_ID);

        Bitmap bmp = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(mByteBuffer);
        FileOutputStream fos = null;
        File directory = new File(mOutputRootDir);
        if (!directory.exists() && !directory.mkdirs()) {
            LogUtil.e(TAG, "saveCapture: failed to make root directory");
            return;
        }
        try {
            fos = new FileOutputStream(new File(directory, TimeUtil.getCurrentTimeStr() + ".png"));
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (Exception e) {
            LogUtil.e(TAG, "saveCapture: exception = " + e.getMessage());
            fos = null;
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    LogUtil.e(TAG, "saveCapture: failed to close, msg = " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void setFlip(boolean horizontalFlip, boolean verticalFlip) {
        MatrixUtil.get().flip(mMatrix, horizontalFlip, verticalFlip);
    }

    public void setOutputRootDir(String dir) {
        mOutputRootDir = dir;
    }

}
