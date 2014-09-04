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
#ifndef TANGO_API_JNI_H_
#define TANGO_API_JNI_H_

#define TAG "TangoJNI"
#include <common.h>

#define TANGO_DATA_SOURCE "[Superframes Small-Peanut]"
#define DEPTH_BPP 1

#define CHECK_FAIL(x) (x == kCAPIFail || x == kCAPINotImplemented || x == kCAPIOperationFailed)

void setbufferlength(int length);

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL Java_edu_uml_TangoAPI_init(JNIEnv *env);
JNIEXPORT jboolean JNICALL Java_edu_uml_TangoAPI_deinit(JNIEnv *env);
JNIEXPORT jobject JNICALL Java_edu_uml_TangoAPI_allocNativeBuffer(JNIEnv *env, jobject caller, jint size);
JNIEXPORT void JNICALL Java_edu_uml_TangoAPI_freeNativeBuffer(JNIEnv *env, jobject caller);
JNIEXPORT jobject JNICALL Java_edu_uml_TangoAPI_allocNativeOdomBuffer(JNIEnv *env, jobject caller);
JNIEXPORT void JNICALL Java_edu_uml_TangoAPI_freeNativeOdomBuffer(JNIEnv *env, jobject caller);
JNIEXPORT jint JNICALL Java_edu_uml_TangoAPI_dowork(JNIEnv *env, jobject caller);

#ifdef __cplusplus
}
#endif

#endif  // TANGO_API_JNI
