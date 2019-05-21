//
// Created by mecer on 19-5-13. opengl 操作类
//

#ifndef MYAPPLICATION_OPENGL_H
#define MYAPPLICATION_OPENGL_H


#include "../egl/EglThread.h"
#include "android/native_window.h"
#include "android/native_window_jni.h"
#include "OpenglBase.h"
#include "OpenglFilterOne.h"
#include "OpenglFilterTwo.h"
#include "../log/opengllog.h"

class OpenglController {

public:
    EglThread *eglThread = NULL;
    ANativeWindow *aNativeWindow = NULL;
    OpenglBase *openglBase = NULL;

    //int draNum;

    // Begin 作为中转站，目的是传给 OpenglBase 的子类使用
    int picture_width;
    int picture_height;
    void *pixels = NULL; // 像素值 即 纹理
    // End 作为中转站，目的是传给 OpenglBase 的子类使用

    bool isEnable = false; // 判断滤镜是否生效

public:
    OpenglController();
    ~OpenglController();

    void onCreateSurface(JNIEnv *env, jobject surface);

    void onChangeSurface(int width, int height);

    void onDestroySurface();

    void setImageData(jint w, jint h, jint length,
                      void *data);

    void setDrawType(int num);

    void onChangeFilter();

    void DestroyBase(OpenglController *openglController);


};


#endif //MYAPPLICATION_OPENGL_H
