#ifndef _COMMON_H
#define _COMMON_H
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <stdarg.h>
#include <math.h>
#include <stdio.h>
#include <tango-api/tango_client_api.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#endif
