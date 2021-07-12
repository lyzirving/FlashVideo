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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void getBitmapFromView(Window window, View view, Bitmap dst,
                                  PixelCopy.OnPixelCopyFinishedListener listener,
                                  Handler handler) {
        if (window == null || view == null) {
            LogUtil.i(TAG, "getBitmapFromView: invalid input");
            return;
        }
        int[] location = new int[2];
        view.getLocationInWindow(location);
        Rect rect = new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
        LogUtil.i(TAG, "getBitmapFromView: view rect in window = " + rect.toString());
        PixelCopy.request(window, rect, dst, listener, handler);
    }
}
