package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.nio.ByteBuffer;

import opengl.NativeOpengl;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private Button triangle_btn;
    private Button quradrilateral_btn;
    private Button imgdraw_btn;

    private NativeOpengl nativeOpengl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nativeOpengl = new NativeOpengl();

        triangle_btn = findViewById(R.id.triangle);
        quradrilateral_btn = findViewById(R.id.quadrilateral);
        imgdraw_btn = findViewById(R.id.imgdata);

        triangle_btn.setOnClickListener(this);
        quradrilateral_btn.setOnClickListener(this);
        imgdraw_btn.setOnClickListener(this);


        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.pciture);
        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getHeight() * bitmap.getWidth() * 4);
        bitmap.copyPixelsFromBuffer(buffer);
        buffer.flip();
        byte[] imgData = buffer.array();
        nativeOpengl.imgData(bitmap.getWidth(), bitmap.getHeight(), imgData.length, imgData);

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
                Log.i("swq","=== dra image");
                nativeOpengl.createTriangle(5);
                break;
            }
        }
    }
}
