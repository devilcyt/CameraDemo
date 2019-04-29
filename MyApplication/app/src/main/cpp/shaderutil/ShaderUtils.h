//
// Created by mecer on 19-4-29.
//

#ifndef MYAPPLICATION_SHADERUTILS_H
#define MYAPPLICATION_SHADERUTILS_H

#include "GLES2/gl2.h"
#include "../log/opengllog.h"

/*
 *  着色器加载流程
 */

static int LoadShaders(int shaderType, const char *shaderCode)
{
    int shader = glCreateShader(shaderType); // 创建着色器
    glShaderSource(shader, 1, &shaderCode, 0); // 加载着色器代码
    glCompileShader(shader);   // 编译代码
    if(shader > 0){
        return shader;
    }else{
        LOGD("shader is %d ,create shader failed, return 0", shader);
    }


}

static int CreatProgram(const char *vertexShaderCode, const char *fragmentShaderCode)
{
    int program = glCreateProgram(); // 创建一个渲染程序
    int vertex = LoadShaders(GL_VERTEX_SHADER, &vertexShaderCode);
    int fragment = LoadShaders(GL_FRAGMENT_SHADER, &fragmentShaderCode); //
    glAttachShader(program, vertex);
    glAttachShader(program, fragment);  // 将顶点和纹理着色器添加到渲染程序中
    glLinkProgram(program); // 链接源程序
}



#endif //MYAPPLICATION_SHADERUTILS_H
