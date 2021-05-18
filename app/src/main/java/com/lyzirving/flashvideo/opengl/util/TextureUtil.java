package com.lyzirving.flashvideo.opengl.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Environment;

import com.lyzirving.flashvideo.util.LogUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author lyzirving
 */
public class TextureUtil {
    private static final String TAG = "TextureUtil";
    public static final int ID_NO_TEXTURE = -1;

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

    public float[] getDefaultTextureCoordinates() {
        int len = mDefaultTextureCoordinates.length;
        float[] result = new float[len];
        System.arraycopy(mDefaultTextureCoordinates, 0, result, 0, len);
        return result;
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

    private void saveBmp(Bitmap bmp) {
        StringBuilder dst = new StringBuilder();
        dst.append(Environment.getExternalStorageDirectory().getAbsolutePath())
                .append(File.separator).append("test")
                .append(File.separator).append("source")
                .append(File.separator).append(System.currentTimeMillis()).append(".jpg");
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(dst.toString());
            if (fos != null) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void updateTexture(int textureId, int[] data, int textureWidth, int textureHeight) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        Bitmap tmp = Bitmap.createBitmap(data, 0, textureWidth, textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, tmp, 0);
        tmp.recycle();
    }
}