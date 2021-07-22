package com.lyzirving.flashvideo.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.SeekBar;

import com.airbnb.lottie.LottieAnimationView;
import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.imgedit.ImgEditView;
import com.lyzirving.flashvideo.imgedit.algorithm.ImgAlgorithm;
import com.lyzirving.flashvideo.imgedit.algorithm.ImgAlgorithmListener;
import com.lyzirving.flashvideo.imgedit.filter.ImgContrastFilter;
import com.lyzirving.flashvideo.imgedit.filter.ImgSaturationFilter;
import com.lyzirving.flashvideo.imgedit.filter.ImgSharpenFilter;
import com.lyzirving.flashvideo.util.AssetsManager;
import com.lyzirving.flashvideo.util.LogUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * @author lyzirving
 */
public class ImgEditActivity extends AppCompatActivity implements ImgEditView.ImgEditViewListener,
        View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "ImgEditActivity";
    private static final int MSG_SHOW_LOADING_ANIM = 1;

    private int mAniDistance;
    private ImgEditView mImgEditView;
    private ConstraintLayout mRootAdjustBeauty;
    private LottieAnimationView mLottieLoading;

    private SeekBar mSeekBarContrast, mSeekBarSharpen, mSeekBarSaturation;
    private int mSrcId = R.drawable.landscape1;

    private ImgAlgorithm mAlgorithm;
    private ImgAlgorithmListener mAlgorithmListener;
    private boolean mHasExecuteHistEqual;
    private boolean mEnableAutoBeauty = false;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SHOW_LOADING_ANIM: {
                    showLoadingView(msg.arg1 > 0);
                    if (msg.obj instanceof Bitmap) {
                        Bitmap pic = (Bitmap) msg.obj;
                        mImgEditView.setImageBitmap(pic, false);
                        mImgEditView.addFilter(ImgContrastFilter.class.getSimpleName(), false);
                        mImgEditView.addFilter(ImgSharpenFilter.class.getSimpleName(), false);
                        mImgEditView.addFilter(ImgSaturationFilter.class.getSimpleName(), true);
                    }
                    break;
                }
                default: {
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img_edit);
        initView();
        initData();
    }

    @Override
    public void onViewCreate() {}

    @Override
    public void onViewChange(int width, int height) {
        mImgEditView.setImageResource(mSrcId, true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_beautify: {
                boolean show = mRootAdjustBeauty.getVisibility() == View.VISIBLE;
                showRootAdjust(mRootAdjustBeauty, !show);
                if (!show && !mHasExecuteHistEqual && mEnableAutoBeauty) {
                    mHasExecuteHistEqual = true;
                    showLoadingView(true);
                    AssetsManager.get().executeAsyncTask(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap tmp = BitmapFactory.decodeResource(getResources(), mSrcId);
                            mAlgorithm.histEqual(tmp);
                        }
                    });
                } else if (!show) {
                    mImgEditView.addFilter(ImgContrastFilter.class.getSimpleName(), false);
                    mImgEditView.addFilter(ImgSharpenFilter.class.getSimpleName(), false);
                    mImgEditView.addFilter(ImgSaturationFilter.class.getSimpleName(), true);
                }
                break;
            }
            case R.id.layout_clear: {
                mHasExecuteHistEqual = false;
                mImgEditView.setImageResource(mSrcId, false);
                mImgEditView.clear();
                mSeekBarContrast.setProgress(50);
                mSeekBarSharpen.setProgress(50);
                mSeekBarSaturation.setProgress(50);
                showRootAdjust(mRootAdjustBeauty, false);
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.i(TAG, "onDestroy");
        mAlgorithm.release();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()) {
            case R.id.seek_bar_contrast: {
                mImgEditView.adjust(ImgContrastFilter.class.getSimpleName(), seekBar.getProgress());
                break;
            }
            case R.id.seek_bar_sharpen: {
                mImgEditView.adjust(ImgSharpenFilter.class.getSimpleName(), seekBar.getProgress());
                break;
            }
            case R.id.seek_bar_saturation: {
                mImgEditView.adjust(ImgSaturationFilter.class.getSimpleName(), seekBar.getProgress());
                break;
            }
            default: {
                break;
            }
        }
    }

    private ImgAlgorithmListener getAlgorithmListener() {
        if (mAlgorithmListener == null) {
            mAlgorithmListener = new ImgAlgorithmListener() {
                @Override
                public void onFail() {
                    super.onFail();
                    LogUtil.i(TAG, "onFail");
                }

                @Override
                public void onGetImage(final Bitmap bitmap) {
                    super.onGetImage(bitmap);
                    LogUtil.i(TAG, "onGetImage");
                    mHandler.obtainMessage(MSG_SHOW_LOADING_ANIM, 0, 0, bitmap).sendToTarget();
                }
            };
        }
        return mAlgorithmListener;
    }

    private void initView() {
        mImgEditView = findViewById(R.id.view_img_edit);
        mRootAdjustBeauty = findViewById(R.id.layout_adjust_beauty_root);
        mSeekBarContrast = findViewById(R.id.seek_bar_contrast);
        mSeekBarSharpen = findViewById(R.id.seek_bar_sharpen);
        mSeekBarSaturation = findViewById(R.id.seek_bar_saturation);
        mLottieLoading = findViewById(R.id.lottie_loading);

        mSeekBarContrast.setProgress(50);
        mSeekBarContrast.setOnSeekBarChangeListener(this);
        mSeekBarSharpen.setProgress(50);
        mSeekBarSharpen.setOnSeekBarChangeListener(this);
        mSeekBarSaturation.setProgress(50);
        mSeekBarSaturation.setOnSeekBarChangeListener(this);
        findViewById(R.id.layout_beautify).setOnClickListener(this);
        findViewById(R.id.layout_clear).setOnClickListener(this);
    }

    private void initData() {
        mImgEditView.setListener(this);
        mAlgorithm = new ImgAlgorithm(getAlgorithmListener());

        mImgEditView.post(new Runnable() {
            @Override
            public void run() {
                View guide = findViewById(R.id.hor_guide_dot_92);
                View root = findViewById(R.id.img_edit_activity_root);
                mAniDistance = root.getBottom() - guide.getBottom();
            }
        });
    }

    private void showLoadingView(boolean show) {
        mLottieLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            mLottieLoading.playAnimation();
        } else {
            mLottieLoading.pauseAnimation();
        }
    }

    private void showRootAdjust(View view, boolean show) {
        if (show) {
            AnimationSet set = new AnimationSet(false);
            AlphaAnimation alphaAni = new AlphaAnimation(0, 1);
            TranslateAnimation transAni = new TranslateAnimation(0, 0, mAniDistance, 0);
            set.addAnimation(alphaAni);
            set.addAnimation(transAni);
            set.setDuration(500);
            view.setAnimation(set);
            view.setVisibility(View.VISIBLE);
            set.start();
        } else {
            view.setVisibility(View.GONE);
        }
    }
}
