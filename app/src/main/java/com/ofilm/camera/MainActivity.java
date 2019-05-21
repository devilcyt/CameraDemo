package com.ofilm.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.ofilm.cameraview.CameraView;
import com.ofilm.utils.AspectRatio;
import com.ofilm.utils.AspectRatioDialog;
import com.ofilm.utils.ConfirmDialogUtil;
import com.ofilm.utils.Constants;
import com.ofilm.utils.LogUtil;
import com.ofilm.utils.SystemReflectionProxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback,
        AspectRatioDialog.Listener{

    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    private static final int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };

    private int mCurrentFlash;

    private CameraView mCameraView;
    private Handler mPictureTakenHandler = null; // 子线程，用于后台保存拍照图片


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = findViewById(R.id.camera_view);
        // 设置回调
        if(mCameraView != null){
            mCameraView.addCallback(mCameraCallback);
        }

        // 初始化组件
        FloatingActionButton fab = findViewById(R.id.take_picture);
        if(fab != null){
            fab.setOnClickListener(this);
        }

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayShowTitleEnabled(false); // 去掉actionbar title 显示
        }
        LogUtil.d("onCreate(): finish .");
    }


    @Override
    protected void onResume() {
        super.onResume();
        // 先检查权限
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)){
            LogUtil.d("onResume(): no should show permission dialog ?");
            // 当用户点击权限申请提示框的“禁止后不再询问弹出”时，再次进入应用走此流程
            ConfirmDialogUtil.newInstance(R.string.confirm_camera_permission,
                    new String[]{Manifest.permission.CAMERA},
                    1,
                    R.string.permission_not_granted)
                    .show(getSupportFragmentManager(), "dialog");
        }else if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED){

            if(mCameraView != null){
                LogUtil.d("onResume(): already has permission. start .");
                mCameraView.start(); // 开启相机
            }

        }else{
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 1);
        }
        LogUtil.d("onResume: current ratio is : " + mCameraView.getAspectRatio());

    }


    @Override
    protected void onPause() {
        if(mCameraView != null){
            mCameraView.stop();
        }
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mPictureTakenHandler != null){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
                // SDK >= API 18
                mPictureTakenHandler.getLooper().quitSafely(); // 释放
            }else{
                mPictureTakenHandler.getLooper().quit();
            }
            mPictureTakenHandler = null;
        }
        if(mCameraView != null){
            mCameraView.stop();
            mCameraView = null;
        }
    }

    /**
     *  申请权限结果回调
     *
     * */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch(requestCode){
            case 1 :{ // requestCode 和之前申请时传入的要一致
                if(permissions.length != 1 || grantResults.length != 1){
                    throw new RuntimeException("请求相机权限时发生错误");
                }
                if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, R.string.permission_not_granted, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }


    /**
    *   加载顶部菜单布局
    *
    * */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menus, menu);

        return true;
    }


    private void updateUi(MenuItem item){
        if(SystemReflectionProxy.getInt(Constants.PROP_RECTIFY, 3) == 3 ){
            item.setTitle(R.string.rectify_on);
        }else{
            item.setTitle(R.string.rectify_off);
        }
    }

    /**
    *  顶部菜单按钮设置监听事件
    *
    * */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()){
                case R.id.aspect_ratio:{
                    // 切换预览比例
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    if(mCameraView != null
                            && fragmentManager.findFragmentByTag("dialog") == null){
                        final Set<AspectRatio> ratio = mCameraView.getSupportedAspectRatios();
                        final AspectRatio currentRatio = mCameraView.getAspectRatio();
                        LogUtil.d("ssss : " + mCameraView.getAspectRatio());
                        AspectRatioDialog.newInstance(ratio, currentRatio).show(fragmentManager, "dialog");
                    }
                    return true;
                }

                case R.id.switch_flash:{
                    // 切换闪光灯模式
                    if(mCameraView != null){
                        mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                        item.setTitle(FLASH_TITLES[mCurrentFlash]);
                        item.setIcon(FLASH_ICONS[mCurrentFlash]);
                        mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
                    }
                    return true;
                }

                case R.id.switch_camera:{
                    // 切换前后摄
                    if(mCameraView != null){
                        int facing = mCameraView.getFacing();
                        mCameraView.setFacing(facing == CameraView.FACING_FRONT ? CameraView.FACING_BACK : CameraView.FACING_FRONT);
                    }
                    return true;
                }

            }
            return super.onOptionsItemSelected(item);
    }





    /**
     *  保存图片线程
     *
    * */
    private Handler getPictureTakenHandler(){
        // 在回调函数 onPictureTaken 中调用
        if(mPictureTakenHandler == null){
            HandlerThread thread = new HandlerThread("savepicture"); // 开启一个线程，传入一个别名
            thread.start();
            mPictureTakenHandler = new Handler(thread.getLooper()); // 传入线程的消息池，handler从这里循环读取消息
        }

        return mPictureTakenHandler;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case R.id.take_picture:{
                // 拍照
                mCameraView.takePicture();
                break;
            }


        }
    }


    @Override
    public void onAspectRatioSelected(@NonNull AspectRatio ratio) {
        if(mCameraView != null){
            LogUtil.d("set a new ratio .");
            mCameraView.setAspectRatio(ratio);
        }
    }


    private CameraView.Callback mCameraCallback = new CameraView.Callback() {
        @Override
        public void onCameraOpened(CameraView cameraView) {
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
        }


        /**
         *  将图片保存到本地
         *
         * */
        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            LogUtil.d("onPictureTaken : " + data.length);
            Toast.makeText(cameraView.getContext(), R.string.take_picture, Toast.LENGTH_SHORT).show();
            getPictureTakenHandler().post(new Runnable() {
                @Override
                public void run() {
                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "picture.jpg");
                    OutputStream os = null;
                    try{
                        os = new FileOutputStream(file);
                        os.write(data);
                        os.close();
                    }catch (IOException e){
                        LogUtil.d("can not write to " + file + e);
                    }finally {
                        if(os != null){
                            try{
                                os.close();
                            }catch (IOException e){
                                // do nothing
                            }
                        }
                    }
                }
            });
        }
    };
}
