package com.example.camera.utils;

import com.example.camerademo.BuildConfig;
import android.util.Log;


/**
 *
 * 日志封装 mecer
 */

public class LogUtil {
    private static String className;//类名
    private static String methodName;//方法名
    private static int lineNumber;//行数
    private static boolean isDebug = BuildConfig.DEBUG;


    private static String createLog(String log ) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("================ : ");
        buffer.append(log);
        return buffer.toString();
    }

    /**
     * 获取文件名、方法名、所在行数
     *
     */
    private static void getMethodNames(StackTraceElement[] sElements){
        className = "swq: " + sElements[1].getFileName();
        methodName = sElements[1].getMethodName();
        lineNumber = sElements[1].getLineNumber();
    }

    public static void e(String message){
        if (!isDebug) {
            return;
        }
        getMethodNames(new Throwable().getStackTrace());
        Log.e(className, createLog(message));
    }

    public static void i(String message){
        if (!isDebug) {
            return;
        }
        getMethodNames(new Throwable().getStackTrace());
        Log.i(className, createLog(message));
    }

    public static void d(String message){
        if (!isDebug) {
            return;
        }
        getMethodNames(new Throwable().getStackTrace());
        Log.d(className, createLog(message));
    }

    public static void v(String message){
        if (!isDebug) {
            return;
        }
        getMethodNames(new Throwable().getStackTrace());
        Log.v(className, createLog(message));
    }

    public static void w(String message){
        if (!isDebug) {
            return;
        }
        getMethodNames(new Throwable().getStackTrace());
        Log.w(className, createLog(message));
    }
}