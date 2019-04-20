package com.example.camerademo.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SystemPropertiesProxy2 {

    private static Method sysPropGet;
    private static Method sysPropSet;

    static {
        try{
            Class<?> S = Class.forName("android.os.Systenproperties");
            Method[] M = S.getMethods();
            for(Method method : M){
                String methodName = method.getName();
                if(methodName.equals("get")){
                    sysPropGet = method;
                }else if(methodName.equals("set")){
                    sysPropSet = method;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static SystemPropertiesProxy2 newInstance(){
        return new SystemPropertiesProxy2();
    }


    public static String get(String key, String def){
        try{
            if(sysPropGet != null)
                return (String)sysPropGet.invoke(null, key, def);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }catch (InvocationTargetException e){
            e.printStackTrace();
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }
        return def;

    }

    public static void set(String key, String value){
        try{
            if(sysPropSet != null)
                sysPropSet.invoke(null, key, value);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }catch (InvocationTargetException e){
            e.printStackTrace();
        }

    }

}