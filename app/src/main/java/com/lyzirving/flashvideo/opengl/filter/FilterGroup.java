package com.lyzirving.flashvideo.opengl.filter;

import android.text.TextUtils;

import com.lyzirving.flashvideo.util.LogUtil;

/**
 * @author lyzirving
 */
public class FilterGroup implements IFilter {
    private static final String TAG = "FilterGroup";
    private static final FilterNode NODE_NULL = null;

    private int mCount;
    private FilterNode mHead;

    public FilterGroup() {
        mHead = NODE_NULL;
    }

    public void adjustContrast(float val) {
        FilterNode tmp = mHead;
        ContrastFilter target = null;
        while(tmp != null) {
            if (contains(tmp, ContrastFilter.class)) {
                target = (ContrastFilter) tmp.filter;
                break;
            }
            tmp = tmp.next;
        }
        if (target != null) {
            target.adjust(val);
        }
    }

    public void add(BaseFilter input) {
        if (mHead == NODE_NULL) {
            LogUtil.d(TAG, "add: first");
            mHead = new FilterNode(input, input.getClass());
            mCount++;
        } else {
            Class key = input.getClass();
            FilterNode tmp = mHead;
            boolean hasFilter = contains(tmp, key);
            while (!hasFilter && tmp.next != null) {
                tmp = tmp.next;
                hasFilter = contains(tmp, key);
            }
            if (!hasFilter) {
                FilterNode newObj = new FilterNode(input, input.getClass());
                tmp.next = newObj;
                newObj.prev = tmp;
                mCount++;
                LogUtil.d(TAG, "add: " + mCount);
            }
        }
    }

    @Override
    public int draw(int inputTextureId) {
        FilterNode tmp = mHead;
        int lastTextureId = inputTextureId;
        if (tmp != NODE_NULL) {
            do {
                lastTextureId = tmp.filter.draw(lastTextureId);
                tmp = tmp.next;
            } while (tmp != NODE_NULL);
        }
        return lastTextureId;
    }

    public BaseFilter dequeue(Class key) {
        if (mHead == null) {
            return null;
        }
        FilterNode tmp = mHead;
        do {
            if (contains(tmp, key)) {
                mCount--;
                if (tmp == mHead) {
                    mHead = tmp.next;
                    if (mHead != null) {
                        mHead.prev = null;
                    }
                    return tmp.filter;
                } else {
                    tmp.prev.next = tmp.next;
                    tmp.next.prev = tmp.prev;
                    return tmp.filter;
                }
            } else {
                tmp = tmp.next;
            }
        } while (tmp != null);
        return null;
    }

    @Override
    public void init() {
        FilterNode tmp = mHead;
        if (tmp != NODE_NULL) {
            do {
                tmp.filter.init();
                tmp = tmp.next;
            } while (tmp != NODE_NULL);
        }
    }

    public BaseFilter peek() {
        if (mHead == null) {
            return null;
        } else {
            FilterNode first = mHead;
            mHead = first.next;
            if (mHead != null) {
                mHead.prev = null;
            }
            mCount--;
            return first.filter;
        }
    }

    @Override
    public void release() {
        FilterNode tmp = mHead;
        if (tmp != NODE_NULL) {
            do {
                tmp.filter.release();
                tmp.filter = null;
                tmp = tmp.next;
            } while (tmp != NODE_NULL);
        }
        mHead = null;
    }

    @Override
    public void setVertexCoordinates(float[] vertex) {
        throw new RuntimeException("FilterGroup should not call setVertexCoordinates");
    }

    @Override
    public void setTextureCoordinates(float[] textureCoordinates) {
        throw new RuntimeException("FilterGroup should not call setVertexCoordinates");
    }

    @Override
    public void setOutputSize(int width, int height) {
        FilterNode tmp = mHead;
        if (tmp != NODE_NULL) {
            do {
                tmp.filter.setOutputSize(width, height);
                tmp = tmp.next;
            } while (tmp != NODE_NULL);
        }
    }

    public int size() {
        return mCount;
    }

    private boolean contains(FilterNode node, Class input) {
        return TextUtils.equals(node.key, input.getName());
    }

    private static class FilterNode {
        String key;
        BaseFilter filter;
        FilterNode next;
        FilterNode prev;
        FilterNode(BaseFilter input, Class filterClass) {
            key = filterClass.getName();
            filter = input;
            next = null;
            prev = null;
        }
    }
}
