package com.lyzirving.flashvideo.edit;

import com.lyzirving.flashvideo.R;

/**
 * @author lyzirving
 */
public class MediaEditOp {
    public float leftAnchor, rightAnchor;
    public String pcmTmpDir, wavTmpDir;
    public int picResId;

    public MediaInfo info;

    public MediaEditOp() {
        leftAnchor = 0;
        rightAnchor = 1;
        picResId = R.drawable.one_piece_bg;
    }
}
