package com.lyzirving.flashvideo.imgedit.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.text.TextUtils;

import com.lyzirving.flashvideo.opengl.filter.BaseFilter;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.util.ComponentUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * @author lyzirving
 */
public class ImgFilterGroup extends BaseFilter {
    private static final String TAG = "ImgFilterGroup";
    private static final int INVALID = -1;

    private int[] mFrameBuffers, mTextures;
    private HashMap<String, Integer> mFilterIndex;
    private LinkedHashMap<String, BaseFilter> mFilters;

    public ImgFilterGroup(Context ctx) {
        super(ctx);
        //iterator by assertion order
        mFilters = new LinkedHashMap<>();
        mFilterIndex = new HashMap<>();
    }

    @Override
    public int draw(int textureId) {
        Iterator<String> iterator = mFilters.keySet().iterator();
        BaseFilter filter;
        String tag;
        Integer index;
        int inputTexture = textureId;
        while (iterator.hasNext()) {
            tag = iterator.next();
            filter = mFilters.get(tag);
            index = mFilterIndex.get(tag);
            if (filter == null) {
                LogUtil.i(TAG, "draw: tag " + tag + " has null filter");
                iterator.remove();
                continue;
            }
            if (index == null) {
                LogUtil.i(TAG, "draw: tag " + tag + " index is null");
                mFilterIndex.remove(index);
                iterator.remove();
                continue;
            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[index]);
            filter.draw(inputTexture);
            inputTexture = mTextures[index];
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
        return inputTexture;
    }

    @Override
    public void release() {
        super.release();
        LogUtil.i(TAG, "release");
        Iterator<String> iterator = mFilters.keySet().iterator();
        BaseFilter filter;
        String tag;
        while (iterator.hasNext()) {
            tag = iterator.next();
            filter = mFilters.get(tag);
            if (filter == null) {
                continue;
            }
            filter.release();
        }
        mFilterIndex.clear();
        mFilters.clear();
    }

    public boolean adjust(String tag, int progress) {
        if (mFilters == null || mFilters.size() == 0) {
            LogUtil.i(TAG, "adjust: tag " + tag + ", no filter");
            return false;
        }
        if (mFilters.containsKey(tag)) {
            BaseFilter tmp = mFilters.get(tag);
            if (tmp != null) {
                tmp.adjustProgress(progress);
                return true;
            } else {
                LogUtil.i(TAG, "adjust: " + tag + " is null");
                return false;
            }
        } else {
            LogUtil.i(TAG, "adjust: no filter " + tag + " in collection");
            return false;
        }
    }

    public boolean addFilter(String tag) {
        if (TextUtils.isEmpty(tag)) {
            LogUtil.i(TAG, "addFilter: tag is empty");
            return false;
        }
        if (mFilters.containsKey(tag)) {
            LogUtil.i(TAG, "addFilter: already contains tag = " + tag);
            return false;
        } else {
            addFilterInner(tag);
            return true;
        }
    }

    public int getFilterSize() {
        if (mFilters == null) {
            return 0;
        } else {
            return mFilters.size();
        }
    }

    private void addFilterInner(final String tag) {
        BaseFilter newFilter = null;
        if (TextUtils.equals(ImgContrastFilter.class.getSimpleName(), tag)) {
            LogUtil.i(TAG, "addFilterInner: " + ImgContrastFilter.class.getSimpleName());
            newFilter = new ImgContrastFilter(ComponentUtil.get().ctx());
        } else if (TextUtils.equals(ImgSharpenFilter.class.getSimpleName(), tag)) {
            LogUtil.i(TAG, "addFilterInner: " + ImgSharpenFilter.class.getSimpleName());
            newFilter = new ImgSharpenFilter(ComponentUtil.get().ctx());
        } else if (TextUtils.equals(ImgSaturationFilter.class.getSimpleName(), tag)) {
            LogUtil.i(TAG, "addFilterInner: " + ImgSaturationFilter.class.getSimpleName());
            newFilter = new ImgSaturationFilter(ComponentUtil.get().ctx());
        }
        if (newFilter != null) {
            mFilters.put(tag, newFilter);
            mFilterIndex.put(tag, mFilters.size() - 1);
            newFilter.setOutputSize(mOutputWidth, mOutputHeight);
            addPreDrawTask(new InitTask(mFilters.size() - 1, newFilter));
        }
    }

    private class InitTask implements Runnable {
        int index;
        BaseFilter newFilter;
        InitTask(int ind, BaseFilter filter) {
            index = ind;
            newFilter = filter;
        }

        @Override
        public void run() {
            if (mFrameBuffers == null || mFrameBuffers.length == 0) {
                LogUtil.i(TAG, "InitTask: init frame buffers and textures");
                mFrameBuffers = new int[1];
                mTextures = new int[1];
                TextureUtil.get().generateFrameBufferAndTexture(1, mFrameBuffers, mTextures, mOutputWidth, mOutputHeight);
            } else if (index < mFrameBuffers.length) {
                if (mFrameBuffers[index] == INVALID) {
                    LogUtil.i(TAG, "InitTask: init frame buffers and textures at index = " + index);
                } else {
                    LogUtil.i(TAG, "InitTask: destroy and init frame buffers and textures at index = " + index);
                    TextureUtil.get().deleteFrameBufferAndTextureAtIndex(index, mFrameBuffers, mTextures);
                }
                TextureUtil.get().generateFrameBufferAndTextureAtIndex(index, mFrameBuffers, mTextures, mOutputWidth, mOutputHeight);
            } else if (index >= mFrameBuffers.length) {
                LogUtil.i(TAG, "InitTask: extend frame buffers and textures, index = " + index + ", now size = " + mFrameBuffers.length);
                int newLen = index + 1;
                int[] tmpFrameBuffer = new int[newLen];
                int[] tmpTextures = new int[newLen];
                System.arraycopy(mFrameBuffers, 0, tmpFrameBuffer, 0, mFrameBuffers.length);
                System.arraycopy(mTextures, 0, tmpTextures, 0, mTextures.length);
                for (int i = mFrameBuffers.length; i < newLen; i++) {
                    tmpFrameBuffer[i] = INVALID;
                    tmpTextures[i] = INVALID;
                }
                mFrameBuffers = tmpFrameBuffer;
                mTextures = tmpTextures;
                TextureUtil.get().generateFrameBufferAndTextureAtIndex(index, mFrameBuffers, mTextures, mOutputWidth, mOutputHeight);
            }
            newFilter.init();
        }
    }
}
