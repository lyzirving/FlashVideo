package com.lyzirving.flashvideo.ui;

import android.hardware.camera2.CameraMetadata;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.camera.CameraRender;
import com.lyzirving.flashvideo.camera.GLCameraView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener {

    private CheckBox mCheckContrast, mCheckSaturation, mCheckBlur;
    private RadioGroup mRadioGroup;
    private SeekBar mSeekBar;
    private GLCameraView mCameraView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_front: {
                mCameraView.switchFace(CameraMetadata.LENS_FACING_FRONT);
                mCheckContrast.setChecked(false);
                mCheckSaturation.setChecked(false);
                mCheckBlur.setChecked(false);
                mRadioGroup.check(-1);
                mSeekBar.setProgress(0);
                break;
            }
            case R.id.btn_back: {
                mCameraView.switchFace(CameraMetadata.LENS_FACING_BACK);
                mCheckContrast.setChecked(false);
                mCheckSaturation.setChecked(false);
                mCheckBlur.setChecked(false);
                mRadioGroup.check(-1);
                mSeekBar.setProgress(0);
                break;
            }
            case R.id.btn_capture: {
                mCameraView.takePhoto();
                break;
            }
            case R.id.btn_start_record: {
                mCameraView.switchRecord(true);
                break;
            }
            case R.id.btn_stop_record: {
                mCameraView.switchRecord(false);
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.check_contrast: {
                if (isChecked) {
                    mCameraView.addFilter(CameraRender.FILTER_CONTRAST);
                } else {
                    mCameraView.dequeueFilter(CameraRender.FILTER_CONTRAST);
                }
                break;
            }
            case R.id.check_saturation: {
                if (isChecked) {
                    mCameraView.addFilter(CameraRender.FILTER_SATURATION);
                } else {
                    mCameraView.dequeueFilter(CameraRender.FILTER_SATURATION);
                }
                break;
            }
            case R.id.check_blur: {
                if (isChecked) {
                    mCameraView.addFilter(CameraRender.FILTER_BLUR);
                } else {
                    mCameraView.dequeueFilter(CameraRender.FILTER_BLUR);
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        switch (mRadioGroup.getCheckedRadioButtonId()) {
            case R.id.btn_contrast: {
                if (mCheckContrast.isChecked()) {
                    mCameraView.adjust(CameraRender.FILTER_CONTRAST, seekBar.getProgress());
                }
                break;
            }
            case R.id.btn_saturation: {
                if (mCheckSaturation.isChecked()) {
                    mCameraView.adjust(CameraRender.FILTER_SATURATION, seekBar.getProgress());
                }
                break;
            }
            case R.id.btn_blur: {
                if (mCheckBlur.isChecked()) {
                    mCameraView.adjust(CameraRender.FILTER_BLUR, seekBar.getProgress());
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.switchRecord(false);
        mCameraView.closeCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initView() {
        mCameraView = findViewById(R.id.view_camera_preview);
        mCheckContrast = findViewById(R.id.check_contrast);
        mCheckSaturation = findViewById(R.id.check_saturation);
        mCheckBlur = findViewById(R.id.check_blur);
        mRadioGroup = findViewById(R.id.radio_group);
        mSeekBar = findViewById(R.id.seek_bar);

        findViewById(R.id.btn_front).setOnClickListener(this);
        findViewById(R.id.btn_back).setOnClickListener(this);
        findViewById(R.id.btn_capture).setOnClickListener(this);
        findViewById(R.id.btn_start_record).setOnClickListener(this);
        findViewById(R.id.btn_stop_record).setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
        mCheckContrast.setOnCheckedChangeListener(this);
        mCheckSaturation.setOnCheckedChangeListener(this);
        mCheckBlur.setOnCheckedChangeListener(this);
    }
}
