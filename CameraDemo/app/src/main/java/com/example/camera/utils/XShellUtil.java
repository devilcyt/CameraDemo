package com.example.camera.utils;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
/**
 *
 * java 运行 终端命令
 * author: mecer
 */

public class XShellUtil {

    ProcessBuilder processBuilder;
    Process process;
//,"adb","remount","adb","shell","setenforce","0"
    //private String[] commands = {"adb","root"};
    public static XShellUtil newInstance(){
        return new XShellUtil();
    }

    public String doCommand(){

        processBuilder = new ProcessBuilder();
        processBuilder.command("");
        processBuilder.redirectErrorStream(true);
        try {
            process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (true) {
                String line = reader.readLine();
                LogUtil.i(line);
                if (line == null) {
                    break;
                }
                return "exc commands successed";
            }
            process.destroy();
            reader.close();
        }catch (IOException e){
            e.printStackTrace();
            LogUtil.i("Exception ac~~~");
        }
        return "exc commands failed";
    }
}
