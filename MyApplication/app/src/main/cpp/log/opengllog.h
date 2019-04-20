//
// Created by mecer on 19-4-14.
//

#ifndef MYAPPLICATION_LOG_H
#define MYAPPLICATION_LOG_H

#include "android/log.h"
#define LOGD(FORMATE, ...) __android_log_print(ANDROID_LOG_DEBUG, "swq", FORMATE, ##__VA_ARGS__);

class opengllog {
};


#endif //MYAPPLICATION_LOG_H
