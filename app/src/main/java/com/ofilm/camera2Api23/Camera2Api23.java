package com.ofilm.camera2Api23;


import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.params.StreamConfigurationMap;

import com.ofilm.camera2.Camera2;
import com.ofilm.cameraview.PreviewImpl;
import com.ofilm.utils.Size;
import com.ofilm.utils.SizeMap;

/**
 *  继承自Camera2 主要区别在 sdk > 21 时, 收集相机支持的最高分辨率尺寸 </>,
 *
 * */

public class Camera2Api23 extends Camera2 {

    public Camera2Api23(Callback callback, PreviewImpl preview, Context context) {
        super(callback, preview, context);
    }

    @Override
    protected void collectPictureSizes(SizeMap mPictureSizes, StreamConfigurationMap map) {
        android.util.Size[] outputSize = map.getHighResolutionOutputSizes(ImageFormat.JPEG);
        if(outputSize != null){
            for(android.util.Size size : outputSize){
                mPictureSizes.add(new Size(size.getWidth(), size.getHeight()));
            }
        }
        if(mPictureSizes.isEmpty()){
            super.collectPictureSizes(mPictureSizes, map);
        }
    }
}
