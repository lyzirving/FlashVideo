package com.lyzirving.flashvideo.opengl.util;

import android.opengl.Matrix;

public class MatrixUtil {

    private static volatile MatrixUtil sInstance;

    private MatrixUtil() {}

    public static MatrixUtil get() {
        if (sInstance == null) {
            synchronized (MatrixUtil.class) {
                if (sInstance == null) {
                    sInstance = new MatrixUtil();
                }
            }
        }
        return sInstance;
    }

    public void flip(float[] matrix, boolean flipX, boolean flipY) {
        assert matrix != null;
        if (flipX || flipY) {
            Matrix.scaleM(matrix, 0, flipX ? -1 : 1, flipY ? -1 : 1, 1);
        }
    }

    public void initMatrix(float[] matrix) {
        assert matrix != null;
        Matrix.setIdentityM(matrix, 0);
    }

    public void scale(float[] matrix, float xScale, float yScale) {
        assert matrix != null;
        Matrix.scaleM(matrix, 0, xScale, yScale, 1);
    }

    public void rotate(float[] matrix, float angle) {
        assert matrix != null;
        Matrix.rotateM(matrix, 0, angle, 0, 0, 1);
    }


}
