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
import com.lyzirving.flashvideo.imgedit.filter.ImgGaussianFilter;
import com.lyzirving.flashvideo.imgedit.filter.ImgSaturationFilter;
import com.lyzirving.flashvideo.imgedit.filter.ImgSharpenFilter;
import com.lyzirving.flashvideo.imgedit.filter.ImgToonFilter;
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
    private ConstraintLayout mRootAdjustBeauty, mRootAdjustDenoise;
    private LottieAnimationView mLottieLoading;

    private SeekBar mSeekBarContrast, mSeekBarSharpen, mSeekBarSaturation, mSeekBarHorDenoise, mSeekBarVerDenoise,
            mSeekbarQuantization;
    private int mSrcId = R.drawable.landscape;

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
                        mImgEditView.addFilter(new ImgContrastFilter(getApplicationContext()), false);
                        mImgEditView.addFilter(new ImgSharpenFilter(getApplicationContext()), false);
                        mImgEditView.addFilter(new ImgSaturationFilter(getApplicationContext()), true);
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
                if (rootAdjustShow()) {
                    hideAllRootAdjust();
                    return;
                }
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
                    mImgEditView.addFilter(new ImgContrastFilter(getApplicationContext()), false);
                    mImgEditView.addFilter(new ImgSharpenFilter(getApplicationContext()), false);
                    mImgEditView.addFilter(new ImgSaturationFilter(getApplicationContext()), true);
                }
                break;
            }
            case R.id.layout_denoise: {
                if (rootAdjustShow()) {
                    hideAllRootAdjust();
                    return;
                }
                boolean show = mRootAdjustDenoise.getVisibility() == View.VISIBLE;
                showRootAdjust(mRootAdjustDenoise, !show);
                if (!show) {
                    mImgEditView.addFilter(new ImgGaussianFilter(getApplicationContext()), false);
                    mImgEditView.addFilter(new ImgToonFilter(getApplicationContext()), true);
                }
                break;
            }
            case R.id.layout_clear: {
                mHasExecuteHistEqual = false;
                mImgEditView.setImageResource(mSrcId, false);
                mImgEditView.clear();
                hideAllRootAdjust();
                setSeekBarDefault();
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
            case R.id.seek_bar_hor_blur: {
                mImgEditView.adjustHorDenoise(seekBar.getProgress());
                break;
            }
            case R.id.seek_bar_ver_blur: {
                mImgEditView.adjustVerDenoise(seekBar.getProgress());
                break;
            }
            case R.id.seek_bar_toon_quantization_level: {
                mImgEditView.adjustToonQuantizationLevel(seekBar.getProgress());
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

    private void hideAllRootAdjust() {
        mRootAdjustBeauty.setVisibility(View.GONE);
        mRootAdjustDenoise.setVisibility(View.GONE);
    }

    private void initView() {
        mImgEditView = findViewById(R.id.view_img_edit);
        mRootAdjustBeauty = findViewById(R.id.layout_adjust_beauty_root);
        mRootAdjustDenoise = findViewById(R.id.layout_adjust_denoise_root);
        mSeekBarContrast = findViewById(R.id.seek_bar_contrast);
        mSeekBarSharpen = findViewById(R.id.seek_bar_sharpen);
        mSeekBarSaturation = findViewById(R.id.seek_bar_saturation);
        mSeekBarHorDenoise = findViewById(R.id.seek_bar_hor_blur);
        mSeekBarVerDenoise = findViewById(R.id.seek_bar_ver_blur);
        mSeekbarQuantization = findViewById(R.id.seek_bar_toon_quantization_level);
        mLottieLoading = findViewById(R.id.lottie_loading);

        mSeekBarContrast.setOnSeekBarChangeListener(this);
        mSeekBarSharpen.setOnSeekBarChangeListener(this);
        mSeekBarSaturation.setOnSeekBarChangeListener(this);
        mSeekBarHorDenoise.setOnSeekBarChangeListener(this);
        mSeekBarVerDenoise.setOnSeekBarChangeListener(this);
        mSeekbarQuantization.setOnSeekBarChangeListener(this);
        findViewById(R.id.layout_beautify).setOnClickListener(this);
        findViewById(R.id.layout_denoise).setOnClickListener(this);
        findViewById(R.id.layout_clear).setOnClickListener(this);
    }

    private void initData() {
        mImgEditView.setListener(this);
        mAlgorithm = new ImgAlgorithm(getAlgorithmListener());
        setSeekBarDefault();

        mImgEditView.post(new Runnable() {
            @Override
            public void run() {
                View guide = findViewById(R.id.hor_guide_dot_92);
                View root = findViewById(R.id.img_edit_activity_root);
                mAniDistance = root.getBottom() - guide.getBottom();
            }
        });
    }

    private boolean rootAdjustShow() {
        return mRootAdjustBeauty.getVisibility() == View.VISIBLE ||
                mRootAdjustDenoise.getVisibility() == View.VISIBLE;
    }

    private void setSeekBarDefault() {
        mSeekBarContrast.setProgress(0);
        mSeekBarSharpen.setProgress(50);
        mSeekBarSaturation.setProgress(50);
        mSeekBarHorDenoise.setProgress(0);
        mSeekBarVerDenoise.setProgress(0);
        mSeekbarQuantization.setProgress(0);
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
