package com.lyzirving.flashvideo.util;

import android.annotation.SuppressLint;
import android.content.Context;

import java.util.Objects;

/**
 * @author lyzirving
 */
public class ComponentUtil {

    @SuppressLint("StaticFieldLeak")
    private static volatile ComponentUtil sInstance;
    private Context mAppCtx;

    private ComponentUtil() {}

    public static ComponentUtil get() {
        if (sInstance == null) {
            synchronized (ComponentUtil.class) {
                if (sInstance == null) {
                    sInstance = new ComponentUtil();
                }
            }
        }
        return sInstance;
    }

    public void init(Context ctx) {
        mAppCtx = ctx;
    }

    public void destroy() {
        mAppCtx = null;
    }

    public Context getAppCtx() {
        return Objects.requireNonNull(mAppCtx);
    }
}
