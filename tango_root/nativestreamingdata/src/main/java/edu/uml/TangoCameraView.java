package edu.uml;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by emarcoux on 9/11/14.
 */
public class TangoCameraView extends GLSurfaceView {
    private static final String TAG = "TangoCameraView";
    private static final boolean DEBUG = false;


    private boolean stopOnDetach;
    private boolean isInitialized;

    private VideoReciever videoReciever;

    private Thread         renderThread;
    private RenderRunnable renderRunnable;

    public ByteBuffer applicationState; // native ByteBuffer holding tango application state

    public TangoCameraView(Context context) {
        super(context);
        Log.v(TAG, "ITZ ALIVVVEE");
        this.isInitialized = false;
        this.stopOnDetach = true;
        // generate some no-op receivers until the actual receivers are set
        videoReciever   = new VideoReciever() { @Override public void VideoCallback() { } };

        isInitialized = true;

        initGL(false, 0, 0);
    }

    public TangoCameraView(Context context, AttributeSet attrs) { this(context); }


    /**
     * Whether or not the activity should destroy it's inner state when it's
     * not currently attached to the active activity.
     *
     * If this is disabled MAKE SURE you manually call onDetachedFromWindow()
     * when you're done or there will be memory leaks!
     *
     * @param stopOnDetach
     */
    public void setStopOnDetach(boolean stopOnDetach) { this.stopOnDetach = stopOnDetach; }
    public boolean getStopOnDetach() { return stopOnDetach; }


    /**
     * Initialize the tango_api application, video overlay and
     * VIO whenever the user attaches the view to a window.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInitialized) {
//            videoReciever.buffer = (ByteBuffer) allocNativeVideoBuffer(0);
            if (init()) {
                isInitialized = true;

                renderRunnable = new RenderRunnable();
                renderThread = new Thread(renderRunnable);
                renderThread.start();
            }
        }
    }

    /**
     * When the view is no longer part of the active activity
     * destroy it's underlying jni state
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(isInitialized && stopOnDetach) {
            if(destruct()) {
                isInitialized = false;

                try {
                    renderRunnable.terminate();
                    renderThread.join();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Could not terminate the render thread", e);
                } finally {
                    freeNativeVideoBuffer();
                }
            }
        }
    }

    /**
     * Register the receiver that holds the state of the depth
     * buffer.  There must always be a single receiver so one
     * is created under the hood to temporarily hold state until
     * the user attaches their own.
     */
    public void setVideoReceiver(VideoReciever videoReciever) {
        try {
            if(renderThread != null) {
                renderRunnable.terminate();
                renderThread.join();
            }

            videoReciever.buffer = this.videoReciever.buffer;
            this.videoReciever = videoReciever;
            if(renderThread != null) {
                renderRunnable = new RenderRunnable();
                renderThread.start();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Could not terminate the render thread", e);
        }
    }



    /**
     * Called from native-land when native-land thinks the bytebuffer size might be incorrect
     */
    public void setDepthBufferLength(int length) { //TODO change name native side
        Log.i(TAG, "Trying to change the size of our ByteBuffer to " + length);
        if (videoReciever != null) {
            if (videoReciever.buffer != null) {
                videoReciever.buffer.clear();
                freeNativeVideoBuffer();
                videoReciever.buffer = null;
            }
            if (length > 0) {                videoReciever.VideoCallback();

                videoReciever.buffer = (ByteBuffer) allocNativeVideoBuffer(length);
                videoReciever.buffer.order(ByteOrder.LITTLE_ENDIAN);
            }
        }
    }

    /**
     * The runnable that polls the camera for frames, renders them using OpenGL, and
     * then notifies the vio and depth receivers that they have new information.
     */
    private class RenderRunnable implements Runnable {
        private boolean terminated;
        {}

        public RenderRunnable() { terminated = false; }

        @Override
        public void run() {
            int res = 0;
            while(!terminated) {
//                TangoCameraView.this.invalidate();

                try {
                    Thread.sleep(33);
                } catch (InterruptedException e) {
                    Log.e(TAG, "wasn't able to wake the sleeping bear", e);
                }
            }
        }

        public void terminate() { this.terminated = true; }
    }


    // START of OpenGL stuffs
    private void initGL(boolean translucent, int depth, int stencil) {

        /* By default, GLSurfaceView() creates a RGB_565 opaque surface.
         * If we want a translucent one, we should change the surface's
         * format here, using PixelFormat.TRANSLUCENT for GL Surfaces
         * is interpreted as any 32-bit surface with alpha by SurfaceFlinger.
         */
        if (translucent) {
            this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }

        /* Setup the context factory for 2.0 rendering.
         * See ContextFactory class definition below
         */
        setEGLContextFactory(new TangoCameraViewContextFactory());

        /* We need to choose an EGLConfig that matches the format of
         * our surface exactly. This is going to be done in our
         * custom config chooser. See ConfigChooser class definition
         * below.
         */
        setEGLConfigChooser( translucent ?
                new ConfigChooser(8, 8, 8, 8, depth, stencil) :
                new ConfigChooser(5, 6, 5, 0, depth, stencil) );

        /* Set the renderer responsible for frame rendering */
        setRenderer(new TangoCameraViewRenderer());
    }

    private static class TangoCameraViewContextFactory implements GLSurfaceView.EGLContextFactory {
        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
            Log.w(TAG, "creating OpenGL ES 2.0 context");
            checkEglError("Before eglCreateContext", egl);
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
            checkEglError("After eglCreateContext", egl);
            return context;
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            egl.eglDestroyContext(display, context);
        }
    }

    private static void checkEglError(String prompt, EGL10 egl) {
        int error;
        while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS) {
            Log.e(TAG, String.format("%s: EGL error: 0x%x", prompt, error));
        }
    }

    private static class ConfigChooser implements GLSurfaceView.EGLConfigChooser {

        public ConfigChooser(int r, int g, int b, int a, int depth, int stencil) {
            mRedSize = r;
            mGreenSize = g;
            mBlueSize = b;
            mAlphaSize = a;
            mDepthSize = depth;
            mStencilSize = stencil;
        }

        /* This EGL config specification is used to specify 2.0 rendering.
         * We use a minimum size of 4 bits for red/green/blue, but will
         * perform actual matching in chooseConfig() below.
         */
        private static int EGL_OPENGL_ES2_BIT = 4;
        private static int[] s_configAttribs2 =
                {
                        EGL10.EGL_RED_SIZE, 4,
                        EGL10.EGL_GREEN_SIZE, 4,
                        EGL10.EGL_BLUE_SIZE, 4,
                        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                        EGL10.EGL_NONE
                };

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {

            /* Get the number of minimally matching EGL configurations
             */
            int[] num_config = new int[1];
            egl.eglChooseConfig(display, s_configAttribs2, null, 0, num_config);

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException("No configs match configSpec");
            }

            /* Allocate then read the array of minimally matching EGL configs
             */
            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, s_configAttribs2, configs, numConfigs, num_config);

            if (DEBUG) {
                printConfigs(egl, display, configs);
            }
            /* Now return the "best" one
             */
            return chooseConfig(egl, display, configs);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                                      EGLConfig[] configs) {
            for(EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);

                // We need at least mDepthSize and mStencilSize bits
                if (d < mDepthSize || s < mStencilSize)
                    continue;

                // We want an *exact* match for red/green/blue/alpha
                int r = findConfigAttrib(egl, display, config,
                 // here be dragons
           EGL10.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(egl, display, config,
                        EGL10.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(egl, display, config,
                        EGL10.EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(egl, display, config,
                        EGL10.EGL_ALPHA_SIZE, 0);

                if (r == mRedSize && g == mGreenSize && b == mBlueSize && a == mAlphaSize)
                    return config;
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                                     EGLConfig config, int attribute, int defaultValue) {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }

        private void printConfigs(EGL10 egl, EGLDisplay display,
                 // here be dragons
                     EGLConfig[] configs) {
            int numConfigs = configs.length;
            Log.w(TAG, String.format("%d configurations", numConfigs));
            for (int i = 0; i < numConfigs; i++) {
                Log.w(TAG, String.format("Configuration %d:\n", i));
                printConfig(egl, display, configs[i]);
            }
        }

        private void printConfig(EGL10 egl, EGLDisplay display,
                                 EGLConfig config) {
            int[] attributes = {
                    EGL10.EGL_BUFFER_SIZE,
                    EGL10.EGL_ALPHA_SIZE,
                    EGL10.EGL_BLUE_SIZE,
                    EGL10.EGL_GREEN_SIZE,
                    EGL10.EGL_RED_SIZE,
                    EGL10.EGL_DEPTH_SIZE,
                    EGL10.EGL_STENCIL_SIZE,
                    EGL10.EGL_CONFIG_CAVEAT,
                    EGL10.EGL_CONFIG_ID,
                    EGL10.EGL_LEVEL,
                    EGL10.EGL_MAX_PBUFFER_HEIGHT,
                    EGL10.EGL_MAX_PBUFFER_PIXELS,
                    EGL10.EGL_MAX_PBUFFER_WIDTH,
                    EGL10.EGL_NATIVE_RENDERABLE,
                    EGL10.EGL_NATIVE_VISUAL_ID,
                    EGL10.EGL_NATIVE_VISUAL_TYPE,
                    0x3030, // EGL10.EGL_PRESERVED_RESOURCES,
                    EGL10.EGL_SAMPLES,
                    EGL10.EGL_SAMPLE_BUFFERS,
                    EGL10.EGL_SURFACE_TYPE,
                    EGL10.EGL_TRANSPARENT_TYPE,
                    EGL10.EGL_TRANSPARENT_RED_VALUE,
                    EGL10.EGL_TRANSPARENT_GREEN_VALUE,
                    EGL10.EGL_TRANSPARENT_BLUE_VALUE,
                    0x3039, // EGL10.EGL_BIND_TO_TEXTURE_RGB,
                    0x303A, // EGL10.EGL_BIND_TO_TEXTURE_RGBA,
                    0x303B, // EGL10.EGL_MIN_SWAP_INTERVAL,
                    0x303C, // EGL10.EGL_MAX_SWAP_INTERVAL,
                    EGL10.EGL_LUMINANCE_SIZE,
                    EGL10.EGL_ALPHA_MASK_SIZE,
                    EGL10.EGL_COLOR_BUFFER_TYPE,
                    EGL10.EGL_RENDERABLE_TYPE,
                    0x3042 // EGL10.EGL_CONFORMANT
            };
            String[] names = {
                    "EGL_BUFFER_SIZE",
                    "EGL_ALPHA_SIZE",
                    "EGL_BLUE_SIZE",
                    "EGL_GREEN_SIZE",
                    "EGL_RED_SIZE",
                    "EGL_DEPTH_SIZE",
                    "EGL_STENCIL_SIZE",
                    "EGL_CONFIG_CAVEAT",
                    "EGL_CONFIG_ID",
                    "EGL_LEVEL",
                    "EGL_MAX_PBUFFER_HEIGHT",
                    "EGL_MAX_PBUFFER_PIXELS",
                    "EGL_MAX_PBUFFER_WIDTH",
                    "EGL_NATIVE_RENDERABLE",
                    "EGL_NATIVE_VISUAL_ID",
                    "EGL_NATIVE_VISUAL_TYPE",
                    "EGL_PRESERVED_RESOURCES",
                    "EGL_SAMPLES",
                    "EGL_SAMPLE_BUFFERS",
                    "EGL_SURFACE_TYPE",
                    "EGL_TRANSPARENT_TYPE",
                    "EGL_TRANSPARENT_RED_VALUE",
                    "EGL_TRANSPARENT_GREEN_VALUE",
                    "EGL_TRANSPARENT_BLUE_VALUE",
                    "EGL_BIND_TO_TEXTURE_RGB",
                    "EGL_BIND_TO_TEXTURE_RGBA",
                    "EGL_MIN_SWAP_INTERVAL",
                    "EGL_MAX_SWAP_INTERVAL",
                    "EGL_LUMINANCE_SIZE",
                    "EGL_ALPHA_MASK_SIZE",
                    "EGL_COLOR_BUFFER_TYPE",
                    "EGL_RENDERABLE_TYPE",
                    "EGL_CONFORMANT"
            };
            int[] value = new int[1];
            for (int i = 0; i < attributes.length; i++) {
                int attribute = attributes[i];
                String name = names[i];
                if ( egl.eglGetConfigAttrib(display, config, attribute, value)) {
                    Log.w(TAG, String.format("  %s: %d\n", name, value[0]));
                } else {
                    // Log.w(TAG, String.format("  %s: failed\n", name));
                    while (egl.eglGetError() != EGL10.EGL_SUCCESS);
                }
            }
        }

        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
        private int[] mValue = new int[1];
    }


    private class TangoCameraViewRenderer implements GLSurfaceView.Renderer {
        public void onDrawFrame(GL10 gl) {
            onDrawNative();
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            onSurfaceChangedNative(width, height);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            onSurfaceCreatedNative();
        }
    }
    //END of OpenGL Stuffs


    // here be dragons
    static {
        System.loadLibrary("tango_api");
        System.loadLibrary("tango_camera_view");
    }

    public native boolean init();
    public native boolean destruct();

    public native Object allocNativeVideoBuffer(int size);
    public native void freeNativeVideoBuffer();

    public native void onSurfaceCreatedNative();
    public native void onSurfaceChangedNative(int w, int h);

    public native void onDrawNative();
}
