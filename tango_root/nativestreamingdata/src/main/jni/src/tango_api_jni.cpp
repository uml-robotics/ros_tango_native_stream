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
 * Author: Eric McCann <emccann@cs.uml.edu>
*/
#include <tango_api_jni.hpp>
application_handle_t *application;
video_overlay_handle_t *overlay;
vio_handle_t *vio;

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
double _depthstamp,_viostamp;

const int UPDATED_NOTHING = 0;
const int UPDATED_ODOM = 1 << 1;
const int UPDATED_DEPTH = 1 << 2;

JNIEXPORT jboolean JNICALL Java_edu_uml_TangoAPI_init(JNIEnv *env)
{
    if (application != NULL)
        return true;
    LOGW("INITIALIZING!");
    const char *source = TANGO_DATA_SOURCE;
    application = ApplicationInitialize(source);
    if (application != NULL)
    {
        if (!CHECK_FAIL(DepthStartBuffering(application)))
        {
#ifdef VIDEOOVERLAY_WORKS
            // THIS FAILS RELIABLY... WHY?!
            CAPIErrorCodes err = VideoOverlayInitialize(overlay);
            LOGI("Video overlay err status = %d",err);
            if (!CHECK_FAIL(err))
            {
#endif
                if (!CHECK_FAIL(VIOInitialize(application, true, NULL)))
                {
                    LOGI("INITIALIZED");
                    return (jboolean)true;
                }
                else
                {
                    LOGE("Failed to initialize VIO");
                    return false;
                }
#ifdef VIDEOOVERLAY_WORKS
            }
            else
                LOGE("Failed to initialize VideoOverlay");
#endif
        }
        else
        {
            LOGE("Failed to start buffering");
            return false;
        }
    }
    else
        LOGE("Failed to initialize application");
    return (jboolean)false;
}

JNIEXPORT jboolean JNICALL Java_edu_uml_TangoAPI_deinit(JNIEnv *env)
{
    LOGW("DEINITIALIZING!");
    if (application != NULL)
    {
        CAPIErrorCodes err;
#ifdef VIDEOOVERLAY_WORKS
        err = VideoOverlayShutdown(application);
        if (err != 0)
            LOGE("Deinitialized overlay with error status: %d", (int)err);
#endif
        err = VIOShutdown(application);
        if (CHECK_FAIL(err) == 0)
            vio = NULL;
        if (err != 0)
            LOGE("Deinitialized vio with error status: %d", (int)err);
        err = DepthStopBuffering(application);
        if (err != 0)
            LOGE("Deinitialized application with error status: %d", (int)err);

        err = ApplicationShutdown(application);
        if (CHECK_FAIL(err) == 0)
            application = NULL;
        if (err != 0)
            LOGE("Deinitialized application with error status: %d", (int)err);
    }
    bool success = (application == NULL);
    application = NULL;
    /*if (_depthbuffer != NULL)
    {
        free(_depthbuffer);
        _depthbuffer = NULL;
    }*/
    return success;
}

jint sendOdom(JNIEnv *env, jobject caller, VIOStatus viostatus, jint returnval)
{
    if (viostatus.timestamp == _viostamp)
        return returnval;
    static jfieldID tx;
    static jfieldID ty;
    static jfieldID tz;
    static jfieldID rx;
    static jfieldID ry;
    static jfieldID rz;
    static jfieldID rw;
    if (tx == 0 || ty == 0 || tz == 0 || rx == 0 || ry == 0 || rz == 0 || rw == 0)
    {
        jclass clazz = env->GetObjectClass(caller);
        tx = env->GetFieldID(clazz, "tx", "F");
        ty = env->GetFieldID(clazz, "ty", "F");
        tz = env->GetFieldID(clazz, "tz", "F");
        rx = env->GetFieldID(clazz, "rx", "F");
        ry = env->GetFieldID(clazz, "ry", "F");
        rz = env->GetFieldID(clazz, "rz", "F");
        rw = env->GetFieldID(clazz, "rw", "F");
    }
    if (tx == 0 || ty == 0 || tz == 0 || rx == 0 || ry == 0 || rz == 0 || rw == 0)
    {
        LOGE("COULD NOT FIND AN ODOM FIELD!!!");
        return returnval;
    }
    env->SetFloatField(caller,tx,viostatus.translation[0]);
    env->SetFloatField(caller,ty,viostatus.translation[1]);
    env->SetFloatField(caller,tz,viostatus.translation[2]);
    env->SetFloatField(caller,rx,viostatus.rotation[0]);
    env->SetFloatField(caller,ry,viostatus.rotation[1]);
    env->SetFloatField(caller,rz,viostatus.rotation[2]);
    env->SetFloatField(caller,rw,viostatus.rotation[3]);
    return returnval | UPDATED_ODOM;
}

JNIEXPORT jobject JNICALL Java_edu_uml_TangoAPI_allocNativeBuffer(JNIEnv *env, jobject caller, jint length)
{
    depth_stuff.shorts = (uint16_t*)malloc(length);
    jobject directBuffer = env->NewDirectByteBuffer(depth_stuff.shorts, length);
    return env->NewGlobalRef(directBuffer);
}
JNIEXPORT void JNICALL Java_edu_uml_TangoAPI_freeNativeBuffer(JNIEnv *env, jobject caller, jobject ref)
{
    env->DeleteGlobalRef(ref);
    free(depth_stuff.shorts);
}

JNIEXPORT jint JNICALL Java_edu_uml_TangoAPI_dowork(JNIEnv *env, jobject caller)
{
    int status = UPDATED_NOTHING;
    if (application == NULL) return UPDATED_NOTHING;
    ApplicationDoStep(application);

    //VIO stuff
    static VIOStatus viostatus;
    CAPIErrorCodes err = VIOGetLatestPoseUnity(application,&viostatus);
    if (CHECK_FAIL(err))
    {
        LOGE("Could not get closest pose");
        return UPDATED_NOTHING;
    }

    //after this point, we invoke the implementation of VIOCallback in java-land immediately before returning
    #define RETURN_AFTER_PUBLISHING_ODOM(x) return sendOdom(env, caller, viostatus, x)

    //get depth resolution
    static int dims[2];
    err = DepthGetResolution(dims);
    if (CHECK_FAIL(err))
    {
        LOGE("Could not get resolution");
        RETURN_AFTER_PUBLISHING_ODOM(UPDATED_NOTHING);
    }
    //if it has changed or hasn't been initialized, allocate a buffer of appropriate size
//    LOGI("HELP! Width: %d Height: %d BPP: %d", width, height, DEPTH_BPP);
    if (_depthwidth != dims[0] || _depthheight != dims[1])
    {
        _depthwidth = dims[0];
        _depthheight = dims[1];
        _depthbufferlength = _depthwidth * _depthheight * DEPTH_BPP;
        LOGI("Trying to tell java-land to resize the ByteBuffer to %d",(sizeof(uint16_t)*_depthbufferlength));
        if (env == NULL || caller == NULL)
        {
            RETURN_AFTER_PUBLISHING_ODOM(UPDATED_NOTHING);
        }
        static jmethodID mid = 0;
        if (mid == 0)
        {
            jclass clazz = env->GetObjectClass(caller);
            mid = env->GetMethodID(clazz, "setBufferLength", "(I)V");
        }
        if (mid == 0)
        {
            LOGE("COULD NOT FIND setbufferlength METHOD!!!");
            RETURN_AFTER_PUBLISHING_ODOM(UPDATED_NOTHING);
        }
        env->CallObjectMethod(caller, mid, (sizeof(uint16_t)*_depthbufferlength));
    }

    //try to get the latest depth frame
    double depthstamp;
    if (depth_stuff.shorts == NULL)
    {
        LOGE("DEPTH HOLDER IS NULL - GTFO!");
        RETURN_AFTER_PUBLISHING_ODOM(UPDATED_NOTHING);
    }
    if (depth_stuff_buffer.ints == NULL)
    {
        LOGW("MALLOCING");
    	depth_stuff_buffer.ints = (int*)malloc(sizeof(int)*_depthbufferlength);
    }
    err = DepthGetLatestFrame(application,depth_stuff_buffer.ints,(sizeof(int)*_depthbufferlength),&depthstamp);
    if (CHECK_FAIL(err))
    {
        LOGE("Could not get latest depthframe - error=%d",err);
        RETURN_AFTER_PUBLISHING_ODOM(UPDATED_NOTHING);
    }
    //if there's no newer frame, then MISSION ACCOMPLISHED
    if (depthstamp == _depthstamp)
        RETURN_AFTER_PUBLISHING_ODOM(UPDATED_NOTHING);
    //otherwise, keep going
    _depthstamp = depthstamp;

    int i=0;
    if (depth_stuff.shorts == NULL || depth_stuff_buffer.ints == NULL)
    {
        LOGE("SOMETHING IS NULL VERY DEEP IN DOWORK - GTFO!");
        RETURN_AFTER_PUBLISHING_ODOM(false);
    }
    for(;i<sizeof(int)*_depthbufferlength;i+=sizeof(int))
    {
        //convert from ints to shorts
        depth_stuff.shorts[i/4] = (uint16_t)((((int) depth_stuff_buffer.bytes[i + 1]) << 8) & 0xff00)
          | (((int) depth_stuff_buffer.bytes[i]) & 0x00ff);
        //LOGE("%d",(int)depth_stuff.shorts[i/2]);
    }

#ifdef VIDEOOVERLAY_WORKS
    {
        double color_timestamp;
        if (CHECK_FAIL(VideoOverlayRenderLatestFrame(application, textureID, _width, _height, &color_timestamp)))
        {
            LOGE("Could not get latest color frame");
            RETURN_AFTER_PUBLISHING_ODOM(UPDATED_NOTHING);
        }
    }
#endif

    RETURN_AFTER_PUBLISHING_ODOM(UPDATED_DEPTH);
}
