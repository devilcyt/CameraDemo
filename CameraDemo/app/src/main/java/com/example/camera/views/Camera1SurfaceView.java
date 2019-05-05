package com.example.camera.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class Camera1SurfaceView extends SurfaceView {
    public Camera1SurfaceView(Context context) {
        this(context, null ,0);
    }

    public Camera1SurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Camera1SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = width / 3 * 4;
        setMeasuredDimension(width, height);
    }
}
