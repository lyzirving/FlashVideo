package com.lyzirving.flashvideo.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.text.TextUtils;

import com.lyzirving.flashvideo.imgedit.filter.ImgGaussianFilter;
import com.lyzirving.flashvideo.opengl.util.TextureUtil;
import com.lyzirving.flashvideo.util.LogUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author lyzirving
 */
public class BaseFilterGroup extends BaseFilter {
    private static final String TAG = "BaseFilterGroup";

    protected int[] mGroupFrameBuffers, mGroupTextures;
    protected LinkedHashMap<String, BaseFilter> mFilters;

    public BaseFilterGroup(Context ctx) {
        super(ctx);
        //iterator by assertion order
        mFilters = new LinkedHashMap<>();
        mGroupFrameBuffers = new int[0];
        mGroupTextures = new int[0];
    }

    /**
     * the initFilterGroup should be called later
     * @param filter specific filter
     */
    public boolean addFilterThenInit(BaseFilter filter) {
        if (mFilters.containsKey(filter.getClass().getSimpleName())) {
            LogUtil.i(TAG, "addFilterThenInit: already contains " + filter.getClass().getSimpleName());
            return false;
        }
        LogUtil.i(TAG, "addFilterThenInit: " + filter.getClass().getSimpleName());
        mFilters.put(filter.getClass().getSimpleName(), filter);
        filter.setOutputSize(mOutputWidth, mOutputHeight);
        return true;
    }

    public boolean addFilter(BaseFilter filter, boolean forceRender) {
        if (mFilters.containsKey(filter.getClass().getSimpleName())) {
            LogUtil.i(TAG, "addFilter: already contains " + filter.getClass().getSimpleName());
            return false;
        }
        LogUtil.i(TAG, "addFilter: " + filter.getClass().getSimpleName());
        mFilters.put(filter.getClass().getSimpleName(), filter);
        filter.setOutputSize(mOutputWidth, mOutputHeight);
        if (forceRender) {
            addPreDrawTask(new Runnable() {
                @Override
                public void run() { initFilterGroup(); }
            });
        }
        return true;
    }

    @Override
    public int draw(int textureId) {
        Iterator<String> iterator = mFilters.keySet().iterator();
        BaseFilter filter;
        String tag;
        int index = 0;
        int inputTexture = textureId;
        while (iterator.hasNext()) {
            tag = iterator.next();
            filter = mFilters.get(tag);
            if (filter == null) { throw new RuntimeException("draw: key = " + tag + ", filter is null"); }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGroupFrameBuffers[index]);
            if (filter instanceof ImgGaussianFilter) {
                ((ImgGaussianFilter) filter).draw(inputTexture, mGroupFrameBuffers[index]);
            } else {
                filter.draw(inputTexture);
            }
            inputTexture = mGroupTextures[index];
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            index++;
        }
        return inputTexture;
    }

    public BaseFilter getFilter(String filterName) {
        if (TextUtils.isEmpty(filterName)) {
            LogUtil.i(TAG, "getFilter: input is empty");
            return null;
        }
        BaseFilter filter = mFilters.get(filterName);
        LogUtil.i(TAG, "getFilter: name = " + filterName + ", result = " + (filter != null));
        return filter;
    }

    public int getFilterCount() {
        if (mFilters == null) {
            mFilters = new LinkedHashMap<>();
        }
        return mFilters.size();
    }

    final public void initFilterGroup() {
        Iterator<String> iterator = mFilters.keySet().iterator();
        BaseFilter filter;
        String tag;
        while (iterator.hasNext()) {
            tag = iterator.next();
            filter = mFilters.get(tag);
            if (filter == null) { throw new RuntimeException("initFilterGroup: get null filter by key " + tag); }
            if (filter instanceof BaseFilterGroup) {
                ((BaseFilterGroup) filter).initFilterGroup();
            } else {
                filter.init();
            }
        }
        generateFrameBufferAndTexture();
    }

    @Override
    public void release() {
        super.release();
        if (mFilters.size() == 0){
            LogUtil.i(TAG, "release: no filters");
            return;
        }
        final List<BaseFilter> tmpFilters = new ArrayList<>();
        Iterator<String> iterator = mFilters.keySet().iterator();
        BaseFilter filter;
        while (iterator.hasNext()) {
            filter = mFilters.get(iterator.next());
            if (filter == null) { continue; }
            tmpFilters.add(filter);
        }
        mFilters.clear();
        for (BaseFilter destroyingFilter : tmpFilters) { destroyingFilter.release();}
        tmpFilters.clear();
        int nowFilterSize = mFilters.size();
        int nowFrameBufferSize = mGroupFrameBuffers.length;
        LogUtil.i(TAG, "release: pre draw, now filter size = " + nowFilterSize + ", now frame buffer size = " + nowFrameBufferSize);
        destroyFrameBufferAndTexture(nowFrameBufferSize, nowFilterSize);
    }

    public void removeAllFilter() {
        if (mFilters.size() == 0){
            LogUtil.i(TAG, "removeAllFilter: no filters");
            return;
        }
        final List<BaseFilter> tmpFilters = new ArrayList<>();
        Iterator<String> iterator = mFilters.keySet().iterator();
        BaseFilter filter;
        while (iterator.hasNext()) {
            filter = mFilters.get(iterator.next());
            if (filter == null) { continue; }
            tmpFilters.add(filter);
        }
        mFilters.clear();
        addPreDrawTaskHead(new Runnable() {
            @Override
            public void run() {
                for (BaseFilter destroyingFilter : tmpFilters) { destroyingFilter.release();}
                tmpFilters.clear();
                int nowFilterSize = mFilters.size();
                int nowFrameBufferSize = mGroupFrameBuffers.length;
                LogUtil.i(TAG, "removeAllFilter: pre draw, now filter size = " + nowFilterSize + ", now frame buffer size = " + nowFrameBufferSize);
                destroyFrameBufferAndTexture(nowFrameBufferSize, nowFilterSize);
            }
        });
    }

    public void removeFilter(final String name) {
        if (!mFilters.containsKey(name)) {
            LogUtil.i(TAG, "removeFilter: does not contain filter = " + name);
            return;
        }
        LogUtil.i(TAG, "removeFilter: " + name);
        final BaseFilter filter = mFilters.remove(name);
        addPreDrawTaskHead(new Runnable() {
            @Override
            public void run() {
                if (filter != null) {filter.release();}
                int nowFilterSize = mFilters.size();
                int nowFrameBufferSize = mGroupFrameBuffers.length;
                LogUtil.i(TAG, "removeFilter: pre draw, now filter size = " + nowFilterSize + ", now frame buffer size = " + nowFrameBufferSize);
                destroyFrameBufferAndTexture(nowFrameBufferSize, nowFilterSize);
            }
        });
    }

    @Override
    public void setOutputSize(int width, int height) {
        super.setOutputSize(width, height);
        Iterator<String> iterator = mFilters.keySet().iterator();
        BaseFilter filter;
        while (iterator.hasNext()) {
            filter = mFilters.get(iterator.next());
            if (filter == null) { throw new RuntimeException("setOutputSize: filter is null"); }
            filter.setOutputSize(width, height);
        }
    }

    private void destroyFrameBufferAndTexture(int nowFrameBufferSize, int nowFilterSize) {
        if (nowFrameBufferSize < nowFilterSize) {
            LogUtil.i(TAG, "destroyFrameBufferAndTexture: pre draw, need more frame buffer");
            generateFrameBufferAndTexture();
        } else if (nowFrameBufferSize > nowFilterSize) {
            LogUtil.i(TAG, "destroyFrameBufferAndTexture: pre draw, remove more framebuffer");
            TextureUtil.get().deleteFrameBufferAndTextureFromOffset(nowFilterSize, mGroupFrameBuffers, mGroupTextures);
            int[] tmpFrameBuffer = new int[nowFilterSize];
            int[] tmpTextures = new int[nowFilterSize];
            System.arraycopy(mGroupFrameBuffers, 0, tmpFrameBuffer, 0, nowFilterSize);
            System.arraycopy(mGroupTextures, 0, tmpTextures, 0, nowFilterSize);
            mGroupFrameBuffers = tmpFrameBuffer;
            mGroupTextures = tmpTextures;
        }
    }

    private void generateFrameBufferAndTexture() {
        if (mGroupFrameBuffers.length == 0) {
            LogUtil.i(TAG, "generateFrameBufferAndTexture: first time build frame buffers and textures");
            int size = mFilters.size();
            mGroupFrameBuffers = new int[size];
            mGroupTextures = new int[size];
            TextureUtil.get().generateFrameBufferAndTexture(size, mGroupFrameBuffers, mGroupTextures, mOutputWidth, mOutputHeight);
        } else {
            if (mGroupFrameBuffers.length == mFilters.size()) {
                LogUtil.i(TAG, "generateFrameBufferAndTexture: size is the same, do nothing");
            } else if (mGroupFrameBuffers.length < mFilters.size()) {
                int nowSize = mGroupFrameBuffers.length;
                int dstSize = mFilters.size();
                LogUtil.i(TAG, "generateFrameBufferAndTexture: need to create more frame buffer, now = " + nowSize + ", target = " + dstSize);
                int[] tmpFrameBuffer = new int[dstSize];
                int[] tmpTextures = new int[dstSize];
                System.arraycopy(mGroupFrameBuffers, 0, tmpFrameBuffer, 0, nowSize);
                System.arraycopy(mGroupTextures, 0, tmpTextures, 0, nowSize);
                int offset = mGroupFrameBuffers.length;
                for (int i = offset; i < dstSize; i++) {
                    tmpFrameBuffer[i] = INVALID_ID;
                    tmpTextures[i] = INVALID_ID;
                }
                mGroupFrameBuffers = tmpFrameBuffer;
                mGroupTextures = tmpTextures;
                TextureUtil.get().generateFrameBufferAndTextureOffset(offset, mGroupFrameBuffers, mGroupTextures, mOutputWidth, mOutputHeight);
            } else {
                throw new RuntimeException("generateFrameBufferAndTexture: frame buffer is larger than filter size");
            }
        }
    }
}
