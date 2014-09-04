#include <offscreen.hpp>

void scrapeTexture(void *destination)
{
    //glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, destination);
    glReadPixels(0,0,1280,720,GL_RGBA,GL_UNSIGNED_BYTE,destination);
}

void renderScrape(void *destination, uint32_t w, uint32_t h)
{

    GLuint fb,textureID;
    glGenTextures(1, &textureID);
    glGenFramebuffers(1, &fb);
    glBindTexture(GL_TEXTURE_2D, textureID);

    double color_timestamp;

    glBindFramebuffer(GL_FRAMEBUFFER, fb);
    glFramebufferTexture1D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                            GL_TEXTURE_2D, textureID, 0);

    VideoOverlayRenderLatestFrame(application, textureID, w, h, &color_timestamp);

    glViewport(0, 0, w, h);
    scrape(destination);
}

void initOffscreen(uint32_t w, uint32_t h, void **storage)
{
    if (storage != NULL && *storage == NULL)
    {
        *storage = malloc(4 * w * h);
    }
}

//this should never return
JNIEXPORT void JNICALL Java_edu_uml_OffscreenRenderer_dowork(JNIEnv *env, jobject caller)
{
   static double laststamp;
   glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
   double color_timestamp;
   if (tango_ready && application != NULL)
   {
       static void *storage;
       initOffscreen(CLR_W, CLR_H, &storage);
       renderScrape(storage, CLR_W, CLR_H);
       if (laststamp != color_timestamp)
       {
            LOGI("HOLY SHIT");
       }
   }
}