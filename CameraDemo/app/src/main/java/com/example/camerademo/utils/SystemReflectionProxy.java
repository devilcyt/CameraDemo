package com.example.camerademo.utils;


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

    static {
        try{
            /*Class<?> S = Class.forName("android.os.Systemproperties");
            Method[] M = S.getMethods();
            LogUtil.d("SystemProperties ---- M : " + M.toString());
            for(Method method : M){
                String methodName = method.getName();
                LogUtil.d("SystemProperties ---- methodName : " + methodName);
                if(methodName.equals("get")){
                    sysPropGet = method;
                }else if(methodName.equals("set")){
                    sysPropSet = method;
                }
            }*/
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
            return (Camera)openCamera.invoke(null, cameraId, halVersion);
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }catch (InvocationTargetException e){
            e.printStackTrace();
        }
        return null;
    }

}
