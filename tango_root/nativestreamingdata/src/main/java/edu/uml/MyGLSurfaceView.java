package edu.uml;

import android.content.Context;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by csrobot on 9/4/14.
 */
public class MyGLSurfaceView extends GLSurfaceView {

    //MyGLSurfaceRenderer renderer;

    public MyGLSurfaceView(Context context) {
        super(context);
        setRenderer(new Renderer());
    }

    private static class Renderer implements GLSurfaceView.Renderer {
        public void onDrawFrame(GL10 gl) {
            OffScreenRenderer.dowork();
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            //init(width, height);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
// Do nothing.
        }

    }
}
