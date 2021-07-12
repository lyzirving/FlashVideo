package com.lyzirving.flashvideo.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.PixelCopy;
import android.view.View;
import android.widget.ImageView;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.face.FaceDetectAdapter;
import com.lyzirving.flashvideo.face.FaceDetector;
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

    private ImageView mIvImg;
    private FaceDetector mFaceDetector;
    private Bitmap mBmpInView;
    private Handler mMainHandler;
    private FaceDetectAdapter mFaceDetectAdapter;

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
                mFaceDetector.init(null, getFaceDetectAdapter());
                break;
            }
            case R.id.btn_detect: {
                if (mBmpInView == null) {
                    BitmapFactory.Options op = new BitmapFactory.Options();
                    op.inJustDecodeBounds = true;
                    BitmapFactory.decodeResource(getResources(), R.drawable.multiplefaces, op);
                    mBmpInView = Bitmap.createBitmap(op.outWidth, op.outHeight, op.outConfig);
                }
                BitmapUtil.getBitmapFromView(getWindow(), mIvImg, mBmpInView, this, mMainHandler);
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
        }
    }

    private void initView() {
        mIvImg = findViewById(R.id.iv_img);

        findViewById(R.id.btn_init_face_detector).setOnClickListener(this);
        findViewById(R.id.btn_detect).setOnClickListener(this);
    }

    private void initData() {
        mMainHandler = new Handler(getMainLooper());
        AssetsManager.get().executeAsyncTask(new Runnable() {
            @Override
            public void run() {
                AssetsManager.get().copyAssets(AssetsManager.AssetsType.CLASSIFIER);
            }
        });
    }

    private FaceDetectAdapter getFaceDetectAdapter() {
        if (mFaceDetectAdapter == null) {
            mFaceDetectAdapter = new FaceDetectAdapter() {
                @Override
                public void onFaceDetectFail() {
                    super.onFaceDetectFail();
                    LogUtil.i(TAG, "onFaceDetectFail");
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
                }

                @Override
                public void onNoFaceDetect() {
                    super.onNoFaceDetect();
                    LogUtil.i(TAG, "onNoFaceDetect");
                }
            };
        }
        return mFaceDetectAdapter;
    }
}
