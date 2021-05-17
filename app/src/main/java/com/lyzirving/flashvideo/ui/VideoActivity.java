package com.lyzirving.flashvideo.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.opengl.GLVideoView;
import com.lyzirving.flashvideo.util.TimeUtil;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

// TODO: 2021/5/16 (1) function of speeding the video
// TODO: 2021/5/16 (2) use JNI to play the media in assets file
/**
 * @author lyzirving
 */
public class VideoActivity extends AppCompatActivity implements View.OnClickListener, GLVideoView.GLVideoViewListener,
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "VideoActivity";
    private static final String LOCAL_VIDEO_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "0test" + File.separator + "source" + File.separator + "onepieceads.mp4";
    private static final int MSG_PREPARE = 0x00;
    private static final int MSG_TICK_TIME = 0x01;
    private static final int MSG_STOP = 0x02;

    private GLVideoView mVideoView;
    private TextView mTvCurrentTime, mTvTotalTime;
    private SeekBar mVideoProgressBar;
    private Button mBtnPlay, mBtnPause, mBtnStop;
    private double mTotalTime;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_PREPARE: {
                    mTvTotalTime.setText(TimeUtil.transferDoubleTimeToHourMinuteSecond(mTotalTime));
                    enableBtn(true);
                    break;
                }
                case MSG_TICK_TIME: {
                    double curTime = (Double) msg.obj;
                    mTvCurrentTime.setText(TimeUtil.transferDoubleTimeToHourMinuteSecond(curTime));
                    mVideoProgressBar.setProgress((int) ((curTime / mTotalTime) * 100));
                    break;
                }
                case MSG_STOP: {
                    enableBtn(false);
                    mTvCurrentTime.setText(getText(R.string.msg_default_time));
                    mTvTotalTime.setText(getText(R.string.msg_default_time));
                    mVideoProgressBar.setProgress(0);
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

    @Override
    public void onPrepare(double duration) {
        mTotalTime = duration;
        mHandler.sendEmptyMessage(MSG_PREPARE);
    }

    @Override
    public void onVideoPlay() {}

    @Override
    public void onVideoPause() {}

    @Override
    public void onVideoTickTime(double currentTime) {
        mHandler.obtainMessage(MSG_TICK_TIME, currentTime).sendToTarget();
    }

    @Override
    public void onVideoStop() {
        mHandler.sendEmptyMessage(MSG_STOP);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mVideoView.seek(seekBar.getProgress() * 1f / 100);
    }

    private void enableBtn(boolean enable) {
        mBtnPlay.setEnabled(enable);
        mBtnPause.setEnabled(enable);
        mBtnStop.setEnabled(enable);
    }

    private void initView() {
        mVideoView = findViewById(R.id.view_video);
        mVideoView.setVideoViewListener(this);
        mTvCurrentTime = findViewById(R.id.tv_current_time);
        mTvTotalTime = findViewById(R.id.tv_total_time);
        mVideoProgressBar = findViewById(R.id.video_progress_bar);
        mBtnPlay = findViewById(R.id.btn_play);
        mBtnPause = findViewById(R.id.btn_pause);
        mBtnStop = findViewById(R.id.btn_stop);

        mVideoProgressBar.setOnSeekBarChangeListener(this);
        mBtnPlay.setOnClickListener(this);
        mBtnPause.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);
        findViewById(R.id.btn_init).setOnClickListener(this);
    }
}
