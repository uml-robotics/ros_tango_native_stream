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
 * Author: Jordan Allspaw <jallspaw@cs.uml.edu>
*/
package edu.uml;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by csrobot on 9/4/14.
 */
public class OffscreenRenderer extends Thread {
    private static final String TAG = "OffscreenRenderer";
    @Override
    public void start() {
        Log.e(TAG, "STARTING");
        State currState = getState();
        Log.e(TAG, "CURRENT THREAD STATE = " + currState.toString());
        if ((currState == State.RUNNABLE || currState == State.NEW || currState == State.TERMINATED))
            super.start();
    }
    public static Context context;

    @Override
    public void run() {
        surface = new MyGLSurfaceView(context);
    }

    public static native void offScreenRenderer();

    public static native void dowork();

    MyGLSurfaceView surface;
}
