package com.example.camera.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

import com.example.camera.utils.LogUtil;

/**
 *
 *  mecer
 *
 */


public class Camera2TextureView extends TextureView {

    private int mRatioWidth;
    private int mRatioHeight;


    public Camera2TextureView(Context context) {
        super(context);
    }

    public Camera2TextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Camera2TextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public Camera2TextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    // Sets the aspect ratio
    public void setAspectRatio(int width, int height ){
        if(width < 0 || height < 0){
            throw new IllegalArgumentException("negative size");
        }
        mRatioHeight = height;
        mRatioWidth = width;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LogUtil.d("mRatioWidth = " + mRatioWidth + ", mRatioHeight = " + mRatioHeight);
        int screenWidth = MeasureSpec.getSize(widthMeasureSpec);
        int screenHeight = MeasureSpec.getSize(heightMeasureSpec);
        if(mRatioWidth == 0 || mRatioHeight == 0){
            setMeasuredDimension(screenWidth, screenHeight);
        }else if(screenWidth < screenHeight * mRatioWidth / mRatioHeight){
            setMeasuredDimension(screenWidth, screenWidth * mRatioHeight / mRatioWidth);
        }else{
            setMeasuredDimension(screenHeight * mRatioWidth / mRatioHeight, screenHeight);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);




    }
}
