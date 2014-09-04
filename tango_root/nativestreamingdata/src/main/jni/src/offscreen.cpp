#include <offscreen.hpp>

void scrapeTexture(void *destination)
{
    glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, destination);
}

void renderScrape(void *destionation, uint32_t w, uint32_t h)
{

    GLuint fb,textureID;
    glGenTextures(1, &textureID);
    glGenFramebuffersEXT(1, &fb);
    glBindTexture(GL_TEXTURE_2D, textureID);

    double color_timestamp;
    VideoOverlayRenderLatestFrame(application, textureID, w, h, &color_timestamp);

    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, fb);
    glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT,
                            GL_TEXTURE_2D, *textureID, 0);

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

static void
Draw(void)
{
   static double laststamp;
   glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
   double color_timestamp;
   VideoOverlayRenderLatestFrame(application, textureID, _width, _height, &color_timestamp)
   if (laststamp != color_timestamp)
   {
       static void *storage;
       initOffscreen(CLR_W, CLR_H, &storage);
       renderScrape(storage, CLR_W, CLR_H);
   }
   glutSwapBuffers();
}


static void
Reshape(int width, int height)
{
}


static void
Key(unsigned char key, int x, int y)
{
}


static void
Init(void)
{
}

//this should never return
JNIEXPORT void JNICALL Java_edu_uml_OffscreenRenderer_dowork(JNIEnv *env, jobject caller)
{
   glutInit(NULL, 0);
   glutInitWindowPosition(0, 0);
   glutInitWindowSize(1280, 720);
   glutInitDisplayMode(GLUT_RGB | GLUT_DOUBLE | GLUT_DEPTH);
   Win = glutCreateWindow("test?");
   glewInit();
   glutReshapeFunc(Reshape);
   glutKeyboardFunc(Key);
   glutDisplayFunc(Draw);
   Init();
   glutMainLoop();
}