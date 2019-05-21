package com.ofilm.cameraview;


/*
* 以向后兼容的方式封装与相机预览相关的所有操作
*
* */

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

public abstract class PreviewImpl {

    public interface Callback{ // call by camera1 camera2 camera2Api23
        void onSurfaceChanged();
    }

    private Callback mCallback;

    private int mWidth;

    private int mHeight;

    public void setCallback(Callback callback){
        mCallback = callback;
    }

    public abstract Surface getSurface();

    public abstract View getView();

    public abstract Class getOutputClass(); // 获取预览的class, { es: return SurfaceTexture.class }

    public abstract void setDisplayOrientation(int displayOrientation);

    public abstract boolean isReady();

    protected void dispatchSurfaceChanged(){
        mCallback.onSurfaceChanged();
    }

    public SurfaceHolder getSurfaceHolder(){return null;} // 子类会覆写

    public Object getSurfaceTexture(){return null;}

    public void setBufferSize(int width, int height){
        mWidth = width;
        mHeight = height;
    }

    public int getWidth(){return mWidth;}

    public int getHeight(){return mHeight;}
}
