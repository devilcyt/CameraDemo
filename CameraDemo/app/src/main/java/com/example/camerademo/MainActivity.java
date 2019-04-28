package com.example.camerademo;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.example.camerademo.fragments.Camera1Fragment;
import com.example.camerademo.fragments.Camera2Fragment;


/**
 *
 * 应用入口
 * author: mecer
 */

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        try {
            fragmentTransaction.replace(R.id.container, Camera1Fragment.newInstance(),"camera_fragment");
            //fragmentTransaction.addToBackStack("camera_fragment");
            fragmentTransaction.commit();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
