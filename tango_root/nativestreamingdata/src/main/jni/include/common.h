#ifndef _COMMON_H
#define _COMMON_H
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <stdarg.h>
#include <math.h>
#include <stdio.h>
#include <tango-api/public-api.h>
#include <tango-api/application-interface.h>
#include <tango-api/hardware-control-interface.h>
#include <tango-api/util-interface.h>
#include <tango-api/depth-interface.h>
#include <tango-api/video-overlay-interface.h>
#include <tango-api/vio-interface.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#endif