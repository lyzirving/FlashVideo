package com.lyzirving.flashvideo.opengl.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.lyzirving.flashvideo.util.LogUtil;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author lyzirving
 */
public class TextureUtil {
    private static final String TAG = "TextureUtil";
    public static final int ID_NO_TEXTURE = -1;
    public static final int ID_NO_FRAME_BUFFER = -1;

    private static volatile TextureUtil sInstance;

    /** how the texture coordinate are arranged on screen
     * (0,0)------------------(1,0)
     *   |                   |  |
     *   |   center of screen   |
     *   |       (0.5,0.5)      |
     *   |       |              |
     *   |   |                  |
     * (0,1)------------------(1,1)
     */
    private float[] mDefaultTextureCoordinates = new float[]{
            1, 0,
            0, 1,
            0, 0,
            1, 0,
            1, 1,
            0, 1,
    };

    private TextureUtil() {}

    public static TextureUtil get() {
        if (sInstance == null) {
            synchronized (TextureUtil.class) {
                if (sInstance == null) {
                    sInstance = new TextureUtil();
                }
            }
        }
        return sInstance;
    }

    public void deleteFrameBufferAndTexture(int[] frameBuffers, int[] textures) {
        for (int i = 0; i < frameBuffers.length; i++) {
            GLES20.glDeleteTextures(1, textures, i);
            GLES20.glDeleteFramebuffers(1, frameBuffers, i);
        }
    }

    public void deleteFrameBufferAndTextureAtIndex(int index, int[] frameBuffers, int[] textures) {
        GLES20.glDeleteTextures(1, textures, index);
        GLES20.glDeleteFramebuffers(1, frameBuffers, index);
    }

    public void deleteTexture(int textureId) {
        if (textureId != TextureUtil.ID_NO_TEXTURE) {
            int[] ids = new int[]{textureId};
            GLES20.glDeleteTextures(1, ids, 0);
        }
    }

    public float[] getDefaultTextureCoordinates() {
        int len = mDefaultTextureCoordinates.length;
        float[] result = new float[len];
        System.arraycopy(mDefaultTextureCoordinates, 0, result, 0, len);
        return result;
    }

    public void generateFrameBufferAndTexture(int size, int[] frameBuffers, int[] textures, int width, int height) {
        if (size != frameBuffers.length || size != textures.length) {
            throw new RuntimeException("generateFrameBufferAndTexture: invalid size");
        }
        for (int i = 0; i < size; i++) {
            GLES20.glGenFramebuffers(1, frameBuffers, i);

            GLES20.glGenTextures(1, textures, i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[i]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, textures[i], 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
    }

    public void generateFrameBufferAndTextureAtIndex(int index, int[] frameBuffers, int[] textures, int width, int height) {
        if (index >= frameBuffers.length || index >= textures.length) {
            throw new RuntimeException("generateFrameBufferAndTextureAtIndex: invalid index = " + index + ", size = " + frameBuffers.length);
        }
        GLES20.glGenFramebuffers(1, frameBuffers, index);

        GLES20.glGenTextures(1, textures, index);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[index]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[index]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textures[index], 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public int generateTexture(int[] data, int textureWidth, int textureHeight) {
        LogUtil.d(TAG, "generateTexture:");
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        int textureId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        Bitmap tmp = Bitmap.createBitmap(data, 0, textureWidth, textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        //saveBmp(tmp);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, tmp, 0);
        tmp.recycle();

        return textureId;
    }

    public int generateTexture(Context ctx, int drawableId) {
        LogUtil.d(TAG, "generateTexture:");
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        int textureId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        InputStream is = ctx.getResources().openRawResource(drawableId);
        Bitmap bitmapTmp;
        try {
            bitmapTmp = BitmapFactory.decodeStream(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }//end
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmapTmp, 0);
        bitmapTmp.recycle();
        return textureId;
    }

    public int generateOesTexture() {
        int[] textureId = new int[1];
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return textureId[0];
    }

    public void updateTexture(int textureId, int[] data, int textureWidth, int textureHeight) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        Bitmap tmp = Bitmap.createBitmap(data, 0, textureWidth, textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, tmp, 0);
        tmp.recycle();
    }
}
