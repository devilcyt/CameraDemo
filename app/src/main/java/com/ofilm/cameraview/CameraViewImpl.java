package com.ofilm.cameraview;


/**
*   抽象类, 子类是camera1 camera2 camera2Api23
*
*   功能描述： 子类实现开启相机等操作，此类提供对外一致的接口, 达到封装的目的
*
* */

import android.view.View;

import com.ofilm.utils.AspectRatio;

import java.util.Set;


public abstract class CameraViewImpl {

    protected final Callback mCallback; // 相机状态回调
    protected final PreviewImpl mPreview; // surfaceview

    public CameraViewImpl(Callback callback, PreviewImpl preview){
        mCallback = callback;
        mPreview = preview;
    }

    public View getView(){
        return mPreview.getView(); // 返回 surfaceview
    }

    public abstract boolean start();

    public abstract  void stop();

    public abstract boolean isCameraOpened();

    public abstract void setFacing(int facing); // 设置camera id

    public abstract int getFacing();

    public abstract Set<AspectRatio> getSupportedAspectRatios(); // 获取相机支持的预览比例

    /**
     * @return {@code true} if the aspect ratio was changed.
     */
    public abstract boolean setAspectRatio(AspectRatio ratio); // 设置拍摄图片比例

    public abstract AspectRatio getAspectRatio();

    public abstract void setAutoFocus(boolean autoFocus); // 设置自对焦

    public abstract boolean getAutoFocus(); // 获取自动对焦状态, 判断是否支持自动对焦

    public abstract int getFlash();

    public abstract void setFlash(int flash); // 设置闪光灯模式

    public abstract void takePicture();

    public abstract void setDisplayOrientation(int displayOrientation); // 设置预览矫正角度

    public interface Callback {

        void onCameraOpened();

        void onCameraClosed();

        void onPictureTaken(byte[] data);

    }

    public String getUsingCamName() {
        return usingCamName;
    }

    public void setUsingCamName(String usingCamName) {
        this.usingCamName = usingCamName;
    }

    private String usingCamName; // 代表使用哪个版本的CAM
}
