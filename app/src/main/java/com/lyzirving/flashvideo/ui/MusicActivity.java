package com.lyzirving.flashvideo.ui;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.core.FlashVideo;
import com.lyzirving.flashvideo.util.LogUtil;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author lyzirving
 */
public class MusicActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MusicActivity";
    private static final String LOCAL_SRC_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "0test" + File.separator + "source" + File.separator + "SpaceBound.mp3";
    private FlashVideo mFlashVideo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_music);
        initView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_init: {
                mFlashVideo = new FlashVideo();
                mFlashVideo.setSourcePath(LOCAL_SRC_PATH);
                boolean success = mFlashVideo.init();
                Toast.makeText(this,
                        success ? getString(R.string.tip_success) : getString(R.string.tip_fail),
                        Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.btn_play: {
                if (mFlashVideo == null) {
                    LogUtil.e(TAG, "onClick: play, video obj is null");
                } else {
                    mFlashVideo.play();
                }
                break;
            }
            case R.id.btn_pause: {
                if (mFlashVideo == null) {
                    LogUtil.e(TAG, "onClick: pause, video obj is null");
                } else {
                    mFlashVideo.pause();
                }
                break;
            }
            case R.id.btn_stop: {
                if (mFlashVideo == null) {
                    LogUtil.e(TAG, "onClick: stop, video obj is null");
                } else {
                    mFlashVideo.stop();
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    private void initView() {
        findViewById(R.id.btn_init).setOnClickListener(this);
        findViewById(R.id.btn_play).setOnClickListener(this);
        findViewById(R.id.btn_pause).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
    }
}
