package com.example.camera.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.camera.utils.DialogUtil;
import com.example.camera.utils.SystemReflectionProxy;
import com.example.camera.views.Camera1SurfaceView;
import com.example.camerademo.R;

import java.io.IOException;
import java.util.List;

public class Camera1Fragment extends Fragment implements View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback, Handler.Callback{

    private static final String TAG = "Camera1Fragment";
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private static final boolean DEBUG = true;

    private Activity mActivity;

    private Camera mCamera = null;
    private Camera.Parameters mParameters;
    private Camera.CameraInfo mCameraInfo;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private Camera1SurfaceView mCamera1SurfaceView;
    private SurfaceHolder mPreviewSurface;
    private int mPreviewSurfaceWidth;
    private int mPreviewSurfaceHeight;
    private Button rectifyButton;
    private ImageButton pictureButton;
    private Button switchCamButton;


    //消息标志位
    private static final int MSG_OPEN_CAMERA = 1;
    private static final int MSG_CLOSE_CAMERA = 2;
    private static final int MSG_SET_PREVIEW_SIZE = 3;
    private static final int MSG_SET_PREVIEW_SURFACE = 4;
    private static final int MSG_START_PREVIEW = 5;
    private static final int MSG_STOP_PREVIEW = 6;
    private static final int MSG_SET_PICTURE_SIZE = 7;
    private static final int MSG_TAKE_PICTURE = 8;


    private static final int HAL_VERSION_1 = 0x100;
    private static final int PREVIEW_FORMAT = ImageFormat.NV21;

    private static final String PROP_RECTIFY = "persist.camera.rectify.enable";

    private HandlerThread mCameraThread = null;
    private Handler mCameraHandler = null;


    public static Camera1Fragment newInstance() {
        return new Camera1Fragment();
    }


    /**
     * 听过消息机制集中处理Camera的各个事件
     */
    @Override
    public boolean handleMessage(Message message) {
        switch (message.what){
            case MSG_OPEN_CAMERA:{
                Log.i(TAG, "==========  openCamera");
                openCamera(message.arg1);
                break;
            }

            case MSG_CLOSE_CAMERA:{
                Log.i(TAG, "==========  closeCamera");
                closeCamera();
                break;
            }

            case MSG_SET_PREVIEW_SURFACE:{
                Log.i(TAG, "==========  setPreviewSurface");
                SurfaceHolder previewSurface = (SurfaceHolder)message.obj;
                setPreviewSurface(previewSurface);
                break;
            }

            case MSG_SET_PREVIEW_SIZE:{
                Log.i(TAG, "==========  setPreviewSize");
                int width = message.arg1;
                int height = message.arg2;
                setPreviewSize(width, height);
                break;
            }

            case MSG_SET_PICTURE_SIZE:{
                Log.i(TAG, "==========  setPictureSize");
                int width = message.arg1;
                int height = message.arg2;
                setPictureSize(width,height);
                break;
            }
            case MSG_START_PREVIEW:{
                Log.i(TAG, "==========  startPreview");
                startPreview();
                break;
            }

            case MSG_STOP_PREVIEW:{
                Log.i(TAG, "==========  stopPreview");
                stopPreview();
                break;
            }


            case MSG_TAKE_PICTURE:{
                takePicture();
            }
            default:
                throw new IllegalArgumentException("Illegal message : " + message.what);

        }

        return false;
    }


    private void updateUi(){
        if(SystemReflectionProxy.getInt(PROP_RECTIFY, 3) == 3 ){
            rectifyButton.setText(R.string.prop_enable);
        }else{
            rectifyButton.setText(R.string.prop_disable);
        }
    }

    private void requestCameraPermission(){
        if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
            DialogUtil.ErrorDialog.newInstance(getResources().getString(R.string.error_permission)).show(getChildFragmentManager(), "dialog");
        }else {
            requestPermissions(new String[] {Manifest.permission.CAMERA} , REQUEST_PERMISSIONS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSIONS_CODE){
            if(grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                DialogUtil.ErrorDialog.newInstance(getResources().getString(R.string.error_permission)).show(getChildFragmentManager(),"dialog");
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    /**
     * 加载布局
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return  inflater.inflate(R.layout.camera_layout, container,false);
    }

    /**
     * 初始化组件和ui
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        SystemReflectionProxy.newInstance();

        mCamera1SurfaceView = view.findViewById(R.id.surface_view);
        mCamera1SurfaceView.setVisibility(View.VISIBLE);
        mCamera1SurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mPreviewSurface = holder;
                mPreviewSurfaceWidth = width;  // 保存当前的预览大小， 当切换前后摄时，优先使用此预览大小。
                mPreviewSurfaceHeight = height;
                if(mCameraHandler != null){
                    mCameraHandler.obtainMessage(MSG_SET_PREVIEW_SIZE, width, height).sendToTarget();
                    mCameraHandler.obtainMessage(MSG_SET_PICTURE_SIZE,width,height).sendToTarget();
                    mCameraHandler.obtainMessage(MSG_SET_PREVIEW_SURFACE,holder).sendToTarget();
                    mCameraHandler.sendEmptyMessage(MSG_START_PREVIEW);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mPreviewSurface = null;
                mPreviewSurfaceWidth = 0;
                mPreviewSurfaceHeight = 0;
            }
        });

        rectifyButton = view.findViewById(R.id.prop_rectify);
        rectifyButton.setOnClickListener(this);
        pictureButton = view.findViewById(R.id.photo_btn);
        pictureButton.setOnClickListener(this);
        switchCamButton = view.findViewById(R.id.switchcam_btn);
        switchCamButton.setOnClickListener(this);
        updateUi();

    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();
    }


    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        initCameraInfo();
        if(mCameraHandler != null){
            Log.i(TAG,"======= obtainMessage MSG_OPEN_CAMERA");
            mCameraHandler.obtainMessage(MSG_OPEN_CAMERA, mCameraId, 0).sendToTarget();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        stopBackgroundThread();
        if(mCameraHandler != null){
            mCameraHandler.removeMessages(MSG_OPEN_CAMERA);
            mCameraHandler.sendEmptyMessage(MSG_CLOSE_CAMERA);
        }
    }


    private void openCamera(int id){
        if(mCamera != null){
            return;
        }
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestCameraPermission();
            return;
        }
        Log.i(TAG, "===== into openCamera");

        mCamera = SystemReflectionProxy.openCameraLegacy(id,HAL_VERSION_1);
        mCameraId = id;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        mCamera.getCameraInfo(id, cameraInfo);
        assert mCamera != null;
        mCamera.setDisplayOrientation(getCameraDisPlayOrientation(cameraInfo));

    }

    private void closeCamera(){
        if(mCamera != null){
            mCamera.release();
            mCamera = null;
            mCameraInfo = null;
        }
    }


    /**
     * 初始化摄像头信息。
     */
    private void initCameraInfo(){
        int numberOfCameras = Camera.getNumberOfCameras();
        if(DEBUG){
            Log.d("camera1", "numberOfCameras : " + numberOfCameras);
        }
        for(int cameraid = 0; cameraid < numberOfCameras; cameraid++){
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraid, cameraInfo);
            if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
                // cameraid is 0
                mCameraId = cameraid;
            }else if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                // camera id is 1
                mCameraId = cameraid;
            }
        }
    }



    /**
     * 设置预览 Surface。
     */
    private void setPreviewSurface(SurfaceHolder holder){
        if(mCamera != null && holder != null){
            try{
                mCamera.setPreviewDisplay(holder);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void setPreviewSize(int width, int height){
        if(mCamera != null && width != 0 && height != 0){
            float aspecRatio = (float) width / height;
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> supportdPreviewSize = parameters.getSupportedPreviewSizes();
            for(Camera.Size previewSize : supportdPreviewSize){
                if((float)previewSize.width / previewSize.height == aspecRatio
                    && previewSize.height <= height && previewSize.width <= width){
                    if(DEBUG){
                        Log.d(TAG, "setPreviewSize() called with: width = " + previewSize.width + ", height = " + previewSize.height);
                    }

                    if(isPreviewFormatSupported(parameters, PREVIEW_FORMAT)){
                        parameters.setPreviewFormat(PREVIEW_FORMAT);
                        int frameWidth = previewSize.width;
                        int frameHeight = previewSize.height;
                        int previewFormat = parameters.getPreviewFormat();
                        PixelFormat pixelFormat = new PixelFormat();
                        PixelFormat.getPixelFormatInfo(previewFormat, pixelFormat);
                        int bufferSize = (frameWidth * frameHeight * pixelFormat.bitsPerPixel) / 8;
                        mCamera.addCallbackBuffer(new byte[bufferSize]);
                        mCamera.addCallbackBuffer(new byte[bufferSize]);
                        mCamera.addCallbackBuffer(new byte[bufferSize]);
                    }
                    mCamera.setParameters(parameters);
                    break;
                }
            }
        }
    }


    /**
     * 判断指定的预览格式是否支持。
     */
    private boolean isPreviewFormatSupported(Camera.Parameters parameters, int format) {
        List<Integer> supportedPreviewFormats = parameters.getSupportedPreviewFormats();
        return supportedPreviewFormats != null && supportedPreviewFormats.contains(format);
    }

    /**
     * 开始预览。
     */
    private void startPreview(){
        if(mCamera != null && mPreviewSurface != null){
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bytes, Camera camera) {
                    camera.addCallbackBuffer(bytes); // 回收复用buffer
                }
            });
            mCamera.startPreview();
        }
    }

    /**
     * 停止预览。
     */
    private void stopPreview(){
        if(mCamera !=null){
            mCamera.stopPreview();
        }
    }

    /**
     * 根据指定的尺寸要求设置照片尺寸，需要考虑指定尺寸的比例，选择符合比例的最大尺寸作为照片尺寸。
     *
     */
    private void setPictureSize(int width, int height){
        if(mCamera != null && width != 0 && height != 0){
            float aspecRatio = (float)width / height;
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> supportedPictureSize = parameters.getSupportedPictureSizes();
            for(Camera.Size pictureSize: supportedPictureSize){
                if((float)pictureSize.width / pictureSize.height == aspecRatio){
                    parameters.setPictureSize(pictureSize.width, pictureSize.height);
                    mCamera.setParameters(parameters);
                    break;
                }
            }
        }
    }


    /**
     * 拍照。
     */
    private void takePicture(){
        showToast("take picture");
    }


    private void startBackgroundThread(){
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper(), this);
    }

    private void stopBackgroundThread(){
        mCameraThread.quitSafely();
        try{
            mCameraThread.join();
            mCameraThread = null;
            mCameraHandler = null;
        }catch (Exception e){
            e.printStackTrace();
        }
    }




    /**
     * 获取预览画面要校正的角度。
     */
    private int getCameraDisPlayOrientation(Camera.CameraInfo cameraInfo){
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation){
            case Surface.ROTATION_0:{
                degrees = 0;
                break;
            }

            case Surface.ROTATION_90:{
                degrees = 90;
                break;
            }

            case Surface.ROTATION_180:{
                degrees = 180;
                break;
            }

            case Surface.ROTATION_270:{
                degrees = 270;
                break;
            }
        }

        int result;
        if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360; // mirror
        }else {
            result = (cameraInfo.orientation - degrees + 360 ) % 360;
        }

        return result;

    }


    /**
     * 点击事件处理
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.photo_btn:{
                takePicture();
                break;
            }

            case R.id.prop_rectify:{
                //SystemReflectionProxy.set("ro.boot.selinux", "permissive");
                if(SystemReflectionProxy.getInt(PROP_RECTIFY, 3) == 3){
                    SystemReflectionProxy.set(PROP_RECTIFY,String.valueOf(0));
                    updateUi();
                }else{
                    SystemReflectionProxy.set(PROP_RECTIFY,String.valueOf(3));
                    updateUi();
                }
                break;
            }

            case R.id.switchcam_btn:{
                break;
            }
        }
    }


    /**
     *  单独写一个显示提示语的方法
     */
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
}
