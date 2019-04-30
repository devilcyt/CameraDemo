#include <jni.h>
#include <string>

#include "egl/EglHelper.h"

#include "GLES2/gl2.h"
#include "android/native_window_jni.h"
#include "android/native_window.h"
#include "egl/EglThread.h"
#include "shaderutil/ShaderUtils.h"


ANativeWindow *aNativeWindow = NULL;
EglThread *eglThread = NULL;



// 三角形1顶点数组
float vertex1[] = {

    /*-1,0,
    0,1,    菱形图形坐标
    0,-1,
    1,0*/

    -1,1,
    1,1,
    -1,-1,
    1,-1

};


// 纹理坐标，与顶点坐标一一对应。
float fragment1[] = {

        0,0,
        1,0,
        0,1,
        1,1
};

// vertex code

const char *vertex = "attribute vec4 v_Position; \n"
                     "attribute vec2 f_Position; \n"
                     "varying vec2 ft_Position; \n"
                     "\n"
                     "void main(){ \n"
                     "  ft_Position = f_Position; \n"
                     "  gl_Position = v_Position; \n"
                     "}";

// fragment code
const char *fragment = "precision mediump float; \n"
                       "varying vec2 ft_Position; \n"
                       "uniform sampler2D sTexture; \n"
                       "\n"
                       "void main(){ \n"
                       "    gl_FragColor = texture2D(sTexture, ft_Position); \n"  // Rrd Green Blue Alpha
                       "}";



int program;
GLint vPosition;  // 表示顶点变量，通过它来设置绘制前的坐标参数
GLint fPosition; // 纹理坐标返回值
GLint vTexture; // 2D纹理
GLuint textureId; // 纹理ID

int width;
int height;
void *pixels = NULL; // 像素值

int draNum;


// egl线程 -- 外部的回调方法实体
/*
 *  Surface ： 创建
 */
void Callback_SurfaceCreate(void *context){
    LOGD("Callback_SurfaceCreate");
    EglThread *eglThread1 = static_cast<EglThread *>(context);


    // 获得渲染程序
    program = CreateProgram(vertex, fragment);
    LOGD("create program & program = %d", program);
    if(program > 0){
        // 根据变量名得到顶点变量 attribute, 也就是code中定义的 v_Position
        vPosition = glGetAttribLocation(program, "v_Position"); // 返回 v_Position 的下标
        fPosition = glGetAttribLocation(program, "f_Position"); // 返回 f_Position 的下标
        vTexture = glGetUniformLocation(program, "sTexture"); // 返回2D纹理的句柄
        LOGD("vPosition = %d , fPosition = %d", vPosition, fPosition);

        // 绑定2D纹理
        glGenTextures(1, &textureId); // 生成一个纹理ID，赋值给textureId, 每创建一个纹理都需要绑定一个id

        glBindTexture(GL_TEXTURE_2D, textureId);

        // 设置过滤 和 环绕 方式, 创建纹理必须配置的
        // 环绕 GL_REPEAT --> 重复模式
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT); // s --> x 对应关系
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT); // t --> y 对应关系

        //过滤    GL_LINEAR --> 放大丶缩小时填充像素的模式
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR); // 设置最小, linear 线性过滤
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); // 设置最大, 同上


        if(pixels != NULL){
            LOGD("pixels is not null");
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels); // 传入图片资源 byte类型数组
        }

        glBindTexture(GL_TEXTURE_2D, 0);  // 解绑
    }





}

/*
 *   Surface ： 宽高 旋转 等设置
 */
void Callback_SurfaceChange(void *context, int width, int height){
    LOGD("Callback_SurfaceChange");
    EglThread *eglThread1 = static_cast<EglThread *>(context);

    glViewport(0,0,width,height); // 设置屏幕宽高
}


/*
 *  Surface ： 绘制方法
 */
void Callback_SurfaceDraw(void *context){
    LOGD("Callback_SurfaceDraw");
    EglThread *eglThread1 = static_cast<EglThread *>(context);

    glClearColor(1.0f,1.0f,1.0f,1.0f); // 设置清屏颜色为白色
    glClear(GL_COLOR_BUFFER_BIT); //清空颜色缓冲 准备绘制


    // 绘制三角形
    glUseProgram(program);  // 使用 program 不声明后面属性参数设置都无效
    glGenTextures(1, &textureId);
    /*
     *  参数说明:
     *  GLuint index : 顶点坐标变量下标
     *  GLint size : 每个顶点包含几个坐标 es: (x, y) 2 , (x, y, z) 3 ....
     *  GLenum type : 顶点数据类型 也就是 vertex1 vertex2 的类型
     *  GLboolean normalized : 是否规划，例如当手机是720 * 1080 时，需要转换成规划顶点坐标 （-1 , 1） 这样的，就设置为true
     *                          此处我们直接设置了规划坐标，设置为false
     *  GLsizei stride : 跨度，指跨越多少个字节对应一个坐标 8 意思跨越8个字节，然后增加或减少一个坐标
     *  const void *pointer : 点集合指针
     */
    //glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 8, vertex1);


    /*
     *  参数说明:
     *  GLenum mode : 绘制模式, GL_TRIANGLES 表示绘制三角形
     *  GLint first : 绘制起点, 0 表示数组的下标即第一个点
     *  GLsizei count : 绘制点数量, 3 表示总共绘制3个点
     */
    //glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

     switch(draNum){
     case 3:{
         LOGD("native triangle");
         glEnableVertexAttribArray(vPosition); // 声明顶点坐标可用
         glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 8, vertex1);
         glDrawArrays(GL_TRIANGLE_STRIP, 0, 3);
         break;
     }

     case 4:{
         LOGD("native quadrilateral");
         glEnableVertexAttribArray(vPosition); // 声明顶点坐标可用
         glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 8, vertex1);
         glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
         break;

     }
     case 5:{
         LOGD("native draw picture");
         glBindTexture(GL_TEXTURE_2D, textureId);
         glEnableVertexAttribArray(vPosition); // 声明顶点坐标可用
         glEnableVertexAttribArray(fPosition); // 声明纹理坐标可用
         glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 8, vertex1);
         glVertexAttribPointer(fPosition, 2, GL_FLOAT, false, 8, fragment1);
         glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
         glBindTexture(GL_TEXTURE_2D, 0);
         break;
     }
 }
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

eglThread->setRenderType(2);
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

extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_createTriangle(JNIEnv *env, jobject instance, jint num) {

    draNum = num;
    eglThread->notifyRender();

}


extern "C"
JNIEXPORT void JNICALL
Java_opengl_NativeOpengl_imgData(JNIEnv *env, jobject instance, jint w, jint h, jint length,
                                 jbyteArray data_) {
    jbyte *data = env->GetByteArrayElements(data_, NULL);

    // data.length 用于计算需要分配的内存空间

    width = w;  // 图片的宽
    height = h; // 图片的高
    pixels = malloc(length); // 根据lenth个pixels分配内存空间
    memcpy(pixels, data, length); // 将data数据拷贝到pixels，拷贝长度为lenth。


    env->ReleaseByteArrayElements(data_, data, 0); // 释放data, 所以赋值要在这之前执行
}