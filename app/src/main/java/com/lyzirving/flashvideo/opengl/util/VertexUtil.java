package com.lyzirving.flashvideo.opengl.util;

import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class VertexUtil {
    private static final String TAG = "VertexUtil";

    private static volatile VertexUtil sInstance;

    /** how the vertexes are arranged on screen
     *  0 means center of screen
     *   3-----------1(4)
     *   |         | |
     *   |       |   |
     *   |   （0）    |
     *   |   |       |
     *   | |         |
     *   2(6)--------5
     */
    private float[] mDefaultVertexCoordinates = new float[]{
            //right - top
            1, 1, 0,
            //left - bottom
            -1, -1, 0,
            //left - top
            -1, 1, 0,
            //right - top
            1, 1, 0,
            //right - bottom
            1, -1, 0,
            //left - bottom
            -1, -1, 0
    };

    private VertexUtil() {}

    public static VertexUtil get() {
        if (sInstance == null) {
            synchronized (VertexUtil.class) {
                if (sInstance == null) {
                    sInstance = new VertexUtil();
                }
            }
        }
        return sInstance;
    }

    public float[] getDefaultVertexCoordinates() {
        int len = mDefaultVertexCoordinates.length;
        float[] result = new float[len];
        System.arraycopy(mDefaultVertexCoordinates, 0, result, 0, len);
        return result;
    }

    public float[] createVertexCoordinates(int width, int height) {
        boolean mainHorizontal = (width >= height);
        float ratio = mainHorizontal ? (height * 1f / width) : (width * 1f / height);
        float[] result;
        if (mainHorizontal) {
            result = new float[] {
                    1, ratio, 0,
                    -1, -ratio, 0,
                    -1, ratio, 0,
                    1, ratio, 0,
                    1, -ratio, 0,
                    -1, -ratio, 0
            };
        } else {
            result = new float[] {
                    ratio, 1, 0,
                    -ratio, -1, 0,
                    -ratio, 1, 0,
                    ratio, 1, 0,
                    ratio, -1, 0,
                    -ratio, -1, 0
            };
        }
        return result;
    }
}
