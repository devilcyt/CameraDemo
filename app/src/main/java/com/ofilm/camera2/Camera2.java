package com.ofilm.camera2;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;
import android.view.Surface;

import com.ofilm.cameraview.CameraViewImpl;
import com.ofilm.cameraview.PreviewImpl;
import com.ofilm.utils.AspectRatio;
import com.ofilm.utils.Constants;
import com.ofilm.utils.LogUtil;
import com.ofilm.utils.Size;
import com.ofilm.utils.SizeMap;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;

/**
 *   api2 camera
 *
 *   ae_mode : 自动曝光
 *
* */

@SuppressWarnings("MissingPermission")
@TargetApi(21)
public class Camera2 extends CameraViewImpl {

    private static final SparseIntArray INTERNAL_FACINGS = new SparseIntArray();

    static {
        INTERNAL_FACINGS.put(Constants.FACING_BACK, CameraCharacteristics.LENS_FACING_BACK);
        INTERNAL_FACINGS.put(Constants.FACING_FRONT, CameraCharacteristics.LENS_FACING_FRONT);
    }


    private static final int MAX_PREVIEW_WIDTH = 1920;

    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private CameraManager mCameraManager = null;

    private CameraDevice mCamera;

    private String mCameraId;

    private CameraCaptureSession mCaptureSession;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private ImageReader mImageReader;

    private final SizeMap mPreviewSizes = new SizeMap();

    private final SizeMap mPictureSizes = new SizeMap();

    private int mFacing;

    private boolean mAutoFocus;

    private int mFlash;

    private int mDisplayOrientation;

    private AspectRatio mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;

    private CameraCharacteristics mCameraCharacteristics;

    private final CameraDevice.StateCallback mCameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            mCallback.onCameraOpened();
            startCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCallback.onCameraClosed();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCamera = null;
        }
    };

    private final CameraCaptureSession.StateCallback mSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {

            if(mCamera == null){
                return;
            }
            mCaptureSession = session;
            updateAutoFocus();
            updateFlash();

            try{
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                        mCaptureCallback, null);
            }catch (CameraAccessException e){
                LogUtil.d("Failed to start camera preview because it couldn't access camera : " + e);
            }catch (IllegalStateException e){
                LogUtil.d("Failed to start camera preview : " + e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            LogUtil.d("Failed to configure capture session");
        }
    };

    PictureCaptureCallback mCaptureCallback = new PictureCaptureCallback() {
        @Override
        public void onReady() {
            captureStillPicture();
        }

        @Override
        public void onPrecaptureRequired() {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            setState(STATE_PRECAPTURE);
            try{
                mCaptureSession.capture(mPreviewRequestBuilder.build(), this, null);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
            }catch (CameraAccessException e){
                LogUtil.d("Failed to tun precapture sequence ." + e);
            }
        }
    };


    /**
     *  图片保存操作
     *
    * */

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
             = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireNextImage()){
                Image.Plane[] planes = image.getPlanes();
                if(planes.length > 0){
                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    mCallback.onPictureTaken(data);
                }
            }
        }
    };


    public Camera2(Callback callback, PreviewImpl preview, Context context){
        super(callback, preview);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mPreview.setCallback(new PreviewImpl.Callback() {
            @Override
            public void onSurfaceChanged() {
                startCaptureSession();
            }
        });
    }

    @Override
    public boolean start() {
        if(!chooseCameraIdByFacing()){
            return false;
        }
        collectCameraInfo();
        prepareImageReader();
        startOpeningCamera();
        return true;
    }

    private void startOpeningCamera() {
        try{
            mCameraManager.openCamera(mCameraId, mCameraDeviceCallback, null);
        }catch (CameraAccessException e){
            throw new RuntimeException("Failed to open camera by " + mCameraId);
        }
    }

    private void startCaptureSession() {
        if(!isCameraOpened() || !mPreview.isReady() || mImageReader == null){
            return;
        }
        Size previewSize = chooseOptimalSize();
        mPreview.setBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = mPreview.getSurface();
        try{
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCamera.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    mSessionCallback, null);
        }catch (CameraAccessException e){
            throw new RuntimeException("Failed to start camera session.");
        }
    }

    private void collectCameraInfo() {
        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if(map == null){
            throw new IllegalStateException("Failed to get configuration map:" + mCameraId);
        }
        mPreviewSizes.clear();
        for (android.util.Size size : map.getOutputSizes(mPreview.getOutputClass())){
            int width = size.getWidth();
            int height = size.getHeight();
            if(width <= MAX_PREVIEW_WIDTH && height <= MAX_PREVIEW_HEIGHT){
                mPreviewSizes.add(new Size(width, height));
            }
        }
        mPictureSizes.clear();
        collectPictureSizes(mPictureSizes, map);
        for(AspectRatio ratio : mPreviewSizes.ratios()){
            if(!mPictureSizes.ratios().contains(ratio)){
                mPreviewSizes.remove(ratio);
            }
        }

        if(!mPreviewSizes.ratios().contains(mAspectRatio)){
            mAspectRatio = mPreviewSizes.ratios().iterator().next();
        }
    }

    protected void collectPictureSizes(SizeMap mPictureSizes, StreamConfigurationMap map) {
        for(android.util.Size size : map.getOutputSizes(ImageFormat.JPEG)){
            mPictureSizes.add(new com.ofilm.utils.Size(size.getWidth(), size.getHeight()));
        }
    }

    /**
     *  选取合适的预览大小
     *
     * */
    private Size chooseOptimalSize(){
        int surfaceLonger, surfaceShorter;
        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();
        if(surfaceWidth < surfaceHeight){
            surfaceLonger = surfaceHeight;
            surfaceShorter = surfaceWidth;
        } else {
            surfaceLonger = surfaceWidth;
            surfaceShorter = surfaceHeight;
        }
        SortedSet<Size> candidates = mPreviewSizes.sizes(mAspectRatio);

        // 选取一个最适合屏幕的大小
        for (Size size : candidates){
            if(size.getWidth() >= surfaceLonger && size.getHeight() >= surfaceShorter){
                return size;
            }
        }
        return candidates.last();
    }

    private boolean chooseCameraIdByFacing() {
        try {
            int internalFacing = INTERNAL_FACINGS.get(mFacing);
            // 拿到所有的camera id
            String[] ids = mCameraManager.getCameraIdList();
            if (ids.length == 0) {
                throw new RuntimeException("no camera available.");
            }

            /**
             *  INFO_SUPPORTED_HARDWARE_LEVEL： 相机设备权限 LEGACY < LIMITED < FULL < LEVEL_3. 越靠右边权限越大
             * */
            for (String id : ids) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    continue;
                }
                Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (internal == null) {
                    throw new NullPointerException(" LENS_FACING is null .");
                }
                if (internal == internalFacing) {
                    LogUtil.d("choose a Compatible id  = " + id);
                    mCameraId = id;
                    mCameraCharacteristics = characteristics;
                    return true;
                }
            }

            // if not found
            mCameraId = ids[0];
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            LogUtil.d("choose the first id of ids  = " + mCameraId);
            Integer level = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
            {
                return false;
            }
            Integer internal = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (internal == null) {
                throw new NullPointerException("LENS_FACING is null.");
            }
            for (int i = 0, count = INTERNAL_FACINGS.size(); i < count; i++) {
                if (INTERNAL_FACINGS.valueAt(i) == internal) {
                    mFacing = INTERNAL_FACINGS.valueAt(i);
                    return true;
                }
            }
            mFacing = Constants.FACING_BACK;
            //LogUtil.d("mFaceing = " + mFacing + ", mCameraId = " + mCameraId + ", mCameraCharacteristics = " + mCameraCharacteristics);
            return true;
        }catch (CameraAccessException e){
            throw new RuntimeException("Failed to get a list of camera device." + e);
        }
    }

    @Override
    public void stop() {
        if(mCaptureSession != null){
            mCaptureSession.close();
            mCaptureSession = null;
        }

        if(mCamera != null){
            mCamera.close();
            mCamera = null;
        }

        if(mImageReader != null){
            mImageReader.close();
            mImageReader = null;
        }
    }

    @Override
    public boolean isCameraOpened() {
        return mCamera != null;
    }

    /**
     *  初始化和切换摄像头时调用.
     * */
    @Override
    public void setFacing(int facing) {
        if(mFacing == facing){
            return;
        }
        mFacing = facing;
        if(isCameraOpened()){
            stop();
            start();
        }
    }

    @Override
    public int getFacing() {
        return mFacing;
    }

    @Override
    public Set<AspectRatio> getSupportedAspectRatios() {
        return mPreviewSizes.ratios();
    }

    @Override
    public boolean setAspectRatio(AspectRatio ratio) {
        if(ratio == null || ratio.equals(mAspectRatio)
                || !mPreviewSizes.ratios().contains(ratio)){
            return false;
        }
        mAspectRatio = ratio;
        prepareImageReader();
        if(mCaptureSession != null){
            mCaptureSession.close();
            mCaptureSession = null;
            startCaptureSession(); // 更改了比例, 重新创建会话
        }
        return true;
    }

    @Override
    public AspectRatio getAspectRatio() {
        return mAspectRatio;
    }

    @Override
    public void setAutoFocus(boolean autoFocus) {
        if(mAutoFocus == autoFocus){
            return;
        }
        mAutoFocus = autoFocus;
        if(mPreviewRequestBuilder != null){
            updateAutoFocus();
            if(mCaptureSession != null){
                try{
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                            mCaptureCallback, null);
                }catch (CameraAccessException e){
                    mAutoFocus = !mAutoFocus; // 取反
                }
            }
        }
    }

    @Override
    public boolean getAutoFocus() {
        return mAutoFocus;
    }

    @Override
    public void setFlash(int flash) {
        if(mFlash == flash){
            return;
        }
        int temp = mFlash; // 先保存上一个值, 如果设置失败就恢复
        mFlash = flash;
        if(mPreviewRequestBuilder != null){
            updateFlash();
            if(mCaptureSession != null){
                try{
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                            mCaptureCallback, null);
                }catch (CameraAccessException e){
                    mFlash = temp;
                }
            }
        }
    }

    @Override
    public void takePicture() {
        if(mAutoFocus){
            lockFocus();
        }else {
            captureStillPicture();
        }
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        mPreview.setDisplayOrientation(mDisplayOrientation);
    }

    @Override
    public int getFlash() {
        return mFlash;
    }

    private void prepareImageReader() {
        if(mImageReader != null){
            mImageReader.close();
        }
        Size largest = mPictureSizes.sizes(mAspectRatio).last();
        mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
    }

    private void updateFlash() {

        switch (mFlash){
            case Constants.FLASH_OFF : {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            }

            case Constants.FLASH_ON : {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            }

            case Constants.FLASH_TORCH : {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH);
                break;
            }

            case Constants.FLASH_AUTO : {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            }

            // 红眼模式暂时不添加
            /*
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
            * */
        }
    }

    private void lockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        try{
            mCaptureCallback.setState(PictureCaptureCallback.STATE_LOCKING);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
        }catch (CameraAccessException e){
            LogUtil.d("Failed to lock focus .");
        }
    }

    /**
     *  拍照后执行，解锁自动对焦并重启预览
     *
     * */
    private void unlockFocus(){
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
            updateAutoFocus();
            updateFlash();
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback,
                    null);
            mCaptureCallback.setState(PictureCaptureCallback.STATE_PREVIEW);
        } catch (CameraAccessException e) {
            LogUtil.d("Failed to restart camera preview." + e);
        }
    }


    private void updateAutoFocus() {
        if(mAutoFocus){
            int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            if(modes == null || modes.length == 0
                    || (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AE_MODE_OFF)){
                mAutoFocus = false;
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);

            } else {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }
        } else {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        }
    }

    private void captureStillPicture() {
        try {
            CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE));
            switch (mFlash) {
                case Constants.FLASH_OFF:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_OFF);
                    break;
                case Constants.FLASH_ON:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    break;
                case Constants.FLASH_TORCH:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_TORCH);
                    break;
                case Constants.FLASH_AUTO:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
                case Constants.FLASH_RED_EYE:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
            }
            // Calculate JPEG orientation.
            int sensorOrientation = mCameraCharacteristics.get(
                    CameraCharacteristics.SENSOR_ORIENTATION);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    (sensorOrientation +
                            mDisplayOrientation * (mFacing == Constants.FACING_FRONT ? 1 : -1) +
                            360) % 360);
            // Stop preview and capture a still picture.
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureRequestBuilder.build(),
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                       @NonNull CaptureRequest request,
                                                       @NonNull TotalCaptureResult result) {
                            unlockFocus();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            LogUtil.d("Cannot capture a still picture." + e);
        }
    }

    /**
     *  A callback for capture picture
     *
     * */
    private static abstract class PictureCaptureCallback extends
            CameraCaptureSession.CaptureCallback{

        static final int STATE_PREVIEW = 0;
        static final int STATE_LOCKING = 1;
        static final int STATE_LOCKED = 2;
        static final int STATE_PRECAPTURE = 3;
        static final int STATE_WAITTING = 4;
        static final int STATE_CAPTURING = 5;

        private int mState;

        PictureCaptureCallback(){}

        void setState(int state){
            mState = state;
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

        void process(CaptureResult result){
            switch(mState){
                case STATE_LOCKING : {
                    Integer af = result.get(CaptureResult.CONTROL_AF_STATE);
                    if(af == null){
                        break;
                    }
                    if(af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                        af == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                        Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                        if(ae == null || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED){
                            setState(STATE_CAPTURING);
                            onReady();
                        }else{
                            setState(STATE_LOCKED);
                            onPrecaptureRequired();
                        }
                    }
                    break;
                }

                case STATE_PRECAPTURE : {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_PRECAPTURE
                            || ae == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED
                            || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED){
                        setState(STATE_WAITTING);
                    }
                    break;
                }

                case STATE_WAITTING : {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae != CaptureResult.CONTROL_AE_STATE_PRECAPTURE){
                        setState(STATE_CAPTURING);
                        onReady();
                    }
                    break;
                }
            }


        }

        /**
         * 当准备好拍照时调用.
         */
        public abstract void onReady();

        /**
         * 当需要运行拍照时调用.
         */
        public abstract void onPrecaptureRequired();
    }

}
