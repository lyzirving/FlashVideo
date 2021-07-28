package com.lyzirving.flashvideo.face;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.lyzirving.flashvideo.util.ComponentUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * @author lyzirving
 */
public class FaceRectView extends View {
    private static final String TAG = "FaceRectView";

    private List<Rect> mFaces;
    private Paint mPaint;

    public FaceRectView(Context context) {
        this(context, null);
    }

    public FaceRectView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceRectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFaces != null && mFaces.size() > 0) {
            Rect face;
            for (int i = 0; i < mFaces.size(); i++) {
                face = mFaces.get(i);
                canvas.drawRect(face, mPaint);
            }
        }
    }

    public void setFaceRect(int[] faces) {
        if (faces == null || faces.length == 0 || faces.length % 4 != 0) {
            LogUtil.i(TAG, "setFaceRect: invalid faces");
            return;
        }
        if (mFaces == null) { mFaces = new ArrayList<>(); }
        mFaces.clear();
        Rect face;
        int faceCount = faces.length / 4;
        LogUtil.i(TAG, "setFaceRect: face count = " + faceCount);
        for (int i = 0; i < faceCount; i++) {
            face = new Rect();
            face.left = faces[i * 4];
            face.top = faces[i * 4 + 1];
            face.right = faces[i * 4 + 2];
            face.bottom = faces[i * 4 + 3];
            mFaces.add(face);
        }
        invalidate();
    }

    private void init() {
        mFaces = new ArrayList<>();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.parseColor("#ff0e00"));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(ComponentUtil.get().dp2px(3));
    }
}
