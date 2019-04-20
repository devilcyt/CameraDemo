package com.example.camerademo.utils;

import android.content.Context;

import java.io.File;
import java.lang.reflect.Method;

import dalvik.system.DexFile;


/**
 *
 * 设置属性操作类 使用反射获取 API: android.os.SystemProperties
 * author: mecer
 */


public class SystemPropertiesProxy {

    public static Integer get(Context context, String key, int def) throws IllegalArgumentException{
        Integer ret = def;
        try{
            ClassLoader classLoader = context.getClassLoader();
            Class SystemProperties = classLoader.loadClass("android.os.SystemProperties");
            Class[] parameType = new Class[2];
            parameType[0] = String.class;
            parameType[1] = int.class;
            Method getInt = SystemProperties.getMethod("getInt", parameType);
            Object[] params = new Object[2];
            params[0] = new String(key);
            params[1] = new Integer(def);
            ret = (Integer)getInt.invoke(SystemProperties, params);

        }catch (IllegalArgumentException e){
            LogUtil.d("getInt 参数错误");
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }

        return  ret;
    }


    public static void set(Context context, String key, String value){
        try{
            //DexFile dexFile = new DexFile(new File("system/app/Settings.apk"));
            ClassLoader classLoader = context.getClassLoader();
            Class SystemProperties = classLoader.loadClass("android.os.SystemProperties");
            Class[] paramesType = new Class[2];
            paramesType[0] = String.class;
            paramesType[1] = String.class;
            Method set = SystemProperties.getMethod("set",paramesType);
            Object[] objects = new Object[2];
            objects[0] = new String(key);
            objects[1] = new String(value);
            set.invoke(SystemProperties, objects);
        }catch (IllegalArgumentException e){
            LogUtil.d("set 参数错误");
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
    }


}
