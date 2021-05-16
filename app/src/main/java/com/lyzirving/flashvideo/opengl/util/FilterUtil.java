package com.lyzirving.flashvideo.opengl.util;

import android.content.Context;
import android.opengl.GLES20;

import com.lyzirving.flashvideo.util.LogUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author lyzirving
 */
public class FilterUtil {
    private static final String TAG = "FilterUtil";
    private static volatile FilterUtil sInstance;
    private FilterUtil() {}

    public static FilterUtil get() {
        if (sInstance == null) {
            synchronized (FilterUtil.class) {
                if (sInstance == null) {
                    sInstance = new FilterUtil();
                }
            }
        }
        return sInstance;
    }

    public int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = -1;
        if ((vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)) == 0) {
            return 0;
        }
        int fragmentShader = -1;
        if ((fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)) == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, fragmentShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                LogUtil.e(TAG, "createProgram: Could not link program: " + GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        } else {
            LogUtil.e(TAG, "createProgram: failed to create program");
        }
        return program;
    }

    public String readRawText(Context ctx, int resId) {
        InputStream inputStream = ctx.getResources().openRawResource(resId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) { builder.append(line).append("\n"); }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private void checkGlError(String op) {
        int error;
        if ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            LogUtil.e(TAG, "checkGlError: " + op + ", error = " + error);
            throw new RuntimeException(op + ", GL error: " + error);
        }
    }

    /**
     * @param shaderType shader GLES20.GL_VERTEX_SHADER   GLES20.GL_FRAGMENT_SHADER
     * @param source shader content
     */
    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                LogUtil.e(TAG, "loadShader: could not compile shader " + shaderType
                        + ", " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        } else {
            LogUtil.e(TAG, "loadShader: failed to load shader");
        }
        return shader;
    }
}
