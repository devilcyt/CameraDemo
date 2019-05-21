//
// Created by mecer on 19-4-23.
//

#ifndef MYAPPLICATION_EGLTHREAD_H
#define MYAPPLICATION_EGLTHREAD_H

#include <EGL/eglplatform.h>
#include "pthread.h"
#include "android/native_window.h"
#include "android/native_window_jni.h"
#include "EglHelper.h"
#include "unistd.h"

#define OPENGL_RENDER_AUTO 1
#define OPENGL_RENDER_HANDLE 2

class EglThread {

public:
    pthread_t eglThread = -1;
    ANativeWindow *aNativeWindow = NULL;

    bool isCreate = false;
    bool isChange = false;
    bool isExit = false;
    bool isStart = false;
    bool isChangeFilter = false;

    int surfaceWidth;
    int surfaceHeight;

    typedef void(*OnCreate)(void *);  // 回调函数
    OnCreate onCreate;
    void *onCreateContext;

    typedef void(*OnChange)(void *, int width, int height); // 回调函数
    OnChange onChange;
    void *onChangeContext;

    typedef void(*OnDraw)(void *); // 回调函数
    OnDraw onDraw;
    void *onDrawContext;

    typedef void(*OnChangeFilter)(void *, int width, int height);
    OnChangeFilter onChangeFilter;
    void *onChangeFilterContext;

    typedef void(*OnDestroy)(void *);
    OnDestroy onDestroy;
    void *onDestroyContext;


    // 手动 自动模式
    int renderType = OPENGL_RENDER_AUTO;
    pthread_mutex_t pthreadMutex; // 锁变量
    pthread_cond_t pthreadCond; // 条件变量


public:
    EglThread();
    ~EglThread();

    void onSurfaceCreate(EGLNativeWindowType window);
    void onSurfaceChange(int, int);
    void onSurfaceDestroy();
    void onSurfaceChangeFilter();

    void callBackOnCreate(OnCreate onCreate1, void *context);
    void callBackOnChange(OnChange onChange1, void *context);
    void callBackOnDraw(OnDraw onDraw1, void *context);
    void callBackOnChangeFIlter(OnChangeFilter onChangeFilter1, void *context);
    void callBackOnDestroy(OnDestroy onDestroy1, void *context);


    void setRenderType(int);
    void notifyRender();

};


#endif //MYAPPLICATION_EGLTHREAD_H
