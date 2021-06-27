package com.lyzirving.flashvideo.util;

import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.text.TextUtils;

import com.lyzirving.flashvideo.edit.MediaInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lyzirving
 */
public class AssetsManager {
    private static final String TAG = "AssetsManager";
    private static volatile AssetsManager sInstance;
    private static final int END_OF_STREAM = -1;

    private ThreadPoolExecutor mCachedThreadPool;
    private AssetsListener mListener;

    private AssetsManager() {
        buildThreadPoolIfNeed();
    }

    public static AssetsManager get() {
        if (sInstance == null) {
            synchronized (AssetsManager.class) {
                if (sInstance == null) {
                    sInstance = new AssetsManager();
                }
            }
        }
        return sInstance;
    }

    public void copyAssets(final AssetsType type) {
        buildThreadPoolIfNeed();
        mCachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] fileNames = ComponentUtil.get().getAppCtx().getAssets().list(getAssetsTypeStr(type));
                    if (fileNames == null || fileNames.length == 0) {
                        LogUtil.e(TAG, "copyAssets: open failed, " + getAssetsTypeStr(type) + " is empty");
                        return;
                    }
                    List<MediaInfo> infos = new ArrayList<>();
                    MediaInfo tmp;
                    for (int i = 0; i < fileNames.length; i++) {
                        if (copyAssetsInner(type, fileNames[i])) {
                            tmp = getMediaInfo(type, fileNames[i]);
                            if (tmp != null) {
                                infos.add(tmp);
                            }
                        }
                    }
                    if (infos.size() != 0 && mListener != null) {
                        mListener.onMediaDetected(infos);
                    }
                } catch (Exception e) {
                    LogUtil.e(TAG, "copyAssets: exception = " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    public void destroy() {
        if (mCachedThreadPool != null) {
            mCachedThreadPool.shutdown();
            mCachedThreadPool = null;
        }
        mListener = null;
    }

    public void setAssetsListener(AssetsListener listener) {
        mListener = listener;
    }

    private void buildThreadPoolIfNeed() {
        if (mCachedThreadPool == null) {
            mCachedThreadPool = new ThreadPoolExecutor(
                    0, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new AssetsThreadFactory(TAG),
                    new ThreadPoolExecutor.AbortPolicy());
        }
    }

    private boolean copyAssetsInner(AssetsType type, String name) {
        String rootDir = getAppAssetsDirectory(type);
        if (TextUtils.isEmpty(rootDir)) {
            LogUtil.d(TAG, "copyAssetsInner: type " + type + " has no root directory");
            return false;
        }
        if (TextUtils.isEmpty(name)) {
            LogUtil.d(TAG, "copyAssetsInner: name " + name + " is empty");
            return false;
        }
        File root = new File(rootDir);
        if (!root.exists()) {
            root.mkdirs();
        }
        File dst = new File(root, name);
        if (dst.exists()) {
            LogUtil.d(TAG, "copyAssetsInner: " + dst.getAbsolutePath() + " already exists");
            return true;
        }
        LogUtil.d(TAG, "copyAssetsInner: prepare to copy" + dst.getAbsolutePath());
        try {
            InputStream is = ComponentUtil.get().getAppCtx().getAssets().open(getAssetsTypeStr(type) + "/" + name);
            FileOutputStream fos = new FileOutputStream(dst);
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = is.read(buffer)) != END_OF_STREAM) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
            is.close();
            fos.close();
        } catch (Exception e) {
            LogUtil.d(TAG, "copyAssetsInner: exception = " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        LogUtil.d(TAG, "copyAssetsInner: succeed to copy" + dst.getAbsolutePath());
        return true;
    }

    private String getAppAssetsDirectory(AssetsType type) {
        switch (type) {
            case MUSIC: {
                return ComponentUtil.get().getAppCtx().getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath();
            }
            case VIDEO: {
                return ComponentUtil.get().getAppCtx().getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath();
            }
            default: {
                return "";
            }
        }
    }

    private String getAssetsTypeStr(AssetsType type) {
        switch (type) {
            case MUSIC: {
                return "music";
            }
            case VIDEO: {
                return "video";
            }
            default: {
                return "";
            }
        }
    }

    private MediaInfo getMediaInfo(AssetsType type, String name) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            String path = getAppAssetsDirectory(type) + "/" + name;
            retriever.setDataSource(path);
            MediaInfo info = new MediaInfo(type);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            info.name = name;
            info.duration = (TextUtils.isEmpty(durationStr) ? 0 : Integer.parseInt(durationStr)) / 1000;
            info.path = path;
            LogUtil.d(TAG, "getMediaInfo: " + info);
            return info;
        } catch (Exception e) {
            LogUtil.d(TAG, "getMediaInfo: " + name + ", exception = " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public enum AssetsType {
        /**
         * music source
         */
        MUSIC,
        /**
         * video source
         */
        VIDEO;
    }

    public interface AssetsListener {
        /**
         * called when medias are detected
         * @param list
         */
        void onMediaDetected(List<MediaInfo> list);
    }

    private static class AssetsThreadFactory implements ThreadFactory {
        private final ThreadFactory mDefaultThreadFactory;
        private final String mBaseName;
        private final AtomicInteger mCount = new AtomicInteger(0);

        public AssetsThreadFactory(final String baseName) {
            mDefaultThreadFactory = Executors.defaultThreadFactory();
            mBaseName = baseName;
        }

        @Override
        public Thread newThread(Runnable r) {
            final Thread thread = mDefaultThreadFactory.newThread(r);
            thread.setName(mBaseName + "-" + mCount.getAndIncrement());
            return thread;
        }
    }
}
