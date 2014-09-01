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
package edu.uml;

import java.nio.ByteBuffer;

import android.util.Log;

public class TangoAPI extends Thread {

    private final int UPDATED_DEPTH = 1 << 2;
    private final int UPDATED_ODOM = 1 << 1;

    private VIOReceiver vioReceiver;
    private DepthReceiver depthReceiver;

    private boolean mBreakout = false;
    private static final String TAG = "TangoApi";
    private boolean ok = true;

    float tx;
    float ty;
    float tz;
    float rx;
    float ry;
    float rz;
    float rw;

    public TangoAPI(VIOReceiver vrec, DepthReceiver drec) {
        if (vrec == null) {
            throw new IllegalArgumentException("TANGOAPI vioReceiver must not be null");
        }
        if (drec == null) {
            throw new IllegalArgumentException("TANGOAPI depthReceiver must not be null");
        }
        vioReceiver = vrec;
        depthReceiver = drec;
    }

    static {
        System.loadLibrary("tango_api");
        System.loadLibrary("tango_api_jni");
    }

    /*
     * Called from native-land when native-land thinks the bytebuffer size might be incorrect
     */
    public void setBufferLength(int length) {
        Log.i(TAG, "Trying to change the size of our ByteBuffer to " + length);
        if (depthReceiver != null) {
            if (depthReceiver.buffer != null) {
                depthReceiver.buffer.clear();
                depthReceiver.buffer = null;
            }
            depthReceiver.buffer = ByteBuffer.allocateDirect(length);
            setbuffer(depthReceiver.buffer);
        }
    }

    public void die() {
        Log.e(TAG, "DYING");
        mBreakout = true;
        try {
            join();
        }
        catch(InterruptedException ie)
        {
            Log.e(TAG, "Interrupted while joining: ",ie);
        }
    }

    @Override
    public void start() {
        Log.e(TAG, "STARTING");
        mBreakout = false;
        ok = true;
        State currState = getState();
        Log.e(TAG, "CURRENT THREAD STATE = " + currState.toString());
        if ((currState == State.RUNNABLE || currState == State.NEW || currState == State.TERMINATED) && init())
            super.start();
    }

    @Override
    public void run() {
        int res=0;
        while (true) {
            if (mBreakout) {
                Log.e(TAG, "Breaking out");
                break;
            }
            res = dowork();
            if ((res & UPDATED_DEPTH) != 0)
                depthReceiver.DepthCallback();
            if ((res & UPDATED_ODOM) != 0)
                vioReceiver.VIOCallback(tx, ty, tz, rx, ry, rz, rw);
            try {
                sleep(20);
            } catch (InterruptedException e) {
                Log.e(TAG, "INSOMNIA", e);
            }
        }
        Log.e(TAG, "Broke out");
    }

    public native void setbuffer(ByteBuffer b);

    public native int dowork();

    public static native boolean init();

    public static native boolean deinit();
}
