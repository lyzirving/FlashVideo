package com.lyzirving.flashvideo.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lyzirving.flashvideo.R;
import com.lyzirving.flashvideo.edit.MediaInfo;
import com.lyzirving.flashvideo.edit.MusicEditOp;
import com.lyzirving.flashvideo.edit.ProgressSelectBar;
import com.lyzirving.flashvideo.edit.core.MediaEditor;
import com.lyzirving.flashvideo.util.AssetsManager;
import com.lyzirving.flashvideo.util.LogUtil;
import com.lyzirving.flashvideo.util.TimeUtil;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author lyzirving
 */
public class EditActivity extends AppCompatActivity implements View.OnClickListener, AssetsManager.AssetsListener,
        ProgressSelectBar.ProgressSelectBarListener {
    private static final String TAG = "EditActivity";
    public static final int MSG_MEDIA_DETECT = 1;

    private TextView mTvMediaInfo, mTvCurrentSelect, mTvLeftAnchor, mTvRightAnchor;
    private ImageView mIvSrcImg;
    private ProgressSelectBar mProgressSelector;
    private EditHandler mHandler;

    private List<MediaInfo> mMediaInfoList;
    private MusicEditOp mMusicEditOp;

    private MediaEditor mEditor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        initView();
        initData();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_prepare_edit: {
                if (mEditor == null) {
                    mEditor = new MediaEditor();
                }
                mEditor.prepare();
                break;
            }
            case R.id.btn_start_edit: {
                if (mEditor != null) {
                    mEditor.startRecord();
                }
                break;
            }
            case R.id.btn_stop_edit: {
                if (mEditor != null) {
                    mEditor.quit();
                    mEditor = null;
                }
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
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mProgressSelector != null) {
            mProgressSelector.setListener(null);
            mProgressSelector = null;
        }
        AssetsManager.get().destroy();
    }

    @Override
    public void onLeftAnchorUp(float leftRatio) {
        if (mMusicEditOp == null) {
            LogUtil.d(TAG, "onLeftAnchorUp: no select op");
            return;
        }
        LogUtil.d(TAG, "onLeftAnchorUp: " + leftRatio);
        mMusicEditOp.left = leftRatio;
        mTvLeftAnchor.setText(TimeUtil.transferDoubleTimeToHourMinuteSecond(mMusicEditOp.info.duration * leftRatio));
    }

    @Override
    public void onRightAnchorUp(float rightRatio) {
        if (mMusicEditOp == null) {
            LogUtil.d(TAG, "onRightAnchorUp: no select op");
            return;
        }
        LogUtil.d(TAG, "onRightAnchorUp: " + rightRatio);
        mMusicEditOp.right = rightRatio;
        mTvRightAnchor.setText(TimeUtil.transferDoubleTimeToHourMinuteSecond(mMusicEditOp.info.duration * rightRatio));
    }

    @Override
    public void onMediaDetected(List<MediaInfo> list) {
        if (list != null && list.size() > 0) {
            mHandler.obtainMessage(MSG_MEDIA_DETECT, list).sendToTarget();
        }
    }

    private void handleMediaDetect(List<MediaInfo> infos) {
        if (infos != null && infos.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < infos.size(); i++) {
                sb.append(infos.get(i).name)
                        .append(", time = ")
                        .append(TimeUtil.transferDoubleTimeToHourMinuteSecond(infos.get(i).duration));
                if (i != infos.size() - 1) {
                    sb.append("\n");
                }
            }
            mTvMediaInfo.setText(sb.toString());
            mMediaInfoList.clear();
            mMediaInfoList.addAll(infos);

            if (mMediaInfoList.size() == 1) {
                MediaInfo info = mMediaInfoList.get(0);
                mTvCurrentSelect.setText(String.format(getString(R.string.tip_select_media), info.name));
                mTvLeftAnchor.setText(TimeUtil.transferDoubleTimeToHourMinuteSecond(0));
                mTvRightAnchor.setText(TimeUtil.transferDoubleTimeToHourMinuteSecond(info.duration));
                mMusicEditOp = new MusicEditOp();
                mMusicEditOp.info = info;
                mProgressSelector.active(true);
            } else {
                mTvCurrentSelect.setText(getString(R.string.tip_multiple_media));
                mProgressSelector.active(false);
                mMusicEditOp = null;
            }
        }
    }

    private void initView() {
        mTvMediaInfo = findViewById(R.id.tv_media_metadata);
        mTvCurrentSelect = findViewById(R.id.tv_select_media);
        mTvLeftAnchor = findViewById(R.id.tv_left_anchor);
        mTvRightAnchor = findViewById(R.id.tv_right_anchor);
        mIvSrcImg = findViewById(R.id.iv_src_img);
        mProgressSelector = findViewById(R.id.progress_selector);
        findViewById(R.id.btn_prepare_edit).setOnClickListener(this);
        findViewById(R.id.btn_start_edit).setOnClickListener(this);
        findViewById(R.id.btn_stop_edit).setOnClickListener(this);

        mHandler = new EditHandler(this);
        mProgressSelector.setListener(this);
    }

    private void initData() {
        mIvSrcImg.setImageResource(R.drawable.one_piece_bg);
        mMediaInfoList = new ArrayList<>();
        AssetsManager.get().setAssetsListener(this);
        AssetsManager.get().copyAssets(AssetsManager.AssetsType.MUSIC);
    }

    private static class EditHandler extends Handler {
        private SoftReference<EditActivity> mRef;

        EditHandler(EditActivity activity) {
            super(Looper.getMainLooper());
            mRef = new SoftReference<>(activity);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            EditActivity activity = null;
            if ((activity = mRef.get()) == null) {
                LogUtil.d(TAG, "handleMessage: activity is null");
                return;
            }
            switch (msg.what) {
                case MSG_MEDIA_DETECT: {
                    activity.handleMediaDetect((List<MediaInfo>) msg.obj);
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }
}
