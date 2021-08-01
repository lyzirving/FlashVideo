package com.lyzirving.flashvideo.ui;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.PixelCopy;
import android.view.View;
import android.widget.ImageView;

import com.airbnb.lottie.LottieAnimationView;
import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.face.FaceDetectAdapter;
import com.lyzirving.flashvideo.face.FaceDetector;
import com.lyzirving.flashvideo.face.FaceRectView;
import com.lyzirving.flashvideo.util.AssetsManager;
import com.lyzirving.flashvideo.util.BitmapUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author lyzirving
 */
public class FaceDetectActivity extends AppCompatActivity implements View.OnClickListener,
        PixelCopy.OnPixelCopyFinishedListener {
    private static final String TAG = "FaceDetectActivity";

    private ImageView mIvSrc;
    private FaceDetector mFaceDetector;
    private Bitmap mBmpInView;
    private Handler mMainHandler;
    private FaceDetectAdapter mFaceDetectAdapter;
    private FaceRectView mFaceRectView;
    private LottieAnimationView mLoadingView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_face_detect);
        initView();
        initData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtil.i(TAG, "onStop");
        if (mFaceDetector != null) {
            mFaceDetector.destroy();
            mFaceDetector = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_init_face_detector: {
                if (mFaceDetector != null) {
                    mFaceDetector.destroy();
                }
                mFaceDetector = new FaceDetector();
                mFaceDetector.init(null, null, getFaceDetectAdapter());
                break;
            }
            case R.id.btn_detect: {
                Rect viewRect = BitmapUtil.getViewLocationInWindow(getWindow(), mIvSrc);
                mBmpInView = Bitmap.createBitmap(viewRect.width(), viewRect.height(), Bitmap.Config.RGB_565);
                showLoadingView(true);
                BitmapUtil.getBitmapFromWindow(getWindow(), viewRect, mBmpInView, this, mMainHandler);
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void onPixelCopyFinished(int copyResult) {
        LogUtil.i(TAG, "onPixelCopyFinished: result = " + copyResult);
        if (copyResult == PixelCopy.SUCCESS) {
            mFaceDetector.detect(mBmpInView);
        } else {
            showLoadingView(false);
        }
    }

    private FaceDetectAdapter getFaceDetectAdapter() {
        if (mFaceDetectAdapter == null) {
            mFaceDetectAdapter = new FaceDetectAdapter() {
                @Override
                public void onFaceDetectFail() {
                    super.onFaceDetectFail();
                    LogUtil.i(TAG, "onFaceDetectFail");
                    showLoadingView(false);
                }

                @Override
                public void onFaceRectFound(int[] faceRectArray) {
                    super.onFaceRectFound(faceRectArray);
                    LogUtil.i(TAG, "onFaceRectFound: count = " + faceRectArray.length / 4);
                    StringBuilder sb = new StringBuilder();
                    int faceCount = faceRectArray.length / 4;
                    for (int i = 0; i < faceCount; i++) {
                        sb.append("index: ").append(i).append(", left = ").append(faceRectArray[i * 4])
                                .append(", top = ").append(faceRectArray[i * 4 + 1])
                                .append(", right = ").append(faceRectArray[i * 4 + 2])
                                .append(", bottom = ").append(faceRectArray[i * 4 + 3]).append("\n");
                    }
                    LogUtil.i(TAG, "onFaceRectFound: " + sb.toString());
                    mFaceRectView.setFaceRect(faceRectArray);
                    showLoadingView(false);
                }

                @Override
                public void onLandmarkFound(int[] landmarks) {
                    super.onLandmarkFound(landmarks);
                    LogUtil.i(TAG, "onLandmarkFound");
                    showLoadingView(false);
                }

                @Override
                public void onNoFaceDetect() {
                    super.onNoFaceDetect();
                    LogUtil.i(TAG, "onNoFaceDetect");
                    showLoadingView(false);
                }

                @Override
                public void noLandmarkDetect() {
                    super.noLandmarkDetect();
                    LogUtil.i(TAG, "noLandmarkDetect");
                    showLoadingView(false);
                }
            };
        }
        return mFaceDetectAdapter;
    }

    private void initView() {
        mIvSrc = findViewById(R.id.iv_src);
        mFaceRectView = findViewById(R.id.face_view);
        mLoadingView = findViewById(R.id.loading_view);
        findViewById(R.id.btn_init_face_detector).setOnClickListener(this);
        findViewById(R.id.btn_detect).setOnClickListener(this);
    }

    private void initData() {
        mMainHandler = new Handler(getMainLooper());
        AssetsManager.get().executeAsyncTask(new Runnable() {
            @Override
            public void run() {
                AssetsManager.get().copyAssets(AssetsManager.AssetsType.CLASSIFIER);
                AssetsManager.get().copyAssets(AssetsManager.AssetsType.LANDMARK);
            }
        });
    }

    private void showLoadingView(boolean show) {
        LogUtil.i(TAG, "showLoadingView: " + show);
        mLoadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            mLoadingView.playAnimation();
        } else {
            mLoadingView.pauseAnimation();
        }
    }
}
