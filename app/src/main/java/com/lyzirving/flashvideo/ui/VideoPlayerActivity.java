package com.lyzirving.flashvideo.ui;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.video.VideoViewListener;
import com.lyzirving.flashvideo.opengl.video.YuvVideoView;

import java.io.File;
import java.lang.ref.SoftReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author lyzirving
 */
public class VideoPlayerActivity extends AppCompatActivity implements View.OnClickListener, VideoViewListener {
    private static final String TAG = "VideoPlayerActivity";
    private static final int MSG_PREPARE = 1;
    private static final int MSG_STOP = 2;
    private static final String LOCAL_VIDEO_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "0test" + File.separator + "source" + File.separator + /*"onepieceads.mp4"*/"Joker.mp4";

    private Button mBtnPlay, mBtnPause, mBtnStop;
    private YuvVideoView mVideoView;

    private VideoPlayerHandler mHandler;

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
        mHandler.sendEmptyMessage(MSG_PREPARE);
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
        mHandler.sendEmptyMessage(MSG_STOP);
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

        mHandler = new VideoPlayerHandler(this, Looper.getMainLooper());
    }

    private void handlePrepare() {
        setBtnEnable(true);
    }

    private void handleStop() {
        setBtnEnable(false);
    }

    private void setBtnEnable(boolean enable) {
        mBtnPlay.setEnabled(enable);
        mBtnPause.setEnabled(enable);
        mBtnStop.setEnabled(enable);
    }

    private static class VideoPlayerHandler extends Handler {
        private SoftReference<VideoPlayerActivity> mRef;

        VideoPlayerHandler(VideoPlayerActivity activity, Looper looper) {
            super(looper);
            mRef = new SoftReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_PREPARE: {
                    mRef.get().handlePrepare();
                    break;
                }
                case MSG_STOP: {
                    mRef.get().handleStop();
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }
}
