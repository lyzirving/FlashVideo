package com.lyzirving.flashvideo.opengl.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import androidx.annotation.NonNull;

/**
 * @author lyzirving
 */
public class BitmapFilter extends BaseFilter {
    private static final String TAG = "BitmapFilter";
    private int mBmpResId;
    private Bitmap mTmp;

    public BitmapFilter(Context ctx, int bmpResId) {
        super(ctx);
        mBmpResId = bmpResId;
    }

    @Override
    public int draw(int textureId) {
        GLES20.glUseProgram(mProgram);
        runPreDraw();

        GLES20.glVertexAttribPointer(mVertexPosHandler, 3, GLES20.GL_FLOAT, false,
                3 * 4, mVertexBuffer);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandler, 2, GLES20.GL_FLOAT, false,
                2 * 4, mTexCoordinatesBuffer);
        GLES20.glEnableVertexAttribArray(mVertexPosHandler);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandler);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
        GLES20.glUniform1i(mTextureSamplerHandler, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, DEFAULT_VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return mTextureId[0];
    }

    @Override
    protected void initTexture() {
        GLES20.glGenTextures(1, mTextureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mTmp, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        mTmp.recycle();
        mTmp = null;
    }

    public void prepareBeforeInit(@NonNull Context ctx, int bgWidth, int bgHeight) {
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(ctx.getResources(), mBmpResId, op);
        int bmpWidth = op.outWidth, bmpHeight = op.outHeight;
        float bmpRatio = bmpWidth * 1f / bmpHeight;
        int dstWidth = bmpWidth, dstHeight = bmpHeight;
        if (bmpWidth > bgWidth || bmpHeight > bgHeight) {
            if (bmpRatio < 1) {
                dstHeight = bgHeight;
                dstWidth = (int) (bgHeight * bmpRatio);
            } else {
                dstWidth = bgWidth;
                dstHeight = (int) (bgWidth / bmpRatio);
                while (dstHeight > bgHeight) {
                    dstWidth -= 10;
                    dstHeight = (int) (dstWidth / bmpRatio);
                }
            }
        }
        setTextureCoordinates(TextureUtil.get().getDefaultTextureCoordinates());
        float horRatio = dstWidth * 1f / bgWidth;
        float verRatio = dstHeight * 1f / bgHeight;
        float[] vertex = new float[]{
                //right - top
                horRatio, verRatio, 0,
                //left - bottom
                -horRatio, -verRatio, 0,
                //left - top
                -horRatio, verRatio, 0,
                //right - top
                horRatio, verRatio, 0,
                //right - bottom
                horRatio, -verRatio, 0,
                //left - bottom
                -horRatio, -verRatio, 0
        };
        setVertexCoordinates(vertex);
        mTmp = BitmapFactory.decodeResource(ctx.getResources(), mBmpResId);
        mOutputWidth = dstWidth;
        mOutputHeight = dstHeight;
        LogUtil.i(TAG, "prepareBeforeInit: output size = (" + mOutputWidth + "," + mOutputHeight + ")" + ", ratio = (" + horRatio + "," + verRatio + ")");
    }
}
