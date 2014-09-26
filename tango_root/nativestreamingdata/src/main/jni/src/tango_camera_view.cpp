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
#include <tango_camera_view.hpp>

#define APPLICATION_STATE_FIELD_NAME "applicationState"
/**
 * Holds all state information for the current application (global to the application).
 */
typedef struct _tango_application_state {
    application_handle_t *application;
    bool video_overlay_init; // VideoOverlayInitialize has been called and succeeded
    bool vio_init;           // VIOInitialize          has been called and succeeded
    bool depth_init;         // DepthStartBuffering    has been called and succeeded
    struct {
        GLuint textureID;
        int width;
        int height;
        GLubyte *current_frame;
    } video_settings;
    GLuint* video_overlay_textureID; // the handle for the opengl texture holding the rendered fbo
} tango_application_state;

tango_application_state* state = NULL;

#define DEPTH_BPP 1
int _depthwidth, _depthheight;
union DEPTH_STUFF_ {
    unsigned char *bytes;
    uint16_t *shorts;
} depth_stuff;

union depth_stuff_buffer {
    unsigned char *bytes;
    int *ints;
} depth_stuff_buffer; //SURPRISE!

int _depthbufferlength;
double _depthstamp,
       _viostamp,
       _color_timestamp;

float *odombuf;

const int UPDATED_NOTHING = 0;
const int UPDATED_ODOM = 1 << 1;
const int UPDATED_DEPTH = 1 << 2;

bool init = false;




tango_application_state* applicationFromByteBuffer(JNIEnv *env, jobject caller, jobject byteBuffer) {
    return (tango_application_state*) env->GetDirectBufferAddress(byteBuffer);
}

jobject applicationStateToByteBuffer(JNIEnv *env, jobject caller, tango_application_state* applicationState) {
    jobject byte_buffer = env->NewDirectByteBuffer((void*)applicationState, sizeof(tango_application_state));
    if(!byte_buffer) LOGW("could not allocate byte buffer for application state");
    return byte_buffer;
}

tango_application_state* getCallerApplicationState(JNIEnv *env, jobject caller) {
    static jclass clazz = env->GetObjectClass(caller);
    if(clazz == 0) { LOGW("could not get class of caller"); return 0; }
    static jfieldID fid = env->GetFieldID(clazz, APPLICATION_STATE_FIELD_NAME, "Ljava/nio/ByteBuffer");
    if(fid == 0) { LOGW("could not get the applicationState field"); return 0; }

    return applicationFromByteBuffer(env, caller, env->GetObjectField(caller, fid));
}

// create a new native ByteBuffer in the caller that points to the state struct
int setCallerApplicationState(JNIEnv *env, jobject caller, tango_application_state* state) {
    static jclass clazz = env->GetObjectClass(caller);
    if(clazz == 0) { LOGW("could not get class of caller"); return 1; }
    static jfieldID fid = env->GetFieldID(clazz, "applicationState", "Ljava/nio/ByteBuffer");
    if(fid == 0) { LOGW("could not get the applicationState field"); return 1; }

    env->SetObjectField(caller, fid, applicationStateToByteBuffer(env, caller, state));
    return 0;
}

JNIEXPORT jboolean JNICALL Java_edu_uml_TangoCameraView_init
  (JNIEnv *env, jobject caller)
{
    if(state != NULL) { return (jboolean) true; } // don't allow reinitialization

    state = (tango_application_state*) malloc(sizeof(tango_application_state));
    if(state == NULL) return (jboolean) false;

    state->video_settings.current_frame = (GLubyte*) malloc(4 * 1280 * 720);

//    if(setCallerApplicationState(env, caller, state)) { // !0 on failure
//        LOGW("could not set the caller's application state");
//        return (jboolean) false;
//    }

    state->application = ApplicationInitialize(TANGO_DATA_SOURCE);
    if(state->application != NULL) {
        LOGW("TANGO_APPLICATION: alive!");
        if(CHECK_FAIL(VideoOverlayInitialize(state->application))) {
            LOGW("Failed to initialize the video overlay");
            state->video_overlay_init = false;
            return (jboolean) false;
        }

        state->video_overlay_init = true;

//        if(CHECK_FAIL(VIOInitialize(state->application, true, NULL))) {
//            LOGW("Failed to initialize the VIO");
//            state->vio_init = false;
//            return (jboolean) false;
//        }
//        state->vio_init = true;
    } else {
        LOGW("TANGO_APPLICATION: could not be instantiated");
        return false;
    }
    return (jboolean) true;
}

JNIEXPORT jboolean JNICALL Java_edu_uml_TangoCameraView_destruct
  (JNIEnv *env, jobject caller)
{
    if(CHECK_FAIL(VideoOverlayShutdown(state->application))) { // video overlay fails on 0
        LOGW("failed to shutdown video overlay");
        return (jboolean) false;
    }

    if(CHECK_FAIL(ApplicationShutdown(state->application))) { // video overlay fails on 0
        LOGW("failed to shutdown video overlay");
        return (jboolean) false;
    }
    free(state->video_settings.current_frame);
    free(state);

    return (jboolean) true;
}


JNIEXPORT jobject JNICALL Java_edu_uml_TangoCameraView_allocNativeVideoBuffer
  (JNIEnv *env, jobject caller, jint size)
{
    return (jobject) NULL;
}

JNIEXPORT void JNICALL Java_edu_uml_TangoCameraView_freeNativeVideoBuffer
  (JNIEnv *env, jobject caller)
{}



JNIEXPORT void JNICALL Java_edu_uml_TangoCameraView_onSurfaceCreatedNative
  (JNIEnv *env, jobject caller)
{
//    Java_edu_uml_TangoCameraView_init(env,caller);
    glGenTextures(1, (GLuint*) &state->video_settings.textureID);
}


JNIEXPORT void JNICALL Java_edu_uml_TangoCameraView_onSurfaceChangedNative
  (JNIEnv *env, jobject caller, jint width, jint height)
{
    state->video_settings.width = width;
    state->video_settings.height = height;

//    glMatrixMode(GL_PROJECTION);
//    glLoadIdentity();
//    glOrthoOf(0, width, 0, height, -10f, -10f);
    glEnable(GL_TEXTURE_2D);

    glViewport(0, 0, state->video_settings.width,
                     state->video_settings.height);
}

JNIEXPORT void JNICALL Java_edu_uml_TangoCameraView_onDrawNative
  (JNIEnv *env, jobject caller)
{
//    GLuint fb;
    if(!init){
    Java_edu_uml_TangoCameraView_init(env, caller);
    init = false;
    }

    GLuint textureID;
    glGenTextures(1, &textureID);
    double color_timestamp, depth_stamp;

    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glClearColor(0.1f,0.1f,0.4f,1.f);
    glEnable (GL_DEPTH_TEST);
//    static int dims[2];
//    DepthGetResolution(dims);
//    int _depthbufferlength = _depthwidth * _depthheight * DEPTH_BPP;
//    depth_stuff_buffer.ints = (int*) malloc(sizeof(int) * _depthbufferlength);


    LOGW("rendering to texture %d", textureID);
    if(CHECK_FAIL(ApplicationDoStep(state->application))) LOGW("could not do step of application");

//    if(CHECK_FAIL(DepthGetLatestFrame(state->application,
//                        depth_stuff_buffer.ints,
//                        (sizeof(int)*_depthbufferlength),
//                        &depth_stamp)))
//        LOGW("could not get latest depth frame!");

    if(CHECK_FAIL(VideoOverlayRenderLatestFrame(state->application,
                                  textureID,
//                                  state->video_settings.textureID,
                                  state->video_settings.width,
                                  state->video_settings.height,
                                  &color_timestamp)))
        LOGW("could not render latest video frame");


     // object is stored in texture through another frame buffer
     GLuint fb;
     glGenFramebuffers(1, &fb);
     glBindFramebuffer(GL_FRAMEBUFFER, fb);
     glBindTexture(GL_TEXTURE_2D, textureID);
     glFramebufferTexture2D(GL_FRAMEBUFFER,
                            GL_COLOR_ATTACHMENT0,
                            GL_TEXTURE_2D,
                            textureID,
                            0);
     glReadPixels(0,0,1280,720,GL_RGBA,GL_UNSIGNED_BYTE,(void*)state->video_settings.current_frame);
     glDeleteFramebuffers(1, &fb);
     glDeleteTextures(1, &textureID);

     LOGW("some pixels - %d %d", state->video_settings.current_frame[100], state->video_settings.current_frame[101]);
//     free(depth_stuff_buffer.ints);
}
