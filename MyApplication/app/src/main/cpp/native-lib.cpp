#include <jni.h>
#include <string>

#include "opengl/OpenglController.h"



OpenglController *openglController = NULL;


extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_openGlNative(JNIEnv *env, jobject instance, jobject surface) {

    if(openglController == NULL){
       openglController = new OpenglController();
    }
    openglController->onCreateSurface(env,surface);

}

extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_surfaceChange(JNIEnv *env, jobject instance, jint width, jint height) {

    if(openglController != NULL){
        openglController->onChangeSurface(width, height);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_createTriangle(JNIEnv *env, jobject instance, jint num) {
    if(openglController != NULL){
        openglController->setDrawType(num);
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_setimgData(JNIEnv *env, jobject instance, jint w, jint h, jint length,
                                 jbyteArray data_) {
    jbyte *data = env->GetByteArrayElements(data_, NULL);

    if(openglController != NULL){
        openglController->setImageData(w, h, length, data);
    }

    env->ReleaseByteArrayElements(data_, data, 0); // 释放data, 所以赋值要在这之前执行
}

extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_surfaceDestroy(JNIEnv *env, jobject instance) {

    if(openglController != NULL){
        openglController->onDestroySurface();
        delete openglController;
        openglController = NULL;
    }


}extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_surfaceChangeFilter(JNIEnv *env, jobject instance) {

    if(openglController != NULL){
        // 切换滤镜
            openglController->onChangeFilter();
    }

}