/*
 * Copyright (c) 014, University Of Massachusetts Lowell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * . Redistributions in binary form must reproduce the above copyright
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

/* Header for class edu_uml_TangoCameraView */
#ifndef _Included_edu_uml_TangoCameraView
#define _Included_edu_uml_TangoCameraView

#include <common.h>
#include <math.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <GLES2/gl2platform.h>

#define TANGO_DATA_SOURCE "[Superframes Small-Peanut]"
#define CHECK_FAIL(x) (x == kCAPIFail || x == kCAPINotImplemented || x == kCAPIOperationFailed)

#ifdef __cplusplus
extern "C" {
#endif
#define TAG "TangoCameraViewJNI"
/*
 * Class:     edu_uml_TangoCameraView
 * Method:    init
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_edu_uml_TangoCameraView_init
  (JNIEnv *, jobject);

/*
 * Class:     edu_uml_TangoCameraView
 * Method:    destruct
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_edu_uml_TangoCameraView_destruct
  (JNIEnv *, jobject);

/*
 * Class:     edu_uml_TangoCameraView
 * Method:    allocNativeVideoBuffer
 * Signature: (I)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_edu_uml_TangoCameraView_allocNativeVideoBuffer
  (JNIEnv *, jobject, jobject);

/*
 * Class:     edu_uml_TangoCameraView
 * Method:    freeNativeVideoBuffer
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_edu_uml_TangoCameraView_freeNativeVideoBuffer
  (JNIEnv *, jobject, jobject);

/*
 * Class:     edu_uml_TangoCameraView
 * Method:    onSurfaceCreatedNative
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_edu_uml_TangoCameraView_onSurfaceCreatedNative
  (JNIEnv *, jobject);

/*
 * Class:     edu_uml_TangoCameraView
 * Method:    onSurfaceChangedNative
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_edu_uml_TangoCameraView_onSurfaceChangedNative
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     edu_uml_TangoCameraView
 * Method:    onDrawNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_edu_uml_TangoCameraView_onDrawNative
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif

