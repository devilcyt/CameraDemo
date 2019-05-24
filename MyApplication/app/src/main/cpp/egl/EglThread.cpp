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
            LOGD("eglThread: begin ");
            if(eglThread->isCreate)
            {
                LOGD("egltThread: call create");
                eglThread->isCreate = false;
                eglThread->onCreate(eglThread->onCreateContext);
            }

            if(eglThread->isChange){
                LOGD("eglThread: call change");
                eglThread->isChange = false;
                eglThread->onChange(eglThread->onChangeContext, eglThread->surfaceWidth, eglThread->surfaceHeight);
                eglThread->isStart = true;
            }

            if(eglThread->isChangeFilter){
                LOGD("eglThread: call change filter");
                eglThread->isChangeFilter = false;
                eglThread->onChangeFilter(eglThread->onChangeFilterContext, eglThread->surfaceWidth, eglThread->surfaceHeight);
            }

            // call draw method
            if(eglThread->isStart){
                eglThread->onDraw(eglThread->onDrawContext);
                eglHelper->swapBuffer();
                LOGD("start swap buffer in Eglthread");
            }
            LOGD("eglThread: type = %d", eglThread->renderType);
            if(eglThread->renderType == OPENGL_RENDER_AUTO){

                // slepp thread
                LOGD("eglThread: OPENGL_RENDER_AUTO");
                usleep(1000000 / 60);
            }else if(eglThread->renderType == OPENGL_RENDER_HANDLE){
                LOGD("eglThread: OPENGL_RENDER_HANDLE　cond wait");
                pthread_mutex_lock(&eglThread->pthreadMutex); // 加锁
                pthread_cond_wait(&eglThread->pthreadCond, &eglThread->pthreadMutex); // 阻塞
                pthread_mutex_unlock(&eglThread->pthreadMutex); // 解锁
            }

            if(eglThread->isExit){ // 控制循环
                LOGD("eglThread: begin call exit");
                eglThread->onDestroy(eglThread->onDestroyContext); // 回调具体实现类的 destroy()方法
                //退出线程
                eglHelper->destoryEgl();
                //销毁 new出来的对象
                delete eglHelper;
                eglHelper = NULL;
                LOGD("eglThread: finish call exit");
                return 0;
            }
        }
    }
    LOGD("结束。、。。");
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
    notifyRender();
}


void EglThread::onSurfaceDestroy() {
    isExit = true;
    //退出线程
    notifyRender();
    pthread_join(eglThread, NULL); // 线程同步作用,等待线程执行完，再执行下面的语句
    aNativeWindow = NULL;
    eglThread = -1;
}


void EglThread::callBackOnCreate(EglThread::OnCreate onCreate1, void *context) {
    this->onCreate = onCreate1;
    this->onCreateContext = context;
}


void EglThread::callBackOnChange(EglThread::OnChange onChange1, void *context) {
    this->onChange = onChange1;
    this->onChangeContext = context;
}


void EglThread::callBackOnDraw(EglThread::OnDraw onDraw1, void *context) {
    this->onDraw = onDraw1;
    this->onDrawContext = context;
}

void EglThread::callBackOnChangeFIlter(EglThread::OnChangeFilter onChangeFilter1, void *context) {

    // 动态切换滤镜
    this->onChangeFilter = onChangeFilter1;
    this->onChangeFilterContext = context;
}

void EglThread::callBackOnDestroy(EglThread::OnDestroy onDestroy1, void *context) {
    this->onDestroy = onDestroy1;
    this->onDestroyContext = context;
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

void EglThread::onSurfaceChangeFilter() {
    LOGD("---------notify change filter")
    isChangeFilter = true;
    notifyRender();
}

