//
// Created by mecer on 19-4-14.
//

#include "EglHelper.h"

EglHelper::EglHelper() {

    mEglDisplay = EGL_NO_DISPLAY;
    mEglSurface = EGL_NO_SURFACE;
    mEglContext = EGL_NO_CONTEXT;
    mEglConfig = NULL;
}

EglHelper::~EglHelper() {

}

int EglHelper::initEgl(EGLNativeWindowType window) {

    // 1. 得到默认显示窗口
    mEglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if(mEglDisplay == EGL_NO_DISPLAY){
        LOGD("eglGetDisplay error");
        return -1;
    }

    // 2. 初始化默认显示设备
    EGLint *version = new EGLint[2];
    if(!eglInitialize(mEglDisplay, &version[0], &version[1])){

        LOGD("eglInitialize error");
        return -1;
    }

    // 3. 设置默认显示设备的属性
    const EGLint attribs[] = {
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 8,
            EGL_STENCIL_SIZE, 8,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_NONE
    };

    EGLint num_config;
    if(!eglChooseConfig(mEglDisplay, attribs, NULL, 1, &num_config)){

        return -1;
    }

    // 4. 从系统中获取对应属性的配置
    if(!eglChooseConfig(mEglDisplay, attribs, &mEglConfig, num_config, &num_config)){

        return -1;
    }

    // 5. 创建egl上下文
    const EGLint attr_list[] = {
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL_NONE
    };
    mEglContext = eglCreateContext(mEglDisplay, mEglConfig, EGL_NO_CONTEXT, attr_list);
    if(mEglContext == EGL_NO_CONTEXT){

        return -1;
    }

    // 6.
    mEglSurface = eglCreateWindowSurface(mEglDisplay, mEglConfig, window, NULL);
    if(mEglSurface == EGL_NO_SURFACE){

        return -1;
    }

    // 7. 渲染，刷新数据
    if(!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)){

        return  -1;
    }

    LOGD("success !!! ");
    return 0;
}

int EglHelper::swapBuffer() {

    if(mEglDisplay != EGL_NO_DISPLAY && mEglSurface != EGL_NO_SURFACE){
        if(eglSwapBuffers(mEglDisplay, mEglSurface)){ // 将后台surface的数据渲染到前台显示的窗口
            return 0;
        }
    }
    return -1;
}

// 8. 销毁 释放
void EglHelper::destoryEgl() {

    if(mEglDisplay != EGL_NO_DISPLAY){
        eglMakeCurrent(mEglDisplay, EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
    }

    if(mEglDisplay != EGL_NO_DISPLAY && mEglSurface != EGL_NO_SURFACE){
        eglDestroySurface(mEglDisplay, mEglSurface);
        mEglSurface = EGL_NO_SURFACE;
    }

    if(mEglDisplay != EGL_NO_DISPLAY && mEglContext != EGL_NO_CONTEXT){
        eglDestroyContext(mEglDisplay, mEglContext);
        mEglContext = EGL_NO_CONTEXT;
    }

    if(mEglDisplay != EGL_NO_DISPLAY){
        eglTerminate(mEglDisplay);
        mEglDisplay = EGL_NO_DISPLAY;
    }
}
