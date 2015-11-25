/*
 * Copyright (c) 2014, University Of Massachusetts Lowell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Massachusetts Lowell nor the names
 * from of its contributors may be used to endorse or promote products
 * derived this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * Author: Eric McCann <emccann@cs.uml.edu>, Eric Marcoux <emarcoux@cs.uml.edu>
*/
#include <jni.h>
#include <tango_api_jni.hpp>
#include <pthread.h>
#include <unistd.h>
#include <string.h>
#include <string>
#include <sys/types.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include "reader_writer.h"
#include "thread_pool.hpp"
#include "byte_buffer.hpp"
#include "common.h"
#include "debugging_tools.h"

//#define DEBUGGING_OFF
#define NUM_DEPTH_PUBLISHER_THREADS    4
#define DEPTH_BUFFER_OVERCOMMIT        0
#define NUM_EXCEPTION_HANDLER_THREADS  0
#define TARGET_VIO_RATE               15
#define TARGET_RGB_RATE               2
#define NUM_IMAGE_PUBLISHER_THREADS    4
#define IMAGE_BUFFER_OVERCOMMIT        0

#define YUV_CONVERTER_STRIDE 1280 // 1024 rounded up to next multiple of 16 utilize simd and try and keep in cacheln
#define CLAMP(x) (x<0?0:x>255?255:x)

JavaVM* g_vm;
struct DepthBufferDouble{
    ByteBuffer<float>  *depth_buffer;
    ByteBuffer<int>    *depth_buffer_size;

    DepthBufferDouble(JNIEnv *env, uint32_t num_pts) :
        depth_buffer(     new ByteBuffer<float>(g_vm, env,  num_pts*3)),
        depth_buffer_size(new ByteBuffer<int>(g_vm, env,    1)) {}

    virtual ~DepthBufferDouble() {
        delete depth_buffer;
        delete depth_buffer_size;
    }
};

struct ImageStruct{
    ByteBuffer<uint8_t> *image_buffer;
    //int height;
    //int width;
    //int stride;

    ImageStruct(JNIEnv *env, size_t size)
    {
        image_buffer = new ByteBuffer<uint8_t> (g_vm, env, size*sizeof(uint8_t));
    }

    virtual ~ImageStruct()
    {
        delete image_buffer;
    }
};

typedef struct {
    JNIEnv    *env;
    jobject    caller;
    jobject    activity;

    jobject    exceptionHandler;
    jmethodID  exceptionHandlerMID;

    jobject    vioReciever;
    jmethodID  vioRecieverCallbackMID;
    
    jobject    depthReciever;
    jmethodID  depthRecieverCallbackMID;

    jobject    imageReceiver;
    jmethodID  imageReceiverCallbackMID;
    jmethodID  imageReceiverInfoMID;

    ThreadPool<DepthBufferDouble> *depth_object_pool;
    ThreadPool<ImageStruct> *image_pool;
//    ThreadPool<ExceptionData>     *exception_handler_thread_pool;

    GLuint color_image_texture;
    GLuint offscreen_framebuffer;
    bool vio_run;
    bool hasImageInfoBeenCalled;
    pthread_t vio_thread;
} tango_context;

enum tango_error {
    SUCCESS = 0,
    ERR_ALREADY_RUNNING,
    ERR_ALREADY_STOPPED,
    ERR_COULD_NOT_CREATE_WORKER_THREAD,
    ERR_COULD_NOT_JOIN_WORKER_THREAD,
    ERR_COULD_NOT_CREATE_CONTEXT_MUTEX,
    ERR_COULD_NOT_DESTROY_CONTEXT_MUTEX
};

tango_context ctxt;                 // global context object

void onDepthCallback(void *context, const TangoXYZij *xyzij);
void onImageCallback(void *context, TangoCameraId id);

void onTangoEvent(void* context, const TangoEvent* event);

void jni_tp_start(void** start_data);
void jni_tp_shutdown(void *start_data);
void depth_buffer_tp_notify(void *start_data, DepthBufferDouble *notify_data);
void image_buffer_tp_notify(void *start_data, ImageStruct *data);

typedef struct _jni_thread_start_data {
    JNIEnv* env;
} *jni_thread_start_data;

uint8_t *temp_image;

// ================= START OF COLOR IMAGE PROC ===============================
void onImageCallback(void *context, TangoCameraId id)
{
    //TODO: Get image publishing to work
     /*LOGI("Got image!");
     tango_context* c = (tango_context*)context;
     ImageStruct* img_struct;
     if (!c->image_pool->grabObjectNonBlocking(&img_struct)) return;
     TangoService_updateTexture(TANGO_CAMERA_COLOR,NULL);
     glBindTexture(GL_TEXTURE_2D, c->color_image_texture);
     glBindFramebuffer(GL_FRAMEBUFFER, c->offscreen_framebuffer);
     glViewport(0, 0, 1280, 720);
     glReadPixels(0,0,1280,720, GL_RGB, GL_UNSIGNED_BYTE, img_struct->image_buffer->getBuffer());
     //memcpy(img_struct->image_buffer->getBuffer(), glReadPixels(0,0,1280,720, ((size_t)(1280 * 720 * 3* sizeof(uint8_t))));
     //img_struct->width = buffer->width;
     //img_struct->height = buffer->height;
     //img_struct->stride = buffer->stride;
     c->image_pool->notifyWithData(img_struct);*/

}

void image_buffer_tp_notify(void *start_data, ImageStruct* data)
{
    JNIEnv *env = ((jni_thread_start_data)start_data)->env;
    if (!ctxt.hasImageInfoBeenCalled)
    {
        env->CallVoidMethod(ctxt.imageReceiver, ctxt.imageReceiverInfoMID, 720, 1280, 1280);///data->height, data->width, data->stride);
        ctxt.hasImageInfoBeenCalled = true;
    }
    env->CallVoidMethod(ctxt.imageReceiver, ctxt.imageReceiverCallbackMID, data->image_buffer->getObject());
}

// ================= END OF COLOR IMAGE PROC =================================

// ================= START OF THREAD POOL JNI_THREAD START DATA =================

void jni_tp_start(void** start_data)
{
    JNIEnv* env;
    char threadName[32];
    sprintf(threadName, "tp_thread[%08x]", (unsigned)pthread_self());
    JavaVMAttachArgs args = { JNI_VERSION_1_6, threadName, NULL };

    jni_thread_start_data my_start_data =  (jni_thread_start_data) malloc(sizeof(_jni_thread_start_data));
    if(g_vm->AttachCurrentThread(&env, &args) != 0) {
        LOGE("Failed to attach thread JNIEnv");
    }
    my_start_data->env = env;

    (*start_data) = (void*)my_start_data;
    LOGI("thread %s started", threadName);
}

void jni_tp_shutdown(void *start_data)
{
    g_vm->DetachCurrentThread();
    free(start_data);
}
// ================= END OF THREAD POOL JNI_THREAD START DATA =================





// ================= START OF DEPTH PROC =================
// this function is done asynchronously by the thread pool every time it is notified by the onDepthCallback
void depth_buffer_tp_notify(void *start_data, DepthBufferDouble* data)
{
    static const TangoCoordinateFramePair pair = { TANGO_COORDINATE_FRAME_START_OF_SERVICE, TANGO_COORDINATE_FRAME_DEVICE };
    JNIEnv *env                                = ((jni_thread_start_data)start_data)->env;
    env->CallVoidMethod(ctxt.depthReciever,
                        ctxt.depthRecieverCallbackMID,
                        data->depth_buffer->getObject(),
                        data->depth_buffer_size->getObject());
}

void onDepthCallback(void *context, const TangoXYZij *xyzij)
{
    //LOGI("onDepthCallback");
    /*LOGI("IJRows = %u", xyzij->ij_rows);
    LOGI("IJColumns = %u", xyzij->ij_cols);
    if (xyzij->ij_rows > 0 && xyzij->ij_cols > 0)
    {
        LOGI("IJ0 is %u", xyzij->ij[0]);
        LOGI("IJ1 is %u", xyzij->ij[1]);
    }
    LOGI("XYZCount is %u", xyzij->xyz_count);*/
    tango_context* c = (tango_context*)context;
    DepthBufferDouble* db_double;
    if(!c->depth_object_pool->grabObjectNonBlocking(&db_double)) return; // only pointer copy

    (*db_double->depth_buffer_size->getBuffer()) = (int) xyzij->xyz_count;
    memcpy(db_double->depth_buffer->getBuffer(), xyzij->xyz, xyzij->xyz_count * 3 * sizeof(float));

    ctxt.depth_object_pool->notifyWithData(db_double);
}
// ================= END OF DEPTH PROC =================

// ================= START OF VIO PROC =================
// set to run at 15 hz
void* th_vio_proc(void*) {
    static JavaVMAttachArgs args = { JNI_VERSION_1_6, "th_vio_proc", NULL };
    static const TangoCoordinateFramePair pair = { TANGO_COORDINATE_FRAME_START_OF_SERVICE, TANGO_COORDINATE_FRAME_DEVICE };
    JNIEnv* env = NULL;
    TangoPoseData pose;
    struct timeval tv_start, tv_end;
    static uint32_t sleep_time_us = (uint32_t)((1.0/TARGET_VIO_RATE)*1000000.0), elapsed_time_us;
    if(g_vm->AttachCurrentThread(&env, &args) != 0) {
        LOGE("Failed to attach thread JNIEnv in th_vio_proc");
        return NULL;
    }
    ByteBuffer<double>* vio_buffer = new ByteBuffer<double>(g_vm, env, 7);
    double* underlying_vio_buffer = vio_buffer->getBuffer();

    while(ctxt.vio_run) {
        gettimeofday(&tv_start, 0); // start timer
        if(TangoService_getPoseAtTime(0.0, pair, &pose) != TANGO_SUCCESS) {
            if(!ctxt.vio_run) {
                break;
            }
        } else {
            memcpy(underlying_vio_buffer,   pose.translation, 3*sizeof(double));
            memcpy(underlying_vio_buffer+3, pose.orientation, 4*sizeof(double));
            if(vio_buffer)env->CallVoidMethod(ctxt.vioReciever, ctxt.vioRecieverCallbackMID, vio_buffer->getObject());
        }
        gettimeofday(&tv_end, 0);   // end timer
        elapsed_time_us = (tv_end.tv_sec-tv_start.tv_sec)*1000000 + (tv_end.tv_usec-tv_start.tv_usec);
        // sleep remaining time
        if(elapsed_time_us < sleep_time_us) usleep(sleep_time_us - elapsed_time_us);

    }
    delete vio_buffer;
    g_vm->DetachCurrentThread();
    return NULL;
}
// ================= END OF VIO PROC ===================



// start of new api
JNIEXPORT jint Java_edu_uml_TangoAPI_nStartTangoAPI(JNIEnv *env, jobject caller, jobject activity, jobject vioReciever, jobject imageReciever, jobject depthReciever, jobject exceptionHandler)
{
    // ====== begin populating the context ======
    ctxt.caller               = env->NewGlobalRef(caller);
    ctxt.activity             = env->NewGlobalRef(activity);
    ctxt.vioReciever          = env->NewGlobalRef(vioReciever);
    ctxt.depthReciever        = env->NewGlobalRef(depthReciever);
    ctxt.exceptionHandler     = env->NewGlobalRef(exceptionHandler);
    ctxt.imageReceiver        = env->NewGlobalRef(imageReciever);

    // cache each of the method ids for the callbacks so they only need to be generated once
    jclass    vio_reciever_class      = env->GetObjectClass(ctxt.vioReciever);
    jclass    depth_reciever_class    = env->GetObjectClass(ctxt.depthReciever);
    jclass    caller_class            = env->GetObjectClass(ctxt.caller);
    jclass    exception_handler_class = env->GetObjectClass(ctxt.exceptionHandler);
    jclass    image_receiver_class    = env->GetObjectClass(ctxt.imageReceiver);

    ctxt.imageReceiverCallbackMID  = env->GetMethodID(image_receiver_class,    "ImageCallback",   "(Ljava/nio/ByteBuffer;)V");
    ctxt.imageReceiverInfoMID      = env->GetMethodID(image_receiver_class,    "ImageInfo",       "(III)V");
    ctxt.vioRecieverCallbackMID    = env->GetMethodID(vio_reciever_class,      "VIOCallback",     "(Ljava/nio/ByteBuffer;)V");
    ctxt.depthRecieverCallbackMID  = env->GetMethodID(depth_reciever_class,    "DepthCallback",   "(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)V");
    ctxt.exceptionHandlerMID       = env->GetMethodID(exception_handler_class, "handleException", "(Ljava/lang/String;Ljava/lang/String;)V");
    ctxt.vio_run = true;
    ctxt.hasImageInfoBeenCalled = false;

    ///OpenGL preparation
    glGenFramebuffers(1, &ctxt.offscreen_framebuffer);
    glBindFramebuffer(GL_FRAMEBUFFER, ctxt.offscreen_framebuffer);
    glGenTextures (1, &ctxt.color_image_texture);
    glBindTexture(GL_TEXTURE_2D, ctxt.color_image_texture);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ctxt.color_image_texture, 0);
    GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if(status != GL_FRAMEBUFFER_COMPLETE) {
       LOGI("ERROR: Failed to make complete framebuffer object %x", status);
    }


//    ctxt.exception_handler_thread_pool = new ThreadPool<ExceptionData>(0,
//                                                                                          NUM_EXCEPTION_HANDLER_THREADS,
//                                                                                          exception_handler_tp_start,
//                                                                                          exception_handler_tp_notify,
//                                                                                          jni_tp_shutdown);

    ctxt.depth_object_pool = new ThreadPool<DepthBufferDouble>(NUM_DEPTH_PUBLISHER_THREADS+DEPTH_BUFFER_OVERCOMMIT,
                                                               NUM_DEPTH_PUBLISHER_THREADS,
                                                               jni_tp_start,
                                                               depth_buffer_tp_notify,
                                                               jni_tp_shutdown);

    ctxt.image_pool = new ThreadPool<ImageStruct>(NUM_IMAGE_PUBLISHER_THREADS+IMAGE_BUFFER_OVERCOMMIT,
                                                         NUM_IMAGE_PUBLISHER_THREADS,
                                                         jni_tp_start,
                                                         image_buffer_tp_notify,
                                                         jni_tp_shutdown);

    if (CHECK_FAIL(TangoService_initialize(env, ctxt.activity))) {
        LOGE("Failed to initialize service");
    }
    TangoConfig config = TangoService_getConfig(TANGO_CONFIG_DEFAULT);
    if (!config) {
        LOGE("could not get the config using TangoService_getConfig");
        return (jint)ERR_COULD_NOT_CREATE_WORKER_THREAD;
    }

    bool currmotion, currdepth;
    double depth_period_in_seconds;
    if ((CHECK_FAIL(TangoConfig_getBool(config, "config_enable_depth", &currdepth)) || !currdepth)) {
        if (CHECK_FAIL(TangoConfig_setBool(config, "config_enable_depth", true))) {
            LOGE("COULD NOT SET MISSING OR WRONG PARAM: depth");
            return (jint)ERR_COULD_NOT_CREATE_WORKER_THREAD;
        }
    }
    if ((CHECK_FAIL(TangoConfig_getBool(config, "config_enable_motion_tracking", &currmotion)) || !currmotion)) {
        if (CHECK_FAIL(TangoConfig_setBool(config, "config_enable_motion_tracking", true))) {
            LOGE("COULD NOT SET MISSING OR WRONG PARAM: motion_tracking");
            return (jint)ERR_COULD_NOT_CREATE_WORKER_THREAD;
        }
    }


    int max_point_cloud_elements;
    if(CHECK_FAIL(TangoConfig_getInt32(config, "max_point_cloud_elements", &max_point_cloud_elements)) || !max_point_cloud_elements) {
        LOGE("COULD NOT GET MISSING OR WRONG PARAM: max_point_cloud_elements");
        return (jint)ERR_COULD_NOT_CREATE_WORKER_THREAD;
    }
    for(size_t i = 0; i < NUM_DEPTH_PUBLISHER_THREADS+DEPTH_BUFFER_OVERCOMMIT; ++i) {
        DepthBufferDouble* db_double = new DepthBufferDouble(env, max_point_cloud_elements);
        ctxt.depth_object_pool->returnObjectToPool(db_double);
    }

    TangoCameraIntrinsics ccIntrinsics;
    TangoService_getCameraIntrinsics(TANGO_CAMERA_COLOR, &ccIntrinsics);

    for(size_t i = 0; i < NUM_IMAGE_PUBLISHER_THREADS+IMAGE_BUFFER_OVERCOMMIT; ++i) {
            ImageStruct* img_struct = new ImageStruct(env, ccIntrinsics.height*ccIntrinsics.width*3);
            ctxt.image_pool->returnObjectToPool(img_struct);
    }


    pthread_create(&ctxt.vio_thread, NULL, th_vio_proc, NULL);
    
    if (TangoService_connectOnXYZijAvailable(onDepthCallback) != TANGO_SUCCESS) {
         LOGI("TangoService_connectOnXYZijAvailable(): Failed");
         return (jint)ERR_COULD_NOT_CREATE_WORKER_THREAD;
    }

    if (TangoService_connectTextureId(TANGO_CAMERA_COLOR, ctxt.color_image_texture, &ctxt, onImageCallback) != TANGO_SUCCESS){
            LOGI("TangoService_connectOnFrameAvailable(): Failed");
            return (jint)ERR_COULD_NOT_CREATE_WORKER_THREAD;
    }

//    if (CHECK_FAIL(TangoService_connectOnTangoEvent(onTangoEvent))) {
//         LOGI("TangoService_connectOnTangoEvent(): Failed");
//         return (jint)ERR_COULD_NOT_CREATE_WORKER_THREAD;
//    }

    if (CHECK_FAIL(TangoService_connect(&ctxt, config))) {
        LOGE("Connect failed");
        return (jint)ERR_COULD_NOT_CREATE_WORKER_THREAD;
    }
    LOGI("TangoAPI started successfully");
    return (jint)SUCCESS;
}

JNIEXPORT jint Java_edu_uml_TangoAPI_nStopTangoAPI(JNIEnv *env, jobject caller)
{
    TangoService_disconnect();
    ctxt.vio_run = false;

    if(ctxt.vio_thread) pthread_join(ctxt.vio_thread, NULL);
    ctxt.vio_thread = 0;

    if(ctxt.caller)            env->DeleteGlobalRef(ctxt.caller);
    ctxt.caller = NULL;
    if(ctxt.activity)          env->DeleteGlobalRef(ctxt.activity);
    ctxt.activity = NULL;
    if(ctxt.vioReciever)       env->DeleteGlobalRef(ctxt.vioReciever);
    ctxt.vioReciever = NULL;
    if(ctxt.depthReciever)     env->DeleteGlobalRef(ctxt.depthReciever);
    ctxt.depthReciever = NULL;
    if(ctxt.exceptionHandler)  env->DeleteGlobalRef(ctxt.exceptionHandler);
    ctxt.exceptionHandler = NULL;

    delete ctxt.depth_object_pool;


    LOGI("TangoAPI shutdown successfully");
    return (jint)SUCCESS;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *resPoseerved) { g_vm = vm; return JNI_VERSION_1_6; }
