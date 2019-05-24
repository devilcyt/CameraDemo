package com.ofilm.cameraview;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.ofilm.camera.R;
import com.ofilm.utils.LogUtil;

public class SurfaceViewPreview extends PreviewImpl {

    final SurfaceView mSurfaceView;

    SurfaceViewPreview(Context context, ViewGroup parent){
        View view = View.inflate(context, R.layout.surface_view, parent);
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surface_view);
        SurfaceHolder holder = mSurfaceView.getHolder();

        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                LogUtil.d("surfaceCreated() .");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                LogUtil.d("surfaceChanged() .");
                setSize(width, height);
                if(!ViewCompat.isInLayout(mSurfaceView)){
                    dispatchSurfaceChanged();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                LogUtil.d("surfaceDestroyed() .");
                setSize(0, 0);
            }
        });
    }

    @Override
    public Surface getSurface() {
        return getSurfaceHolder().getSurface();
    }

    @Override
    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    @Override
    public View getView() {
        return mSurfaceView;
    }

    @Override
    public Class getOutputClass() {
        return SurfaceHolder.class;
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {

    }

    @Override
    public boolean isReady() {
        return getWidth() != 0 && getHeight() != 0;
    }
}
