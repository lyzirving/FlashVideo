package com.lyzirving.flashvideo.util;

import android.annotation.SuppressLint;
import android.content.Context;

import java.util.Objects;

import androidx.annotation.NonNull;

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

    public int dp2px(float dp) {
        float scale = mAppCtx.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public int px2dp(float pxValue) {
        final float scale = mAppCtx.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    @NonNull
    public Context ctx() {
        return Objects.requireNonNull(mAppCtx);
    }
}
