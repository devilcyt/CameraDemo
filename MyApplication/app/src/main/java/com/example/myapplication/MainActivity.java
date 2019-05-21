package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import opengl.NativeOpengl;
import opengl.OpenglSurface;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private Button triangle_btn;
    private Button quradrilateral_btn;
    private Button imgdraw_btn;
    private Button filter_btn;

    private NativeOpengl nativeOpengl;
    private OpenglSurface openglSurface;

    private List<Integer> imgList = new ArrayList<>();

    private byte[] imgData;

    private int imageIds[] = {
            R.drawable.pciture,
            R.drawable.png_1,
            R.drawable.png_2,
            R.drawable.png_3
    };

    private int index = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("swq","111111111");
        nativeOpengl = new NativeOpengl();
        openglSurface = findViewById(R.id.surface);
        triangle_btn = findViewById(R.id.triangle);
        quradrilateral_btn = findViewById(R.id.quadrilateral);
        imgdraw_btn = findViewById(R.id.imgdata);
        filter_btn = findViewById(R.id.changefiter);

        triangle_btn.setOnClickListener(this);
        quradrilateral_btn.setOnClickListener(this);
        imgdraw_btn.setOnClickListener(this);
        filter_btn.setOnClickListener(this);

        setImageList(); // 添加图片
        Log.i("swq","2222222");
        openglSurface.setOnSurfaceListener(new OpenglSurface.OnSurfaceListener() {
            @Override
            public void init() {
                Log.i("swq","3333333");
                //setImageToNative();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.triangle:{
                nativeOpengl.createTriangle(3);
                Log.i("swq","=== triangle");
                break;
            }

            case R.id.quadrilateral:{
                Log.i("swq","=== quadrilateral");
                nativeOpengl.createTriangle(4);
                break;
            }

            case R.id.imgdata:{
                Log.i("swq","=== draw image");
                nativeOpengl.createTriangle(5);
                setImageToNative();
                break;
            }

            case R.id.changefiter:{
                Log.i("swq", "=== change filter");
                nativeOpengl.surfaceChangeFilter();
                nativeOpengl.createTriangle(5);
                break;
            }
        }
    }


    private void setImageList(){
        for(int i = 0; i < imageIds.length; i++){
            imgList.add(imageIds[i]);
        }
    }

    private int getImageIds(){
        index ++;
        if(index >= imgList.size()){
            index = 0; // 回到第一张
        }
        return imgList.get(index);
    }

    private void setImageToNative(){
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), getImageIds());
        Log.i("==== index =  ", String.valueOf(index));
        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getHeight() * bitmap.getWidth() * 4);
        bitmap.copyPixelsToBuffer(buffer);
        buffer.flip();
        imgData = buffer.array();
        nativeOpengl.setimgData(bitmap.getWidth(), bitmap.getHeight(), imgData.length, imgData);
    }
}
