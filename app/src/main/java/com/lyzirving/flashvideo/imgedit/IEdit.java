package com.lyzirving.flashvideo.imgedit;

import android.graphics.Bitmap;

/**
 * @author lyzirving
 */
public interface IEdit {
    void setImageBitmap(Bitmap bmp, boolean forceRender);
    void setImageResource(int srcId, boolean forceRender);
}
