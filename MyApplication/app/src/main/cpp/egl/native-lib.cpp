#include <jni.h>
#include <string>

#include "EglHelper.h"

#include "GLES2/gl2.h"
#include "android/native_window_jni.h"
#include "android/native_window.h"
#include "EglThread.h"


ANativeWindow *aNativeWindow = NULL;
EglThread *eglThread = NULL;


extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_openGlNative(JNIEnv *env, jobject instance, jobject surface) {

    // TODO


    eglThread = new EglThread();

    aNativeWindow = ANativeWindow_fromSurface(env, surface);

    eglThread->onSurfaceCreate(aNativeWindow);


}extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_surfaceChange(JNIEnv *env, jobject instance, jint width, jint height) {

    // TODO
    eglThread->onSurfaceChange(width, height);

}