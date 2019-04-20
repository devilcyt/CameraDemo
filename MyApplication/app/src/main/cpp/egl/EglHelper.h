//
// Created by mecer on 19-4-14.
//

#ifndef MYAPPLICATION_EGLHELPER_H
#define MYAPPLICATION_EGLHELPER_H

#include "../log/opengllog.h"
#include "EGL/egl.h"
#include "GLES2/gl2.h"


class EglHelper {

public:
    EGLDisplay mEglDisplay;
    EGLSurface mEglSurface;
    EGLContext mEglContext;
    EGLConfig mEglConfig;


public:
    EglHelper();
    ~EglHelper();

    int initEgl(EGLNativeWindowType win);

    int swapBuffer();

    void destoryEgl();


};


#endif //MYAPPLICATION_EGLHELPER_H
