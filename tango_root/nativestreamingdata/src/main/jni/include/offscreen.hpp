/*
 * This file is based on a test from mesa3d in the AOSP repos.
 * Some of mesa is GPL. This probably needs to be handled better than this.
 *   https://android.googlesource.com/platform/external/mesa3d/+/cd7a822/progs/tests/getteximage.c
**/

#ifndef _OFFSCREEN_H
#define _OFFSCREEN_H
#define TAG "TangoOffscreen"
#include <common.h>
#include <math.h>
#include <GLES/gl.h>
#include <GLES/glext.h>
#include <GLES/glplatform.h>

#define CLR_W 1280
#define CLR_H 720

#ifdef __cplusplus
extern "C" {
#endif

extern bool tango_ready;
extern int textureID;
extern application_handle_t *application;

void scrape(void *destination);
void renderScrape(void *destionation, uint32_t w, uint32_t h);
void initOffscreen(uint32_t w, uint32_t h, GLint *textureID);

JNIEXPORT void JNICALL Java_edu_uml_OffscreenRenderer_dowork(JNIEnv *env, jobject caller);

#ifdef __cplusplus
}
#endif
#endif