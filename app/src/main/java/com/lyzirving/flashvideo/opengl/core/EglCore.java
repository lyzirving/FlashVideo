package com.lyzirving.flashvideo.opengl.core;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;

import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class EglCore {
    private static final String TAG = "EglCore";

    public static final int GL_ES_3_VERSION = 3;
    public static final int GL_ES_2_VERSION = 2;

    /**
     * Android-specific extension.
     */
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    /**
     * Constructor flag: surface must be recordable.
     * This discourages EGL from using a pixel format that cannot be converted efficiently to something usable by the video encoder.
     */
    public static final int FLAG_RECORDABLE = 0x01;

    /**
     * Constructor flag: ask for GLES3, fall back to GLES2 if not available.
     * Without this flag, GLES2 is used.
     */
    public static final int FLAG_TRY_GLES3 = 0x02;

    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mEglSurface = EGL14.EGL_NO_SURFACE;
    private EGLConfig mEglConfig = null;
    private int mGlVersion = -1;

    public boolean createWindowSurface(Object surface) {
        if (mEglSurface != EGL14.EGL_NO_SURFACE) {
            LogUtil.e(TAG, "createWindowSurface: surface already created");
            return false;
        }
        if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
            LogUtil.e(TAG, "createWindowSurface: invalid surface, " + surface);
            return false;
        }
        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttributes = {EGL14.EGL_NONE};
        mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface, surfaceAttributes, 0);
        if (!checkEglError()) {
            LogUtil.e(TAG, "createWindowSurface: error state");
            return false;
        }
        if (mEglSurface == null) {
            LogUtil.e(TAG, "createWindowSurface: created surface is null");
            return false;
        }
        return true;
    }

    public boolean makeCurrent() {
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            LogUtil.e(TAG, "makeCurrent: no display");
            return false;
        }
        if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            LogUtil.e(TAG, "makeCurrent: failed");
            return false;
        }
        return true;
    }

    /**
     * Prepares EGL display and context.
     * @param sharedContext The context to share, or null if sharing is not desired.
     * @param flags Configuration bit flags, e.g. FLAG_RECORDABLE.
     * @return true, if succeed; otherwise, false
     */
    public boolean prepare(EGLContext sharedContext, int flags) {
        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            LogUtil.e(TAG, "prepare: EGL already set up");
            return false;
        }
        if (sharedContext == null) {
            sharedContext = EGL14.EGL_NO_CONTEXT;
        }
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            LogUtil.e(TAG, "prepare: unable to get EGL14 display");
            return false;
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            mEglDisplay = null;
            LogUtil.e(TAG, "prepare: unable to initialize EGL14");
            return false;
        }
        //try to get GL ES 3 context, if requested.
        if ((flags & FLAG_TRY_GLES3) != 0) {
            LogUtil.d(TAG, "prepare: try get GL ES 3 context");
            EGLConfig config = getConfig(flags, GL_ES_3_VERSION);
            if (config != null) {
                int[] glEs3AttributeList = {
                        EGL14.EGL_CONTEXT_CLIENT_VERSION,
                        GL_ES_3_VERSION,
                        EGL14.EGL_NONE
                };
                EGLContext context = EGL14.eglCreateContext(mEglDisplay, config, sharedContext, glEs3AttributeList, 0);
                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                    LogUtil.d(TAG, "get GL ES 3");
                    mEglConfig = config;
                    mEglContext = context;
                    mGlVersion = GL_ES_3_VERSION;
                }
            }
        }
        //GL ES 2 only, or GL ES 3 attempt failed
        if (mEglContext == EGL14.EGL_NO_CONTEXT) {
            LogUtil.d(TAG, "prepare: try to get GL ES 2");
            EGLConfig config = getConfig(flags, GL_ES_2_VERSION);
            if (config == null) {
                LogUtil.e(TAG, "prepare: unable to find a suitable EGLConfig");
                return false;
            }
            int[] egl2AttributeList = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION,
                    GL_ES_2_VERSION,
                    EGL14.EGL_NONE
            };
            EGLContext context = EGL14.eglCreateContext(mEglDisplay, config, sharedContext, egl2AttributeList, 0);
            if (!checkEglError()) {
                LogUtil.e(TAG, "prepare: get EL ES 2 error");
                return false;
            }
            mEglConfig = config;
            mEglContext = context;
            mGlVersion = GL_ES_2_VERSION;
        }
        // Confirm with query.
        int[] values = new int[1];
        EGL14.eglQueryContext(mEglDisplay, mEglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        LogUtil.d(TAG, "prepare: EGLContext created, client version " + values[0]);
        return true;
    }

    public void release() {
        EGL14.eglDestroySurface(mEglDisplay, mEglSurface);
        mEglSurface = EGL14.EGL_NO_SURFACE;

        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.  So for
            // every eglInitialize() we need an eglTerminate().
            EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroyContext(mEglDisplay, mEglContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEglDisplay);
        }
        mEglDisplay = EGL14.EGL_NO_DISPLAY;
        mEglContext = EGL14.EGL_NO_CONTEXT;
        mEglConfig = null;
    }

    /**
     * Sends the presentation time stamp to EGL.
     * @param timeStamp Time is expressed in nanoseconds.
     */
    public void setCurrentTimeStamp(long timeStamp) {
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, mEglSurface, timeStamp);
    }

    public boolean swapBuffers() {
        boolean result = EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
        if (!result) {
            Log.d(TAG, "swapBuffers: failed");
        }
        return result;
    }

    /**
     * Checks for EGL errors.  Throws an exception if an error has been raised.
     */
    private boolean checkEglError() {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            LogUtil.e(TAG, "checkEglError: error code = " + error);
            return false;
        }
        return true;
    }

    /**
     * Finds a suitable EGLConfig.
     * @param flags Bit flags from constructor.
     * @param version Must be 2 or 3.
     */
    private EGLConfig getConfig(int flags, int version) {
        int renderType = EGL14.EGL_OPENGL_ES2_BIT;
        if (version >= GL_ES_3_VERSION) {
            renderType |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;
        }
        // The actual surface is generally RGBA or RGBX, so situationally omitting alpha doesn't really help.
        // It can also lead to a huge performance hit on glReadPixels() when reading into a GL_RGBA buffer.
        int[] attributeList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                //EGL14.EGL_DEPTH_SIZE, 16,
                //EGL14.EGL_STENCIL_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderType,
                EGL14.EGL_NONE,
                // placeholder for recordable [@-3]
                0,
                EGL14.EGL_NONE
        };
        if ((flags & FLAG_RECORDABLE) != 0) {
            attributeList[attributeList.length - 3] = EGL_RECORDABLE_ANDROID;
            attributeList[attributeList.length - 2] = 1;
        }
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEglDisplay, attributeList, 0, configs, 0,
                configs.length, numConfigs, 0)) {
            LogUtil.e(TAG, "getConfig: unable to find RGB8888 " + version + " EGLConfig");
            return null;
        }
        return configs[0];
    }
}
