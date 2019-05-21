//
// Created by mecer on 19-5-13.
//

#include "OpenglController.h"


// ------------------- 生命周期的封装 begin --------------------//
void Callback_SurfaceCreate(void *context){

    // 调用具体实现类的方法
    OpenglController *openglController = static_cast<OpenglController *>(context);

    if(openglController != NULL){
        if(openglController->openglBase != NULL){
            openglController->openglBase->onCreate();
        }
    }
}

void Callback_SurfaceChange(void *context, int width, int height){

    // 调用具体实现类的方法
    OpenglController *openglController = static_cast<OpenglController *>(context);

    if(openglController != NULL){
        if(openglController->openglBase != NULL){
            openglController->openglBase->onChange(width,height);
        }
    }

}

void Callback_SurfaceDraw(void *context){

    // 调用具体实现类的方法
    OpenglController *openglController = static_cast<OpenglController *>(context);

    if(openglController !=NULL){
        if(openglController->openglBase != NULL){
            openglController->openglBase->draw();
        }
    }
}

void Callback_SurfaceChangeFilter(void *context, int width, int height){

    // 调用具体实现类的方法
    OpenglController *openglController = static_cast<OpenglController *>(context);

    LOGD("----- %d", openglController == NULL ? 1 : 0);
    LOGD("===== %d", openglController->openglBase == NULL ? 1 : 0);

    LOGD("++++++ width, height = (%d, %d) ------picture_width, picture_height = (%d ,%d)", width,height,
            openglController->picture_width, openglController->picture_height);

    if(openglController != NULL){
        LOGD("begin change filter function");
        if(openglController->openglBase != NULL){
            // 回收原来的资源
            openglController->DestroyBase(openglController);
        }

        if ( openglController->isEnable ) {
            openglController->isEnable = false;
            openglController->openglBase = new OpenglFilterOne();
            openglController->openglBase->onCreate();
            openglController->openglBase->setImageByte(openglController->picture_width,
                                                       openglController->picture_height,
                                                       openglController->pixels);
            openglController->openglBase->onChange(width,height);
            openglController->eglThread->notifyRender();

        } else {
            openglController->isEnable = true;
            // 切换新的滤镜 two
            openglController->openglBase = new OpenglFilterTwo();
            openglController->openglBase->onCreate();
            // 先传入图片宽高，在onSurfaceChange中会再传入屏幕宽高.
            openglController->openglBase->setImageByte(openglController->picture_width,
                                                       openglController->picture_height,
                                                       openglController->pixels);
            openglController->openglBase->onChange(width, height);

            openglController->eglThread->notifyRender();  // 通知线程draw
        }
    }
}

void Callback_SurfaceDestroy(void *context){

    // 调用具体实现类的方法
    OpenglController *openglController = static_cast<OpenglController *>(context);

    if(openglController != NULL){
        if(openglController->openglBase != NULL){
            openglController->openglBase->onDestroy();
        }
    }

}
// ------------------- 生命周期的封装 end --------------------//


OpenglController::OpenglController() {


}

OpenglController::~OpenglController() {

}

void OpenglController::onCreateSurface(JNIEnv *env, jobject surface) {

    // 初始化egl线程和native window

    eglThread = new EglThread();
    aNativeWindow = ANativeWindow_fromSurface(env, surface);
    eglThread->callBackOnCreate(Callback_SurfaceCreate, this);
    eglThread->callBackOnChange(Callback_SurfaceChange, this);
    eglThread->callBackOnDraw(Callback_SurfaceDraw, this);
    eglThread->callBackOnChangeFIlter(Callback_SurfaceChangeFilter, this);
    eglThread->callBackOnDestroy(Callback_SurfaceDestroy, this);
    eglThread->setRenderType(2); // 设置手动渲染或者自动渲染模式
    openglBase = new OpenglFilterOne(); // 指向基类的子类指向一个具体实现类
    eglThread->onSurfaceCreate(aNativeWindow);

}

void OpenglController::onChangeSurface(int width, int height) {

    if(eglThread != NULL){
        if(openglBase != NULL){
            openglBase->surface_width = width;
            openglBase->surface_height = height; //  为了从后台回到前台可以正常绘制, 所以要保留一下宽高数据
        }
        eglThread->onSurfaceChange(width,height);
        if(eglThread->renderType == OPENGL_RENDER_HANDLE){
            usleep(1000000);
            eglThread->notifyRender();
        }
    }

}

void OpenglController::onDestroySurface() {

    if(eglThread != NULL){
        LOGD("OpenglController: destroy egl thread");
        eglThread->onSurfaceDestroy();
    }

    if(openglBase != NULL){
        LOGD("OpenglController: destroy openglBase");
        delete openglBase;
        openglBase = NULL;
    }

    if(aNativeWindow != NULL){
        LOGD("OpenglController: destroy aNativeWindow");
        ANativeWindow_release(aNativeWindow);
        aNativeWindow = NULL;
    }
}

void OpenglController::setImageData(jint pic_width, jint pic_height, jint length, void *data) {
    // data.length 用于计算需要分配的内存空间
    LOGD("Native set image data");
    LOGD("picture_width : %d,  picture_height : %d", pic_width, pic_height);
    picture_width = pic_width;  // 图片的宽
    picture_height = pic_height; // 图片的高
    if(pixels != NULL){
        free(pixels);
        pixels = NULL;
    }
    pixels = malloc(length); // 根据lenth个pixels分配内存空间
    memcpy(pixels, data, length); // 将data数据拷贝到pixels，拷贝长度为lenth。

    if(openglBase != NULL){
        openglBase->setImageByte(picture_width, picture_height, pixels);
    }
    if(eglThread != NULL){
        eglThread->notifyRender();
    }

}

void OpenglController::setDrawType(int num) {
   // draNum = num;
    if(openglBase != NULL){
        openglBase->setDrawType(num);
    }
}

void OpenglController::onChangeFilter() {
    if(eglThread != NULL){
        eglThread->onSurfaceChangeFilter();
    }
}

void OpenglController::DestroyBase(OpenglController *openglController) {

    openglController->openglBase->onDestroy();
    delete openglController->openglBase;
    openglController->openglBase = NULL;

}
