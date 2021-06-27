package com.lyzirving.flashvideo.edit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.util.ComponentUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

/**
 * @author lyzirving
 */
public class ProgressSelectBar extends View {
    private static final String TAG = "ProgressSelectBar";

    private int mAnchorSrc, mAnchorSize, mBarLength, mBarHeight, mBarCorner;
    private @ColorInt int mSelectorBg;
    private Bitmap mAnchorBmp;

    private boolean mLeftAnchorPress, mRightAnchorPress;
    private float mLastX, mLastY;
    private RectF mLeftAnchorDrawingRect, mRightAnchorDrawingRect;
    private RectF mLeftAnchorTouchRect, mRightAnchorTouchRect;

    private float mTouchHorRatio;

    private Paint mPaint;
    private boolean mActive;

    private ProgressSelectBarListener mListener;

    public ProgressSelectBar(Context context) {
        this(context, null);
    }

    public ProgressSelectBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressSelectBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ProgressSelectBar);
        mAnchorSrc = ta.getResourceId(R.styleable.ProgressSelectBar_anchor_src, R.drawable.anchor);
        mAnchorSize = ta.getDimensionPixelSize(R.styleable.ProgressSelectBar_anchor_size, ComponentUtil.get().dp2px(9));
        mSelectorBg = ta.getColor(R.styleable.ProgressSelectBar_bar_background, Color.parseColor("#48D1CC"));
        mBarLength = ta.getDimensionPixelSize(R.styleable.ProgressSelectBar_bar_length, ComponentUtil.get().dp2px(250));
        mBarHeight = ta.getDimensionPixelSize(R.styleable.ProgressSelectBar_bar_height, ComponentUtil.get().dp2px(10));
        mBarCorner = ta.getDimensionPixelSize(R.styleable.ProgressSelectBar_bar_corner, ComponentUtil.get().dp2px(5));
        ta.recycle();
        mTouchHorRatio = 2;
        init();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            width = mBarLength + mAnchorSize;
        }
        //process the abnormal case
        if (width < mBarLength + mAnchorSize) {
            if (width < mAnchorSize) {
                width = 4 * mAnchorSize;
            }
            mBarLength = width - mAnchorSize;
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            height = mAnchorSize * 2 + mBarHeight;
        }
        //process the abnormal case
        if (height < mAnchorSize * 2 + mBarHeight) {
            height = mAnchorSize * 2 + mBarHeight;
        }
        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mAnchorBmp == null) {
            mAnchorBmp = createAnchor(mAnchorSize, mAnchorSrc, mPaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        createAnchorRectIfNeed(width, height);
        float left = getPaddingLeft() + mAnchorSize / 2f;
        float right = width - getPaddingRight() - mAnchorSize / 2f;
        float top = height / 2f - mBarHeight / 2f;
        float bottom = top + mBarHeight;
        mPaint.setColor(mSelectorBg);
        canvas.drawRoundRect(left, top, right, bottom, mBarCorner, mBarCorner, mPaint);

        canvas.drawBitmap(mAnchorBmp, mLeftAnchorDrawingRect.left, mLeftAnchorDrawingRect.top, mPaint);
        canvas.drawBitmap(mAnchorBmp, mRightAnchorDrawingRect.left, mRightAnchorDrawingRect.top, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mLeftAnchorTouchRect == null || mRightAnchorTouchRect == null) {
            LogUtil.d(TAG, "onTouchEvent: rect is invalid");
            return true;
        }
        if (!mActive) {
            return true;
        }
        int action = event.getAction();
        float x = event.getX(), y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if (mRightAnchorTouchRect.contains(x, y) && mLeftAnchorTouchRect.contains(x, y)) {
                    if (Math.abs(x - mLeftAnchorTouchRect.centerX()) < Math.abs(x - mRightAnchorTouchRect.centerX())) {
                        mLeftAnchorPress = true;
                    } else {
                        mRightAnchorPress = true;
                    }
                    mLastX = x;
                    mLastY = y;
                } else if (mRightAnchorTouchRect.contains(x, y)) {
                    mLastX = x;
                    mLastY = y;
                    mRightAnchorPress = true;
                } else if (mLeftAnchorTouchRect.contains(x, y)) {
                    mLastX = x;
                    mLastY = y;
                    mLeftAnchorPress = true;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mLeftAnchorPress) {
                    updateLeftAnchor(x - mLastX);
                    mLastX = x;
                    mLastY = y;
                    checkLeftAnchor();
                    invalidate();
                } else if (mRightAnchorPress) {
                    updateRightAnchor(x - mLastX);
                    mLastX = x;
                    mLastY = y;
                    checkRightAnchor();
                    invalidate();
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (mLeftAnchorPress) {
                    if (mListener != null) {
                        mListener.onLeftAnchorUp(getLeftAnchorRatio());
                    }
                } else if (mRightAnchorPress) {
                    if (mListener != null) {
                        mListener.onRightAnchorUp(getRightAnchorRatio());
                    }
                }
                mLeftAnchorPress = false;
                mRightAnchorPress = false;
                break;
            }
            default: {
                LogUtil.d(TAG, "onTouchEvent: other action = " + action);
                break;
            }
        }
        return true;
    }

    public void active(boolean active) {
        LogUtil.d(TAG, "active: " + active);
        mActive = active;
    }

    public void setListener(ProgressSelectBarListener listener) {
        mListener = listener;
    }

    private Bitmap createAnchor(int anchorSize, int bmpSrcId, Paint paint) {
        Bitmap result = Bitmap.createBitmap(anchorSize, anchorSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Bitmap src = BitmapFactory.decodeResource(getResources(), bmpSrcId);
        Matrix matrix = new Matrix();
        int srcWidth = src.getWidth(), srcHeight = src.getHeight();
        float srcRatio = srcWidth * 1f / srcHeight;
        int targetWidth = srcWidth, targetHeight = srcHeight;
        if (srcWidth > anchorSize && srcHeight > anchorSize) {
            if (srcWidth >= srcHeight) {
                targetWidth = anchorSize;
                targetHeight = (int) (anchorSize / srcRatio);
            } else {
                targetHeight = anchorSize;
                targetWidth = (int)(anchorSize * srcRatio);
            }
        } else if (srcWidth > anchorSize) {
            targetWidth = anchorSize;
            targetHeight = (int) (anchorSize / srcRatio);
        } else if (srcHeight > anchorSize) {
            targetHeight = anchorSize;
            targetWidth = (int)(anchorSize * srcRatio);
        }
        matrix.setScale(targetWidth * 1f / srcWidth, targetHeight * 1f / srcHeight);
        canvas.drawBitmap(src, matrix, paint);
        return result;
    }

    private void createAnchorRectIfNeed(int width, int height) {
        if (mLeftAnchorDrawingRect == null) {
            mLeftAnchorDrawingRect = new RectF();
            mLeftAnchorDrawingRect.left = getPaddingLeft();
            mLeftAnchorDrawingRect.right = mLeftAnchorDrawingRect.left + mAnchorSize;
            mLeftAnchorDrawingRect.top = height / 2f + mBarHeight / 2f;
            mLeftAnchorDrawingRect.bottom = mLeftAnchorDrawingRect.top + mAnchorSize;
            mLeftAnchorTouchRect = new RectF();
            mLeftAnchorTouchRect.left = mLeftAnchorDrawingRect.left - mTouchHorRatio * mAnchorSize;
            mLeftAnchorTouchRect.right = mLeftAnchorDrawingRect.right + mTouchHorRatio * mAnchorSize;
            mLeftAnchorTouchRect.top = 0;
            mLeftAnchorTouchRect.bottom = height;

            mRightAnchorDrawingRect = new RectF();
            mRightAnchorDrawingRect.left = width - getPaddingRight() - mAnchorSize;
            mRightAnchorDrawingRect.right = mRightAnchorDrawingRect.left + mAnchorSize;
            mRightAnchorDrawingRect.top = height / 2f + mBarHeight / 2f;
            mRightAnchorDrawingRect.bottom = mRightAnchorDrawingRect.top + mAnchorSize;
            mRightAnchorTouchRect = new RectF();
            mRightAnchorTouchRect.left = mRightAnchorDrawingRect.left - mTouchHorRatio * mAnchorSize;
            mRightAnchorTouchRect.right = mRightAnchorDrawingRect.right + mTouchHorRatio * mAnchorSize;
            mRightAnchorTouchRect.top = 0;
            mRightAnchorTouchRect.bottom = height;
        }
    }

    private void checkLeftAnchor() {
        float paddingLeft = getPaddingLeft();
        if (mLeftAnchorDrawingRect.left < paddingLeft) {
            mLeftAnchorDrawingRect.left = paddingLeft;
            mLeftAnchorDrawingRect.right = paddingLeft + mAnchorSize;
            mLeftAnchorTouchRect.left = mLeftAnchorDrawingRect.left - mTouchHorRatio * mAnchorSize;
            mLeftAnchorTouchRect.right = mLeftAnchorDrawingRect.right + mTouchHorRatio * mAnchorSize;
        }
        if (mLeftAnchorDrawingRect.right > mRightAnchorDrawingRect.left - mAnchorSize) {
            mLeftAnchorDrawingRect.right = mRightAnchorDrawingRect.left - mAnchorSize;
            mLeftAnchorDrawingRect.left = mLeftAnchorDrawingRect.right - mAnchorSize;
            mLeftAnchorTouchRect.left = mLeftAnchorDrawingRect.left - mTouchHorRatio * mAnchorSize;
            mLeftAnchorTouchRect.right = mLeftAnchorDrawingRect.right + mTouchHorRatio * mAnchorSize;
        }
    }

    private void checkRightAnchor() {
        int paddingRight = getPaddingRight();
        int width = getMeasuredWidth();
        if (mRightAnchorDrawingRect.right > width - paddingRight) {
            mRightAnchorDrawingRect.right = width - paddingRight;
            mRightAnchorDrawingRect.left = mRightAnchorDrawingRect.right - mAnchorSize;
            mRightAnchorTouchRect.left = mRightAnchorDrawingRect.left - mTouchHorRatio * mAnchorSize;
            mRightAnchorTouchRect.right = mRightAnchorDrawingRect.right + mTouchHorRatio * mAnchorSize;
        }
        if (mRightAnchorDrawingRect.left < mLeftAnchorDrawingRect.right + mAnchorSize) {
            mRightAnchorDrawingRect.left = mLeftAnchorDrawingRect.right + mAnchorSize;
            mRightAnchorDrawingRect.right = mRightAnchorDrawingRect.left + mAnchorSize;
            mRightAnchorTouchRect.left = mRightAnchorDrawingRect.left - mTouchHorRatio * mAnchorSize;
            mRightAnchorTouchRect.right = mRightAnchorDrawingRect.right + mTouchHorRatio * mAnchorSize;
        }
    }

    private float getLeftAnchorRatio() {
        if (mLeftAnchorDrawingRect == null) {
            LogUtil.d(TAG, "getLeftAnchorRatio: rect is null");
            return 0;
        }
        return mLeftAnchorDrawingRect.centerX() / mBarLength;
    }

    private float getRightAnchorRatio() {
        if (mRightAnchorDrawingRect == null) {
            LogUtil.d(TAG, "getRightAnchorRatio: rect is null");
            return 0;
        }
        return mRightAnchorDrawingRect.centerX() / mBarLength;
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        mActive = false;
    }

    private void updateLeftAnchor(float dx) {
        mLeftAnchorDrawingRect.left += dx;
        mLeftAnchorDrawingRect.right += dx;
        mLeftAnchorTouchRect.left += dx;
        mLeftAnchorTouchRect.right += dx;
    }

    private void updateRightAnchor(float dx) {
        mRightAnchorDrawingRect.left += dx;
        mRightAnchorDrawingRect.right += dx;
        mRightAnchorTouchRect.left += dx;
        mRightAnchorTouchRect.right += dx;
    }

    public interface ProgressSelectBarListener {
        /**
         * called when left finishes moving;
         * @param leftRatio specify the position of left anchor [0, 1]
         */
        void onLeftAnchorUp(float leftRatio);

        /**
         * called when right finishes moving;
         * @param rightRatio specify the position of right anchor [0, 1]
         */
        void onRightAnchorUp(float rightRatio);
    }
}
