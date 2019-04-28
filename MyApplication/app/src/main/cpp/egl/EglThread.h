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



class EglThread {

public:
    pthread_t eglThread = -1;
    ANativeWindow *aNativeWindow = NULL;

    bool isCreate = false;
    bool isChange = false;
    bool isExit = false;

    int surfaceWidth;
    int surfaceHeight;


public:
    EglThread();
    ~EglThread();

    void onSurfaceCreate(EGLNativeWindowType window);
    void onSurfaceChange(int, int);
    void onSurfaceDestroy();



};


#endif //MYAPPLICATION_EGLTHREAD_H
