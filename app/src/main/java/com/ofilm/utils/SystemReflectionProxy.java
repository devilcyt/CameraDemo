package com.ofilm.utils;

import android.hardware.Camera;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 反射代理类
 */

public class SystemReflectionProxy {

    private static Method sysPropGetInt;
    private static Method sysPropSet;
    private static Method openCamera;
    private static final String PROP_RECTIFY = "persist.camera.rectify.enable";
    public static final int HAL_VERSION_1 = 0x100;

    static {
        try{
            sysPropGetInt = Class.forName("android.os.SystemProperties").getMethod("getInt", String.class, int.class);
            sysPropSet = Class.forName("android.os.SystemProperties").getMethod("set", String.class, String.class);
            openCamera = Class.forName("android.hardware.Camera").getMethod("openLegacy", int.class, int.class);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static SystemReflectionProxy newInstance(){
        return new SystemReflectionProxy();
    }


    public static int getInt(String key, int def){
        if(sysPropGetInt != null){
            try {
                return (int) sysPropGetInt.invoke(null, PROP_RECTIFY, 3);
            }catch(IllegalAccessException e){
                e.printStackTrace();
            }catch(InvocationTargetException e){
                e.printStackTrace();
            }
        }
        return def;

    }

    public static void set(String key, String value){
        if(sysPropSet != null){
            try {
                sysPropSet.invoke(null, PROP_RECTIFY, value);
            }catch (IllegalAccessException e){
                e.printStackTrace();
            }catch (InvocationTargetException e){
                e.printStackTrace();
            }
        }
    }


    public static Camera openCameraLegacy(int cameraId, int halVersion){
        try {
            if(openCamera != null){
                LogUtil.d("proxy : open camera legacy  with hal version 1.");
                return (android.hardware.Camera)openCamera.invoke(null, cameraId, halVersion);
            }
        }catch (Exception e){
            e.printStackTrace();
            LogUtil.d("proxy : open camera with hal version default .");
            return android.hardware.Camera.open(cameraId); // 如果反射获取失败, 用这个方法代替。
        }
        return null;
    }

}