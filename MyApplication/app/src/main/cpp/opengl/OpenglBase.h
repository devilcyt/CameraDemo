//
// Created by mecer on 19-5-13.  基础类
//

#ifndef MYAPPLICATION_OPENGLBASE_H
#define MYAPPLICATION_OPENGLBASE_H

#include "GLES2/gl2.h"
#include "EGL/egl.h"
#include "android/native_window.h"
#include "android/native_window_jni.h"
#include "../log/opengllog.h"

#include <cstring>
#include "jni.h"

class OpenglBase {

public:

    char *vertex = NULL;
    char *fragment = NULL;

    int surface_width;
    int surface_height;


    float *vertexs = NULL;
    float *fragments = NULL;

    int drawType;

    GLuint program;

    GLuint vShader;
    GLuint fShader;

public:
    OpenglBase();
    ~OpenglBase();

    virtual void onCreate();

    virtual void onChange(int width, int height);

    virtual void draw();

    virtual void onDestroy();

    virtual void setImageByte(jint w, jint h, void *data);

    virtual void setDrawType(int num);

};


#endif //MYAPPLICATION_OPENGLBASE_H
