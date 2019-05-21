package com.ofilm.cameraview;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;

/**
 *  抽象类，监视从 Display.getRotation()返回的值
 *
 * */

abstract class DisplayOrientationDetector {

    private final OrientationEventListener orientationEventListener;

    static final SparseIntArray DISPLAY_ORIENTATION = new SparseIntArray();

    static {

        DISPLAY_ORIENTATION.put(Surface.ROTATION_0, 0);
        DISPLAY_ORIENTATION.put(Surface.ROTATION_90, 90);
        DISPLAY_ORIENTATION.put(Surface.ROTATION_180, 180);
        DISPLAY_ORIENTATION.put(Surface.ROTATION_270, 270);
    }

    Display mDisplay;

    private int mLastKnownDisplayOrientation = 0;

    public DisplayOrientationDetector(Context context){
        orientationEventListener = new OrientationEventListener(context) {
            private int mLastKnownRotation = -1;
            @Override
            public void onOrientationChanged(int orientation) {
                if(orientation == OrientationEventListener.ORIENTATION_UNKNOWN ||
                        mDisplay == null){
                    return;
                }
                final int rotation = mDisplay.getRotation();
                if(mLastKnownRotation != rotation){
                    mLastKnownRotation = rotation;
                    dispatchOnDisplayOrientationChanged(DISPLAY_ORIENTATION.get(rotation));
                }
            }
        };
    }

    public void enable(Display display){
        mDisplay = display;
        orientationEventListener.enable();
        dispatchOnDisplayOrientationChanged(DISPLAY_ORIENTATION.get(display.getRotation()));
    }

    public void disable(){
        orientationEventListener.disable();
        mDisplay = null;
    }

    public int getLastKnownDisplayOrientation(){
        return mLastKnownDisplayOrientation;
    }


    private void dispatchOnDisplayOrientationChanged(int displayOrientation){
        mLastKnownDisplayOrientation = displayOrientation;
        onDisplayOrientationChanged(displayOrientation);
    }


    /**
     * Called when display orientation is changed.
     *
     * @param displayOrientation One of 0, 90, 180, and 270.
     */
    public abstract void onDisplayOrientationChanged(int displayOrientation); // 回调方法 具体实现在 CameraView 中
}
