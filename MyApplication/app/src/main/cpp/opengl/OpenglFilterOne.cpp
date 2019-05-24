//
// Created by mecer on 19-5-13.
//

#include "OpenglFilterOne.h"

OpenglFilterOne::OpenglFilterOne() {

}

OpenglFilterOne::~OpenglFilterOne() {

}

void OpenglFilterOne::onCreate() {

    vertex = "attribute vec4 v_Position; \n"
             "attribute vec2 f_Position; \n"
             "varying vec2 ft_Position; \n"
             "uniform mat4 u_Matrix;\n"
             "\n"
             "void main(){ \n"
             "  ft_Position = f_Position; \n"
             "  gl_Position = v_Position * u_Matrix; \n"
             "}";

    fragment = "precision mediump float; \n"
               "varying vec2 ft_Position; \n"
               "uniform sampler2D sTexture; \n"
               "\n"
               "void main(){ \n"
               "    gl_FragColor = texture2D(sTexture, ft_Position); \n"  // Rrd Green Blue Alpha
               "}";

    // 获得渲染程序
    program = CreateProgram(vertex, fragment, &vShader, &fShader);
    LOGD("create program & program = %d", program);
    if(program > 0){
        // 根据变量名得到顶点变量 attribute, 也就是code中定义的 v_Position
        vPosition = glGetAttribLocation(program, "v_Position"); // 返回 v_Position 的下标
        fPosition = glGetAttribLocation(program, "f_Position"); // 返回 f_Position 的下标
        vTexture = glGetUniformLocation(program, "sTexture"); // 返回2D纹理的句柄
        vMatrix = glGetUniformLocation(program, "u_Matrix"); // 矩阵ID

        //initMatrix(matrix);
        //rotateMatrixByZ(90, matrix);
        //scaleMatrix(0.5, matrix);
        //shadowMatrix(-1,1,-1,1,matrix);

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

        glBindTexture(GL_TEXTURE_2D, 0);  // 解绑
    }

}

void OpenglFilterOne::onChange(int width, int height) {
    surface_width = width;
    surface_height = height;
    glViewport(0,0,width,height); // 设置屏幕宽高
    LOGD("One: set matrix in onChange()");
    setMatrix(width,height, pic_width, pic_height);
    
}

void OpenglFilterOne::draw() {

    LOGD("draw() : one drawtype = %d ", drawType);

    glClearColor(1.0f,0,0,1.0f); // 设置清屏颜色
    glClear(GL_COLOR_BUFFER_BIT); //清空颜色缓冲 准备绘制

    // 绘制三角形
    glUseProgram(program);  // 使用 program 不声明后面属性参数设置都无效
    glUniformMatrix4fv(vMatrix, 1, GL_FALSE, matrix);

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

    switch(drawType){
        case 3:{
            LOGD("native triangle && vertexs = %d" ,vertexs);
            glEnableVertexAttribArray(vPosition); // 声明顶点坐标可用
            glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 8, vertexs);

            glDrawArrays(GL_TRIANGLE_STRIP, 0, 3);
            break;
        }

        case 4:{
            LOGD("native quadrilateral");
            glEnableVertexAttribArray(vPosition); // 声明顶点坐标可用
            glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 8, vertexs);

            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            break;
        }

        case 5:{
            LOGD("native draw picture ");
            glBindTexture(GL_TEXTURE_2D, textureId);

            if(onePixels != NULL){
                LOGD("onePixels is not null");
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, pic_width, pic_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, onePixels); // 传入图片资源 byte类型数组
            }

            glEnableVertexAttribArray(vPosition); // 声明顶点坐标可用
            glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 8, vertexs);


            glEnableVertexAttribArray(fPosition); // 声明纹理坐标可用
            glVertexAttribPointer(fPosition, 2, GL_FLOAT, false, 8, fragments);

            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            glBindTexture(GL_TEXTURE_2D, 0);
            break;
        }
        default:
            break;
    }

}

void OpenglFilterOne::setMatrix(int screenWidth, int screenHeight, int picture_width, int picture_height) {

    initMatrix(matrix);

    float screen_scale = 1.0 * screenWidth / screenHeight ;
    float picture_scale = 1.0 * picture_width / picture_height;
    LOGD("mecer screen_scale = %f, picture_scale = %f", screen_scale, picture_scale);
    float  r;
    if(screen_scale < picture_scale){ // 图片高度缩放
        r = screenHeight / (1.0 * screenWidth / picture_width * screenHeight); // 得到高度缩放的比例
        shadowMatrix(-1, 1, -r, r, matrix);
    }else if (screen_scale > picture_scale){  // 图片宽度缩放
        r = screenWidth / (1.0 * screenHeight / picture_height * screenWidth); // 得到宽度缩放的比例
        shadowMatrix(-r, r, -1, 1, matrix);
    }
}

void OpenglFilterOne::setImageByte(jint w, jint h, void *data) {

    pic_width = w;  // 图片的宽
    pic_height = h; // 图片的高
    onePixels = data;
    LOGD("true is 1, false is 0 : %d",onePixels == NULL ? true : false);
    if(pic_width > 0 && pic_height > 0){
        LOGD("One: set matrix in setImageByte()");
        setMatrix(surface_width, surface_height, pic_width, pic_height);
    }

}

void OpenglFilterOne::setDrawType(int num) {
    drawType = num;
}

void OpenglFilterOne::onDestroy() {
    // 释放资源
    LOGD("One: onDestroy(), %d, %d", vShader, fShader);
    glDeleteTextures(1, &textureId);
    glDetachShader(program, vShader);
    glDetachShader(program, fShader);
    glDeleteShader(vShader);
    glDeleteShader(fShader);
    glDeleteProgram(program);
    if(onePixels != NULL){
        onePixels = NULL;
    }
    LOGD("One: onDestroy() finish ");
}

