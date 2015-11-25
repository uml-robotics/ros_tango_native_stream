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
 * Author: Eric McCann <emccann@cs.uml.edu>, Eric Marcoux <emarcoux@cs.uml.edu>
*/
package edu.uml;


import android.app.Activity;

import com.google.common.base.Preconditions;

public class TangoAPI {

    private static TangoAPI instance = null;
    private static Object   instanceLock = new Object();


    private VIOReceiver              mVioReceiver;
    private ImageReciever            mImageReciever;
    private DepthReceiver            mDepthReceiver;
    private TangoAPIExceptionHandler mTangoAPIExceptionHandler;
    private Activity                 mActivity;

    private static final String TAG = "TangoApi";

    private TangoAPI(Activity activity,
                    VIOReceiver vioReceiver,
                    ImageReciever imageReciever,
                    DepthReceiver depthReceiver,
                    TangoAPIExceptionHandler tangoAPIExceptionHandler) {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(vioReceiver);
        Preconditions.checkNotNull(imageReciever);
        Preconditions.checkNotNull(depthReceiver);
        Preconditions.checkNotNull(tangoAPIExceptionHandler);
        mActivity = activity;
        mTangoAPIExceptionHandler = tangoAPIExceptionHandler;


        mVioReceiver = vioReceiver;
        mVioReceiver.tangoAPI = this;

        mImageReciever = imageReciever;
        mImageReciever.tangoAPI = this;

        mDepthReceiver = depthReceiver;
        mDepthReceiver.tangoAPI = this;
    }


    public static int startTangoAPI(Activity activity,
                                    VIOReceiver vioReceiver,
                                    ImageReciever imageReciever,
                                    DepthReceiver depthReceiver,
                                    TangoAPIExceptionHandler tangoAPIExceptionHandler)
                                        throws IllegalArgumentException
    {
        synchronized (instanceLock) {
            if(instance == null) {
                instance = new TangoAPI(activity, vioReceiver, imageReciever, depthReceiver, tangoAPIExceptionHandler);
                return instance.nStartTangoAPI(instance.mActivity,
                        instance.mVioReceiver,
                        instance.mImageReciever,
                        instance.mDepthReceiver,
                        instance.mTangoAPIExceptionHandler);
            }
            return TangoAPIException.ERR_ALREADY_RUNNING;
        }
    }



    public static TangoAPI getInstance() { synchronized (instanceLock) {return instance;}}

    public int stopTangoAPI() {
        synchronized (instanceLock) {
            if(instance != null) {
                int ret = instance.nStopTangoAPI();
                instance = null;
                return ret;
            }
            return TangoAPIException.ERR_ALREADY_STOPPED;
        }
    }

    public void sendTheoraHeadersOnNextPublish() {
        nSendTheoraHeaderOnNextPublish();
    }



    private native int nStartTangoAPI(Activity activity,
                                         VIOReceiver vioReceiver,
                                         ImageReciever imageReciever,
                                         DepthReceiver depthReceiver,
                                         TangoAPIExceptionHandler exceptionHandler);
    private native int nStopTangoAPI();

    private native int nSendTheoraHeaderOnNextPublish();

    static {
        System.loadLibrary("lfds611");
        System.loadLibrary("tango_client_api");
        System.loadLibrary("tango_api_jni");
    }
}
