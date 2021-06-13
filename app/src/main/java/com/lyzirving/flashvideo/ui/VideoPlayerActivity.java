package com.lyzirving.flashvideo.ui;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.video.VideoViewListener;
import com.lyzirving.flashvideo.opengl.video.YuvVideoView;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author lyzirving
 */
public class VideoPlayerActivity extends AppCompatActivity implements View.OnClickListener, VideoViewListener {
    private static final String TAG = "VideoPlayerActivity";
    private static final String LOCAL_VIDEO_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "0test" + File.separator + "source" + File.separator + "onepieceads.mp4";

    private Button mBtnPlay, mBtnPause, mBtnStop;
    private YuvVideoView mVideoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_video_player_panel);
        initView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_init: {
                mVideoView.prepare(LOCAL_VIDEO_PATH, this);
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

    @Override
    public void onPrepare(double duration) {

    }

    @Override
    public void onVideoPlay() {

    }

    @Override
    public void onVideoPause() {

    }

    @Override
    public void onVideoTickTime(double currentTime) {

    }

    @Override
    public void onVideoStop() {

    }

    private void initView() {
        mVideoView = findViewById(R.id.view_yuv_video);
        mBtnPlay = findViewById(R.id.btn_play);
        mBtnPause = findViewById(R.id.btn_pause);
        mBtnStop = findViewById(R.id.btn_stop);

        mBtnPlay.setOnClickListener(this);
        mBtnPause.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);
        findViewById(R.id.btn_init).setOnClickListener(this);
    }
}
