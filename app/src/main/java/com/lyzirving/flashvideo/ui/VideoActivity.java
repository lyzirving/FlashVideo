package com.lyzirving.flashvideo.ui;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.GLVideoView;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author lyzirving
 */
public class VideoActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "VideoActivity";
    private static final String LOCAL_VIDEO_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "0test" + File.separator + "source" + File.separator + "onepieceads.mp4";

    private GLVideoView mVideoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_video_player);
        initView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_init: {
                mVideoView.setDataSource(LOCAL_VIDEO_PATH);
                mVideoView.prepare();
                break;
            }
            case R.id.btn_play: {
                mVideoView.play();
                break;
            }
            case R.id.btn_pause: {
                mVideoView.pause();
                break;
            }
            case R.id.btn_stop: {
                mVideoView.stop();
                break;
            }
            default: {
                break;
            }
        }
    }

    private void initView() {
        mVideoView = findViewById(R.id.view_video);
        findViewById(R.id.btn_init).setOnClickListener(this);
        findViewById(R.id.btn_play).setOnClickListener(this);
        findViewById(R.id.btn_pause).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
    }
}
