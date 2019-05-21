package com.example.camera.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;

import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.camera.utils.DialogUtil;
import com.example.camera.utils.SystemReflectionProxy;
import com.example.camera.views.Camera2TextureView;
import com.example.camerademo.R;
import com.example.camera.utils.LogUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;



/**
 *
 * Camera操作类
 * author: mecer
 */

public class Camera2Fragment extends Fragment implements View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    };

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private static final String PROP_RECTIFY = "persist.camera.rectify.enable";

    // 记录相机状态
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    public static Camera2Fragment newInstance() {
        return new Camera2Fragment();
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
             = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // do opencamera
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // do config action
            LogUtil.i("onSurfaceTextureSizeChanged");
            confiurerTansform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            LogUtil.i("111");
        }
    };

    // 相机初始化等需要的一些变量
    private String mCameraId;
    private Camera2TextureView mTextureView;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;

    private ImageReader mImageReader;
    private File mFile;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    //camera fps range
    private static Range<Integer>[] fpsRnage;

    // 后台保存图片线程
    private static class ImageSaver implements Runnable {

        private final Image mImage;
        private final File mFile;

        ImageSaver(Image image, File file){
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(mFile);
                outputStream.write(bytes);
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                mImage.close();
                if (outputStream != null){
                    try{
                        outputStream.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
            LogUtil.i("保存图片流程结束");
        }
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
             = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //存放照片
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }
    };

    private Semaphore mCameraLock = new Semaphore(1); // 安全锁，防止app突然crash



    // 相机状态监听回调
    private final CameraDevice.StateCallback mStateCallBack;

    {
        mStateCallBack = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                LogUtil.i("camera onOpened");
                mCameraLock.release();
                mCameraDevice = camera;
                //创建预览
                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                LogUtil.i("camera onDisconnected");
                mCameraLock.release();
                camera.close();
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                LogUtil.i("camera onError");
                mCameraLock.release();
                camera.close();
                mCameraDevice = null;
                Activity activity = getActivity();
                if(activity != null){
                    activity.finish();
                }
            }
        };
    }


    // 拍照回调
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result){
            switch(mState){
                case STATE_PREVIEW:
                    break;
                case STATE_WAITING_LOCK:{
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if(afState == null){
                        captureStillPicture(); // 拍照
                    }else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if( aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED)
                        {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        }else{
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE:{
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE:{
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,  CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted( CameraCaptureSession session,  CaptureRequest request,  TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);
        }
    };

    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private int mState = STATE_PREVIEW;
    private boolean mFlashSupported;
    private int mSensorOrientation;

    private Button rectifyButton;
    private Button switchCamButton;
    private SystemReflectionProxy propertiesProxy2;

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.camera_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        rectifyButton = view.findViewById(R.id.prop_rectify);
        rectifyButton.setOnClickListener(this);
        switchCamButton = view.findViewById(R.id.switchcam_btn);
        switchCamButton.setOnClickListener(this);
        propertiesProxy2 = SystemReflectionProxy.newInstance();
        propertiesProxy2.set(PROP_RECTIFY, String.valueOf(0));
        updateUi();
        view.findViewById(R.id.photo_btn).setOnClickListener(this);
        mTextureView = view.findViewById(R.id.texture_view);
        mTextureView.setVisibility(View.VISIBLE);
    }

    private void updateUi(){
        if(SystemReflectionProxy.getInt(PROP_RECTIFY, 3) == 3 ){
            rectifyButton.setText(R.string.prop_enable);
        }else{
            rectifyButton.setText(R.string.prop_disable);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFile = new File(getActivity().getExternalFilesDir(null), "pic.jpg");
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        updateUi();
        if(mTextureView.isAvailable()){
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        }else{
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    //设置合适的预览尺寸
    private static Size chooseOptmalSize(Size[] sizes, int textureViewWidth, int textureViewHeight,
            int maxWidth, int maxHeight, Size aspectRatio){
        List<Size> bigEnough = new ArrayList<>();   // size比surfacevie大
        List<Size> notBigEnough = new ArrayList<>(); // size比surfaceview小

        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for(Size option : sizes){
            if(option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w){
                if(option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight){
                    LogUtil.i("bigEnough ++");
                    bigEnough.add(option);
                }else{
                    notBigEnough.add(option);
                    LogUtil.i("notBigEnough ++");
                }
            }
        }

        if(bigEnough.size() > 0){
            return Collections.min(bigEnough, new CompareSizesByArea());
        }else if(notBigEnough.size() > 0){
            return Collections.max(notBigEnough, new CompareSizesByArea());
        }else {
            LogUtil.i("找不到合适的预览 size");
            return sizes[0];
        }

    }

    // 配置相机参数
    private void setUpCameraOutputs(int width, int height){
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try{
            for(String cameraId : manager.getCameraIdList()){
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                //camera fps settings
                fpsRnage = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                LogUtil.i("FPS INFO : " + Arrays.toString(fpsRnage) + SystemReflectionProxy.getInt(PROP_RECTIFY,3));

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }


                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if(map == null){
                    continue;
                }

                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, 2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation){
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if(mSensorOrientation == 90 || mSensorOrientation == 270){
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_270:
                    case Surface.ROTATION_90:
                        if(mSensorOrientation == 0 || mSensorOrientation == 180){
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        LogUtil.i("invalid display rotation : " + displayRotation);
                }

                Point disPlaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(disPlaySize);
                int rotatePreviewWidth = width;
                int rotatePreviewHeight = height;
                int maxPreviewWidth = disPlaySize.x;
                int maxPreviewHeight = disPlaySize.y;

                if(swappedDimensions){  // 矫正画面
                    rotatePreviewWidth = height;
                    rotatePreviewHeight = width;
                    maxPreviewWidth = disPlaySize.y;
                    maxPreviewHeight = disPlaySize.x;
                }

                if(maxPreviewWidth > MAX_PREVIEW_WIDTH){
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }
                if(maxPreviewHeight > MAX_PREVIEW_HEIGHT){
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                mPreviewSize = chooseOptmalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatePreviewWidth,rotatePreviewHeight, maxPreviewWidth, maxPreviewHeight, largest);
                LogUtil.i("mPreviewSize = " + mPreviewSize);

                // 横竖屏
                int orientation = getResources().getConfiguration().orientation;
                if(orientation == Configuration.ORIENTATION_LANDSCAPE){
                    mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                }else{
                    mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                Boolean avaiable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = avaiable == null ? false : avaiable;

                mCameraId = cameraId;
                return;
            }
        }catch (CameraAccessException e){
            e.printStackTrace();
        }catch (NullPointerException e){
            DialogUtil.ErrorDialog.newInstance("相机初始化失败").show(getChildFragmentManager(),"dialog");
        }
    }

    // 通过配置surfaceview的matrix 防止画面拉伸或者变形什么的

    private void confiurerTansform(int viewWidth, int viewHeight){
        Activity activity = getActivity();
        if(null == mTextureView || null == mPreviewSize || null == activity){
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        LogUtil.i("rotation = " + rotation);
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0,0, viewWidth, viewHeight);
        RectF  bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if(Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation){
            LogUtil.i("90 | 270 ");
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float)viewHeight / mPreviewSize.getHeight(),
                    (float)viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }else if(Surface.ROTATION_180 == rotation){
            LogUtil.i("180");
            matrix.postRotate(180, centerX, centerY);
        }else{
            LogUtil.i("0");
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float)viewHeight / mPreviewSize.getHeight(),
                    (float)viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(0, centerX, centerY);
        }
        LogUtil.i("matix = " + matrix);
        mTextureView.setTransform(matrix);
    }

    // open camera
    private void openCamera(int width, int height){
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width,height);
        confiurerTansform(width,height);

        Activity activity = getActivity();
        CameraManager manager = (CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);
        try{
            if(!mCameraLock.tryAcquire(2500, TimeUnit.MILLISECONDS)){
                throw new RuntimeException("Time out waiting to lock camera opening");
            }
            manager.openCamera(mCameraId, mStateCallBack, mBackgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();

        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    // close camera
    private void closeCamera(){
        try{
            mCameraLock.acquire();
            if(mCaptureSession != null){
                mCaptureSession.close();
                mCaptureSession = null;
            }

            if(mCameraDevice != null){
                mCameraDevice.close();
                mCameraDevice = null;
            }

            if(mImageReader != null){
                mImageReader.close();
                mImageReader = null;
            }
        }catch (InterruptedException e){
            // 打开和关闭相机都会加一个安全锁,防止打开/关闭相机时 app发生异常导致打开/关闭相机出现异常
            throw new RuntimeException("Interupted happende while trying to lock camera closing", e);
        }finally {
            mCameraLock.release();
        }
    }

   /* private static final CaptureRequest.Key<Integer> INSTANT_AEC =
            new CaptureRequest.Key<Integer>(",", Integer.class);*/

    // 创建会话,用于开启预览和拍照
    private void createCameraPreviewSession(){
        try{
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            assert surfaceTexture != null;
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(surfaceTexture);
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured( CameraCaptureSession session) {
                            if(mCameraDevice == null){
                                return;
                            }
                            mCaptureSession = session;
                            try{
                                // 配置一些参数,例如对焦模式等
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                //mPreviewRequestBuilder.set(CaptureRequest.  );
                                setAutoFlash(mPreviewRequestBuilder);

                                // set fps range
                                //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRnage[3]);
                                // 开启预览
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            }catch (CameraAccessException e){
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed( CameraCaptureSession session) {
                            showToast(" createCameraPreviewSession failed ");
                        }
                    }, null);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void captureStillPicture(){
        try{
            final Activity activity = getActivity();
            if(activity == null || mCameraDevice == null){
                return;
            }

            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());


            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback captureCallback
                     = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted( CameraCaptureSession session,  CaptureRequest request,  TotalCaptureResult result) {
                        showToast("capture complete Saved: " + mFile.toString());
                        unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), captureCallback, null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.photo_btn:{
                takePicture();
                break;
            }
            case R.id.prop_rectify:{
                if(SystemReflectionProxy.getInt(PROP_RECTIFY, 3) == 3){
                    SystemReflectionProxy.set(PROP_RECTIFY,String.valueOf(0));
                    rectifyButton.setText(R.string.prop_disable);
                    showToast("disable rectify ===========");
                }else{
                    SystemReflectionProxy.set(PROP_RECTIFY,String.valueOf(3));
                    rectifyButton.setText(R.string.prop_enable);
                    showToast("enable rectify ===========");
                }
                break;
            }
            case R.id.switchcam_btn:{
                showToast("working please wait");
                break;
            }
        }
    }


    private void showToast(final String text){
        final Activity activity = getActivity();
        if(activity != null){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // 拍照
    private void takePicture(){
        try{
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void unlockFocus(){
        try{
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mState = STATE_PREVIEW;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }


    private int getOrientation(int rotation){
        return (ORIENTATION.get(rotation) + mSensorOrientation + 270) % 360;
    }


    private void setAutoFlash(CaptureRequest.Builder mPreviewRequestBuilder){
        if(mFlashSupported){
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // 检查size会不会溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    // 权限检查
    private void requestCameraPermission(){
        if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
            DialogUtil.ErrorDialog.newInstance(getResources().getString(R.string.error_permission)).show(getChildFragmentManager(), "dialog");
        }else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION){
            if(grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                LogUtil.i("没有相关权限");
                DialogUtil.ErrorDialog.newInstance("申请相机权限").show(getChildFragmentManager(), "dialog");
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

