#include <jni.h>
#include <string>

#include "EglHelper.h"

#include "GLES2/gl2.h"
#include "android/native_window_jni.h"
#include "android/native_window.h"

EglHelper *mEglHelper= NULL;
ANativeWindow *aNativeWindow = NULL;

extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_openGlNative(JNIEnv *env, jobject instance, jobject surface) {

    // TODO


    mEglHelper = new EglHelper();
    aNativeWindow = ANativeWindow_fromSurface(env, surface);

    mEglHelper->initEgl(aNativeWindow);


    //opengl
    glViewport(0,0,1080,1920);
    glClearColor(0,0,1.0f,1.0f);
    glClear(GL_COLOR_BUFFER_BIT);

    mEglHelper->swapBuffer();


}