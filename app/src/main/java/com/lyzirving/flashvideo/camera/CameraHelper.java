package com.lyzirving.flashvideo.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.lyzirving.flashvideo.util.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * @author lyzirving
 */
public class CameraHelper {
    private static final String TAG = "CameraHelper";
    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private volatile static CameraHelper sInstance;

    private CameraDevice mCamera;
    private int mFrontType;
    private SurfaceTexture mOesTexture;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private CameraCaptureSession mCaptureSession;

    /**
     * for landscape preview,
     * view's width should be preview.getWidth(),
     * view's height should be preview.getHeight();
     * for portrait preview,
     * view's width should be preview.getHeight(),
     * view's height should be preview.getWidth();
     */
    private Size mPreviewSize;
    private String mCameraId;
    private boolean mFlashSupport;

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;

    private CameraHelper() {}

    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            LogUtil.d(TAG, "onOpened");
            mCamera = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            LogUtil.d(TAG, "onDisconnected");
            camera.close();
            mCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            LogUtil.d(TAG, "onError: code = " + error);
            camera.close();
            mCamera = null;
        }
    };

    private CameraCaptureSession.StateCallback mCaptureSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            LogUtil.d(TAG, "onConfigured");
            if (mCamera == null) {
                LogUtil.d(TAG, "onConfigured: camera device is null");
                return;
            }
            mCaptureSession = session;
            try {
                // Auto focus should be continuous for camera preview.
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // Flash is automatically enabled when necessary.
                /*if (mFlashSupport) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                }*/
                // Finally, we start displaying the camera preview.
                mPreviewRequest = mPreviewRequestBuilder.build();
                //the preview data will continuously send data to SurfaceTexture
                //the onFrameAvailable callback will be called
                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            LogUtil.d(TAG, "onConfigureFailed");
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {};

    public static CameraHelper get() {
        if (sInstance == null) {
            synchronized (CameraHelper.class) {
                if (sInstance == null) {
                    sInstance = new CameraHelper();
                }
            }
        }
        return sInstance;
    }

    public void closeCamera() {
        try {
            if (null != mCaptureSession) {
                mCaptureSession.close();
            }
            if (null != mCamera) {
                mCamera.close();
            }
            stopCameraThread();
        } catch (Exception e) {
            LogUtil.e(TAG, "closeCamera: exception happens, " + e.getMessage());
            e.printStackTrace();
        } finally {
            mCaptureSession = null;
            mCamera = null;
        }
    }

    public int getFrontType() {
        return mFrontType;
    }

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    @SuppressLint("MissingPermission")
    public void open(Context ctx) {
        if (TextUtils.isEmpty(mCameraId) || mCameraHandler == null) {
            LogUtil.e(TAG, "openCamera: camera does not prepare");
            return;
        }
        try {
            CameraManager manager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            manager.openCamera(mCameraId, mCameraStateCallback, mCameraHandler);
        } catch (Exception e) {
            LogUtil.e(TAG, "openCamera: failed to open, msg = " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void prepare(Context ctx, int width, int height, int frontType) {
        mFrontType = frontType;
        CameraManager manager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics attr = getCameraAttr(manager, frontType);
        if (attr == null) {
            LogUtil.e(TAG, "openCamera: failed to find camera " + frontType);
            return;
        }
        StreamConfigurationMap map = attr.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            LogUtil.e(TAG, "openCamera: config map is null");
            return;
        }
        //get largest image size of jpeg this camera supports
        Size largestOutputSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
        Boolean available = attr.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        mFlashSupport = available == null ? false : available;
        mPreviewSize = getPreviewSize(ctx, attr, map, width, height, largestOutputSize);
        LogUtil.d(TAG, "openCamera: largest jpeg image size = ("
                + largestOutputSize.getWidth() + ", " + largestOutputSize.getHeight() + ")"
                + ", flash support = " + mFlashSupport + ", preview size = ("
                + mPreviewSize.getWidth() + ", " + mPreviewSize.getHeight() + ")");

        startCameraThread();
    }

    public void setOesTexture(SurfaceTexture texture) {
        mOesTexture = texture;
    }

    private void createCameraPreviewSession() {
        try {
            if (mOesTexture == null) {
                LogUtil.e(TAG, "createCameraPreviewSession: texture must be set");
                mCamera.close();
                mCamera = null;
                return;
            }
            mOesTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            // This is the output Surface we need to start preview.
            Surface surface = new Surface(mOesTexture);
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCamera.createCaptureSession(Arrays.asList(surface), mCaptureSessionCallback, mCameraHandler);
        } catch (Exception e) {
            LogUtil.d(TAG, "createCameraPreviewSession: exception happens, " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private CameraCharacteristics getCameraAttr(CameraManager manager, int frontType) {
        try {
            CameraCharacteristics result;
            Integer facing;
            for (String cameraId : manager.getCameraIdList()) {
                result = manager.getCameraCharacteristics(cameraId);
                facing = result.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == frontType) {
                    mCameraId = cameraId;
                    return result;
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "getCameraAttr: exception, " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private Size getPreviewSize(Context ctx, CameraCharacteristics attr, StreamConfigurationMap map,
                                int width, int height, Size largestJpegOutputSize) {
        Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        //phone's natural rotation is vertical-stand, the display rotation is usually 0
        //i-pad's natural direction is horizontal-stand
        int displayRotation = display.getRotation();
        LogUtil.d(TAG, "getPreviewSize: display rotation = " + displayRotation);
        Integer sensorOrientation = attr.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (sensorOrientation == null) {
            sensorOrientation = mFrontType == CameraMetadata.LENS_FACING_FRONT ? 270 : 0;
        }
        LogUtil.d(TAG, "getPreviewSize: sensor rotation = " + sensorOrientation);
        boolean swapDimension = needSwapDimension(displayRotation, sensorOrientation);
        Point windowSize = new Point();
        display.getSize(windowSize);
        int maxPreviewWidth = windowSize.x;
        int maxPreviewHeight = windowSize.y;
        int rotatedPreviewWidth = width;
        int rotatedPreviewHeight = height;
        if (swapDimension) {
            rotatedPreviewWidth = height;
            rotatedPreviewHeight = width;
            maxPreviewWidth = windowSize.y;
            maxPreviewHeight = windowSize.x;
        }
        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
            maxPreviewWidth = MAX_PREVIEW_WIDTH;
        }
        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }
        return chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                maxPreviewHeight, largestJpegOutputSize);
    }

    private boolean needSwapDimension(int displayRotation, int sensorOrientation) {
        LogUtil.d(TAG, "needSwapDimension: displayRotation = " + displayRotation);
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180: {
                return sensorOrientation == 90 || sensorOrientation == 270;
            }
            case Surface.ROTATION_90:
            case Surface.ROTATION_270: {
                return sensorOrientation == 0 || sensorOrientation == 180;
            }
            default: {
                LogUtil.d(TAG, "needSwapDimension: default");
                return false;
            }
        }
    }

    private void startCameraThread() {
        mCameraThread = new HandlerThread(TAG);
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    private void stopCameraThread() {
        mCameraThread.quitSafely();
        try {
            mCameraThread.join();
            mCameraThread = null;
            mCameraHandler = null;
        } catch (Exception e) {
            LogUtil.e(TAG, "stopCameraThread: exception, " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
