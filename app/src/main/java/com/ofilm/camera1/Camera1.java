package com.ofilm.camera1;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.support.v4.util.SparseArrayCompat;
import android.view.SurfaceHolder;

import com.ofilm.cameraview.CameraViewImpl;
import com.ofilm.cameraview.PreviewImpl;
import com.ofilm.utils.AspectRatio;
import com.ofilm.utils.Constants;
import com.ofilm.utils.LogUtil;
import com.ofilm.utils.Size;
import com.ofilm.utils.SizeMap;
import com.ofilm.utils.SystemReflectionProxy;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class Camera1 extends CameraViewImpl {

    private static final int INVALID_CAMERA_ID = -1;

    private static final SparseArrayCompat<String> FLASH_MODES = new SparseArrayCompat<>();

    static {
        FLASH_MODES.put(Constants.FLASH_OFF, Camera.Parameters.FLASH_MODE_OFF);
        FLASH_MODES.put(Constants.FLASH_ON, Camera.Parameters.FLASH_MODE_ON);
        FLASH_MODES.put(Constants.FLASH_TORCH, Camera.Parameters.FLASH_MODE_TORCH);
        FLASH_MODES.put(Constants.FLASH_AUTO, Camera.Parameters.FLASH_MODE_AUTO);
        FLASH_MODES.put(Constants.FLASH_RED_EYE, Camera.Parameters.FLASH_MODE_RED_EYE);
    }

    private int mCameraId;

    //保证方法只有一个线程执行,类似与锁
    private final AtomicBoolean isPictureCaptureInProgress = new AtomicBoolean(false);

    Camera mCamera;

    private Camera.Parameters mCameraParameters;

    private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();

    private final SizeMap mPreviewSize = new SizeMap();

    private final SizeMap mPictureSize = new SizeMap();

    private AspectRatio mAspectRatio;

    private boolean mShowingPreview;

    private boolean mAutoFocus;

    private int mFacing;

    private int mFlash;

    private int mDisplayOrientation;


    public Camera1(Callback callback, PreviewImpl preview){
        super(callback, preview);
        LogUtil.d("Use Camera1 ------");
        preview.setCallback(new PreviewImpl.Callback() {
            @Override
            public void onSurfaceChanged() {
                // songweiqiang fix bug begin
                if(mCamera == null){
                    chooseCamera();
                    openCamera();
                }
                // songweiqiang fix bug end
                if(mCamera != null){
                    LogUtil.d("onSurfaceChanged() : set preview and set parameters ------");
                    setupPreview();
                    adjustCameraParameters();
                    mCamera.startPreview();
                }
            }
        });
    }

    @Override
    public boolean start() {
        // songweiqiang fix bug begin
        if(mPreview.isReady()){
            LogUtil.d("start(): begin start preview .");
            chooseCamera();
            openCamera();
            setupPreview();
        }
        mShowingPreview = true;
        if(mCamera != null){
            mCamera.startPreview(); // 解决后台唤醒时, 预览画面卡主的问题.
        }
        /*if(mCamera != null){
            mCamera.startPreview(); // 开启预览
        }*/
        LogUtil.d("start(): finish start preview .");
        // songweiqiang fix bug end
        return true;

    }

    @Override
    public void stop() {
        if(mCamera != null){
            mCamera.stopPreview();
        }
        mShowingPreview = false;
        releseCamera();
    }

    @Override
    public boolean isCameraOpened() {
        return mCamera != null;
    }

    @Override
    public void setFacing(int facing) {
        if(mFacing == facing){
            return;
        }
        mFacing = facing;
        if(isCameraOpened()){
            // 切换相机,需要重新初始化
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
        SizeMap idealAspectRadios = mPreviewSize;
        for(AspectRatio aspectRatio : idealAspectRadios.ratios()){
            if(mPictureSize.sizes(aspectRatio) == null &&(aspectRatio != AspectRatio.of(4, 3) || aspectRatio != AspectRatio.of(16,9))){
                idealAspectRadios.remove(aspectRatio); // 剔除预览比例与拍照比例不一致的选项
            }
        }
        return idealAspectRadios.ratios();
    }

    /**
     *  更改比例
     *
     * */
    @Override
    public boolean setAspectRatio(AspectRatio ratio) {
        if(mAspectRatio == null || !isCameraOpened()){
            mAspectRatio = ratio;
            return true;
        } else if(!mAspectRatio.equals(ratio)){
            final Set<Size> sizes = mPreviewSize.sizes(ratio);
            if(sizes == null){
                throw new UnsupportedOperationException(ratio + "is not supported");
            } else {
                mAspectRatio = ratio;
                adjustCameraParameters();
                return true;
            }
        }
        return false;
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
            if(setAutoFocusInternal(autoFocus)){
                mCamera.setParameters(mCameraParameters);
            }
    }

    @Override
    public boolean getAutoFocus() {
        if(!isCameraOpened()){
            return mAutoFocus;
        }
        String focusMode = mCameraParameters.getFocusMode();
        return focusMode != null && focusMode.contains("continuous");
    }

    @Override
    public void setFlash(int flash) {
        if(mFlash == flash){
            return;
        }
        if(setFlashInternal(flash)){
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    public int getFlash() {
        return mFlash;
    }

    @Override
    public void takePicture() {
        if(!isCameraOpened()){
            throw new IllegalStateException("Camera is not ready, Call start() before take picture.");
        }
        if(getAutoFocus()){
            mCamera.cancelAutoFocus();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    takePictureInternal();
                }
            });
        }else{
            takePictureInternal();
        }
    }

    /**
     *   拍照回调，被 CameraView.onPictureTaken(x, y)调用，最后在MainActivity中进行数据操作
     * */
    private void takePictureInternal() {
        if(!isPictureCaptureInProgress.getAndSet(true)){
            mCamera.takePicture(null, null, null, new Camera.PictureCallback(){
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    isPictureCaptureInProgress.set(false);
                    mCallback.onPictureTaken(data);
                    camera.cancelAutoFocus();
                    camera.startPreview(); // 拍照后重新开启预览
                }
            });
        }
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        if(mDisplayOrientation == displayOrientation){
            return;
        }

        mDisplayOrientation = displayOrientation;
        if(isCameraOpened()){
            mCameraParameters.setRotation(calcCameraRotation(displayOrientation));
            mCamera.setParameters(mCameraParameters);
            final boolean needToStopPreview = mShowingPreview && Build.VERSION.SDK_INT < 14;
            if(needToStopPreview){
                mCamera.stopPreview();
            }
            mCamera.setDisplayOrientation(calcDisplayOrientation(displayOrientation));
            if(needToStopPreview){
                mCamera.startPreview();
            }
        }
    }


    private void openCamera() {
        if(mCamera != null){
            releseCamera();
        }
        LogUtil.d("open camera with id = " + mCameraId + ", mCamera = " + mCamera + ", mDisplayOrientation = " + calcDisplayOrientation(mDisplayOrientation));
        mCamera = SystemReflectionProxy.openCameraLegacy(mCameraId, SystemReflectionProxy.HAL_VERSION_1);
        //mCamera = Camera.open(mCameraId);
        mCamera.setErrorCallback(errorCallback);
        mCameraParameters = mCamera.getParameters();
        // preview size
        mPreviewSize.clear();
        for(Camera.Size size : mCameraParameters.getSupportedPreviewSizes()){
            mPreviewSize.add(new Size(size.width, size.height));
        }

        // picture size
        mPictureSize.clear();
        for(Camera.Size size : mCameraParameters.getSupportedPictureSizes()){
            mPictureSize.add(new Size(size.width, size.height));
        }

        // AspectRatio 默认4：3
        if(mAspectRatio == null){
            mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;
        }

        adjustCameraParameters();
        mCamera.setDisplayOrientation(90/*calcDisplayOrientation(mDisplayOrientation)*/);
        mCallback.onCameraOpened();
    }

    Camera.ErrorCallback errorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int error, Camera camera) {
            switch (error){
                case Camera.CAMERA_ERROR_SERVER_DIED:{
                    // songweiqiang fix bug
                    stop();
                    start();
                    LogUtil.d("camera server died");
                    break;
                }
            }
        }
    };

    /**
     *  计算相机显示角度
     * */
    private int calcDisplayOrientation(int screenDisplayOrientation) {
        if(mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            return (360 - (mCameraInfo.orientation + screenDisplayOrientation) % 360);
        }else{
            return (mCameraInfo.orientation - screenDisplayOrientation + 360) % 360;
        }

    }

    private void releseCamera() {
        if(mCamera != null){
            mCamera.release();
            mCamera = null;
            mCallback.onCameraClosed();
        }

    }

    /**
     *  根据facing设置相机id，例如后摄，前摄
     *
     *  link{setFacing}
     * */
    private void chooseCamera(){
        for(int i = 0, count = Camera.getNumberOfCameras(); i < count; i ++){
            Camera.getCameraInfo(i, mCameraInfo);
            if(mCameraInfo.facing == mFacing){
                mCameraId = i;
                LogUtil.d("chooseCamera(): camera id " + mCameraId + " info is :" + mCameraInfo);
                return;
            }
        }
        mCameraId = INVALID_CAMERA_ID;
    }

    private void setupPreview(){
        try{
            if(mPreview.getOutputClass() == SurfaceHolder.class){
                LogUtil.d("set preview when using SurfaceView .");
                final boolean needsToStopPreview = mShowingPreview && Build.VERSION.SDK_INT < 14;
                if(needsToStopPreview){
                    mCamera.stopPreview();
                }
                mCamera.setPreviewDisplay(mPreview.getSurfaceHolder());
                if(needsToStopPreview){
                    mCamera.startPreview();
                }
            }else{
                LogUtil.d("set preview when using SurfaceTexture .");
                mCamera.setPreviewTexture((SurfaceTexture) mPreview.getSurfaceTexture());
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private void adjustCameraParameters(){
        SortedSet<Size> sizes = mPreviewSize.sizes(mAspectRatio);
        if(sizes == null){ // 不支持
            mAspectRatio = chooseAspectRatio();
            sizes = mPreviewSize.sizes(mAspectRatio);
        }
        Size size = chooseOptimalSize(sizes); // 选取合适的预览size

        // 选取比例中最大的一个size
        final Size pictureSize = mPictureSize.sizes(mAspectRatio).last();
        if(mShowingPreview){
            mCamera.stopPreview();
        }
        mCameraParameters.setPreviewSize(size.getWidth(), size.getHeight()/*1440, 1080*/);
        LogUtil.d("preview size  = ( " + size.getWidth() + size.getHeight() + ")");
        mCameraParameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        LogUtil.d("picture size  = ( " + pictureSize.getWidth() + pictureSize.getHeight() + ")");
        // setRotation() 设置拍照图片的旋转方向,影响的是JPeg的那个PictureCallback
        // 很多时候只是修改这里返回的exif信息，不会真的旋转图像数据
        mCameraParameters.setRotation(calcCameraRotation(mDisplayOrientation));

        // 自动对焦
        setAutoFocusInternal(mAutoFocus);
        // 设置闪光灯
        setFlashInternal(mFlash);

        LogUtil.d("here set parameters .");
        mCamera.setParameters(mCameraParameters);
        if(mShowingPreview){
            LogUtil.d("here start preview .");
            mCamera.startPreview();
        }

    }

    private int calcCameraRotation(int screenOrientationDegres) {
        if(mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            return (mCameraInfo.orientation + screenOrientationDegres ) % 360;
        }else{
            final int landscapeFilp = isLandscape(screenOrientationDegres) ? 180 : 0;
            return(mCameraInfo.orientation + screenOrientationDegres + landscapeFilp) % 360;
        }
    }

    /**
     *   设置闪光灯
     * */
    private boolean setFlashInternal(int flash) {
        if(isCameraOpened()){
            List<String> modes = mCameraParameters.getSupportedFlashModes();
            String mode = FLASH_MODES.get(flash);
            if(modes != null && modes.contains(mode)){
                mCameraParameters.setFlashMode(mode);
                mFlash = flash;
                return true;
            }
            String currentMode = FLASH_MODES.get(mFlash);
            if(modes == null || !modes.contains(currentMode)){
                mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mFlash = 0; // OFF
                return true;
            }
            return false;
        }else{
            mFlash = flash;
            return false;
        }
    }

    /**
     *   设置对焦模式
     * */
    private boolean setAutoFocusInternal(boolean autoFocus) {
        mAutoFocus = autoFocus;
        if(isCameraOpened()){
            final List<String> modes = mCameraParameters.getSupportedFocusModes();
            if(autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }else if(modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)){
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            }else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                mCameraParameters.setFocusMode(modes.get(0));
            }
            return true;
        }else{
            return false;
        }
    }

    private Size chooseOptimalSize(SortedSet<Size> sizes) {
        if(!mPreview.isReady()){
            LogUtil.d("?????????");
            return sizes.first(); // 返回最小的一个size
        }
        int desiredWidth;
        int desiredHeight;
        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();
        if(isLandscape(mDisplayOrientation)){
            // 横屏下宽高取相反
            desiredWidth = surfaceHeight;
            desiredHeight = surfaceWidth;
        }else{
            desiredWidth = surfaceWidth;
            desiredHeight = surfaceHeight;
        }
        Size result = null;
        for(Size size : sizes){
            LogUtil.d("w , h = " + size.getWidth() + size.getHeight() + "dw , dh = " + desiredWidth +  desiredHeight);
            if(desiredWidth <= size.getWidth() && desiredHeight <= size.getHeight()){
                return size;
            }
            result = size;
        }
        return result;
    }

    private boolean isLandscape(int orientation) {
        return (orientation == Constants.LANDSCAPE_90 || orientation == Constants.LANDSCAPE_270);
    }

    private AspectRatio chooseAspectRatio() {
        AspectRatio r = null;
        for(AspectRatio ratio : mPreviewSize.ratios()){
            r = ratio;
            if(ratio.equals(AspectRatio.of(4,3))){
                return ratio;
            }
        }
        return r;
    }


}
