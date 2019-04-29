//
// Created by mecer on 19-4-23.
//

#include "EglThread.h"

EglThread::EglThread() {

    // 初始化线程锁
    pthread_mutex_init(&pthreadMutex, NULL);
    pthread_cond_init(&pthreadCond, NULL);


}


EglThread::~EglThread() {

    pthread_mutex_destroy(&pthreadMutex);
    pthread_cond_destroy(&pthreadCond);
}


void *EglThreadImpl(void *context){
    // 线程的回调函数,创建时作为参数传入pthread_create方法
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
                eglThread->onCreate(eglThread->onCreateContext);
            }

            if(eglThread->isChange){
                LOGD("eglThread: call change");
                eglThread->isChange = false;
                eglThread->onChange(eglThread->onChangeCOntext, eglThread->surfaceWidth, eglThread->surfaceHeight);
                eglThread->isStart = true;
            }

            // call draw method
            if(eglThread->isStart){
                eglThread->onDraw(eglThread->onDrawContext);
                eglHelper->swapBuffer();
            }

            if(eglThread->renderType == OPENGL_RENDER_AUTO){

                // slepp thread
                usleep(1000000 / 60);
            }else if(eglThread->renderType == OPENGL_RENDER_HANDLE){
                pthread_mutex_lock(&eglThread->pthreadMutex); // 加锁
                pthread_cond_wait(&eglThread->pthreadCond, &eglThread->pthreadMutex); // 阻塞
                pthread_mutex_unlock(&eglThread->pthreadMutex); // 解锁
            }

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
    isExit = true;
    isStart = false;
}


void EglThread::callBackOnCreate(EglThread::OnCreate onCreate1, void *context) {
    this->onCreate = onCreate1;
    this->onCreateContext = context;
}


void EglThread::callBackOnChange(EglThread::OnChange onChange1, void *context) {
    this->onChange = onChange1;
    this->onChangeCOntext = context;
}


void EglThread::callBackOnDraw(EglThread::OnDraw onDraw1, void *context) {
    this->onDraw = onDraw1;
    this->onDrawContext = context;
}

void EglThread::setRenderType(int renderType) {
    this->renderType = renderType;
}

void EglThread::notifyRender() {
        // 唤醒线程
        pthread_mutex_lock(&this->pthreadMutex);
        pthread_cond_signal(&this->pthreadCond);
        pthread_mutex_unlock(&this->pthreadMutex);
}

