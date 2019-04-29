#include <jni.h>
#include <string>

#include "EglHelper.h"

#include "GLES2/gl2.h"
#include "android/native_window_jni.h"
#include "android/native_window.h"
#include "EglThread.h"


ANativeWindow *aNativeWindow = NULL;
EglThread *eglThread = NULL;


// 外部的回调方法实体
void Callback_SurfaceCreate(void *context){
    LOGD("Callback_SurfaceCreate");
    EglThread *eglThread1 = static_cast<EglThread *>(context);


}

void Callback_SurfaceChange(void *context, int width, int height){
    LOGD("Callback_SurfaceChange");
    EglThread *eglThread1 = static_cast<EglThread *>(context);

    glViewport(0,0,width,height); // 设置屏幕宽高
}

void Callback_SurfaceDraw(void *context){
    LOGD("Callback_SurfaceDraw");
    EglThread *eglThread1 = static_cast<EglThread *>(context);

    glClearColor(1.0f,1.0f,0,0); // 设置颜色
    glClear(GL_COLOR_BUFFER_BIT); //清空颜色缓冲 准备绘制

}



extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_openGlNative(JNIEnv *env, jobject instance, jobject surface) {

    // TODO

    eglThread = new EglThread();

    aNativeWindow = ANativeWindow_fromSurface(env, surface);

    eglThread->callBackOnCreate(Callback_SurfaceCreate, eglThread);
    eglThread->callBackOnChange(Callback_SurfaceChange, eglThread);
    eglThread->callBackOnDraw(Callback_SurfaceDraw, eglThread);

    eglThread->setRenderType(1);
    eglThread->onSurfaceCreate(aNativeWindow);

}extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_surfaceChange(JNIEnv *env, jobject instance, jint width, jint height) {

    // TODO
    if(eglThread != NULL){

        eglThread->onSurfaceChange(width, height);
        if(eglThread->renderType == OPENGL_RENDER_HANDLE){
            usleep(1000000);
            eglThread->notifyRender();
        }
    }

}