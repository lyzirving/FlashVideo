package com.lyzirving.flashvideo.ui;

import android.hardware.camera2.CameraMetadata;
import android.os.Bundle;
import android.view.View;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.camera.GLCameraView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

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
                break;
            }
            case R.id.btn_back: {
                mCameraView.switchFace(CameraMetadata.LENS_FACING_BACK);
                break;
            }
            case R.id.btn_capture: {
                mCameraView.takePhoto();
                break;
            }
            default: {
                break;
            }
        }
    }

    private void initView() {
        mCameraView = findViewById(R.id.view_camera_preview);

        findViewById(R.id.btn_front).setOnClickListener(this);
        findViewById(R.id.btn_back).setOnClickListener(this);
        findViewById(R.id.btn_capture).setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.closeCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
