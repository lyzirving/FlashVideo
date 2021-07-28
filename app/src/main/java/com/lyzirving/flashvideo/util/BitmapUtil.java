package com.lyzirving.flashvideo.util;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.view.PixelCopy;
import android.view.View;
import android.view.Window;

import androidx.annotation.RequiresApi;

/**
 * @author lyzirving
 */
public class BitmapUtil {
    private static final String TAG = "BitmapUtil";

    public static Rect getViewLocationInWindow(Window window, View view) {
        if (window == null || view == null) {
            throw new RuntimeException(TAG + ":getViewLocationInWindow, null input");
        }
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void getBitmapFromWindow(Window window, Rect viewRect, Bitmap dst,
                                           PixelCopy.OnPixelCopyFinishedListener listener,
                                           Handler handler) {
        PixelCopy.request(window, viewRect, dst, listener, handler);
    }
}
