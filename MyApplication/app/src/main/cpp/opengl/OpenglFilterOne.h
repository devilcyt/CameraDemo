//
// Created by mecer on 19-5-13. 具体实现类
//

#ifndef MYAPPLICATION_OPENGLFILTERONE_H
#define MYAPPLICATION_OPENGLFILTERONE_H

#include "OpenglBase.h"
#include "../shaderutil/ShaderUtils.h"
#include "../matrix/MatrixUtil.h"
#include <cstdlib>

class OpenglFilterOne : public OpenglBase{

public:

    GLint vPosition;  // 表示顶点变量，通过它来设置绘制前的坐标参数
    GLint fPosition; // 纹理坐标返回值
    GLint vTexture; // 2D纹理
    GLuint textureId; // 纹理ID
    GLint vMatrix;

    int pic_width;
    int pic_height;
    void *onePixels = NULL; // 像素值 即 纹理
    // 矩阵
    float matrix[16];

public:
    OpenglFilterOne();
    ~OpenglFilterOne();

    void onCreate();

    void onChange(int width, int height);

    void onDestroy();

    void draw();

    void setMatrix(int width, int height, int pwidth, int pheight);

    void setImageByte(jint w, jint h, void *data);

    void setDrawType(int num);
};


#endif //MYAPPLICATION_OPENGLFILTERONE_H
