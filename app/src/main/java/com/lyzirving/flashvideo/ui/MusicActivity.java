package com.lyzirving.flashvideo.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.core.FlashVideo;
import com.lyzirving.flashvideo.core.VideoListenerAdapter;
import com.lyzirving.flashvideo.util.LogUtil;
import com.lyzirving.flashvideo.util.TimeUtil;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author lyzirving
 */
public class MusicActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "MusicActivity";
    private static final int MSG_PREPARE = 1;
    private static final int MSG_TIME_TICK = 2;
    private static final int MSG_STOP = 3;
    private static final String LOCAL_SRC_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "0test" + File.separator + "source" + File.separator + "SpaceBound.mp3";
    private FlashVideo mFlashVideo;

    private VideoListenerAdapter mListener;
    private TextView mTvCurrentTime, mTvTotalTime;
    private boolean mProgressBarDragging;
    private SeekBar mProgressBar, mVolumeBar;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_PREPARE: {
                    mFlashVideo.setTotalTime((Double) msg.obj);
                    mTvTotalTime.setText(TimeUtil.transferDoubleTimeToHourMinuteSecond((Double) msg.obj));
                    break;
                }
                case MSG_TIME_TICK: {
                    mTvCurrentTime.setText(TimeUtil.transferDoubleTimeToHourMinuteSecond((Double) msg.obj));
                    if (!mProgressBarDragging) {
                        mProgressBar.setProgress((int)((Double) msg.obj / mFlashVideo.getTotalTime() * 100));
                    }
                    break;
                }
                case MSG_STOP: {
                    mFlashVideo = null;
                    mTvCurrentTime.setText(getString(R.string.msg_default_time));
                    mTvTotalTime.setText(getString(R.string.msg_default_time));
                    mProgressBar.setProgress(0);
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
        setContentView(R.layout.layout_music);
        initView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_init: {
                if (mFlashVideo == null) {
                    mFlashVideo = new FlashVideo();
                    mFlashVideo.setVideoListener(getVideoListener());
                    mFlashVideo.setSourcePath(LOCAL_SRC_PATH);
                    boolean success = mFlashVideo.init();
                    Toast.makeText(this,
                            success ? getString(R.string.tip_success) : getString(R.string.tip_fail),
                            Toast.LENGTH_SHORT).show();
                }
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

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()) {
            case R.id.progress_bar: {
                mProgressBarDragging = true;
                break;
            }
            case R.id.volume_bar:
            default: {
                break;
            }
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()) {
            case R.id.progress_bar: {
                mProgressBarDragging = false;
                if (mFlashVideo != null) {
                    mFlashVideo.seek(seekBar.getProgress() * 1f / 100);
                }
                break;
            }
            case R.id.volume_bar: {
                if (mFlashVideo != null) {
                    mFlashVideo.setVolume(seekBar.getProgress());
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    private VideoListenerAdapter getVideoListener() {
        if (mListener == null) {
            mListener = new VideoListenerAdapter() {
                @Override
                public void onPrepare(double duration) {
                    super.onPrepare(duration);
                    mHandler.obtainMessage(MSG_PREPARE, duration).sendToTarget();
                }

                @Override
                public void onStop() {
                    super.onStop();
                    mHandler.sendEmptyMessage(MSG_STOP);
                }

                @Override
                public void onTickTime(double currentTime) {
                    super.onTickTime(currentTime);
                    mHandler.obtainMessage(MSG_TIME_TICK, currentTime).sendToTarget();
                }
            };
        }
        return mListener;
    }

    private void initView() {
        mTvCurrentTime = findViewById(R.id.tv_current_time);
        mTvTotalTime = findViewById(R.id.tv_total_time);
        mProgressBar = findViewById(R.id.progress_bar);
        mVolumeBar = findViewById(R.id.volume_bar);
        findViewById(R.id.btn_init).setOnClickListener(this);
        findViewById(R.id.btn_play).setOnClickListener(this);
        findViewById(R.id.btn_pause).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
        mProgressBar.setOnSeekBarChangeListener(this);
        mVolumeBar.setOnSeekBarChangeListener(this);
        mProgressBar.setProgress(0);
        mVolumeBar.setProgress(0);
    }
}
