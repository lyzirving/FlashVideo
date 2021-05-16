package com.lyzirving.flashvideo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.lyzirving.flashvideo.ui.MusicActivity;
import com.lyzirving.flashvideo.ui.VideoActivity;
import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    static {
        System.loadLibrary("native-lib");
    }

    private static final String TAG = "MainActivity";
    private static final int CODE_REQUEST_READ_WRITE_PERMISSION = 0x01;

    private Button mBtnStartMusic, mBtnStartVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        requestUserPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CODE_REQUEST_READ_WRITE_PERMISSION: {
                LogUtil.d(TAG, "onRequestPermissionsResult: " + CODE_REQUEST_READ_WRITE_PERMISSION);
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    LogUtil.d(TAG, "onRequestPermissionsResult: permission granted");
                    enableButtons(true);
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_music: {
                startActivity(MusicActivity.class);
                break;
            }
            case R.id.btn_start_video: {
                startActivity(VideoActivity.class);
                break;
            }
            default: {
                break;
            }
        }
    }

    private void enableButtons(boolean enable) {
        mBtnStartMusic.setEnabled(enable);
        mBtnStartVideo.setEnabled(enable);
    }

    private void initView() {
        mBtnStartMusic = findViewById(R.id.btn_start_music);
        mBtnStartVideo = findViewById(R.id.btn_start_video);

        mBtnStartMusic.setOnClickListener(this);
        mBtnStartVideo.setOnClickListener(this);
    }

    private void requestUserPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    CODE_REQUEST_READ_WRITE_PERMISSION);
        } else {
            LogUtil.d(TAG, "requestUserPermission: permission granted");
            enableButtons(true);
        }
    }

    private void startActivity(Class activityClass) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), activityClass);
        startActivity(intent);
    }
}
