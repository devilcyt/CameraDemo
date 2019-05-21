//
// Created by mecer on 19-5-13.
//

#include "OpenglBase.h"

OpenglBase::OpenglBase() {


    // 把顶点，纹理坐标的初始化放到这里执行
    vertexs = new float[8];
    fragments = new float[8];

    float v[] = {
            -1,1,
            1,1,
            -1,-1,
            1,-1
    };
    memcpy(vertexs, v, sizeof(v));

    float f[] = {
            0,0,
            1,0,
            0,1,
            1,1
    };
    memcpy(fragments, f, sizeof(f));

}

OpenglBase::~OpenglBase() {

    // 回收数组对象
    delete []vertexs;
    delete []fragments;

}

void OpenglBase::onCreate() {

}

void OpenglBase::onChange(int width, int height) {
    surface_width = width;
    surface_height = height;
}

void OpenglBase::draw() {

}

void OpenglBase::onDestroy() {
    LOGD("base destroy()");
}

void OpenglBase::setImageByte(jint w, jint h, void *data) {

}

void OpenglBase::setDrawType(int num) {

}
