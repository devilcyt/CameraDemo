//
// Created by mecer on 19-5-7.
//
//   矩阵操作工具类
//
//  角度转弧度公式:
//  弧度 = 角度 × （pi / 180.0）
//

#ifndef MYAPPLICATION_MATRIXUTIL_H
#define MYAPPLICATION_MATRIXUTIL_H

#include "math.h"


static void initMatrix(float *matrix){
    for(int i = 0; i < 16; i++){
        if(i % 5 == 0){
            matrix[i] = 1;
        }else{
            matrix[i] = 0;
        }
    }
}


//  沿着Z轴旋转
static void rotateMatrixByZ(double angle, float *matrix){

    angle = angle * (M_PI / 180.0);

    matrix[0] = cos(angle);
    matrix[1] = -sin(angle);
    matrix[4] = sin(angle);
    matrix[5] = cos(angle);
}

//  沿着X轴旋转
static void rotateMatrixByX(double angle, float *matrix){
    angle = angle * (M_PI / 180.0);

    matrix[0] = 1;
    matrix[5] = cos(angle);
    matrix[6] = -sin(angle);
    matrix[9] = sin(angle);
    matrix[10] = cos(angle);
    matrix[15] = 1;
}

//  沿着Y轴旋转
static void rotateMatrixByY(double angle, float *matrix){

    angle = angle * (M_PI / 180.0);

    matrix[0] = cos(angle);
    matrix[2] = sin(angle);
    matrix[5] = 1;
    matrix[8] = -sin(angle);
    matrix[10] = cos(angle);
    matrix[15] = 1;

}

// 缩放
static void scaleMatrix(double scale, float *matrix){
    // matrix[0] 对应 X, matrix[5], 对应y, matrix[10] 对应z

    /*s1, 0, 0, 0,
    0, s2, 0, 0,       s1 = x. s2 = y, s3 = z;  缩放向量 v3(s1,s2,s3)
    0, 0, s3, 0,
    0, 0, 0, 1*/

    matrix[0] = scale;   // 沿着x, y轴，均匀缩放
    matrix[5] = scale;

}


// 平移矩阵
static void transMatrix(double x, double y, double z, float *matrix){

    matrix[3] = x;
    matrix[7] = y;  // 2D 不移动z轴
}


// 正交投影  -- for 2D
static void shadowMatrix(float left, float right, float bottom, float top, float *matrix){
    matrix[0] = 2 / (right - left);
    matrix[3] = (right + left) / (right - left) * -1;
    matrix[5] = 2 / (top - bottom);
    matrix[7] = (top + bottom) / (top - bottom) * -1;
    matrix[10] = 1;
    matrix[11] = 1;
}

#endif //MYAPPLICATION_MATRIXUTIL_H
