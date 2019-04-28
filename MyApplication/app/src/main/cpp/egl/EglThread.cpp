//
// Created by mecer on 19-4-23.
//

#include "EglThread.h"

EglThread::EglThread() {

}

EglThread::~EglThread() {

}

void *EglThreadImpl(void *context){
    // 线程的回调函数,创建后会进行回调
    EglThread *eglThread = static_cast<EglThread *>(context);
    if(eglThread != NULL)
    {
        EglHelper *eglHelper = new EglHelper();
        eglHelper ->initEgl(eglThread->aNativeWindow);
        eglThread->isExit = false;
        while (true) //循环绘制方法，保证surface创建OK,且部分方法走一次就OK
        {
            if(eglThread->isCreate)
            {
                LOGD("egltThread: call create");

                eglThread->isCreate = false;
            }

            if(eglThread->isChange){
                LOGD("eglThread: call change");
                eglThread->isChange = false;
                glViewport(0,0,1080,1920); // 清屏 设置屏幕大小 surfaceWidth surfaceHeight
            }

            // call draw method
            glClearColor(0,0,1.0f,1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            eglHelper->swapBuffer();

            // slepp thread
            usleep(1000000 / 60);

            if(eglThread->isExit){ // 控制循环
                LOGD("eglThread: call exit");
                break;
            }
        }
    }

}

void EglThread::onSurfaceCreate(EGLNativeWindowType window) {

    if(eglThread == -1){
        isCreate = true;
        aNativeWindow = window;

        //创建 egl 子线程
        pthread_create(&eglThread, NULL, EglThreadImpl, this);
    }

}

void EglThread::onSurfaceChange(int width, int height) {
    isChange = true;
    surfaceWidth = width;
    surfaceHeight = height;

}

void EglThread::onSurfaceDestroy() {

}
